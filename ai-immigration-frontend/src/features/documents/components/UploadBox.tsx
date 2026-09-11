import {
  useId,
  useRef,
  useState,
  type ChangeEvent,
  type DragEvent,
} from "react";

import {
  CheckCircle2,
  FileImage,
  FileText,
  Loader2,
  UploadCloud,
  X,
} from "lucide-react";

import Button from "../../../components/common/Button";
import UploadProgressBar from "./UploadProgressBar";
import FileTypeError from "./FileTypeError";

interface Props {
  onUpload: (
    file: File,
    onProgress?: (progress: number) => void
  ) => Promise<void>;
}

/**
 * Maximum file size accepted by the client.
 *
 * IMPORTANT:
 * The backend should enforce the same limit independently.
 */
const MAX_FILE_SIZE = 10 * 1024 * 1024;
const MAX_FILE_SIZE_MB = 10;

/**
 * MIME types accepted by the application.
 *
 * PDF:
 *   application/pdf
 *
 * PNG:
 *   image/png
 *
 * JPEG:
 *   image/jpeg
 */
const ALLOWED_MIME_TYPES = new Set([
  "application/pdf",
  "image/png",
  "image/jpeg",
]);

/**
 * Extensions are checked as a fallback because some browsers,
 * operating systems, or drag/drop sources can provide an empty
 * or unreliable MIME type.
 */
const ALLOWED_EXTENSIONS = new Set([
  ".pdf",
  ".png",
  ".jpg",
  ".jpeg",
]);

const ACCEPT_ATTRIBUTE =
  ".pdf,.png,.jpg,.jpeg,application/pdf,image/png,image/jpeg";

type UploadErrorKind =
  | "validation"
  | "server"
  | "network"
  | "authentication"
  | "unknown";

interface NormalizedUploadError {
  kind: UploadErrorKind;
  message: string;
  status?: number;
}

interface AxiosLikeError {
  response?: {
    status?: number;
    data?: {
      message?: string;
      error?: string;
      detail?: string;
    };
  };
  request?: unknown;
  message?: string;
}

/**
 * Safely extracts a file extension.
 */
const getFileExtension = (fileName: string): string => {
  const lastDot = fileName.lastIndexOf(".");

  if (lastDot === -1) {
    return "";
  }

  return fileName.slice(lastDot).toLowerCase();
};

/**
 * Returns a human-readable file size.
 */
