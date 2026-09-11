import {
  Download,
  FileText,
  Search,
  RefreshCw,
  Filter,
  AlertTriangle,
  Receipt,
  CheckCircle2,
  Clock3,
  XCircle,
} from "lucide-react";
import {
  useCallback,
  useEffect,
  useMemo,
  useState,
} from "react";

/* ============================================================================
   TYPES
   ========================================================================== */

const INVOICE_STATUSES = [
  "PAID",
  "PENDING",
  "FAILED",
  "CANCELLED",
] as const;

type InvoiceStatus =
  (typeof INVOICE_STATUSES)[number];

interface Invoice {
  id: string;
  invoiceNumber: string;
  date: string;
  amount: number;
  currency: string;
  status: InvoiceStatus;
  description: string;
  downloadUrl?: string;
}

interface BillingResponse {
  invoices: Invoice[];
  total?: number;
}

interface ApiErrorResponse {
  detail?: string;
  message?: string;
  error?: string;
}

/* ============================================================================
   ENVIRONMENT CONFIGURATION
   ========================================================================== */

const BILLING_HISTORY_ENDPOINT =
  import.meta.env.VITE_BILLING_HISTORY_ENDPOINT;

const INVOICE_DOWNLOAD_ENDPOINT =
  import.meta.env.VITE_INVOICE_DOWNLOAD_ENDPOINT;

const CURRENCY_LOCALE =
  import.meta.env.VITE_CURRENCY_LOCALE || "en-US";

if (!BILLING_HISTORY_ENDPOINT) {
  console.error(
    "VITE_BILLING_HISTORY_ENDPOINT is not configured."
  );
}

/* ============================================================================
   HELPERS
   ========================================================================== */

function normalizeStatus(
  value: unknown
): InvoiceStatus {
  const normalized = String(
    value ?? ""
  )
    .trim()
    .toUpperCase();

  if (
    INVOICE_STATUSES.includes(
      normalized as InvoiceStatus
    )
  ) {
    return normalized as InvoiceStatus;
  }

  return "PENDING";
}

function normalizeAmount(
  value: unknown
): number {
  const amount =
    typeof value === "number"
      ? value
      : Number(value);

  if (!Number.isFinite(amount)) {
    return 0;
  }

  return amount;
}

function normalizeInvoice(
  value: unknown
): Invoice | null {
  if (
    !value ||
    typeof value !== "object"
  ) {
    return null;
  }

  const data =
    value as Record<
      string,
      unknown
    >;

  const id = String(
    data.id ??
      data.invoiceId ??
      ""
  ).trim();

  const invoiceNumber =
    String(
      data.invoiceNumber ??
        data.number ??
        data.reference ??
        id
    ).trim();

  if (!id || !invoiceNumber) {
    return null;
  }

  return {
    id,

    invoiceNumber,

    date: String(
      data.date ??
        data.invoiceDate ??
        data.createdAt ??
        ""
    ),

    amount: normalizeAmount(
      data.amount ??
        data.total ??
        data.totalAmount
    ),

    currency:
      String(
        data.currency ??
          data.currencyCode ??
          ""
      ).trim() || "USD",

    status:
      normalizeStatus(
        data.status
      ),

    description:
      String(
        data.description ??
          data.planName ??
          data.productName ??
          ""
      ).trim(),

    downloadUrl:
      data.downloadUrl
        ? String(
            data.downloadUrl
          )
        : undefined,
  };
}

function normalizeBillingResponse(
  payload: unknown
): BillingResponse {
  if (Array.isArray(payload)) {
    return {
      invoices:
        payload
          .map(normalizeInvoice)
          .filter(
            (
              invoice
            ): invoice is Invoice =>
              invoice !== null
          ),
    };
  }

  if (
    !payload ||
    typeof payload !== "object"
  ) {
    return {
      invoices: [],
    };
  }

  const data =
    payload as Record<
      string,
      unknown
    >;

  const rawInvoices =
    Array.isArray(
      data.invoices
    )
      ? data.invoices
      : Array.isArray(
          data.data
        )
      ? data.data
      : Array.isArray(
          data.items
        )
      ? data.items
      : [];

  return {
    invoices:
      rawInvoices
        .map(normalizeInvoice)
        .filter(
          (
            invoice
          ): invoice is Invoice =>
            invoice !== null
        ),

    total:
      Number.isFinite(
        Number(data.total)
      )
        ? Number(data.total)
        : undefined,
  };
}

function formatDate(
  value: string
): string {
  if (!value) {
    return "—";
  }

  const date =
    new Date(value);

  if (
    Number.isNaN(
      date.getTime()
    )
  ) {
    return value;
  }

  return new Intl.DateTimeFormat(
    undefined,
    {
      dateStyle: "medium",
    }
  ).format(date);
}

