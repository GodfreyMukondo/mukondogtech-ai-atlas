import API from "./axios";

import type {
  AxiosProgressEvent,
  AxiosRequestConfig,
} from "axios";

import type {
  Document,
  DocumentUploadRequest,
} from "../types/document";

/**
 * ============================================================================
 * DOCUMENT API CLIENT
 * ============================================================================
 *
 * Centralized HTTP client for authenticated document operations.
 *
 * Authentication:
 * ----------------------------------------------------------------------------
 * Authentication is handled centrally by ./axios.
 *
 * The frontend MUST NOT send:
 *
 *     userId
 *
 * The backend resolves the authenticated user from the JWT.
 *
 * Endpoints:
 * ----------------------------------------------------------------------------
 *
 * GET    /api/documents
 * GET    /api/documents/{documentId}
 * POST   /api/documents/upload
 * DELETE /api/documents/{documentId}
 *
 * Upload request:
 * ----------------------------------------------------------------------------
 *
 * Content-Type:
 *
 *     multipart/form-data
 *
 * Multipart fields:
 *
 *     file
 *     documentType
 *
 * ============================================================================
 */


/**
 * ============================================================================
 * CONSTANTS
 * ============================================================================
 */

const DOCUMENT_API_BASE_PATH = "/documents";

const DOCUMENT_UPLOAD_PATH =
  `${DOCUMENT_API_BASE_PATH}/upload`;

const MAX_FILE_SIZE_BYTES =
  10 * 1024 * 1024;

const MAX_FILE_SIZE_MB = 10;

const ALLOWED_FILE_TYPES =
  new Set<string>([
    "application/pdf",
    "image/png",
    "image/jpeg",
  ]);


/**
 * ============================================================================
 * API RESPONSE CONTRACT
 * ============================================================================
 *
 * Supports both:
 *
 *     {
 *       "data": {...},
 *       "message": "...",
 *       "success": true
 *     }
 *
 * and direct responses:
 *
 *     {...}
 *
 * This allows the frontend to remain compatible with either response style.
 * ============================================================================
 */

interface ApiResponse<T> {
  data: T;
  message?: string;
  success?: boolean;
}


/**
 * ============================================================================
 * UNKNOWN OBJECT TYPE
 * ============================================================================
 */

type UnknownRecord =
  Record<string, unknown>;


/**
 * ============================================================================
 * RESPONSE UNWRAPPER
 * ============================================================================
 *
 * Safely unwraps the API envelope when one exists.
 * ============================================================================
 */

const unwrapApiResponse = <T>(
  responseData: ApiResponse<T> | T,
): T => {

  if (
    responseData !== null &&
    typeof responseData === "object" &&
    "data" in responseData
  ) {

    return (
      responseData as ApiResponse<T>
    ).data;
  }

  return responseData as T;
};


/**
 * ============================================================================
 * DOCUMENT OBJECT VALIDATION
 * ============================================================================
 *
 * Performs minimal runtime validation.
 *
 * The TypeScript compiler protects compile-time usage, but HTTP responses
 * originate outside the application and therefore must be validated at
 * runtime as well.
 * ============================================================================
 */

const isDocument = (
  value: unknown,
): value is Document => {

  if (
    value === null ||
    typeof value !== "object"
  ) {

    return false;
  }


  const record =
    value as UnknownRecord;


  return (
    typeof record.id === "number" &&
    Number.isInteger(record.id) &&
    record.id > 0
  );
};


/**
 * ============================================================================
 * UPLOAD RESPONSE NORMALIZATION
 * ============================================================================
 *
 * POST /api/documents/upload does NOT return the same shape as
 * GET /api/documents and GET /api/documents/{id}.
 *
 * Backend contract:
 *
 *     GET  /api/documents        -> DocumentResponse   { id, ..., status }
 *     GET  /api/documents/{id}   -> DocumentResponse   { id, ..., status }
 *     POST /api/documents/upload -> DocumentUploadResponse
 *                                    { documentId, ..., uploadStatus }
 *
 * DocumentUploadResponse intentionally excludes fields such as fileSize,
 * mimeType, and updatedAt (see the Java DTO for why).
 *
 * The frontend Document type mirrors DocumentResponse (id, status), so the
 * upload response must be translated into that shape before it can be
 * validated with isDocument() or merged into application state.
 * ============================================================================
 */

const normalizeUploadResponse = (
  value: unknown,
): unknown => {

  if (
    value === null ||
    typeof value !== "object"
  ) {

    return value;
  }


  const record =
    value as UnknownRecord;


  /**
   * Already shaped like a Document (id/status present).
   *
   * Nothing to translate.
   */
  if (
    typeof record.id === "number"
  ) {

    return record;
  }


  /**
   * Translate DocumentUploadResponse -> Document.
   */
  if (
    typeof record.documentId === "number"
  ) {

    return {
      ...record,
      id: record.documentId,
      status:
        typeof record.uploadStatus === "string"
          ? record.uploadStatus
          : record.status,
    };
  }


  return record;
};


