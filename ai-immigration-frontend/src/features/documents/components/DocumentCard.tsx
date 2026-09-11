import { Link } from "react-router-dom";

import {
  Calendar,
  FileText,
  Waypoints,
} from "lucide-react";

import type {
  Document,
} from "../../../types/document";


/**
 * ============================================================================
 * DOCUMENT CARD
 * ============================================================================
 *
 * Displays an individual immigration document.
 *
 * Responsibilities:
 *
 * - Display document metadata
 * - Display upload date
 * - Display document type
 * - Display processing/verification status
 * - Safely handle incomplete API responses
 *
 * The component intentionally does not assume that optional backend fields
 * are always present. This prevents runtime rendering errors when Oracle or
 * Spring Boot returns null/undefined values.
 *
 * ============================================================================
 */


/**
 * ============================================================================
 * PROPS
 * ============================================================================
 */

interface Props {
  doc: Document;
}


/**
 * ============================================================================
 * STATUS TYPES
 * ============================================================================
 */

type DocumentStatus =
  | "VERIFIED"
  | "COMPLETED"
  | "SUCCESS"
  | "PENDING"
  | "REJECTED"
  | "PROCESSING"
  | "ANALYZING"
  | "IN_PROGRESS"
  | "UPLOADED"
  | "UPLOADING"
  | "FAILED"
  | "ERROR"
  | "UNDER_REVIEW"
  | "REVIEW"
  | "UNKNOWN";


/**
 * ============================================================================
 * STATUS NORMALIZATION
 * ============================================================================
 *
 * Converts potentially unsafe API values into a predictable status value.
 *
 * Examples:
 *
 *     "verified"       → "VERIFIED"
 *     " VERIFIED "     → "VERIFIED"
 *     "under_review"   → "UNDER_REVIEW"
 *     undefined        → "UNKNOWN"
 *     null             → "UNKNOWN"
 *
 * ============================================================================
 */

const normalizeStatus = (
  status?: string | null
): DocumentStatus => {

  if (
    typeof status !== "string" ||
    !status.trim()
  ) {
    return "UNKNOWN";
  }


  const normalized =
    status
      .trim()
      .replace(/[\s-]+/g, "_")
      .toUpperCase();


  switch (normalized) {

    case "VERIFIED":
      return "VERIFIED";

    case "COMPLETED":
      return "COMPLETED";

    case "SUCCESS":
      return "SUCCESS";

    case "PENDING":
      return "PENDING";

    case "REJECTED":
      return "REJECTED";

    case "PROCESSING":
      return "PROCESSING";

    case "ANALYZING":
      return "ANALYZING";

    case "IN_PROGRESS":
      return "IN_PROGRESS";

    case "UPLOADED":
      return "UPLOADED";

    case "UPLOADING":
      return "UPLOADING";

    case "FAILED":
      return "FAILED";

    case "ERROR":
      return "ERROR";

    case "UNDER_REVIEW":
      return "UNDER_REVIEW";

    case "REVIEW":
      return "REVIEW";

    default:
      return "UNKNOWN";
  }
};


/**
 * ============================================================================
 * STATUS COLOR
 * ============================================================================
 */

const getStatusColor = (
  status?: string | null
): string => {

  switch (
    normalizeStatus(status)
  ) {

    case "VERIFIED":
    case "COMPLETED":
    case "SUCCESS":
      return "text-emerald-400";

    case "PENDING":
      return "text-[#C6A15B]";

    case "PROCESSING":
    case "ANALYZING":
    case "IN_PROGRESS":
    case "UPLOADED":
    case "UPLOADING":
      return "text-blue-400";

    case "UNDER_REVIEW":
    case "REVIEW":
      return "text-indigo-300";

    case "REJECTED":
    case "FAILED":
    case "ERROR":
      return "text-red-400";

    case "UNKNOWN":
    default:
      return "text-slate-400";
  }
};


/**
 * ============================================================================
 * STATUS LABEL
 * ============================================================================
 *
 * Converts technical status values into user-friendly display text.
 *
 * ============================================================================
 */

const getStatusLabel = (
  status?: string | null
): string => {

  const normalized =
    normalizeStatus(status);


  switch (normalized) {

    case "VERIFIED":
      return "Verified";

    case "COMPLETED":
      return "Completed";

    case "SUCCESS":
      return "Success";

    case "PENDING":
      return "Pending";

    case "PROCESSING":
      return "Processing";

    case "ANALYZING":
      return "Analyzing";

    case "IN_PROGRESS":
      return "In Progress";

    case "UPLOADED":
      return "Uploaded";

    case "UPLOADING":
      return "Uploading";

    case "UNDER_REVIEW":
      return "Under Review";

    case "REVIEW":
      return "Review Required";

    case "REJECTED":
      return "Rejected";

    case "FAILED":
      return "Failed";

    case "ERROR":
      return "Error";

    case "UNKNOWN":
    default:
      return "Unknown";
  }
};


