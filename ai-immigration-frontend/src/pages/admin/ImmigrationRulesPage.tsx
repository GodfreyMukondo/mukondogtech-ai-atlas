import React, {
    useCallback,
    useEffect,
    useMemo,
    useState,
    type FormEvent,
} from "react";

import {
    AlertCircle,
    CheckCircle2,
    Clock3,
    Database,
    Edit3,
    Eye,
    FileText,
    Filter,
    Globe2,
    Loader2,
    Plus,
    RefreshCw,
    Scale,
    Search,
    ShieldCheck,
    Trash2,
    X,
} from "lucide-react";

import {
    createImmigrationRule,
    deleteImmigrationRule,
    getImmigrationRule,
    getImmigrationRuleStatistics,
    getImmigrationRules,
    updateImmigrationRule,
    type CreateImmigrationRuleRequest,
    type ImmigrationRule,
    type ImmigrationRuleStatus,
    type ImmigrationRuleSummary,
    type ImmigrationRuleStatistics,
    type UpdateImmigrationRuleRequest,
} from "../../api/immigrationRulesApi";

import { toast } from "sonner";

/**
 * ============================================================================
 * FORM TYPE
 * ============================================================================
 */

type RuleForm = {
    country: string;
    visaType: string;
    category: string;
    status: ImmigrationRuleStatus;
    version: string;
    source: string;
    sourceUrl: string;
    description: string;
    requirements: string;
    eligibility: string;
    restrictions: string;
    processingTime: string;
    fees: string;
    effectiveDate: string;
    expiryDate: string;
};

/**
 * ============================================================================
 * DEFAULT FORM
 * ============================================================================
 */

const emptyForm: RuleForm = {
    country: "",
    visaType: "",
    category: "",
    status: "DRAFT",
    version: "",
    source: "",
    sourceUrl: "",
    description: "",
    requirements: "",
    eligibility: "",
    restrictions: "",
    processingTime: "",
    fees: "",
    effectiveDate: "",
    expiryDate: "",
};

/**
 * ============================================================================
 * STATUS BADGE
 * ============================================================================
 */

function StatusBadge({
    status,
}: {
    status: ImmigrationRuleStatus;
}) {
    const styles: Record<
        ImmigrationRuleStatus,
        string
    > = {
        ACTIVE:
            "bg-emerald-500/10 text-emerald-300 ring-1 ring-emerald-500/20",

        DRAFT:
            "bg-white/10 text-slate-300 ring-1 ring-white/10",

        REVIEW:
            "bg-amber-500/10 text-amber-300 ring-1 ring-amber-500/20",
    };

    return (
        <span
            className={`inline-flex rounded-full px-3 py-1 text-xs font-bold shadow-sm ${styles[status]}`}
        >
            {status}
        </span>
    );
}

/**
 * ============================================================================
 * METRIC CARD
 * ============================================================================
 */

function MetricCard({
    title,
    value,
    icon: Icon,
    color,
    loading,
}: {
    title: string;
    value: number;
    icon: React.ElementType;
    color: string;
    loading?: boolean;
}) {
    return (
        <div className="group relative overflow-hidden rounded-3xl border border-white/10 bg-white/5 backdrop-blur-xl p-6 shadow-sm transition-all duration-300 hover:-translate-y-1 hover:border-white/20 hover:shadow-xl">
            <div className="absolute -right-8 -top-8 h-24 w-24 rounded-full bg-white/5 transition-transform duration-500 group-hover:scale-150" />

            <div className="relative flex items-center justify-between">
                <div>
                    <p className="text-sm font-semibold text-slate-400">
                        {title}
                    </p>

                    {loading ? (
                        <div className="mt-3 h-9 w-20 animate-pulse rounded-lg bg-white/10" />
                    ) : (
                        <h2 className="mt-2 text-3xl font-black tracking-tight text-white">
                            {value.toLocaleString()}
                        </h2>
                    )}
                </div>

                <div
                    className={`rounded-2xl p-4 shadow-sm transition-transform duration-300 group-hover:scale-110 ${color}`}
                >
                    <Icon size={25} />
                </div>
            </div>
        </div>
    );
}

/**
 * ============================================================================
 * FORM INPUT
 * ============================================================================
 */

function FormInput({
    label,
    value,
    onChange,
    placeholder,
    required,
    type = "text",
}: {
    label: string;
    value: string;
    onChange: (
        value: string
    ) => void;
    placeholder?: string;
    required?: boolean;
    type?: string;
}) {
    return (
        <div>
            <label className="mb-2 block text-sm font-semibold text-slate-300">
                {label}

                {required && (
                    <span className="ml-1 text-red-400">
                        *
                    </span>
                )}
            </label>

            <input
                type={type}
                value={value}
                onChange={(event) =>
                    onChange(
                        event.target.value
                    )
                }
                placeholder={placeholder}
                required={required}
                className="w-full rounded-xl border border-white/15 bg-white/5 px-4 py-3 text-sm text-white shadow-sm outline-none transition-all duration-200 placeholder:text-slate-500 hover:border-white/25 focus:border-blue-400 focus:ring-4 focus:ring-blue-500/20"
            />
        </div>
    );
}

/**
 * ============================================================================
 * FORM TEXTAREA
 * ============================================================================
 */

function FormTextarea({
    label,
    value,
    onChange,
    placeholder,
    rows = 4,
}: {
    label: string;
    value: string;
    onChange: (
        value: string
    ) => void;
    placeholder?: string;
    rows?: number;
}) {
    return (
        <div>
            <label className="mb-2 block text-sm font-semibold text-slate-300">
                {label}
            </label>

            <textarea
                value={value}
                onChange={(event) =>
                    onChange(
                        event.target.value
                    )
                }
                placeholder={placeholder}
                rows={rows}
                className="w-full resize-y rounded-xl border border-white/15 bg-white/5 px-4 py-3 text-sm leading-6 text-white shadow-sm outline-none transition-all duration-200 placeholder:text-slate-500 hover:border-white/25 focus:border-blue-400 focus:ring-4 focus:ring-blue-500/20"
            />
        </div>
    );
}

/**
 * ============================================================================
 * FORM SELECT
 * ============================================================================
 */

function FormSelect({
    label,
    value,
    onChange,
    children,
    required,
}: {
    label: string;
    value: string;
    onChange: (
        value: string
    ) => void;
    children: React.ReactNode;
    required?: boolean;
}) {
    return (
        <div>
            <label className="mb-2 block text-sm font-semibold text-slate-300">
                {label}

                {required && (
                    <span className="ml-1 text-red-400">
                        *
                    </span>
                )}
            </label>

            <select
                value={value}
                onChange={(event) =>
                    onChange(
                        event.target.value
                    )
                }
                required={required}
                className="w-full rounded-xl border border-white/15 bg-white/5 px-4 py-3 text-sm text-white shadow-sm outline-none transition-all duration-200 hover:border-white/25 focus:border-blue-400 focus:ring-4 focus:ring-blue-500/20"
            >
                {children}
            </select>
        </div>
    );
}

