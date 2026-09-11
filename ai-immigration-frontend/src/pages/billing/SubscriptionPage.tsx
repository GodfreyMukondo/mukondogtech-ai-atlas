import {
  useCallback,
  useEffect,
  useMemo,
  useState,
} from "react";
import { motion } from "framer-motion";
import {
  AlertTriangle,
  Calendar,
  Check,
  CheckCircle2,
  CreditCard,
  Loader2,
  RefreshCcw,
  ShieldCheck,
  Sparkles,
  XCircle,
} from "lucide-react";

/* ============================================================================
   TYPES
   ========================================================================== */

const SUBSCRIPTION_STATUSES = [
  "ACTIVE",
  "TRIALING",
  "PAST_DUE",
  "CANCELED",
  "INCOMPLETE",
  "INACTIVE",
] as const;

type SubscriptionStatus =
  (typeof SUBSCRIPTION_STATUSES)[number];

interface SubscriptionPlan {
  id: string;
  name: string;
  description?: string;
  price?: number;
  currency?: string;
  billingInterval?: string;
  features: string[];
}

interface Subscription {
  id: string;
  status: SubscriptionStatus;
  plan: SubscriptionPlan;
  currentPeriodStart?: string | null;
  currentPeriodEnd?: string | null;
  nextBillingDate?: string | null;
  cancelAtPeriodEnd?: boolean;
  autoRenew?: boolean;
  paymentMethod?: {
    type?: string;
    brand?: string;
    last4?: string;
    expiryMonth?: number;
    expiryYear?: number;
  } | null;
}

interface ApiErrorResponse {
  message?: string;
  detail?: string;
  error?: string;
}

/* ============================================================================
   ENVIRONMENT CONFIGURATION
   ========================================================================== */

const API_BASE_URL =
  import.meta.env.VITE_API_URL?.replace(/\/+$/, "") || "";

const SUBSCRIPTION_ENDPOINT =
  import.meta.env.VITE_SUBSCRIPTION_ENDPOINT ||
  `${API_BASE_URL}/api/subscription`;

const UPGRADE_ENDPOINT =
  import.meta.env.VITE_SUBSCRIPTION_UPGRADE_ENDPOINT ||
  `${API_BASE_URL}/api/subscription/upgrade`;

const CANCEL_ENDPOINT =
  import.meta.env.VITE_SUBSCRIPTION_CANCEL_ENDPOINT ||
  `${API_BASE_URL}/api/subscription/cancel`;

const AUTH_MODE =
  import.meta.env.VITE_AUTH_MODE || "credentials";

/*
 * Recommended .env configuration:
 *
 * VITE_API_URL=http://localhost:8080
 * VITE_SUBSCRIPTION_ENDPOINT=/api/subscription
 * VITE_SUBSCRIPTION_UPGRADE_ENDPOINT=/api/subscription/upgrade
 * VITE_SUBSCRIPTION_CANCEL_ENDPOINT=/api/subscription/cancel
 * VITE_AUTH_MODE=credentials
 *
 * For production, use your deployed API configuration.
 */

/* ============================================================================
   HELPERS
   ========================================================================== */

function getFetchCredentials():
  | RequestCredentials
  | undefined {
  if (AUTH_MODE === "credentials") {
    return "include";
  }

  return "omit";
}

function normalizeStatus(
  value: unknown
): SubscriptionStatus {
  const normalized = String(
    value ?? "INACTIVE"
  ).toUpperCase();

  if (
    SUBSCRIPTION_STATUSES.includes(
      normalized as SubscriptionStatus
    )
  ) {
    return normalized as SubscriptionStatus;
  }

  return "INACTIVE";
}

function normalizeFeatures(
  value: unknown
): string[] {
  if (!Array.isArray(value)) {
    return [];
  }

  return value
    .map((feature) => {
      if (
        typeof feature === "string"
      ) {
        return feature.trim();
      }

      if (
        feature &&
        typeof feature === "object"
      ) {
        const item =
          feature as Record<
            string,
            unknown
          >;

        return String(
          item.name ??
            item.title ??
            item.description ??
            ""
        ).trim();
      }

      return "";
    })
    .filter(Boolean);
}

