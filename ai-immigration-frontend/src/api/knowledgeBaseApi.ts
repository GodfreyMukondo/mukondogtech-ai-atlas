import API from "./axios";

/**
 * ============================================================================
 * KNOWLEDGE BASE API
 * ============================================================================
 *
 * Centralized API client for the admin knowledge base.
 *
 * Supported operations:
 * - List knowledge documents
 * - Search / filter / paginate
 * - Get statistics
 * - Get document by ID
 * - Create article
 * - Update article
 * - Delete document
 * - Upload knowledge file
 * - Trigger AI indexing
 *
 * Backend base path:
 *
 *     /api/admin/knowledge-base
 *
 * Axios baseURL is expected to already contain:
 *
 *     http://localhost:8080/api
 *
 * Therefore we only use:
 *
 *     /admin/knowledge-base
 *
 * here.
 * ============================================================================
 */


/**
 * ============================================================================
 * ENUMS / TYPES
 * ============================================================================
 */

export type KnowledgeDocumentStatus =
    | "DRAFT"
    | "REVIEW"
    | "PUBLISHED";


/**
 * ============================================================================
 * KNOWLEDGE DOCUMENT SUMMARY
 * ============================================================================
 *
 * Used by the paginated table.
 */

export interface KnowledgeDocumentSummary {

    id: number;

    title: string;

    category: string;

    country: string;

    status: KnowledgeDocumentStatus;

    version: string;

    updatedBy: string;

    updatedAt: string;

    aiIndexed: boolean;
}


/**
 * ============================================================================
 * FULL KNOWLEDGE DOCUMENT
 * ============================================================================
 */

export interface KnowledgeDocument {

    id: number;

    title: string;

    category: string;

    country: string;

    status: KnowledgeDocumentStatus;

    version: string;

    content: string;

    sourceUrl?: string | null;

    aiIndexed: boolean;

    aiIndexedAt?: string | null;

    createdBy: string;

    updatedBy: string;

    createdAt: string;

    updatedAt: string;
}


/**
 * ============================================================================
 * KNOWLEDGE BASE STATISTICS
 * ============================================================================
 */

export interface KnowledgeBaseStats {

    totalDocuments: number;

    aiIndexedDocuments: number;

    countriesCovered: number;

    categoriesCovered: number;

    publishedDocuments: number;

    reviewDocuments: number;

    draftDocuments: number;

    pendingAiIndexDocuments: number;
}


/**
 * ============================================================================
 * PAGINATION RESPONSE
 * ============================================================================
 */

export interface KnowledgeDocumentPage {

    content: KnowledgeDocumentSummary[];

    totalElements: number;

    totalPages: number;

    size: number;

    number: number;

    first: boolean;

    last: boolean;

    numberOfElements?: number;

    empty?: boolean;
}


/**
 * ============================================================================
 * CREATE ARTICLE REQUEST
 * ============================================================================
 */

export interface CreateKnowledgeDocumentRequest {

    title: string;

    category: string;

    country: string;

    status?: KnowledgeDocumentStatus;

    version: string;

    content: string;

    sourceUrl?: string;
}


/**
 * ============================================================================
 * UPDATE ARTICLE REQUEST
 * ============================================================================
 */

export interface UpdateKnowledgeDocumentRequest {

    title: string;

    category: string;

    country: string;

    status?: KnowledgeDocumentStatus;

    version: string;

    content: string;

    sourceUrl?: string;
}


/**
 * ============================================================================
 * UPLOAD KNOWLEDGE REQUEST
 * ============================================================================
 *
 * Multipart upload fields.
 *
 * The actual FormData object is created inside the API function so
 * callers do not need to manually construct FormData.
 */

export interface UploadKnowledgeRequest {

    file: File;

    title?: string;

    category?: string;

    country?: string;

    status?: KnowledgeDocumentStatus;

    version?: string;

    sourceUrl?: string;
}


/**
 * ============================================================================
 * API RESPONSE TYPES
 * ============================================================================
 */

export interface KnowledgeUploadResponse
    extends KnowledgeDocument {
}


/**
 * ============================================================================
 * API PATH
 * ============================================================================
 */

const KNOWLEDGE_BASE_API =
    "/admin/knowledge-base";


