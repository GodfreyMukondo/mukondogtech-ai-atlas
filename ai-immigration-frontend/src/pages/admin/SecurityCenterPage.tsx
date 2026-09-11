import React, {
    useCallback,
    useEffect,
    useMemo,
    useState,
} from "react";

import {
    Activity,
    AlertTriangle,
    CheckCircle2,
    Database,
    Fingerprint,
    Globe2,
    KeyRound,
    LockKeyhole,
    RefreshCcw,
    ScanSearch,
    ShieldAlert,
    ShieldCheck,
    Users,
    Server,
    Cloud,
    FileWarning,
    Loader2,
    XCircle,
} from "lucide-react";

import { toast } from "sonner";

/**
 * ============================================================================
 * TYPES
 * ============================================================================
 */

type SecurityStatus =
    | "SECURE"
    | "WARNING"
    | "CRITICAL"
    | "UNKNOWN";

type SecurityMetric = {
    key: string;
    title: string;
    value: string | number;
    description: string;
    icon: string;
    status?: SecurityStatus;
};

type SecurityControl = {
    name: string;
    value: string;
    status: SecurityStatus;
};

type SecurityAlert = {
    id: string | number;
    title: string;
    message: string;
    severity: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
    createdAt: string;
    resolved?: boolean;
};

type SecurityAction = {
    key: string;
    label: string;
    endpoint?: string;
    method?: "POST" | "GET";
    enabled: boolean;
};

type SecurityDashboardResponse = {
    securityScore?: number | null;
    securityStatus?: SecurityStatus | null;
    securityStatusMessage?: string | null;

    metrics?: SecurityMetric[];

    authentication?: SecurityControl[];

    infrastructure?: SecurityControl[];

    aiProtection?: SecurityControl[];

    alerts?: SecurityAlert[];

    actions?: SecurityAction[];

    lastScanAt?: string | null;

    scanInProgress?: boolean;

    updatedAt?: string | null;
};

/**
 * ============================================================================
 * API CONFIGURATION
 * ============================================================================
 *
 * No security values are hardcoded here.
 *
 * The API base URL comes from the Vite environment configuration.
 *
 * Example:
 *
 * VITE_API_URL=http://localhost:8080/api
 *
 * Production:
 *
 * VITE_API_URL=https://your-production-api.example.com/api
 *
 * Backend endpoints:
 *
 * GET  {VITE_API_URL}/admin/security/dashboard
 * POST {VITE_API_URL}/admin/security/scan
 *
 * ============================================================================
 */

const API_BASE_URL =
    import.meta.env.VITE_API_URL?.replace(/\/+$/, "") || "";

/**
 * ============================================================================
 * API ERROR
 * ============================================================================
 */

class SecurityApiError extends Error {
    status: number;

    constructor(
        message: string,
        status: number
    ) {
        super(message);
        this.name = "SecurityApiError";
        this.status = status;
    }
}

/**
 * ============================================================================
 * API REQUEST
 * ============================================================================
 */

async function securityApiRequest<T>(
    endpoint: string,
    options: RequestInit = {}
): Promise<T> {
    if (!API_BASE_URL) {
        throw new SecurityApiError(
            "VITE_API_URL is not configured.",
            500
        );
    }

    const response = await fetch(
        `${API_BASE_URL}${endpoint}`,
        {
            ...options,
            credentials: "include",
            headers: {
                Accept: "application/json",
                "Content-Type": "application/json",
                ...(options.headers || {}),
            },
        }
    );

    if (!response.ok) {
        let message =
            `Security API request failed with status ${response.status}.`;

        try {
            const errorBody =
                await response.json();

            if (errorBody?.message) {
                message =
                    errorBody.message;
            } else if (errorBody?.error) {
                message =
                    errorBody.error;
            }
        } catch {
            // Keep the default error message.
        }

        throw new SecurityApiError(
            message,
            response.status
        );
    }

    if (response.status === 204) {
        return undefined as T;
    }

    return response.json();
}

/**
 * ============================================================================
 * SECURITY API
 * ============================================================================
 */

