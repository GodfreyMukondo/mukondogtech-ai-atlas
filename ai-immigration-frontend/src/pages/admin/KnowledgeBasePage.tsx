import React, {
    useCallback,
    useEffect,
    useMemo,
    useRef,
    useState,
} from "react";

import {
    AlertCircle,
    BookOpen,
    BrainCircuit,
    CheckCircle2,
    Database,
    Eye,
    FileText,
    Filter,
    Globe2,
    Loader2,
    Plus,
    RefreshCw,
    Search,
    ShieldCheck,
    Trash2,
    Upload,
    Users,
    X,
} from "lucide-react";

import {
    createKnowledgeDocument,
    deleteKnowledgeDocument,
    getKnowledgeBaseStats,
    getKnowledgeDocuments,
    indexKnowledgeDocument,
    uploadKnowledgeDocument,
    type KnowledgeBaseStats,
    type KnowledgeDocumentSummary,
    type KnowledgeDocumentStatus,
} from "../../api/knowledgeBaseApi";

import { toast } from "sonner";

/**
 * ============================================================================
 * TYPES
 * ============================================================================
 */

type ModalType = "ARTICLE" | "UPLOAD" | null;

interface ArticleForm {
    title: string;
    category: string;
    country: string;
    version: string;
    status: KnowledgeDocumentStatus;
    content: string;
}

interface UploadForm {
    title: string;
    category: string;
    country: string;
    version: string;
    status: KnowledgeDocumentStatus;
    file: File | null;
}

const INITIAL_ARTICLE_FORM: ArticleForm = {
    title: "",
    category: "",
    country: "",
    version: "1.0",
    status: "DRAFT",
    content: "",
};

const INITIAL_UPLOAD_FORM: UploadForm = {
    title: "",
    category: "",
    country: "",
    version: "1.0",
    status: "DRAFT",
    file: null,
};

const ACCEPTED_FILE_TYPES = [
    ".pdf",
    ".doc",
    ".docx",
    ".txt",
];

const MAX_FILE_SIZE = 25 * 1024 * 1024;

/**
 * ============================================================================
 * STAT CARD
 * ============================================================================
 */

function StatCard({
    title,
    value,
    icon: Icon,
    color,
}: {
    title: string;
    value: number;
    icon: React.ElementType;
    color: string;
}) {
    return (
        <div className="rounded-3xl border border-white/10 bg-white/5 backdrop-blur-xl p-6 shadow-sm transition hover:shadow-md">
            <div className="flex items-center justify-between">
                <div>
                    <p className="text-sm font-medium text-slate-400">
                        {title}
                    </p>

                    <h3 className="mt-2 text-3xl font-black text-white">
                        {value.toLocaleString()}
                    </h3>
                </div>

                <div className={`rounded-2xl p-3 ${color}`}>
                    <Icon size={24} />
                </div>
            </div>
        </div>
    );
}

/**
 * ============================================================================
 * STATUS BADGE
 * ============================================================================
 */

function StatusBadge({
    status,
}: {
    status: KnowledgeDocumentStatus;
}) {
    const styles: Record<
        KnowledgeDocumentStatus,
        string
    > = {
        PUBLISHED:
            "bg-emerald-500/10 text-emerald-300",

        DRAFT:
            "bg-white/10 text-slate-300",

        REVIEW:
            "bg-amber-500/10 text-amber-300",
    };

    return (
        <span
            className={`inline-flex rounded-full px-3 py-1 text-xs font-semibold ${styles[status] ?? "bg-white/10 text-slate-300"}`}
        >
            {status}
        </span>
    );
}

/**
 * ============================================================================
 * FORMAT DATE
 * ============================================================================
 */

function formatDate(value: string): string {
    if (!value) {
        return "—";
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return "—";
    }

    return date.toLocaleDateString(undefined, {
        year: "numeric",
        month: "short",
        day: "numeric",
    });
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
    required = false,
    type = "text",
}: {
    label: string;
    value: string;
    onChange: (
        event: React.ChangeEvent<HTMLInputElement>
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
                onChange={onChange}
                placeholder={placeholder}
                required={required}
                className="w-full rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm text-white outline-none transition placeholder:text-slate-500 focus:border-blue-400 focus:ring-4 focus:ring-blue-500/20"
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
}: {
    label: string;
    value: string;
    onChange: (
        event: React.ChangeEvent<HTMLSelectElement>
    ) => void;
    children: React.ReactNode;
}) {
    return (
        <div>
            <label className="mb-2 block text-sm font-semibold text-slate-300">
                {label}
            </label>

            <select
                value={value}
                onChange={onChange}
                className="w-full rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm text-white outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-500/20"
            >
                {children}
            </select>
        </div>
    );
}

/**
 * ============================================================================
 * MODAL
 * ============================================================================
 */

