/**
 * ============================================================================
 * APPLICATION TYPES
 * ============================================================================
 *
 * Centralized TypeScript definitions for immigration applications.
 *
 * Shared by:
 *
 * - SubmitApplicationPage (applicant submission form)
 * - ApplicationsPage (applicant's own application list)
 * - applicationApi
 *
 * Authentication:
 *
 * The authenticated user is resolved by the backend from the JWT.
 * The frontend must NOT send userId for application operations.
 * ============================================================================
 */

export type ApplicationStatus =
  | "PENDING"
  | "APPROVED"
  | "REJECTED";

export type ApplicationRiskLevel =
  | "LOW"
  | "MEDIUM"
  | "HIGH";

/**
 * ============================================================================
 * APPLICATION DOCUMENT SUMMARY
 * ============================================================================
 */

export interface ApplicationDocumentSummary {

  id: number;

  fileName: string;

  documentType: string;

  status: string;
}

/**
 * ============================================================================
 * APPLICATION
 * ============================================================================
 *
 * Applicant-facing representation, matching the backend's
 * ApplicationResponse DTO.
 * ============================================================================
 */

export interface Application {

  id: number;

  fullName: string;

  email: string;

  phone?: string | null;

  dateOfBirth?: string | null;

  country: string;

  visaType: string;

  notes?: string | null;

  status: ApplicationStatus | string;

  riskLevel: ApplicationRiskLevel | string;

  aiConfidence?: number | null;

  rejectionReason?: string | null;

  submittedAt: string;

  updatedAt?: string | null;

  documentCount: number;

  documents?: ApplicationDocumentSummary[] | null;
}

/**
 * ============================================================================
 * SUBMIT APPLICATION REQUEST
 * ============================================================================
 *
 * IMPORTANT:
 *
 * documentIds must reference documents already uploaded via
 * POST /api/documents/upload by the same authenticated user.
 * ============================================================================
 */

export interface ApplicationSubmitRequest {

  fullName: string;

  email: string;

  phone?: string;

  dateOfBirth?: string;

  country: string;

  visaType: string;

  notes?: string;

  documentIds: number[];
}
