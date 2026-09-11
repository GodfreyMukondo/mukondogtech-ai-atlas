import {
  useCallback,
  useEffect,
  type ReactNode,
} from "react";

import { motion } from "framer-motion";

import {
  CheckCircle2,
  FileText,
  FolderOpen,
  RefreshCw,
  ShieldCheck,
  UploadCloud,
} from "lucide-react";

import {
  useDocuments,
} from "../../features/documents/hooks/useDocuments";

import UploadBox from "../../features/documents/components/UploadBox";

import DocumentCard from "../../features/documents/components/DocumentCard";

import Loader from "../../components/common/Loader";

import ErrorAlert from "../../components/common/ErrorAlert";

/**
 * ============================================================================
 * DOCUMENTS PAGE
 * ============================================================================
 *
 * Production responsibilities:
 *
 * - Load authenticated user's documents
 * - Display document statistics
 * - Upload documents
 * - Refresh document library
 * - Display loading state
 * - Display refresh state
 * - Display errors
 * - Display empty state
 *
 * SECURITY:
 *
 * This page NEVER supplies a user ID.
 *
 * The authenticated user must be resolved by useDocuments() / backend
 * authentication infrastructure.
 *
 * REFRESH:
 *
 * The Refresh button explicitly invokes fetchDocuments().
 *
 * A successful request replaces the local document collection with the
 * latest server response.
 *
 * IMPORTANT:
 *
 * `loading` is treated as the initial/full collection loading state.
 *
 * If useDocuments() exposes a dedicated `refreshing` state, it is preferred.
 * Otherwise this page safely falls back to `loading`.
 * ============================================================================
 */