function Modal({
    title,
    description,
    icon: Icon,
    children,
    onClose,
    submitting,
}: {
    title: string;
    description: string;
    icon: React.ElementType;
    children: React.ReactNode;
    onClose: () => void;
    submitting: boolean;
}) {
    useEffect(() => {
        const handleEscape = (event: KeyboardEvent) => {
            if (event.key === "Escape" && !submitting) {
                onClose();
            }
        };

        window.addEventListener(
            "keydown",
            handleEscape
        );

        return () => {
            window.removeEventListener(
                "keydown",
                handleEscape
            );
        };
    }, [onClose, submitting]);

    return (
        <div
            className="fixed inset-0 z-[100] flex items-center justify-center bg-slate-950/60 p-4 backdrop-blur-sm"
            onMouseDown={(event) => {
                if (
                    event.target === event.currentTarget &&
                    !submitting
                ) {
                    onClose();
                }
            }}
        >
            <div className="max-h-[92vh] w-full max-w-3xl overflow-hidden rounded-3xl border border-white/10 bg-[#1F314A] backdrop-blur-xl shadow-2xl shadow-black/40">
                <div className="flex items-start justify-between border-b border-white/10 p-6">
                    <div className="flex items-start gap-4">
                        <div className="rounded-2xl bg-blue-500/10 p-3 text-blue-300">
                            <Icon size={24} />
                        </div>

                        <div>
                            <h2 className="text-xl font-black text-white">
                                {title}
                            </h2>

                            <p className="mt-1 max-w-xl text-sm leading-6 text-slate-400">
                                {description}
                            </p>
                        </div>
                    </div>

                    <button
                        type="button"
                        onClick={onClose}
                        disabled={submitting}
                        className="rounded-xl p-2 text-slate-400 transition hover:bg-white/10 hover:text-white disabled:opacity-40"
                        aria-label="Close"
                    >
                        <X size={20} />
                    </button>
                </div>

                <div className="max-h-[calc(92vh-110px)] overflow-y-auto p-6">
                    {children}
                </div>
            </div>
        </div>
    );
}

/**
 * ============================================================================
 * KNOWLEDGE BASE PAGE
 * ============================================================================
 */