/**
 * ============================================================================
 * DOCUMENT ARRAY VALIDATION
 * ============================================================================
 */

const isDocumentArray = (
  value: unknown,
): value is Document[] => {

  return (
    Array.isArray(value) &&
    value.every(
      isDocument,
    )
  );
};


/**
 * ============================================================================
 * DOCUMENT ID VALIDATION
 * ============================================================================
 */

const validateDocumentId = (
  id: number,
): void => {

  if (
    typeof id !== "number" ||
    !Number.isInteger(id) ||
    id <= 0
  ) {

    throw new Error(
      "Invalid document ID.",
    );
  }
};


/**
 * ============================================================================
 * FILE VALIDATION
 * ============================================================================
 */

const validateFile = (
  file: File,
): void => {

  if (!file) {

    throw new Error(
      "A document file is required.",
    );
  }


  /**
   * Runtime File validation.
   *
   * The typeof File guard is important for environments where File may not
   * exist, such as certain SSR/test environments.
   */
  if (
    typeof File !== "undefined" &&
    !(file instanceof File)
  ) {

    throw new Error(
      "The selected file is invalid.",
    );
  }


  if (
    typeof file.size !== "number" ||
    file.size <= 0
  ) {

    throw new Error(
      "The selected file is empty.",
    );
  }


  if (
    file.size >
    MAX_FILE_SIZE_BYTES
  ) {

    throw new Error(
      `File size must be ${MAX_FILE_SIZE_MB}MB or smaller.`,
    );
  }


  /**
   * MIME type validation.
   *
   * Some browsers can provide an empty MIME type.
   *
   * Therefore:
   *
   *     empty type -> allow
   *     known invalid type -> reject
   *
   * The backend remains the final security authority.
   */
  const mimeType =
    file.type?.trim().toLowerCase();


  if (
    mimeType &&
    !ALLOWED_FILE_TYPES.has(
      mimeType,
    )
  ) {

    throw new Error(
      "Unsupported file type. Please upload a PDF, PNG, or JPEG file.",
    );
  }
};


/**
 * ============================================================================
 * UPLOAD REQUEST VALIDATION
 * ============================================================================
 */

const validateUploadRequest = (
  data: DocumentUploadRequest,
): void => {

  if (!data) {

    throw new Error(
      "Document upload data is required.",
    );
  }


  if (!data.file) {

    throw new Error(
      "A document file is required.",
    );
  }


  validateFile(
    data.file,
  );


  if (
    typeof data.documentType !== "string"
  ) {

    throw new Error(
      "Document type is required.",
    );
  }


  const documentType =
    data.documentType.trim();


  if (!documentType) {

    throw new Error(
      "Document type is required.",
    );
  }


  /**
   * Prevent accidental excessively large multipart field values.
   *
   * This is not a replacement for backend validation.
   */
  if (documentType.length > 100) {

    throw new Error(
      "Document type is too long.",
    );
  }
};


/**
 * ============================================================================
 * UPLOAD PROGRESS
 * ============================================================================
 */

const calculateUploadProgress = (
  event: AxiosProgressEvent,
): number | null => {

  if (
    typeof event.loaded !== "number"
  ) {

    return null;
  }


  if (
    typeof event.total !== "number" ||
    event.total <= 0
  ) {

    return null;
  }


  const percentage =
    Math.round(
      (
        event.loaded /
        event.total
      ) *
      100,
    );


  return Math.min(
    100,
    Math.max(
      0,
      percentage,
    ),
  );
};


/**
 * ============================================================================
 * NORMALIZE UPLOAD PROGRESS CALLBACK
 * ============================================================================
 */

const notifyUploadProgress = (
  onProgress: (
    (progress: number) => void
  ) | undefined,
  progress: number,
): void => {

  if (!onProgress) {
    return;
  }


  const normalizedProgress =
    Math.min(
      100,
      Math.max(
        0,
        Math.round(progress),
      ),
    );


  onProgress(
    normalizedProgress,
  );
};


/**
 * ============================================================================
 * CREATE UPLOAD FORM DATA
 * ============================================================================
 *
 * IMPORTANT:
 *
 * Do not append userId.
 *
 * The authenticated user is determined by Spring Security from the JWT.
 * ============================================================================
 */

const createUploadFormData = (
  data: DocumentUploadRequest,
): FormData => {

  const formData =
    new FormData();


  formData.append(
    "file",
    data.file,
    data.file.name,
  );


  formData.append(
    "documentType",
    data.documentType.trim(),
  );


  return formData;
};


/**
 * ============================================================================
 * UPLOAD DOCUMENT
 * ============================================================================
 *
 * POST /api/documents/upload
 *
 * Request:
 *
 *     multipart/form-data
 *
 * Fields:
 *
 *     file
 *     documentType
 *
 * IMPORTANT:
 * ----------------------------------------------------------------------------
 *
 * Do NOT send JSON.
 *
 * Do NOT do this:
 *
 *     API.post("/documents/upload", {
 *       file,
 *       documentType
 *     });
 *
 * That produces:
 *
 *     Content-Type: application/json
 *
 * which is exactly the error currently shown by Spring:
 *
 *     HttpMediaTypeNotSupportedException
 *
 * Instead we send FormData.
 *
 * Also:
 *
 *     DO NOT manually set
 *
 *     Content-Type: multipart/form-data
 *
 * in browser code.
 *
 * Axios/browser must generate the multipart boundary automatically.
 * ============================================================================
 */

