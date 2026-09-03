import {
  useState,
  type ChangeEvent,
  type DragEvent,
} from "react";

import {
  UploadCloud,
  FileText,
  X,
} from "lucide-react";

import Button from "../../../components/common/Button";

import UploadProgressBar from "./UploadProgressBar";

import FileTypeError from "./FileTypeError";


interface Props {

  onUpload: (
    file: File,
    onProgress?: (
      progress: number
    ) => void
  ) => Promise<void>;

}


const MAX_FILE_SIZE =
  10 * 1024 * 1024;


const ALLOWED_TYPES = [
  "application/pdf",
  "image/png",
  "image/jpeg",
];


const extractUploadError = (
  error: unknown
): string => {

  if (
    error &&
    typeof error === "object"
  ) {

    const axiosError =
      error as {
        response?: {
          status?: number;

          data?: {
            message?: string;

            error?: string;

            detail?: string;
          };
        };

        message?: string;
      };


    const status =
      axiosError.response?.status;


    const message =
      axiosError.response?.data?.message
      ||
      axiosError.response?.data?.detail
      ||
      axiosError.response?.data?.error;


    /**
     * HTTP 500 is a server-side problem.
     *
     * Do NOT describe it as a file-type problem.
     */
    if (
      status &&
      status >= 500
    ) {

      return (
        message ||
        "The server could not process this document. Please try again."
      );

    }


    if (message) {

      return message;

    }


    if (
      typeof axiosError.message ===
      "string"
    ) {

      return axiosError.message;

    }

  }


  if (
    error instanceof Error
  ) {

    return error.message;

  }


  return (
    "Upload failed. Please try again."
  );

};


export default function UploadBox({
  onUpload,
}: Props) {

  const [
    file,
    setFile,
  ] = useState<File | null>(null);


  const [
    loading,
    setLoading,
  ] = useState(false);


  const [
    progress,
    setProgress,
  ] = useState(0);


  const [
    error,
    setError,
  ] = useState("");


  const [
    dragActive,
    setDragActive,
  ] = useState(false);


  const validateFile = (
    selected: File
  ): string => {

    if (
      !ALLOWED_TYPES.includes(
        selected.type
      )
    ) {

      return (
        "Unsupported file type. Please select a PDF, PNG, or JPEG file."
      );

    }


    if (
      selected.size <= 0
    ) {

      return (
        "The selected file is empty."
      );

    }


    if (
      selected.size >
      MAX_FILE_SIZE
    ) {

      return (
        "File size must be 10MB or smaller."
      );

    }


    return "";

  };


  const selectFile = (
    selected: File | undefined
  ) => {

    if (!selected) {
      return;
    }


    const validation =
      validateFile(
        selected
      );


    if (validation) {

      setError(
        validation
      );

      setFile(null);

      return;

    }


    setError("");

    setProgress(0);

    setFile(selected);

  };


  const handleFileChange = (
    event: ChangeEvent<HTMLInputElement>
  ) => {

    selectFile(
      event.target.files?.[0]
    );

    /**
     * Allow the user to select the same file again after an error.
     */
    event.target.value = "";

  };


  const handleDragOver = (
    event: DragEvent<HTMLDivElement>
  ) => {

    event.preventDefault();

    event.dataTransfer.dropEffect =
      "copy";

    setDragActive(true);

  };


  const handleDragLeave = () => {

    setDragActive(false);

  };


  const handleDrop = (
    event: DragEvent<HTMLDivElement>
  ) => {

    event.preventDefault();

    setDragActive(false);


    selectFile(
      event.dataTransfer.files?.[0]
    );

  };


  const submit = async () => {

    if (!file || loading) {
      return;
    }


    try {

      setLoading(true);

      setError("");

      setProgress(0);


      await onUpload(
        file,
        (
          value: number
        ) => {

          const safeProgress =
            Math.min(
              100,
              Math.max(
                0,
                Math.round(value)
              )
            );


          setProgress(
            safeProgress
          );

        }
      );


      setProgress(100);

      setFile(null);


    } catch (error: unknown) {

      setError(
        extractUploadError(
          error
        )
      );

    } finally {

      setLoading(false);

    }

  };


  return (

    <div
      className="
        rounded-3xl
        border
        border-[#E5DED1]
        bg-white
        p-8
        shadow-sm
      "
    >

      <div
        onDragOver={
          handleDragOver
        }
        onDragLeave={
          handleDragLeave
        }
        onDrop={
          handleDrop
        }
        className={`
          rounded-2xl
          border-2
          border-dashed
          p-12
          text-center
          transition

          ${
            dragActive
              ? "border-[#F4B81A] bg-yellow-50"
              : "border-[#D9DDE5]"
          }
        `}
      >

        <UploadCloud
          size={50}
          className="
            mx-auto
            text-[#F4B81A]
          "
        />


        <h3
          className="
            mt-5
            text-xl
            font-bold
            text-[#0B1736]
          "
        >
          Upload Document
        </h3>


        <p
          className="
            mt-2
            text-[#7D8CA3]
          "
        >
          Drag & drop or select PDF,
          passport, visa or image files
        </p>


        <input
          type="file"
          accept=".pdf,.png,.jpg,.jpeg,application/pdf,image/png,image/jpeg"
          className="hidden"
          id="document-upload"
          onChange={
            handleFileChange
          }
          disabled={loading}
        />


        <label
          htmlFor="document-upload"
          className="
            inline-block
            mt-6
            cursor-pointer
            rounded-xl
            bg-[#0B1736]
            px-5
            py-3
            font-medium
            text-white
            hover:opacity-90
            has-[:disabled]:cursor-not-allowed
          "
        >
          Choose File
        </label>


        {file && (

          <div
            className="
              mt-6
              flex
              items-center
              justify-center
              gap-3
              text-[#0B1736]
            "
          >

            <FileText
              size={18}
            />

            <span
              className="
                max-w-xs
                truncate
              "
            >
              {file.name}
            </span>


            <button
              type="button"
              onClick={() => {

                if (!loading) {

                  setFile(null);

                  setError("");

                  setProgress(0);

                }

              }}
              disabled={loading}
              className="
                text-red-500
                disabled:cursor-not-allowed
                disabled:opacity-50
              "
              aria-label="Remove selected file"
            >

              <X
                size={18}
              />

            </button>

          </div>

        )}


        {error && (

          <div
            className="mt-5"
          >

            <FileTypeError
              message={error}
              allowedTypes={[
                "PDF",
                "PNG",
                "JPEG",
              ]}
            />

          </div>

        )}


        {loading && file && (

          <div
            className="mt-6"
          >

            <UploadProgressBar
              fileName={
                file.name
              }
              progress={
                progress
              }
              status="uploading"
            />

          </div>

        )}

      </div>


      <Button
        loading={loading}
        disabled={
          !file ||
          loading
        }
        className="
          mt-6
          w-full
        "
        onClick={
          submit
        }
      >
        Upload Document
      </Button>

    </div>

  );

}