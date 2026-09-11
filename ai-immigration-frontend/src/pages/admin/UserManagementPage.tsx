import React, { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import axios from "axios";
import {
  Search,
  Users,
  UserPlus,
  Shield,
  UserCheck,
  Mail,
  Phone,
  Globe,
  MoreVertical,
  Filter,
  Download,
  RefreshCw,
  Lock,
  Unlock,
  Eye,
  Edit,
  Trash2,
  Crown,
  CheckCircle2,
  AlertTriangle,
  Clock3,
  Activity,
  ChevronLeft,
  ChevronRight,
  X,
} from "lucide-react";

import ConfirmDialog from "../../components/common/ConfirmDialog";
import { useUsers } from "../../hooks/useUsers";
import {
  exportUsers,
  suspendUser,
  activateUser,
  deleteUser,
  updateUser,
  type AdminUser as User,
  type UserRole,
  type UserStatus,
} from "../../api/userApi";

const PAGE_SIZE = 20;

const ROLE_OPTIONS: UserRole[] = ["USER", "ADMIN"];
const STATUS_OPTIONS: UserStatus[] = ["ACTIVE", "PENDING", "SUSPENDED"];

/**
 * Delays updating `value` until the caller has stopped changing it for
 * `delayMs`, so a search box does not fire a backend request on every
 * keystroke.
 */
function useDebouncedValue<T>(value: T, delayMs: number): T {
  const [debounced, setDebounced] = useState(value);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      setDebounced(value);
    }, delayMs);

    return () => window.clearTimeout(timeoutId);
  }, [value, delayMs]);

  return debounced;
}

function StatCard({
  title,
  value,
  icon: Icon,
  color,
}: {
  title: string;
  value: string;
  icon: React.ElementType;
  color: string;
}) {
  return (
    <div className="rounded-3xl border border-white/10 bg-white/5 backdrop-blur-xl p-6 shadow-sm">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm text-slate-400">{title}</p>
          <h3 className="mt-2 text-3xl font-black text-white">{value}</h3>
        </div>

        <div className={`flex h-14 w-14 items-center justify-center rounded-2xl ${color}`}>
          <Icon size={24} />
        </div>
      </div>
    </div>
  );
}

function formatNumber(value: number): string {
  return new Intl.NumberFormat("en-US").format(value);
}