function formatCurrency(
  amount: number,
  currency: string
): string {
  try {
    return new Intl.NumberFormat(
      CURRENCY_LOCALE,
      {
        style: "currency",
        currency:
          currency || "USD",
      }
    ).format(amount);
  } catch {
    return `${currency} ${amount.toFixed(
      2
    )}`;
  }
}

function getStatusLabel(
  status: InvoiceStatus
): string {
  return status
    .replace(/_/g, " ")
    .replace(
      /\b\w/g,
      (letter) =>
        letter.toUpperCase()
    );
}

function getStatusClasses(
  status: InvoiceStatus
): string {
  switch (status) {
    case "PAID":
      return "border-emerald-500/30 bg-emerald-500/10 text-emerald-300";

    case "PENDING":
      return "border-amber-500/30 bg-amber-500/10 text-amber-300";

    case "FAILED":
      return "border-red-500/30 bg-red-500/10 text-red-300";

    case "CANCELLED":
      return "border-white/15 bg-white/5 text-slate-300";

    default:
      return "border-white/15 bg-white/5 text-slate-300";
  }
}

function getStatusIcon(
  status: InvoiceStatus
) {
  switch (status) {
    case "PAID":
      return CheckCircle2;

    case "PENDING":
      return Clock3;

    case "FAILED":
      return XCircle;

    case "CANCELLED":
      return XCircle;

    default:
      return Clock3;
  }
}

/* ============================================================================
   PAGE
   ========================================================================== */

