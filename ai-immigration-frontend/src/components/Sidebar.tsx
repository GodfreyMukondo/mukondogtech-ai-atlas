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
  Compass,
  CreditCard,
  Database,
  FileCheck,
  FileText,
  History,
  IdCard,
  LayoutDashboard,
  LifeBuoy,
  LockKeyhole,
  LogOut,
  MessageCircle,
  Receipt,
  Scale,
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

  /**
   * Controlled collapse state.
   *
   * When provided (together with onCollapsedChange), the parent layout
   * owns the collapsed/expanded state so it can size its own content
   * offset in sync with the sidebar's actual width instead of guessing
   * with a fixed margin.
   *
   * When omitted, the sidebar manages its own collapse state internally,
   * as before.
   */
  collapsed?: boolean;
  onCollapsedChange?: (collapsed: boolean) => void;
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
        label: "Immigration Profile",
        path: "/dashboard/immigration-profile",
        icon: IdCard,
      },
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
        label: "Pathway Assessment",
        path: "/dashboard/pathways/assessments/new",
        icon: Compass,
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
      {
        label: "Pathways",
        path: "/admin/pathways",
        icon: Compass,
      },
      {
        label: "Requirements",
        path: "/admin/requirements",
        icon: FileCheck,
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
  gold: "border border-[#C6A15B]/30 bg-[#C6A15B]/15 text-[#C6A15B]",
  red: "border border-red-500/30 bg-red-500/15 text-red-300",
  emerald: "border border-emerald-500/30 bg-emerald-500/15 text-emerald-300",
  blue: "border border-blue-500/30 bg-blue-500/15 text-blue-300",
};

/* ============================================================================
   COMPONENT
============================================================================ */