function normalizeSubscription(
  payload: unknown
): Subscription | null {
  if (
    !payload ||
    typeof payload !== "object"
  ) {
    return null;
  }

  const root =
    payload as Record<
      string,
      unknown
    >;

  const rawSubscription =
    root.subscription &&
    typeof root.subscription ===
      "object"
      ? root.subscription
      : root;

  if (
    !rawSubscription ||
    typeof rawSubscription !==
      "object"
  ) {
    return null;
  }

  const data =
    rawSubscription as Record<
      string,
      unknown
    >;

  const rawPlan =
    data.plan &&
    typeof data.plan === "object"
      ? data.plan
      : {};

  const plan =
    rawPlan as Record<
      string,
      unknown
    >;

  const rawPaymentMethod =
    data.paymentMethod &&
    typeof data.paymentMethod ===
      "object"
      ? data.paymentMethod
      : null;

  const paymentMethod =
    rawPaymentMethod as
      | Record<
          string,
          unknown
        >
      | null;

  const id = String(
    data.id ??
      data.subscriptionId ??
      ""
  ).trim();

  if (!id) {
    return null;
  }

  return {
    id,

    status: normalizeStatus(
      data.status
    ),

    plan: {
      id: String(
        plan.id ??
          plan.planId ??
          ""
      ),

      name: String(
        plan.name ??
          plan.planName ??
          ""
      ).trim(),

      description:
        plan.description
          ? String(
              plan.description
            )
          : undefined,

      price:
        plan.price !== undefined &&
        plan.price !== null
          ? Number(plan.price)
          : undefined,

      currency:
        plan.currency
          ? String(
              plan.currency
            ).toUpperCase()
          : undefined,

      billingInterval:
        plan.billingInterval
          ? String(
              plan.billingInterval
            )
          : undefined,

      features:
        normalizeFeatures(
          plan.features ??
            data.features
        ),
    },

    currentPeriodStart:
      data.currentPeriodStart
        ? String(
            data.currentPeriodStart
          )
        : null,

    currentPeriodEnd:
      data.currentPeriodEnd
        ? String(
            data.currentPeriodEnd
          )
        : null,

    nextBillingDate:
      data.nextBillingDate
        ? String(
            data.nextBillingDate
          )
        : null,

    cancelAtPeriodEnd:
      Boolean(
        data.cancelAtPeriodEnd ??
          false
      ),

    autoRenew:
      Boolean(
        data.autoRenew ??
          data.automaticRenewal ??
          false
      ),

    paymentMethod:
      paymentMethod
        ? {
            type:
              paymentMethod.type
                ? String(
                    paymentMethod.type
                  )
                : undefined,

            brand:
              paymentMethod.brand
                ? String(
                    paymentMethod.brand
                  )
                : undefined,

            last4:
              paymentMethod.last4
                ? String(
                    paymentMethod.last4
                  )
                : undefined,

            expiryMonth:
              paymentMethod.expiryMonth !==
              undefined
                ? Number(
                    paymentMethod.expiryMonth
                  )
                : undefined,

            expiryYear:
              paymentMethod.expiryYear !==
              undefined
                ? Number(
                    paymentMethod.expiryYear
                  )
                : undefined,
          }
        : null,
  };
}

function formatCurrency(
  amount: number | undefined,
  currency: string | undefined
): string {
  if (
    amount === undefined ||
    !Number.isFinite(amount) ||
    !currency
  ) {
    return "—";
  }

  try {
    return new Intl.NumberFormat(
      undefined,
      {
        style: "currency",
        currency,
        maximumFractionDigits: 2,
      }
    ).format(amount);
  } catch {
    return `${currency} ${amount.toFixed(
      2
    )}`;
  }
}

function formatDate(
  value?: string | null
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
    return "—";
  }

  return new Intl.DateTimeFormat(
    undefined,
    {
      dateStyle: "medium",
    }
  ).format(date);
}

function formatBillingInterval(
  value?: string
): string {
  if (!value) {
    return "—";
  }

  return value
    .replace(/[_-]/g, " ")
    .replace(
      /\b\w/g,
      (letter) =>
        letter.toUpperCase()
    );
}

function getStatusLabel(
  status: SubscriptionStatus
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
  status: SubscriptionStatus
): string {
  switch (status) {
    case "ACTIVE":
      return "border-emerald-500/30 bg-emerald-500/10 text-emerald-300";

    case "TRIALING":
      return "border-blue-500/30 bg-blue-500/10 text-blue-300";

    case "PAST_DUE":
      return "border-amber-500/30 bg-amber-500/10 text-amber-300";

    case "CANCELED":
      return "border-red-500/30 bg-red-500/10 text-red-300";

    case "INCOMPLETE":
      return "border-orange-500/30 bg-orange-500/10 text-orange-300";

    case "INACTIVE":
    default:
      return "border-white/15 bg-white/5 text-slate-300";
  }
}