/**
 * ============================================================================
 * GET KNOWLEDGE DOCUMENTS
 * ============================================================================
 *
 * GET:
 *
 * /api/admin/knowledge-base
 *
 * Supports:
 * - search
 * - category
 * - status
 * - page
 * - size
 */

export async function getKnowledgeDocuments(
    params: {
        search?: string;

        category?: string;

        status?: KnowledgeDocumentStatus;

        page?: number;

        size?: number;
    } = {}
): Promise<KnowledgeDocumentPage> {

    const response =
        await API.get<KnowledgeDocumentPage>(
            KNOWLEDGE_BASE_API,
            {
                params: {

                    search:
                        params.search?.trim() ||
                        undefined,

                    category:
                        params.category?.trim() ||
                        undefined,

                    status:
                        params.status ||
                        undefined,

                    page:
                        Math.max(
                            params.page ?? 0,
                            0
                        ),

                    size:
                        Math.min(
                            Math.max(
                                params.size ?? 20,
                                1
                            ),
                            100
                        ),
                },
            }
        );

    return response.data;
}


/**
 * ============================================================================
 * GET KNOWLEDGE BASE STATISTICS
 * ============================================================================
 *
 * GET:
 *
 * /api/admin/knowledge-base/stats
 */

export async function getKnowledgeBaseStats():
    Promise<KnowledgeBaseStats> {

    const response =
        await API.get<KnowledgeBaseStats>(
            `${KNOWLEDGE_BASE_API}/stats`
        );

    return response.data;
}


/**
 * ============================================================================
 * GET DOCUMENT BY ID
 * ============================================================================
 *
 * GET:
 *
 * /api/admin/knowledge-base/{id}
 */

export async function getKnowledgeDocument(
    id: number
): Promise<KnowledgeDocument> {

    validateDocumentId(id);

    const response =
        await API.get<KnowledgeDocument>(
            `${KNOWLEDGE_BASE_API}/${id}`
        );

    return response.data;
}


/**
 * ============================================================================
 * CREATE KNOWLEDGE ARTICLE
 * ============================================================================
 *
 * POST:
 *
 * /api/admin/knowledge-base
 *
 * Used by:
 *
 *     New Article
 *
 * The page/modal should collect:
 * - title
 * - category
 * - country
 * - status
 * - version
 * - content
 * - source URL
 */

export async function createKnowledgeDocument(
    request: CreateKnowledgeDocumentRequest
): Promise<KnowledgeDocument> {

    validateCreateRequest(
        request
    );

    const response =
        await API.post<KnowledgeDocument>(
            KNOWLEDGE_BASE_API,
            {
                title:
                    request.title.trim(),

                category:
                    request.category.trim(),

                country:
                    request.country.trim(),

                status:
                    request.status ?? "DRAFT",

                version:
                    request.version.trim(),

                content:
                    request.content.trim(),

                sourceUrl:
                    request.sourceUrl?.trim() ||
                    undefined,
            }
        );

    return response.data;
}


/**
 * ============================================================================
 * UPDATE KNOWLEDGE ARTICLE
 * ============================================================================
 *
 * PUT:
 *
 * /api/admin/knowledge-base/{id}
 */

export async function updateKnowledgeDocument(
    id: number,
    request: UpdateKnowledgeDocumentRequest
): Promise<KnowledgeDocument> {

    validateDocumentId(id);

    validateUpdateRequest(
        request
    );

    const response =
        await API.put<KnowledgeDocument>(
            `${KNOWLEDGE_BASE_API}/${id}`,
            {
                title:
                    request.title.trim(),

                category:
                    request.category.trim(),

                country:
                    request.country.trim(),

                status:
                    request.status ?? "DRAFT",

                version:
                    request.version.trim(),

                content:
                    request.content.trim(),

                sourceUrl:
                    request.sourceUrl?.trim() ||
                    undefined,
            }
        );

    return response.data;
}


/**
 * ============================================================================
 * DELETE KNOWLEDGE DOCUMENT
 * ============================================================================
 *
 * DELETE:
 *
 * /api/admin/knowledge-base/{id}
 */