export default function Sidebar({
  mobileOpen = false,
  onClose,
  collapsed: controlledCollapsed,
  onCollapsedChange,
}: SidebarProps) {
  const location = useLocation();

  const { user, logout } = useAuth();

  const [internalCollapsed, setInternalCollapsed] = useState(false);
  const [query, setQuery] = useState("");

  const isControlled =
    controlledCollapsed !== undefined;

  const collapsed = isControlled
    ? controlledCollapsed
    : internalCollapsed;

  /* --------------------------------------------------------------------------
     RESTORE SIDEBAR STATE

     Only needed in uncontrolled mode - a controlling parent is
     responsible for restoring its own initial state.
  -------------------------------------------------------------------------- */

  useEffect(() => {
    if (isControlled) {
      return;
    }

    try {
      const stored = window.localStorage.getItem(
        "mgt-sidebar-collapsed",
      );

      if (stored === "true") {
        setInternalCollapsed(true);
      }
    } catch {
      // Ignore storage failures.
    }
  }, [isControlled]);

  /* --------------------------------------------------------------------------
     COLLAPSE SIDEBAR
  -------------------------------------------------------------------------- */

  const toggleCollapsed = () => {
    const next = !collapsed;

    try {
      window.localStorage.setItem(
        "mgt-sidebar-collapsed",
        String(next),
      );
    } catch {
      // Ignore storage failures.
    }

    if (isControlled) {
      onCollapsedChange?.(next);
    } else {
      setInternalCollapsed(next);
    }
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
    const name = user?.fullName?.trim();

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
  }, [user?.fullName]);

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
          <div className="mx-auto mb-3 flex h-10 w-10 items-center justify-center rounded-xl bg-white/5">
            <Search
              size={17}
              className="text-slate-400"
            />
          </div>

          <p className="text-xs font-semibold text-slate-300">
            No navigation results
          </p>

          <p className="mt-1 text-[11px] text-slate-500">
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
                <span className="h-3 w-1 rounded-full bg-gradient-to-b from-[#C6A15B] to-[#C6A15B]/20" />

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
                        "group relative flex min-h-[48px] items-center gap-3 rounded-2xl px-3 transition-all duration-200 ease-out focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-offset-[#0B1F3A]";

                      const layout = collapsed
                        ? "justify-center"
                        : "";

                      const state = isActive
                        ? admin
                          ? "bg-red-500/15 text-red-300 shadow-sm ring-1 ring-red-500/30 focus-visible:ring-red-400"
                          : "bg-gradient-to-br from-[#C6A15B] to-[#A8894D] text-[#071426] shadow-[0_8px_22px_-4px_rgba(198, 161, 91,0.55)] focus-visible:ring-[#C6A15B]"
                        : admin
                          ? "text-slate-300 hover:translate-x-0.5 hover:bg-red-500/10 hover:text-red-300 focus-visible:ring-red-400/40"
                          : "text-slate-300 hover:translate-x-0.5 hover:bg-white/10 hover:text-white focus-visible:ring-white/30";

                      return `${base} ${layout} ${state}`;
                    }}
                  >
                    {({ isActive }) => (
                      <>
                        {/* Active indicator */}
                        {isActive && !collapsed && (
                          <span
                            className={`absolute left-1.5 top-1/2 h-5 w-[3px] -translate-y-1/2 rounded-full ${
                              admin
                                ? "bg-red-400"
                                : "bg-[#071426]/70"
                            }`}
                          />
                        )}

                        {/* Icon */}
                        <span
                          className={`
                            flex h-9 w-9 shrink-0 items-center justify-center rounded-xl
                            transition-all duration-200 ease-out
                            ${
                              isActive
                                ? admin
                                  ? "bg-red-500/20 text-red-200"
                                  : "bg-[#071426]/15 text-[#071426]"
                                : admin
                                  ? "bg-red-500/10 text-red-300 group-hover:bg-red-500/20"
                                  : "bg-white/10 text-slate-300 backdrop-blur-sm group-hover:scale-105 group-hover:bg-[#C6A15B] group-hover:text-[#071426]"
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
          fixed inset-0 z-40 bg-[#071426]/50
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
          fixed left-0 top-20 z-40 flex
          h-[calc(100vh-5rem)] flex-col
          border-r border-white/10
          bg-gradient-to-b from-[#0B1F3A] to-[#071426]
          shadow-[8px_0_30px_rgba(7, 20, 38,0.35)]
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
            MOBILE CLOSE BAR

            The app's brand identity now lives once, in the fixed header
            above (DashboardHeader/AdminLayout), which the sidebar sits
            below rather than beside - so this row only needs to carry
            the mobile drawer's close affordance. It's replaced entirely
            by the collapse toggle below on desktop.
        ==================================================================== */}

        <div
          className="
            flex h-14 shrink-0 items-center justify-end
            border-b border-white/10 px-4
            lg:hidden
          "
        >
          <button
            type="button"
            onClick={onClose}
            aria-label="Close navigation"
            className="
              shrink-0 rounded-lg p-2 text-slate-400
              transition hover:bg-white/10
              hover:text-white
            "
          >
            <X size={20} />
          </button>
        </div>

        {/* ====================================================================
            COLLAPSE TOGGLE
        ==================================================================== */}

        <div
          className="
            hidden shrink-0
            border-b border-white/10
            px-4 pb-3 pt-5
            lg:block
          "
        >
          <button
            type="button"
            onClick={toggleCollapsed}
            aria-label={
              collapsed
                ? "Expand sidebar"
                : "Collapse sidebar"
            }
            className={`
              flex h-9 w-full
              items-center justify-center gap-2
              rounded-xl border border-white/10
              bg-white/5 backdrop-blur-sm text-[11px]
              font-bold text-slate-300
              transition-all duration-200
              hover:border-[#C6A15B]/40
              hover:bg-[#C6A15B]/10
              hover:text-[#C6A15B]
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
          <div className="shrink-0 border-b border-white/10 px-4 py-3">
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
                  h-10 w-full rounded-xl
                  border border-white/10
                  bg-white/5 backdrop-blur-sm
                  pl-10 pr-3
                  text-xs font-medium
                  text-white
                  placeholder:text-slate-400
                  outline-none
                  transition-all duration-200
                  focus:border-[#C6A15B]/60
                  focus:bg-white/10
                  focus:ring-4
                  focus:ring-[#C6A15B]/15
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
          className="min-h-0 flex-1 overflow-y-auto px-3 py-5 scrollbar-thin scrollbar-track-transparent scrollbar-thumb-slate-600"
        >
          {/* USER WORKSPACE */}

          {isUserArea && (
            <>
              {!collapsed && (
                <div className="mb-4 flex items-center gap-2 px-2">
                  <span className="flex h-5 w-5 items-center justify-center rounded-lg bg-[#C6A15B]/15 text-[#C6A15B]">
                    <Sparkles size={12} />
                  </span>

                  <span className="text-[10px] font-extrabold uppercase tracking-[0.16em] text-slate-400">
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
                  <span className="flex h-5 w-5 items-center justify-center rounded-lg bg-red-500/15 text-red-300">
                    <ShieldCheck size={12} />
                  </span>

                  <span className="text-[10px] font-extrabold uppercase tracking-[0.16em] text-red-400">
                    Administrator
                  </span>
                </div>
              )}

              {renderGroups(
                adminNavigationGroups,
                true,
              )}

              {/* Return to user workspace */}
              <div className="my-5 border-t border-white/10" />

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
                  items-center gap-3 rounded-2xl
                  border border-white/10
                  bg-white/5 backdrop-blur-sm
                  px-3
                  text-slate-300
                  transition-all duration-200
                  hover:translate-x-0.5
                  hover:border-white/20
                  hover:bg-white/10
                  hover:text-white
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
                    rounded-xl bg-white/10
                    text-slate-300
                    transition-colors
                    group-hover:text-white
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

        <div className="shrink-0 border-t border-white/10 p-3">
          <div
            className={`
              rounded-2xl
              border border-white/10
              bg-white/5 backdrop-blur-sm
              p-2.5
              transition-colors duration-200
              hover:bg-white/10
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
                  rounded-xl
                  bg-gradient-to-br from-[#C6A15B] to-[#A8894D]
                  text-xs font-extrabold
                  text-[#071426]
                  shadow-sm
                "
              >
                {initials}
              </div>

              {/* User information */}
              {!collapsed && (
                <div className="min-w-0 flex-1">
                  <p className="truncate text-xs font-bold text-white">
                    {user?.fullName ?? "Guest User"}
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
                    hover:bg-red-500/15
                    hover:text-red-300
                    focus-visible:outline-none
                    focus-visible:ring-2
                    focus-visible:ring-red-400/40
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

        <footer className="shrink-0 border-t border-white/10 p-3">
          <a
            href="/help"
            title={collapsed ? "Help & Support" : undefined}
            className={`
              group flex min-h-[46px]
              items-center gap-3 rounded-2xl
              border border-white/10
              bg-white/5 backdrop-blur-sm
              px-3
              text-slate-300
              transition-all duration-200
              hover:translate-x-0.5
              hover:border-[#C6A15B]/40
              hover:bg-[#C6A15B]/10
              hover:text-[#C6A15B]
              ${collapsed ? "justify-center" : ""}
            `}
          >
            <span
              className="
                flex h-9 w-9 shrink-0
                items-center justify-center
                rounded-xl bg-white/10
                text-slate-300
                transition-colors
                group-hover:text-[#C6A15B]
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