function getErrorMessage(
  error: unknown
): string {
  if (error instanceof Error) {
    return error.message;
  }

  return "An unexpected error occurred. Please try again.";
}

/* ============================================================================
   STATUS BADGE
   ========================================================================== */

function StatusBadge({
  status,
}: {
  status: SubscriptionStatus;
}) {
  return (
    <span
      className={`inline-flex items-center gap-2 rounded-full border px-4 py-2 text-sm font-semibold ${getStatusClasses(
        status
      )}`}
    >
      <CheckCircle2
        size={16}
        aria-hidden="true"
      />

      {getStatusLabel(status)}
    </span>
  );
}

/* ============================================================================
   INFORMATION CARD
   ========================================================================== */

function InformationCard({
  icon: Icon,
  label,
  value,
}: {
  icon: React.ElementType;
  label: string;
  value: string;
}) {
  return (
    <div className="rounded-2xl border border-white/10 bg-white/5 p-5">
      <Icon
        size={22}
        className="text-[#C6A15B]"
        aria-hidden="true"
      />

      <p className="mt-3 text-sm text-slate-400">
        {label}
      </p>

      <p className="mt-1 break-words text-lg font-bold text-white">
        {value}
      </p>
    </div>
  );
}

/* ============================================================================
   PAGE
   ========================================================================== */