export async function deleteKnowledgeDocument(
    id: number
): Promise<void> {

    validateDocumentId(id);

    await API.delete(
        `${KNOWLEDGE_BASE_API}/${id}`
    );
}


/**
 * ============================================================================
 * UPLOAD KNOWLEDGE DOCUMENT
 * ============================================================================
 *
 * POST:
 *
 * /api/admin/knowledge-base/upload
 *
 * Content-Type:
 *
 * multipart/form-data
 *
 * Supported file types should normally be validated by the backend.
 *
 * Recommended:
 * - PDF
 * - DOC
 * - DOCX
 * - TXT
 *
 * The browser automatically creates the multipart boundary when
 * FormData is supplied to Axios.
 */

export async function uploadKnowledgeDocument(
    request: UploadKnowledgeRequest
): Promise<KnowledgeUploadResponse> {

    validateUploadRequest(
        request
    );

    const formData =
        new FormData();

    formData.append(
        "file",
        request.file
    );

    if (
        request.title?.trim()
    ) {

        formData.append(
            "title",
            request.title.trim()
        );
    }

    if (
        request.category?.trim()
    ) {

        formData.append(
            "category",
            request.category.trim()
        );
    }

    if (
        request.country?.trim()
    ) {

        formData.append(
            "country",
            request.country.trim()
        );
    }

    if (
        request.status
    ) {

        formData.append(
            "status",
            request.status
        );
    }

    if (
        request.version?.trim()
    ) {

        formData.append(
            "version",
            request.version.trim()
        );
    }

    if (
        request.sourceUrl?.trim()
    ) {

        formData.append(
            "sourceUrl",
            request.sourceUrl.trim()
        );
    }

    const response =
        await API.post<KnowledgeUploadResponse>(
            `${KNOWLEDGE_BASE_API}/upload`,
            formData,
            {
                /**
                 * The shared `API` axios instance sets a hard
                 * "Content-Type: application/json" default header.
                 *
                 * Axios only lets FormData pass through untouched when it
                 * does NOT already see a JSON content type; otherwise it
                 * silently JSON.stringifies the FormData while still
                 * sending "Content-Type: application/json", which the
                 * backend's multipart endpoint rejects.
                 *
                 * Clearing it here lets the browser generate the correct
                 * multipart/form-data boundary for this request only.
                 */
                headers: {
                    "Content-Type": undefined,
                },
            }
        );

    return response.data;
}


/**
 * ============================================================================
 * INDEX KNOWLEDGE DOCUMENT
 * ============================================================================
 *
 * POST:
 *
 * /api/admin/knowledge-base/{id}/index
 *
 * This tells the backend to process/index the document for AI retrieval.
 */

export async function indexKnowledgeDocument(
    id: number
): Promise<KnowledgeDocument> {

    validateDocumentId(id);

    const response =
        await API.post<KnowledgeDocument>(
            `${KNOWLEDGE_BASE_API}/${id}/index`
        );

    return response.data;
}


/**
 * ============================================================================
 * PUBLISH KNOWLEDGE DOCUMENT
 * ============================================================================
 *
 * Optional production helper.
 *
 * If your Spring Boot backend exposes:
 *
 * POST /api/admin/knowledge-base/{id}/publish
 *
 * this can be used directly.
 *
 * If the endpoint does not exist yet, do not call this function
 * from the UI until the backend endpoint has been implemented.
 */

export async function publishKnowledgeDocument(
    id: number
): Promise<KnowledgeDocument> {

    validateDocumentId(id);

    const response =
        await API.post<KnowledgeDocument>(
            `${KNOWLEDGE_BASE_API}/${id}/publish`
        );

    return response.data;
}


/**
 * ============================================================================
 * UNPUBLISH KNOWLEDGE DOCUMENT
 * ============================================================================
 *
 * Optional production helper.
 */

export async function unpublishKnowledgeDocument(
    id: number
): Promise<KnowledgeDocument> {

    validateDocumentId(id);

    const response =
        await API.post<KnowledgeDocument>(
            `${KNOWLEDGE_BASE_API}/${id}/unpublish`
        );

    return response.data;
}


/**
 * ============================================================================
 * VALIDATION HELPERS
 * ============================================================================
 *
 * These provide fast client-side protection before making invalid
 * HTTP requests.
 */


