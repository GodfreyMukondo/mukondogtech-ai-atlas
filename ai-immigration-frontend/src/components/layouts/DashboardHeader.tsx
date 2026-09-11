import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import {
  AlertTriangle,
  Bell,
  BellOff,
  CheckCircle2,
  ChevronDown,
  ChevronRight,
  FileText,
  LogOut,
  Menu,
  ScanSearch,
  ShieldCheck,
  Sparkles,
  UploadCloud,
  UserCircle,
} from "lucide-react";

import { useAuth } from "../../features/auth/hooks/useAuth";
import notificationApi from "../../api/notificationApi";
import type { Notification } from "../../types/notification";

interface DashboardHeaderProps {
  onMenuClick: () => void;
}

interface PageMeta {
  title: string;
  subtitle: string;
}

const PAGE_META: Record<string, PageMeta> = {
  "/dashboard": {
    title: "Dashboard",
    subtitle: "Your document activity and analysis at a glance.",
  },
  "/dashboard/chat": {
    title: "AI Immigration Assistant",
    subtitle: "Ask questions about your documents and process.",
  },
  "/dashboard/documents": {
    title: "My Documents",
    subtitle: "Upload, review and manage your immigration documents.",
  },
  "/dashboard/applications": {
    title: "Applications",
    subtitle: "Track the status of your immigration applications.",
  },
  "/dashboard/reports": {
    title: "Reports & Analytics",
    subtitle: "Review analysis history and document insights.",
  },
  "/dashboard/billing/subscription": {
    title: "Subscription",
    subtitle: "Manage your plan and billing details.",
  },
  "/dashboard/billing/history": {
    title: "Billing History",
    subtitle: "View previous invoices and payments.",
  },
  "/dashboard/profile": {
    title: "Profile Settings",
    subtitle: "Manage your personal information.",
  },
  "/dashboard/account": {
    title: "Account",
    subtitle: "Manage your account preferences.",
  },
  "/dashboard/settings/profile": {
    title: "Settings",
    subtitle: "Manage your application settings.",
  },
  "/dashboard/settings/security": {
    title: "Security Settings",
    subtitle: "Manage password and account security.",
  },
  "/dashboard/settings/notifications": {
    title: "Notification Settings",
    subtitle: "Choose how we keep you updated.",
  },
};

const DEFAULT_PAGE_META: PageMeta = {
  title: "Workspace",
  subtitle: "Your immigration document workspace.",
};

function resolvePageMeta(pathname: string): PageMeta {
  return PAGE_META[pathname] ?? DEFAULT_PAGE_META;
}

/**
 * Derive up to two initials from a display name, falling back to a neutral
 * mark when no name is available yet (e.g. auth state still hydrating).
 */
function resolveInitials(name?: string | null): string {
  const trimmed = name?.trim();

  if (!trimmed) {
    return "MG";
  }

  const parts = trimmed.split(/\s+/).filter(Boolean);

  return parts
    .map((part) => part.charAt(0))
    .slice(0, 2)
    .join("")
    .toUpperCase();
}

