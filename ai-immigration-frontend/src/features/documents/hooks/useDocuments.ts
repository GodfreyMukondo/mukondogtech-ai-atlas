import {
  useCallback,
  useEffect,
  useRef,
  useState,
} from "react";

import {
  getUserDocuments,
  uploadDocument,
  removeDocument,
} from "../services/documentService";

import type {
  Document,
  DocumentUploadRequest,
} from "../../../types/document";

import { errorService } from "../../../services/errorService";
import { notificationService } from "../../../services/notificationService";

import { useAuth } from "../../../context/AuthContext";

/**
 * ============================================================================
 * DOCUMENTS HOOK
 * ============================================================================
 *
 * Centralized React hook for authenticated document management.
 *
 * Responsibilities:
 *
 * - Load authenticated user's documents
 * - Upload documents
 * - Delete documents
 * - Protect against duplicate requests
 * - Handle authentication state
 * - Prevent stale state updates after unmount
 * - Normalize user/document IDs
 * - Provide consistent application errors
 * - Refresh document state after successful uploads
 *
 * IMPORTANT SECURITY RULE
 * -----------------------
 *
 * The frontend MUST NOT be responsible for deciding which user's documents
 * are returned by the backend.
 *
 * The backend resolves the authenticated user from the JWT/SecurityContext.
 *
 * Therefore:
 *
 *     GET    /api/documents
 *     POST   /api/documents/upload
 *     DELETE /api/documents/{id}
 *
 * are preferred over:
 *
 *     GET /api/documents?userId=1
 *     POST /api/documents/upload + userId
 *
 * The authenticated user ID is used locally only for:
 *
 * - authentication-state tracking
 * - preventing stale responses
 * - determining whether the current user changed
 *
 * It is NOT sent as an authorization parameter to the API.
 *
 * ============================================================================
 */

/**
 * ============================================================================
 * PUBLIC HOOK API
 * ============================================================================
 */

export interface UseDocumentsReturn {
  documents: Document[];

  loading: boolean;

  refreshing: boolean;

  error: string | null;

  fetchDocuments: () => Promise<void>;

  upload: (
    data: DocumentUploadRequest
  ) => Promise<Document | null>;

  remove: (
    id: number
  ) => Promise<void>;
}

/**
 * ============================================================================
 * CONSTANTS
 * ============================================================================
 */

const INVALID_USER_ID_MESSAGE =
  "Unable to determine the authenticated user. Please sign in again.";

const INVALID_DOCUMENT_ID_MESSAGE =
  "Invalid document ID.";

const AUTHENTICATION_REQUIRED_MESSAGE =
  "You must be authenticated to manage documents.";

const DELETE_AUTHENTICATION_REQUIRED_MESSAGE =
  "You must be authenticated to delete a document.";

const INVALID_DOCUMENT_RESPONSE_MESSAGE =
  "The server returned an invalid documents response.";

const INVALID_UPLOAD_RESPONSE_MESSAGE =
  "The server returned an invalid document response.";

const DOCUMENT_UPLOAD_SUCCESS_MESSAGE =
  "Document uploaded successfully.";

const DOCUMENT_DELETE_SUCCESS_MESSAGE =
  "Document removed successfully.";

const DOCUMENT_REFRESH_ERROR_MESSAGE =
  "Document was uploaded successfully, but the document list could not be refreshed.";

/**
 * ============================================================================
 * ID RESOLUTION
 * ============================================================================
 *
 * AuthContext implementations sometimes expose IDs as:
 *
 *     number
 *
 * or:
 *
 *     string
 *
 * These helpers normalize them safely.
 */

/**
 * Resolve an authenticated user ID.
 */
function resolveUserId(
  value: unknown
): number | null {
  if (
    value === null ||
    value === undefined ||
    value === ""
  ) {
    return null;
  }

  const numericValue =
    typeof value === "number"
      ? value
      : Number(value);

  if (
    !Number.isInteger(numericValue) ||
    numericValue <= 0
  ) {
    return null;
  }

  return numericValue;
}

/**
 * Resolve a document ID.
 */
function resolveDocumentId(
  value: unknown
): number | null {
  if (
    value === null ||
    value === undefined ||
    value === ""
  ) {
    return null;
  }

  const numericValue =
    typeof value === "number"
      ? value
      : Number(value);

  if (
    !Number.isInteger(numericValue) ||
    numericValue <= 0
  ) {
    return null;
  }

  return numericValue;
}

/**
 * ============================================================================
 * RESPONSE VALIDATION
 * ============================================================================
 */