export const uploadDocumentApi = async (
  data: DocumentUploadRequest,
  onProgress?: (
    progress: number,
  ) => void,
  config?: AxiosRequestConfig,
): Promise<Document> => {

  validateUploadRequest(
    data,
  );


  const formData =
    createUploadFormData(
      data,
    );


  notifyUploadProgress(
    onProgress,
    0,
  );


  const response =
    await API.post<
      ApiResponse<Document> | Document
    >(
      DOCUMENT_UPLOAD_PATH,
      formData,
      {
        ...config,

        /**
         * IMPORTANT:
         *
         * The shared `API` axios instance (./axios.ts) sets a hard
         * "Content-Type: application/json" default header for every
         * request.
         *
         * Axios's own transformRequest only skips JSON-encoding a
         * FormData payload when it does NOT see a JSON content type
         * already present. Since our instance always has one, axios
         * would otherwise silently JSON.stringify() this FormData
         * (discarding the file) while still sending
         * "Content-Type: application/json" — which is exactly what
         * produced:
         *
         *     HttpMediaTypeNotSupportedException:
         *     Content-Type 'application/json' is not supported
         *
         * Explicitly clearing it here (not deleting the key, but
         * setting it to undefined) removes the instance default for
         * this request only, so axios/the browser can set the correct
         * multipart/form-data boundary automatically.
         *
         * Do NOT manually set "multipart/form-data" as the value —
         * only the browser can generate a valid boundary.
         */
        headers: {
          ...config?.headers,
          "Content-Type": undefined,
        },

        onUploadProgress: (
          event: AxiosProgressEvent,
        ) => {

          const progress =
            calculateUploadProgress(
              event,
            );


          if (
            progress !== null
          ) {

            notifyUploadProgress(
              onProgress,
              progress,
            );
          }
        },
      },
    );


  const document =
    normalizeUploadResponse(
      unwrapApiResponse<unknown>(
        response.data,
      ),
    );


  if (
    !isDocument(document)
  ) {

    throw new Error(
      "The document upload succeeded, but the server returned an invalid document response.",
    );
  }


  notifyUploadProgress(
    onProgress,
    100,
  );


  return document;
};


/**
 * ============================================================================
 * GET CURRENT USER DOCUMENTS
 * ============================================================================
 *
 * GET /api/documents
 *
 * The backend obtains the authenticated user from the JWT.
 *
 * No userId query parameter is permitted.
 * ============================================================================
 */

export const getDocumentsApi =
  async (): Promise<Document[]> => {

    const response =
      await API.get<
        ApiResponse<Document[]> |
        Document[]
      >(
        DOCUMENT_API_BASE_PATH,
      );


    const documents =
      unwrapApiResponse<Document[]>(
        response.data,
      );


    if (
      !isDocumentArray(
        documents,
      )
    ) {

      throw new Error(
        "The server returned an invalid documents response.",
      );
    }


    return documents;
  };


/**
 * ============================================================================
 * GET SINGLE DOCUMENT
 * ============================================================================
 *
 * GET /api/documents/{documentId}
 *
 * Ownership is enforced by the backend using:
 *
 *     authenticatedUserId
 *     documentId
 * ============================================================================
 */

export const getDocumentApi = async (
  id: number,
): Promise<Document> => {

  validateDocumentId(
    id,
  );


  const response =
    await API.get<
      ApiResponse<Document> |
      Document
    >(
      `${DOCUMENT_API_BASE_PATH}/${id}`,
    );


  const document =
    unwrapApiResponse<Document>(
      response.data,
    );


  if (
    !isDocument(document)
  ) {

    throw new Error(
      "The server returned an invalid document response.",
    );
  }


  return document;
};


/**
 * ============================================================================
 * DELETE DOCUMENT
 * ============================================================================
 *
 * DELETE /api/documents/{documentId}
 *
 * Ownership is enforced by the backend.
 * ============================================================================
 */

export const deleteDocumentApi =
  async (
    id: number,
  ): Promise<void> => {

    validateDocumentId(
      id,
    );


    await API.delete(
      `${DOCUMENT_API_BASE_PATH}/${id}`,
    );
  };


/**
 * ============================================================================
 * DEFAULT API OBJECT
 * ============================================================================
 *
 * Keeping a default object makes the service convenient to consume while the
 * named exports above remain tree-shakable and testable.
 * ============================================================================
 */

const documentApi = {
  uploadDocument: uploadDocumentApi,
  getDocuments: getDocumentsApi,
  getDocument: getDocumentApi,
  deleteDocument: deleteDocumentApi,
};


export default documentApi;