export default function DocumentsPage() {
  const {
    documents,
    loading,
    error,
    fetchDocuments,
    upload,

    /*
     * If the hook exposes `refreshing`, it will be used.
     *
     * The optional access keeps this page compatible with a hook that
     * currently exposes only `loading`.
     */
    refreshing,
  } = useDocuments();

  /**
   * ==========================================================================
   * INITIAL DOCUMENT LOAD
   * ==========================================================================
   *
   * The hook must memoize fetchDocuments with useCallback().
   *
   * Therefore this effect runs:
   *
   * - on initial mount
   * - if the authenticated-user context changes in a way that changes
   *   fetchDocuments
   *
   * It does NOT run on every render.
   */
  useEffect(() => {
    void fetchDocuments();
  }, [fetchDocuments]);

  /**
   * ==========================================================================
   * UPLOAD HANDLER
   * ==========================================================================
   *
   * The authenticated user ID is intentionally NOT supplied here.
   *
   * The hook/API layer is responsible for resolving the authenticated user.
   */
  const handleUpload = useCallback(
    async (
      file: File,
      onProgress?: (
        progress: number
      ) => void,
    ): Promise<void> => {
      await upload({
        file,
        documentType: "PASSPORT",
        onProgress,
      });
    },
    [upload],
  );

  /**
   * ==========================================================================
   * REFRESH HANDLER
   * ==========================================================================
   *
   * This is the actual action triggered by the Refresh button.
   *
   * The request is intentionally awaited so:
   *
   * - the button remains in the correct state while the request is running
   * - rejected promises are handled by the hook
   * - callers do not receive an unhandled Promise rejection
   *
   * fetchDocuments() should update the hook's `documents` state after a
   * successful GET /documents request.
   */
  const handleRefresh = useCallback(async (): Promise<void> => {
    await fetchDocuments();
  }, [fetchDocuments]);

  /**
   * ==========================================================================
   * DERIVED STATE
   * ==========================================================================
   */

  const documentCount = documents.length;

  const documentLabel =
    documentCount === 1
      ? "document"
      : "documents";

  const libraryDescription =
    documentCount === 0
      ? "Your uploaded documents will appear here."
      : `You currently have ${documentCount} ${documentLabel} in your library.`;

  const documentStatus =
    documentCount > 0
      ? "Active"
      : "Pending";

  const documentStatusDescription =
    documentCount > 0
      ? "Your document library is active"
      : "Upload a document to get started";

  /*
   * Prefer a dedicated refreshing state when available.
   *
   * If the current hook does not expose it, loading is used as a safe
   * compatibility fallback.
   */
  const isRefreshing =
    typeof refreshing === "boolean"
      ? refreshing
      : loading;

  /*
   * Initial loading should only show the full collection loader when there
   * are no documents available yet.
   *
   * During refresh, existing documents remain visible.
   */
  const isInitialLoading =
    loading &&
    documentCount === 0;

  return (
    <div className="min-h-screen">
      <main
        className="
          w-full
          px-4
          py-6
          sm:px-6
          lg:px-8
          lg:py-8
        "
      >
        {/* ==================================================================
            PAGE HEADER
        ================================================================== */}

        <motion.section
          initial={{
            opacity: 0,
            y: 10,
          }}
          animate={{
            opacity: 1,
            y: 0,
          }}
          transition={{
            duration: 0.3,
          }}
          className="mb-8"
        >
          <div
            className="
              flex
              flex-col
              gap-5
              lg:flex-row
              lg:items-end
              lg:justify-between
            "
          >
            <div className="max-w-3xl">
              <div
                className="
                  mb-3
                  flex
                  items-center
                  gap-2
                  text-sm
                  font-medium
                  text-[#C6A15B]
                "
              >
                <FolderOpen
                  size={18}
                  aria-hidden="true"
                />

                <span>
                  Document management
                </span>
              </div>

              <h1
                className="
                  text-3xl
                  font-bold
                  tracking-tight
                  text-white
                  sm:text-4xl
                "
              >
                My Documents
              </h1>

              <p
                className="
                  mt-2
                  max-w-2xl
                  text-base
                  leading-7
                  text-slate-400
                "
              >
                Keep your immigration documents organised
                in one secure place. Upload, review and
                manage your documents whenever you need them.
              </p>
            </div>

            {/* ==============================================================
                REFRESH BUTTON
                ============================================================== */}

            <button
              type="button"
              onClick={handleRefresh}
              disabled={isRefreshing}
              aria-busy={isRefreshing}
              aria-label={
                isRefreshing
                  ? "Refreshing documents"
                  : "Refresh documents"
              }
              title={
                isRefreshing
                  ? "Refreshing documents..."
                  : "Refresh document library"
              }
              className="
                inline-flex
                h-11
                items-center
                justify-center
                gap-2
                self-start
                rounded-lg
                border
                border-white/15
                bg-white/5
                px-4
                text-sm
                font-semibold
                text-slate-200
                shadow-sm
                transition
                duration-200
                hover:border-white/25
                hover:bg-white/10
                hover:text-white
                focus:outline-none
                focus:ring-2
                focus:ring-[#3C4C61]/20
                disabled:cursor-not-allowed
                disabled:opacity-60
                lg:self-auto
              "
            >
              <RefreshCw
                size={17}
                aria-hidden="true"
                className={
                  isRefreshing
                    ? "animate-spin"
                    : undefined
                }
              />

              <span>
                {isRefreshing
                  ? "Refreshing..."
                  : "Refresh"}
              </span>
            </button>
          </div>
        </motion.section>

        {/* ==================================================================
            DOCUMENT SUMMARY
        ================================================================== */}

        <motion.section
          initial={{
            opacity: 0,
            y: 10,
          }}
          animate={{
            opacity: 1,
            y: 0,
          }}
          transition={{
            duration: 0.3,
            delay: 0.05,
          }}
          className="
            mb-8
            grid
            grid-cols-1
            gap-4
            sm:grid-cols-2
            lg:grid-cols-3
          "
        >
          <StatsCard
            icon={
              <FolderOpen
                size={21}
                aria-hidden="true"
              />
            }
            value={documentCount}
            label="Total documents"
            description="Documents currently in your library"
          />

          <StatsCard
            icon={
              <ShieldCheck
                size={21}
                aria-hidden="true"
              />
            }
            value="Secure"
            label="Storage"
            description="Your uploaded documents are protected"
          />

          <StatsCard
            icon={
              <CheckCircle2
                size={21}
                aria-hidden="true"
              />
            }
            value={documentStatus}
            label="Document status"
            description={documentStatusDescription}
          />
        </motion.section>

        {/* ==================================================================
            ERROR ALERT
        ================================================================== */}

        {error && (
          <motion.div
            initial={{
              opacity: 0,
            }}
            animate={{
              opacity: 1,
            }}
            className="mb-6"
            role="alert"
            aria-live="polite"
          >
            <ErrorAlert
              message={error}
            />
          </motion.div>
        )}

        {/* ==================================================================
            UPLOAD SECTION
        ================================================================== */}

        <motion.section
          initial={{
            opacity: 0,
            y: 10,
          }}
          animate={{
            opacity: 1,
            y: 0,
          }}
          transition={{
            duration: 0.3,
            delay: 0.1,
          }}
          className="
            mb-10
            overflow-hidden
            rounded-xl
            border
            border-white/10
            bg-white/5
            backdrop-blur-xl
            shadow-sm
          "
        >
          <div
            className="
              border-b
              border-white/10
              px-5
              py-5
              sm:px-6
            "
          >
            <div
              className="
                flex
                items-start
                gap-3
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
                  bg-blue-500/10
                  text-blue-300
                "
              >
                <UploadCloud
                  size={20}
                  aria-hidden="true"
                />
              </div>

              <div>
                <h2
                  className="
                    text-base
                    font-semibold
                    text-white
                  "
                >
                  Upload a document
                </h2>

                <p
                  className="
                    mt-1
                    text-sm
                    leading-6
                    text-slate-400
                  "
                >
                  Add a passport, visa, permit, certificate
                  or other supporting immigration document.
                </p>
              </div>
            </div>
          </div>

          <div className="p-5 sm:p-6">
            <UploadBox
              onUpload={handleUpload}
            />
          </div>
        </motion.section>

        {/* ==================================================================
            DOCUMENT LIBRARY HEADER
        ================================================================== */}

        <motion.section
          initial={{
            opacity: 0,
          }}
          animate={{
            opacity: 1,
          }}
          transition={{
            duration: 0.3,
            delay: 0.15,
          }}
          className="mb-5"
        >
          <div
            className="
              flex
              flex-col
              gap-2
              sm:flex-row
              sm:items-end
              sm:justify-between
            "
          >
            <div>
              <h2
                className="
                  text-xl
                  font-bold
                  tracking-tight
                  text-white
                "
              >
                Document Library
              </h2>

              <p
                className="
                  mt-1
                  text-sm
                  text-slate-400
                "
              >
                {libraryDescription}
              </p>
            </div>

            {documentCount > 0 && (
              <div
                className="
                  text-sm
                  font-medium
                  text-slate-400
                "
                aria-label={`${documentCount} ${documentLabel}`}
              >
                {documentCount}{" "}
                {documentLabel}
              </div>
            )}
          </div>
        </motion.section>

        {/* ==================================================================
            DOCUMENT CONTENT
        ================================================================== */}

        {isInitialLoading ? (
          <div
            className="
              flex
              min-h-[280px]
              items-center
              justify-center
              rounded-xl
              border
              border-white/10
              bg-white/5
              shadow-sm
            "
            role="status"
            aria-live="polite"
          >
            <Loader
              text="Loading your documents..."
            />
          </div>
        ) : documentCount === 0 ? (
          <EmptyDocumentsState />
        ) : (
          <motion.section
            initial={{
              opacity: 0,
            }}
            animate={{
              opacity: 1,
            }}
            transition={{
              duration: 0.3,
            }}
            aria-label="Uploaded documents"
            aria-busy={isRefreshing}
          >
            {/* ==============================================================
                REFRESHING INDICATOR
                ============================================================== */}

            {isRefreshing && (
              <div
                className="
                  mb-4
                  flex
                  items-center
                  gap-2
                  rounded-lg
                  border
                  border-white/10
                  bg-white/5
                  px-4
                  py-3
                  text-sm
                  text-slate-400
                  shadow-sm
                "
                role="status"
                aria-live="polite"
              >
                <RefreshCw
                  size={16}
                  className="animate-spin"
                  aria-hidden="true"
                />

                <span>
                  Refreshing your document library...
                </span>
              </div>
            )}

            <div
              className="
                grid
                grid-cols-1
                gap-5
                md:grid-cols-2
                xl:grid-cols-3
              "
            >
              {documents.map(
                (
                  document,
                  index,
                ) => (
                  <motion.div
                    key={document.id}
                    initial={{
                      opacity: 0,
                      y: 10,
                    }}
                    animate={{
                      opacity: 1,
                      y: 0,
                    }}
                    transition={{
                      duration: 0.25,
                      delay: Math.min(
                        index * 0.04,
                        0.25,
                      ),
                    }}
                  >
                    <DocumentCard
                      doc={document}
                    />
                  </motion.div>
                ),
              )}
            </div>
          </motion.section>
        )}
      </main>
    </div>
  );
}

/**
 * ============================================================================
 * STATS CARD
 * ============================================================================
 */

interface StatsCardProps {
  icon: ReactNode;
  value: string | number;
  label: string;
  description: string;
}

function StatsCard({
  icon,
  value,
  label,
  description,
}: StatsCardProps) {
  return (
    <article
      className="
        rounded-xl
        border
        border-white/10
        bg-white/5
        p-5
        shadow-sm
        transition
        duration-200
        hover:border-white/20
        hover:shadow-md
      "
    >
      <div
        className="
          flex
          items-start
          justify-between
          gap-4
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
            bg-blue-500/10
            text-blue-300
          "
          aria-hidden="true"
        >
          {icon}
        </div>

        <span
          className="
            rounded-full
            bg-white/10
            px-2.5
            py-1
            text-xs
            font-medium
            text-slate-400
          "
        >
          {label}
        </span>
      </div>

      <div className="mt-5">
        <div
          className="
            text-2xl
            font-bold
            tracking-tight
            text-white
          "
        >
          {value}
        </div>

        <p
          className="
            mt-1
            text-sm
            leading-5
            text-slate-400
          "
        >
          {description}
        </p>
      </div>
    </article>
  );
}

/**
 * ============================================================================
 * EMPTY DOCUMENT STATE
 * ============================================================================
 */

function EmptyDocumentsState() {
  return (
    <motion.section
      initial={{
        opacity: 0,
        y: 10,
      }}
      animate={{
        opacity: 1,
        y: 0,
      }}
      className="
        rounded-xl
        border
        border-white/10
        bg-white/5
        px-6
        py-14
        text-center
        shadow-sm
        sm:px-10
      "
    >
      <div
        className="
          mx-auto
          flex
          h-14
          w-14
          items-center
          justify-center
          rounded-xl
          bg-white/10
          text-[#C6A15B]
        "
        aria-hidden="true"
      >
        <FileText
          size={27}
          aria-hidden="true"
        />
      </div>

      <h3
        className="
          mt-5
          text-lg
          font-semibold
          text-white
        "
      >
        No documents yet
      </h3>

      <p
        className="
          mx-auto
          mt-2
          max-w-md
          text-sm
          leading-6
          text-slate-400
        "
      >
        You have not uploaded any immigration documents
        yet. Use the upload section above to add your
        first document.
      </p>

      <div
        className="
          mx-auto
          mt-6
          flex
          max-w-lg
          flex-wrap
          items-center
          justify-center
          gap-x-4
          gap-y-2
          text-xs
          font-medium
          text-slate-400
        "
        aria-label="Supported document examples"
      >
        <span>
          Passport
        </span>

        <span
          className="hidden sm:inline"
          aria-hidden="true"
        >
          •
        </span>

        <span>
          Visa
        </span>

        <span
          className="hidden sm:inline"
          aria-hidden="true"
        >
          •
        </span>

        <span>
          Permit
        </span>

        <span
          className="hidden sm:inline"
          aria-hidden="true"
        >
          •
        </span>

        <span>
          Certificates
        </span>
      </div>
    </motion.section>
  );
}