/**
 * ============================================================================
 * PAGE
 * ============================================================================
 */

export default function ImmigrationRulesPage() {
    const [
        rules,
        setRules,
    ] = useState<ImmigrationRuleSummary[]>(
        []
    );

    const [
        statistics,
        setStatistics,
    ] = useState<ImmigrationRuleStatistics | null>(
        null
    );

    const [
        search,
        setSearch,
    ] = useState("");

    const [
        countryFilter,
        setCountryFilter,
    ] = useState("");

    const [
        categoryFilter,
        setCategoryFilter,
    ] = useState("");

    const [
        statusFilter,
        setStatusFilter,
    ] = useState<
        ImmigrationRuleStatus | ""
    >("");

    const [
        page,
        setPage,
    ] = useState(0);

    const [
        totalPages,
        setTotalPages,
    ] = useState(0);

    const [
        totalElements,
        setTotalElements,
    ] = useState(0);

    const [
        loading,
        setLoading,
    ] = useState(true);

    const [
        statisticsLoading,
        setStatisticsLoading,
    ] = useState(true);

    const [
        refreshing,
        setRefreshing,
    ] = useState(false);

    const [
        deletingId,
        setDeletingId,
    ] = useState<number | null>(
        null
    );

    const [
        actionId,
        setActionId,
    ] = useState<number | null>(
        null
    );

    const [
        modal,
        setModal,
    ] = useState<
        "create" |
        "edit" |
        "view" |
        null
    >(null);

    const [
        selectedRule,
        setSelectedRule,
    ] = useState<ImmigrationRule | null>(
        null
    );

    const [
        form,
        setForm,
    ] = useState<RuleForm>(
        emptyForm
    );

    const [
        formLoading,
        setFormLoading,
    ] = useState(false);

    /**
     * =========================================================================
     * LOAD RULES
     * =========================================================================
     */

    const loadRules =
        useCallback(
            async () => {
                setLoading(true);

                try {
                    const response =
                        await getImmigrationRules(
                            {
                                search:
                                    search.trim() ||
                                    undefined,

                                country:
                                    countryFilter ||
                                    undefined,

                                category:
                                    categoryFilter ||
                                    undefined,

                                status:
                                    statusFilter ||
                                    undefined,

                                page,

                                size: 20,
                            }
                        );

                    setRules(
                        response.content
                    );

                    setTotalPages(
                        response.totalPages
                    );

                    setTotalElements(
                        response.totalElements
                    );
                } catch (error) {
                    console.error(
                        "Failed to load immigration rules:",
                        error
                    );

                    toast.error(
                        "Unable to load immigration rules."
                    );
                } finally {
                    setLoading(false);
                }
            },
            [
                search,
                countryFilter,
                categoryFilter,
                statusFilter,
                page,
            ]
        );

    /**
     * =========================================================================
     * LOAD STATISTICS
     * =========================================================================
     */

    const loadStatistics =
        useCallback(
            async () => {
                setStatisticsLoading(
                    true
                );

                try {
                    const response =
                        await getImmigrationRuleStatistics();

                    setStatistics(
                        response
                    );
                } catch (error) {
                    console.error(
                        "Failed to load immigration rule statistics:",
                        error
                    );

                    toast.error(
                        "Unable to load rule statistics."
                    );
                } finally {
                    setStatisticsLoading(
                        false
                    );
                }
            },
            []
        );

    /**
     * =========================================================================
     * INITIAL LOAD
     * =========================================================================
     */

    useEffect(() => {
        void loadRules();
    }, [loadRules]);

    useEffect(() => {
        void loadStatistics();
    }, [loadStatistics]);

    /**
     * =========================================================================
     * FILTER OPTIONS
     * =========================================================================
     */

    const countries =
        useMemo(
            () =>
                Array.from(
                    new Set(
                        rules
                            .map(
                                rule =>
                                    rule.country
                            )
                            .filter(Boolean)
                    )
                ).sort(),
            [rules]
        );

    const categories =
        useMemo(
            () =>
                Array.from(
                    new Set(
                        rules
                            .map(
                                rule =>
                                    rule.category
                            )
                            .filter(Boolean)
                    )
                ).sort(),
            [rules]
        );

    /**
     * =========================================================================
     * FORM HELPERS
     * =========================================================================
     */

    const updateForm = <
        K extends keyof RuleForm
    >(
        field: K,
        value: RuleForm[K]
    ) => {
        setForm(
            current => ({
                ...current,
                [field]: value,
            })
        );
    };

    const resetForm = () => {
        setForm(
            emptyForm
        );

        setSelectedRule(
            null
        );
    };

    /**
     * =========================================================================
     * OPEN CREATE
     * =========================================================================
     */

    const openCreateModal = () => {
        resetForm();

        setModal(
            "create"
        );
    };

    /**
     * =========================================================================
     * OPEN VIEW
     * =========================================================================
     */

    const openViewModal =
        async (
            rule: ImmigrationRuleSummary
        ) => {
            try {
                setActionId(
                    rule.id
                );

                const fullRule =
                    await getImmigrationRule(
                        rule.id
                    );

                setSelectedRule(
                    fullRule
                );

                setModal(
                    "view"
                );
            } catch (error) {
                console.error(
                    "Failed to load immigration rule:",
                    error
                );

                toast.error(
                    "Unable to load rule details."
                );
            } finally {
                setActionId(
                    null
                );
            }
        };

    /**
     * =========================================================================
     * OPEN EDIT
     * =========================================================================
     */

    const openEditModal =
        async (
            rule: ImmigrationRuleSummary
        ) => {
            try {
                setActionId(
                    rule.id
                );

                const fullRule =
                    await getImmigrationRule(
                        rule.id
                    );

                setSelectedRule(
                    fullRule
                );

                setForm({
                    country:
                        fullRule.country ??
                        "",

                    visaType:
                        fullRule.visaType ??
                        "",

                    category:
                        fullRule.category ??
                        "",

                    status:
                        fullRule.status ??
                        "DRAFT",

                    version:
                        fullRule.version ??
                        "",

                    source:
                        fullRule.source ??
                        "",

                    sourceUrl:
                        fullRule.sourceUrl ??
                        "",

                    description:
                        fullRule.description ??
                        "",

                    requirements:
                        fullRule.requirements ??
                        "",

                    eligibility:
                        fullRule.eligibility ??
                        "",

                    restrictions:
                        fullRule.restrictions ??
                        "",

                    processingTime:
                        fullRule.processingTime ??
                        "",

                    fees:
                        fullRule.fees ??
                        "",

                    effectiveDate:
                        fullRule.effectiveDate ??
                        "",

                    expiryDate:
                        fullRule.expiryDate ??
                        "",
                });

                setModal(
                    "edit"
                );
            } catch (error) {
                console.error(
                    "Failed to load immigration rule:",
                    error
                );

                toast.error(
                    "Unable to load rule for editing."
                );
            } finally {
                setActionId(
                    null
                );
            }
        };

    /**
     * =========================================================================
     * CLOSE MODAL
     * =========================================================================
     */

    const closeModal = () => {
        if (formLoading) {
            return;
        }

        setModal(
            null
        );

        resetForm();
    };

    /**
     * =========================================================================
     * SAVE RULE
     * =========================================================================
     */

    const handleSubmit =
        async (
            event: FormEvent<HTMLFormElement>
        ) => {
            event.preventDefault();

            setFormLoading(
                true
            );

            try {
                const payload:
                    CreateImmigrationRuleRequest &
                    UpdateImmigrationRuleRequest =
                    {
                        country:
                            form.country.trim(),

                        visaType:
                            form.visaType.trim(),

                        category:
                            form.category.trim(),

                        status:
                            form.status,

                        version:
                            form.version.trim(),

                        source:
                            form.source.trim(),

                        sourceUrl:
                            form.sourceUrl.trim() ||
                            undefined,

                        description:
                            form.description.trim() ||
                            undefined,

                        requirements:
                            form.requirements.trim() ||
                            undefined,

                        eligibility:
                            form.eligibility.trim() ||
                            undefined,

                        restrictions:
                            form.restrictions.trim() ||
                            undefined,

                        processingTime:
                            form.processingTime.trim() ||
                            undefined,

                        fees:
                            form.fees.trim() ||
                            undefined,

                        effectiveDate:
                            form.effectiveDate ||
                            undefined,

                        expiryDate:
                            form.expiryDate ||
                            undefined,
                    };

                if (
                    modal === "edit" &&
                    selectedRule
                ) {
                    await updateImmigrationRule(
                        selectedRule.id,
                        payload
                    );

                    toast.success(
                        "Immigration rule updated successfully."
                    );
                } else {
                    await createImmigrationRule(
                        payload
                    );

                    toast.success(
                        "Immigration rule created successfully."
                    );
                }

                closeModal();

                await Promise.all([
                    loadRules(),
                    loadStatistics(),
                ]);
            } catch (error) {
                console.error(
                    "Failed to save immigration rule:",
                    error
                );

                toast.error(
                    "Unable to save immigration rule."
                );
            } finally {
                setFormLoading(
                    false
                );
            }
        };

    /**
     * =========================================================================
     * DELETE
     * =========================================================================
     */

    const handleDelete =
        async (
            rule: ImmigrationRuleSummary
        ) => {
            const confirmed =
                window.confirm(
                    `Delete "${rule.visaType}" for ${rule.country}? This action cannot be undone.`
                );

            if (!confirmed) {
                return;
            }

            setDeletingId(
                rule.id
            );

            try {
                await deleteImmigrationRule(
                    rule.id
                );

                toast.success(
                    "Immigration rule deleted successfully."
                );

                await Promise.all([
                    loadRules(),
                    loadStatistics(),
                ]);
            } catch (error) {
                console.error(
                    "Failed to delete immigration rule:",
                    error
                );

                toast.error(
                    "Unable to delete immigration rule."
                );
            } finally {
                setDeletingId(
                    null
                );
            }
        };

    /**
     * =========================================================================
     * REFRESH
     * =========================================================================
     */

    const handleRefresh =
        async () => {
            setRefreshing(
                true
            );

            try {
                await Promise.all([
                    loadRules(),
                    loadStatistics(),
                ]);

                toast.success(
                    "Immigration rules refreshed."
                );
            } finally {
                setRefreshing(
                    false
                );
            }
        };

    /**
     * =========================================================================
     * CLEAR FILTERS
     * =========================================================================
     */

    const clearFilters = () => {
        setSearch("");
        setCountryFilter("");
        setCategoryFilter("");
        setStatusFilter("");
        setPage(0);
    };

    const hasFilters =
        Boolean(
            search ||
            countryFilter ||
            categoryFilter ||
            statusFilter
        );

    /**
     * =========================================================================
     * RENDER
     * =========================================================================
     */

    return (
        <div className="min-h-screen p-4 sm:p-6">
            <div className="w-full">

                {/* =========================================================
                    HEADER
                   ========================================================= */}

                <div className="mb-8 flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">

                    <div>
                        <div className="flex items-center gap-3">

                            <div className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-[#071426] via-[#123B68] to-[#2563EB] p-3 shadow-lg shadow-blue-900/20">
                                <div className="absolute inset-0 bg-white/10" />

                                <Scale
                                    className="relative text-[#C6A15B]"
                                    size={28}
                                />
                            </div>

                            <div>
                                <h1 className="bg-gradient-to-r from-white via-blue-100 to-blue-300 bg-clip-text text-3xl font-black tracking-tight text-transparent sm:text-4xl">
                                    Immigration Rules Engine
                                </h1>

                                <p className="mt-1 max-w-3xl text-slate-300">
                                    Manage immigration regulations,
                                    visa requirements, compliance
                                    rules and AI knowledge sources.
                                </p>
                            </div>

                        </div>
                    </div>

                    <div className="flex flex-wrap gap-3">

                        <button
                            type="button"
                            onClick={
                                openCreateModal
                            }
                            className="group flex items-center gap-2 rounded-2xl bg-gradient-to-r from-[#C6A15B] to-[#FFD45A] px-5 py-3 font-bold text-[#071426] shadow-lg shadow-amber-500/20 transition-all duration-200 hover:-translate-y-0.5 hover:from-[#e8aa0c] hover:to-[#C6A15B] hover:shadow-xl focus:outline-none focus:ring-4 focus:ring-[#C6A15B]/30"
                        >
                            <Plus
                                size={19}
                                className="transition-transform duration-200 group-hover:rotate-90"
                            />

                            Add Immigration Rule
                        </button>

                        <button
                            type="button"
                            onClick={
                                handleRefresh
                            }
                            disabled={
                                refreshing
                            }
                            className="flex items-center gap-2 rounded-2xl border border-white/15 bg-white/5 px-5 py-3 font-semibold text-slate-200 shadow-sm transition-all duration-200 hover:-translate-y-0.5 hover:border-blue-400/50 hover:bg-blue-500/10 hover:text-blue-300 hover:shadow-md disabled:cursor-not-allowed disabled:opacity-60"
                        >
                            <RefreshCw
                                size={18}
                                className={
                                    refreshing
                                        ? "animate-spin"
                                        : ""
                                }
                            />

                            Refresh
                        </button>

                    </div>
                </div>

                {/* =========================================================
                    METRICS
                   ========================================================= */}

                <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-4">

                    <MetricCard
                        title="Total Rules"
                        value={
                            statistics?.totalRules ??
                            0
                        }
                        icon={Database}
                        color="bg-gradient-to-br from-blue-500/15 to-cyan-500/10 text-blue-300"
                        loading={
                            statisticsLoading
                        }
                    />

                    <MetricCard
                        title="Active Regulations"
                        value={
                            statistics?.activeRules ??
                            0
                        }
                        icon={CheckCircle2}
                        color="bg-gradient-to-br from-emerald-500/15 to-teal-500/10 text-emerald-300"
                        loading={
                            statisticsLoading
                        }
                    />

                    <MetricCard
                        title="Pending Review"
                        value={
                            statistics?.reviewRules ??
                            0
                        }
                        icon={Clock3}
                        color="bg-gradient-to-br from-amber-500/15 to-orange-500/10 text-amber-300"
                        loading={
                            statisticsLoading
                        }
                    />

                    <MetricCard
                        title="AI Sources"
                        value={
                            statistics?.aiIndexedRules ??
                            0
                        }
                        icon={Globe2}
                        color="bg-gradient-to-br from-violet-500/15 to-fuchsia-500/10 text-violet-300"
                        loading={
                            statisticsLoading
                        }
                    />

                </div>

                {/* =========================================================
                    SECONDARY METRICS
                   ========================================================= */}

                <div className="mt-5 grid gap-4 sm:grid-cols-3">

                    <div className="group rounded-2xl border border-blue-500/20 bg-white/5 backdrop-blur-xl p-5 shadow-sm transition-all duration-300 hover:-translate-y-0.5 hover:border-blue-400/30 hover:shadow-md">

                        <div className="flex items-center justify-between">
                            <p className="text-sm font-semibold text-slate-400">
                                Countries Covered
                            </p>

                            <Globe2
                                size={20}
                                className="text-blue-400 transition-transform group-hover:scale-110"
                            />
                        </div>

                        <p className="mt-2 text-2xl font-black text-white">
                            {statisticsLoading
                                ? "—"
                                : (
                                    statistics?.countriesCovered ??
                                    0
                                ).toLocaleString()}
                        </p>

                    </div>

                    <div className="group rounded-2xl border border-violet-500/20 bg-white/5 backdrop-blur-xl p-5 shadow-sm transition-all duration-300 hover:-translate-y-0.5 hover:border-violet-400/30 hover:shadow-md">

                        <div className="flex items-center justify-between">
                            <p className="text-sm font-semibold text-slate-400">
                                Visa Types
                            </p>

                            <FileText
                                size={20}
                                className="text-violet-400 transition-transform group-hover:scale-110"
                            />
                        </div>

                        <p className="mt-2 text-2xl font-black text-white">
                            {statisticsLoading
                                ? "—"
                                : (
                                    statistics?.visaTypesCovered ??
                                    0
                                ).toLocaleString()}
                        </p>

                    </div>

                    <div className="group rounded-2xl border border-amber-500/20 bg-white/5 backdrop-blur-xl p-5 shadow-sm transition-all duration-300 hover:-translate-y-0.5 hover:border-amber-400/30 hover:shadow-md">

                        <div className="flex items-center justify-between">
                            <p className="text-sm font-semibold text-slate-400">
                                Pending AI Index
                            </p>

                            <Clock3
                                size={20}
                                className="text-amber-400 transition-transform group-hover:scale-110"
                            />
                        </div>

                        <p className="mt-2 text-2xl font-black text-white">
                            {statisticsLoading
                                ? "—"
                                : (
                                    statistics?.pendingAiIndexRules ??
                                    0
                                ).toLocaleString()}
                        </p>

                    </div>

                </div>

                {/* =========================================================
                    SEARCH / FILTERS
                   ========================================================= */}

                <div className="mt-8 rounded-3xl border border-white/10 bg-white/5 p-5 shadow-lg shadow-black/20 backdrop-blur-xl">

                    <div className="mb-4 flex items-center gap-2">
                        <div className="rounded-xl bg-blue-500/10 p-2 text-blue-300">
                            <Filter size={17} />
                        </div>

                        <div>
                            <p className="font-bold text-white">
                                Search & Filters
                            </p>

                            <p className="text-xs text-slate-400">
                                Find immigration regulations quickly
                            </p>
                        </div>
                    </div>

                    <div className="grid gap-4 xl:grid-cols-[2fr_1fr_1fr_1fr_auto]">

                        <div className="relative">

                            <Search
                                size={18}
                                className="absolute left-4 top-3.5 text-blue-400"
                            />

                            <input
                                value={
                                    search
                                }
                                onChange={(event) => {
                                    setSearch(
                                        event.target.value
                                    );

                                    setPage(0);
                                }}
                                placeholder="Search country, visa type, category..."
                                className="w-full rounded-2xl border border-white/15 bg-white/5 text-white py-3 pl-11 pr-4 text-sm outline-none transition-all duration-200 placeholder:text-slate-500 hover:border-blue-400/40 focus:border-blue-400 focus:ring-4 focus:ring-blue-500/20"
                            />

                        </div>

                        <select
                            value={
                                countryFilter
                            }
                            onChange={(event) => {
                                setCountryFilter(
                                    event.target.value
                                );

                                setPage(0);
                            }}
                            className="rounded-2xl border border-white/15 bg-white/5 text-white px-4 py-3 text-sm outline-none transition-all duration-200 hover:border-blue-400/40 focus:border-blue-400 focus:ring-4 focus:ring-blue-500/20"
                        >

                            <option value="">
                                All Countries
                            </option>

                            {countries.map(
                                country => (
                                    <option
                                        key={country}
                                        value={country}
                                    >
                                        {country}
                                    </option>
                                )
                            )}

                        </select>

                        <select
                            value={
                                categoryFilter
                            }
                            onChange={(event) => {
                                setCategoryFilter(
                                    event.target.value
                                );

                                setPage(0);
                            }}
                            className="rounded-2xl border border-white/15 bg-white/5 text-white px-4 py-3 text-sm outline-none transition-all duration-200 hover:border-violet-400/40 focus:border-violet-400 focus:ring-4 focus:ring-violet-500/20"
                        >

                            <option value="">
                                All Categories
                            </option>

                            {categories.map(
                                category => (
                                    <option
                                        key={category}
                                        value={category}
                                    >
                                        {category}
                                    </option>
                                )
                            )}

                        </select>

                        <select
                            value={
                                statusFilter
                            }
                            onChange={(event) => {
                                setStatusFilter(
                                    event.target.value as
                                        | ImmigrationRuleStatus
                                        | ""
                                );

                                setPage(0);
                            }}
                            className="rounded-2xl border border-white/15 bg-white/5 text-white px-4 py-3 text-sm outline-none transition-all duration-200 hover:border-emerald-400/40 focus:border-emerald-400 focus:ring-4 focus:ring-emerald-500/20"
                        >

                            <option value="">
                                All Statuses
                            </option>

                            <option value="ACTIVE">
                                Active
                            </option>

                            <option value="REVIEW">
                                Review
                            </option>

                            <option value="DRAFT">
                                Draft
                            </option>

                        </select>

                        <button
                            type="button"
                            onClick={
                                clearFilters
                            }
                            disabled={
                                !hasFilters
                            }
                            className="flex items-center justify-center gap-2 rounded-2xl border border-white/15 px-5 py-3 text-sm font-semibold text-slate-200 transition-all duration-200 hover:border-red-400/40 hover:bg-red-500/10 hover:text-red-300 disabled:cursor-not-allowed disabled:opacity-40"
                        >
                            <Filter
                                size={18}
                            />

                            Clear
                        </button>

                    </div>

                </div>

                {/* =========================================================
                    RESULTS SUMMARY
                   ========================================================= */}

                <div className="mt-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">

                    <p className="text-sm text-slate-300">
                        Showing{" "}
                        <span className="font-bold text-blue-300">
                            {rules.length}
                        </span>{" "}
                        of{" "}
                        <span className="font-bold text-white">
                            {totalElements.toLocaleString()}
                        </span>{" "}
                        immigration rules
                    </p>

                    {statisticsLoading && (
                        <div className="flex items-center gap-2 rounded-full bg-blue-500/10 px-3 py-1.5 text-sm font-medium text-blue-300">
                            <Loader2
                                size={15}
                                className="animate-spin"
                            />

                            Updating statistics...
                        </div>
                    )}

                </div>

                {/* =========================================================
                    TABLE
                   ========================================================= */}

                <div className="mt-4 overflow-hidden rounded-3xl border border-white/10 bg-white/5 backdrop-blur-xl shadow-xl shadow-black/20">

                    <div className="overflow-x-auto">

                        <table className="w-full min-w-[1100px] text-left">

                            <thead className="bg-gradient-to-r from-[#071426] via-[#102C54] to-[#164E8A] text-white">

                                <tr>

                                    <th className="px-5 py-4 text-xs font-bold uppercase tracking-wide">
                                        Country
                                    </th>

                                    <th className="px-5 py-4 text-xs font-bold uppercase tracking-wide">
                                        Visa Type
                                    </th>

                                    <th className="px-5 py-4 text-xs font-bold uppercase tracking-wide">
                                        Category
                                    </th>

                                    <th className="px-5 py-4 text-xs font-bold uppercase tracking-wide">
                                        Status
                                    </th>

                                    <th className="px-5 py-4 text-xs font-bold uppercase tracking-wide">
                                        Version
                                    </th>

                                    <th className="px-5 py-4 text-xs font-bold uppercase tracking-wide">
                                        Updated
                                    </th>

                                    <th className="px-5 py-4 text-xs font-bold uppercase tracking-wide">
                                        Actions
                                    </th>

                                </tr>

                            </thead>

                            <tbody>

                                {loading ? (
                                    <tr>
                                        <td
                                            colSpan={7}
                                            className="px-5 py-16 text-center"
                                        >
                                            <Loader2
                                                size={30}
                                                className="mx-auto animate-spin text-blue-600"
                                            />

                                            <p className="mt-3 text-sm font-medium text-slate-400">
                                                Loading immigration rules...
                                            </p>
                                        </td>
                                    </tr>
                                ) : rules.length === 0 ? (
                                    <tr>
                                        <td
                                            colSpan={7}
                                            className="px-5 py-16 text-center"
                                        >
                                            <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-white/5">
                                                <AlertCircle
                                                    size={40}
                                                    className="text-slate-400"
                                                />
                                            </div>

                                            <h3 className="mt-4 font-bold text-white">
                                                No immigration rules found
                                            </h3>

                                            <p className="mt-2 text-sm text-slate-400">
                                                Try changing your filters
                                                or create a new immigration
                                                rule.
                                            </p>

                                            <button
                                                type="button"
                                                onClick={
                                                    openCreateModal
                                                }
                                                className="mt-5 inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-[#C6A15B] to-[#FFD45A] px-4 py-2.5 text-sm font-bold text-[#071426] shadow-md transition-all hover:-translate-y-0.5 hover:shadow-lg"
                                            >
                                                <Plus
                                                    size={16}
                                                />

                                                Add Rule
                                            </button>
                                        </td>
                                    </tr>
                                ) : (
                                    rules.map(
                                        rule => (
                                            <tr
                                                key={
                                                    rule.id
                                                }
                                                className="group border-t border-white/10 transition-all duration-200 hover:bg-blue-500/5"
                                            >

                                                <td className="px-5 py-4">

                                                    <div className="flex items-center gap-3">

                                                        <div className="rounded-xl bg-gradient-to-br from-blue-500/15 to-cyan-500/10 p-2 text-blue-300 shadow-sm transition-transform duration-200 group-hover:scale-105">
                                                            <Globe2
                                                                size={18}
                                                            />
                                                        </div>

                                                        <div>
                                                            <p className="font-bold text-white">
                                                                {
                                                                    rule.country
                                                                }
                                                            </p>

                                                            <p className="text-xs text-slate-400">
                                                                {
                                                                    rule.source
                                                                }
                                                            </p>
                                                        </div>

                                                    </div>

                                                </td>

                                                <td className="px-5 py-4 font-semibold text-slate-200">
                                                    {
                                                        rule.visaType
                                                    }
                                                </td>

                                                <td className="px-5 py-4 text-sm text-slate-300">
                                                    {
                                                        rule.category
                                                    }
                                                </td>

                                                <td className="px-5 py-4">
                                                    <StatusBadge
                                                        status={
                                                            rule.status
                                                        }
                                                    />
                                                </td>

                                                <td className="px-5 py-4">

                                                    <p className="inline-flex rounded-lg bg-white/10 px-2.5 py-1 font-semibold text-slate-200">
                                                        {
                                                            rule.version
                                                        }
                                                    </p>

                                                </td>

                                                <td className="px-5 py-4">

                                                    <p className="text-sm font-medium text-slate-300">
                                                        {rule.updatedAt
                                                            ? new Date(
                                                                rule.updatedAt
                                                            ).toLocaleDateString(
                                                                undefined,
                                                                {
                                                                    year: "numeric",
                                                                    month: "short",
                                                                    day: "numeric",
                                                                }
                                                            )
                                                            : "—"}
                                                    </p>

                                                    <p className="mt-1 text-xs text-slate-400">
                                                        {
                                                            rule.updatedBy
                                                        }
                                                    </p>

                                                </td>

                                                <td className="px-5 py-4">

                                                    <div className="flex gap-2">

                                                        <button
                                                            type="button"
                                                            title="View rule"
                                                            onClick={() =>
                                                                void openViewModal(
                                                                    rule
                                                                )
                                                            }
                                                            disabled={
                                                                actionId ===
                                                                rule.id
                                                            }
                                                            className="rounded-xl bg-blue-500/10 p-2 text-blue-300 shadow-sm transition-all duration-200 hover:-translate-y-0.5 hover:bg-blue-500/20 hover:text-blue-200 hover:shadow-md disabled:opacity-50"
                                                        >
                                                            {actionId ===
                                                            rule.id ? (
                                                                <Loader2
                                                                    size={18}
                                                                    className="animate-spin"
                                                                />
                                                            ) : (
                                                                <Eye
                                                                    size={18}
                                                                />
                                                            )}
                                                        </button>

                                                        <button
                                                            type="button"
                                                            title="Edit rule"
                                                            onClick={() =>
                                                                void openEditModal(
                                                                    rule
                                                                )
                                                            }
                                                            disabled={
                                                                actionId ===
                                                                rule.id
                                                            }
                                                            className="rounded-xl bg-amber-500/10 p-2 text-amber-300 shadow-sm transition-all duration-200 hover:-translate-y-0.5 hover:bg-amber-500/20 hover:text-amber-200 hover:shadow-md disabled:opacity-50"
                                                        >
                                                            <Edit3
                                                                size={18}
                                                            />
                                                        </button>

                                                        <button
                                                            type="button"
                                                            title="Delete rule"
                                                            onClick={() =>
                                                                void handleDelete(
                                                                    rule
                                                                )
                                                            }
                                                            disabled={
                                                                deletingId ===
                                                                rule.id
                                                            }
                                                            className="rounded-xl bg-red-500/10 p-2 text-red-300 shadow-sm transition-all duration-200 hover:-translate-y-0.5 hover:bg-red-500/20 hover:text-red-200 hover:shadow-md disabled:opacity-50"
                                                        >
                                                            {deletingId ===
                                                            rule.id ? (
                                                                <Loader2
                                                                    size={18}
                                                                    className="animate-spin"
                                                                />
                                                            ) : (
                                                                <Trash2
                                                                    size={18}
                                                                />
                                                            )}
                                                        </button>

                                                    </div>

                                                </td>

                                            </tr>
                                        )
                                    )
                                )}

                            </tbody>

                        </table>

                    </div>

                </div>

                {/* =========================================================
                    PAGINATION
                   ========================================================= */}

                {totalPages > 1 && (
                    <div className="mt-5 flex items-center justify-between rounded-2xl border border-white/10 bg-white/5 backdrop-blur-xl p-4 shadow-sm">

                        <button
                            type="button"
                            disabled={
                                page === 0
                            }
                            onClick={() =>
                                setPage(
                                    current =>
                                        Math.max(
                                            current - 1,
                                            0
                                        )
                                )
                            }
                            className="rounded-xl border border-white/15 px-4 py-2 text-sm font-semibold text-slate-200 transition-all hover:border-blue-400/40 hover:bg-blue-500/10 hover:text-blue-300 disabled:cursor-not-allowed disabled:opacity-40"
                        >
                            Previous
                        </button>

                        <span className="rounded-full bg-blue-500/10 px-4 py-2 text-sm text-slate-300">
                            Page{" "}
                            <strong className="text-blue-300">
                                {page + 1}
                            </strong>{" "}
                            of{" "}
                            <strong className="text-white">
                                {totalPages}
                            </strong>
                        </span>

                        <button
                            type="button"
                            disabled={
                                page >=
                                totalPages - 1
                            }
                            onClick={() =>
                                setPage(
                                    current =>
                                        Math.min(
                                            current + 1,
                                            totalPages - 1
                                        )
                                )
                            }
                            className="rounded-xl border border-white/15 px-4 py-2 text-sm font-semibold text-slate-200 transition-all hover:border-blue-400/40 hover:bg-blue-500/10 hover:text-blue-300 disabled:cursor-not-allowed disabled:opacity-40"
                        >
                            Next
                        </button>

                    </div>
                )}

                {/* =========================================================
                    AI INFORMATION
                   ========================================================= */}

                <div className="relative mt-8 overflow-hidden rounded-3xl bg-gradient-to-br from-[#071426] via-[#102C54] to-[#164E8A] p-7 text-white shadow-2xl shadow-blue-950/20">

                    <div className="absolute -right-16 -top-16 h-48 w-48 rounded-full bg-blue-400/10" />
                    <div className="absolute -bottom-20 left-1/3 h-56 w-56 rounded-full bg-cyan-400/10" />

                    <div className="relative flex items-start gap-4">

                        <div className="rounded-2xl bg-white/10 p-3 backdrop-blur-sm">
                            <ShieldCheck
                                className="text-[#C6A15B]"
                                size={32}
                            />
                        </div>

                        <div>

                            <h2 className="text-2xl font-black">
                                AI Compliance Protection
                            </h2>

                            <p className="mt-2 max-w-3xl leading-7 text-blue-100">
                                Immigration rules stored in the
                                database can be used by the AI
                                document analysis and compliance
                                workflows after they have been
                                reviewed and approved.
                            </p>

                        </div>

                    </div>

                    <div className="relative mt-5 inline-flex items-center gap-3 rounded-full border border-emerald-400/20 bg-emerald-400/10 px-4 py-2 text-emerald-300">

                        <CheckCircle2
                            size={18}
                        />

                        <span className="text-sm font-semibold">
                            {
                                statistics?.aiIndexedRules ??
                                0
                            }{" "}
                            rules currently indexed for AI
                        </span>

                    </div>

                </div>

            </div>

            {/* =================================================================
                CREATE / EDIT MODAL
               ================================================================= */}

            {(modal === "create" ||
                modal === "edit") && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/70 p-4 backdrop-blur-md">

                    <div className="max-h-[95vh] w-full max-w-5xl overflow-hidden rounded-3xl border border-white/10 bg-[#1F314A] backdrop-blur-xl shadow-2xl shadow-black/40">

                        <div className="flex items-center justify-between border-b border-white/10 bg-gradient-to-r from-white/5 via-transparent to-blue-500/10 px-6 py-5">

                            <div>

                                <div className="flex items-center gap-3">
                                    <div className="rounded-xl bg-gradient-to-br from-blue-600 to-indigo-700 p-2.5 text-white shadow-md">
                                        {modal === "create" ? (
                                            <Plus size={20} />
                                        ) : (
                                            <Edit3 size={20} />
                                        )}
                                    </div>

                                    <div>
                                        <h2 className="text-xl font-black text-white">
                                            {modal ===
                                            "create"
                                                ? "Create Immigration Rule"
                                                : "Edit Immigration Rule"}
                                        </h2>

                                        <p className="mt-1 text-sm text-slate-400">
                                            Manage regulatory information
                                            used by the immigration
                                            compliance engine.
                                        </p>
                                    </div>
                                </div>

                            </div>

                            <button
                                type="button"
                                onClick={
                                    closeModal
                                }
                                disabled={
                                    formLoading
                                }
                                className="rounded-xl p-2 text-slate-400 transition-all hover:bg-red-500/10 hover:text-red-300 disabled:opacity-50"
                            >
                                <X
                                    size={22}
                                />
                            </button>

                        </div>

                        <form
                            onSubmit={
                                handleSubmit
                            }
                            className="max-h-[calc(95vh-90px)] overflow-y-auto"
                        >

                            <div className="grid gap-6 p-6">

                                {/* Basic information */}

                                <div className="rounded-2xl border border-blue-500/15 bg-blue-500/5 p-5">

                                    <h3 className="mb-4 flex items-center gap-2 text-sm font-black uppercase tracking-wide text-blue-300">
                                        <Database size={16} />
                                        Basic Information
                                    </h3>

                                    <div className="grid gap-5 md:grid-cols-2">

                                        <FormInput
                                            label="Country"
                                            value={
                                                form.country
                                            }
                                            onChange={value =>
                                                updateForm(
                                                    "country",
                                                    value
                                                )
                                            }
                                            placeholder="Enter country"
                                            required
                                        />

                                        <FormInput
                                            label="Visa Type"
                                            value={
                                                form.visaType
                                            }
                                            onChange={value =>
                                                updateForm(
                                                    "visaType",
                                                    value
                                                )
                                            }
                                            placeholder="Enter visa type"
                                            required
                                        />

                                        <FormInput
                                            label="Category"
                                            value={
                                                form.category
                                            }
                                            onChange={value =>
                                                updateForm(
                                                    "category",
                                                    value
                                                )
                                            }
                                            placeholder="Enter rule category"
                                            required
                                        />

                                        <FormInput
                                            label="Version"
                                            value={
                                                form.version
                                            }
                                            onChange={value =>
                                                updateForm(
                                                    "version",
                                                    value
                                                )
                                            }
                                            placeholder="e.g. 1.0"
                                            required
                                        />

                                        <FormInput
                                            label="Source"
                                            value={
                                                form.source
                                            }
                                            onChange={value =>
                                                updateForm(
                                                    "source",
                                                    value
                                                )
                                            }
                                            placeholder="Official immigration authority"
                                            required
                                        />

                                        <FormInput
                                            label="Source URL"
                                            value={
                                                form.sourceUrl
                                            }
                                            onChange={value =>
                                                updateForm(
                                                    "sourceUrl",
                                                    value
                                                )
                                            }
                                            placeholder="https://..."
                                            type="url"
                                        />

                                        <FormSelect
                                            label="Status"
                                            value={
                                                form.status
                                            }
                                            onChange={value =>
                                                updateForm(
                                                    "status",
                                                    value as ImmigrationRuleStatus
                                                )
                                            }
                                            required
                                        >
                                            <option value="DRAFT">
                                                Draft
                                            </option>

                                            <option value="REVIEW">
                                                Review
                                            </option>

                                            <option value="ACTIVE">
                                                Active
                                            </option>
                                        </FormSelect>

                                    </div>
                                </div>

                                {/* Rule information */}

                                <div className="rounded-2xl border border-violet-500/15 bg-violet-500/5 p-5">

                                    <h3 className="mb-4 flex items-center gap-2 text-sm font-black uppercase tracking-wide text-violet-300">
                                        <Scale size={16} />
                                        Rule Information
                                    </h3>

                                    <div className="grid gap-5">

                                        <FormTextarea
                                            label="Description"
                                            value={
                                                form.description
                                            }
                                            onChange={value =>
                                                updateForm(
                                                    "description",
                                                    value
                                                )
                                            }
                                            placeholder="Describe the immigration rule..."
                                        />

                                        <FormTextarea
                                            label="Requirements"
                                            value={
                                                form.requirements
                                            }
                                            onChange={value =>
                                                updateForm(
                                                    "requirements",
                                                    value
                                                )
                                            }
                                            placeholder="List the required documents and conditions..."
                                        />

                                        <FormTextarea
                                            label="Eligibility"
                                            value={
                                                form.eligibility
                                            }
                                            onChange={value =>
                                                updateForm(
                                                    "eligibility",
                                                    value
                                                )
                                            }
                                            placeholder="Describe applicant eligibility..."
                                        />

                                        <FormTextarea
                                            label="Restrictions"
                                            value={
                                                form.restrictions
                                            }
                                            onChange={value =>
                                                updateForm(
                                                    "restrictions",
                                                    value
                                                )
                                            }
                                            placeholder="Describe restrictions or exclusions..."
                                        />

                                    </div>
                                </div>

                                {/* Processing */}

                                <div className="rounded-2xl border border-emerald-500/15 bg-emerald-500/5 p-5">

                                    <h3 className="mb-4 flex items-center gap-2 text-sm font-black uppercase tracking-wide text-emerald-300">
                                        <Clock3 size={16} />
                                        Processing Information
                                    </h3>

                                    <div className="grid gap-5 md:grid-cols-2">

                                        <FormInput
                                            label="Processing Time"
                                            value={
                                                form.processingTime
                                            }
                                            onChange={value =>
                                                updateForm(
                                                    "processingTime",
                                                    value
                                                )
                                            }
                                            placeholder="e.g. 4–8 weeks"
                                        />

                                        <FormInput
                                            label="Fees"
                                            value={
                                                form.fees
                                            }
                                            onChange={value =>
                                                updateForm(
                                                    "fees",
                                                    value
                                                )
                                            }
                                            placeholder="Enter applicable fees"
                                        />

                                        <FormInput
                                            label="Effective Date"
                                            value={
                                                form.effectiveDate
                                            }
                                            onChange={value =>
                                                updateForm(
                                                    "effectiveDate",
                                                    value
                                                )
                                            }
                                            type="date"
                                        />

                                        <FormInput
                                            label="Expiry Date"
                                            value={
                                                form.expiryDate
                                            }
                                            onChange={value =>
                                                updateForm(
                                                    "expiryDate",
                                                    value
                                                )
                                            }
                                            type="date"
                                        />

                                    </div>
                                </div>

                            </div>

                            {/* Footer */}

                            <div className="sticky bottom-0 flex items-center justify-end gap-3 border-t border-white/10 bg-[#1F314A]/95 px-6 py-4 shadow-[0_-8px_20px_rgba(0,0,0,0.25)] backdrop-blur-xl">

                                <button
                                    type="button"
                                    onClick={
                                        closeModal
                                    }
                                    disabled={
                                        formLoading
                                    }
                                    className="rounded-xl border border-white/15 px-5 py-2.5 text-sm font-semibold text-slate-200 transition-all hover:border-white/25 hover:bg-white/10 disabled:opacity-50"
                                >
                                    Cancel
                                </button>

                                <button
                                    type="submit"
                                    disabled={
                                        formLoading
                                    }
                                    className="inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-[#C6A15B] to-[#FFD45A] px-5 py-2.5 text-sm font-bold text-[#071426] shadow-md transition-all hover:-translate-y-0.5 hover:shadow-lg disabled:cursor-not-allowed disabled:opacity-60"
                                >
                                    {formLoading && (
                                        <Loader2
                                            size={17}
                                            className="animate-spin"
                                        />
                                    )}

                                    {modal ===
                                    "create"
                                        ? "Create Rule"
                                        : "Save Changes"}
                                </button>

                            </div>

                        </form>

                    </div>
                </div>
            )}

            {/* =================================================================
                VIEW MODAL
               ================================================================= */}

            {modal === "view" &&
                selectedRule && (
                    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/70 p-4 backdrop-blur-md">

                        <div className="max-h-[90vh] w-full max-w-4xl overflow-hidden rounded-3xl border border-white/10 bg-[#1F314A] backdrop-blur-xl shadow-2xl shadow-black/40">

                            <div className="flex items-center justify-between border-b border-white/10 bg-gradient-to-r from-white/5 via-transparent to-blue-500/10 px-6 py-5">

                                <div>

                                    <div className="flex items-center gap-3">

                                        <div className="rounded-xl bg-gradient-to-br from-blue-600 to-indigo-700 p-2.5 text-white shadow-md">
                                            <Scale
                                                size={20}
                                            />
                                        </div>

                                        <h2 className="text-xl font-black text-white">
                                            {
                                                selectedRule.visaType
                                            }
                                        </h2>

                                    </div>

                                    <p className="mt-1 text-sm text-slate-400">
                                        {
                                            selectedRule.country
                                        }
                                        {" • "}
                                        {
                                            selectedRule.category
                                        }
                                    </p>

                                </div>

                                <button
                                    type="button"
                                    onClick={
                                        closeModal
                                    }
                                    className="rounded-xl p-2 text-slate-400 transition-all hover:bg-red-500/10 hover:text-red-300"
                                >
                                    <X
                                        size={22}
                                    />
                                </button>

                            </div>

                            <div className="max-h-[calc(90vh-90px)] overflow-y-auto p-6">

                                <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">

                                    <div className="rounded-2xl border border-emerald-500/15 bg-emerald-500/5 p-4">
                                        <p className="text-xs font-semibold uppercase tracking-wide text-emerald-300">
                                            Status
                                        </p>

                                        <div className="mt-2">
                                            <StatusBadge
                                                status={
                                                    selectedRule.status
                                                }
                                            />
                                        </div>
                                    </div>

                                    <div className="rounded-2xl border border-blue-500/15 bg-blue-500/5 p-4">
                                        <p className="text-xs font-semibold uppercase tracking-wide text-blue-300">
                                            Version
                                        </p>

                                        <p className="mt-2 font-bold text-white">
                                            {
                                                selectedRule.version
                                            }
                                        </p>
                                    </div>

                                    <div className="rounded-2xl border border-violet-500/15 bg-violet-500/5 p-4">
                                        <p className="text-xs font-semibold uppercase tracking-wide text-violet-300">
                                            AI Indexed
                                        </p>

                                        <p className="mt-2 font-bold text-white">
                                            {
                                                selectedRule.aiIndexed
                                                    ? "Yes"
                                                    : "No"
                                            }
                                        </p>
                                    </div>

                                    <div className="rounded-2xl border border-amber-500/15 bg-amber-500/5 p-4">
                                        <p className="text-xs font-semibold uppercase tracking-wide text-amber-300">
                                            Source
                                        </p>

                                        <p className="mt-2 font-bold text-white">
                                            {
                                                selectedRule.source
                                            }
                                        </p>
                                    </div>

                                </div>

                                <div className="mt-6 space-y-6">

                                    {[
                                        [
                                            "Description",
                                            selectedRule.description,
                                        ],
                                        [
                                            "Eligibility",
                                            selectedRule.eligibility,
                                        ],
                                        [
                                            "Requirements",
                                            selectedRule.requirements,
                                        ],
                                        [
                                            "Restrictions",
                                            selectedRule.restrictions,
                                        ],
                                        [
                                            "Processing Time",
                                            selectedRule.processingTime,
                                        ],
                                        [
                                            "Fees",
                                            selectedRule.fees,
                                        ],
                                    ].map(
                                        ([title, content]) =>
                                            content ? (
                                                <div
                                                    key={
                                                        title
                                                    }
                                                    className="rounded-2xl border border-white/10 bg-white/5 p-5"
                                                >
                                                    <h3 className="font-bold text-white">
                                                        {
                                                            title
                                                        }
                                                    </h3>

                                                    <p className="mt-2 whitespace-pre-wrap text-sm leading-7 text-slate-300">
                                                        {
                                                            content
                                                        }
                                                    </p>
                                                </div>
                                            ) : null
                                    )}

                                </div>

                                <div className="mt-8 rounded-2xl border border-blue-500/20 bg-blue-500/10 p-4">

                                    <div className="flex items-start gap-3">

                                        <div className="rounded-xl bg-blue-500/10 p-2 text-blue-300">
                                            <FileText
                                                size={20}
                                            />
                                        </div>

                                        <div>

                                            <p className="font-semibold text-white">
                                                Source
                                            </p>

                                            {selectedRule.sourceUrl ? (
                                                <a
                                                    href={
                                                        selectedRule.sourceUrl
                                                    }
                                                    target="_blank"
                                                    rel="noopener noreferrer"
                                                    className="mt-1 block break-all text-sm font-medium text-blue-300 underline decoration-blue-400/50 underline-offset-2 transition-colors hover:text-blue-100"
                                                >
                                                    {
                                                        selectedRule.sourceUrl
                                                    }
                                                </a>
                                            ) : (
                                                <p className="mt-1 text-sm text-blue-200">
                                                    {
                                                        selectedRule.source
                                                    }
                                                </p>
                                            )}

                                        </div>

                                    </div>

                                </div>

                            </div>

                        </div>
                    </div>
                )}

        </div>
    );
}