export default function BillingHistoryPage() {
  const [invoices, setInvoices] =
    useState<Invoice[]>([]);

  const [search, setSearch] =
    useState("");

  const [statusFilter, setStatusFilter] =
    useState<
      "ALL" | InvoiceStatus
    >("ALL");

  const [loading, setLoading] =
    useState(true);

  const [refreshing, setRefreshing] =
    useState(false);

  const [error, setError] =
    useState<string | null>(null);

  const [
    downloadingInvoiceId,
    setDownloadingInvoiceId,
  ] = useState<string | null>(null);

  /* ==========================================================================
     LOAD BILLING HISTORY
     ======================================================================== */

  const loadInvoices =
    useCallback(
      async (
        signal?: AbortSignal
      ) => {
        if (
          !BILLING_HISTORY_ENDPOINT
        ) {
          setError(
            "Billing history API endpoint is not configured."
          );

          setLoading(false);
          setRefreshing(false);

          return;
        }

        try {
          setError(null);

          const response =
            await fetch(
              BILLING_HISTORY_ENDPOINT,
              {
                method: "GET",

                headers: {
                  Accept:
                    "application/json",
                },

                credentials:
                  "include",

                signal,
              }
            );

          if (!response.ok) {
            let message =
              `Unable to load billing history (${response.status}).`;

            try {
              const body =
                (await response.json()) as ApiErrorResponse;

              message =
                body.detail ??
                body.message ??
                body.error ??
                message;
            } catch {
              // Preserve HTTP fallback.
            }

            throw new Error(
              message
            );
          }

          const payload =
            await response.json();

          const normalized =
            normalizeBillingResponse(
              payload
            );

          setInvoices(
            normalized.invoices
          );
        } catch (requestError) {
          if (
            requestError instanceof
              DOMException &&
            requestError.name ===
              "AbortError"
          ) {
            return;
          }

          const message =
            requestError instanceof
            Error
              ? requestError.message
              : "An unexpected error occurred while loading billing history.";

          setError(message);
        } finally {
          setLoading(false);
          setRefreshing(false);
        }
      },
      []
    );

  /* ==========================================================================
     INITIAL LOAD
     ======================================================================== */

  useEffect(() => {
    const controller =
      new AbortController();

    void loadInvoices(
      controller.signal
    );

    return () =>
      controller.abort();
  }, [loadInvoices]);

  /* ==========================================================================
     FILTERED INVOICES
     ======================================================================== */

  const filteredInvoices =
    useMemo(() => {
      const normalizedSearch =
        search
          .trim()
          .toLowerCase();

      return invoices.filter(
        (invoice) => {
          const matchesSearch =
            !normalizedSearch ||
            invoice.invoiceNumber
              .toLowerCase()
              .includes(
                normalizedSearch
              ) ||
            invoice.description
              .toLowerCase()
              .includes(
                normalizedSearch
              ) ||
            invoice.currency
              .toLowerCase()
              .includes(
                normalizedSearch
              );

          const matchesStatus =
            statusFilter === "ALL" ||
            invoice.status ===
              statusFilter;

          return (
            matchesSearch &&
            matchesStatus
          );
        }
      );
    }, [
      invoices,
      search,
      statusFilter,
    ]);

  /* ==========================================================================
     REFRESH
     ======================================================================== */

  const handleRefresh =
    async () => {
      setRefreshing(true);

      await loadInvoices();
    };

  /* ==========================================================================
     DOWNLOAD INVOICE
     ======================================================================== */

  const handleDownload =
    async (
      invoice: Invoice
    ) => {
      try {
        setDownloadingInvoiceId(
          invoice.id
        );

        /*
         * Prefer a URL supplied securely
         * by the backend.
         */
        if (
          invoice.downloadUrl
        ) {
          window.open(
            invoice.downloadUrl,
            "_blank",
            "noopener,noreferrer"
          );

          return;
        }

        if (
          !INVOICE_DOWNLOAD_ENDPOINT
        ) {
          throw new Error(
            "Invoice download endpoint is not configured."
          );
        }

        /*
         * Supports endpoints such as:
         *
         * /api/billing/invoices/{id}/download
         *
         * The backend remains responsible
         * for authentication and authorization.
         */
        const endpoint =
          INVOICE_DOWNLOAD_ENDPOINT.replace(
            /\/$/,
            ""
          ).replace(
            "{id}",
            encodeURIComponent(
              invoice.id
            )
          );

        const response =
          await fetch(endpoint, {
            method: "GET",

            headers: {
              Accept:
                "application/pdf",
            },

            credentials:
              "include",
          });

        if (!response.ok) {
          throw new Error(
            `Unable to download invoice (${response.status}).`
          );
        }

        const blob =
          await response.blob();

        const objectUrl =
          URL.createObjectURL(
            blob
          );

        const anchor =
          document.createElement(
            "a"
          );

        anchor.href =
          objectUrl;

        anchor.download =
          `${invoice.invoiceNumber}.pdf`;

        document.body.appendChild(
          anchor
        );

        anchor.click();

        anchor.remove();

        URL.revokeObjectURL(
          objectUrl
        );
      } catch (downloadError) {
        const message =
          downloadError instanceof
          Error
            ? downloadError.message
            : "Unable to download invoice.";

        setError(message);
      } finally {
        setDownloadingInvoiceId(
          null
        );
      }
    };

  /* ==========================================================================
     SUMMARY
     ======================================================================== */

  const paidCount =
    invoices.filter(
      (invoice) =>
        invoice.status ===
        "PAID"
    ).length;

  const pendingCount =
    invoices.filter(
      (invoice) =>
        invoice.status ===
        "PENDING"
    ).length;

  /* ==========================================================================
     RENDER
     ======================================================================== */

  return (
    <main className="min-h-screen p-4 text-slate-100 sm:p-6">
      <div className="w-full">

        {/* ================================================================
            HEADER
        ================================================================= */}

        <section className="mb-8">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <div className="mb-3 inline-flex items-center gap-2 rounded-full border border-[#C6A15B]/30 bg-[#C6A15B]/10 px-3 py-1.5 text-sm font-semibold text-[#C6A15B]">
                <Receipt
                  size={16}
                  aria-hidden="true"
                />

                Billing
              </div>

              <h1 className="text-3xl font-black tracking-tight text-white sm:text-4xl">
                Billing History
              </h1>

              <p className="mt-3 max-w-2xl text-slate-300">
                View your invoices,
                payment records, and
                downloadable billing
                documents.
              </p>
            </div>

            <button
              type="button"
              onClick={
                handleRefresh
              }
              disabled={
                loading ||
                refreshing
              }
              className="inline-flex items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-[#C6A15B] to-[#A8894D] px-5 py-3 font-semibold text-[#071426] transition hover:-translate-y-0.5 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <RefreshCw
                size={18}
                className={
                  refreshing
                    ? "animate-spin"
                    : ""
                }
                aria-hidden="true"
              />

              Refresh
            </button>
          </div>
        </section>

        {/* ================================================================
            ERROR
        ================================================================= */}

        {error && (
          <div
            role="alert"
            className="mb-8 flex flex-col gap-4 rounded-2xl border border-red-500/30 bg-red-500/10 p-5 text-red-200 sm:flex-row sm:items-center sm:justify-between"
          >
            <div className="flex items-start gap-3">
              <AlertTriangle
                className="mt-0.5 shrink-0 text-red-300"
                size={20}
                aria-hidden="true"
              />

              <div>
                <p className="font-bold text-red-100">
                  Unable to load billing
                  information
                </p>

                <p className="mt-1 text-sm">
                  {error}
                </p>
              </div>
            </div>

            <button
              type="button"
              onClick={
                handleRefresh
              }
              className="rounded-lg bg-red-500/20 px-4 py-2 text-sm font-bold text-red-200 hover:bg-red-500/30"
            >
              Try Again
            </button>
          </div>
        )}

        {/* ================================================================
            SUMMARY CARDS
        ================================================================= */}

        <div className="mb-8 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">

          <SummaryCard
            icon={FileText}
            title="Total Invoices"
            value={
              loading
                ? "—"
                : String(
                    invoices.length
                  )
            }
          />

          <SummaryCard
            icon={
              CheckCircle2
            }
            title="Paid"
            value={
              loading
                ? "—"
                : String(
                    paidCount
                  )
            }
          />

          <SummaryCard
            icon={Clock3}
            title="Pending"
            value={
              loading
                ? "—"
                : String(
                    pendingCount
                  )
            }
          />

        </div>

        {/* ================================================================
            FILTERS
        ================================================================= */}

        <section className="mb-8 rounded-3xl border border-white/10 bg-white/5 backdrop-blur-xl p-5 shadow-sm">
          <div className="grid gap-4 lg:grid-cols-[1fr_260px]">

            <div className="relative">
              <Search
                size={18}
                className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400"
                aria-hidden="true"
              />

              <label
                htmlFor="invoice-search"
                className="sr-only"
              >
                Search invoices
              </label>

              <input
                id="invoice-search"
                type="search"
                value={search}
                onChange={(
                  event
                ) =>
                  setSearch(
                    event.target
                      .value
                  )
                }
                placeholder="Search invoices..."
                autoComplete="off"
                className="w-full rounded-2xl border border-white/15 bg-white/5 py-3 pl-11 pr-4 text-white outline-none transition placeholder:text-slate-500 focus:border-[#C6A15B] focus:ring-2 focus:ring-[#C6A15B]/20"
              />
            </div>

            <div className="relative">
              <Filter
                size={18}
                className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400"
                aria-hidden="true"
              />

              <label
                htmlFor="invoice-status"
                className="sr-only"
              >
                Filter invoices by
                status
              </label>

              <select
                id="invoice-status"
                value={
                  statusFilter
                }
                onChange={(
                  event
                ) =>
                  setStatusFilter(
                    event.target
                      .value as
                      | "ALL"
                      | InvoiceStatus
                  )
                }
                className="w-full appearance-none rounded-2xl border border-white/15 bg-white/5 py-3 pl-11 pr-4 text-white outline-none transition focus:border-[#C6A15B] focus:ring-2 focus:ring-[#C6A15B]/20"
              >
                <option value="ALL">
                  All Statuses
                </option>

                {INVOICE_STATUSES.map(
                  (status) => (
                    <option
                      key={status}
                      value={status}
                    >
                      {getStatusLabel(
                        status
                      )}
                    </option>
                  )
                )}
              </select>
            </div>

          </div>
        </section>

        {/* ================================================================
            BILLING TABLE
        ================================================================= */}

        <section className="overflow-hidden rounded-3xl border border-white/10 bg-white/5 backdrop-blur-xl shadow-sm">

          <div className="border-b border-white/10 p-6">
            <h2 className="text-xl font-black text-white">
              Invoices
            </h2>

            <p className="mt-1 text-sm text-slate-400">
              {loading
                ? "Loading invoices..."
                : `${filteredInvoices.length} invoice${
                    filteredInvoices.length ===
                    1
                      ? ""
                      : "s"
                  } shown`}
            </p>
          </div>

          <div className="overflow-x-auto">

            {loading ? (
              <div
                className="flex min-h-[280px] items-center justify-center"
                aria-live="polite"
              >
                <div className="flex items-center gap-3 text-slate-400">
                  <RefreshCw
                    size={20}
                    className="animate-spin"
                    aria-hidden="true"
                  />

                  Loading billing
                  history...
                </div>
              </div>
            ) : filteredInvoices.length ===
              0 ? (
              <div className="flex min-h-[300px] flex-col items-center justify-center p-8 text-center">
                <FileText
                  size={44}
                  className="text-slate-500"
                  aria-hidden="true"
                />

                <h3 className="mt-4 text-lg font-bold text-white">
                  No invoices found
                </h3>

                <p className="mt-2 max-w-md text-sm text-slate-400">
                  No billing records
                  match your current
                  search or status
                  filter.
                </p>
              </div>
            ) : (
              <table className="w-full min-w-[850px]">
                <caption className="sr-only">
                  Billing history
                </caption>

                <thead>
                  <tr className="border-b border-white/10 bg-white/5">
                    <th
                      scope="col"
                      className="px-6 py-4 text-left text-xs font-bold uppercase tracking-wide text-slate-400"
                    >
                      Invoice
                    </th>

                    <th
                      scope="col"
                      className="px-6 py-4 text-left text-xs font-bold uppercase tracking-wide text-slate-400"
                    >
                      Date
                    </th>

                    <th
                      scope="col"
                      className="px-6 py-4 text-left text-xs font-bold uppercase tracking-wide text-slate-400"
                    >
                      Amount
                    </th>

                    <th
                      scope="col"
                      className="px-6 py-4 text-left text-xs font-bold uppercase tracking-wide text-slate-400"
                    >
                      Status
                    </th>

                    <th
                      scope="col"
                      className="px-6 py-4 text-right text-xs font-bold uppercase tracking-wide text-slate-400"
                    >
                      Action
                    </th>
                  </tr>
                </thead>

                <tbody>
                  {filteredInvoices.map(
                    (invoice) => {
                      const StatusIcon =
                        getStatusIcon(
                          invoice.status
                        );

                      const isDownloading =
                        downloadingInvoiceId ===
                        invoice.id;

                      return (
                        <tr
                          key={
                            invoice.id
                          }
                          className="border-b border-white/10 transition hover:bg-white/5"
                        >
                          <td className="px-6 py-5">
                            <div className="flex items-center gap-3">
                              <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-white/10">
                                <FileText
                                  size={
                                    19
                                  }
                                  className="text-[#C6A15B]"
                                  aria-hidden="true"
                                />
                              </div>

                              <div className="min-w-0">
                                <p className="font-bold text-white">
                                  {
                                    invoice.invoiceNumber
                                  }
                                </p>

                                {invoice.description && (
                                  <p className="mt-1 max-w-xs truncate text-xs text-slate-400">
                                    {
                                      invoice.description
                                    }
                                  </p>
                                )}
                              </div>
                            </div>
                          </td>

                          <td className="whitespace-nowrap px-6 py-5 text-slate-300">
                            {formatDate(
                              invoice.date
                            )}
                          </td>

                          <td className="whitespace-nowrap px-6 py-5 font-bold text-white">
                            {formatCurrency(
                              invoice.amount,
                              invoice.currency
                            )}
                          </td>

                          <td className="px-6 py-5">
                            <span
                              className={`inline-flex items-center gap-2 rounded-full border px-3 py-1.5 text-xs font-bold ${getStatusClasses(
                                invoice.status
                              )}`}
                            >
                              <StatusIcon
                                size={
                                  14
                                }
                                aria-hidden="true"
                              />

                              {getStatusLabel(
                                invoice.status
                              )}
                            </span>
                          </td>

                          <td className="px-6 py-5 text-right">
                            <button
                              type="button"
                              onClick={() =>
                                void handleDownload(
                                  invoice
                                )
                              }
                              disabled={
                                isDownloading
                              }
                              aria-label={`Download ${invoice.invoiceNumber}`}
                              className="inline-flex items-center gap-2 rounded-xl border border-white/15 px-4 py-2.5 text-sm font-semibold text-white transition hover:border-[#C6A15B] hover:bg-[#C6A15B]/10 disabled:cursor-not-allowed disabled:opacity-50"
                            >
                              {isDownloading ? (
                                <RefreshCw
                                  size={
                                    17
                                  }
                                  className="animate-spin"
                                  aria-hidden="true"
                                />
                              ) : (
                                <Download
                                  size={
                                    17
                                  }
                                  aria-hidden="true"
                                />
                              )}

                              {isDownloading
                                ? "Downloading..."
                                : "Invoice"}
                            </button>
                          </td>
                        </tr>
                      );
                    }
                  )}
                </tbody>
              </table>
            )}

          </div>
        </section>

      </div>
    </main>
  );
}

/* ============================================================================
   SUMMARY CARD
   ========================================================================== */

function SummaryCard({
  icon: Icon,
  title,
  value,
}: {
  icon: typeof FileText;
  title: string;
  value: string;
}) {
  return (
    <div className="rounded-3xl border border-white/10 bg-white/5 backdrop-blur-xl p-6 shadow-sm">
      <div className="flex items-center justify-between gap-4">

        <div>
          <p className="text-sm font-medium text-slate-400">
            {title}
          </p>

          <p className="mt-2 text-3xl font-black text-white">
            {value}
          </p>
        </div>

        <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-[#C6A15B]/15 text-[#C6A15B]">
          <Icon
            size={22}
            aria-hidden="true"
          />
        </div>

      </div>
    </div>
  );
}