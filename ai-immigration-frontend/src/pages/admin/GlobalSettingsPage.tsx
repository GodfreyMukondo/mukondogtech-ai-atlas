import React, {
  useCallback,
  useEffect,
  useMemo,
  useState,
} from "react";

import axios from "axios";

import {
  AlertTriangle,
  BrainCircuit,
  CheckCircle2,
  CreditCard,
  Database,
  FileText,
  Globe2,
  Info,
  KeyRound,
  Loader2,
  Mail,
  RefreshCw,
  Save,
  Server,
  Settings,
  ShieldCheck,
  ToggleLeft,
  ToggleRight,
  XCircle,
} from "lucide-react";

/* ============================================================
   ENVIRONMENT CONFIGURATION
============================================================ */

const SETTINGS_API_URL =
  import.meta.env.VITE_ADMIN_SETTINGS_API_URL;

const SETTINGS_UPDATE_API_URL =
  import.meta.env.VITE_ADMIN_SETTINGS_UPDATE_API_URL ||
  SETTINGS_API_URL;

/* ============================================================
   VALIDATION
============================================================ */

if (!SETTINGS_API_URL) {
  console.error(
    "[GlobalSettingsPage] Missing VITE_ADMIN_SETTINGS_API_URL."
  );
}

/* ============================================================
   TYPES
============================================================ */

type SettingKey =
  | "aiEnabled"
  | "autoReview"
  | "fraudDetection"
  | "twoFactor"
  | "securityMonitoring"
  | "documentOCR"
  | "automaticDocumentVerification"
  | "emailNotifications"
  | "adminSecurityAlerts"
  | "payments"
  | "maintenance"
  | "enterpriseApiAccess";

type PlatformSettings = {
  aiEnabled: boolean;
  autoReview: boolean;
  fraudDetection: boolean;

  twoFactor: boolean;
  securityMonitoring: boolean;

  documentOCR: boolean;
  automaticDocumentVerification: boolean;

  emailNotifications: boolean;
  adminSecurityAlerts: boolean;

  payments: boolean;
  maintenance: boolean;
  enterpriseApiAccess: boolean;
};

type PlatformInformation = {
  paymentProvider?: string | null;
  currency?: string | null;
  subscriptionModel?: string | null;
};

type SettingsResponse = {
  settings: PlatformSettings;
  platform?: PlatformInformation;
};

type SettingToggleProps = {
  title: string;
  description: string;
  enabled: boolean;
  disabled?: boolean;
  loading?: boolean;
  onChange: () => void;
  icon: React.ElementType;
};

type SectionProps = {
  title: string;
  description: string;
  icon: React.ElementType;
  children: React.ReactNode;
};

type ApiErrorResponse = {
  message?: string;
  error?: string;
};

/* ============================================================
   DEFAULT STATE
   Used only as an empty-safe UI shape.
   Actual values are loaded from the backend.
============================================================ */

const EMPTY_SETTINGS: PlatformSettings = {
  aiEnabled: false,
  autoReview: false,
  fraudDetection: false,

  twoFactor: false,
  securityMonitoring: false,

  documentOCR: false,
  automaticDocumentVerification: false,

  emailNotifications: false,
  adminSecurityAlerts: false,

  payments: false,
  maintenance: false,
  enterpriseApiAccess: false,
};

/* ============================================================
   AXIOS CLIENT
============================================================ */

const settingsApi = axios.create({
  timeout: 30_000,
  headers: {
    Accept: "application/json",
    "Content-Type": "application/json",
  },
});

/* ============================================================
   API ERROR HELPER
============================================================ */

function getApiErrorMessage(
  error: unknown,
  fallback: string
): string {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    return (
      error.response?.data?.message ||
      error.response?.data?.error ||
      error.message ||
      fallback
    );
  }

  if (error instanceof Error) {
    return error.message;
  }

  return fallback;
}

/* ============================================================
   RESPONSE NORMALIZER
============================================================ */

