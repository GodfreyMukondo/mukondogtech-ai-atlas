
// ============================================================
// ADMIN LAYOUT
// Production-ready administrative application shell.
//
// File:
// src/components/layouts/AdminLayout.tsx
//
// Responsibilities:
// - Render the shared Sidebar
// - Render the admin header
// - Render functional global admin search
// - Render admin page metadata
// - Handle mobile sidebar state
// - Handle Escape key
// - Handle "/" keyboard shortcut for search
// - Prevent body scrolling when mobile navigation is open
// - Render nested admin routes through Outlet
// - Render administrative footer
//
// IMPORTANT:
// The actual Sidebar component lives at:
// src/components/Sidebar.tsx
//
// Therefore:
// import Sidebar from "../Sidebar";
// ============================================================

import React, {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";

import {
  Outlet,
  useLocation,
  useNavigate,
} from "react-router-dom";

import {
  AlertTriangle,
  Bell,
  BellOff,
  Briefcase,
  CheckCircle2,
  ChevronRight,
  Command,
  FileText,
  LayoutDashboard,
  Menu,
  Search,
  Settings,
  ShieldCheck,
  Users,
  X,
  Brain,
  BarChart3,
  BookOpen,
  Scale,
  Activity,
  ClipboardList,
  Compass,
} from "lucide-react";

import Sidebar from "../Sidebar";
import notificationApi from "../../api/notificationApi";
import type { Notification } from "../../types/notification";

// ============================================================
// TYPES
// ============================================================

interface AdminLayoutProps {
  children?: React.ReactNode;
}

interface AdminPageMeta {
  title: string;
  description: string;
}

interface AdminSearchItem {
  title: string;
  description: string;
  path: string;
  keywords: string[];
  icon: React.ComponentType<{
    size?: number;
    className?: string;
  }>;
}

// ============================================================
// ADMIN PAGE METADATA
// ============================================================

const ADMIN_PAGE_META: Record<string, AdminPageMeta> = {
  "/admin": {
    title: "Admin Control Center",
    description:
      "Monitor your immigration platform, users, applications, and AI services.",
  },

  "/admin/analytics": {
    title: "Analytics",
    description:
      "Review platform performance, business metrics, and operational insights.",
  },

  "/admin/users": {
    title: "User Management",
    description:
      "Manage platform users, roles, accounts, and access permissions.",
  },

  "/admin/users/create": {
    title: "Create User",
    description:
      "Create and configure a new platform user account.",
  },

  "/admin/cases/create": {
    title: "Create Case",
    description:
      "Submit a new immigration application on behalf of a user.",
  },

  "/admin/applications": {
    title: "Application Review",
    description:
      "Review and manage immigration applications submitted by users.",
  },

  "/admin/ai": {
    title: "AI Model Monitoring",
    description:
      "Monitor AI model performance, accuracy, availability, and usage.",
  },

  "/admin/knowledge": {
    title: "Knowledge Base",
    description:
      "Manage the documents and knowledge used by the AI platform.",
  },

  "/admin/rules": {
    title: "Immigration Rules",
    description:
      "Manage immigration rules, requirements, and regulatory information.",
  },

  "/admin/pathways": {
    title: "Pathway Catalogue",
    description:
      "Create, review, and publish immigration pathways for assessment.",
  },

  "/admin/requirements": {
    title: "Requirement Definitions",
    description:
      "Author reusable, regulatory-sourced requirement definitions.",
  },

  "/admin/security": {
    title: "Security Center",
    description:
      "Monitor security events, authentication activity, and platform protection.",
  },

  "/admin/system": {
    title: "System Health",
    description:
      "Monitor APIs, services, infrastructure, and system availability.",
  },

  "/admin/audit": {
    title: "Audit Logs",
    description:
      "Review administrative actions and platform activity.",
  },

  "/admin/settings": {
    title: "Global Settings",
    description:
      "Configure platform-wide administrative settings.",
  },
};

// ============================================================
// DEFAULT PAGE METADATA
// ============================================================

const DEFAULT_PAGE_META: AdminPageMeta = {
  title: "Administration",
  description:
    "Manage and monitor the LUCCO AI platform.",
};

// ============================================================
// ADMIN SEARCH ITEMS
// ============================================================

const ADMIN_SEARCH_ITEMS: AdminSearchItem[] = [
  {
    title: "Admin Control Center",
    description:
      "Monitor your immigration platform and AI services.",
    path: "/admin",
    keywords: [
      "dashboard",
      "home",
      "control",
      "center",
      "overview",
      "admin",
    ],
    icon: LayoutDashboard,
  },

  {
    title: "Analytics",
    description:
      "Review business metrics and operational insights.",
    path: "/admin/analytics",
    keywords: [
      "analytics",
      "metrics",
      "statistics",
      "reports",
      "performance",
    ],
    icon: BarChart3,
  },

  {
    title: "User Management",
    description:
      "Manage users, roles, accounts, and permissions.",
    path: "/admin/users",
    keywords: [
      "users",
      "user",
      "accounts",
      "roles",
      "permissions",
      "customers",
    ],
    icon: Users,
  },

  {
    title: "Create User",
    description:
      "Create a new platform user account.",
    path: "/admin/users/create",
    keywords: [
      "create",
      "new",
      "user",
      "account",
      "register",
    ],
    icon: Users,
  },

  {
    title: "Create Case",
    description:
      "Submit a new immigration application on behalf of a user.",
    path: "/admin/cases/create",
    keywords: [
      "create",
      "new",
      "case",
      "application",
      "immigration",
    ],
    icon: Briefcase,
  },

  {
    title: "Application Review",
    description:
      "Review and manage immigration applications.",
    path: "/admin/applications",
    keywords: [
      "applications",
      "application",
      "cases",
      "review",
      "immigration",
    ],
    icon: FileText,
  },

  {
    title: "AI Model Monitoring",
    description:
      "Monitor AI performance, accuracy, and availability.",
    path: "/admin/ai",
    keywords: [
      "ai",
      "artificial intelligence",
      "model",
      "models",
      "machine learning",
      "accuracy",
      "llm",
    ],
    icon: Brain,
  },

  {
    title: "Knowledge Base",
    description:
      "Manage documents and AI knowledge.",
    path: "/admin/knowledge",
    keywords: [
      "knowledge",
      "documents",
      "document",
      "rag",
      "vector",
      "database",
    ],
    icon: BookOpen,
  },

  {
    title: "Immigration Rules",
    description:
      "Manage immigration requirements and regulatory information.",
    path: "/admin/rules",
    keywords: [
      "rules",
      "immigration",
      "requirements",
      "regulations",
      "visa",
    ],
    icon: Scale,
  },

  {
    title: "Pathway Catalogue",
    description:
      "Create, review, and publish immigration pathways.",
    path: "/admin/pathways",
    keywords: [
      "pathway",
      "pathways",
      "catalogue",
      "immigration",
      "publish",
    ],
    icon: Compass,
  },

  {
    title: "Requirement Definitions",
    description:
      "Author reusable, regulatory-sourced requirements.",
    path: "/admin/requirements",
    keywords: [
      "requirement",
      "requirements",
      "eligibility",
      "regulatory",
    ],
    icon: ClipboardList,
  },

  {
    title: "Security Center",
    description:
      "Monitor security events and platform protection.",
    path: "/admin/security",
    keywords: [
      "security",
      "authentication",
      "protection",
      "fraud",
      "access",
    ],
    icon: ShieldCheck,
  },

  {
    title: "System Health",
    description:
      "Monitor APIs, services, infrastructure, and availability.",
    path: "/admin/system",
    keywords: [
      "system",
      "health",
      "api",
      "availability",
      "infrastructure",
      "server",
    ],
    icon: Activity,
  },

  {
    title: "Audit Logs",
    description:
      "Review administrative actions and platform activity.",
    path: "/admin/audit",
    keywords: [
      "audit",
      "logs",
      "activity",
      "history",
      "administration",
    ],
    icon: ClipboardList,
  },

  {
    title: "Global Settings",
    description:
      "Configure platform-wide administrative settings.",
    path: "/admin/settings",
    keywords: [
      "settings",
      "configuration",
      "preferences",
      "global",
    ],
    icon: Settings,
  },
];

// ============================================================
// PAGE METADATA HELPER
// ============================================================

function getAdminPageMeta(
  pathname: string,
): AdminPageMeta {
  const exactMatch =
    ADMIN_PAGE_META[pathname];

  if (exactMatch) {
    return exactMatch;
  }

  if (
    pathname.startsWith(
      "/admin/users/",
    )
  ) {
    return {
      title: "User Management",
      description:
        "Manage platform users, roles, accounts, and access permissions.",
    };
  }

  if (
    pathname.startsWith(
      "/admin/applications/",
    )
  ) {
    return {
      title: "Application Review",
      description:
        "Review and manage immigration applications submitted by users.",
    };
  }

  return DEFAULT_PAGE_META;
}

// ============================================================
// NOTIFICATION HELPERS
// ============================================================

function resolveNotificationIcon(type: string) {
  const normalized = type.trim().toUpperCase();

  if (
    normalized === "DOCUMENT_FLAGGED" ||
    normalized === "APPLICATION_REJECTED"
  ) {
    return AlertTriangle;
  }

  if (
    normalized === "DOCUMENT_PROCESSED" ||
    normalized === "APPLICATION_SUBMITTED"
  ) {
    return FileText;
  }

  return CheckCircle2;
}

function formatRelativeTime(isoDate: string): string {
  const date = new Date(isoDate);

  if (Number.isNaN(date.getTime())) {
    return "";
  }

  const diffMs = Date.now() - date.getTime();
  const diffMinutes = Math.round(diffMs / 60000);

  if (diffMinutes < 1) {
    return "Just now";
  }

  if (diffMinutes < 60) {
    return `${diffMinutes}m ago`;
  }

  const diffHours = Math.round(diffMinutes / 60);

  if (diffHours < 24) {
    return `${diffHours}h ago`;
  }

  const diffDays = Math.round(diffHours / 24);

  return `${diffDays}d ago`;
}

// ============================================================
// COMPONENT
// ============================================================

export default function AdminLayout({
  children,
}: AdminLayoutProps) {
  const location = useLocation();
  const navigate = useNavigate();

  // ==========================================================
  // MOBILE SIDEBAR STATE
  // ==========================================================

  const [
    mobileSidebarOpen,
    setMobileSidebarOpen,
  ] = useState(false);

  // ==========================================================
  // DESKTOP SIDEBAR COLLAPSE
  //
  // Owned here (rather than left to Sidebar's own internal state)
  // so the header/content offset below can size itself to match the
  // sidebar's actual current width. Previously this layout used a
  // fixed lg:pl-[320px]/lg:pl-[336px] offset sized for the expanded
  // sidebar only - collapsing the sidebar (which Sidebar still did
  // internally) left a large dead gap between the collapsed 88px
  // sidebar and the header/content, which still assumed 290px.
  // ==========================================================

  const [
    sidebarCollapsed,
    setSidebarCollapsed,
  ] = useState(false);

  useEffect(() => {
    try {
      const stored = window.localStorage.getItem(
        "mgt-sidebar-collapsed",
      );

      if (stored === "true") {
        setSidebarCollapsed(true);
      }
    } catch {
      // Ignore storage failures - default to expanded.
    }
  }, []);

  const sidebarContentOffset =
    sidebarCollapsed ? "88px" : "290px";

  // ==========================================================
  // SEARCH STATE
  // ==========================================================

  const [
    searchOpen,
    setSearchOpen,
  ] = useState(false);

  const [
    searchQuery,
    setSearchQuery,
  ] = useState("");

  const searchInputRef =
    useRef<HTMLInputElement | null>(null);

  // ==========================================================
  // NOTIFICATIONS STATE
  // ==========================================================

  const [
    notificationsOpen,
    setNotificationsOpen,
  ] = useState(false);

  const [
    notifications,
    setNotifications,
  ] = useState<Notification[]>([]);

  const [
    unreadCount,
    setUnreadCount,
  ] = useState(0);

  const [
    notificationsLoading,
    setNotificationsLoading,
  ] = useState(false);

  const notificationsRef =
    useRef<HTMLDivElement | null>(null);

  const refreshUnreadCount = useCallback(async () => {
    try {
      const count = await notificationApi.getUnreadCount();
      setUnreadCount(count);
    } catch {
      // Non-critical background refresh; the bell simply keeps its last count.
    }
  }, []);

  useEffect(() => {
    refreshUnreadCount();

    const intervalId = window.setInterval(
      refreshUnreadCount,
      60000,
    );

    return () => window.clearInterval(intervalId);
  }, [refreshUnreadCount]);

  const loadNotifications = useCallback(async () => {
    setNotificationsLoading(true);

    try {
      const data = await notificationApi.getNotifications();
      setNotifications(data);
    } catch {
      setNotifications([]);
    } finally {
      setNotificationsLoading(false);
    }
  }, []);

  const handleMarkAsRead = useCallback(async (id: number) => {
    setNotifications((previous) =>
      previous.map((item) =>
        item.id === id ? { ...item, read: true } : item,
      ),
    );

    setUnreadCount((previous) => Math.max(0, previous - 1));

    try {
      await notificationApi.markAsRead(id);
    } catch {
      // The next refresh reconciles state if this call failed.
    }
  }, []);

  const handleMarkAllAsRead = useCallback(async () => {
    setNotifications((previous) =>
      previous.map((item) => ({ ...item, read: true })),
    );

    setUnreadCount(0);

    try {
      await notificationApi.markAllAsRead();
    } catch {
      // The next refresh reconciles state if this call failed.
    }
  }, []);

  const handleNotificationClick = useCallback(
    (notification: Notification) => {
      setNotificationsOpen(false);

      if (!notification.read) {
        handleMarkAsRead(notification.id);
      }

      navigate(notification.link ?? "/admin");
    },
    [handleMarkAsRead, navigate],
  );

  const toggleNotifications = useCallback(() => {
    setNotificationsOpen((previous) => {
      const next = !previous;

      if (next) {
        loadNotifications();
      }

      return next;
    });
  }, [loadNotifications]);

  // Closing the panel on an outside click or Escape mirrors the same
  // dropdown behavior already used for the dashboard header's bell.
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      const target = event.target as Node;

      if (
        notificationsRef.current &&
        !notificationsRef.current.contains(target)
      ) {
        setNotificationsOpen(false);
      }
    }

    function handleEscape(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setNotificationsOpen(false);
      }
    }

    document.addEventListener("mousedown", handleClickOutside);
    document.addEventListener("keydown", handleEscape);

    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
      document.removeEventListener("keydown", handleEscape);
    };
  }, []);

  // ==========================================================
  // PAGE METADATA
  // ==========================================================

  const pageMeta = useMemo(
    () =>
      getAdminPageMeta(
        location.pathname,
      ),
    [location.pathname],
  );

  // ==========================================================
  // SEARCH RESULTS
  // ==========================================================

  const searchResults = useMemo(() => {
    const query =
      searchQuery
        .trim()
        .toLowerCase();

    if (!query) {
      return ADMIN_SEARCH_ITEMS.slice(
        0,
        7,
      );
    }

    const terms =
      query
        .split(/\s+/)
        .filter(Boolean);

    return ADMIN_SEARCH_ITEMS
      .map((item) => {
        const searchableText =
          [
            item.title,
            item.description,
            ...item.keywords,
          ]
            .join(" ")
            .toLowerCase();

        let score = 0;

        for (const term of terms) {
          if (
            item.title
              .toLowerCase()
              .includes(term)
          ) {
            score += 10;
          }

          if (
            item.keywords.some(
              (keyword) =>
                keyword
                  .toLowerCase()
                  .includes(term),
            )
          ) {
            score += 6;
          }

          if (
            searchableText.includes(
              term,
            )
          ) {
            score += 2;
          }
        }

        return {
          item,
          score,
        };
      })
      .filter(
        ({ score }) =>
          score > 0,
      )
      .sort(
        (a, b) =>
          b.score - a.score,
      )
      .map(
        ({ item }) =>
          item,
      )
      .slice(0, 8);
  }, [searchQuery]);

  // ==========================================================
  // SIDEBAR HANDLERS
  // ==========================================================

  const openMobileSidebar =
    useCallback(() => {
      setMobileSidebarOpen(true);
    }, []);

  const closeMobileSidebar =
    useCallback(() => {
      setMobileSidebarOpen(false);
    }, []);

  // ==========================================================
  // SEARCH HANDLERS
  // ==========================================================

  const openSearch =
    useCallback(() => {
      setSearchOpen(true);

      window.setTimeout(() => {
        searchInputRef.current?.focus();
      }, 50);
    }, []);

  const closeSearch =
    useCallback(() => {
      setSearchOpen(false);
      setSearchQuery("");
    }, []);

  const executeSearch =
    useCallback(
      (path: string) => {
        closeSearch();
        navigate(path);
      },
      [closeSearch, navigate],
    );

  // ==========================================================
  // CLOSE MOBILE SIDEBAR ON ROUTE CHANGE
  // ==========================================================

  useEffect(() => {
    setMobileSidebarOpen(false);
  }, [location.pathname]);

  // ==========================================================
  // BODY SCROLL LOCK
  // ==========================================================

  useEffect(() => {
    if (!mobileSidebarOpen) {
      return;
    }

    const previousOverflow =
      document.body.style.overflow;

    document.body.style.overflow =
      "hidden";

    return () => {
      document.body.style.overflow =
        previousOverflow;
    };
  }, [mobileSidebarOpen]);

  // ==========================================================
  // KEYBOARD SHORTCUTS
  // ==========================================================

  useEffect(() => {
    const handleKeyDown = (
      event: KeyboardEvent,
    ) => {
      // ------------------------------------------------------
      // Escape
      // ------------------------------------------------------

      if (
        event.key === "Escape"
      ) {
        if (searchOpen) {
          closeSearch();
          return;
        }

        if (mobileSidebarOpen) {
          closeMobileSidebar();
        }

        return;
      }

      // ------------------------------------------------------
      // "/" opens search
      // ------------------------------------------------------

      if (
        event.key === "/" &&
        !searchOpen
      ) {
        const target =
          event.target as HTMLElement | null;

        const tagName =
          target?.tagName?.toLowerCase();

        const isTyping =
          tagName === "input" ||
          tagName === "textarea" ||
          target?.isContentEditable;

        if (isTyping) {
          return;
        }

        event.preventDefault();

        openSearch();
      }

      // ------------------------------------------------------
      // CTRL/CMD + K opens search
      // ------------------------------------------------------

      if (
        event.key?.toLowerCase() ===
          "k" &&
        (event.ctrlKey ||
          event.metaKey)
      ) {
        event.preventDefault();

        if (searchOpen) {
          searchInputRef.current?.focus();
        } else {
          openSearch();
        }
      }
    };

    window.addEventListener(
      "keydown",
      handleKeyDown,
    );

    return () => {
      window.removeEventListener(
        "keydown",
        handleKeyDown,
      );
    };
  }, [
    searchOpen,
    mobileSidebarOpen,
    openSearch,
    closeSearch,
    closeMobileSidebar,
  ]);

  // ==========================================================
  // SEARCH ENTER KEY
  // ==========================================================

  const handleSearchKeyDown =
    useCallback(
      (
        event: React.KeyboardEvent<HTMLInputElement>,
      ) => {
        if (
          event.key !== "Enter"
        ) {
          return;
        }

        const firstResult =
          searchResults[0];

        if (firstResult) {
          executeSearch(
            firstResult.path,
          );
        }
      },
      [
        searchResults,
        executeSearch,
      ],
    );

  // ==========================================================
  // RENDER
  // ==========================================================

  return (
    <div className="min-h-screen">
      {/* ====================================================
          SHARED SIDEBAR
      ==================================================== */}

      <Sidebar
        mobileOpen={mobileSidebarOpen}
        onClose={closeMobileSidebar}
        collapsed={sidebarCollapsed}
        onCollapsedChange={setSidebarCollapsed}
      />

      {/* ====================================================
          MOBILE OVERLAY
      ==================================================== */}

      {mobileSidebarOpen && (
        <button
          type="button"
          aria-label="Close admin navigation"
          onClick={closeMobileSidebar}
          className="
            fixed
            inset-0
            z-40
            cursor-default
            bg-slate-950/50
            backdrop-blur-sm
            lg:hidden
          "
        />
      )}

      {/* ====================================================
          GLOBAL SEARCH MODAL
      ==================================================== */}

      {searchOpen && (
        <div
          className="
            fixed
            inset-0
            z-[100]
            flex
            items-start
            justify-center
            bg-slate-950/40
            px-4
            pt-[10vh]
            backdrop-blur-sm
            sm:px-6
          "
          onMouseDown={(event) => {
            if (
              event.target ===
              event.currentTarget
            ) {
              closeSearch();
            }
          }}
        >
          <div
            className="
              w-full
              max-w-2xl
              overflow-hidden
              rounded-2xl
              border
              border-white/10
              bg-[#1F314A]
              shadow-2xl
              shadow-black/40
              backdrop-blur-xl
            "
            role="dialog"
            aria-modal="true"
            aria-label="Search administration"
          >
            {/* Search header */}

            <div
              className="
                flex
                items-center
                gap-3
                border-b
                border-white/10
                px-4
                py-4
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
                  bg-[#C6A15B]/15
                  text-[#C6A15B]
                "
              >
                <Search
                  size={19}
                  aria-hidden="true"
                />
              </div>

              <input
                ref={searchInputRef}
                value={searchQuery}
                onChange={(event) =>
                  setSearchQuery(
                    event.target.value,
                  )
                }
                onKeyDown={
                  handleSearchKeyDown
                }
                type="search"
                autoComplete="off"
                spellCheck={false}
                placeholder="Search admin pages..."
                aria-label="Search admin pages"
                className="
                  min-w-0
                  flex-1
                  border-0
                  bg-transparent
                  text-base
                  font-semibold
                  text-white
                  outline-none
                  placeholder:text-slate-400
                "
              />

              <kbd
                className="
                  hidden
                  rounded-lg
                  border
                  border-white/10
                  bg-white/5
                  px-2
                  py-1
                  text-[10px]
                  font-bold
                  text-slate-400
                  sm:inline-flex
                "
              >
                ESC
              </kbd>

              <button
                type="button"
                onClick={closeSearch}
                aria-label="Close search"
                className="
                  flex
                  h-9
                  w-9
                  shrink-0
                  items-center
                  justify-center
                  rounded-lg
                  text-slate-400
                  transition
                  hover:bg-white/10
                  hover:text-white
                  focus:outline-none
                  focus:ring-2
                  focus:ring-[#C6A15B]
                "
              >
                <X
                  size={18}
                  aria-hidden="true"
                />
              </button>
            </div>

            {/* Results */}

            <div
              className="
                max-h-[60vh]
                overflow-y-auto
                p-2
              "
            >
              {searchResults.length >
              0 ? (
                <div className="space-y-1">
                  {searchResults.map(
                    (item) => {
                      const Icon =
                        item.icon;

                      return (
                        <button
                          key={item.path}
                          type="button"
                          onClick={() =>
                            executeSearch(
                              item.path,
                            )
                          }
                          className="
                            group
                            flex
                            w-full
                            items-center
                            gap-3
                            rounded-xl
                            px-3
                            py-3
                            text-left
                            transition
                            hover:bg-white/5
                            focus:bg-white/5
                            focus:outline-none
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
                              bg-white/5
                              text-slate-400
                              transition
                              group-hover:bg-[#C6A15B]/15
                              group-hover:text-[#C6A15B]
                            "
                          >
                            <Icon
                              size={18}
                              aria-hidden="true"
                            />
                          </div>

                          <div className="min-w-0 flex-1">
                            <p
                              className="
                                truncate
                                text-sm
                                font-black
                                text-white
                              "
                            >
                              {item.title}
                            </p>

                            <p
                              className="
                                truncate
                                text-xs
                                text-slate-400
                              "
                            >
                              {
                                item.description
                              }
                            </p>
                          </div>

                          <ChevronRight
                            size={17}
                            className="
                              shrink-0
                              text-slate-500
                              transition
                              group-hover:text-[#C6A15B]
                            "
                            aria-hidden="true"
                          />
                        </button>
                      );
                    },
                  )}
                </div>
              ) : (
                <div
                  className="
                    px-6
                    py-12
                    text-center
                  "
                >
                  <div
                    className="
                      mx-auto
                      mb-3
                      flex
                      h-12
                      w-12
                      items-center
                      justify-center
                      rounded-2xl
                      bg-white/5
                      text-slate-400
                    "
                  >
                    <Search
                      size={22}
                      aria-hidden="true"
                    />
                  </div>

                  <p
                    className="
                      text-sm
                      font-black
                      text-white
                    "
                  >
                    No results found
                  </p>

                  <p
                    className="
                      mt-1
                      text-xs
                      text-slate-400
                    "
                  >
                    Try searching for users,
                    analytics, applications,
                    AI, security, or settings.
                  </p>
                </div>
              )}
            </div>

            {/* Search footer */}

            <div
              className="
                flex
                items-center
                justify-between
                border-t
                border-white/10
                bg-white/5
                px-4
                py-3
              "
            >
              <div
                className="
                  flex
                  items-center
                  gap-2
                  text-[11px]
                  font-semibold
                  text-slate-400
                "
              >
                <Command
                  size={13}
                  aria-hidden="true"
                />

                <span>
                  Quick navigation
                </span>
              </div>

              <span
                className="
                  text-[10px]
                  font-bold
                  uppercase
                  tracking-wider
                  text-slate-400
                "
              >
                Enter to open
              </span>
            </div>
          </div>
        </div>
      )}

      {/* ====================================================
          MAIN APPLICATION AREA
      ==================================================== */}

      <div
        className="
          min-h-screen
          pt-20
          transition-[padding]
          duration-300
          lg:pl-[var(--admin-content-offset)]
        "
        style={
          {
            "--admin-content-offset":
              sidebarContentOffset,
          } as React.CSSProperties
        }
      >
        {/* ==================================================
            ADMIN HEADER

            Fixed to the true viewport edges (inset-x-0) rather than
            sticky within the padded content column, so the header bar
            reaches both the left and right edges of the screen. Its
            own content no longer needs to be padded clear of the
            sidebar - the shared Sidebar component now sits below this
            header (top-20) rather than beside it, so the header spans
            the full width uninterrupted on both mobile and desktop.
        ================================================== */}

        <header
          className="
            fixed
            inset-x-0
            top-0
            z-40
            border-b
            border-white/10
            bg-[#0B1F3A]/95
            shadow-[0_4px_20px_rgba(7, 20, 38,0.25)]
            backdrop-blur-xl
          "
        >
          <div
            className="
              flex
              min-h-20
              items-center
              justify-between
              gap-4
              px-4
              py-3
              sm:px-6
              lg:px-8
            "
          >
            {/* ==================================================
                LEFT SIDE
            ================================================== */}

            <div
              className="
                flex
                min-w-0
                flex-1
                items-center
                gap-3
              "
            >
              {/* Mobile menu */}

              <button
                type="button"
                onClick={
                  openMobileSidebar
                }
                aria-label="Open admin navigation"
                aria-expanded={
                  mobileSidebarOpen
                }
                aria-controls="admin-sidebar"
                className="
                  flex
                  h-11
                  w-11
                  shrink-0
                  items-center
                  justify-center
                  rounded-2xl
                  border
                  border-white/10
                  bg-white/5
                  backdrop-blur-sm
                  text-white
                  shadow-sm
                  transition-all
                  duration-200
                  hover:-translate-y-0.5
                  hover:border-white/20
                  hover:bg-white/10
                  focus:outline-none
                  focus:ring-2
                  focus:ring-[#C6A15B]
                  focus:ring-offset-2
                  focus:ring-offset-[#0B1F3A]
                  lg:hidden
                "
              >
                <Menu
                  size={21}
                  aria-hidden="true"
                />
              </button>

              {/* Page information */}

              <div
                className="
                  min-w-0
                  flex-1
                "
              >
                {/* Breadcrumb */}

                <div
                  className="
                    hidden
                    items-center
                    gap-1.5
                    text-xs
                    font-semibold
                    text-slate-400
                    sm:flex
                  "
                >
                  <span>
                    Administration
                  </span>

                  <ChevronRight
                    size={13}
                    aria-hidden="true"
                  />

                  <span
                    className="
                      truncate
                      text-slate-300
                    "
                  >
                    {pageMeta.title}
                  </span>
                </div>

                {/* Title */}

                <h1
                  className="
                    truncate
                    text-lg
                    font-black
                    tracking-tight
                    text-white
                    sm:text-xl
                  "
                >
                  {pageMeta.title}
                </h1>

                {/* Description */}

                <p
                  className="
                    hidden
                    max-w-3xl
                    truncate
                    text-sm
                    text-slate-300
                    md:block
                  "
                >
                  {pageMeta.description}
                </p>
              </div>
            </div>

            {/* ==================================================
                RIGHT SIDE
            ================================================== */}

            <div
              className="
                flex
                shrink-0
                items-center
                gap-2
                sm:gap-3
              "
            >
              {/* ==================================================
                  SEARCH
              ================================================== */}

              <button
                type="button"
                onClick={openSearch}
                aria-label="Search administration"
                className="
                  hidden
                  h-10
                  min-w-[150px]
                  items-center
                  gap-2
                  rounded-2xl
                  border
                  border-white/10
                  bg-white/5
                  backdrop-blur-sm
                  px-3
                  text-sm
                  font-semibold
                  text-slate-300
                  shadow-sm
                  transition-all
                  duration-200
                  hover:-translate-y-0.5
                  hover:border-[#C6A15B]/50
                  hover:text-white
                  focus:outline-none
                  focus:ring-2
                  focus:ring-[#C6A15B]
                  focus:ring-offset-2
                  focus:ring-offset-[#0B1F3A]
                  sm:flex
                "
              >
                <Search
                  size={17}
                  aria-hidden="true"
                />

                <span className="flex-1 text-left">
                  Search
                </span>

                <kbd
                  className="
                    hidden
                    rounded-md
                    border
                    border-white/10
                    bg-white/10
                    px-1.5
                    py-0.5
                    text-[10px]
                    font-bold
                    text-slate-300
                    md:inline-block
                  "
                >
                  /
                </kbd>
              </button>

              {/* Mobile search */}

              <button
                type="button"
                onClick={openSearch}
                aria-label="Search administration"
                className="
                  flex
                  h-10
                  w-10
                  items-center
                  justify-center
                  rounded-2xl
                  border
                  border-white/10
                  bg-white/5
                  backdrop-blur-sm
                  text-slate-300
                  shadow-sm
                  transition-all
                  duration-200
                  hover:-translate-y-0.5
                  hover:border-[#C6A15B]/50
                  hover:bg-white/10
                  hover:text-white
                  focus:outline-none
                  focus:ring-2
                  focus:ring-[#C6A15B]
                  focus:ring-offset-2
                  focus:ring-offset-[#0B1F3A]
                  sm:hidden
                "
              >
                <Search
                  size={18}
                  aria-hidden="true"
                />
              </button>

              {/* ==================================================
                  NOTIFICATIONS
              ================================================== */}

              <div
                className="relative"
                ref={notificationsRef}
              >
                <button
                  type="button"
                  onClick={toggleNotifications}
                  aria-label="View notifications"
                  aria-expanded={notificationsOpen}
                  className="
                    relative
                    flex
                    h-10
                    w-10
                    items-center
                    justify-center
                    rounded-2xl
                    border
                    border-white/10
                    bg-white/5
                    backdrop-blur-sm
                    text-slate-300
                    shadow-sm
                    transition-all
                    duration-200
                    hover:-translate-y-0.5
                    hover:border-white/20
                    hover:bg-white/10
                    hover:text-white
                    focus:outline-none
                    focus:ring-2
                    focus:ring-[#C6A15B]
                    focus:ring-offset-2
                    focus:ring-offset-[#0B1F3A]
                  "
                >
                  <Bell
                    size={18}
                    aria-hidden="true"
                  />

                  {unreadCount > 0 && (
                    <span
                      aria-hidden="true"
                      className="
                        absolute
                        right-1
                        top-1
                        flex
                        h-4
                        min-w-4
                        items-center
                        justify-center
                        rounded-full
                        bg-red-500
                        px-1
                        text-[9px]
                        font-extrabold
                        text-white
                        ring-2
                        ring-[#0B1F3A]
                      "
                    >
                      {unreadCount > 9 ? "9+" : unreadCount}
                    </span>
                  )}
                </button>

                {notificationsOpen && (
                  <div
                    role="menu"
                    aria-label="Notifications"
                    className="
                      absolute
                      right-0
                      top-[calc(100%+10px)]
                      z-40
                      w-80
                      max-w-[calc(100vw-2rem)]
                      overflow-hidden
                      rounded-3xl
                      border
                      border-white/10
                      bg-[#1F314A]
                      shadow-2xl
                      shadow-black/40
                      backdrop-blur-xl
                    "
                  >
                    <div className="flex items-center justify-between border-b border-white/10 px-4 py-3">
                      <p className="text-sm font-bold text-white">
                        Notifications
                      </p>

                      {notifications.some((item) => !item.read) && (
                        <button
                          type="button"
                          onClick={handleMarkAllAsRead}
                          className="text-xs font-semibold text-slate-400 transition hover:text-white"
                        >
                          Mark all read
                        </button>
                      )}
                    </div>

                    <div className="max-h-96 overflow-y-auto">
                      {notificationsLoading ? (
                        <div className="flex flex-col items-center gap-2 px-6 py-8 text-center">
                          <p className="text-xs text-slate-400">
                            Loading notifications...
                          </p>
                        </div>
                      ) : notifications.length === 0 ? (
                        <div className="flex flex-col items-center gap-2 px-6 py-8 text-center">
                          <span className="flex h-10 w-10 items-center justify-center rounded-full bg-white/5 text-slate-400">
                            <BellOff size={18} />
                          </span>

                          <p className="text-sm font-semibold text-white">
                            You&apos;re all caught up
                          </p>

                          <p className="text-xs text-slate-400">
                            No new notifications right now.
                          </p>
                        </div>
                      ) : (
                        <ul className="divide-y divide-white/10">
                          {notifications.map((notification) => {
                            const Icon = resolveNotificationIcon(
                              notification.type,
                            );

                            return (
                              <li key={notification.id}>
                                <button
                                  type="button"
                                  onClick={() =>
                                    handleNotificationClick(notification)
                                  }
                                  className={`
                                    flex w-full items-start gap-3 px-4 py-3.5 text-left transition hover:bg-white/5
                                    ${notification.read ? "" : "bg-[#C6A15B]/[0.06]"}
                                  `}
                                >
                                  <span
                                    className={`
                                      mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-full
                                      ${
                                        notification.type === "DOCUMENT_FLAGGED" ||
                                        notification.type === "APPLICATION_REJECTED"
                                          ? "bg-amber-500/15 text-amber-300"
                                          : "bg-white/10 text-slate-300"
                                      }
                                    `}
                                  >
                                    <Icon size={15} />
                                  </span>

                                  <span className="min-w-0 flex-1">
                                    <span className="flex items-center gap-2">
                                      <span className="truncate text-sm font-semibold text-white">
                                        {notification.title}
                                      </span>

                                      {!notification.read && (
                                        <span
                                          aria-hidden="true"
                                          className="h-1.5 w-1.5 shrink-0 rounded-full bg-[#C6A15B]"
                                        />
                                      )}
                                    </span>

                                    {notification.message && (
                                      <span className="mt-0.5 block text-xs leading-5 text-slate-400">
                                        {notification.message}
                                      </span>
                                    )}

                                    <span className="mt-1 block text-[11px] font-medium text-slate-500">
                                      {formatRelativeTime(notification.createdAt)}
                                    </span>
                                  </span>
                                </button>
                              </li>
                            );
                          })}
                        </ul>
                      )}
                    </div>
                  </div>
                )}
              </div>

              {/* ==================================================
                  ADMIN ACCESS
              ================================================== */}

              <div
                className="
                  hidden
                  items-center
                  gap-2
                  rounded-2xl
                  border
                  border-white/10
                  bg-white/5
                  backdrop-blur-sm
                  px-3
                  py-2
                  shadow-sm
                  md:flex
                "
              >
                <div
                  className="
                    flex
                    h-8
                    w-8
                    items-center
                    justify-center
                    rounded-xl
                    bg-gradient-to-br
                    from-[#C6A15B]
                    to-[#A8894D]
                    text-[#071426]
                    shadow-sm
                  "
                >
                  <ShieldCheck
                    size={17}
                    aria-hidden="true"
                  />
                </div>

                <div className="leading-tight">
                  <p
                    className="
                      text-[10px]
                      font-bold
                      uppercase
                      tracking-wider
                      text-slate-400
                    "
                  >
                    Access
                  </p>

                  <p
                    className="
                      text-xs
                      font-black
                      text-white
                    "
                  >
                    Administrator
                  </p>
                </div>
              </div>
            </div>
          </div>
        </header>

        {/* ====================================================
            MAIN CONTENT
        ==================================================== */}

        <main
          className="
            min-h-[calc(100vh-5rem)]
            px-4
            py-6
            sm:px-6
            sm:py-8
            lg:px-8
          "
        >
          {children ?? <Outlet />}
        </main>

        {/* ====================================================
            FOOTER
        ==================================================== */}

        <footer
          className="
            border-t
            border-white/10
            bg-white/5
            backdrop-blur-xl
            px-4
            py-4
            sm:px-6
            lg:px-8
          "
        >
          <div
            className="
              flex
              w-full
              flex-col
              gap-2
              text-xs
              text-slate-400
              sm:flex-row
              sm:items-center
              sm:justify-between
            "
          >
            <p>
              ©{" "}
              {new Date().getFullYear()}{" "}
              MukondoGTech AI. All rights
              reserved.
            </p>

            <div
              className="
                flex
                items-center
                gap-2
                font-semibold
              "
            >
              <span
                className="
                  h-2
                  w-2
                  rounded-full
                  bg-emerald-500
                  shadow-[0_0_0_3px_rgba(16,185,129,0.12)]
                "
                aria-hidden="true"
              />

              <span>
                Administrative system operational
              </span>
            </div>
          </div>
        </footer>
      </div>
    </div>
  );
}