async function getSecurityDashboard(): Promise<SecurityDashboardResponse> {
    return securityApiRequest<SecurityDashboardResponse>(
        "/admin/security/dashboard",
        {
            method: "GET",
        }
    );
}

async function runSecurityScan(): Promise<
    SecurityDashboardResponse | void
> {
    return securityApiRequest<
        SecurityDashboardResponse | void
    >(
        "/admin/security/scan",
        {
            method: "POST",
            body: JSON.stringify({}),
        }
    );
}

/**
 * ============================================================================
 * ICON MAP
 * ============================================================================
 */

const ICON_MAP: Record<
    string,
    React.ElementType
> = {
    activity: Activity,
    users: Users,
    shieldAlert: ShieldAlert,
    lock: LockKeyhole,
    fingerprint: Fingerprint,
    database: Database,
    cloud: Cloud,
    server: Server,
    globe: Globe2,
    scan: ScanSearch,
    key: KeyRound,
    shield: ShieldCheck,
    alert: AlertTriangle,
    fileWarning: FileWarning,
};

/**
 * ============================================================================
 * STATUS STYLES
 * ============================================================================
 */

const STATUS_STYLES: Record<
    SecurityStatus,
    {
        badge: string;
        icon: string;
        text: string;
        background: string;
    }
> = {
    SECURE: {
        badge:
            "bg-emerald-500/10 text-emerald-300",
        icon:
            "bg-emerald-500/10 text-emerald-300",
        text:
            "text-emerald-400",
        background:
            "bg-emerald-500/10",
    },

    WARNING: {
        badge:
            "bg-amber-500/10 text-amber-300",
        icon:
            "bg-amber-500/10 text-amber-300",
        text:
            "text-amber-400",
        background:
            "bg-amber-500/10",
    },

    CRITICAL: {
        badge:
            "bg-red-500/10 text-red-300",
        icon:
            "bg-red-500/10 text-red-300",
        text:
            "text-red-400",
        background:
            "bg-red-500/10",
    },

    UNKNOWN: {
        badge:
            "bg-white/10 text-slate-400",
        icon:
            "bg-white/10 text-slate-400",
        text:
            "text-slate-400",
        background:
            "bg-white/10",
    },
};

/**
 * ============================================================================
 * HELPERS
 * ============================================================================
 */

function getIcon(
    iconName?: string
): React.ElementType {
    if (!iconName) {
        return ShieldCheck;
    }

    return (
        ICON_MAP[iconName] ||
        ShieldCheck
    );
}

function normalizeStatus(
    status?: SecurityStatus | null
): SecurityStatus {
    if (
        status === "SECURE" ||
        status === "WARNING" ||
        status === "CRITICAL"
    ) {
        return status;
    }

    return "UNKNOWN";
}

function formatNumber(
    value: string | number | null | undefined
): string {
    if (
        value === null ||
        value === undefined ||
        value === ""
    ) {
        return "—";
    }

    if (typeof value === "number") {
        return value.toLocaleString();
    }

    return value;
}