function normalizeSettingsResponse(
  response: unknown
): SettingsResponse {
  const payload = response as
    | SettingsResponse
    | PlatformSettings
    | {
        data?: SettingsResponse | PlatformSettings;
      };

  const raw =
    "data" in payload && payload.data
      ? payload.data
      : payload;

  if (
    raw &&
    typeof raw === "object" &&
    "settings" in raw &&
    raw.settings
  ) {
    return {
      settings: {
        ...EMPTY_SETTINGS,
        ...raw.settings,
      },
      platform:
        "platform" in raw
          ? raw.platform
          : undefined,
    };
  }

  return {
    settings: {
      ...EMPTY_SETTINGS,
      ...(raw as Partial<PlatformSettings>),
    },
  };
}

/* ============================================================
   STATEMENT HELPER
============================================================ */

function hasSettingsChanged(
  current: PlatformSettings,
  original: PlatformSettings
): boolean {
  return (
    current.aiEnabled !== original.aiEnabled ||
    current.autoReview !== original.autoReview ||
    current.fraudDetection !== original.fraudDetection ||
    current.twoFactor !== original.twoFactor ||
    current.securityMonitoring !==
      original.securityMonitoring ||
    current.documentOCR !== original.documentOCR ||
    current.automaticDocumentVerification !==
      original.automaticDocumentVerification ||
    current.emailNotifications !==
      original.emailNotifications ||
    current.adminSecurityAlerts !==
      original.adminSecurityAlerts ||
    current.payments !== original.payments ||
    current.maintenance !== original.maintenance ||
    current.enterpriseApiAccess !==
      original.enterpriseApiAccess
  );
}

/* ============================================================
   SETTING TOGGLE
============================================================ */

