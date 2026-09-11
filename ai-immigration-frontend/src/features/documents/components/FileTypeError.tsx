import {
  AlertCircle,
  FileWarning,
  LockKeyhole,
  RefreshCw,
  ServerCrash,
  ShieldAlert,
  Upload,
  WifiOff,
} from "lucide-react";

type FileTypeErrorKind =
  | "validation"
  | "server"
  | "network"
  | "authentication"
  | "unknown";

interface FileTypeErrorProps {
  fileName?: string;

  allowedTypes?: string[];

  message?: string;

  kind?: FileTypeErrorKind;

  onRetry?: () => void;

  onChooseAnother?: () => void;

  onDismiss?: () => void;
}

interface ErrorPresentation {
  title: string;
  icon: typeof FileWarning;
  iconClassName: string;
  containerClassName: string;
  titleClassName: string;
  messageClassName: string;
  supportedTypesClassName: string;
}

const getPresentation = (
  kind: FileTypeErrorKind
): ErrorPresentation => {
  switch (kind) {
    case "server":
      return {
        title: "Document processing failed",
        icon: ServerCrash,
        iconClassName:
          "text-orange-400",
        containerClassName:
          "border-orange-400/20 bg-orange-400/10",
        titleClassName:
          "text-orange-300",
        messageClassName:
          "text-orange-200/90",
        supportedTypesClassName:
          "text-slate-400",
      };

    case "network":
      return {
        title: "Connection problem",
        icon: WifiOff,
        iconClassName:
          "text-yellow-400",
        containerClassName:
          "border-yellow-400/20 bg-yellow-400/10",
        titleClassName:
          "text-yellow-300",
        messageClassName:
          "text-yellow-200/90",
        supportedTypesClassName:
          "text-slate-400",
      };

    case "authentication":
      return {
        title: "Authentication required",
        icon: LockKeyhole,
        iconClassName:
          "text-purple-400",
        containerClassName:
          "border-purple-400/20 bg-purple-400/10",
        titleClassName:
          "text-purple-300",
        messageClassName:
          "text-purple-200/90",
        supportedTypesClassName:
          "text-slate-400",
      };

    case "validation":
      return {
        title: "File cannot be uploaded",
        icon: FileWarning,
        iconClassName:
          "text-red-400",
        containerClassName:
          "border-red-400/20 bg-red-400/10",
        titleClassName:
          "text-red-300",
        messageClassName:
          "text-red-200/90",
        supportedTypesClassName:
          "text-slate-400",
      };

    default:
      return {
        title: "Upload failed",
        icon: AlertCircle,
        iconClassName:
          "text-red-400",
        containerClassName:
          "border-red-400/20 bg-red-400/10",
        titleClassName:
          "text-red-300",
        messageClassName:
          "text-red-200/90",
        supportedTypesClassName:
          "text-slate-400",
      };
  }
};