const formatFileSize = (bytes: number): string => {
  if (bytes < 1024) {
    return `${bytes} B`;
  }

  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`;
  }

  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
};

/**
 * Extracts a useful error message from Axios-style errors,
 * Fetch-style errors, or regular JavaScript errors.
 *
 * Server errors are deliberately classified separately from
 * file validation errors.
 */
const extractUploadError = (
  error: unknown
): NormalizedUploadError => {
  if (error && typeof error === "object") {
    const axiosError = error as AxiosLikeError;

    const status = axiosError.response?.status;

    const serverMessage =
      axiosError.response?.data?.message ||
      axiosError.response?.data?.detail ||
      axiosError.response?.data?.error;

    if (status === 401 || status === 403) {
      return {
        kind: "authentication",
        status,
        message:
          serverMessage ||
          "Your session has expired or you do not have permission to upload this document.",
      };
    }

    if (status && status >= 500) {
      return {
        kind: "server",
        status,
        message:
          serverMessage ||
          "The server could not process this document. Please try again.",
      };
    }

    if (status === 413) {
      return {
        kind: "validation",
        status,
        message:
          "The document is too large. Please select a file smaller than 10MB.",
      };
    }

    if (status === 400 || status === 415 || status === 422) {
      return {
        kind: "validation",
        status,
        message:
          serverMessage ||
          "The document could not be accepted. Please check the file and try again.",
      };
    }

    if (serverMessage) {
      return {
        kind: "unknown",
        status,
        message: serverMessage,
      };
    }

    if (axiosError.request) {
      return {
        kind: "network",
        message:
          "Unable to reach the server. Please check your connection and try again.",
      };
    }

    if (typeof axiosError.message === "string") {
      return {
        kind: "unknown",
        message: axiosError.message,
      };
    }
  }

  if (error instanceof Error) {
    return {
      kind: "unknown",
      message:
        error.message || "Upload failed. Please try again.",
    };
  }

  return {
    kind: "unknown",
    message: "Upload failed. Please try again.",
  };
};

/**
 * Validates the selected document before upload.
 *
 * Both MIME type and extension are considered.
 * This provides better browser compatibility while the backend
 * remains the authoritative security validation layer.
 */
const validateFile = (
  selected: File
): string => {
  const fileName = selected.name.trim();

  if (!fileName) {
    return "Please select a valid document.";
  }

  if (selected.size <= 0) {
    return "The selected file is empty.";
  }

  if (selected.size > MAX_FILE_SIZE) {
    return `File size must be ${MAX_FILE_SIZE_MB}MB or smaller.`;
  }

  const extension = getFileExtension(fileName);
  const mimeType = selected.type.toLowerCase().trim();

  const extensionAllowed =
    ALLOWED_EXTENSIONS.has(extension);

  const mimeAllowed =
    mimeType.length === 0 ||
    ALLOWED_MIME_TYPES.has(mimeType);

  /**
   * If a browser reports no MIME type, allow the extension
   * to determine the client-side result.
   *
   * If a MIME type is explicitly supplied and is unsupported,
   * reject it even if the extension appears valid.
   */
  if (!extensionAllowed) {
    return (
      "Unsupported file type. Please select a PDF, PNG, or JPEG document."
    );
  }

  if (!mimeAllowed) {
    return (
      "The selected file type is not supported. Please select a PDF, PNG, or JPEG document."
    );
  }

  return "";
};

export default function UploadBox({
  onUpload,
}: Props) {
  const inputId = useId();
  const inputRef = useRef<HTMLInputElement | null>(null);

  const [file, setFile] =
    useState<File | null>(null);

  const [loading, setLoading] =
    useState(false);

  const [progress, setProgress] =
    useState(0);

  const [error, setError] =
    useState("");

  const [errorKind, setErrorKind] =
    useState<UploadErrorKind>("validation");

  const [dragActive, setDragActive] =
    useState(false);

  const [uploadComplete, setUploadComplete] =
    useState(false);

  const clearSelection = () => {
    if (loading) {
      return;
    }

    setFile(null);
    setError("");
    setProgress(0);
    setUploadComplete(false);

    if (inputRef.current) {
      inputRef.current.value = "";
    }
  };

  const selectFile = (
    selected: File | undefined
  ) => {
    if (!selected || loading) {
      return;
    }

    const validation = validateFile(selected);

    if (validation) {
      setFile(null);
      setProgress(0);
      setUploadComplete(false);
      setError(validation);
      setErrorKind("validation");
      return;
    }

    setFile(selected);
    setError("");
    setProgress(0);
    setUploadComplete(false);
  };

  const handleFileChange = (
    event: ChangeEvent<HTMLInputElement>
  ) => {
    selectFile(event.target.files?.[0]);

    /**
     * Reset the input so the same file can be selected again
     * after an upload failure.
     */
    event.target.value = "";
  };

  const handleDragOver = (
    event: DragEvent<HTMLDivElement>
  ) => {
    event.preventDefault();

    if (loading) {
      return;
    }

    event.dataTransfer.dropEffect = "copy";
    setDragActive(true);
  };

  const handleDragLeave = (
    event: DragEvent<HTMLDivElement>
  ) => {
    event.preventDefault();

    /**
     * Avoid flickering when moving between children inside
     * the drop zone.
     */
    if (
      event.currentTarget ===
      event.target
    ) {
      setDragActive(false);
    }
  };

  const handleDrop = (
    event: DragEvent<HTMLDivElement>
  ) => {
    event.preventDefault();

    setDragActive(false);

    if (loading) {
      return;
    }

    const droppedFiles =
      event.dataTransfer.files;

    if (!droppedFiles?.length) {
      return;
    }

    if (droppedFiles.length > 1) {
      setFile(null);
      setError(
        "Please select only one document at a time."
      );
      setErrorKind("validation");
      return;
    }

    selectFile(droppedFiles[0]);
  };

  const handleBrowseClick = () => {
    if (loading) {
      return;
    }

    inputRef.current?.click();
  };

  const submit = async () => {
    if (!file || loading) {
      return;
    }

    /**
     * Revalidate immediately before sending.
     *
     * This protects against a File object changing unexpectedly
     * between selection and submission.
     */
    const validation = validateFile(file);

    if (validation) {
      setError(validation);
      setErrorKind("validation");
      return;
    }

    try {
      setLoading(true);
      setError("");
      setProgress(0);
      setUploadComplete(false);

      await onUpload(
        file,
        (value: number) => {
          const numericValue =
            Number.isFinite(value)
              ? value
              : 0;

          const safeProgress = Math.min(
            100,
            Math.max(
              0,
              Math.round(numericValue)
            )
          );

          setProgress(safeProgress);
        }
      );

      setProgress(100);
      setUploadComplete(true);
      setError("");
    } catch (uploadError: unknown) {
      const normalizedError =
        extractUploadError(uploadError);

      setError(
        normalizedError.message
      );

      setErrorKind(
        normalizedError.kind
      );

      setUploadComplete(false);
    } finally {
      setLoading(false);
    }
  };

  const getFileIcon = () => {
    if (!file) {
      return (
        <UploadCloud
          size={48}
          strokeWidth={1.7}
          aria-hidden="true"
        />
      );
    }

    const extension =
      getFileExtension(file.name);

    if (
      extension === ".png" ||
      extension === ".jpg" ||
      extension === ".jpeg"
    ) {
      return (
        <FileImage
          size={42}
          strokeWidth={1.7}
          aria-hidden="true"
        />
      );
    }

    return (
      <FileText
        size={42}
        strokeWidth={1.7}
        aria-hidden="true"
      />
    );
  };

  return (
    <section
      aria-label="Document upload"
      className="
        rounded-3xl
        border
        border-white/10
        bg-white/[0.04]
        p-5
        shadow-2xl
        shadow-black/20
        backdrop-blur-xl
        sm:p-7
        lg:p-8
      "
    >
      <div
        role="button"
        tabIndex={loading ? -1 : 0}
        aria-label="Document upload drop zone"
        aria-disabled={loading}
        onClick={handleBrowseClick}
        onKeyDown={(event) => {
          if (
            loading
          ) {
            return;
          }

          if (
            event.key === "Enter" ||
            event.key === " "
          ) {
            event.preventDefault();
            handleBrowseClick();
          }
        }}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        className={`
          relative
          rounded-2xl
          border-2
          border-dashed
          p-7
          text-center
          transition-all
          duration-200
          sm:p-10
          lg:p-12

          ${
            dragActive
              ? `
                border-[#C6A15B]
                bg-[#C6A15B]/10
                shadow-lg
                shadow-[#C6A15B]/10
              `
              : `
                border-white/15
                bg-black/5
                hover:border-white/25
                hover:bg-white/[0.03]
              `
          }

          ${
            loading
              ? "cursor-not-allowed opacity-80"
              : "cursor-pointer"
          }

          focus:outline-none
          focus-visible:ring-2
          focus-visible:ring-[#C6A15B]
          focus-visible:ring-offset-2
          focus-visible:ring-offset-slate-950
        `}
      >
        <div
          className="
            mx-auto
            flex
            h-16
            w-16
            items-center
            justify-center
            rounded-2xl
            bg-[#C6A15B]/10
            text-[#C6A15B]
            ring-1
            ring-[#C6A15B]/20
          "
        >
          {loading ? (
            <Loader2
              size={42}
              className="animate-spin"
              aria-hidden="true"
            />
          ) : (
            getFileIcon()
          )}
        </div>

        <h3
          className="
            mt-5
            text-xl
            font-bold
            tracking-tight
            text-white
          "
        >
          {loading
            ? "Uploading document…"
            : uploadComplete
              ? "Document uploaded"
              : "Upload your document"}
        </h3>

        <p
          className="
            mx-auto
            mt-2
            max-w-xl
            text-sm
            leading-6
            text-slate-300
          "
        >
          {loading
            ? "Please keep this page open while your document is being uploaded."
            : uploadComplete
              ? "Your document has been successfully uploaded."
              : "Drag and drop your document here, or choose a file from your device."}
        </p>

        {!loading && !uploadComplete && (
          <div
            className="
              mt-5
              flex
              flex-wrap
              items-center
              justify-center
              gap-2
              text-xs
              text-slate-400
            "
          >
            <span
              className="
                rounded-full
                border
                border-white/10
                bg-white/5
                px-3
                py-1.5
              "
            >
              PDF
            </span>

            <span
              className="
                rounded-full
                border
                border-white/10
                bg-white/5
                px-3
                py-1.5
              "
            >
              PNG
            </span>

            <span
              className="
                rounded-full
                border
                border-white/10
                bg-white/5
                px-3
                py-1.5
              "
            >
              JPEG
            </span>

            <span
              className="
                rounded-full
                border
                border-white/10
                bg-white/5
                px-3
                py-1.5
              "
            >
              Max {MAX_FILE_SIZE_MB}MB
            </span>
          </div>
        )}

        <input
          ref={inputRef}
          id={inputId}
          type="file"
          accept={ACCEPT_ATTRIBUTE}
          className="sr-only"
          onChange={handleFileChange}
          disabled={loading}
          aria-describedby={`${inputId}-help`}
        />

        {!loading && !uploadComplete && (
          <button
            type="button"
            onClick={(event) => {
              event.stopPropagation();
              handleBrowseClick();
            }}
            disabled={loading}
            className="
              mt-7
              inline-flex
              min-h-11
              items-center
              justify-center
              rounded-xl
              bg-[#C6A15B]
              px-6
              py-3
              text-sm
              font-semibold
              text-black
              shadow-lg
              shadow-[#C6A15B]/10
              transition
              hover:bg-[#A8894D]
              focus:outline-none
              focus-visible:ring-2
              focus-visible:ring-[#C6A15B]
              focus-visible:ring-offset-2
              focus-visible:ring-offset-slate-950
              disabled:cursor-not-allowed
              disabled:opacity-50
            "
          >
            Choose File
          </button>
        )}

        <p
          id={`${inputId}-help`}
          className="
            mt-4
            text-xs
            text-slate-500
          "
        >
          Supported document formats: PDF, PNG, and JPEG.
        </p>

        {file && !uploadComplete && (
          <div
            onClick={(event) =>
              event.stopPropagation()
            }
            className="
              mx-auto
              mt-6
              flex
              max-w-xl
              items-center
              gap-3
              rounded-xl
              border
              border-white/10
              bg-white/[0.04]
              p-3
              text-left
            "
          >
            <div
              className="
                flex
                h-10
                w-10
                shrink-0
                items-center
                justify-center
                rounded-lg
                bg-white/5
                text-slate-300
              "
            >
              {getFileIcon()}
            </div>

            <div className="min-w-0 flex-1">
              <p
                className="
                  truncate
                  text-sm
                  font-medium
                  text-white
                "
                title={file.name}
              >
                {file.name}
              </p>

              <p
                className="
                  mt-0.5
                  text-xs
                  text-slate-400
                "
              >
                {formatFileSize(file.size)}
              </p>
            </div>

            <button
              type="button"
              onClick={clearSelection}
              disabled={loading}
              className="
                flex
                h-9
                w-9
                shrink-0
                items-center
                justify-center
                rounded-lg
                text-slate-400
                transition
                hover:bg-red-400/10
                hover:text-red-400
                focus:outline-none
                focus-visible:ring-2
                focus-visible:ring-red-400
                disabled:cursor-not-allowed
                disabled:opacity-40
              "
              aria-label={`Remove ${file.name}`}
              title="Remove file"
            >
              <X
                size={18}
                aria-hidden="true"
              />
            </button>
          </div>
        )}

        {uploadComplete && file && (
          <div
            className="
              mx-auto
              mt-6
              flex
              max-w-xl
              items-center
              gap-3
              rounded-xl
              border
              border-emerald-400/20
              bg-emerald-400/10
              p-4
              text-left
            "
          >
            <CheckCircle2
              size={22}
              className="shrink-0 text-emerald-400"
              aria-hidden="true"
            />

            <div className="min-w-0">
              <p
                className="
                  text-sm
                  font-semibold
                  text-emerald-300
                "
              >
                Upload successful
              </p>

              <p
                className="
                  mt-0.5
                  truncate
                  text-xs
                  text-emerald-200/70
                "
                title={file.name}
              >
                {file.name}
              </p>
            </div>
          </div>
        )}

        {error && (
          <div
            onClick={(event) =>
              event.stopPropagation()
            }
            className="mx-auto mt-6 max-w-xl"
          >
            <FileTypeError
              message={error}
              kind={errorKind}
              fileName={file?.name}
              allowedTypes={[
                "PDF",
                "PNG",
                "JPEG",
              ]}
              onRetry={
                errorKind === "server" ||
                errorKind === "network" ||
                errorKind === "unknown"
                  ? submit
                  : undefined
              }
              onChooseAnother={() => {
                clearSelection();
                requestAnimationFrame(() => {
                  inputRef.current?.click();
                });
              }}
            />
          </div>
        )}

        {loading && file && (
          <div
            onClick={(event) =>
              event.stopPropagation()
            }
            className="mx-auto mt-6 max-w-xl"
          >
            <UploadProgressBar
              fileName={file.name}
              progress={progress}
              status="uploading"
            />
          </div>
        )}
      </div>

      <Button
        loading={loading}
        disabled={
          !file ||
          loading ||
          uploadComplete
        }
        className="
          mt-6
          w-full
        "
        onClick={submit}
      >
        {uploadComplete
          ? "Document Uploaded"
          : "Upload Document"}
      </Button>
    </section>
  );
}