function SettingToggle({
  title,
  description,
  enabled,
  disabled = false,
  loading = false,
  onChange,
  icon: Icon,
}: SettingToggleProps) {
  return (
    <div
      className="
        flex
        items-center
        justify-between
        gap-5
        rounded-2xl
        border
        border-white/10
        bg-white/5
        backdrop-blur-xl
        p-5
        transition-all
        duration-200
        hover:border-white/20
        hover:shadow-md
      "
    >
      <div className="flex min-w-0 items-start gap-4">
        <div
          className="
            flex
            h-11
            w-11
            shrink-0
            items-center
            justify-center
            rounded-xl
            bg-[#C6A15B]/15
            text-[#C6A15B]
            shadow-sm
          "
        >
          <Icon size={21} strokeWidth={2} />
        </div>

        <div className="min-w-0">
          <h3
            className="
              font-bold
              text-white
            "
          >
            {title}
          </h3>

          <p
            className="
              mt-1
              text-sm
              leading-6
              text-slate-400
            "
          >
            {description}
          </p>
        </div>
      </div>

      <button
        type="button"
        role="switch"
        aria-checked={enabled}
        aria-label={`${title}: ${
          enabled ? "enabled" : "disabled"
        }`}
        disabled={disabled || loading}
        onClick={onChange}
        className="
          shrink-0
          rounded-xl
          transition
          focus:outline-none
          focus:ring-2
          focus:ring-[#C6A15B]
          focus:ring-offset-2
          disabled:cursor-not-allowed
          disabled:opacity-50
        "
      >
        {loading ? (
          <Loader2
            size={34}
            className="animate-spin text-slate-400"
          />
        ) : enabled ? (
          <ToggleRight
            size={42}
            strokeWidth={1.8}
            className="text-emerald-400"
          />
        ) : (
          <ToggleLeft
            size={42}
            strokeWidth={1.8}
            className="text-slate-400"
          />
        )}
      </button>
    </div>
  );
}

/* ============================================================
   SECTION
============================================================ */

function Section({
  title,
  description,
  icon: Icon,
  children,
}: SectionProps) {
  return (
    <section
      className="
        rounded-3xl
        border
        border-white/10
        bg-white/5
        backdrop-blur-xl
        p-6
        shadow-sm
        transition-all
        duration-300
        hover:shadow-lg
        sm:p-7
      "
    >
      <div
        className="
          mb-6
          flex
          items-start
          gap-4
        "
      >
        <div
          className="
            flex
            h-12
            w-12
            shrink-0
            items-center
            justify-center
            rounded-2xl
            bg-gradient-to-br
            from-[#071426]
            via-[#10254D]
            to-[#24558F]
            shadow-lg
          "
        >
          <Icon
            size={23}
            strokeWidth={2}
            className="text-[#C6A15B]"
          />
        </div>

        <div className="min-w-0">
          <h2
            className="
              text-xl
              font-black
              tracking-tight
              text-white
            "
          >
            {title}
          </h2>

          <p
            className="
              mt-1
              text-sm
              leading-6
              text-slate-400
            "
          >
            {description}
          </p>
        </div>
      </div>

      {children}
    </section>
  );
}

/* ============================================================
   INFORMATION CARD
============================================================ */

function InformationCard({
  label,
  value,
  icon: Icon,
}: {
  label: string;
  value?: string | null;
  icon: React.ElementType;
}) {
  return (
    <div
      className="
        rounded-2xl
        border
        border-white/10
        bg-white/5
        p-5
      "
    >
      <div className="flex items-center gap-3">
        <div
          className="
            flex
            h-10
            w-10
            items-center
            justify-center
            rounded-xl
            bg-blue-500/10
            text-blue-300
          "
        >
          <Icon size={19} />
        </div>

        <p className="text-sm font-medium text-slate-400">
          {label}
        </p>
      </div>

      <p
        className="
          mt-4
          break-words
          text-lg
          font-black
          text-white
        "
      >
        {value || "Not configured"}
      </p>
    </div>
  );
}

/* ============================================================
   MAIN PAGE
============================================================ */

export default function GlobalSettingsPage() {
  const [settings, setSettings] =
    useState<PlatformSettings>(EMPTY_SETTINGS);

  const [originalSettings, setOriginalSettings] =
    useState<PlatformSettings>(EMPTY_SETTINGS);

  const [platform, setPlatform] =
    useState<PlatformInformation>({});

  const [loading, setLoading] =
    useState(true);

  const [saving, setSaving] =
    useState(false);

  const [error, setError] =
    useState<string | null>(null);

  const [success, setSuccess] =
    useState<string | null>(null);

  const [lastUpdated, setLastUpdated] =
    useState<string | null>(null);

  /* ==========================================================
     LOAD SETTINGS
  ========================================================== */

  const loadSettings = useCallback(
    async (showLoader = true) => {
      if (!SETTINGS_API_URL) {
        setError(
          "Settings API URL is not configured. Set VITE_ADMIN_SETTINGS_API_URL."
        );
        setLoading(false);
        return;
      }

      if (showLoader) {
        setLoading(true);
      }

      setError(null);

      try {
        const response =
          await settingsApi.get(
            SETTINGS_API_URL
          );

        const normalized =
          normalizeSettingsResponse(
            response.data
          );

        setSettings(normalized.settings);

        setOriginalSettings(
          normalized.settings
        );

        setPlatform(
          normalized.platform ?? {}
        );

        setLastUpdated(
          new Date().toISOString()
        );
      } catch (requestError) {
        setError(
          getApiErrorMessage(
            requestError,
            "Unable to load platform settings."
          )
        );
      } finally {
        setLoading(false);
      }
    },
    []
  );

  /* ==========================================================
     INITIAL LOAD
  ========================================================== */

  useEffect(() => {
    void loadSettings();
  }, [loadSettings]);

  /* ==========================================================
     TOGGLE
  ========================================================== */

  const toggle = useCallback(
    (key: SettingKey) => {
      setSettings((current) => ({
        ...current,
        [key]: !current[key],
      }));

      setSuccess(null);
      setError(null);
    },
    []
  );

  /* ==========================================================
     DIRTY STATE
  ========================================================== */

  const hasChanges = useMemo(
    () =>
      hasSettingsChanged(
        settings,
        originalSettings
      ),
    [settings, originalSettings]
  );

  /* ==========================================================
     RESET CHANGES
  ========================================================== */

  const resetChanges = useCallback(() => {
    setSettings(originalSettings);
    setError(null);
    setSuccess(null);
  }, [originalSettings]);

  /* ==========================================================
     SAVE SETTINGS
  ========================================================== */

  const saveSettings = useCallback(
    async () => {
      if (!SETTINGS_UPDATE_API_URL) {
        setError(
          "Settings update API URL is not configured."
        );
        return;
      }

      if (!hasChanges || saving) {
        return;
      }

      setSaving(true);
      setError(null);
      setSuccess(null);

      try {
        const response =
          await settingsApi.put(
            SETTINGS_UPDATE_API_URL,
            settings
          );

        const normalized =
          normalizeSettingsResponse(
            response.data
          );

        const savedSettings =
          normalized.settings;

        setSettings(savedSettings);

        setOriginalSettings(
          savedSettings
        );

        if (normalized.platform) {
          setPlatform(
            normalized.platform
          );
        }

        setLastUpdated(
          new Date().toISOString()
        );

        setSuccess(
          "Platform settings have been successfully updated."
        );
      } catch (requestError) {
        setError(
          getApiErrorMessage(
            requestError,
            "Unable to save platform settings."
          )
        );
      } finally {
        setSaving(false);
      }
    },
    [
      hasChanges,
      saving,
      settings,
    ]
  );

  /* ==========================================================
     RENDER
  ========================================================== */

  return (
    <div
      className="
        min-h-screen
        p-4
        sm:p-6
        lg:p-8
      "
    >
      <div
        className="
          w-full
          space-y-8
        "
      >
        {/* ====================================================
            HEADER
        ==================================================== */}

        <header
          className="
            overflow-hidden
            rounded-[2rem]
            bg-gradient-to-br
            from-[#050D21]
            via-[#0B1F3A]
            to-[#24558F]
            text-white
            shadow-2xl
          "
        >
          <div
            className="
              flex
              flex-col
              gap-6
              p-6
              sm:p-8
              lg:flex-row
              lg:items-center
              lg:justify-between
            "
          >
            <div className="min-w-0">
              <div
                className="
                  mb-4
                  flex
                  items-center
                  gap-3
                "
              >
                <div
                  className="
                    flex
                    h-12
                    w-12
                    items-center
                    justify-center
                    rounded-2xl
                    bg-white/10
                    ring-1
                    ring-white/10
                  "
                >
                  <Settings
                    size={25}
                    className="text-[#C6A15B]"
                  />
                </div>

                <span
                  className="
                    text-xs
                    font-black
                    uppercase
                    tracking-[0.18em]
                    text-blue-100
                  "
                >
                  Platform Administration
                </span>
              </div>

              <h1
                className="
                  text-3xl
                  font-black
                  tracking-tight
                  sm:text-4xl
                "
              >
                Global Platform Settings
              </h1>

              <p
                className="
                  mt-3
                  max-w-3xl
                  text-sm
                  leading-7
                  text-blue-100
                  sm:text-base
                "
              >
                Configure AI services, security,
                document processing, communication,
                payments and platform operations.
              </p>

              {lastUpdated && (
                <p
                  className="
                    mt-3
                    text-xs
                    text-blue-200/80
                  "
                >
                  Settings synchronized with the
                  platform backend.
                </p>
              )}
            </div>

            <div
              className="
                flex
                flex-col
                gap-3
                sm:flex-row
              "
            >
              {hasChanges && (
                <button
                  type="button"
                  onClick={resetChanges}
                  disabled={saving}
                  className="
                    inline-flex
                    items-center
                    justify-center
                    gap-2
                    rounded-2xl
                    border
                    border-white/20
                    bg-white/10
                    px-5
                    py-3
                    font-bold
                    text-white
                    backdrop-blur
                    transition
                    hover:bg-white/15
                    disabled:cursor-not-allowed
                    disabled:opacity-50
                  "
                >
                  <RefreshCw size={18} />

                  Discard Changes
                </button>
              )}

              <button
                type="button"
                onClick={() =>
                  void saveSettings()
                }
                disabled={
                  loading ||
                  saving ||
                  !hasChanges
                }
                className="
                  inline-flex
                  items-center
                  justify-center
                  gap-3
                  rounded-2xl
                  bg-[#C6A15B]
                  px-6
                  py-3
                  font-black
                  text-[#071426]
                  shadow-lg
                  transition-all
                  hover:-translate-y-0.5
                  hover:bg-[#FFD45C]
                  disabled:cursor-not-allowed
                  disabled:opacity-50
                "
              >
                {saving ? (
                  <Loader2
                    size={20}
                    className="animate-spin"
                  />
                ) : (
                  <Save size={20} />
                )}

                {saving
                  ? "Saving..."
                  : "Save Changes"}
              </button>
            </div>
          </div>
        </header>

        {/* ====================================================
            ERROR
        ==================================================== */}

        {error && (
          <div
            role="alert"
            className="
              flex
              flex-col
              gap-4
              rounded-2xl
              border
              border-red-500/30
              bg-red-500/10
              p-4
              text-red-300
              sm:flex-row
              sm:items-center
              sm:justify-between
            "
          >
            <div className="flex items-start gap-3">
              <XCircle
                size={21}
                className="mt-0.5 shrink-0"
              />

              <div>
                <p className="font-bold">
                  Settings error
                </p>

                <p className="mt-1 text-sm">
                  {error}
                </p>
              </div>
            </div>

            <button
              type="button"
              onClick={() =>
                void loadSettings()
              }
              className="
                inline-flex
                shrink-0
                items-center
                justify-center
                gap-2
                rounded-xl
                bg-red-600
                px-4
                py-2.5
                text-sm
                font-bold
                text-white
                transition
                hover:bg-red-700
              "
            >
              <RefreshCw size={16} />

              Retry
            </button>
          </div>
        )}

        {/* ====================================================
            SUCCESS
        ==================================================== */}

        {success && (
          <div
            role="status"
            className="
              flex
              items-center
              gap-3
              rounded-2xl
              border
              border-emerald-500/30
              bg-emerald-500/10
              p-4
              text-sm
              font-bold
              text-emerald-300
            "
          >
            <CheckCircle2 size={21} />

            {success}
          </div>
        )}

        {/* ====================================================
            LOADING
        ==================================================== */}

        {loading ? (
          <div
            className="
              flex
              min-h-[420px]
              items-center
              justify-center
              rounded-3xl
              border
              border-white/10
              bg-white/5
              backdrop-blur-xl
              shadow-sm
            "
          >
            <div className="text-center">
              <Loader2
                size={42}
                className="
                  mx-auto
                  animate-spin
                  text-blue-300
                "
              />

              <p
                className="
                  mt-4
                  font-bold
                  text-white
                "
              >
                Loading platform settings...
              </p>

              <p
                className="
                  mt-1
                  text-sm
                  text-slate-400
                "
              >
                Synchronizing configuration
                with the backend.
              </p>
            </div>
          </div>
        ) : (
          <>
            {/* ==================================================
                AI SETTINGS
            ================================================== */}

            <Section
              title="Artificial Intelligence"
              description="Control AI document analysis and intelligent application processing."
              icon={BrainCircuit}
            >
              <div className="space-y-4">
                <SettingToggle
                  title="AI Document Analysis"
                  description="Enable automated immigration document intelligence."
                  enabled={
                    settings.aiEnabled
                  }
                  onChange={() =>
                    toggle("aiEnabled")
                  }
                  icon={BrainCircuit}
                  disabled={saving}
                />

                <SettingToggle
                  title="Automatic Application Review"
                  description="Allow AI to pre-check applications before human approval."
                  enabled={
                    settings.autoReview
                  }
                  onChange={() =>
                    toggle("autoReview")
                  }
                  icon={FileText}
                  disabled={saving}
                />

                <SettingToggle
                  title="Fraud Detection Engine"
                  description="Detect suspicious documents and application patterns."
                  enabled={
                    settings.fraudDetection
                  }
                  onChange={() =>
                    toggle(
                      "fraudDetection"
                    )
                  }
                  icon={ShieldCheck}
                  disabled={saving}
                />
              </div>
            </Section>

            {/* ==================================================
                SECURITY
            ================================================== */}

            <Section
              title="Security & Compliance"
              description="Manage authentication, monitoring and security protection."
              icon={ShieldCheck}
            >
              <div className="space-y-4">
                <SettingToggle
                  title="Two-Factor Authentication"
                  description="Require additional authentication verification for protected accounts."
                  enabled={
                    settings.twoFactor
                  }
                  onChange={() =>
                    toggle("twoFactor")
                  }
                  icon={KeyRound}
                  disabled={saving}
                />

                <SettingToggle
                  title="Security Monitoring"
                  description="Monitor suspicious activities, authentication events and access attempts."
                  enabled={
                    settings.securityMonitoring
                  }
                  onChange={() =>
                    toggle(
                      "securityMonitoring"
                    )
                  }
                  icon={Server}
                  disabled={saving}
                />
              </div>
            </Section>

            {/* ==================================================
                DOCUMENT PROCESSING
            ================================================== */}

            <Section
              title="Document Processing"
              description="Configure document extraction and verification services."
              icon={Database}
            >
              <div className="space-y-4">
                <SettingToggle
                  title="OCR Document Extraction"
                  description="Extract structured information from uploaded immigration documents."
                  enabled={
                    settings.documentOCR
                  }
                  onChange={() =>
                    toggle("documentOCR")
                  }
                  icon={FileText}
                  disabled={saving}
                />

                <SettingToggle
                  title="Automatic Document Verification"
                  description="Automatically validate uploaded documents against configured verification rules."
                  enabled={
                    settings.automaticDocumentVerification
                  }
                  onChange={() =>
                    toggle(
                      "automaticDocumentVerification"
                    )
                  }
                  icon={CheckCircle2}
                  disabled={saving}
                />
              </div>
            </Section>

            {/* ==================================================
                COMMUNICATION
            ================================================== */}

            <Section
              title="Notifications & Communication"
              description="Control user notifications and administrator security alerts."
              icon={Mail}
            >
              <div className="space-y-4">
                <SettingToggle
                  title="Email Notifications"
                  description="Allow the platform to send configured transactional and system notifications."
                  enabled={
                    settings.emailNotifications
                  }
                  onChange={() =>
                    toggle(
                      "emailNotifications"
                    )
                  }
                  icon={Mail}
                  disabled={saving}
                />

                <SettingToggle
                  title="Administrator Security Alerts"
                  description="Send urgent security events and threat notifications to configured administrators."
                  enabled={
                    settings.adminSecurityAlerts
                  }
                  onChange={() =>
                    toggle(
                      "adminSecurityAlerts"
                    )
                  }
                  icon={AlertTriangle}
                  disabled={saving}
                />
              </div>
            </Section>

            {/* ==================================================
                BILLING
            ================================================== */}

            <Section
              title="Billing & Payments"
              description="View payment and subscription configuration supplied by the backend."
              icon={CreditCard}
            >
              <div className="space-y-5">
                <SettingToggle
                  title="Payment Processing"
                  description="Enable payment processing functionality configured by the platform."
                  enabled={
                    settings.payments
                  }
                  onChange={() =>
                    toggle("payments")
                  }
                  icon={CreditCard}
                  disabled={saving}
                />

                <div
                  className="
                    grid
                    gap-4
                    sm:grid-cols-2
                    lg:grid-cols-3
                  "
                >
                  <InformationCard
                    label="Payment Provider"
                    value={
                      platform.paymentProvider
                    }
                    icon={CreditCard}
                  />

                  <InformationCard
                    label="Currency"
                    value={
                      platform.currency
                    }
                    icon={Globe2}
                  />

                  <InformationCard
                    label="Subscription Model"
                    value={
                      platform.subscriptionModel
                    }
                    icon={Settings}
                  />
                </div>
              </div>
            </Section>

            {/* ==================================================
                PLATFORM
            ================================================== */}

            <Section
              title="Platform Controls"
              description="Manage global platform availability and external integrations."
              icon={Globe2}
            >
              <div className="space-y-4">
                <SettingToggle
                  title="Maintenance Mode"
                  description="Temporarily restrict platform access while maintenance operations are performed."
                  enabled={
                    settings.maintenance
                  }
                  onChange={() =>
                    toggle("maintenance")
                  }
                  icon={Server}
                  disabled={saving}
                />

                <SettingToggle
                  title="Enterprise API Access"
                  description="Allow authorized external systems to access platform APIs."
                  enabled={
                    settings.enterpriseApiAccess
                  }
                  onChange={() =>
                    toggle(
                      "enterpriseApiAccess"
                    )
                  }
                  icon={Globe2}
                  disabled={saving}
                />
              </div>
            </Section>

            {/* ==================================================
                CONFIGURATION NOTICE
            ================================================== */}

            <div
              className="
                flex
                items-start
                gap-4
                rounded-3xl
                border
                border-blue-500/20
                bg-blue-500/10
                p-6
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
                  rounded-xl
                  bg-blue-500/10
                  text-blue-300
                "
              >
                <Info size={20} />
              </div>

              <div>
                <h3
                  className="
                    font-black
                    text-white
                  "
                >
                  Backend-controlled configuration
                </h3>

                <p
                  className="
                    mt-1
                    text-sm
                    leading-6
                    text-slate-300
                  "
                >
                  Platform configuration is retrieved
                  from the backend rather than being
                  embedded in the frontend. Changes are
                  only persisted after selecting
                  <span className="font-bold">
                    {" "}
                    Save Changes
                  </span>
                  .
                </p>
              </div>
            </div>
          </>
        )}

        {/* ====================================================
            UNSAVED CHANGES BAR
        ==================================================== */}

        {!loading && hasChanges && (
          <div
            className="
              sticky
              bottom-4
              z-20
              flex
              flex-col
              gap-4
              rounded-2xl
              border
              border-amber-500/20
              bg-[#1F314A]/95
              p-4
              shadow-xl
              backdrop-blur-xl
              sm:flex-row
              sm:items-center
              sm:justify-between
            "
          >
            <div className="flex items-start gap-3">
              <AlertTriangle
                size={21}
                className="
                  mt-0.5
                  shrink-0
                  text-amber-400
                "
              />

              <div>
                <p className="font-black text-white">
                  Unsaved changes
                </p>

                <p className="mt-0.5 text-sm text-amber-200">
                  Your configuration changes have
                  not been saved yet.
                </p>
              </div>
            </div>

            <div className="flex gap-3">
              <button
                type="button"
                onClick={resetChanges}
                disabled={saving}
                className="
                  rounded-xl
                  border
                  border-white/15
                  bg-white/5
                  px-4
                  py-2.5
                  text-sm
                  font-bold
                  text-slate-200
                  transition
                  hover:bg-white/10
                  disabled:opacity-50
                "
              >
                Discard
              </button>

              <button
                type="button"
                onClick={() =>
                  void saveSettings()
                }
                disabled={saving}
                className="
                  inline-flex
                  items-center
                  gap-2
                  rounded-xl
                  bg-[#071426]
                  px-5
                  py-2.5
                  text-sm
                  font-bold
                  text-white
                  transition
                  hover:bg-[#3C4C61]
                  disabled:cursor-not-allowed
                  disabled:opacity-50
                "
              >
                {saving ? (
                  <Loader2
                    size={16}
                    className="animate-spin"
                  />
                ) : (
                  <Save size={16} />
                )}

                Save
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