export default function UserManagementPage() {
  const navigate = useNavigate();

  const [search, setSearch] = useState("");
  const debouncedSearch = useDebouncedValue(search, 350);

  const [showFilters, setShowFilters] = useState(false);
  const [roleFilter, setRoleFilter] = useState<UserRole | "ALL">("ALL");
  const [statusFilter, setStatusFilter] = useState<UserStatus | "ALL">("ALL");
  const [page, setPage] = useState(0);

  const [exporting, setExporting] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [pendingActionId, setPendingActionId] = useState<string | null>(null);
  const [openMenuId, setOpenMenuId] = useState<string | null>(null);

  const [viewDialog, setViewDialog] = useState<User | null>(null);
  const [deleteDialog, setDeleteDialog] = useState<User | null>(null);

  const [editDialog, setEditDialog] = useState<{
    userId: string;
    fullName: string;
    email: string;
    phone: string;
    country: string;
    role: UserRole;
    password: string;
    submitting: boolean;
    error: string | null;
  } | null>(null);

  const {
    users,
    totalElements,
    totalPages,
    statistics,
    loading,
    error,
    refresh,
  } = useUsers({
    search: debouncedSearch,
    role: roleFilter,
    status: statusFilter,
    page,
    size: PAGE_SIZE,
  });

  /**
   * Filtering/pagination happen on the backend (the user table is not
   * assumed to fit client-side), so any filter change must return to the
   * first page - otherwise a narrower result set could leave `page`
   * pointing past the last available page.
   */
  useEffect(() => {
    setPage(0);
  }, [debouncedSearch, roleFilter, statusFilter]);

  const userList: User[] = users;

  /**
   * Stat cards reflect ALL users (from GET /admin/users/statistics), not
   * just the current page - `userList.length` would otherwise cap out at
   * PAGE_SIZE.
   */
  const stats = {
    total: statistics?.totalUsers ?? 0,
    active: statistics?.activeUsers ?? 0,
    suspended: statistics?.disabledUsers ?? 0,
    admins: statistics?.admins ?? 0,
    pending: statistics
      ? Math.max(
          0,
          statistics.totalUsers -
            statistics.activeUsers -
            statistics.disabledUsers,
        )
      : 0,
  };

  const handleCreateUser = useCallback(() => {
    navigate("/admin/users/create");
  }, [navigate]);

  const handleViewUser = useCallback((user: User) => {
    setViewDialog(user);
  }, []);

  const handleCloseViewDialog = useCallback(() => {
    setViewDialog(null);
  }, []);

  const handleEditUser = useCallback((user: User) => {
    setEditDialog({
      userId: user.id,
      fullName: user.name,
      email: user.email,
      phone: user.phone,
      country: user.country,
      role: user.role,
      password: "",
      submitting: false,
      error: null,
    });
  }, []);

  const handleCloseEditDialog = useCallback(() => {
    setEditDialog((current) => (current?.submitting ? current : null));
  }, []);

  const handleConfirmEdit = useCallback(async () => {
    if (!editDialog || editDialog.submitting) return;

    if (!editDialog.fullName.trim() || !editDialog.email.trim()) {
      setEditDialog((current) =>
        current
          ? { ...current, error: "Full name and email are required." }
          : current
      );
      return;
    }

    setEditDialog((current) =>
      current ? { ...current, submitting: true, error: null } : current
    );

    const loadingToast = toast.loading("Saving changes...");

    try {
      await updateUser(editDialog.userId, {
        fullName: editDialog.fullName.trim(),
        email: editDialog.email.trim(),
        phone: editDialog.phone.trim() || undefined,
        country: editDialog.country.trim() || undefined,
        role: editDialog.role,
        password: editDialog.password.trim() || undefined,
      });

      await refresh();

      toast.success("User details updated.", { id: loadingToast });
      setEditDialog(null);
    } catch (err) {
      console.error("[UserManagement] Update failed:", err);

      const message = axios.isAxiosError(err)
        ? err.response?.data?.message ?? "Failed to update user. Please try again."
        : "Failed to update user. Please try again.";

      toast.error(message, { id: loadingToast });

      setEditDialog((current) =>
        current ? { ...current, submitting: false, error: message } : current
      );
    }
  }, [editDialog, refresh]);

  const handleExportUsers = useCallback(async () => {
    if (exporting) return;

    const loadingToast = toast.loading("Exporting users...");

    try {
      setExporting(true);
      await exportUsers();

      toast.success("Export downloaded.", { id: loadingToast });
    } catch (err) {
      console.error("[UserManagement] Export failed:", err);
      toast.error("Failed to export users.", { id: loadingToast });
    } finally {
      setExporting(false);
    }
  }, [exporting]);

  const handleRefreshUsers = useCallback(async () => {
    if (refreshing) return;

    try {
      setRefreshing(true);
      await refresh();
      toast.success("User list refreshed.");
    } catch (err) {
      console.error("[UserManagement] Refresh failed:", err);
      toast.error("Failed to refresh users.");
    } finally {
      setRefreshing(false);
    }
  }, [refreshing, refresh]);

  const handleToggleSuspend = useCallback(
    async (user: User) => {
      if (pendingActionId) return;

      const isSuspending = user.status !== "SUSPENDED";
      const loadingToast = toast.loading(
        isSuspending ? `Suspending ${user.name}...` : `Reactivating ${user.name}...`
      );

      try {
        setPendingActionId(user.id);

        if (isSuspending) {
          await suspendUser(user.id);
        } else {
          await activateUser(user.id);
        }

        await refresh();

        toast.success(
          isSuspending ? `${user.name} has been suspended.` : `${user.name} has been reactivated.`,
          { id: loadingToast }
        );
      } catch (err) {
        console.error("[UserManagement] Status change failed:", err);

        const message = axios.isAxiosError(err)
          ? err.response?.data?.message ?? "Failed to update user status."
          : "Failed to update user status.";

        toast.error(message, { id: loadingToast });
      } finally {
        setPendingActionId(null);
      }
    },
    [pendingActionId, refresh]
  );

  const handleDeleteUser = useCallback(
    (user: User) => {
      if (pendingActionId) return;
      setDeleteDialog(user);
    },
    [pendingActionId]
  );

  const handleCancelDelete = useCallback(() => {
    if (pendingActionId) return;
    setDeleteDialog(null);
  }, [pendingActionId]);

  const handleConfirmDelete = useCallback(async () => {
    const user = deleteDialog;
    if (!user || pendingActionId) return;

    const loadingToast = toast.loading(`Deleting ${user.name}...`);

    try {
      setPendingActionId(user.id);
      await deleteUser(user.id);
      await refresh();

      toast.success(`${user.name} has been deleted.`, { id: loadingToast });
      setDeleteDialog(null);
    } catch (err) {
      console.error("[UserManagement] Delete failed:", err);

      const message = axios.isAxiosError(err)
        ? err.response?.data?.message ?? "Failed to delete user."
        : "Failed to delete user.";

      toast.error(message, { id: loadingToast });
    } finally {
      setPendingActionId(null);
    }
  }, [deleteDialog, pendingActionId, refresh]);

  const toggleMenu = useCallback((userId: string) => {
    setOpenMenuId((current) => (current === userId ? null : userId));
  }, []);

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="text-center">
          <div className="mx-auto mb-5 h-12 w-12 animate-spin rounded-full border-4 border-white/15 border-t-blue-400" />
          <p className="text-slate-300">Loading users...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="max-w-md rounded-3xl border border-red-500/30 bg-red-500/10 p-8 text-center">
          <AlertTriangle size={42} className="mx-auto mb-4 text-red-400" />
          <h2 className="text-xl font-bold text-white">Unable to load users</h2>
          <p className="mt-3 text-red-300">{error}</p>
          <button
            onClick={refresh}
            className="mt-6 rounded-xl bg-red-500 px-5 py-3 font-semibold text-white hover:bg-red-600"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen">
      <div className="w-full p-6">
        {/* Header */}
        <div className="mb-8 rounded-[32px] bg-gradient-to-r from-[#071426] via-[#0B1F3A] to-[#3C4C61] p-8 text-white shadow-2xl">
          <div className="flex flex-col gap-6 xl:flex-row xl:items-center xl:justify-between">
            <div>
              <div className="mb-4 inline-flex items-center gap-2 rounded-full bg-white/10 px-4 py-2 backdrop-blur-xl">
                <Shield size={16} />
                User Administration
              </div>

              <h1 className="text-4xl font-black">User Management Center</h1>

              <p className="mt-3 max-w-3xl text-blue-100">
                Manage applicants, immigration officers, reviewers, administrators,
                permissions, account security and user lifecycle operations.
              </p>
            </div>

            <div className="flex flex-wrap gap-3">
              <button
                type="button"
                onClick={handleCreateUser}
                className="flex items-center gap-2 rounded-2xl bg-[#C6A15B] px-5 py-3 font-bold text-[#071426] transition-opacity hover:opacity-90"
              >
                <UserPlus size={18} />
                Create User
              </button>

              <button
                type="button"
                onClick={handleExportUsers}
                disabled={exporting}
                className="flex items-center gap-2 rounded-2xl border border-white/20 bg-white/10 px-5 py-3 backdrop-blur-md disabled:cursor-not-allowed disabled:opacity-60"
              >
                <Download size={18} className={exporting ? "animate-pulse" : ""} />
                {exporting ? "Exporting..." : "Export Users"}
              </button>

              <button
                type="button"
                onClick={() => void handleRefreshUsers()}
                disabled={refreshing}
                className="flex items-center gap-2 rounded-2xl border border-white/20 bg-white/10 px-5 py-3 backdrop-blur-md disabled:cursor-not-allowed disabled:opacity-60"
              >
                <RefreshCw size={18} className={refreshing ? "animate-spin" : ""} />
                {refreshing ? "Refreshing..." : "Refresh"}
              </button>
            </div>
          </div>
        </div>

        {/* Statistics */}
        <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-4">
          <StatCard
            title="Total Users"
            value={formatNumber(stats.total)}
            icon={Users}
            color="bg-blue-500/10 text-blue-300"
          />

          <StatCard
            title="Active Users"
            value={formatNumber(stats.active)}
            icon={UserCheck}
            color="bg-emerald-500/10 text-emerald-300"
          />

          <StatCard
            title="Pending Verification"
            value={formatNumber(stats.pending)}
            icon={Clock3}
            color="bg-amber-500/10 text-amber-300"
          />

          <StatCard
            title="Administrators"
            value={formatNumber(stats.admins)}
            icon={Crown}
            color="bg-purple-500/10 text-purple-300"
          />
        </div>

        {/* Search & Filters */}
        <div className="mt-8 rounded-3xl border border-white/10 bg-white/5 backdrop-blur-xl p-5 shadow-sm">
          <div className="flex flex-col gap-4 lg:flex-row">
            <div className="relative flex-1">
              <Search size={18} className="absolute left-4 top-4 text-slate-400" />

              <input
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Search users..."
                className="w-full rounded-2xl border border-white/15 bg-white/5 text-white placeholder:text-slate-500 py-3 pl-11 pr-4 outline-none focus:border-blue-400"
              />
            </div>

            <button
              type="button"
              onClick={() => setShowFilters((v) => !v)}
              aria-expanded={showFilters}
              className={`flex items-center gap-2 rounded-2xl border px-5 py-3 transition-colors ${
                showFilters
                  ? "border-blue-400/50 bg-blue-500/10 text-blue-300"
                  : "border-white/15 text-slate-200"
              }`}
            >
              <Filter size={18} />
              Filters
              {(roleFilter !== "ALL" || statusFilter !== "ALL") && (
                <span className="ml-1 rounded-full bg-blue-600 px-2 py-0.5 text-xs font-bold text-white">
                  {[roleFilter !== "ALL", statusFilter !== "ALL"].filter(Boolean).length}
                </span>
              )}
            </button>
          </div>

          {showFilters && (
            <div className="mt-4 flex flex-wrap items-center gap-4 border-t border-white/10 pt-4">
              <div className="flex items-center gap-2">
                <label className="text-sm font-medium text-slate-300" htmlFor="role-filter">
                  Role
                </label>
                <select
                  id="role-filter"
                  value={roleFilter}
                  onChange={(e) => setRoleFilter(e.target.value as UserRole | "ALL")}
                  className="rounded-xl border border-white/15 bg-white/5 text-white px-3 py-2 text-sm outline-none focus:border-blue-400"
                >
                  <option value="ALL">All roles</option>
                  {ROLE_OPTIONS.map((role) => (
                    <option key={role} value={role}>
                      {role}
                    </option>
                  ))}
                </select>
              </div>

              <div className="flex items-center gap-2">
                <label className="text-sm font-medium text-slate-300" htmlFor="status-filter">
                  Status
                </label>
                <select
                  id="status-filter"
                  value={statusFilter}
                  onChange={(e) => setStatusFilter(e.target.value as UserStatus | "ALL")}
                  className="rounded-xl border border-white/15 bg-white/5 text-white px-3 py-2 text-sm outline-none focus:border-blue-400"
                >
                  <option value="ALL">All statuses</option>
                  {STATUS_OPTIONS.map((status) => (
                    <option key={status} value={status}>
                      {status}
                    </option>
                  ))}
                </select>
              </div>

              {(roleFilter !== "ALL" || statusFilter !== "ALL") && (
                <button
                  type="button"
                  onClick={() => {
                    setRoleFilter("ALL");
                    setStatusFilter("ALL");
                  }}
                  className="flex items-center gap-1 text-sm font-semibold text-slate-400 hover:text-white"
                >
                  <X size={14} />
                  Clear filters
                </button>
              )}
            </div>
          )}
        </div>

        {/* Users Table */}
        <div className="mt-8 overflow-hidden rounded-3xl border border-white/10 bg-white/5 backdrop-blur-xl shadow-sm">
          <div className="overflow-x-auto">
            <table className="min-w-full">
              <thead className="bg-white/5 text-slate-400">
                <tr>
                  <th className="px-6 py-4 text-left">User</th>
                  <th className="px-6 py-4 text-left">Role</th>
                  <th className="px-6 py-4 text-left">Status</th>
                  <th className="px-6 py-4 text-left">Country</th>
                  <th className="px-6 py-4 text-left">Applications</th>
                  <th className="px-6 py-4 text-left">Joined</th>
                  <th className="px-6 py-4 text-right">Actions</th>
                </tr>
              </thead>

              <tbody>
                {userList.length === 0 && (
                  <tr>
                    <td colSpan={7} className="px-6 py-10 text-center text-sm text-slate-400">
                      {debouncedSearch.trim() ||
                      roleFilter !== "ALL" ||
                      statusFilter !== "ALL"
                        ? "No users match your search or filters."
                        : "No users found."}
                    </td>
                  </tr>
                )}

                {userList.map((user) => {
                  const isPending = pendingActionId === user.id;
                  const isSuspended = user.status === "SUSPENDED";

                  return (
                    <tr key={user.id} className="border-t border-white/10">
                      <td className="px-6 py-4">
                        <div>
                          <h3 className="font-semibold">{user.name}</h3>

                          <div className="mt-1 flex flex-col gap-1 text-xs text-slate-400">
                            <span className="flex items-center gap-1">
                              <Mail size={12} />
                              {user.email}
                            </span>

                            <span className="flex items-center gap-1">
                              <Phone size={12} />
                              {user.phone || "—"}
                            </span>
                          </div>
                        </div>
                      </td>

                      <td className="px-6 py-4">
                        <span className="rounded-full bg-blue-500/10 px-3 py-1 text-xs font-semibold text-blue-300">
                          {user.role}
                        </span>
                      </td>

                      <td className="px-6 py-4">
                        <span
                          className={`rounded-full px-3 py-1 text-xs font-semibold ${
                            user.status === "ACTIVE"
                              ? "bg-emerald-500/10 text-emerald-300"
                              : user.status === "PENDING"
                              ? "bg-amber-500/10 text-amber-300"
                              : "bg-red-500/10 text-red-300"
                          }`}
                        >
                          {user.status}
                        </span>
                      </td>

                      <td className="px-6 py-4">
                        <div className="flex items-center gap-2">
                          <Globe size={15} />
                          {user.country || "—"}
                        </div>
                      </td>

                      <td className="px-6 py-4 font-semibold">{user.applications}</td>

                      <td className="px-6 py-4 text-slate-300">{user.joined}</td>

                      <td className="px-6 py-4">
                        <div className="relative flex justify-end gap-2">
                          <button
                            type="button"
                            onClick={() => handleViewUser(user)}
                            aria-label={`View ${user.name}`}
                            className="rounded-xl p-2 hover:bg-white/10"
                          >
                            <Eye size={16} />
                          </button>

                          <button
                            type="button"
                            onClick={() => handleEditUser(user)}
                            aria-label={`Edit ${user.name}`}
                            className="rounded-xl p-2 hover:bg-white/10"
                          >
                            <Edit size={16} />
                          </button>

                          <button
                            type="button"
                            onClick={() => handleToggleSuspend(user)}
                            disabled={isPending}
                            aria-label={isSuspended ? `Reactivate ${user.name}` : `Suspend ${user.name}`}
                            className="rounded-xl p-2 hover:bg-white/10 disabled:cursor-not-allowed disabled:opacity-50"
                          >
                            {isSuspended ? <Unlock size={16} /> : <Lock size={16} />}
                          </button>

                          <button
                            type="button"
                            onClick={() => handleDeleteUser(user)}
                            disabled={isPending}
                            aria-label={`Delete ${user.name}`}
                            className="rounded-xl p-2 text-red-400 hover:bg-red-500/10 disabled:cursor-not-allowed disabled:opacity-50"
                          >
                            <Trash2 size={16} />
                          </button>

                          <button
                            type="button"
                            onClick={() => toggleMenu(user.id)}
                            aria-label={`More actions for ${user.name}`}
                            aria-expanded={openMenuId === user.id}
                            className="rounded-xl p-2 hover:bg-white/10"
                          >
                            <MoreVertical size={16} />
                          </button>

                          {openMenuId === user.id && (
                            <div className="absolute right-0 top-11 z-10 w-48 rounded-2xl border border-white/10 bg-[#1F314A] backdrop-blur-xl py-2 shadow-2xl shadow-black/40">
                              <button
                                type="button"
                                onClick={() => {
                                  setOpenMenuId(null);
                                  handleViewUser(user);
                                }}
                                className="flex w-full items-center gap-2 px-4 py-2 text-left text-sm text-slate-200 hover:bg-white/5"
                              >
                                <Eye size={14} />
                                View profile
                              </button>
                              <button
                                type="button"
                                onClick={() => {
                                  setOpenMenuId(null);
                                  handleEditUser(user);
                                }}
                                className="flex w-full items-center gap-2 px-4 py-2 text-left text-sm text-slate-200 hover:bg-white/5"
                              >
                                <Edit size={14} />
                                Edit details
                              </button>
                              <button
                                type="button"
                                onClick={() => {
                                  setOpenMenuId(null);
                                  handleToggleSuspend(user);
                                }}
                                className="flex w-full items-center gap-2 px-4 py-2 text-left text-sm text-slate-200 hover:bg-white/5"
                              >
                                {isSuspended ? <Unlock size={14} /> : <Lock size={14} />}
                                {isSuspended ? "Reactivate account" : "Suspend account"}
                              </button>
                              <button
                                type="button"
                                onClick={() => {
                                  setOpenMenuId(null);
                                  handleDeleteUser(user);
                                }}
                                className="flex w-full items-center gap-2 px-4 py-2 text-left text-sm text-red-400 hover:bg-red-500/10"
                              >
                                <Trash2 size={14} />
                                Delete user
                              </button>
                            </div>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          {totalElements > 0 && (
            <div className="flex flex-col gap-3 border-t border-white/10 px-6 py-4 sm:flex-row sm:items-center sm:justify-between">
              <p className="text-sm text-slate-400">
                Showing{" "}
                <span className="font-semibold text-slate-200">
                  {page * PAGE_SIZE + 1}
                </span>
                {"–"}
                <span className="font-semibold text-slate-200">
                  {Math.min(page * PAGE_SIZE + PAGE_SIZE, totalElements)}
                </span>{" "}
                of{" "}
                <span className="font-semibold text-slate-200">
                  {formatNumber(totalElements)}
                </span>{" "}
                users
              </p>

              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => setPage((current) => Math.max(0, current - 1))}
                  disabled={page === 0}
                  className="flex items-center gap-1 rounded-xl border border-white/15 px-3 py-2 text-sm font-semibold text-slate-200 hover:bg-white/10 disabled:cursor-not-allowed disabled:opacity-40"
                >
                  <ChevronLeft size={16} />
                  Previous
                </button>

                <span className="px-2 text-sm text-slate-400">
                  Page {page + 1} of {Math.max(totalPages, 1)}
                </span>

                <button
                  type="button"
                  onClick={() =>
                    setPage((current) =>
                      Math.min(Math.max(totalPages - 1, 0), current + 1),
                    )
                  }
                  disabled={page + 1 >= totalPages}
                  className="flex items-center gap-1 rounded-xl border border-white/15 px-3 py-2 text-sm font-semibold text-slate-200 hover:bg-white/10 disabled:cursor-not-allowed disabled:opacity-40"
                >
                  Next
                  <ChevronRight size={16} />
                </button>
              </div>
            </div>
          )}
        </div>

        {/* Alerts */}
        <div className="mt-8 grid gap-5 xl:grid-cols-3">
          <div className="rounded-3xl border border-red-500/20 bg-red-500/10 p-5 text-red-300">
            <div className="flex items-center gap-3">
              <AlertTriangle />
              <span className="font-bold">Security Alert</span>
            </div>

            <p className="mt-2 text-sm">
              {stats.suspended > 0
                ? `${formatNumber(stats.suspended)} account${
                    stats.suspended === 1 ? " is" : "s are"
                  } currently suspended pending security review.`
                : "No accounts currently require security review."}
            </p>
          </div>

          <div className="rounded-3xl border border-amber-500/20 bg-amber-500/10 p-5 text-amber-300">
            <div className="flex items-center gap-3">
              <Activity />
              <span className="font-bold">Verification Queue</span>
            </div>

            <p className="mt-2 text-sm">
              {stats.pending > 0
                ? `${formatNumber(stats.pending)} user${
                    stats.pending === 1 ? " is" : "s are"
                  } awaiting identity verification.`
                : "No users are awaiting verification."}
            </p>
          </div>

          <div className="rounded-3xl border border-emerald-500/20 bg-emerald-500/10 p-5 text-emerald-300">
            <div className="flex items-center gap-3">
              <CheckCircle2 />
              <span className="font-bold">System Status</span>
            </div>

            <p className="mt-2 text-sm">User management services are operating normally.</p>
          </div>
        </div>
      </div>

      {/* View User Dialog */}
      {viewDialog && (
        <div
          role="dialog"
          aria-modal="true"
          aria-label="User details"
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
          onClick={handleCloseViewDialog}
        >
          <div
            onClick={(e) => e.stopPropagation()}
            className="w-full max-w-md rounded-[1.5rem] border border-white/10 bg-[#0B1F3A] p-6 shadow-2xl shadow-black/40"
          >
            <div className="flex items-start justify-between gap-3">
              <div className="flex items-start gap-3">
                <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-blue-400/10 text-blue-300">
                  <Eye size={20} />
                </div>

                <div className="min-w-0">
                  <h2 className="truncate text-lg font-black text-white">
                    {viewDialog.name}
                  </h2>
                  <p className="mt-1 text-sm text-slate-400">
                    User #{viewDialog.id}
                  </p>
                </div>
              </div>

              <button
                type="button"
                onClick={handleCloseViewDialog}
                aria-label="Close"
                className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-slate-400 transition hover:bg-white/10 hover:text-white"
              >
                <X size={16} />
              </button>
            </div>

            <div className="mt-5 flex flex-wrap gap-2">
              <span className="rounded-full bg-blue-500/10 px-3 py-1 text-xs font-bold text-blue-300">
                {viewDialog.role}
              </span>
              <span
                className={`rounded-full px-3 py-1 text-xs font-bold ${
                  viewDialog.status === "ACTIVE"
                    ? "bg-emerald-500/10 text-emerald-300"
                    : viewDialog.status === "PENDING"
                    ? "bg-amber-500/10 text-amber-300"
                    : "bg-red-500/10 text-red-300"
                }`}
              >
                {viewDialog.status}
              </span>
            </div>

            <dl className="mt-5 grid grid-cols-2 gap-x-4 gap-y-4 text-sm">
              <div>
                <dt className="text-xs font-medium uppercase tracking-wide text-slate-500">Email</dt>
                <dd className="mt-1 truncate font-semibold text-white">{viewDialog.email || "—"}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium uppercase tracking-wide text-slate-500">Phone</dt>
                <dd className="mt-1 font-semibold text-white">{viewDialog.phone || "—"}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium uppercase tracking-wide text-slate-500">Country</dt>
                <dd className="mt-1 font-semibold text-white">{viewDialog.country || "—"}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium uppercase tracking-wide text-slate-500">Applications</dt>
                <dd className="mt-1 font-semibold text-white">{viewDialog.applications}</dd>
              </div>
              <div className="col-span-2">
                <dt className="text-xs font-medium uppercase tracking-wide text-slate-500">Joined</dt>
                <dd className="mt-1 font-semibold text-white">{viewDialog.joined || "—"}</dd>
              </div>
            </dl>

            <div className="mt-6 flex justify-end">
              <button
                type="button"
                onClick={handleCloseViewDialog}
                className="inline-flex h-11 items-center justify-center rounded-xl border border-white/15 bg-white/5 px-5 text-sm font-semibold text-slate-200 transition hover:bg-white/10"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Edit User Dialog */}
      {editDialog && (
        <div
          role="dialog"
          aria-modal="true"
          aria-label="Edit user"
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
          onClick={handleCloseEditDialog}
        >
          <div
            onClick={(e) => e.stopPropagation()}
            className="w-full max-w-md rounded-[1.5rem] border border-white/10 bg-[#0B1F3A] p-6 shadow-2xl shadow-black/40"
          >
            <div className="flex items-start justify-between gap-3">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-[#C6A15B]/10 text-[#C6A15B]">
                  <Edit size={20} />
                </div>
                <h2 className="text-lg font-black text-white">Edit user</h2>
              </div>

              <button
                type="button"
                onClick={handleCloseEditDialog}
                disabled={editDialog.submitting}
                aria-label="Close"
                className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-slate-400 transition hover:bg-white/10 hover:text-white disabled:cursor-not-allowed disabled:opacity-60"
              >
                <X size={16} />
              </button>
            </div>

            <div className="mt-5 space-y-4">
              <div>
                <label className="mb-1.5 block text-sm font-medium text-slate-200" htmlFor="edit-fullName">
                  Full name
                </label>
                <input
                  id="edit-fullName"
                  value={editDialog.fullName}
                  onChange={(e) =>
                    setEditDialog((c) => (c ? { ...c, fullName: e.target.value } : c))
                  }
                  disabled={editDialog.submitting}
                  className="w-full rounded-xl border border-white/15 bg-white/5 px-4 py-2.5 text-sm text-white outline-none focus:border-blue-400 disabled:cursor-not-allowed disabled:opacity-60"
                />
              </div>

              <div>
                <label className="mb-1.5 block text-sm font-medium text-slate-200" htmlFor="edit-email">
                  Email
                </label>
                <input
                  id="edit-email"
                  type="email"
                  value={editDialog.email}
                  onChange={(e) =>
                    setEditDialog((c) => (c ? { ...c, email: e.target.value } : c))
                  }
                  disabled={editDialog.submitting}
                  className="w-full rounded-xl border border-white/15 bg-white/5 px-4 py-2.5 text-sm text-white outline-none focus:border-blue-400 disabled:cursor-not-allowed disabled:opacity-60"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="mb-1.5 block text-sm font-medium text-slate-200" htmlFor="edit-phone">
                    Phone
                  </label>
                  <input
                    id="edit-phone"
                    value={editDialog.phone}
                    onChange={(e) =>
                      setEditDialog((c) => (c ? { ...c, phone: e.target.value } : c))
                    }
                    disabled={editDialog.submitting}
                    className="w-full rounded-xl border border-white/15 bg-white/5 px-4 py-2.5 text-sm text-white outline-none focus:border-blue-400 disabled:cursor-not-allowed disabled:opacity-60"
                  />
                </div>

                <div>
                  <label className="mb-1.5 block text-sm font-medium text-slate-200" htmlFor="edit-country">
                    Country
                  </label>
                  <input
                    id="edit-country"
                    value={editDialog.country}
                    onChange={(e) =>
                      setEditDialog((c) => (c ? { ...c, country: e.target.value } : c))
                    }
                    disabled={editDialog.submitting}
                    className="w-full rounded-xl border border-white/15 bg-white/5 px-4 py-2.5 text-sm text-white outline-none focus:border-blue-400 disabled:cursor-not-allowed disabled:opacity-60"
                  />
                </div>
              </div>

              <div>
                <label className="mb-1.5 block text-sm font-medium text-slate-200" htmlFor="edit-role">
                  Role
                </label>
                <select
                  id="edit-role"
                  value={editDialog.role}
                  onChange={(e) =>
                    setEditDialog((c) =>
                      c ? { ...c, role: e.target.value as UserRole } : c
                    )
                  }
                  disabled={editDialog.submitting}
                  className="w-full rounded-xl border border-white/15 bg-white/5 px-4 py-2.5 text-sm text-white outline-none focus:border-blue-400 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {ROLE_OPTIONS.map((role) => (
                    <option key={role} value={role}>
                      {role}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="mb-1.5 block text-sm font-medium text-slate-200" htmlFor="edit-password">
                  New password (optional)
                </label>
                <input
                  id="edit-password"
                  type="password"
                  value={editDialog.password}
                  onChange={(e) =>
                    setEditDialog((c) => (c ? { ...c, password: e.target.value } : c))
                  }
                  disabled={editDialog.submitting}
                  placeholder="Leave blank to keep current password"
                  className="w-full rounded-xl border border-white/15 bg-white/5 px-4 py-2.5 text-sm text-white outline-none placeholder:text-slate-500 focus:border-blue-400 disabled:cursor-not-allowed disabled:opacity-60"
                />
              </div>

              {editDialog.error && (
                <p className="text-sm text-red-400">{editDialog.error}</p>
              )}
            </div>

            <div className="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
              <button
                type="button"
                onClick={handleCloseEditDialog}
                disabled={editDialog.submitting}
                className="inline-flex h-11 items-center justify-center rounded-xl border border-white/15 bg-white/5 px-5 text-sm font-semibold text-slate-200 transition hover:bg-white/10 disabled:cursor-not-allowed disabled:opacity-60"
              >
                Cancel
              </button>

              <button
                type="button"
                onClick={() => void handleConfirmEdit()}
                disabled={editDialog.submitting}
                className="inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-[#C6A15B] px-5 text-sm font-bold text-black transition hover:bg-[#A8894D] disabled:cursor-not-allowed disabled:opacity-60"
              >
                {editDialog.submitting ? (
                  <RefreshCw size={16} className="animate-spin" />
                ) : (
                  <Edit size={16} />
                )}
                Save changes
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Delete User Confirmation */}
      <ConfirmDialog
        open={deleteDialog !== null}
        title="Delete user"
        description={
          deleteDialog
            ? `Are you sure you want to delete ${deleteDialog.name}? This will permanently remove their account${
                deleteDialog.applications > 0
                  ? `, ${deleteDialog.applications} application${
                      deleteDialog.applications === 1 ? "" : "s"
                    }, and all related documents`
                  : ""
              }. This action cannot be undone.`
            : ""
        }
        confirmText="Delete"
        cancelText="Cancel"
        variant="danger"
        loading={pendingActionId === deleteDialog?.id}
        onConfirm={() => void handleConfirmDelete()}
        onCancel={handleCancelDelete}
      />
    </div>
  );
}