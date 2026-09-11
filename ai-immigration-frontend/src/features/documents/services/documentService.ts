import {
  uploadDocumentApi,
  getDocumentsApi,
  deleteDocumentApi,
} from "../../../api/documentApi";

import type {
  Document,
  DocumentUploadRequest,
} from "../../../types/document";

import {
  errorService,
} from "../../../services/errorService";

/**
 * ============================================================================
 * DOCUMENT SERVICE
 * ============================================================================
 *
 * Application service layer for immigration document operations.
 *
 * Architecture:
 *
 * Component
 *    ↓
 * useDocuments
 *    ↓
 * documentService
 *    ↓
 * documentApi
 *    ↓
 * Axios
 *    ↓
 * Spring Boot
 *
 * Responsibilities:
 *
 * - Validate service-layer inputs
 * - Normalize upload requests
 * - Delegate HTTP operations to documentApi
 * - Validate API responses
 * - Log errors consistently
 * - Preserve original errors for centralized Axios/error handling
 *
 * IMPORTANT:
 *
 * The backend is authoritative for the authenticated user.
 *
 * The current Spring Boot DocumentController resolves the user ID from:
 *
 *     SecurityContextHolder
 *
 * Therefore:
 *
 *     POST /api/documents/upload
 *
 * does NOT require a userId request parameter.
 *
 * Likewise:
 *
 *     GET /api/documents
 *
 * returns documents belonging to the authenticated user.
 *
 * The frontend must NEVER attempt to override the authenticated user.
 *
 * ============================================================================
 */


/**
 * ============================================================================
 * CONSTANTS
 * ============================================================================
 */

const DOCUMENT_UPLOAD_DATA_REQUIRED =
  "Document upload data is required.";

const DOCUMENT_FILE_REQUIRED =
  "A valid document file is required.";

const DOCUMENT_TYPE_REQUIRED =
  "Document type is required.";

const INVALID_USER_ID =
  "Invalid authenticated user ID.";

const INVALID_DOCUMENT_ID =
  "Invalid document ID.";

const INVALID_DOCUMENT_RESPONSE =
  "The document service received an invalid document response.";

const INVALID_DOCUMENTS_RESPONSE =
  "The document service received an invalid documents response.";


/**
 * ============================================================================
 * VALIDATION
 * ============================================================================
 */


/**
 * Validate a user ID when the calling layer provides one.
 *
 * NOTE:
 *
 * The current backend does not require the frontend to send userId.
 * This helper is retained for compatibility with existing consumers and
 * future service-layer authentication validation.
 */
const validateUserId = (
  userId: number
): void => {

  if (
    !Number.isInteger(userId) ||
    userId <= 0
  ) {

    throw new Error(
      INVALID_USER_ID
    );

  }

};


/**
 * Validate a document ID.
 */
const validateDocumentId = (
  id: number
): void => {

  if (
    !Number.isInteger(id) ||
    id <= 0
  ) {

    throw new Error(
      INVALID_DOCUMENT_ID
    );

  }

};


/**
 * Validate the uploaded file.
 *
 * Detailed file validation is also performed by documentApi.
 *
 * This service-level validation provides an early failure before the request
 * reaches the API layer.
 */
const validateUploadFile = (
  file: File
): void => {

  if (
    !file
  ) {

    throw new Error(
      DOCUMENT_FILE_REQUIRED
    );

  }


  if (
    !(file instanceof File)
  ) {

    throw new Error(
      DOCUMENT_FILE_REQUIRED
    );

  }


  if (
    file.size <= 0
  ) {

    throw new Error(
      "The selected document file is empty."
    );

  }

};


/**
 * Validate document upload data.
 */
const validateUploadData = (
  data: DocumentUploadRequest
): void => {

  if (
    !data
  ) {

    throw new Error(
      DOCUMENT_UPLOAD_DATA_REQUIRED
    );

  }


  validateUploadFile(
    data.file
  );


  if (
    !data.documentType ||
    !data.documentType.trim()
  ) {

    throw new Error(
      DOCUMENT_TYPE_REQUIRED
    );

  }

};


/**
 * ============================================================================
 * UPLOAD DOCUMENT
 * ============================================================================
 *
 * Backend:
 *
 * POST /api/documents/upload
 *
 * Content-Type:
 *
 * multipart/form-data
 *
 * Fields:
 *
 *     file
 *     documentType
 *
 * IMPORTANT:
 *
 * userId is intentionally NOT appended here.
 *
 * The backend resolves the authenticated user from the JWT/Spring Security
 * context:
 *
 *     SecurityContextHolder
 *
 * ============================================================================
 */