/**
 * ============================================================================
 * DATE FORMATTER
 * ============================================================================
 *
 * Safely formats the uploaded date.
 *
 * Invalid/missing dates should never crash the document card.
 *
 * ============================================================================
 */

const formatUploadedDate = (
  uploadedAt?: string | Date | null
): string => {

  if (!uploadedAt) {
    return "Date unavailable";
  }


  const date =
    new Date(uploadedAt);


  if (
    Number.isNaN(
      date.getTime()
    )
  ) {
    return "Date unavailable";
  }


  return date.toLocaleDateString(
    undefined,
    {
      year: "numeric",
      month: "short",
      day: "numeric",
    }
  );
};


/**
 * ============================================================================
 * DOCUMENT CARD COMPONENT
 * ============================================================================
 */

export default function DocumentCard({
  doc,
}: Props) {


  /**
   * --------------------------------------------------------------------------
   * Defensive document values
   * --------------------------------------------------------------------------
   */

  const fileName =
    typeof doc?.fileName === "string" &&
    doc.fileName.trim()
      ? doc.fileName.trim()
      : "Untitled Document";


  const documentType =
    typeof doc?.documentType === "string" &&
    doc.documentType.trim()
      ? doc.documentType.trim()
      : "Document";


  const status =
    doc?.status;


  const statusLabel =
    getStatusLabel(status);


  const formattedDate =
    formatUploadedDate(
      doc?.uploadedAt
    );


  /**
   * --------------------------------------------------------------------------
   * Render
   * --------------------------------------------------------------------------
   */

  return (
    <div
      className="
        border
        border-white/10
        bg-white/5
        backdrop-blur-xl
        rounded-3xl
        p-6
        hover:-translate-y-1
        hover:bg-white/[0.08]
        transition-all
        duration-200
      "
    >

      <div
        className="
          flex
          items-start
          gap-3
        "
      >

        <FileText
          size={22}
          className="text-[#C6A15B] shrink-0"
          aria-hidden="true"
        />


        <div
          className="
            flex-1
            min-w-0
          "
        >

          {/* ---------------------------------------------------------------- */}
          {/* DOCUMENT NAME                                                   */}
          {/* ---------------------------------------------------------------- */}

          <h3
            className="
              font-bold
              text-lg
              text-white
              break-words
            "
          >
            {fileName}
          </h3>


          {/* ---------------------------------------------------------------- */}
          {/* DOCUMENT TYPE                                                   */}
          {/* ---------------------------------------------------------------- */}

          <span
            className="
              inline-block
              mt-3
              px-3
              py-1
              rounded-full
              bg-[#C6A15B]/15
              text-[#C6A15B]
              text-sm
              font-medium
              break-words
            "
          >
            {documentType}
          </span>


          {/* ---------------------------------------------------------------- */}
          {/* UPLOAD DATE                                                     */}
          {/* ---------------------------------------------------------------- */}

          <div
            className="
              mt-4
              flex
              items-center
              gap-2
              text-sm
              text-slate-400
            "
          >

            <Calendar
              size={14}
              className="shrink-0"
              aria-hidden="true"
            />

            <span>
              Uploaded {formattedDate}
            </span>

          </div>


          {/* ---------------------------------------------------------------- */}
          {/* DOCUMENT STATUS                                                 */}
          {/* ---------------------------------------------------------------- */}

          <p
            className={`
              mt-3
              text-sm
              font-semibold
              ${getStatusColor(status)}
            `}
          >
            Status: {statusLabel}
          </p>


          {/* ---------------------------------------------------------------- */}
          {/* TRACE EVIDENCE ENTRY POINT                                       */}
          {/* ---------------------------------------------------------------- */}

          {typeof doc?.id === "number" && (
            <Link
              to={`/dashboard/evidence-graph?document=${doc.id}`}
              className="
                mt-3
                inline-flex
                items-center
                gap-1
                text-xs
                font-semibold
                text-[#C6A15B]
                transition
                hover:text-[#dbb877]
              "
            >
              <Waypoints size={13} />
              Trace evidence
            </Link>
          )}

        </div>

      </div>

    </div>
  );
}