/**
 * Validate document ID.
 */

function validateDocumentId(
    id: number
): void {

    if (
        !Number.isInteger(id) ||
        id <= 0
    ) {

        throw new Error(
            "A valid knowledge document ID is required."
        );
    }
}


/**
 * Validate create request.
 */

function validateCreateRequest(
    request: CreateKnowledgeDocumentRequest
): void {

    if (!request) {

        throw new Error(
            "Knowledge document data is required."
        );
    }

    if (
        !request.title?.trim()
    ) {

        throw new Error(
            "Article title is required."
        );
    }

    if (
        !request.category?.trim()
    ) {

        throw new Error(
            "Article category is required."
        );
    }

    if (
        !request.country?.trim()
    ) {

        throw new Error(
            "Article country is required."
        );
    }

    if (
        !request.version?.trim()
    ) {

        throw new Error(
            "Article version is required."
        );
    }

    if (
        !request.content?.trim()
    ) {

        throw new Error(
            "Article content is required."
        );
    }
}


/**
 * Validate update request.
 */

function validateUpdateRequest(
    request: UpdateKnowledgeDocumentRequest
): void {

    validateCreateRequest(
        request
    );
}


/**
 * Validate upload request.
 */

function validateUploadRequest(
    request: UploadKnowledgeRequest
): void {

    if (!request) {

        throw new Error(
            "Upload data is required."
        );
    }

    if (!request.file) {

        throw new Error(
            "Please select a knowledge file."
        );
    }

    if (
        request.file.size <= 0
    ) {

        throw new Error(
            "The selected file is empty."
        );
    }

    /**
     * 25 MB frontend protection.
     *
     * The backend should also enforce its own
     * upload-size limit.
     */

    const MAX_FILE_SIZE =
        25 * 1024 * 1024;

    if (
        request.file.size >
        MAX_FILE_SIZE
    ) {

        throw new Error(
            "Knowledge files must be 25 MB or smaller."
        );
    }
}


/**
 * ============================================================================
 * FILE TYPE HELPERS
 * ============================================================================
 *
 * These are useful for the Upload Knowledge modal.
 */


/**
 * Supported knowledge file extensions.
 */

export const SUPPORTED_KNOWLEDGE_FILE_EXTENSIONS =
    [
        ".pdf",
        ".doc",
        ".docx",
        ".txt",
    ] as const;


/**
 * Supported MIME types.
 */

export const SUPPORTED_KNOWLEDGE_MIME_TYPES =
    [
        "application/pdf",

        "application/msword",

        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",

        "text/plain",
    ] as const;


/**
 * Validate supported file type.
 */

export function isSupportedKnowledgeFile(
    file: File
): boolean {

    const fileName =
        file.name.toLowerCase();

    const extensionSupported =
        SUPPORTED_KNOWLEDGE_FILE_EXTENSIONS
            .some(
                extension =>
                    fileName.endsWith(
                        extension
                    )
            );

    const mimeSupported =
        SUPPORTED_KNOWLEDGE_MIME_TYPES
            .includes(
                file.type as
                    (typeof SUPPORTED_KNOWLEDGE_MIME_TYPES)[number]
            );

    /**
     * Some browsers may provide an empty MIME type.
     *
     * Therefore extension validation is accepted as
     * a fallback.
     */

    return (
        extensionSupported ||
        mimeSupported
    );
}


/**
 * ============================================================================
 * FORMAT FILE SIZE
 * ============================================================================
 */

export function formatKnowledgeFileSize(
    bytes: number
): string {

    if (
        !Number.isFinite(bytes) ||
        bytes < 0
    ) {

        return "0 Bytes";
    }

    if (
        bytes === 0
    ) {

        return "0 Bytes";
    }

    const units =
        [
            "Bytes",
            "KB",
            "MB",
            "GB",
        ];

    const index =
        Math.floor(
            Math.log(bytes) /
            Math.log(1024)
        );

    const safeIndex =
        Math.min(
            index,
            units.length - 1
        );

    const value =
        bytes /
        Math.pow(
            1024,
            safeIndex
        );

    return `${value.toFixed(
        safeIndex === 0
            ? 0
            : 1
    )} ${units[safeIndex]}`;
}