export const uploadDocument = async (
  data: DocumentUploadRequest,
  onProgress?: (
    progress: number
  ) => void
): Promise<Document> => {

  try {

    /**
     * ---------------------------------------------------------------
     * Validate request
     * ---------------------------------------------------------------
     */

    validateUploadData(
      data
    );


    /**
     * ---------------------------------------------------------------
     * Normalize request
     * ---------------------------------------------------------------
     *
     * Only the fields required by the backend are forwarded.
     *
     * userId is deliberately omitted because the backend determines
     * the authenticated user.
     *
     * onProgress is also not part of the HTTP payload.
     */

    const uploadRequest:
      DocumentUploadRequest = {

      file:
        data.file,

      documentType:
        data.documentType.trim(),

    };


    /**
     * ---------------------------------------------------------------
     * Upload
     * ---------------------------------------------------------------
     */

    const response =
      await uploadDocumentApi(
        uploadRequest,
        onProgress
      );


    /**
     * ---------------------------------------------------------------
     * Validate response
     * ---------------------------------------------------------------
     */

    if (
      !response ||
      typeof response !== "object"
    ) {

      throw new Error(
        INVALID_DOCUMENT_RESPONSE
      );

    }


    /**
     * Basic document identity validation.
     *
     * This prevents malformed API responses from silently entering
     * application state.
     */

    if (
      !Number.isInteger(response.id) ||
      response.id <= 0
    ) {

      throw new Error(
        INVALID_DOCUMENT_RESPONSE
      );

    }


    if (
      typeof response.fileName !== "string"
    ) {

      throw new Error(
        INVALID_DOCUMENT_RESPONSE
      );

    }


    if (
      typeof response.documentType !== "string"
    ) {

      throw new Error(
        INVALID_DOCUMENT_RESPONSE
      );

    }


    return response;


  } catch (
    error: unknown
  ) {

    /**
     * Log consistently.
     *
     * The original error is deliberately re-thrown so that:
     *
     * - Axios interceptors
     * - useDocuments
     * - UploadBox
     * - global error handling
     *
     * can still process the original error.
     */

    errorService.log(
      error,
      "Document Service - Upload"
    );


    throw error;

  }

};


/**
 * ============================================================================
 * FETCH CURRENT USER DOCUMENTS
 * ============================================================================
 *
 * Backend:
 *
 * GET /api/documents
 *
 * IMPORTANT:
 *
 * The current backend controller does NOT accept:
 *
 *     ?userId=1
 *
 * Instead it determines the authenticated user from Spring Security.
 *
 * Therefore the frontend calls:
 *
 *     GET /api/documents
 *
 * and NOT:
 *
 *     GET /api/documents?userId=1
 *
 * This prevents a client from attempting to request another user's
 * documents.
 *
 * ============================================================================
 */

export const getUserDocuments = async (
  userId?: number
): Promise<Document[]> => {

  try {

    /**
     * ---------------------------------------------------------------
     * Optional compatibility validation
     * ---------------------------------------------------------------
     *
     * Existing callers may still pass userId.
     *
     * We validate it when supplied, but we DO NOT send it to the backend.
     *
     * The backend remains the source of truth for authorization.
     */

    if (
      userId !== undefined &&
      userId !== null
    ) {

      validateUserId(
        Number(userId)
      );

    }


    /**
     * ---------------------------------------------------------------
     * Fetch documents
     * ---------------------------------------------------------------
     *
     * documentApi is responsible for calling:
     *
     *     GET /documents
     *
     * without a userId query parameter.
     */

    const documents =
      await getDocumentsApi();


    /**
     * ---------------------------------------------------------------
     * Validate response
     * ---------------------------------------------------------------
     */

    if (
      !Array.isArray(documents)
    ) {

      throw new Error(
        INVALID_DOCUMENTS_RESPONSE
      );

    }


    return documents;


  } catch (
    error: unknown
  ) {

    errorService.log(
      error,
      "Document Service - Fetch"
    );


    throw error;

  }

};


/**
 * ============================================================================
 * DELETE DOCUMENT
 * ============================================================================
 *
 * Backend:
 *
 * DELETE /api/documents/{documentId}
 *
 * The backend verifies that the document belongs to the authenticated user
 * before deleting it.
 *
 * ============================================================================
 */

export const removeDocument = async (
  id: number
): Promise<void> => {

  try {

    /**
     * ---------------------------------------------------------------
     * Validate document ID
     * ---------------------------------------------------------------
     */

    validateDocumentId(
      id
    );


    /**
     * ---------------------------------------------------------------
     * Delete
     * ---------------------------------------------------------------
     */

    await deleteDocumentApi(
      id
    );


  } catch (
    error: unknown
  ) {

    errorService.log(
      error,
      "Document Service - Delete"
    );


    throw error;

  }

};


/**
 * ============================================================================
 * DEFAULT EXPORT
 * ============================================================================
 *
 * Named exports are preferred for this service because they make dependencies
 * explicit and improve tree-shaking.
 *
 * No default export is required.
 *
 * ============================================================================
 */

