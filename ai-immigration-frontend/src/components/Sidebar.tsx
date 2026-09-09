"use client";

import React, { useEffect, useMemo, useState } from "react";
import { NavLink, useLocation } from "react-router-dom";
import {
  Activity,
  BarChart3,
  BrainCircuit,
  ChevronsLeft,
  ChevronsRight,
  ClipboardCheck,
  Clock,
  CreditCard,
  Database,
  FileCheck,
  FileText,
  History,
  LayoutDashboard,
  LifeBuoy,
  LockKeyhole,
  LogOut,
  MessageCircle,
  Receipt,
  Scale,
  ScanSearch,
  Search,
  Settings,
  ShieldCheck,
  Sparkles,
  Telescope,
  UserCircle,
  Users,
  X,
} from "lucide-react";

import { useAuth } from "../features/auth/hooks/useAuth";

interface SidebarProps {
  mobileOpen?: boolean;
  onClose?: () => void;
}

interface NavigationItem {
  label: string;
  path: string;
  icon: React.ElementType;
  badge?: string | number;
  badgeTone?: "gold" | "red" | "emerald" | "blue";
}

interface NavigationGroup {
  label: string;
  items: NavigationItem[];
}

/* ============================================================================
   NAVIGATION
============================================================================ */

const userNavigationGroups: NavigationGroup[] = [
  {
    label: "Workspace",
    items: [
      {
        label: "Dashboard",
        path: "/dashboard",
        icon: LayoutDashboard,
      },
      {
        label: "AI Immigration Assistant",
        path: "/dashboard/chat",
        icon: MessageCircle,
        badge: "AI",
        badgeTone: "gold",
      },
    ],
  },
  {
    label: "Case Management",
    items: [
      {
        label: "My Documents",
        path: "/dashboard/documents",
        icon: FileText,
      },
      {
        label: "Applications",
        path: "/dashboard/applications",
        icon: ClipboardCheck,
      },
      {
        label: "Pathway Discovery",
        path: "/dashboard/pathways/discovery",
        icon: Telescope,
      },
      {
        label: "Case Timeline & Signals",
        path: "/dashboard/case-timeline",
        icon: Clock,
      },
      {
        label: "Reports & Analytics",
        path: "/dashboard/reports",
        icon: BarChart3,
      },
    ],
  },
  {
    label: "Account",
    items: [
      {
        label: "Subscription",
        path: "/dashboard/billing/subscription",
        icon: CreditCard,
      },
      {
        label: "Billing History",
        path: "/dashboard/billing/history",
        icon: Receipt,
      },
      {
        label: "Profile Settings",
        path: "/dashboard/profile",
        icon: UserCircle,
      },
    ],
  },
];

const adminNavigationGroups: NavigationGroup[] = [
  {
    label: "Overview",
    items: [
      {
        label: "Admin Control Center",
        path: "/admin",
        icon: LayoutDashboard,
      },
      {
        label: "Analytics",
        path: "/admin/analytics",
        icon: BarChart3,
      },
    ],
  },
  {
    label: "Operations",
    items: [
      {
        label: "User Management",
        path: "/admin/users",
        icon: Users,
      },
      {
        label: "Application Review",
        path: "/admin/applications",
        icon: FileCheck,
      },
    ],
  },
  {
    label: "Intelligence",
    items: [
      {
        label: "AI Model Monitoring",
        path: "/admin/ai",
        icon: BrainCircuit,
      },
      {
        label: "Knowledge Base",
        path: "/admin/knowledge",
        icon: Database,
      },
      {
        label: "Immigration Rules",
        path: "/admin/rules",
        icon: Scale,
      },
    ],
  },
  {
    label: "Governance",
    items: [
      {
        label: "Security Center",
        path: "/admin/security",
        icon: LockKeyhole,
      },
      {
        label: "System Health",
        path: "/admin/system",
        icon: Activity,
      },
      {
        label: "Audit Logs",
        path: "/admin/audit",
        icon: History,
      },
      {
        label: "Global Settings",
        path: "/admin/settings",
        icon: Settings,
      },
    ],
  },
];

/* ============================================================================
   BADGES
============================================================================ */

const badgeStyles: Record<
  NonNullable<NavigationItem["badgeTone"]>,
  string