export default function SubscriptionPage() {
  const [
    subscription,
    setSubscription,
  ] =
    useState<Subscription | null>(
      null
    );

  const [loading, setLoading] =
    useState(true);

  const [
    refreshing,
    setRefreshing,
  ] = useState(false);

  const [
    actionLoading,
    setActionLoading,
  ] = useState<
    "upgrade" | "cancel" | null
  >(null);

  const [error, setError] =
    useState<string | null>(null);

  const [actionError, setActionError] =
    useState<string | null>(null);

  const [successMessage, setSuccessMessage] =
    useState<string | null>(null);

  /* ==========================================================================
     LOAD SUBSCRIPTION
     ======================================================================== */

  const loadSubscription =
    useCallback(
      async (
        signal?: AbortSignal
      ) => {
        if (!SUBSCRIPTION_ENDPOINT) {
          setError(
            "Subscription API endpoint is not configured."
          );

          setLoading(false);
          setRefreshing(false);

          return;
        }

        try {
          setError(null);

          const response =
            await fetch(
              SUBSCRIPTION_ENDPOINT,
              {
                method: "GET",

                headers: {
                  Accept:
                    "application/json",
                },

                credentials:
                  getFetchCredentials(),

                signal,
              }
            );

          if (!response.ok) {
            let message =
              `Unable to load subscription (${response.status}).`;

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
            normalizeSubscription(
              payload
            );

          setSubscription(
            normalized
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

          setError(
            getErrorMessage(
              requestError
            )
          );
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

    void loadSubscription(
      controller.signal
    );

    return () =>
      controller.abort();
  }, [loadSubscription]);

  /* ==========================================================================
     REFRESH
     ======================================================================== */

  const handleRefresh =
    async () => {
      if (loading || refreshing) {
        return;
      }

      setRefreshing(true);
      setSuccessMessage(null);
      setActionError(null);

      await loadSubscription();
    };

  /* ==========================================================================
     API ACTION
     ======================================================================== */

  const performSubscriptionAction =
    async (
      action:
        | "upgrade"
        | "cancel"
    ) => {
      const endpoint =
        action === "upgrade"
          ? UPGRADE_ENDPOINT
          : CANCEL_ENDPOINT;

      if (!endpoint) {
        setActionError(
          "Subscription action endpoint is not configured."
        );

        return;
      }

      try {
        setActionLoading(action);
        setActionError(null);
        setSuccessMessage(null);

        const response =
          await fetch(
            endpoint,
            {
              method: "POST",

              headers: {
                Accept:
                  "application/json",
                "Content-Type":
                  "application/json",
              },

              credentials:
                getFetchCredentials(),

              body: JSON.stringify({
                subscriptionId:
                  subscription?.id,
              }),
            }
          );

        if (!response.ok) {
          let message =
            `Unable to ${action} subscription (${response.status}).`;

          try {
            const body =
              (await response.json()) as ApiErrorResponse;

            message =
              body.detail ??
              body.message ??
              body.error ??
              message;
          } catch {
            // Preserve fallback.
          }

          throw new Error(
            message
          );
        }

        let responsePayload:
          | unknown
          | null = null;

        try {
          responsePayload =
            await response.json();
        } catch {
          responsePayload =
            null;
        }

        const updatedSubscription =
          normalizeSubscription(
            responsePayload
          );

        if (
          updatedSubscription
        ) {
          setSubscription(
            updatedSubscription
          );
        } else {
          await loadSubscription();
        }

        setSuccessMessage(
          action === "upgrade"
            ? "Your subscription upgrade has been initiated successfully."
            : "Your subscription cancellation request has been processed successfully."
        );
      } catch (requestError) {
        setActionError(
          getErrorMessage(
            requestError
          )
        );
      } finally {
        setActionLoading(null);
      }
    };

  /* ==========================================================================
     DERIVED DATA
     ======================================================================== */

  const isActive =
    subscription?.status ===
      "ACTIVE" ||
    subscription?.status ===
      "TRIALING";

  const canUpgrade =
    Boolean(subscription) &&
    actionLoading === null;

  const canCancel =
    Boolean(
      subscription &&
        isActive &&
        !subscription.cancelAtPeriodEnd
    ) &&
    actionLoading === null;

  const price =
    useMemo(
      () =>
        formatCurrency(
          subscription?.plan.price,
          subscription?.plan.currency
        ),
      [
        subscription?.plan.price,
        subscription?.plan.currency,
      ]
    );

  /* ==========================================================================
     RENDER
     ======================================================================== */

  return (
    <main className="min-h-screen p-4 text-slate-100 sm:p-6">
      <div className="w-full">
        {/* ====================================================================
            HEADER
        ================================================================== */}

        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <div className="mb-3 inline-flex items-center gap-2 rounded-full border border-[#C6A15B]/30 bg-[#C6A15B]/10 px-4 py-2 text-sm font-semibold text-[#C6A15B]">
              <Sparkles
                size={16}
                aria-hidden="true"
              />

              Subscription Management
            </div>

            <h1 className="text-3xl font-black text-white sm:text-4xl">
              Subscription
            </h1>

            <p className="mt-2 max-w-2xl text-slate-300">
              Manage your current plan,
              billing information,
              renewal preferences, and
              subscription features.
            </p>
          </div>

          <button
            type="button"
            onClick={
              handleRefresh
            }
            disabled={
              loading ||
              refreshing ||
              actionLoading !== null
            }
            className="inline-flex items-center justify-center gap-2 rounded-xl border border-white/15 bg-white/5 px-4 py-3 font-semibold text-white transition hover:bg-white/10 disabled:cursor-not-allowed disabled:opacity-50"
          >
            <RefreshCcw
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

        {/* ====================================================================
            ERROR
        ================================================================== */}

        {error && (
          <div
            role="alert"
            className="mb-6 flex items-start gap-3 rounded-2xl border border-red-500/30 bg-red-500/10 p-5 text-red-200"
          >
            <AlertTriangle
              className="mt-0.5 shrink-0 text-red-300"
              size={20}
              aria-hidden="true"
            />

            <div>
              <p className="font-bold text-red-100">
                Unable to load subscription
              </p>

              <p className="mt-1 text-sm">
                {error}
              </p>
            </div>
          </div>
        )}

        {/* ====================================================================
            ACTION ERROR
        ================================================================== */}

        {actionError && (
          <div
            role="alert"
            className="mb-6 flex items-start gap-3 rounded-2xl border border-red-500/30 bg-red-500/10 p-5 text-red-200"
          >
            <XCircle
              className="mt-0.5 shrink-0 text-red-300"
              size={20}
              aria-hidden="true"
            />

            <div>
              <p className="font-bold text-red-100">
                Subscription action failed
              </p>

              <p className="mt-1 text-sm">
                {actionError}
              </p>
            </div>
          </div>
        )}

        {/* ====================================================================
            SUCCESS
        ================================================================== */}

        {successMessage && (
          <div
            role="status"
            className="mb-6 flex items-start gap-3 rounded-2xl border border-emerald-500/30 bg-emerald-500/10 p-5 text-emerald-200"
          >
            <CheckCircle2
              className="mt-0.5 shrink-0 text-emerald-300"
              size={20}
              aria-hidden="true"
            />

            <p className="text-sm font-semibold">
              {successMessage}
            </p>
          </div>
        )}

        {/* ====================================================================
            LOADING
        ================================================================== */}

        {loading && (
          <div
            className="flex min-h-[360px] items-center justify-center rounded-3xl border border-white/10 bg-white/5 backdrop-blur-xl shadow-sm"
            aria-live="polite"
          >
            <div className="flex items-center gap-3 text-slate-300">
              <Loader2
                size={24}
                className="animate-spin"
                aria-hidden="true"
              />

              Loading subscription...
            </div>
          </div>
        )}

        {/* ====================================================================
            NO SUBSCRIPTION
        ================================================================== */}

        {!loading &&
          !subscription && (
            <section className="rounded-3xl border border-white/10 bg-white/5 backdrop-blur-xl p-8 text-center shadow-sm sm:p-12">
              <CreditCard
                size={48}
                className="mx-auto text-slate-500"
                aria-hidden="true"
              />

              <h2 className="mt-5 text-2xl font-black text-white">
                No active subscription
              </h2>

              <p className="mx-auto mt-2 max-w-xl text-slate-400">
                Your account does not
                currently have a subscription
                available.
              </p>
            </section>
          )}

        {/* ====================================================================
            SUBSCRIPTION
        ================================================================== */}

        {!loading &&
          subscription && (
            <>
              <motion.section
                initial={{
                  opacity: 0,
                  y: 20,
                }}
                animate={{
                  opacity: 1,
                  y: 0,
                }}
                className="rounded-3xl border border-white/10 bg-white/5 backdrop-blur-xl p-6 shadow-sm sm:p-8"
              >
                <div className="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
                  <div>
                    <p className="text-sm font-semibold uppercase tracking-wider text-slate-400">
                      Current Plan
                    </p>

                    <h2 className="mt-2 text-3xl font-black text-white">
                      {subscription.plan
                        .name || "—"}
                    </h2>

                    {subscription.plan
                      .description && (
                      <p className="mt-2 max-w-2xl text-slate-400">
                        {
                          subscription
                            .plan
                            .description
                        }
                      </p>
                    )}
                  </div>

                  <StatusBadge
                    status={
                      subscription.status
                    }
                  />
                </div>

                {/* --------------------------------------------------------------
                    INFORMATION
                -------------------------------------------------------------- */}

                <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                  <InformationCard
                    icon={
                      CreditCard
                    }
                    label="Price"
                    value={price}
                  />

                  <InformationCard
                    icon={
                      Calendar
                    }
                    label="Next Billing"
                    value={formatDate(
                      subscription.nextBillingDate ??
                        subscription.currentPeriodEnd
                    )}
                  />

                  <InformationCard
                    icon={
                      RefreshCcw
                    }
                    label="Renewal"
                    value={
                      subscription.cancelAtPeriodEnd
                        ? "Ending at period end"
                        : subscription.autoRenew
                        ? "Automatic"
                        : "Manual"
                    }
                  />

                  <InformationCard
                    icon={
                      ShieldCheck
                    }
                    label="Billing Interval"
                    value={formatBillingInterval(
                      subscription.plan
                        .billingInterval
                    )}
                  />
                </div>

                {/* --------------------------------------------------------------
                    PAYMENT METHOD
                -------------------------------------------------------------- */}

                {subscription.paymentMethod && (
                  <div className="mt-6 rounded-2xl border border-white/10 bg-white/5 p-5">
                    <div className="flex items-center gap-3 text-white">
                      <CreditCard
                        size={20}
                        aria-hidden="true"
                      />

                      <h3 className="font-bold">
                        Payment Method
                      </h3>
                    </div>

                    <div className="mt-3 text-sm text-slate-300">
                      {[
                        subscription
                          .paymentMethod
                          .brand,
                        subscription
                          .paymentMethod
                          .last4
                          ? `•••• ${subscription.paymentMethod.last4}`
                          : undefined,
                      ]
                        .filter(
                          Boolean
                        )
                        .join(" ") ||
                        "Payment method information unavailable."}
                    </div>
                  </div>
                )}

                {/* --------------------------------------------------------------
                    PERIOD
                -------------------------------------------------------------- */}

                {(subscription.currentPeriodStart ||
                  subscription.currentPeriodEnd) && (
                  <div className="mt-6 rounded-2xl border border-white/10 p-5">
                    <div className="grid gap-5 sm:grid-cols-2">
                      <div>
                        <p className="text-sm text-slate-400">
                          Current Period
                          Start
                        </p>

                        <p className="mt-1 font-bold text-white">
                          {formatDate(
                            subscription.currentPeriodStart
                          )}
                        </p>
                      </div>

                      <div>
                        <p className="text-sm text-slate-400">
                          Current Period
                          End
                        </p>

                        <p className="mt-1 font-bold text-white">
                          {formatDate(
                            subscription.currentPeriodEnd
                          )}
                        </p>
                      </div>
                    </div>
                  </div>
                )}

                {/* --------------------------------------------------------------
                    ACTIONS
                -------------------------------------------------------------- */}

                <div className="mt-8 flex flex-wrap gap-3">
                  <button
                    type="button"
                    disabled={
                      !canUpgrade
                    }
                    onClick={() =>
                      void performSubscriptionAction(
                        "upgrade"
                      )
                    }
                    className="inline-flex items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-[#C6A15B] to-[#A8894D] px-6 py-3 font-bold text-[#071426] transition hover:-translate-y-0.5 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    {actionLoading ===
                    "upgrade" ? (
                      <Loader2
                        size={18}
                        className="animate-spin"
                        aria-hidden="true"
                      />
                    ) : (
                      <Sparkles
                        size={18}
                        aria-hidden="true"
                      />
                    )}

                    Upgrade Plan
                  </button>

                  <button
                    type="button"
                    disabled={
                      !canCancel
                    }
                    onClick={() =>
                      void performSubscriptionAction(
                        "cancel"
                      )
                    }
                    className="inline-flex items-center justify-center gap-2 rounded-xl border border-red-500/30 bg-red-500/10 px-6 py-3 font-bold text-red-300 transition hover:bg-red-500/20 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    {actionLoading ===
                    "cancel" ? (
                      <Loader2
                        size={18}
                        className="animate-spin"
                        aria-hidden="true"
                      />
                    ) : (
                      <XCircle
                        size={18}
                        aria-hidden="true"
                      />
                    )}

                    {subscription.cancelAtPeriodEnd
                      ? "Cancellation Scheduled"
                      : "Cancel Subscription"}
                  </button>
                </div>

                {subscription.cancelAtPeriodEnd && (
                  <p className="mt-4 text-sm text-amber-300">
                    Your subscription is
                    scheduled to end at the
                    end of the current billing
                    period.
                  </p>
                )}
              </motion.section>

              {/* ================================================================
                  FEATURES
              ================================================================ */}

              <motion.section
                initial={{
                  opacity: 0,
                  y: 20,
                }}
                animate={{
                  opacity: 1,
                  y: 0,
                }}
                transition={{
                  delay: 0.1,
                }}
                className="mt-8 rounded-3xl border border-white/10 bg-white/5 backdrop-blur-xl p-6 shadow-sm sm:p-8"
              >
                <div className="flex items-center gap-3">
                  <div className="rounded-xl bg-[#C6A15B]/15 p-3">
                    <Check
                      className="text-[#C6A15B]"
                      size={20}
                      aria-hidden="true"
                    />
                  </div>

                  <div>
                    <h2 className="text-xl font-black text-white">
                      Included Features
                    </h2>

                    <p className="mt-1 text-sm text-slate-400">
                      Features provided by your
                      current subscription plan.
                    </p>
                  </div>
                </div>

                {subscription.plan
                  .features.length ===
                0 ? (
                  <div className="mt-6 rounded-2xl bg-white/5 p-6 text-center text-sm text-slate-400">
                    No feature information is
                    currently available.
                  </div>
                ) : (
                  <ul className="mt-6 grid gap-3 sm:grid-cols-2">
                    {subscription.plan.features.map(
                      (feature) => (
                        <li
                          key={feature}
                          className="flex items-start gap-3 rounded-2xl border border-white/10 bg-white/5 p-4"
                        >
                          <Check
                            size={18}
                            className="mt-0.5 shrink-0 text-emerald-400"
                            aria-hidden="true"
                          />

                          <span className="text-sm font-medium text-slate-200">
                            {feature}
                          </span>
                        </li>
                      )
                    )}
                  </ul>
                )}
              </motion.section>
            </>
          )}
      </div>
    </main>
  );
}