function resolveNotificationIcon(type: string) {
  const normalized = type.trim().toUpperCase();

  if (normalized === "DOCUMENT_FLAGGED") {
    return AlertTriangle;
  }

  if (normalized === "DOCUMENT_PROCESSED") {
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

function resolveDisplayRole(role?: string | null): string {
  if (!role) {
    return "Member";
  }

  const normalized = role.trim().toUpperCase();

  return normalized === "ADMIN" ? "Administrator" : "Member";
}

export default function DashboardHeader({ onMenuClick }: DashboardHeaderProps) {
  const location = useLocation();
  const pageMeta = resolvePageMeta(location.pathname);

  const { user, logout } = useAuth();

  const isAdmin = user?.role?.trim().toUpperCase() === "ADMIN";

  const initials = useMemo(
    () => resolveInitials(user?.fullName),
    [user?.fullName],
  );

  const displayRole = useMemo(
    () => resolveDisplayRole(user?.role),
    [user?.role],
  );

  /* ==========================================================================
     PROFILE MENU / NOTIFICATIONS PANEL
     ========================================================================== */

  const [profileOpen, setProfileOpen] = useState(false);
  const [notificationsOpen, setNotificationsOpen] = useState(false);

  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [notificationsLoading, setNotificationsLoading] = useState(false);

  const profileMenuRef = useRef<HTMLDivElement | null>(null);
  const notificationsRef = useRef<HTMLDivElement | null>(null);

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

    const intervalId = window.setInterval(refreshUnreadCount, 60000);

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

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      const target = event.target as Node;

      if (
        profileMenuRef.current &&
        !profileMenuRef.current.contains(target)
      ) {
        setProfileOpen(false);
      }

      if (
        notificationsRef.current &&
        !notificationsRef.current.contains(target)
      ) {
        setNotificationsOpen(false);
      }
    }

    function handleEscape(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setProfileOpen(false);
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

  // Closing one panel when the other opens keeps the header uncluttered.
  const toggleProfile = () => {
    setNotificationsOpen(false);
    setProfileOpen((previous) => !previous);
  };

  const toggleNotifications = () => {
    setProfileOpen(false);

    setNotificationsOpen((previous) => {
      const next = !previous;

      if (next) {
        loadNotifications();
      }

      return next;
    });
  };

  return (
    <header
      className="
        fixed inset-x-0 top-0 z-40
        border-b border-white/10
        bg-[#0B1F3A]/95 backdrop-blur-xl
        shadow-[0_4px_20px_rgba(7, 20, 38,0.25)]
      "
    >
      <div
        className="
          flex min-h-20 items-center justify-between gap-4
          px-4 py-3 sm:px-6 lg:px-8
        "
      >
        <div className="flex min-w-0 flex-1 items-center gap-3">
          {/* Brand mark - the sidebar no longer duplicates this in its
              own top row, so this header is now the single source of
              app identity for the dashboard shell. */}
          <Link
            to="/dashboard"
            aria-label="MukondoGTech AI home"
            className="
              flex shrink-0 items-center gap-2.5
              rounded-2xl
              transition-transform duration-200
              hover:scale-105
            "
          >
            <div
              className="
                relative flex h-11 w-11 shrink-0 items-center justify-center
                overflow-hidden rounded-2xl
                bg-white/10 backdrop-blur-sm
                ring-1 ring-white/10
              "
            >
              <div className="absolute right-0 top-0 h-4 w-4 animate-pulse rounded-full bg-[#C6A15B]/40 blur-md" />

              <ScanSearch
                size={20}
                strokeWidth={2.2}
                className="relative z-10 text-[#C6A15B]"
              />
            </div>

            <div className="hidden leading-tight lg:block">
              <div className="flex items-center">
                <span className="text-[15px] font-extrabold tracking-tight text-white">
                  MukondoGTech
                </span>

                <span className="ml-1 text-[15px] font-extrabold text-[#C6A15B]">
                  AI
                </span>
              </div>

              <p className="mt-0.5 truncate text-[10px] font-semibold uppercase tracking-[0.12em] text-slate-400">
                Immigration Intelligence
              </p>
            </div>
          </Link>

          <span className="hidden h-8 w-px shrink-0 bg-white/10 lg:block" />

          <button
            type="button"
            onClick={onMenuClick}
            aria-label="Open navigation"
            className="
              flex h-11 w-11 shrink-0 items-center justify-center
              rounded-2xl border border-white/10 bg-white/5 backdrop-blur-sm
              text-white shadow-sm transition-all duration-200
              hover:-translate-y-0.5 hover:border-white/20 hover:bg-white/10
              focus:outline-none focus:ring-2 focus:ring-[#C6A15B] focus:ring-offset-2 focus:ring-offset-[#0B1F3A]
              lg:hidden
            "
          >
            <Menu size={21} />
          </button>

          <div className="min-w-0 flex-1">
            <div className="hidden items-center gap-1.5 text-xs font-semibold text-slate-400 sm:flex">
              <span>Workspace</span>
              <ChevronRight size={13} />
              <span className="truncate text-slate-300">{pageMeta.title}</span>

              {isAdmin && (
                <span
                  className="
                    ml-1 inline-flex items-center gap-1 rounded-full
                    border border-red-500/30 bg-red-500/15 px-2 py-0.5
                    text-[10px] font-extrabold uppercase tracking-wide text-red-300
                  "
                >
                  <ShieldCheck size={11} />
                  Admin
                </span>
              )}
            </div>

            <h1 className="truncate text-lg font-black tracking-tight text-white sm:text-xl">
              {pageMeta.title}
            </h1>

            <p className="hidden max-w-2xl truncate text-sm text-slate-300 md:block">
              {pageMeta.subtitle}
            </p>
          </div>
        </div>

        <div className="flex shrink-0 items-center gap-2 sm:gap-3">
          <Link
            to="/dashboard/documents"
            className="
              hidden h-11 shrink-0 items-center justify-center gap-2
              rounded-2xl bg-gradient-to-r from-[#C6A15B] to-[#A8894D] px-4 text-sm font-bold text-[#071426]
              shadow-sm transition-all duration-200
              hover:-translate-y-0.5 hover:shadow-[0_10px_24px_-6px_rgba(198, 161, 91,0.5)]
              sm:inline-flex
            "
          >
            <UploadCloud size={18} />
            <span>Upload document</span>
          </Link>

          <Link
            to="/dashboard/documents"
            aria-label="Upload document"
            className="
              flex h-11 w-11 shrink-0 items-center justify-center
              rounded-2xl bg-gradient-to-br from-[#C6A15B] to-[#A8894D] text-[#071426]
              shadow-sm transition-all duration-200
              hover:-translate-y-0.5 hover:shadow-[0_10px_24px_-6px_rgba(198, 161, 91,0.5)]
              sm:hidden
            "
          >
            <UploadCloud size={18} />
          </Link>

          {/* ==================================================================
              NOTIFICATIONS
              ================================================================== */}

          <div className="relative" ref={notificationsRef}>
            <button
              type="button"
              onClick={toggleNotifications}
              aria-label="View notifications"
              aria-expanded={notificationsOpen}
              className="
                flex h-11 w-11 shrink-0 items-center justify-center
                rounded-2xl border border-white/10 bg-white/5 backdrop-blur-sm
                text-slate-300 shadow-sm transition-all duration-200
                hover:-translate-y-0.5 hover:border-white/20 hover:bg-white/10 hover:text-white
                focus:outline-none focus:ring-2 focus:ring-[#C6A15B] focus:ring-offset-2 focus:ring-offset-[#0B1F3A]
              "
            >
              <Bell size={18} />

              {unreadCount > 0 && (
                <span
                  aria-hidden="true"
                  className="
                    absolute right-1.5 top-1.5 flex h-4 min-w-4 items-center justify-center
                    rounded-full bg-red-500 px-1 text-[9px] font-extrabold text-white
                    ring-2 ring-[#0B1F3A]
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
                  absolute right-0 top-[calc(100%+10px)] z-40
                  w-80 max-w-[calc(100vw-2rem)]
                  overflow-hidden rounded-3xl border border-white/10
                  bg-[#1F314A] shadow-2xl shadow-black/40 backdrop-blur-xl
                "
              >
                <div className="flex items-center justify-between border-b border-white/10 px-4 py-3">
                  <p className="text-sm font-bold text-white">
                    Notifications
                  </p>

                  <div className="flex items-center gap-3">
                    {notifications.some((item) => !item.read) && (
                      <button
                        type="button"
                        onClick={handleMarkAllAsRead}
                        className="text-xs font-semibold text-slate-400 transition hover:text-white"
                      >
                        Mark all read
                      </button>
                    )}

                    <Link
                      to="/dashboard/settings/notifications"
                      onClick={() => setNotificationsOpen(false)}
                      className="text-xs font-semibold text-slate-400 transition hover:text-white"
                    >
                      Settings
                    </Link>
                  </div>
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
                        const Icon = resolveNotificationIcon(notification.type);

                        return (
                          <li key={notification.id}>
                            <Link
                              to={notification.link ?? "/dashboard"}
                              onClick={() => {
                                setNotificationsOpen(false);

                                if (!notification.read) {
                                  handleMarkAsRead(notification.id);
                                }
                              }}
                              className={`
                                flex items-start gap-3 px-4 py-3.5 transition hover:bg-white/5
                                ${notification.read ? "" : "bg-[#C6A15B]/[0.06]"}
                              `}
                            >
                              <span
                                className={`
                                  mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-full
                                  ${
                                    notification.type === "DOCUMENT_FLAGGED"
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
                            </Link>
                          </li>
                        );
                      })}
                    </ul>
                  )}
                </div>
              </div>
            )}
          </div>

          {/* ==================================================================
              PROFILE MENU
              ================================================================== */}

          <div className="relative" ref={profileMenuRef}>
            <button
              type="button"
              onClick={toggleProfile}
              aria-label="Open account menu"
              aria-expanded={profileOpen}
              className="
                flex h-11 items-center gap-2 rounded-2xl
                border border-white/10 bg-white/5 backdrop-blur-sm pl-1.5 pr-2.5
                shadow-sm transition-all duration-200
                hover:-translate-y-0.5 hover:border-white/20 hover:bg-white/10
                focus:outline-none focus:ring-2 focus:ring-[#C6A15B] focus:ring-offset-2 focus:ring-offset-[#0B1F3A]
              "
            >
              <span
                className="
                  flex h-8 w-8 shrink-0 items-center justify-center
                  rounded-xl bg-gradient-to-br from-[#C6A15B] to-[#A8894D] text-xs font-extrabold text-[#071426]
                "
              >
                {initials}
              </span>

              <span className="hidden text-left leading-tight md:block">
                <span className="block max-w-[9rem] truncate text-xs font-bold text-white">
                  {user?.fullName ?? "Guest User"}
                </span>

                <span className="block text-[10px] font-semibold text-slate-300">
                  {displayRole}
                </span>
              </span>

              <ChevronDown
                size={15}
                className={`hidden shrink-0 text-slate-400 transition-transform duration-200 md:block ${
                  profileOpen ? "rotate-180" : ""
                }`}
              />
            </button>

            {profileOpen && (
              <div
                role="menu"
                aria-label="Account menu"
                className="
                  absolute right-0 top-[calc(100%+10px)] z-40
                  w-64 max-w-[calc(100vw-2rem)]
                  overflow-hidden rounded-3xl border border-white/10
                  bg-[#1F314A] shadow-2xl shadow-black/40 backdrop-blur-xl
                "
              >
                <div className="flex items-center gap-3 border-b border-white/10 px-4 py-3.5">
                  <span
                    className="
                      flex h-10 w-10 shrink-0 items-center justify-center
                      rounded-xl bg-gradient-to-br from-[#C6A15B] to-[#A8894D] text-sm font-extrabold text-[#071426]
                    "
                  >
                    {initials}
                  </span>

                  <div className="min-w-0">
                    <p className="truncate text-sm font-bold text-white">
                      {user?.fullName ?? "Guest User"}
                    </p>

                    <p className="truncate text-xs text-slate-400">
                      {user?.email ?? "Not signed in"}
                    </p>
                  </div>
                </div>

                <div className="px-2 py-2">
                  {isAdmin && (
                    <Link
                      to="/admin"
                      onClick={() => setProfileOpen(false)}
                      role="menuitem"
                      className="
                        flex items-center gap-2.5 rounded-xl px-2.5 py-2.5
                        text-sm font-semibold text-slate-300 transition
                        hover:bg-red-500/10 hover:text-red-300
                      "
                    >
                      <ShieldCheck size={16} className="text-slate-400" />
                      Admin control center
                    </Link>
                  )}

                  <Link
                    to="/dashboard/profile"
                    onClick={() => setProfileOpen(false)}
                    role="menuitem"
                    className="
                      flex items-center gap-2.5 rounded-xl px-2.5 py-2.5
                      text-sm font-semibold text-slate-300 transition
                      hover:bg-white/10 hover:text-white
                    "
                  >
                    <UserCircle size={16} className="text-slate-400" />
                    Profile settings
                  </Link>

                  <Link
                    to="/dashboard/billing/subscription"
                    onClick={() => setProfileOpen(false)}
                    role="menuitem"
                    className="
                      flex items-center gap-2.5 rounded-xl px-2.5 py-2.5
                      text-sm font-semibold text-slate-300 transition
                      hover:bg-white/10 hover:text-white
                    "
                  >
                    <Sparkles size={16} className="text-slate-400" />
                    Subscription
                  </Link>
                </div>

                <div className="border-t border-white/10 px-2 py-2">
                  <button
                    type="button"
                    role="menuitem"
                    onClick={() => {
                      setProfileOpen(false);
                      logout();
                    }}
                    className="
                      flex w-full items-center gap-2.5 rounded-xl px-2.5 py-2.5
                      text-sm font-semibold text-red-400 transition
                      hover:bg-red-500/15
                    "
                  >
                    <LogOut size={16} />
                    Log out
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  );
}
