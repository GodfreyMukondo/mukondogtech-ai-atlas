
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
  Bell,
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
} from "lucide-react";

import Sidebar from "../Sidebar";

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
        event.key.toLowerCase() ===
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
    <div className="min-h-screen bg-[#F8F6F1]">
      {/* ====================================================
          SHARED SIDEBAR
      ==================================================== */}

      <Sidebar
        mobileOpen={mobileSidebarOpen}
        onClose={closeMobileSidebar}
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
              border-slate-200
              bg-white
              shadow-2xl
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
                border-slate-200
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
                  bg-[#0B1736]
                  text-[#F4B81A]
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
                  text-[#0B1736]
                  outline-none
                  placeholder:text-slate-400
                "
              />

              <kbd
                className="
                  hidden
                  rounded-lg
                  border
                  border-slate-200
                  bg-slate-50
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
                  hover:bg-slate-100
                  hover:text-slate-700
                  focus:outline-none
                  focus:ring-2
                  focus:ring-[#F4B81A]
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
                            hover:bg-[#F8F6F1]
                            focus:bg-[#F8F6F1]
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
                              bg-slate-100
                              text-slate-600
                              transition
                              group-hover:bg-[#0B1736]
                              group-hover:text-[#F4B81A]
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
                                text-[#0B1736]
                              "
                            >
                              {item.title}
                            </p>

                            <p
                              className="
                                truncate
                                text-xs
                                text-slate-500
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
                              text-slate-300
                              transition
                              group-hover:text-[#0B1736]
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
                      bg-slate-100
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
                      text-[#0B1736]
                    "
                  >
                    No results found
                  </p>

                  <p
                    className="
                      mt-1
                      text-xs
                      text-slate-500
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
                border-slate-200
                bg-slate-50
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
          transition-[padding]
          duration-300
          lg:pl-[320px]
        "
      >
        {/* ==================================================
            ADMIN HEADER
        ================================================== */}

        <header
          className="
            sticky
            top-0
            z-30
            border-b
            border-slate-200/80
            bg-[#F8F6F1]/95
            shadow-[0_4px_20px_rgba(11,23,54,0.04)]
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
                  rounded-xl
                  border
                  border-slate-200
                  bg-white
                  text-[#0B1736]
                  shadow-sm
                  transition
                  duration-200
                  hover:border-slate-300
                  hover:bg-slate-50
                  focus:outline-none
                  focus:ring-2
                  focus:ring-[#F4B81A]
                  focus:ring-offset-2
                  focus:ring-offset-[#F8F6F1]
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
                      text-slate-500
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
                    text-[#0B1736]
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
                    text-slate-500
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
                  rounded-xl
                  border
                  border-slate-200
                  bg-white
                  px-3
                  text-sm
                  font-semibold
                  text-slate-500
                  shadow-sm
                  transition
                  duration-200
                  hover:border-[#F4B81A]
                  hover:text-[#0B1736]
                  focus:outline-none
                  focus:ring-2
                  focus:ring-[#F4B81A]
                  focus:ring-offset-2
                  focus:ring-offset-[#F8F6F1]
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
                    border-slate-200
                    bg-slate-50
                    px-1.5
                    py-0.5
                    text-[10px]
                    font-bold
                    text-slate-400
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
                  rounded-xl
                  border
                  border-slate-200
                  bg-white
                  text-slate-600
                  shadow-sm
                  transition
                  duration-200
                  hover:border-[#F4B81A]
                  hover:bg-slate-50
                  hover:text-[#0B1736]
                  focus:outline-none
                  focus:ring-2
                  focus:ring-[#F4B81A]
                  focus:ring-offset-2
                  focus:ring-offset-[#F8F6F1]
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

              <button
                type="button"
                aria-label="View notifications"
                className="
                  relative
                  flex
                  h-10
                  w-10
                  items-center
                  justify-center
                  rounded-xl
                  border
                  border-slate-200
                  bg-white
                  text-slate-600
                  shadow-sm
                  transition
                  duration-200
                  hover:border-slate-300
                  hover:bg-slate-50
                  hover:text-[#0B1736]
                  focus:outline-none
                  focus:ring-2
                  focus:ring-[#F4B81A]
                  focus:ring-offset-2
                  focus:ring-offset-[#F8F6F1]
                "
              >
                <Bell
                  size={18}
                  aria-hidden="true"
                />

                <span
                  className="
                    absolute
                    right-2
                    top-2
                    h-2
                    w-2
                    rounded-full
                    bg-[#F4B81A]
                    ring-2
                    ring-white
                  "
                  aria-hidden="true"
                />
              </button>

              {/* ==================================================
                  ADMIN ACCESS
              ================================================== */}

              <div
                className="
                  hidden
                  items-center
                  gap-2
                  rounded-xl
                  border
                  border-slate-200
                  bg-white
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
                    rounded-lg
                    bg-[#0B1736]
                    text-[#F4B81A]
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
                      text-[#0B1736]
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
          <div
            className="
              mx-auto
              w-full
              max-w-[1800px]
            "
          >
            {children ?? <Outlet />}
          </div>
        </main>

        {/* ====================================================
            FOOTER
        ==================================================== */}

        <footer
          className="
            border-t
            border-slate-200/80
            bg-white/50
            px-4
            py-4
            sm:px-6
            lg:px-8
          "
        >
          <div
            className="
              mx-auto
              flex
              w-full
              max-w-[1800px]
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