/**
 * Determine whether a value looks like a valid Document object.
 *
 * This intentionally performs structural validation rather than requiring
 * every optional backend field.
 */
function isValidDocument(
  value: unknown
): value is Document {
  if (
    value === null ||
    typeof value !== "object"
  ) {
    return false;
  }

  const document =
    value as Partial<Document>;

  return (
    typeof document.id === "number" &&
    Number.isInteger(document.id) &&
    document.id > 0 &&
    typeof document.fileName === "string" &&
    typeof document.documentType === "string" &&
    typeof document.uploadedAt === "string" &&
    typeof document.status === "string"
  );
}

/**
 * Validate a document collection returned by the API.
 */
function isValidDocumentArray(
  value: unknown
): value is Document[] {
  if (!Array.isArray(value)) {
    return false;
  }

  return value.every(
    isValidDocument
  );
}

/**
 * ============================================================================
 * HOOK
 * ============================================================================
 */

export function useDocuments(): UseDocumentsReturn {
  const {
    user,
    loading: authLoading,
    isAuthenticated,
  } = useAuth();

  /**
   * ==========================================================================
   * STATE
   * ==========================================================================
   */

  const [
    documents,
    setDocuments,
  ] = useState<Document[]>([]);

  const [
    loading,
    setLoading,
  ] = useState(false);

  const [
    refreshing,
    setRefreshing,
  ] = useState(false);

  const [
    error,
    setError,
  ] = useState<string | null>(null);

  /**
   * ==========================================================================
   * LIFECYCLE REFS
   * ==========================================================================
   */

  /**
   * Prevent state updates after unmount.
   */
  const mountedRef =
    useRef(false);

  /**
   * Prevent duplicate document-list requests.
   */
  const fetchInFlightRef =
    useRef(false);

  /**
   * Prevent duplicate upload requests.
   */
  const uploadInFlightRef =
    useRef(false);

  /**
   * Prevent duplicate delete requests for the same document.
   */
  const deletingIdsRef =
    useRef<Set<number>>(
      new Set()
    );

  /**
   * ID of the authenticated user whose documents are currently loaded.
   */
  const loadedUserIdRef =
    useRef<number | null>(null);

  /**
   * Current authenticated user ID.
   */
  const currentUserIdRef =
    useRef<number | null>(null);

  /**
   * Request generation.
   *
   * Every fetch receives a unique request number.
   *
   * This helps prevent an older request from overwriting state belonging
   * to a newer authentication session.
   */
  const fetchRequestIdRef =
    useRef(0);

  /**
   * ==========================================================================
   * MOUNT / UNMOUNT
   * ==========================================================================
   */

  useEffect(() => {
    mountedRef.current = true;

    /**
     * Do NOT bump fetchRequestIdRef here.
     *
     * In React 18 development Strict Mode, this effect's cleanup also
     * runs as part of the deliberate mount -> cleanup -> re-mount cycle
     * used to surface unsafe effects, even though the component never
     * actually unmounts. Incrementing the request ID during that
     * synthetic cleanup poisoned the still in-flight initial
     * fetchDocuments() request: by the time its response arrived,
     * fetchRequestIdRef no longer matched the request's captured
     * requestId, so the "stale response" guard below discarded a
     * perfectly valid result and left the document list empty.
     *
     * fetchRequestIdRef exists to ignore a genuinely superseded fetch
     * (e.g. a newer fetchDocuments() call for a different account) -
     * that invariant is already maintained inside fetchDocuments()
     * itself each time it starts a new request. mountedRef alone is
     * sufficient to avoid updating state after a real unmount.
     */
    return () => {
      mountedRef.current = false;
    };
  }, []);

  /**
   * ==========================================================================
   * AUTHENTICATED USER ID
   * ==========================================================================
   */

  const getAuthenticatedUserId =
    useCallback((): number | null => {
      return resolveUserId(
        user?.id
      );
    }, [user?.id]);

  /**
   * ==========================================================================
   * FETCH DOCUMENTS
   * ==========================================================================
   *
   * Backend endpoint:
   *
   *     GET /api/documents
   *
   * IMPORTANT:
   *
   * No userId query parameter is sent.
   *
   * The backend obtains the authenticated user from the JWT.
   */
  const fetchDocuments =
    useCallback(async (): Promise<void> => {
      /**
       * Authentication is still being restored.
       */
      if (authLoading) {
        return;
      }

      /**
       * User is not authenticated.
       */
      if (!isAuthenticated) {
        if (mountedRef.current) {
          setDocuments([]);
          setError(null);
          setLoading(false);
        }

        loadedUserIdRef.current = null;
        currentUserIdRef.current = null;

        return;
      }

      /**
       * Resolve authenticated user locally.
       *
       * This ID is NOT sent to the backend.
       */
      const userId =
        getAuthenticatedUserId();

      if (userId === null) {
        if (mountedRef.current) {
          setDocuments([]);
          setError(
            INVALID_USER_ID_MESSAGE
          );
          setLoading(false);
        }

        loadedUserIdRef.current = null;
        currentUserIdRef.current = null;

        return;
      }

      /**
       * Detect account changes.
       */
      if (
        currentUserIdRef.current !== null &&
        currentUserIdRef.current !== userId
      ) {
        if (mountedRef.current) {
          setDocuments([]);
          setError(null);
        }

        loadedUserIdRef.current = null;
      }

      currentUserIdRef.current =
        userId;

      /**
       * Prevent duplicate requests.
       */
      if (fetchInFlightRef.current) {
        return;
      }

      fetchInFlightRef.current =
        true;

      /**
       * Generate a request ID.
       */
      const requestId =
        fetchRequestIdRef.current + 1;

      fetchRequestIdRef.current =
        requestId;

      /**
       * A request for a user whose documents were already loaded once is a
       * manual refresh rather than the initial page load.
       */
      const isRefresh =
        loadedUserIdRef.current !== null;

      if (mountedRef.current) {
        setLoading(true);
        setError(null);

        if (isRefresh) {
          setRefreshing(true);
        }
      }

      try {
        /**
         * IMPORTANT:
         *
         * The service MUST call:
         *
         *     GET /documents
         *
         * and MUST NOT send:
         *
         *     ?userId=1
         */
        const response =
          await getUserDocuments();

        /**
         * Ignore stale/unmounted requests.
         */
        if (
          !mountedRef.current ||
          fetchRequestIdRef.current !== requestId
        ) {
          return;
        }

        /**
         * Validate response.
         */
        if (
          !isValidDocumentArray(
            response
          )
        ) {
          throw new Error(
            INVALID_DOCUMENT_RESPONSE_MESSAGE
          );
        }

        /**
         * Verify that the authentication context has not changed.
         */
        const latestUserId =
          getAuthenticatedUserId();

        if (
          latestUserId === null ||
          latestUserId !== userId
        ) {
          return;
        }

        /**
         * Update document state.
         */
        setDocuments(
          response
        );

        loadedUserIdRef.current =
          userId;

        setError(null);

      } catch (err: unknown) {
        if (
          !mountedRef.current ||
          fetchRequestIdRef.current !== requestId
        ) {
          return;
        }

        const appError =
          errorService.log(
            err,
            "Fetch Documents"
          );

        setError(
          appError.message
        );

        notificationService.error(
          appError.message
        );

      } finally {
        /**
         * Only the current request should release the in-flight lock.
         */
        if (
          fetchRequestIdRef.current === requestId
        ) {
          fetchInFlightRef.current =
            false;
        }

        if (mountedRef.current) {
          setLoading(false);
          setRefreshing(false);
        }
      }
    }, [
      authLoading,
      isAuthenticated,
      getAuthenticatedUserId,
    ]);

  /**
   * ==========================================================================
   * AUTOMATIC INITIAL LOAD
   * ==========================================================================
   */

  useEffect(() => {
    if (authLoading) {
      return;
    }

    /**
     * Authentication expired or user logged out.
     */
    if (!isAuthenticated) {
      if (mountedRef.current) {
        setDocuments([]);
        setError(null);
        setLoading(false);
      }

      loadedUserIdRef.current = null;
      currentUserIdRef.current = null;

      return;
    }

    const userId =
      getAuthenticatedUserId();

    /**
     * Authenticated state exists but no valid user ID is available.
     */
    if (userId === null) {
      if (mountedRef.current) {
        setDocuments([]);
        setError(
          INVALID_USER_ID_MESSAGE
        );
        setLoading(false);
      }

      loadedUserIdRef.current = null;
      currentUserIdRef.current = null;

      return;
    }

    /**
     * User account changed.
     *
     * Never display the previous user's document list while loading the
     * new user's data.
     */
    if (
      currentUserIdRef.current !== null &&
      currentUserIdRef.current !== userId
    ) {
      if (mountedRef.current) {
        setDocuments([]);
        setError(null);
      }

      loadedUserIdRef.current = null;
    }

    currentUserIdRef.current =
      userId;

    /**
     * Already loaded for this authenticated user.
     */
    if (
      loadedUserIdRef.current === userId
    ) {
      return;
    }

    void fetchDocuments();

  }, [
    authLoading,
    isAuthenticated,
    getAuthenticatedUserId,
    fetchDocuments,
  ]);

  /**
   * ==========================================================================
   * UPLOAD DOCUMENT
   * ==========================================================================
   *
   * Backend endpoint:
   *
   *     POST /api/documents/upload
   *
   * Multipart:
   *
   *     file
   *     documentType
   *
   * IMPORTANT:
   *
   * userId is intentionally NOT sent.
   *
   * The backend resolves the authenticated user from the JWT.
   */
  const upload =
    useCallback(async (
      data: DocumentUploadRequest
    ): Promise<Document | null> => {
      /**
       * Authentication must be ready.
       */
      if (
        authLoading ||
        !isAuthenticated
      ) {
        const message =
          AUTHENTICATION_REQUIRED_MESSAGE;

        if (mountedRef.current) {
          setError(message);
        }

        notificationService.error(
          message
        );

        return null;
      }

      /**
       * Resolve authenticated user.
       *
       * Used for local state protection only.
       */
      const userId =
        getAuthenticatedUserId();

      if (userId === null) {
        if (mountedRef.current) {
          setError(
            INVALID_USER_ID_MESSAGE
          );
        }

        notificationService.error(
          INVALID_USER_ID_MESSAGE
        );

        return null;
      }

      /**
       * Validate request object.
       */
      if (
        !data ||
        !data.file
      ) {
        const message =
          "Please select a document to upload.";

        if (mountedRef.current) {
          setError(message);
        }

        notificationService.error(
          message
        );

        return null;
      }

      /**
       * Validate document type.
       */
      if (
        typeof data.documentType !== "string" ||
        !data.documentType.trim()
      ) {
        const message =
          "Document type is required.";

        if (mountedRef.current) {
          setError(message);
        }

        notificationService.error(
          message
        );

        return null;
      }

      /**
       * Prevent duplicate upload submissions.
       */
      if (uploadInFlightRef.current) {
        return null;
      }

      uploadInFlightRef.current =
        true;

      if (mountedRef.current) {
        setLoading(true);
        setError(null);
      }

      try {
        /**
         * SECURITY:
         *
         * Do NOT pass userId to the backend.
         *
         * The backend determines the user from the authenticated JWT.
         *
         * We also intentionally remove any userId supplied by a component.
         */
        const uploadRequest:
          DocumentUploadRequest = {
            file: data.file,
            documentType:
              data.documentType.trim(),
            onProgress:
              data.onProgress,
          };

        const uploadedDocument =
          await uploadDocument(
            uploadRequest,
            data.onProgress
          );

        /**
         * Validate upload response.
         */
        if (
          !isValidDocument(
            uploadedDocument
          )
        ) {
          throw new Error(
            INVALID_UPLOAD_RESPONSE_MESSAGE
          );
        }

        /**
         * Component may have unmounted.
         *
         * The server operation has completed successfully, so return the
         * document even though local React state can no longer be updated.
         */
        if (!mountedRef.current) {
          return uploadedDocument;
        }

        /**
         * Make sure the authenticated account has not changed.
         */
        const latestUserId =
          getAuthenticatedUserId();

        if (
          latestUserId === null ||
          latestUserId !== userId
        ) {
          return uploadedDocument;
        }

        /**
         * Refresh document list.
         *
         * IMPORTANT:
         *
         * getUserDocuments() must call:
         *
         *     GET /api/documents
         *
         * without userId.
         */
        try {
          const refreshedDocuments =
            await getUserDocuments();

          if (
            mountedRef.current &&
            isValidDocumentArray(
              refreshedDocuments
            )
          ) {
            /**
             * Verify the account is still the same.
             */
            const refreshedUserId =
              getAuthenticatedUserId();

            if (
              refreshedUserId === userId
            ) {
              setDocuments(
                refreshedDocuments
              );

              loadedUserIdRef.current =
                userId;
            }
          } else if (
            mountedRef.current
          ) {
            errorService.log(
              new Error(
                INVALID_DOCUMENT_RESPONSE_MESSAGE
              ),
              "Refresh Documents After Upload"
            );
          }

        } catch (
          refreshError: unknown
        ) {
          /**
           * The upload succeeded.
           *
           * Therefore, do not report the entire upload operation as failed.
           */
          const refreshAppError =
            errorService.log(
              refreshError,
              "Refresh Documents After Upload"
            );

          if (mountedRef.current) {
            /**
             * Keep the successfully uploaded document visible immediately.
             *
             * This prevents the UI from appearing to lose the newly uploaded
             * document if the follow-up GET request fails.
             */
            setDocuments(
              previousDocuments => {
                const exists =
                  previousDocuments.some(
                    document =>
                      document.id ===
                      uploadedDocument.id
                  );

                if (exists) {
                  return previousDocuments;
                }

                return [
                  uploadedDocument,
                  ...previousDocuments,
                ];
              }
            );

            /**
             * Do not overwrite the upload success notification with a hard
             * failure. Keep the refresh problem available in application logs.
             */
            errorService.log(
              new Error(
                DOCUMENT_REFRESH_ERROR_MESSAGE
              ),
              `Refresh Documents: ${refreshAppError.message}`
            );
          }
        }

        if (mountedRef.current) {
          setError(null);

          notificationService.success(
            DOCUMENT_UPLOAD_SUCCESS_MESSAGE
          );
        }

        return uploadedDocument;

      } catch (err: unknown) {
        const appError =
          errorService.log(
            err,
            "Document Upload"
          );

        if (mountedRef.current) {
          setError(
            appError.message
          );

          notificationService.error(
            appError.message
          );
        }

        /**
         * Preserve the original error.
         *
         * UploadBox/components can therefore handle the failure themselves.
         */
        throw err;

      } finally {
        uploadInFlightRef.current =
          false;

        if (mountedRef.current) {
          setLoading(false);
        }
      }
    }, [
      authLoading,
      isAuthenticated,
      getAuthenticatedUserId,
    ]);

  /**
   * ==========================================================================
   * DELETE DOCUMENT
   * ==========================================================================
   *
   * Backend endpoint:
   *
   *     DELETE /api/documents/{documentId}
   *
   * The backend verifies that the document belongs to the authenticated user.
   */
  const remove =
    useCallback(async (
      id: number
    ): Promise<void> => {
      /**
       * Validate document ID before making a request.
       */
      const documentId =
        resolveDocumentId(id);

      if (documentId === null) {
        const message =
          INVALID_DOCUMENT_ID_MESSAGE;

        if (mountedRef.current) {
          setError(message);
        }

        notificationService.error(
          message
        );

        return;
      }

      /**
       * Validate authentication.
       */
      if (
        authLoading ||
        !isAuthenticated
      ) {
        const message =
          DELETE_AUTHENTICATION_REQUIRED_MESSAGE;

        if (mountedRef.current) {
          setError(message);
        }

        notificationService.error(
          message
        );

        return;
      }

      /**
       * Resolve authenticated user locally.
       */
      const userId =
        getAuthenticatedUserId();

      if (userId === null) {
        if (mountedRef.current) {
          setError(
            INVALID_USER_ID_MESSAGE
          );
        }

        notificationService.error(
          INVALID_USER_ID_MESSAGE
        );

        return;
      }

      /**
       * Prevent duplicate DELETE requests for the same document.
       */
      if (
        deletingIdsRef.current.has(
          documentId
        )
      ) {
        return;
      }

      deletingIdsRef.current.add(
        documentId
      );

      if (mountedRef.current) {
        setLoading(true);
        setError(null);
      }

      try {
        /**
         * The backend performs ownership validation.
         */
        await removeDocument(
          documentId
        );

        if (!mountedRef.current) {
          return;
        }

        /**
         * Verify the account did not change while the DELETE request was
         * executing.
         */
        const latestUserId =
          getAuthenticatedUserId();

        if (
          latestUserId === null ||
          latestUserId !== userId
        ) {
          return;
        }

        /**
         * Optimistically remove the successfully deleted document.
         */
        setDocuments(
          previousDocuments =>
            previousDocuments.filter(
              document =>
                document.id !== documentId
            )
        );

        /**
         * If this was the last known document, keep the loaded state correct.
         */
        loadedUserIdRef.current =
          userId;

        notificationService.success(
          DOCUMENT_DELETE_SUCCESS_MESSAGE
        );

      } catch (err: unknown) {
        const appError =
          errorService.log(
            err,
            "Remove Document"
          );

        if (mountedRef.current) {
          setError(
            appError.message
          );

          notificationService.error(
            appError.message
          );
        }

        /**
         * Preserve original error for the calling component.
         */
        throw err;

      } finally {
        deletingIdsRef.current.delete(
          documentId
        );

        if (mountedRef.current) {
          setLoading(false);
        }
      }
    }, [
      authLoading,
      isAuthenticated,
      getAuthenticatedUserId,
    ]);

  /**
   * ==========================================================================
   * RETURN PUBLIC HOOK API
   * ==========================================================================
   */

  return {
    documents,
    loading,
    refreshing,
    error,
    fetchDocuments,
    upload,
    remove,
  };
}

export default useDocuments;