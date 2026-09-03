/**
 * ============================================================================
 * DOCUMENT TYPES
 * ============================================================================
 *
 * Centralized TypeScript definitions for immigration documents.
 *
 * These types are shared by:
 *
 * - React components
 * - document hooks
 * - document services
 * - document API clients
 *
 * Authentication:
 *
 * The authenticated user is resolved by the backend from the JWT.
 * The frontend must NOT send userId for document operations.
 * ============================================================================
 */


/**
 * ============================================================================
 * DOCUMENT STATUS
 * ============================================================================
 */

export type DocumentStatus =
  | "PENDING"
  | "UPLOADED"
  | "PROCESSING"
  | "ANALYZING"
  | "COMPLETED"
  | "SUCCESS"
  | "FAILED";


/**
 * ============================================================================
 * RISK LEVEL
 * ============================================================================
 */

export type DocumentRiskLevel =
  | "LOW"
  | "MEDIUM"
  | "HIGH";


/**
 * ============================================================================
 * DOCUMENT
 * ============================================================================
 *
 * This represents the API response returned to the authenticated frontend.
 *
 * IMPORTANT:
 *
 * The backend should not expose:
 *
 * - S3 object keys
 * - extractedText
 * - internal database fields
 * - internal storage implementation details
 *
 * unless explicitly required by a protected endpoint.
 * ============================================================================
 */

export interface Document {

  id: number;

  fileName: string;

  documentType: string;

  fileUrl?: string | null;

  fileSize?: number | null;

  mimeType?: string | null;

  summary?: string | null;

  fraudDetected?: boolean;

  riskLevel?: DocumentRiskLevel | string;

  status: DocumentStatus | string;

  uploadedAt: string;

  updatedAt?: string | null;
}


/**
 * ============================================================================
 * DOCUMENT FILE
 * ============================================================================
 */

export interface DocumentFile {

  id: number;

  fileName: string;

  documentType: string;

  fileUrl?: string | null;

  fileSize?: number | null;

  mimeType?: string | null;

  uploadedAt: string;

  updatedAt?: string | null;

  status: DocumentStatus | string;
}


/**
 * ============================================================================
 * DOCUMENT UPLOAD REQUEST
 * ============================================================================
 *
 * IMPORTANT:
 *
 * userId has intentionally been removed.
 *
 * The backend determines the authenticated user from the JWT.
 * ============================================================================
 */

export interface DocumentUploadRequest {

  file: File;

  documentType: string;

  onProgress?: (
    progress: number
  ) => void;
}


/**
 * ============================================================================
 * DOCUMENT UPLOAD RESPONSE
 * ============================================================================
 */

export interface DocumentUploadResponse {

  id: number;

  fileName: string;

  documentType: string;

  status: DocumentStatus | string;

  message?: string;

  fileUrl?: string | null;

  uploadedAt?: string;

  updatedAt?: string;

  summary?: string | null;

  fraudDetected?: boolean;

  riskLevel?: DocumentRiskLevel | string;
}