> = {
  gold: "border border-[#F4B81A]/30 bg-[#FFF7DD] text-[#9A7200]",
  red: "border border-red-200 bg-red-50 text-red-700",
  emerald: "border border-emerald-200 bg-emerald-50 text-emerald-700",
  blue: "border border-blue-200 bg-blue-50 text-blue-700",
};

/* ============================================================================
   COMPONENT
============================================================================ */

export default function Sidebar({
  mobileOpen = false,
  onClose,
}: SidebarProps) {
  const location = useLocation();

  const { user, logout } = useAuth() as {
    user?: {
      name?: string;
      email?: string;
      role?: string;
    };
    logout?: () => void;
  };

  const [collapsed, setCollapsed] = useState(false);
  const [query, setQuery] = useState("");

  /* --------------------------------------------------------------------------
     RESTORE SIDEBAR STATE
  -------------------------------------------------------------------------- */

  useEffect(() => {
    try {
      const stored = window.localStorage.getItem(
        "mgt-sidebar-collapsed",
      );

      if (stored === "true") {
        setCollapsed(true);
      }
    } catch {
      // Ignore storage failures.
    }
  }, []);

  /* --------------------------------------------------------------------------
     COLLAPSE SIDEBAR
  -------------------------------------------------------------------------- */

  const toggleCollapsed = () => {
    setCollapsed((previous) => {
      const next = !previous;

      try {
        window.localStorage.setItem(
          "mgt-sidebar-collapsed",
          String(next),
        );
      } catch {
        // Ignore storage failures.
      }

      return next;
    });
  };

  /* --------------------------------------------------------------------------
     AREA DETECTION
  -------------------------------------------------------------------------- */

  const isAdmin = user?.role === "ADMIN";

  const isAdminArea = location.pathname.startsWith("/admin");

  const isUserArea = location.pathname.startsWith("/dashboard");

  const showSidebar = isAdminArea || isUserArea;

  /* --------------------------------------------------------------------------
     USER INITIALS
  -------------------------------------------------------------------------- */

  const initials = useMemo(() => {
    const name = user?.name?.trim();

    if (!name) {
      return "MG";
    }

    const parts = name
      .split(/\s+/)
      .filter(Boolean);

    return parts
      .map((part) => part.charAt(0))
      .slice(0, 2)
      .join("")
      .toUpperCase();
  }, [user?.name]);

  /* --------------------------------------------------------------------------
     SEARCH
  -------------------------------------------------------------------------- */

  const filterGroups = (
    groups: NavigationGroup[],
  ): NavigationGroup[] => {
    const normalizedQuery = query.trim().toLowerCase();

    if (!normalizedQuery) {
      return groups;
    }

    return groups
      .map((group) => ({
        ...group,
        items: group.items.filter((item) =>
          item.label.toLowerCase().includes(normalizedQuery),
        ),
      }))
      .filter((group) => group.items.length > 0);
  };

  /* --------------------------------------------------------------------------
     HIDE SIDEBAR WHERE NOT REQUIRED
  -------------------------------------------------------------------------- */

  if (!showSidebar) {
    return null;
  }

  /* ==========================================================================
     NAVIGATION GROUP RENDERER
  ========================================================================== */

  const renderGroups = (
    groups: NavigationGroup[],
    admin = false,
  ) => {
    const filteredGroups = filterGroups(groups);

    if (filteredGroups.length === 0) {
      return (
        <div className="px-2 py-10 text-center">
          <div className="mx-auto mb-3 flex h-10 w-10 items-center justify-center rounded-xl bg-slate-100">
            <Search
              size={17}
              className="text-slate-400"
            />
          </div>

          <p className="text-xs font-semibold text-slate-500">
            No navigation results
          </p>

          <p className="mt-1 text-[11px] text-slate-400">
            Try another search term.
          </p>
        </div>
      );
    }

    return (
      <div className="space-y-7">
        {filteredGroups.map((group) => (
          <section key={group.label}>
            {!collapsed && (
              <div className="mb-2.5 flex items-center gap-2 px-2">
                <span className="h-1 w-1 rounded-full bg-slate-300" />

                <span className="text-[10px] font-bold uppercase tracking-[0.16em] text-slate-400">
                  {group.label}
                </span>
              </div>
            )}

            <div className="space-y-1">
              {group.items.map((item) => {
                const Icon = item.icon;

                return (
                  <NavLink
                    key={item.path}
                    to={item.path}
                    onClick={onClose}
                    title={
                      collapsed
                        ? item.label
                        : undefined
                    }
                    end={
                      item.path === "/" ||
                      item.path === "/dashboard" ||
                      item.path === "/admin"
                    }
                    className={({ isActive }) => {
                      const base =
                        "group relative flex min-h-[48px] items-center gap-3 rounded-xl px-3 transition-all duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2";

                      if (collapsed) {
                        return `${base} justify-center`;
                      }

                      if (isActive) {
                        return `${base} ${
                          admin
                            ? "bg-red-50 text-red-700 shadow-sm ring-1 ring-red-100 focus-visible:ring-red-400"
                            : "bg-[#0B1736] text-white shadow-lg shadow-[#0B1736]/10 focus-visible:ring-[#F4B81A]"
                        }`;
                      }

                      return `${base} ${
                        admin
                          ? "text-slate-600 hover:bg-red-50 hover:text-red-700 focus-visible:ring-red-300"
                          : "text-slate-600 hover:bg-slate-100 hover:text-[#0B1736] focus-visible:ring-slate-300"
                      }`;
                    }}
                  >
                    {({ isActive }) => (
                      <>
                        {/* Active indicator */}
                        {isActive && !collapsed && (
                          <span
                            className={`absolute left-0 top-1/2 h-7 w-[3px] -translate-y-1/2 rounded-r-full ${
                              admin
                                ? "bg-red-500"
                                : "bg-[#F4B81A]"
                            }`}
                          />
                        )}

                        {/* Icon */}
                        <span
                          className={`
                            flex h-9 w-9 shrink-0 items-center justify-center rounded-lg
                            transition-all duration-200
                            ${
                              isActive
                                ? admin
                                  ? "bg-red-100 text-red-600"
                                  : "bg-white/10 text-[#F4B81A]"
                                : admin
                                  ? "bg-red-50 text-red-500 group-hover:bg-red-100"
                                  : "bg-slate-100 text-slate-500 group-hover:bg-[#0B1736] group-hover:text-[#F4B81A]"
                            }
                          `}
                        >
                          <Icon size={18} strokeWidth={2} />
                        </span>

                        {/* Label */}
                        {!collapsed && (
                          <span className="min-w-0 flex-1 truncate text-[13px] font-semibold">
                            {item.label}
                          </span>
                        )}

                        {/* Badge */}
                        {!collapsed && item.badge && (
                          <span
                            className={`
                              rounded-full px-2 py-0.5 text-[9px]
                              font-extrabold uppercase tracking-wider
                              ${badgeStyles[item.badgeTone ?? "gold"]}
                            `}
                          >
                            {item.badge}
                          </span>
                        )}
                      </>
                    )}
                  </NavLink>
                );
              })}
            </div>
          </section>
        ))}
      </div>
    );
  };

  /* ==========================================================================
     SIDEBAR
  ========================================================================== */

  return (
    <>
      {/* ----------------------------------------------------------------------
          MOBILE BACKDROP
      ---------------------------------------------------------------------- */}

      <div
        onClick={onClose}
        aria-hidden="true"
        className={`
          fixed inset-0 z-40 bg-[#071330]/50
          backdrop-blur-sm transition-all duration-300
          lg:hidden
          ${
            mobileOpen
              ? "visible opacity-100"
              : "invisible opacity-0"
          }
        `}
      />

      {/* ----------------------------------------------------------------------
          SIDEBAR
      ---------------------------------------------------------------------- */}

      <aside
        aria-label="Primary navigation"
        className={`
          fixed left-0 top-20 z-50 flex
          h-[calc(100vh-5rem)] flex-col
          border-r border-slate-200/80
          bg-white
          shadow-[8px_0_30px_rgba(15,23,42,0.05)]
          transition-all duration-300
          ${
            collapsed
              ? "w-[88px]"
              : "w-[290px]"
          }
          ${
            mobileOpen
              ? "translate-x-0"
              : "-translate-x-full"
          }
          lg:translate-x-0
        `}
      >
        {/* ====================================================================
            BRAND HEADER
        ==================================================================== */}

        <div className="shrink-0 border-b border-slate-200/80 bg-white px-4 py-4">
          <div
            className={`
              flex items-center
              ${
                collapsed
                  ? "justify-center"
                  : "justify-between"
              }
            `}
          >
            <div
              className={`
                flex min-w-0 items-center
                ${
                  collapsed
                    ? "justify-center"
                    : "gap-3"
                }
              `}
            >
              {/* Brand mark */}
              <div
                className="
                  relative flex h-11 w-11 shrink-0
                  items-center justify-center
                  overflow-hidden rounded-xl
                  bg-[#0B1736]
                  shadow-lg shadow-[#0B1736]/20
                "
              >
                <div className="absolute inset-0 bg-gradient-to-br from-[#183B6B] to-[#0B1736]" />

                <div className="absolute right-0 top-0 h-5 w-5 rounded-full bg-[#F4B81A]/30 blur-md" />

                <ScanSearch
                  size={22}
                  strokeWidth={2.2}
                  className="relative z-10 text-[#F4B81A]"
                />
              </div>

              {/* Brand text */}
              {!collapsed && (
                <div className="min-w-0">
                  <div className="flex items-center">
                    <h2 className="truncate text-[15px] font-extrabold tracking-tight text-[#0B1736]">
                      MukondoGTech
                    </h2>

                    <span className="ml-1 text-[15px] font-extrabold text-[#F4B81A]">
                      AI
                    </span>
                  </div>

                  <p className="mt-0.5 truncate text-[10px] font-semibold uppercase tracking-[0.12em] text-slate-400">
                    Immigration Intelligence
                  </p>
                </div>
              )}
            </div>

            {/* Mobile close */}
            <button
              type="button"
              onClick={onClose}
              aria-label="Close navigation"
              className="
                rounded-lg p-2 text-slate-400
                transition hover:bg-slate-100
                hover:text-slate-700
                lg:hidden
              "
            >
              <X size={20} />
            </button>
          </div>

          {/* Collapse */}
          <button
            type="button"
            onClick={toggleCollapsed}
            aria-label={
              collapsed
                ? "Expand sidebar"
                : "Collapse sidebar"
            }
            className={`
              mt-4 hidden h-9 w-full
              items-center justify-center gap-2
              rounded-lg border border-slate-200
              bg-slate-50 text-[11px]
              font-bold text-slate-500
              transition-all duration-200
              hover:border-[#F4B81A]/40
              hover:bg-[#FFF9E8]
              hover:text-[#0B1736]
              lg:flex
              ${
                collapsed
                  ? "border-transparent bg-transparent"
                  : ""
              }
            `}
          >
            {collapsed ? (
              <ChevronsRight size={16} />
            ) : (
              <>
                <ChevronsLeft size={16} />
                <span>Collapse menu</span>
              </>
            )}
          </button>
        </div>

        {/* ====================================================================
            SEARCH
        ==================================================================== */}

        {!collapsed && (
          <div className="shrink-0 border-b border-slate-100 px-4 py-3">
            <div className="relative">
              <Search
                size={15}
                className="
                  pointer-events-none absolute
                  left-3.5 top-1/2
                  -translate-y-1/2
                  text-slate-400
                "
              />

              <input
                type="search"
                value={query}
                onChange={(event) =>
                  setQuery(event.target.value)
                }
                placeholder="Search navigation..."
                aria-label="Search navigation"
                className="
                  h-10 w-full rounded-lg
                  border border-slate-200
                  bg-slate-50
                  pl-10 pr-3
                  text-xs font-medium
                  text-slate-700
                  placeholder:text-slate-400
                  outline-none
                  transition-all
                  focus:border-[#F4B81A]
                  focus:bg-white
                  focus:ring-4
                  focus:ring-[#F4B81A]/10
                "
              />
            </div>
          </div>
        )}

        {/* ====================================================================
            NAVIGATION
        ==================================================================== */}

        <nav
          aria-label="Application navigation"
          className="min-h-0 flex-1 overflow-y-auto px-3 py-5 scrollbar-thin scrollbar-track-transparent scrollbar-thumb-slate-200"
        >
          {/* USER WORKSPACE */}

          {isUserArea && (
            <>
              {!collapsed && (
                <div className="mb-4 flex items-center gap-2 px-2">
                  <span className="flex h-5 w-5 items-center justify-center rounded-md bg-[#FFF7DD] text-[#B17E00]">
                    <Sparkles size={12} />
                  </span>

                  <span className="text-[10px] font-extrabold uppercase tracking-[0.16em] text-slate-500">
                    Workspace
                  </span>
                </div>
              )}

              {renderGroups(userNavigationGroups)}
            </>
          )}

          {/* ADMIN WORKSPACE */}

          {isAdminArea && isAdmin && (
            <>
              {!collapsed && (
                <div className="mb-4 flex items-center gap-2 px-2">
                  <span className="flex h-5 w-5 items-center justify-center rounded-md bg-red-50 text-red-600">
                    <ShieldCheck size={12} />
                  </span>

                  <span className="text-[10px] font-extrabold uppercase tracking-[0.16em] text-red-600">
                    Administrator
                  </span>
                </div>
              )}

              {renderGroups(
                adminNavigationGroups,
                true,
              )}

              {/* Return to user workspace */}
              <div className="my-5 border-t border-slate-200/80" />

              <NavLink
                to="/dashboard"
                onClick={onClose}
                title={
                  collapsed
                    ? "User Workspace"
                    : undefined
                }
                className={`
                  group flex min-h-[46px]
                  items-center gap-3 rounded-xl
                  border border-slate-200
                  bg-slate-50
                  px-3
                  text-slate-600
                  transition-all duration-200
                  hover:border-slate-300
                  hover:bg-white
                  hover:text-[#0B1736]
                  hover:shadow-sm
                  ${
                    collapsed
                      ? "justify-center"
                      : ""
                  }
                `}
              >
                <span
                  className="
                    flex h-9 w-9 shrink-0
                    items-center justify-center
                    rounded-lg bg-white
                    text-slate-500
                    shadow-sm
                    transition-colors
                    group-hover:text-[#0B1736]
                  "
                >
                  <UserCircle size={18} />
                </span>

                {!collapsed && (
                  <span className="text-xs font-bold">
                    User Workspace
                  </span>
                )}
              </NavLink>
            </>
          )}
        </nav>

        {/* ====================================================================
            USER PROFILE
        ==================================================================== */}

        <div className="shrink-0 border-t border-slate-200/80 bg-white p-3">
          <div
            className={`
              rounded-xl
              border border-slate-200
              bg-slate-50
              p-2.5
              ${
                collapsed
                  ? "flex justify-center"
                  : ""
              }
            `}
          >
            <div
              className={`
                flex items-center gap-2.5
                ${
                  collapsed
                    ? "justify-center"
                    : ""
                }
              `}
            >
              {/* Avatar */}
              <div
                className="
                  flex h-9 w-9 shrink-0
                  items-center justify-center
                  rounded-lg
                  bg-[#0B1736]
                  text-xs font-extrabold
                  text-[#F4B81A]
                  shadow-sm
                "
              >
                {initials}
              </div>

              {/* User information */}
              {!collapsed && (
                <div className="min-w-0 flex-1">
                  <p className="truncate text-xs font-bold text-[#0B1736]">
                    {user?.name ?? "Guest User"}
                  </p>

                  <p className="mt-0.5 truncate text-[10px] text-slate-400">
                    {user?.email ?? "Not signed in"}
                  </p>
                </div>
              )}

              {/* Logout */}
              {!collapsed && logout && (
                <button
                  type="button"
                  onClick={logout}
                  aria-label="Log out"
                  title="Log out"
                  className="
                    flex h-8 w-8 shrink-0
                    items-center justify-center
                    rounded-lg
                    text-slate-400
                    transition-all
                    hover:bg-red-50
                    hover:text-red-600
                    focus-visible:outline-none
                    focus-visible:ring-2
                    focus-visible:ring-red-300
                  "
                >
                  <LogOut size={15} />
                </button>
              )}
            </div>
          </div>
        </div>

        {/* ====================================================================
            HELP & SUPPORT
        ==================================================================== */}

        <footer className="shrink-0 border-t border-slate-200/80 bg-white p-3">
          <a
            href="/help"
            title={collapsed ? "Help & Support" : undefined}
            className={`
              group flex min-h-[46px]
              items-center gap-3 rounded-xl
              border border-slate-200
              bg-slate-50
              px-3
              text-slate-600
              transition-all duration-200
              hover:border-[#F4B81A]/40
              hover:bg-[#FFF9E8]
              hover:text-[#0B1736]
              ${collapsed ? "justify-center" : ""}
            `}
          >
            <span
              className="
                flex h-9 w-9 shrink-0
                items-center justify-center
                rounded-lg bg-white
                text-slate-500
                shadow-sm
                transition-colors
                group-hover:text-[#0B1736]
              "
            >
              <LifeBuoy size={18} />
            </span>

            {!collapsed && (
              <span className="text-xs font-bold">
                Help &amp; Support
              </span>
            )}
          </a>
        </footer>
      </aside>
    </>
  );
}