export default function KnowledgeBasePage() {
    const [documents, setDocuments] =
        useState<KnowledgeDocumentSummary[]>([]);

    const [statistics, setStatistics] =
        useState<KnowledgeBaseStats | null>(null);

    const [search, setSearch] =
        useState("");

    const [categoryFilter, setCategoryFilter] =
        useState("");

    const [statusFilter, setStatusFilter] =
        useState<KnowledgeDocumentStatus | "">("");

    const [page, setPage] =
        useState(0);

    const [totalPages, setTotalPages] =
        useState(0);

    const [totalElements, setTotalElements] =
        useState(0);

    const [loading, setLoading] =
        useState(true);

    const [statsLoading, setStatsLoading] =
        useState(true);

    const [refreshing, setRefreshing] =
        useState(false);

    const [deletingId, setDeletingId] =
        useState<number | null>(null);

    const [indexingId, setIndexingId] =
        useState<number | null>(null);

    const [modal, setModal] =
        useState<ModalType>(null);

    const [articleForm, setArticleForm] =
        useState<ArticleForm>(
            INITIAL_ARTICLE_FORM
        );

    const [uploadForm, setUploadForm] =
        useState<UploadForm>(
            INITIAL_UPLOAD_FORM
        );

    const [submittingArticle, setSubmittingArticle] =
        useState(false);

    const [uploadingKnowledge, setUploadingKnowledge] =
        useState(false);

    const fileInputRef =
        useRef<HTMLInputElement | null>(null);

    /**
     * =========================================================================
     * LOAD DOCUMENTS
     * =========================================================================
     */

    const loadDocuments = useCallback(
        async () => {
            setLoading(true);

            try {
                const response =
                    await getKnowledgeDocuments({
                        search:
                            search.trim() ||
                            undefined,

                        category:
                            categoryFilter ||
                            undefined,

                        status:
                            statusFilter ||
                            undefined,

                        page,

                        size: 20,
                    });

                setDocuments(
                    response.content ?? []
                );

                setTotalPages(
                    response.totalPages ?? 0
                );

                setTotalElements(
                    response.totalElements ?? 0
                );
            } catch (error) {
                console.error(
                    "Failed to load knowledge documents:",
                    error
                );

                toast.error(
                    "Unable to load knowledge base."
                );
            } finally {
                setLoading(false);
            }
        },
        [
            search,
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

    const loadStatistics = useCallback(
        async () => {
            setStatsLoading(true);

            try {
                const response =
                    await getKnowledgeBaseStats();

                setStatistics(response);
            } catch (error) {
                console.error(
                    "Failed to load knowledge base statistics:",
                    error
                );

                toast.error(
                    "Unable to load knowledge statistics."
                );
            } finally {
                setStatsLoading(false);
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
        void loadDocuments();
    }, [loadDocuments]);

    useEffect(() => {
        void loadStatistics();
    }, [loadStatistics]);

    /**
     * =========================================================================
     * CATEGORIES
     * =========================================================================
     */

    const categories = useMemo(() => {
        return Array.from(
            new Set(
                documents
                    .map(
                        (document) =>
                            document.category
                    )
                    .filter(Boolean)
            )
        ).sort();
    }, [documents]);

    /**
     * =========================================================================
     * OPEN NEW ARTICLE
     * =========================================================================
     */

    const openNewArticle = () => {
        setArticleForm(
            INITIAL_ARTICLE_FORM
        );

        setModal("ARTICLE");
    };

    /**
     * =========================================================================
     * OPEN UPLOAD MODAL
     * =========================================================================
     */

    const openUploadKnowledge = () => {
        setUploadForm(
            INITIAL_UPLOAD_FORM
        );

        setModal("UPLOAD");
    };

    /**
     * =========================================================================
     * CLOSE MODAL
     * =========================================================================
     */

    const closeModal = () => {
        if (
            submittingArticle ||
            uploadingKnowledge
        ) {
            return;
        }

        setModal(null);
    };

    /**
     * =========================================================================
     * CREATE ARTICLE
     * =========================================================================
     */

    const handleCreateArticle = async (
        event: React.FormEvent<HTMLFormElement>
    ) => {
        event.preventDefault();

        const title =
            articleForm.title.trim();

        const category =
            articleForm.category.trim();

        const country =
            articleForm.country.trim();

        const version =
            articleForm.version.trim();

        const content =
            articleForm.content.trim();

        if (!title) {
            toast.error(
                "Article title is required."
            );

            return;
        }

        if (!category) {
            toast.error(
                "Category is required."
            );

            return;
        }

        if (!country) {
            toast.error(
                "Country is required."
            );

            return;
        }

        if (!content) {
            toast.error(
                "Article content is required."
            );

            return;
        }

        setSubmittingArticle(true);

        try {
            await createKnowledgeDocument({
                title,
                category,
                country,
                version:
                    version || "1.0",
                status:
                    articleForm.status,
                content,
            });

            toast.success(
                "Knowledge article created successfully."
            );

            setModal(null);

            setArticleForm(
                INITIAL_ARTICLE_FORM
            );

            setPage(0);

            await Promise.all([
                loadDocuments(),
                loadStatistics(),
            ]);
        } catch (error) {
            console.error(
                "Failed to create knowledge article:",
                error
            );

            toast.error(
                "Unable to create knowledge article."
            );
        } finally {
            setSubmittingArticle(false);
        }
    };

    /**
     * =========================================================================
     * FILE SELECTION
     * =========================================================================
     */

    const handleFileChange = (
        event: React.ChangeEvent<HTMLInputElement>
    ) => {
        const file =
            event.target.files?.[0] ??
            null;

        if (!file) {
            return;
        }

        const extension =
            `.${file.name.split(".").pop()?.toLowerCase()}`;

        if (
            !ACCEPTED_FILE_TYPES.includes(
                extension
            )
        ) {
            toast.error(
                "Unsupported file type. Use PDF, DOC, DOCX or TXT."
            );

            event.target.value = "";

            return;
        }

        if (
            file.size >
            MAX_FILE_SIZE
        ) {
            toast.error(
                "File is too large. Maximum allowed size is 25 MB."
            );

            event.target.value = "";

            return;
        }

        setUploadForm(
            (current) => ({
                ...current,
                file,
                title:
                    current.title.trim() ||
                    file.name.replace(
                        /\.[^/.]+$/,
                        ""
                    ),
            })
        );
    };

    /**
     * =========================================================================
     * UPLOAD KNOWLEDGE
     * =========================================================================
     */

    const handleUploadKnowledge = async (
        event: React.FormEvent<HTMLFormElement>
    ) => {
        event.preventDefault();

        const file =
            uploadForm.file;

        if (!file) {
            toast.error(
                "Please select a knowledge file."
            );

            return;
        }

        if (
            !uploadForm.title.trim()
        ) {
            toast.error(
                "Knowledge title is required."
            );

            return;
        }

        if (
            !uploadForm.category.trim()
        ) {
            toast.error(
                "Category is required."
            );

            return;
        }

        if (
            !uploadForm.country.trim()
        ) {
            toast.error(
                "Country is required."
            );

            return;
        }

        setUploadingKnowledge(true);

        try {
            await uploadKnowledgeDocument({
                file,

                title:
                    uploadForm.title.trim(),

                category:
                    uploadForm.category.trim(),

                country:
                    uploadForm.country.trim(),

                version:
                    uploadForm.version.trim() ||
                    "1.0",

                status:
                    uploadForm.status,
            });

            toast.success(
                "Knowledge document uploaded successfully."
            );

            setModal(null);

            setUploadForm(
                INITIAL_UPLOAD_FORM
            );

            if (fileInputRef.current) {
                fileInputRef.current.value =
                    "";
            }

            setPage(0);

            await Promise.all([
                loadDocuments(),
                loadStatistics(),
            ]);
        } catch (error) {
            console.error(
                "Failed to upload knowledge document:",
                error
            );

            toast.error(
                "Unable to upload knowledge document."
            );
        } finally {
            setUploadingKnowledge(false);
        }
    };

    /**
     * =========================================================================
     * REFRESH
     * =========================================================================
     */

    const handleRefresh = async () => {
        if (refreshing) {
            return;
        }

        setRefreshing(true);

        try {
            await Promise.all([
                loadDocuments(),
                loadStatistics(),
            ]);

            toast.success(
                "Knowledge base refreshed."
            );
        } finally {
            setRefreshing(false);
        }
    };

    /**
     * =========================================================================
     * DELETE
     * =========================================================================
     */

    const handleDelete = async (
        document: KnowledgeDocumentSummary
    ) => {
        const confirmed =
            window.confirm(
                `Delete "${document.title}"? This action cannot be undone.`
            );

        if (!confirmed) {
            return;
        }

        setDeletingId(
            document.id
        );

        try {
            await deleteKnowledgeDocument(
                document.id
            );

            toast.success(
                "Knowledge document deleted."
            );

            await Promise.all([
                loadDocuments(),
                loadStatistics(),
            ]);
        } catch (error) {
            console.error(
                "Failed to delete knowledge document:",
                error
            );

            toast.error(
                "Unable to delete knowledge document."
            );
        } finally {
            setDeletingId(null);
        }
    };

    /**
     * =========================================================================
     * INDEX
     * =========================================================================
     */

    const handleIndex = async (
        document: KnowledgeDocumentSummary
    ) => {
        if (document.aiIndexed) {
            return;
        }

        setIndexingId(
            document.id
        );

        try {
            await indexKnowledgeDocument(
                document.id
            );

            toast.success(
                "Knowledge document marked as AI indexed."
            );

            await Promise.all([
                loadDocuments(),
                loadStatistics(),
            ]);
        } catch (error) {
            console.error(
                "Failed to index knowledge document:",
                error
            );

            toast.error(
                "Unable to index knowledge document."
            );
        } finally {
            setIndexingId(null);
        }
    };

    /**
     * =========================================================================
     * CLEAR FILTERS
     * =========================================================================
     */

    const clearFilters = () => {
        setSearch("");
        setCategoryFilter("");
        setStatusFilter("");
        setPage(0);
    };

    const hasFilters =
        Boolean(
            search ||
            categoryFilter ||
            statusFilter
        );

    /**
     * =========================================================================
     * RENDER
     * =========================================================================
     */

    return (
        <div className="min-h-screen">
            <div className="w-full px-4 py-8 sm:px-6 lg:px-8">

                {/* ============================================================
                    HEADER
                    ============================================================ */}

                <div className="mb-8 flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">

                    <div>
                        <div className="flex items-center gap-3">
                            <div className="rounded-2xl bg-blue-500/10 p-3 text-blue-300">
                                <BookOpen size={28} />
                            </div>

                            <div>
                                <h1 className="text-3xl font-black tracking-tight text-white sm:text-4xl">
                                    Knowledge Base Center
                                </h1>

                                <p className="mt-1 text-slate-300">
                                    Manage immigration regulations,
                                    visa policies, compliance
                                    documentation and AI knowledge
                                    sources.
                                </p>
                            </div>
                        </div>
                    </div>

                    <div className="flex flex-wrap gap-3">

                        {/* NEW ARTICLE */}

                        <button
                            type="button"
                            onClick={openNewArticle}
                            className="flex items-center gap-2 rounded-2xl bg-blue-600 px-5 py-3 font-semibold text-white shadow-sm transition hover:bg-blue-700 active:scale-[0.98]"
                        >
                            <Plus size={18} />

                            New Article
                        </button>

                        {/* UPLOAD */}

                        <button
                            type="button"
                            onClick={openUploadKnowledge}
                            className="flex items-center gap-2 rounded-2xl border border-white/15 bg-white/5 px-5 py-3 font-semibold text-slate-200 transition hover:bg-white/10 active:scale-[0.98]"
                        >
                            <Upload size={18} />

                            Upload Knowledge
                        </button>

                        {/* REFRESH */}

                        <button
                            type="button"
                            onClick={() =>
                                void handleRefresh()
                            }
                            disabled={refreshing}
                            className="flex items-center gap-2 rounded-2xl border border-white/15 bg-white/5 px-4 py-3 font-semibold text-slate-200 transition hover:bg-white/10 disabled:cursor-not-allowed disabled:opacity-50"
                            title="Refresh"
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

                {/* ============================================================
                    STATISTICS
                    ============================================================ */}

                <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-4">

                    <StatCard
                        title="Knowledge Articles"
                        value={
                            statistics?.totalDocuments ??
                            0
                        }
                        icon={BookOpen}
                        color="bg-blue-500/10 text-blue-300"
                    />

                    <StatCard
                        title="AI Indexed Sources"
                        value={
                            statistics?.aiIndexedDocuments ??
                            0
                        }
                        icon={BrainCircuit}
                        color="bg-violet-500/10 text-violet-300"
                    />

                    <StatCard
                        title="Countries Covered"
                        value={
                            statistics?.countriesCovered ??
                            0
                        }
                        icon={Globe2}
                        color="bg-emerald-500/10 text-emerald-300"
                    />

                    <StatCard
                        title="Categories"
                        value={
                            statistics?.categoriesCovered ??
                            0
                        }
                        icon={ShieldCheck}
                        color="bg-amber-500/10 text-amber-300"
                    />
                </div>

                {/* ============================================================
                    STATUS
                    ============================================================ */}

                <div className="mt-6 grid gap-4 sm:grid-cols-3">

                    <div className="rounded-2xl border border-emerald-500/20 bg-emerald-500/10 p-5">
                        <p className="text-sm font-medium text-emerald-300">
                            Published
                        </p>

                        <p className="mt-1 text-2xl font-black text-white">
                            {statistics?.publishedDocuments ??
                                0}
                        </p>
                    </div>

                    <div className="rounded-2xl border border-amber-500/20 bg-amber-500/10 p-5">
                        <p className="text-sm font-medium text-amber-300">
                            Under Review
                        </p>

                        <p className="mt-1 text-2xl font-black text-white">
                            {statistics?.reviewDocuments ??
                                0}
                        </p>
                    </div>

                    <div className="rounded-2xl border border-white/10 bg-white/5 backdrop-blur-xl p-5">
                        <p className="text-sm font-medium text-slate-300">
                            Pending AI Index
                        </p>

                        <p className="mt-1 text-2xl font-black text-white">
                            {statistics?.pendingAiIndexDocuments ??
                                0}
                        </p>
                    </div>
                </div>

                {/* ============================================================
                    FILTERS
                    ============================================================ */}

                <div className="mt-8 rounded-3xl border border-white/10 bg-white/5 backdrop-blur-xl p-5 shadow-sm">

                    <div className="grid gap-4 xl:grid-cols-[2fr_1fr_1fr_auto]">

                        <div className="relative">
                            <Search
                                size={18}
                                className="absolute left-4 top-3.5 text-slate-400"
                            />

                            <input
                                value={search}
                                onChange={(event) => {
                                    setSearch(
                                        event.target.value
                                    );

                                    setPage(0);
                                }}
                                placeholder="Search title, country or category..."
                                className="w-full rounded-2xl border border-white/15 bg-white/5 text-white placeholder:text-slate-500 py-3 pl-11 pr-4 text-sm outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-500/20"
                            />
                        </div>

                        <select
                            value={categoryFilter}
                            onChange={(event) => {
                                setCategoryFilter(
                                    event.target.value
                                );

                                setPage(0);
                            }}
                            className="rounded-2xl border border-white/15 bg-white/5 text-white px-4 py-3 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-500/20"
                        >
                            <option value="">
                                All Categories
                            </option>

                            {categories.map(
                                (category) => (
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
                            value={statusFilter}
                            onChange={(event) => {
                                setStatusFilter(
                                    event.target.value as
                                        | KnowledgeDocumentStatus
                                        | ""
                                );

                                setPage(0);
                            }}
                            className="rounded-2xl border border-white/15 bg-white/5 text-white px-4 py-3 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-500/20"
                        >
                            <option value="">
                                All Statuses
                            </option>

                            <option value="PUBLISHED">
                                Published
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
                            onClick={clearFilters}
                            disabled={!hasFilters}
                            className="flex items-center justify-center gap-2 rounded-2xl border border-white/15 px-5 py-3 text-sm font-semibold text-slate-200 transition hover:bg-white/10 disabled:cursor-not-allowed disabled:opacity-40"
                        >
                            <Filter size={18} />

                            Clear
                        </button>
                    </div>
                </div>

                {/* ============================================================
                    RESULTS SUMMARY
                    ============================================================ */}

                <div className="mt-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">

                    <p className="text-sm text-slate-300">
                        Showing{" "}
                        <span className="font-semibold text-white">
                            {documents.length}
                        </span>{" "}
                        of{" "}
                        <span className="font-semibold text-white">
                            {totalElements.toLocaleString()}
                        </span>{" "}
                        documents
                    </p>

                    {statsLoading && (
                        <div className="flex items-center gap-2 text-sm text-slate-400">
                            <Loader2
                                size={15}
                                className="animate-spin"
                            />

                            Updating statistics...
                        </div>
                    )}
                </div>

                {/* ============================================================
                    TABLE
                    ============================================================ */}

                <div className="mt-4 overflow-hidden rounded-3xl border border-white/10 bg-white/5 backdrop-blur-xl shadow-sm">

                    <div className="overflow-x-auto">

                        <table className="w-full min-w-[1100px]">

                            <thead className="bg-white/5">
                                <tr>
                                    <th className="px-5 py-4 text-left text-xs font-bold uppercase tracking-wide text-slate-400">
                                        Article
                                    </th>

                                    <th className="px-5 py-4 text-left text-xs font-bold uppercase tracking-wide text-slate-400">
                                        Category
                                    </th>

                                    <th className="px-5 py-4 text-left text-xs font-bold uppercase tracking-wide text-slate-400">
                                        Country
                                    </th>

                                    <th className="px-5 py-4 text-left text-xs font-bold uppercase tracking-wide text-slate-400">
                                        Version
                                    </th>

                                    <th className="px-5 py-4 text-left text-xs font-bold uppercase tracking-wide text-slate-400">
                                        Status
                                    </th>

                                    <th className="px-5 py-4 text-left text-xs font-bold uppercase tracking-wide text-slate-400">
                                        AI Indexed
                                    </th>

                                    <th className="px-5 py-4 text-left text-xs font-bold uppercase tracking-wide text-slate-400">
                                        Updated
                                    </th>

                                    <th className="px-5 py-4 text-left text-xs font-bold uppercase tracking-wide text-slate-400">
                                        Actions
                                    </th>
                                </tr>
                            </thead>

                            <tbody>

                                {loading ? (
                                    <tr>
                                        <td
                                            colSpan={8}
                                            className="px-5 py-16 text-center"
                                        >
                                            <Loader2
                                                size={30}
                                                className="mx-auto animate-spin text-blue-600"
                                            />

                                            <p className="mt-3 text-sm text-slate-400">
                                                Loading knowledge base...
                                            </p>
                                        </td>
                                    </tr>
                                ) : documents.length === 0 ? (
                                    <tr>
                                        <td
                                            colSpan={8}
                                            className="px-5 py-16 text-center"
                                        >
                                            <div className="mx-auto max-w-md">
                                                <AlertCircle
                                                    size={40}
                                                    className="mx-auto text-slate-400"
                                                />

                                                <h3 className="mt-4 font-bold text-white">
                                                    No knowledge documents found
                                                </h3>

                                                <p className="mt-2 text-sm text-slate-400">
                                                    Try changing your
                                                    search or filter
                                                    criteria, or create
                                                    your first article.
                                                </p>

                                                <button
                                                    type="button"
                                                    onClick={
                                                        openNewArticle
                                                    }
                                                    className="mt-5 inline-flex items-center gap-2 rounded-xl bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-blue-700"
                                                >
                                                    <Plus size={16} />

                                                    Create Article
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                ) : (
                                    documents.map(
                                        (document) => (
                                            <tr
                                                key={
                                                    document.id
                                                }
                                                className="border-t border-white/10 transition hover:bg-white/5"
                                            >
                                                <td className="px-5 py-4">
                                                    <div>
                                                        <p className="font-semibold text-white">
                                                            {
                                                                document.title
                                                            }
                                                        </p>

                                                        <p className="mt-1 text-xs text-slate-400">
                                                            ID #
                                                            {
                                                                document.id
                                                            }
                                                        </p>
                                                    </div>
                                                </td>

                                                <td className="px-5 py-4 text-sm text-slate-300">
                                                    {
                                                        document.category
                                                    }
                                                </td>

                                                <td className="px-5 py-4 text-sm text-slate-300">
                                                    {
                                                        document.country
                                                    }
                                                </td>

                                                <td className="px-5 py-4 text-sm font-semibold text-slate-200">
                                                    {
                                                        document.version
                                                    }
                                                </td>

                                                <td className="px-5 py-4">
                                                    <StatusBadge
                                                        status={
                                                            document.status
                                                        }
                                                    />
                                                </td>

                                                <td className="px-5 py-4">
                                                    {document.aiIndexed ? (
                                                        <span className="inline-flex items-center gap-1 rounded-full bg-emerald-500/10 px-3 py-1 text-xs font-semibold text-emerald-300">
                                                            <CheckCircle2
                                                                size={
                                                                    13
                                                                }
                                                            />

                                                            Indexed
                                                        </span>
                                                    ) : (
                                                        <button
                                                            type="button"
                                                            disabled={
                                                                indexingId ===
                                                                document.id
                                                            }
                                                            onClick={() =>
                                                                void handleIndex(
                                                                    document
                                                                )
                                                            }
                                                            className="inline-flex items-center gap-1 rounded-full bg-amber-500/10 px-3 py-1 text-xs font-semibold text-amber-300 transition hover:bg-amber-500/20 disabled:opacity-50"
                                                        >
                                                            {indexingId ===
                                                            document.id ? (
                                                                <Loader2
                                                                    size={
                                                                        13
                                                                    }
                                                                    className="animate-spin"
                                                                />
                                                            ) : (
                                                                <BrainCircuit
                                                                    size={
                                                                        13
                                                                    }
                                                                />
                                                            )}

                                                            Index
                                                        </button>
                                                    )}
                                                </td>

                                                <td className="px-5 py-4">
                                                    <div className="text-sm">
                                                        <p className="text-slate-300">
                                                            {formatDate(
                                                                document.updatedAt
                                                            )}
                                                        </p>

                                                        <p className="mt-1 text-xs text-slate-400">
                                                            {
                                                                document.updatedBy
                                                            }
                                                        </p>
                                                    </div>
                                                </td>

                                                <td className="px-5 py-4">
                                                    <div className="flex gap-2">

                                                        <button
                                                            type="button"
                                                            title="View document"
                                                            className="rounded-xl bg-blue-500/10 p-2 text-blue-300 transition hover:bg-blue-500/20"
                                                        >
                                                            <Eye
                                                                size={
                                                                    18
                                                                }
                                                            />
                                                        </button>

                                                        <button
                                                            type="button"
                                                            title="Edit document"
                                                            className="rounded-xl bg-amber-500/10 p-2 text-amber-300 transition hover:bg-amber-500/20"
                                                        >
                                                            <FileText
                                                                size={
                                                                    18
                                                                }
                                                            />
                                                        </button>

                                                        <button
                                                            type="button"
                                                            title="AI index"
                                                            disabled={
                                                                document.aiIndexed ||
                                                                indexingId ===
                                                                    document.id
                                                            }
                                                            onClick={() =>
                                                                void handleIndex(
                                                                    document
                                                                )
                                                            }
                                                            className="rounded-xl bg-violet-500/10 p-2 text-violet-300 transition hover:bg-violet-500/20 disabled:cursor-not-allowed disabled:opacity-40"
                                                        >
                                                            <Database
                                                                size={
                                                                    18
                                                                }
                                                            />
                                                        </button>

                                                        <button
                                                            type="button"
                                                            title="Delete document"
                                                            disabled={
                                                                deletingId ===
                                                                document.id
                                                            }
                                                            onClick={() =>
                                                                void handleDelete(
                                                                    document
                                                                )
                                                            }
                                                            className="rounded-xl bg-red-500/10 p-2 text-red-300 transition hover:bg-red-500/20 disabled:opacity-50"
                                                        >
                                                            {deletingId ===
                                                            document.id ? (
                                                                <Loader2
                                                                    size={
                                                                        18
                                                                    }
                                                                    className="animate-spin"
                                                                />
                                                            ) : (
                                                                <Trash2
                                                                    size={
                                                                        18
                                                                    }
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

                {/* ============================================================
                    PAGINATION
                    ============================================================ */}

                {totalPages > 1 && (
                    <div className="mt-5 flex items-center justify-between rounded-2xl border border-white/10 bg-white/5 backdrop-blur-xl p-4">

                        <button
                            type="button"
                            disabled={page === 0}
                            onClick={() =>
                                setPage(
                                    (current) =>
                                        Math.max(
                                            current - 1,
                                            0
                                        )
                                )
                            }
                            className="rounded-xl border border-white/15 px-4 py-2 text-sm font-semibold text-slate-200 transition hover:bg-white/10 disabled:cursor-not-allowed disabled:opacity-40"
                        >
                            Previous
                        </button>

                        <span className="text-sm text-slate-300">
                            Page{" "}
                            <strong className="text-white">
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
                                    (current) =>
                                        Math.min(
                                            current + 1,
                                            totalPages - 1
                                        )
                                )
                            }
                            className="rounded-xl border border-white/15 px-4 py-2 text-sm font-semibold text-slate-200 transition hover:bg-white/10 disabled:cursor-not-allowed disabled:opacity-40"
                        >
                            Next
                        </button>
                    </div>
                )}

                {/* ============================================================
                    INFORMATION CARDS
                    ============================================================ */}

                <div className="mt-8 grid gap-6 lg:grid-cols-3">

                    <div className="rounded-3xl border border-white/10 bg-white/5 backdrop-blur-xl p-6 shadow-sm">
                        <BrainCircuit
                            className="mb-4 text-violet-400"
                            size={28}
                        />

                        <h3 className="font-bold text-white">
                            AI Knowledge Index
                        </h3>

                        <p className="mt-2 text-sm leading-6 text-slate-300">
                            Knowledge documents can be indexed for
                            retrieval-augmented AI workflows after
                            they have been reviewed and published.
                        </p>
                    </div>

                    <div className="rounded-3xl border border-white/10 bg-white/5 backdrop-blur-xl p-6 shadow-sm">
                        <Users
                            className="mb-4 text-blue-400"
                            size={28}
                        />

                        <h3 className="font-bold text-white">
                            Contributor Tracking
                        </h3>

                        <p className="mt-2 text-sm leading-6 text-slate-300">
                            Every document records its creator,
                            latest contributor and update timestamp.
                        </p>
                    </div>

                    <div className="rounded-3xl border border-white/10 bg-white/5 backdrop-blur-xl p-6 shadow-sm">
                        <ShieldCheck
                            className="mb-4 text-emerald-400"
                            size={28}
                        />

                        <h3 className="font-bold text-white">
                            Compliance Management
                        </h3>

                        <p className="mt-2 text-sm leading-6 text-slate-300">
                            Documents can move through draft,
                            review and published states before
                            becoming available to AI systems.
                        </p>
                    </div>
                </div>

                {/* ============================================================
                    CONNECTION STATUS
                    ============================================================ */}

                <div className="mt-8 rounded-3xl border border-blue-500/20 bg-blue-500/10 p-5">

                    <div className="flex items-start gap-3">

                        <CheckCircle2
                            className="mt-0.5 shrink-0 text-blue-300"
                        />

                        <div>
                            <p className="font-semibold text-white">
                                Knowledge Base Connected
                            </p>

                            <p className="mt-1 text-sm leading-6 text-blue-200">
                                Knowledge documents and statistics are
                                loaded directly from the Spring Boot
                                API and Oracle database.
                            </p>
                        </div>
                    </div>
                </div>
            </div>

            {/* =================================================================
                NEW ARTICLE MODAL
                ================================================================= */}

            {modal === "ARTICLE" && (
                <Modal
                    title="Create Knowledge Article"
                    description="Create a structured knowledge article that can be reviewed and eventually indexed by the AI knowledge system."
                    icon={BookOpen}
                    onClose={closeModal}
                    submitting={
                        submittingArticle
                    }
                >
                    <form
                        onSubmit={
                            handleCreateArticle
                        }
                        className="space-y-6"
                    >
                        <div className="grid gap-5 md:grid-cols-2">

                            <FormInput
                                label="Article title"
                                value={
                                    articleForm.title
                                }
                                onChange={(event) =>
                                    setArticleForm(
                                        (current) => ({
                                            ...current,
                                            title: event
                                                .target
                                                .value,
                                        })
                                    )
                                }
                                placeholder="e.g. Zimbabwe Work Visa Requirements"
                                required
                            />

                            <FormInput
                                label="Category"
                                value={
                                    articleForm.category
                                }
                                onChange={(event) =>
                                    setArticleForm(
                                        (current) => ({
                                            ...current,
                                            category:
                                                event
                                                    .target
                                                    .value,
                                        })
                                    )
                                }
                                placeholder="e.g. Work Visas"
                                required
                            />

                            <FormInput
                                label="Country"
                                value={
                                    articleForm.country
                                }
                                onChange={(event) =>
                                    setArticleForm(
                                        (current) => ({
                                            ...current,
                                            country:
                                                event
                                                    .target
                                                    .value,
                                        })
                                    )
                                }
                                placeholder="e.g. Zimbabwe"
                                required
                            />

                            <FormInput
                                label="Version"
                                value={
                                    articleForm.version
                                }
                                onChange={(event) =>
                                    setArticleForm(
                                        (current) => ({
                                            ...current,
                                            version:
                                                event
                                                    .target
                                                    .value,
                                        })
                                    )
                                }
                                placeholder="1.0"
                            />

                            <FormSelect
                                label="Publication status"
                                value={
                                    articleForm.status
                                }
                                onChange={(event) =>
                                    setArticleForm(
                                        (current) => ({
                                            ...current,
                                            status:
                                                event
                                                    .target
                                                    .value as KnowledgeDocumentStatus,
                                        })
                                    )
                                }
                            >
                                <option value="DRAFT">
                                    Draft
                                </option>

                                <option value="REVIEW">
                                    Review
                                </option>

                                <option value="PUBLISHED">
                                    Published
                                </option>
                            </FormSelect>
                        </div>

                        <div>
                            <label className="mb-2 block text-sm font-semibold text-slate-300">
                                Article content
                                <span className="ml-1 text-red-400">
                                    *
                                </span>
                            </label>

                            <textarea
                                value={
                                    articleForm.content
                                }
                                onChange={(event) =>
                                    setArticleForm(
                                        (current) => ({
                                            ...current,
                                            content:
                                                event
                                                    .target
                                                    .value,
                                        })
                                    )
                                }
                                placeholder="Write the immigration policy, requirements, procedures, eligibility criteria or other authoritative knowledge here..."
                                rows={12}
                                required
                                className="w-full resize-y rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm leading-6 text-white outline-none transition placeholder:text-slate-500 focus:border-blue-400 focus:ring-4 focus:ring-blue-500/20"
                            />

                            <p className="mt-2 text-xs text-slate-400">
                                {articleForm.content.length.toLocaleString()}{" "}
                                characters
                            </p>
                        </div>

                        <div className="flex justify-end gap-3 border-t border-white/10 pt-5">

                            <button
                                type="button"
                                onClick={closeModal}
                                disabled={
                                    submittingArticle
                                }
                                className="rounded-2xl border border-white/15 px-5 py-3 text-sm font-semibold text-slate-200 transition hover:bg-white/10 disabled:opacity-50"
                            >
                                Cancel
                            </button>

                            <button
                                type="submit"
                                disabled={
                                    submittingArticle
                                }
                                className="inline-flex items-center gap-2 rounded-2xl bg-blue-600 px-6 py-3 text-sm font-semibold text-white transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
                            >
                                {submittingArticle ? (
                                    <>
                                        <Loader2
                                            size={17}
                                            className="animate-spin"
                                        />

                                        Creating...
                                    </>
                                ) : (
                                    <>
                                        <Plus
                                            size={17}
                                        />

                                        Create Article
                                    </>
                                )}
                            </button>
                        </div>
                    </form>
                </Modal>
            )}

            {/* =================================================================
                UPLOAD KNOWLEDGE MODAL
                ================================================================= */}

            {modal === "UPLOAD" && (
                <Modal
                    title="Upload Knowledge"
                    description="Upload an authoritative knowledge document for your immigration knowledge base."
                    icon={Upload}
                    onClose={closeModal}
                    submitting={
                        uploadingKnowledge
                    }
                >
                    <form
                        onSubmit={
                            handleUploadKnowledge
                        }
                        className="space-y-6"
                    >
                        <div
                            onClick={() =>
                                fileInputRef.current?.click()
                            }
                            onDragOver={(event) => {
                                event.preventDefault();
                            }}
                            onDrop={(event) => {
                                event.preventDefault();

                                const file =
                                    event.dataTransfer.files?.[0];

                                if (!file) {
                                    return;
                                }

                                const extension =
                                    `.${file.name.split(".").pop()?.toLowerCase()}`;

                                if (
                                    !ACCEPTED_FILE_TYPES.includes(
                                        extension
                                    )
                                ) {
                                    toast.error(
                                        "Unsupported file type."
                                    );

                                    return;
                                }

                                if (
                                    file.size >
                                    MAX_FILE_SIZE
                                ) {
                                    toast.error(
                                        "File is too large. Maximum allowed size is 25 MB."
                                    );

                                    return;
                                }

                                setUploadForm(
                                    (current) => ({
                                        ...current,
                                        file,
                                        title:
                                            current.title.trim() ||
                                            file.name.replace(
                                                /\.[^/.]+$/,
                                                ""
                                            ),
                                    })
                                );
                            }}
                            className="cursor-pointer rounded-3xl border-2 border-dashed border-white/15 bg-white/5 p-8 text-center transition hover:border-blue-400/50 hover:bg-blue-500/10"
                        >
                            <input
                                ref={
                                    fileInputRef
                                }
                                type="file"
                                accept={ACCEPTED_FILE_TYPES.join(
                                    ","
                                )}
                                onChange={
                                    handleFileChange
                                }
                                className="hidden"
                            />

                            <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-blue-500/10 text-blue-300">
                                <Upload
                                    size={26}
                                />
                            </div>

                            {uploadForm.file ? (
                                <>
                                    <h3 className="mt-4 font-bold text-white">
                                        {
                                            uploadForm
                                                .file
                                                .name
                                        }
                                    </h3>

                                    <p className="mt-1 text-sm text-slate-400">
                                        {(
                                            uploadForm
                                                .file
                                                .size /
                                            1024 /
                                            1024
                                        ).toFixed(
                                            2
                                        )}{" "}
                                        MB
                                    </p>

                                    <p className="mt-3 text-sm font-semibold text-blue-300">
                                        Click to choose
                                        another file
                                    </p>
                                </>
                            ) : (
                                <>
                                    <h3 className="mt-4 font-bold text-white">
                                        Select knowledge document
                                    </h3>

                                    <p className="mt-2 text-sm text-slate-400">
                                        Drag and drop a file
                                        here, or click to
                                        browse.
                                    </p>

                                    <p className="mt-3 text-xs text-slate-500">
                                        PDF, DOC, DOCX or TXT
                                        • Maximum 25 MB
                                    </p>
                                </>
                            )}
                        </div>

                        <div className="grid gap-5 md:grid-cols-2">

                            <FormInput
                                label="Document title"
                                value={
                                    uploadForm.title
                                }
                                onChange={(event) =>
                                    setUploadForm(
                                        (current) => ({
                                            ...current,
                                            title:
                                                event
                                                    .target
                                                    .value,
                                        })
                                    )
                                }
                                placeholder="e.g. Immigration Regulations 2026"
                                required
                            />

                            <FormInput
                                label="Category"
                                value={
                                    uploadForm.category
                                }
                                onChange={(event) =>
                                    setUploadForm(
                                        (current) => ({
                                            ...current,
                                            category:
                                                event
                                                    .target
                                                    .value,
                                        })
                                    )
                                }
                                placeholder="e.g. Immigration Regulations"
                                required
                            />

                            <FormInput
                                label="Country"
                                value={
                                    uploadForm.country
                                }
                                onChange={(event) =>
                                    setUploadForm(
                                        (current) => ({
                                            ...current,
                                            country:
                                                event
                                                    .target
                                                    .value,
                                        })
                                    )
                                }
                                placeholder="e.g. Zimbabwe"
                                required
                            />

                            <FormInput
                                label="Version"
                                value={
                                    uploadForm.version
                                }
                                onChange={(event) =>
                                    setUploadForm(
                                        (current) => ({
                                            ...current,
                                            version:
                                                event
                                                    .target
                                                    .value,
                                        })
                                    )
                                }
                                placeholder="1.0"
                            />

                            <FormSelect
                                label="Publication status"
                                value={
                                    uploadForm.status
                                }
                                onChange={(event) =>
                                    setUploadForm(
                                        (current) => ({
                                            ...current,
                                            status:
                                                event
                                                    .target
                                                    .value as KnowledgeDocumentStatus,
                                        })
                                    )
                                }
                            >
                                <option value="DRAFT">
                                    Draft
                                </option>

                                <option value="REVIEW">
                                    Review
                                </option>

                                <option value="PUBLISHED">
                                    Published
                                </option>
                            </FormSelect>
                        </div>

                        <div className="flex justify-end gap-3 border-t border-white/10 pt-5">

                            <button
                                type="button"
                                onClick={closeModal}
                                disabled={
                                    uploadingKnowledge
                                }
                                className="rounded-2xl border border-white/15 px-5 py-3 text-sm font-semibold text-slate-200 transition hover:bg-white/10 disabled:opacity-50"
                            >
                                Cancel
                            </button>

                            <button
                                type="submit"
                                disabled={
                                    uploadingKnowledge ||
                                    !uploadForm.file
                                }
                                className="inline-flex items-center gap-2 rounded-2xl bg-blue-600 px-6 py-3 text-sm font-semibold text-white transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
                            >
                                {uploadingKnowledge ? (
                                    <>
                                        <Loader2
                                            size={17}
                                            className="animate-spin"
                                        />

                                        Uploading...
                                    </>
                                ) : (
                                    <>
                                        <Upload
                                            size={17}
                                        />

                                        Upload Knowledge
                                    </>
                                )}
                            </button>
                        </div>
                    </form>
                </Modal>
            )}
        </div>
    );
}