function formatDateTime(
    value?: string | null
): string {
    if (!value) {
        return "Not available";
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

    return date.toLocaleString(
        undefined,
        {
            year: "numeric",
            month: "short",
            day: "numeric",
            hour: "2-digit",
            minute: "2-digit",
        }
    );
}

/**
 * ============================================================================
 * SECURITY CARD
 * ============================================================================
 */

function SecurityCard({
    title,
    value,
    description,
    icon: Icon,
    color,
}: {
    title: string;
    value: string;
    description: string;
    icon: React.ElementType;
    color: string;
}) {
    return (
        <div
            className="
                group
                rounded-3xl
                border
                border-white/10
                bg-white/5
                backdrop-blur-xl
                p-6
                shadow-sm
                transition
                duration-300
                hover:-translate-y-1
                hover:shadow-xl
            "
        >
            <div className="flex items-start justify-between gap-4">

                <div className="min-w-0">

                    <p className="text-sm font-medium text-slate-400">
                        {title}
                    </p>

                    <h2
                        className="
                            mt-2
                            break-words
                            text-3xl
                            font-black
                            text-white
                        "
                    >
                        {value}
                    </h2>

                    <p
                        className="
                            mt-2
                            text-xs
                            leading-5
                            text-slate-400
                        "
                    >
                        {description}
                    </p>

                </div>

                <div
                    className={`
                        flex
                        h-12
                        w-12
                        shrink-0
                        items-center
                        justify-center
                        rounded-2xl
                        ${color}
                    `}
                >
                    <Icon size={24} />
                </div>

            </div>
        </div>
    );
}

/**
 * ============================================================================
 * SECTION
 * ============================================================================
 */

function Section({
    title,
    children,
    icon: Icon,
}: {
    title: string;
    children: React.ReactNode;
    icon: React.ElementType;
}) {
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
            "
        >

            <div
                className="
                    mb-6
                    flex
                    items-center
                    gap-3
                "
            >

                <div
                    className="
                        rounded-xl
                        bg-[#C6A15B]/15
                        p-2
                        text-[#C6A15B]
                    "
                >
                    <Icon size={20} />
                </div>

                <h2
                    className="
                        text-lg
                        font-black
                        text-white
                    "
                >
                    {title}
                </h2>

            </div>

            {children}

        </section>
    );
}

/**
 * ============================================================================
 * LOADING CARD
 * ============================================================================
 */

function LoadingCard() {
    return (
        <div
            className="
                animate-pulse
                rounded-3xl
                border
                border-white/10
                bg-white/5
                p-6
            "
        >
            <div className="flex justify-between">

                <div className="space-y-3">
                    <div className="h-4 w-28 rounded bg-white/10" />
                    <div className="h-9 w-24 rounded bg-white/10" />
                    <div className="h-3 w-40 rounded bg-white/10" />
                </div>

                <div className="h-12 w-12 rounded-2xl bg-white/10" />

            </div>
        </div>
    );
}

/**
 * ============================================================================
 * CONTROL ROW
 * ============================================================================
 */

function ControlRow({
    control,
}: {
    control: SecurityControl;
}) {
    const status =
        normalizeStatus(
            control.status
        );

    const styles =
        STATUS_STYLES[status];

    return (
        <div
            className="
                flex
                flex-col
                gap-3
                rounded-2xl
                bg-white/5
                p-4
                sm:flex-row
                sm:items-center
                sm:justify-between
            "
        >

            <span className="text-sm text-slate-300">
                {control.name}
            </span>

            <span
                className={`
                    inline-flex
                    w-fit
                    items-center
                    gap-2
                    rounded-full
                    px-3
                    py-1
                    text-xs
                    font-bold
                    ${styles.badge}
                `}
            >
                {status === "SECURE" && (
                    <CheckCircle2 size={13} />
                )}

                {status === "WARNING" && (
                    <AlertTriangle size={13} />
                )}

                {status === "CRITICAL" && (
                    <XCircle size={13} />
                )}

                {status === "UNKNOWN" && (
                    <Activity size={13} />
                )}

                {control.value}
            </span>

        </div>
    );
}

/**
 * ============================================================================
 * ALERT
 * ============================================================================
 */

function SecurityAlertItem({
    alert,
}: {
    alert: SecurityAlert;
}) {
    const severityStyles: Record<
        SecurityAlert["severity"],
        string
    > = {
        LOW:
            "bg-blue-500/10 text-blue-300 border-blue-500/30",

        MEDIUM:
            "bg-amber-500/10 text-amber-300 border-amber-500/30",

        HIGH:
            "bg-orange-500/10 text-orange-300 border-orange-500/30",

        CRITICAL:
            "bg-red-500/10 text-red-300 border-red-500/30",
    };

    return (
        <div
            className={`
                flex
                flex-col
                gap-4
                rounded-2xl
                border
                p-4
                sm:flex-row
                sm:items-start
                ${severityStyles[alert.severity]}
            `}
        >

            <AlertTriangle
                size={21}
                className="mt-0.5 shrink-0"
            />

            <div className="min-w-0 flex-1">

                <div
                    className="
                        flex
                        flex-col
                        gap-2
                        sm:flex-row
                        sm:items-center
                        sm:justify-between
                    "
                >

                    <p className="font-bold">
                        {alert.title}
                    </p>

                    <span
                        className="
                            text-xs
                            font-semibold
                            opacity-70
                        "
                    >
                        {formatDateTime(
                            alert.createdAt
                        )}
                    </span>

                </div>

                <p className="mt-1 text-sm leading-6 opacity-90">
                    {alert.message}
                </p>

            </div>

        </div>
    );
}

/**
 * ============================================================================
 * EMPTY STATE
 * ============================================================================
 */

function EmptyState({
    message,
}: {
    message: string;
}) {
    return (
        <div
            className="
                rounded-2xl
                border
                border-dashed
                border-white/15
                bg-white/5
                p-8
                text-center
            "
        >
            <ShieldCheck
                size={32}
                className="mx-auto text-slate-400"
            />

            <p className="mt-3 text-sm text-slate-400">
                {message}
            </p>
        </div>
    );
}

/**
 * ============================================================================
 * MAIN PAGE
 * ============================================================================
 */

export default function SecurityCenterPage() {
    const [
        dashboard,
        setDashboard,
    ] = useState<SecurityDashboardResponse | null>(
        null
    );

    const [
        loading,
        setLoading,
    ] = useState(true);

    const [
        refreshing,
        setRefreshing,
    ] = useState(false);

    const [
        scanning,
        setScanning,
    ] = useState(false);

    const [
        error,
        setError,
    ] = useState<string | null>(
        null
    );

    /**
     * ========================================================================
     * LOAD DASHBOARD
     * ========================================================================
     */

    const loadDashboard =
        useCallback(
            async (
                showLoading = true
            ) => {
                if (showLoading) {
                    setLoading(true);
                }

                setError(null);

                try {
                    const response =
                        await getSecurityDashboard();

                    setDashboard(
                        response
                    );
                } catch (requestError) {
                    console.error(
                        "Failed to load security dashboard:",
                        requestError
                    );

                    const message =
                        requestError instanceof
                        SecurityApiError
                            ? requestError.message
                            : "Unable to load security dashboard.";

                    setError(
                        message
                    );

                    if (
                        showLoading
                    ) {
                        toast.error(
                            message
                        );
                    }
                } finally {
                    if (showLoading) {
                        setLoading(
                            false
                        );
                    }
                }
            },
            []
        );

    /**
     * ========================================================================
     * INITIAL LOAD
     * ========================================================================
     */

    useEffect(() => {
        void loadDashboard();
    }, [loadDashboard]);

    /**
     * ========================================================================
     * REFRESH
     * ========================================================================
     */

    const handleRefresh =
        async () => {
            if (refreshing || scanning) {
                return;
            }

            setRefreshing(
                true
            );

            try {
                await loadDashboard(
                    false
                );

                toast.success(
                    "Security dashboard refreshed."
                );
            } finally {
                setRefreshing(
                    false
                );
            }
        };

    /**
     * ========================================================================
     * SECURITY SCAN
     * ========================================================================
     */

    const handleSecurityScan =
        async () => {
            if (scanning || refreshing) {
                return;
            }

            setScanning(
                true
            );

            try {
                const response =
                    await runSecurityScan();

                if (
                    response
                ) {
                    setDashboard(
                        response
                    );
                } else {
                    await loadDashboard(
                        false
                    );
                }

                toast.success(
                    "Security scan completed."
                );
            } catch (requestError) {
                console.error(
                    "Security scan failed:",
                    requestError
                );

                const message =
                    requestError instanceof
                    SecurityApiError
                        ? requestError.message
                        : "Unable to run security scan.";

                toast.error(
                    message
                );
            } finally {
                setScanning(
                    false
                );
            }
        };

    /**
     * ========================================================================
     * COMPUTED VALUES
     * ========================================================================
     */

    const securityStatus =
        useMemo(
            () =>
                normalizeStatus(
                    dashboard?.securityStatus
                ),
            [
                dashboard?.securityStatus,
            ]
        );

    const statusStyles =
        STATUS_STYLES[
            securityStatus
        ];

    const metrics =
        dashboard?.metrics ??
        [];

    const authentication =
        dashboard?.authentication ??
        [];

    const infrastructure =
        dashboard?.infrastructure ??
        [];

    const aiProtection =
        dashboard?.aiProtection ??
        [];

    const alerts =
        dashboard?.alerts ??
        [];

    const actions =
        dashboard?.actions ??
        [];

    /**
     * ========================================================================
     * ACTION HANDLER
     * ========================================================================
     */

    const handleAdminAction =
        async (
            action: SecurityAction
        ) => {
            if (
                !action.enabled ||
                !action.endpoint
            ) {
                toast.info(
                    "This security action is not currently available."
                );

                return;
            }

            try {
                await securityApiRequest(
                    action.endpoint,
                    {
                        method:
                            action.method ||
                            "POST",
                        body:
                            action.method ===
                            "GET"
                                ? undefined
                                : JSON.stringify(
                                    {}
                                ),
                    }
                );

                toast.success(
                    `${action.label} completed successfully.`
                );

                await loadDashboard(
                    false
                );
            } catch (requestError) {
                console.error(
                    `Security action failed: ${action.key}`,
                    requestError
                );

                const message =
                    requestError instanceof
                    SecurityApiError
                        ? requestError.message
                        : `Unable to execute ${action.label}.`;

                toast.error(
                    message
                );
            }
        };

    /**
     * ========================================================================
     * RENDER
     * ========================================================================
     */

    return (
        <div
            className="
                min-h-screen
            "
        >

            <div
                className="
                    w-full
                    p-4
                    sm:p-6
                    lg:p-8
                "
            >

                {/* ============================================================
                    HEADER
                   ============================================================ */}

                <div
                    className="
                        mb-8
                        flex
                        flex-col
                        gap-5
                        lg:flex-row
                        lg:items-center
                        lg:justify-between
                    "
                >

                    <div>

                        <div
                            className="
                                flex
                                items-center
                                gap-3
                            "
                        >

                            <div
                                className="
                                    rounded-2xl
                                    bg-[#C6A15B]/15
                                    p-3
                                    shadow-lg
                                "
                            >
                                <ShieldCheck
                                    size={28}
                                    className="text-[#C6A15B]"
                                />
                            </div>

                            <div>

                                <h1
                                    className="
                                        text-3xl
                                        font-black
                                        tracking-tight
                                        text-white
                                        sm:text-4xl
                                    "
                                >
                                    Security Command Center
                                </h1>

                                <p
                                    className="
                                        mt-2
                                        max-w-3xl
                                        text-sm
                                        leading-6
                                        text-slate-300
                                        sm:text-base
                                    "
                                >
                                    Monitor platform security,
                                    authentication,
                                    infrastructure,
                                    AI protection,
                                    fraud detection,
                                    and security alerts.
                                </p>

                            </div>

                        </div>

                    </div>

                    {/* ========================================================
                        HEADER ACTIONS
                        Run Security Scan comes FIRST, followed by Refresh.
                        Both remain on the same horizontal line.
                       ======================================================== */}

                    <div
                        className="
                            flex
                            flex-nowrap
                            items-center
                            justify-end
                            gap-3
                            whitespace-nowrap
                        "
                    >

                        {/* RUN SECURITY SCAN */}

                        <button
                            type="button"
                            onClick={
                                handleSecurityScan
                            }
                            disabled={
                                scanning ||
                                refreshing ||
                                loading
                            }
                            aria-label={
                                scanning
                                    ? "Security scan in progress"
                                    : "Run security scan"
                            }
                            className="
                                inline-flex
                                shrink-0
                                items-center
                                justify-center
                                gap-2
                                rounded-2xl
                                bg-[#071426]
                                px-5
                                py-3
                                text-sm
                                font-bold
                                text-white
                                shadow-lg
                                shadow-[#071426]/20
                                transition-all
                                duration-200
                                hover:-translate-y-0.5
                                hover:bg-[#3C4C61]
                                hover:shadow-xl
                                focus:outline-none
                                focus:ring-2
                                focus:ring-[#C6A15B]
                                focus:ring-offset-2
                                active:translate-y-0
                                disabled:cursor-not-allowed
                                disabled:opacity-60
                                disabled:hover:translate-y-0
                            "
                        >

                            {scanning ? (
                                <Loader2
                                    size={18}
                                    className="animate-spin"
                                />
                            ) : (
                                <ScanSearch
                                    size={18}
                                />
                            )}

                            <span>
                                {scanning
                                    ? "Scanning..."
                                    : "Run Security Scan"}
                            </span>

                        </button>

                        {/* REFRESH */}

                        <button
                            type="button"
                            onClick={
                                handleRefresh
                            }
                            disabled={
                                refreshing ||
                                scanning ||
                                loading
                            }
                            aria-label="Refresh security dashboard"
                            className="
                                inline-flex
                                shrink-0
                                items-center
                                justify-center
                                gap-2
                                rounded-2xl
                                border
                                border-white/15
                                bg-white/5
                                px-5
                                py-3
                                text-sm
                                font-semibold
                                text-slate-200
                                shadow-sm
                                transition-all
                                duration-200
                                hover:-translate-y-0.5
                                hover:border-white/25
                                hover:bg-white/10
                                hover:shadow-md
                                focus:outline-none
                                focus:ring-2
                                focus:ring-[#C6A15B]
                                focus:ring-offset-2
                                active:translate-y-0
                                disabled:cursor-not-allowed
                                disabled:opacity-60
                                disabled:hover:translate-y-0
                            "
                        >

                            <RefreshCcw
                                size={18}
                                className={
                                    refreshing
                                        ? "animate-spin"
                                        : ""
                                }
                            />

                            <span>
                                Refresh
                            </span>

                        </button>

                    </div>

                </div>

                {/* ============================================================
                    ERROR STATE
                   ============================================================ */}

                {error && (
                    <div
                        className="
                            mb-8
                            flex
                            flex-col
                            gap-4
                            rounded-3xl
                            border
                            border-red-500/30
                            bg-red-500/10
                            p-5
                            text-red-300
                            sm:flex-row
                            sm:items-center
                            sm:justify-between
                        "
                    >

                        <div className="flex items-start gap-3">

                            <AlertTriangle
                                size={22}
                                className="mt-0.5 shrink-0"
                            />

                            <div>

                                <p className="font-bold">
                                    Security dashboard unavailable
                                </p>

                                <p className="mt-1 text-sm">
                                    {error}
                                </p>

                            </div>

                        </div>

                        <button
                            type="button"
                            onClick={() =>
                                void loadDashboard()
                            }
                            className="
                                rounded-xl
                                bg-red-500/15
                                px-4
                                py-2
                                text-sm
                                font-bold
                                text-red-200
                                transition
                                hover:bg-red-500/25
                            "
                        >
                            Retry
                        </button>

                    </div>
                )}

                {/* ============================================================
                    SECURITY SCORE
                   ============================================================ */}

                {loading ? (
                    <div
                        className="
                            mb-8
                            h-52
                            animate-pulse
                            rounded-3xl
                            bg-[#071426]/10
                        "
                    />
                ) : (
                    <div
                        className="
                            mb-8
                            overflow-hidden
                            rounded-3xl
                            bg-gradient-to-r
                            from-[#071426]
                            via-[#3C4C61]
                            to-[#0B1F3A]
                            p-6
                            text-white
                            shadow-xl
                            sm:p-8
                        "
                    >

                        <div
                            className="
                                flex
                                flex-col
                                gap-8
                                lg:flex-row
                                lg:items-center
                                lg:justify-between
                            "
                        >

                            <div>

                                <div className="flex items-center gap-2">

                                    <Activity
                                        size={17}
                                        className="text-blue-200"
                                    />

                                    <p
                                        className="
                                            text-sm
                                            font-semibold
                                            text-blue-200
                                        "
                                    >
                                        Overall Security Score
                                    </p>

                                </div>

                                <h2
                                    className="
                                        mt-2
                                        text-5xl
                                        font-black
                                        text-[#C6A15B]
                                        sm:text-6xl
                                    "
                                >
                                    {dashboard?.securityScore !==
                                    null &&
                                    dashboard?.securityScore !==
                                    undefined
                                        ? `${dashboard.securityScore}%`
                                        : "—"}
                                </h2>

                                <p
                                    className="
                                        mt-3
                                        max-w-xl
                                        text-sm
                                        leading-6
                                        text-blue-100
                                    "
                                >
                                    {dashboard?.securityStatusMessage ||
                                        "Security status information is currently unavailable."}
                                </p>

                                {dashboard?.lastScanAt && (
                                    <p
                                        className="
                                            mt-3
                                            text-xs
                                            text-blue-200
                                        "
                                    >
                                        Last scan:{" "}
                                        {formatDateTime(
                                            dashboard.lastScanAt
                                        )}
                                    </p>
                                )}

                            </div>

                            <div
                                className="
                                    flex
                                    flex-col
                                    items-start
                                    gap-3
                                    sm:flex-row
                                    sm:items-center
                                "
                            >

                                <div
                                    className="
                                        rounded-2xl
                                        border
                                        border-white/10
                                        bg-white/10
                                        px-5
                                        py-4
                                        backdrop-blur
                                    "
                                >

                                    <p
                                        className="
                                            text-xs
                                            text-blue-200
                                        "
                                    >
                                        System Status
                                    </p>

                                    <div
                                        className={`
                                            mt-2
                                            inline-flex
                                            items-center
                                            gap-2
                                            rounded-full
                                            px-3
                                            py-1
                                            text-sm
                                            font-bold
                                            ${statusStyles.badge}
                                        `}
                                    >

                                        {securityStatus ===
                                            "SECURE" && (
                                            <CheckCircle2
                                                size={15}
                                            />
                                        )}

                                        {securityStatus ===
                                            "WARNING" && (
                                            <AlertTriangle
                                                size={15}
                                            />
                                        )}

                                        {securityStatus ===
                                            "CRITICAL" && (
                                            <ShieldAlert
                                                size={15}
                                            />
                                        )}

                                        {securityStatus ===
                                            "UNKNOWN" && (
                                            <Activity
                                                size={15}
                                            />
                                        )}

                                        {securityStatus}

                                    </div>

                                </div>

                            </div>

                        </div>

                    </div>
                )}

                {/* ============================================================
                    KPI CARDS
                   ============================================================ */}

                <div
                    className="
                        grid
                        gap-5
                        md:grid-cols-2
                        xl:grid-cols-4
                    "
                >

                    {loading
                        ? Array.from({
                            length: 4,
                        }).map(
                            (_, index) => (
                                <LoadingCard
                                    key={
                                        index
                                    }
                                />
                            )
                        )
                        : metrics.length === 0
                            ? (
                                <div
                                    className="
                                        md:col-span-2
                                        xl:col-span-4
                                    "
                                >
                                    <EmptyState
                                        message="No security metrics were returned by the backend."
                                    />
                                </div>
                            )
                            : metrics.map(
                                metric => {
                                    const Icon =
                                        getIcon(
                                            metric.icon
                                        );

                                    const status =
                                        normalizeStatus(
                                            metric.status
                                        );

                                    return (
                                        <SecurityCard
                                            key={
                                                metric.key
                                            }
                                            title={
                                                metric.title
                                            }
                                            value={formatNumber(
                                                metric.value
                                            )}
                                            description={
                                                metric.description
                                            }
                                            icon={
                                                Icon
                                            }
                                            color={
                                                STATUS_STYLES[
                                                    status
                                                ]
                                                    .icon
                                            }
                                        />
                                    );
                                }
                            )}

                </div>

                {/* ============================================================
                    SECURITY CONTROLS
                   ============================================================ */}

                <div
                    className="
                        mt-8
                        grid
                        gap-6
                        xl:grid-cols-3
                    "
                >

                    <Section
                        title="Authentication Security"
                        icon={KeyRound}
                    >

                        {authentication.length ===
                        0 ? (
                            <EmptyState
                                message="No authentication security controls were returned."
                            />
                        ) : (
                            <div className="space-y-4">

                                {authentication.map(
                                    control => (
                                        <ControlRow
                                            key={
                                                control.name
                                            }
                                            control={
                                                control
                                            }
                                        />
                                    )
                                )}

                            </div>
                        )}

                    </Section>

                    <Section
                        title="Infrastructure Security"
                        icon={Database}
                    >

                        {infrastructure.length ===
                        0 ? (
                            <EmptyState
                                message="No infrastructure security controls were returned."
                            />
                        ) : (
                            <div className="space-y-4">

                                {infrastructure.map(
                                    control => (
                                        <ControlRow
                                            key={
                                                control.name
                                            }
                                            control={
                                                control
                                            }
                                        />
                                    )
                                )}

                            </div>
                        )}

                    </Section>

                    <Section
                        title="AI Fraud Protection"
                        icon={ScanSearch}
                    >

                        {aiProtection.length ===
                        0 ? (
                            <EmptyState
                                message="No AI security controls were returned."
                            />
                        ) : (
                            <div className="space-y-4">

                                {aiProtection.map(
                                    control => (
                                        <ControlRow
                                            key={
                                                control.name
                                            }
                                            control={
                                                control
                                            }
                                        />
                                    )
                                )}

                            </div>
                        )}

                    </Section>

                </div>

                {/* ============================================================
                    ALERTS
                   ============================================================ */}

                <div className="mt-8">

                    <Section
                        title="Security Alerts"
                        icon={AlertTriangle}
                    >

                        {alerts.length ===
                        0 ? (
                            <EmptyState
                                message="No security alerts were returned by the backend."
                            />
                        ) : (
                            <div className="space-y-4">

                                {alerts.map(
                                    alert => (
                                        <SecurityAlertItem
                                            key={
                                                alert.id
                                            }
                                            alert={
                                                alert
                                            }
                                        />
                                    )
                                )}

                            </div>
                        )}

                    </Section>

                </div>

                {/* ============================================================
                    ADMINISTRATION
                   ============================================================ */}

                <div className="mt-8">

                    <Section
                        title="Security Administration"
                        icon={ShieldCheck}
                    >

                        {actions.length ===
                        0 ? (
                            <EmptyState
                                message="No security administration actions are currently available."
                            />
                        ) : (
                            <div
                                className="
                                    grid
                                    gap-4
                                    sm:grid-cols-2
                                    xl:grid-cols-4
                                "
                            >

                                {actions.map(
                                    action => (
                                        <button
                                            key={
                                                action.key
                                            }
                                            type="button"
                                            disabled={
                                                !action.enabled
                                            }
                                            onClick={() =>
                                                void handleAdminAction(
                                                    action
                                                )
                                            }
                                            className="
                                                rounded-2xl
                                                border
                                                border-white/10
                                                bg-white/5
                                                p-4
                                                text-left
                                                font-semibold
                                                text-white
                                                shadow-sm
                                                transition
                                                hover:border-[#C6A15B]/50
                                                hover:bg-[#C6A15B]/10
                                                hover:shadow-md
                                                disabled:cursor-not-allowed
                                                disabled:opacity-50
                                            "
                                        >
                                            {action.label}
                                        </button>
                                    )
                                )}

                            </div>
                        )}

                    </Section>

                </div>

                {/* ============================================================
                    FOOTER INFORMATION
                   ============================================================ */}

                <div
                    className="
                        mt-8
                        rounded-3xl
                        border
                        border-blue-500/20
                        bg-blue-500/10
                        p-6
                    "
                >

                    <div className="flex items-start gap-3">

                        <ShieldCheck
                            size={22}
                            className="
                                mt-0.5
                                shrink-0
                                text-blue-300
                            "
                        />

                        <div>

                            <h3
                                className="
                                    font-bold
                                    text-white
                                "
                            >
                                Security data source
                            </h3>

                            <p
                                className="
                                    mt-1
                                    text-sm
                                    leading-6
                                    text-blue-200
                                "
                            >
                                Security metrics, alerts,
                                infrastructure status,
                                authentication controls,
                                AI protection status and
                                administrative actions are
                                loaded from the platform
                                security API. No security
                                statistics are generated or
                                stored in this frontend page.
                            </p>

                            {dashboard?.updatedAt && (
                                <p
                                    className="
                                        mt-2
                                        text-xs
                                        font-medium
                                        text-blue-300
                                    "
                                >
                                    Dashboard updated:{" "}
                                    {formatDateTime(
                                        dashboard.updatedAt
                                    )}
                                </p>
                            )}

                        </div>

                    </div>

                </div>

            </div>

        </div>
    );
}