export default function FileTypeError({
  fileName,
  allowedTypes = [
    "PDF",
    "PNG",
    "JPEG",
  ],
  message,
  kind = "validation",
  onRetry,
  onChooseAnother,
  onDismiss,
}: FileTypeErrorProps) {
  const presentation =
    getPresentation(kind);

  const Icon =
    presentation.icon;

  const defaultMessage =
    kind === "server"
      ? "The server could not process this document. Please try again."
      : kind === "network"
        ? "We could not connect to the upload service. Check your internet connection and try again."
        : kind === "authentication"
          ? "Your session may have expired. Please sign in again and retry the upload."
          : kind === "validation"
            ? `${fileName || "This file"} cannot be uploaded.`
            : "Something went wrong while uploading this document. Please try again.";

  const showSupportedTypes =
    kind === "validation" &&
    allowedTypes.length > 0;

  return (
    <div
      role="alert"
      aria-live="assertive"
      className={`
        rounded-2xl
        border
        p-5
        ${presentation.containerClassName}
      `}
    >
      <div className="flex items-start gap-3">
        <div
          className="
            flex
            h-10
            w-10
            shrink-0
            items-center
            justify-center
            rounded-xl
            bg-black/10
          "
        >
          <Icon
            size={21}
            className={
              presentation.iconClassName
            }
            aria-hidden="true"
          />
        </div>

        <div className="min-w-0 flex-1">
          <div className="flex items-start justify-between gap-3">
            <h3
              className={`
                text-sm
                font-semibold
                ${presentation.titleClassName}
              `}
            >
              {presentation.title}
            </h3>

            {onDismiss && (
              <button
                type="button"
                onClick={onDismiss}
                className="
                  shrink-0
                  rounded-md
                  p-1
                  text-slate-400
                  transition
                  hover:bg-white/5
                  hover:text-white
                  focus:outline-none
                  focus-visible:ring-2
                  focus-visible:ring-white/50
                "
                aria-label="Dismiss error"
              >
                ×
              </button>
            )}
          </div>

          <p
            className={`
              mt-1.5
              text-sm
              leading-5
              ${presentation.messageClassName}
            `}
          >
            {message ||
              defaultMessage}
          </p>

          {fileName &&
            kind !== "authentication" && (
              <p
                className="
                  mt-2
                  truncate
                  text-xs
                  text-slate-400
                "
                title={fileName}
              >
                File:{" "}
                <span className="text-slate-300">
                  {fileName}
                </span>
              </p>
            )}

          {showSupportedTypes && (
            <div
              className="
                mt-3
                flex
                flex-wrap
                items-center
                gap-x-2
                gap-y-1
                text-xs
              "
            >
              <span
                className={
                  presentation.supportedTypesClassName
                }
              >
                Supported formats:
              </span>

              <span
                className="
                  font-semibold
                  text-slate-300
                "
              >
                {allowedTypes.join(", ")}
              </span>
            </div>
          )}

          {(onRetry ||
            onChooseAnother) && (
            <div
              className="
                mt-4
                flex
                flex-wrap
                gap-2
              "
            >
              {onRetry && (
                <button
                  type="button"
                  onClick={onRetry}
                  className="
                    inline-flex
                    min-h-9
                    items-center
                    gap-2
                    rounded-lg
                    bg-white/10
                    px-3.5
                    py-2
                    text-xs
                    font-semibold
                    text-white
                    transition
                    hover:bg-white/15
                    focus:outline-none
                    focus-visible:ring-2
                    focus-visible:ring-[#C6A15B]
                  "
                >
                  <RefreshCw
                    size={14}
                    aria-hidden="true"
                  />
                  Try Again
                </button>
              )}

              {onChooseAnother && (
                <button
                  type="button"
                  onClick={onChooseAnother}
                  className="
                    inline-flex
                    min-h-9
                    items-center
                    gap-2
                    rounded-lg
                    bg-[#C6A15B]
                    px-3.5
                    py-2
                    text-xs
                    font-semibold
                    text-black
                    transition
                    hover:bg-[#A8894D]
                    focus:outline-none
                    focus-visible:ring-2
                    focus-visible:ring-[#C6A15B]
                  "
                >
                  <Upload
                    size={14}
                    aria-hidden="true"
                  />
                  Choose Another File
                </button>
              )}
            </div>
          )}
        </div>
      </div>

      {kind === "server" && (
        <div
          className="
            mt-4
            flex
            items-start
            gap-2
            rounded-lg
            border
            border-orange-400/10
            bg-black/10
            p-3
            text-xs
            text-slate-400
          "
        >
          <ShieldAlert
            size={15}
            className="
              mt-0.5
              shrink-0
              text-orange-400
            "
            aria-hidden="true"
          />

          <p>
            Your file type may be valid. The
            upload service encountered a problem
            while processing the document.
          </p>
        </div>
      )}
    </div>
  );
}

