import React, { useCallback, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
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
  X,
} from "lucide-react";

import { useUsers } from "../../hooks/useUsers";
import {
  exportUsers,
  syncUsers,
  suspendUser,
  activateUser,
  deleteUser,
} from "../../api/userApi";

type UserRole = "USER" | "REVIEWER" | "OFFICER" | "ADMIN";
type UserStatus = "ACTIVE" | "PENDING" | "SUSPENDED";

interface User {
  id: string;
  name: string;
  email: string;
  phone: string;
  country: string;
  role: UserRole;
  status: UserStatus;
  applications: number;
  joined: string;
}

const ROLE_OPTIONS: UserRole[] = ["USER", "REVIEWER", "OFFICER", "ADMIN"];
const STATUS_OPTIONS: UserStatus[] = ["ACTIVE", "PENDING", "SUSPENDED"];

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
    <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm text-slate-500">{title}</p>
          <h3 className="mt-2 text-3xl font-black text-slate-900">{value}</h3>
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
  const { users, loading, error, refresh } = useUsers();

  const [search, setSearch] = useState("");
  const [showFilters, setShowFilters] = useState(false);
  const [roleFilter, setRoleFilter] = useState<UserRole | "ALL">("ALL");
  const [statusFilter, setStatusFilter] = useState<UserStatus | "ALL">("ALL");

  const [exporting, setExporting] = useState(false);
  const [syncing, setSyncing] = useState(false);
  const [pendingActionId, setPendingActionId] = useState<string | null>(null);
  const [openMenuId, setOpenMenuId] = useState<string | null>(null);

  const userList: User[] = Array.isArray(users) ? users : [];

  const filteredUsers = useMemo(() => {
    const term = search.trim().toLowerCase();

    return userList.filter((user) => {
      const matchesSearch =
        term.length === 0 ||
        user.name.toLowerCase().includes(term) ||
        user.email.toLowerCase().includes(term);

      const matchesRole = roleFilter === "ALL" || user.role === roleFilter;
      const matchesStatus = statusFilter === "ALL" || user.status === statusFilter;

      return matchesSearch && matchesRole && matchesStatus;
    });
  }, [userList, search, roleFilter, statusFilter]);

  /**
   * Stats are derived live from the loaded user list rather than hardcoded.
   * If the backend exposes aggregate counts separately (e.g. for datasets
   * too large to load client-side), swap these for a dedicated stats
   * endpoint/hook instead of computing from `users`.
   */
  const stats = useMemo(() => {
    const total = userList.length;
    const active = userList.filter((u) => u.status === "ACTIVE").length;
    const pending = userList.filter((u) => u.status === "PENDING").length;
    const admins = userList.filter((u) => u.role === "ADMIN").length;
    const suspended = userList.filter((u) => u.status === "SUSPENDED").length;

    return { total, active, pending, admins, suspended };
  }, [userList]);

  const handleCreateUser = useCallback(() => {
    navigate("/admin/users/create");
  }, [navigate]);

  const handleViewUser = useCallback(
    (user: User) => {
      navigate(`/admin/users/${user.id}`);
    },
    [navigate]
  );

  const handleEditUser = useCallback(
    (user: User) => {
      navigate(`/admin/users/${user.id}/edit`);
    },
    [navigate]
  );

  const handleExportUsers = useCallback(async () => {
    if (exporting) return;

    const loadingToast = toast.loading("Exporting users...");

    try {
      setExporting(true);
      const result = await exportUsers();

      if (result?.downloadUrl) {
        window.open(result.downloadUrl, "_blank", "noopener,noreferrer");
      }

      toast.success(result?.message ?? "Export ready.", { id: loadingToast });
    } catch (err) {
      console.error("[UserManagement] Export failed:", err);
      toast.error("Failed to export users.", { id: loadingToast });
    } finally {
      setExporting(false);
    }
  }, [exporting]);

  const handleSyncUsers = useCallback(async () => {
    if (syncing) return;

    const loadingToast = toast.loading("Syncing users...");

    try {
      setSyncing(true);
      const result = await syncUsers();
      await refresh();

      toast.success(result?.message ?? "Users synced successfully.", {
        id: loadingToast,
      });
    } catch (err) {
      console.error("[UserManagement] Sync failed:", err);
      toast.error("Failed to sync users.", { id: loadingToast });
    } finally {
      setSyncing(false);
    }
  }, [syncing, refresh]);

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
        toast.error("Failed to update user status.", { id: loadingToast });
      } finally {
        setPendingActionId(null);
      }
    },
    [pendingActionId, refresh]
  );

  const handleDeleteUser = useCallback(
    async (user: User) => {
      if (pendingActionId) return;

      const confirmed = window.confirm(
        `Delete ${user.name}? This action cannot be undone.`
      );
      if (!confirmed) return;

      const loadingToast = toast.loading(`Deleting ${user.name}...`);

      try {
        setPendingActionId(user.id);
        await deleteUser(user.id);
        await refresh();

        toast.success(`${user.name} has been deleted.`, { id: loadingToast });
      } catch (err) {
        console.error("[UserManagement] Delete failed:", err);
        toast.error("Failed to delete user.", { id: loadingToast });
      } finally {
        setPendingActionId(null);
      }
    },
    [pendingActionId, refresh]
  );

  const toggleMenu = useCallback((userId: string) => {
    setOpenMenuId((current) => (current === userId ? null : userId));
  }, []);

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-slate-50 via-blue-50 to-amber-50">
        <div className="text-center">
          <div className="mx-auto mb-5 h-12 w-12 animate-spin rounded-full border-4 border-slate-300 border-t-blue-600" />
          <p className="text-slate-600">Loading users...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-slate-50 via-blue-50 to-amber-50">
        <div className="max-w-md rounded-3xl border border-red-200 bg-red-50 p-8 text-center">
          <AlertTriangle size={42} className="mx-auto mb-4 text-red-500" />
          <h2 className="text-xl font-bold text-slate-900">Unable to load users</h2>
          <p className="mt-3 text-red-600">{error}</p>
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
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-amber-50">
      <div className="mx-auto max-w-[1800px] p-6">
        {/* Header */}
        <div className="mb-8 rounded-[32px] bg-gradient-to-r from-[#071330] via-[#0B1736] to-[#183B6B] p-8 text-white shadow-2xl">
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
                className="flex items-center gap-2 rounded-2xl bg-[#F4B81A] px-5 py-3 font-bold text-[#071330] transition-opacity hover:opacity-90"
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
                onClick={handleSyncUsers}
                disabled={syncing}
                className="flex items-center gap-2 rounded-2xl border border-white/20 bg-white/10 px-5 py-3 backdrop-blur-md disabled:cursor-not-allowed disabled:opacity-60"
              >
                <RefreshCw size={18} className={syncing ? "animate-spin" : ""} />
                {syncing ? "Syncing..." : "Sync Users"}
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
            color="bg-blue-100 text-blue-700"
          />

          <StatCard
            title="Active Users"
            value={formatNumber(stats.active)}
            icon={UserCheck}
            color="bg-emerald-100 text-emerald-700"
          />

          <StatCard
            title="Pending Verification"
            value={formatNumber(stats.pending)}
            icon={Clock3}
            color="bg-amber-100 text-amber-700"
          />

          <StatCard
            title="Administrators"
            value={formatNumber(stats.admins)}
            icon={Crown}
            color="bg-purple-100 text-purple-700"
          />
        </div>

        {/* Search & Filters */}
        <div className="mt-8 rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
          <div className="flex flex-col gap-4 lg:flex-row">
            <div className="relative flex-1">
              <Search size={18} className="absolute left-4 top-4 text-slate-400" />

              <input
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Search users..."
                className="w-full rounded-2xl border border-slate-200 py-3 pl-11 pr-4 outline-none focus:border-blue-500"
              />
            </div>

            <button
              type="button"
              onClick={() => setShowFilters((v) => !v)}
              aria-expanded={showFilters}
              className={`flex items-center gap-2 rounded-2xl border px-5 py-3 transition-colors ${
                showFilters
                  ? "border-blue-500 bg-blue-50 text-blue-700"
                  : "border-slate-200 text-slate-700"
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
            <div className="mt-4 flex flex-wrap items-center gap-4 border-t border-slate-100 pt-4">
              <div className="flex items-center gap-2">
                <label className="text-sm font-medium text-slate-600" htmlFor="role-filter">
                  Role
                </label>
                <select
                  id="role-filter"
                  value={roleFilter}
                  onChange={(e) => setRoleFilter(e.target.value as UserRole | "ALL")}
                  className="rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-blue-500"
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
                <label className="text-sm font-medium text-slate-600" htmlFor="status-filter">
                  Status
                </label>
                <select
                  id="status-filter"
                  value={statusFilter}
                  onChange={(e) => setStatusFilter(e.target.value as UserStatus | "ALL")}
                  className="rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-blue-500"
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
                  className="flex items-center gap-1 text-sm font-semibold text-slate-500 hover:text-slate-700"
                >
                  <X size={14} />
                  Clear filters
                </button>
              )}
            </div>
          )}
        </div>

        {/* Users Table */}
        <div className="mt-8 overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm">
          <div className="overflow-x-auto">
            <table className="min-w-full">
              <thead className="bg-slate-50">
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
                {filteredUsers.length === 0 && (
                  <tr>
                    <td colSpan={7} className="px-6 py-10 text-center text-sm text-slate-500">
                      {userList.length === 0
                        ? "No users found."
                        : "No users match your search or filters."}
                    </td>
                  </tr>
                )}

                {filteredUsers.map((user) => {
                  const isPending = pendingActionId === user.id;
                  const isSuspended = user.status === "SUSPENDED";

                  return (
                    <tr key={user.id} className="border-t border-slate-100">
                      <td className="px-6 py-4">
                        <div>
                          <h3 className="font-semibold">{user.name}</h3>

                          <div className="mt-1 flex flex-col gap-1 text-xs text-slate-500">
                            <span className="flex items-center gap-1">
                              <Mail size={12} />
                              {user.email}
                            </span>

                            <span className="flex items-center gap-1">
                              <Phone size={12} />
                              {user.phone}
                            </span>
                          </div>
                        </div>
                      </td>

                      <td className="px-6 py-4">
                        <span className="rounded-full bg-blue-100 px-3 py-1 text-xs font-semibold text-blue-700">
                          {user.role}
                        </span>
                      </td>

                      <td className="px-6 py-4">
                        <span
                          className={`rounded-full px-3 py-1 text-xs font-semibold ${
                            user.status === "ACTIVE"
                              ? "bg-emerald-100 text-emerald-700"
                              : user.status === "PENDING"
                              ? "bg-amber-100 text-amber-700"
                              : "bg-red-100 text-red-700"
                          }`}
                        >
                          {user.status}
                        </span>
                      </td>

                      <td className="px-6 py-4">
                        <div className="flex items-center gap-2">
                          <Globe size={15} />
                          {user.country}
                        </div>
                      </td>

                      <td className="px-6 py-4 font-semibold">{user.applications}</td>

                      <td className="px-6 py-4 text-slate-600">{user.joined}</td>

                      <td className="px-6 py-4">
                        <div className="relative flex justify-end gap-2">
                          <button
                            type="button"
                            onClick={() => handleViewUser(user)}
                            aria-label={`View ${user.name}`}
                            className="rounded-xl p-2 hover:bg-slate-100"
                          >
                            <Eye size={16} />
                          </button>

                          <button
                            type="button"
                            onClick={() => handleEditUser(user)}
                            aria-label={`Edit ${user.name}`}
                            className="rounded-xl p-2 hover:bg-slate-100"
                          >
                            <Edit size={16} />
                          </button>

                          <button
                            type="button"
                            onClick={() => handleToggleSuspend(user)}
                            disabled={isPending}
                            aria-label={isSuspended ? `Reactivate ${user.name}` : `Suspend ${user.name}`}
                            className="rounded-xl p-2 hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-50"
                          >
                            {isSuspended ? <Unlock size={16} /> : <Lock size={16} />}
                          </button>

                          <button
                            type="button"
                            onClick={() => handleDeleteUser(user)}
                            disabled={isPending}
                            aria-label={`Delete ${user.name}`}
                            className="rounded-xl p-2 text-red-600 hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-50"
                          >
                            <Trash2 size={16} />
                          </button>

                          <button
                            type="button"
                            onClick={() => toggleMenu(user.id)}
                            aria-label={`More actions for ${user.name}`}
                            aria-expanded={openMenuId === user.id}
                            className="rounded-xl p-2 hover:bg-slate-100"
                          >
                            <MoreVertical size={16} />
                          </button>

                          {openMenuId === user.id && (
                            <div className="absolute right-0 top-11 z-10 w-48 rounded-2xl border border-slate-200 bg-white py-2 shadow-lg">
                              <button
                                type="button"
                                onClick={() => {
                                  setOpenMenuId(null);
                                  handleViewUser(user);
                                }}
                                className="flex w-full items-center gap-2 px-4 py-2 text-left text-sm hover:bg-slate-50"
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
                                className="flex w-full items-center gap-2 px-4 py-2 text-left text-sm hover:bg-slate-50"
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
                                className="flex w-full items-center gap-2 px-4 py-2 text-left text-sm hover:bg-slate-50"
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
                                className="flex w-full items-center gap-2 px-4 py-2 text-left text-sm text-red-600 hover:bg-red-50"
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
        </div>

        {/* Alerts */}
        <div className="mt-8 grid gap-5 xl:grid-cols-3">
          <div className="rounded-3xl bg-red-50 p-5 text-red-700">
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

          <div className="rounded-3xl bg-amber-50 p-5 text-amber-700">
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

          <div className="rounded-3xl bg-emerald-50 p-5 text-emerald-700">
            <div className="flex items-center gap-3">
              <CheckCircle2 />
              <span className="font-bold">System Status</span>
            </div>

            <p className="mt-2 text-sm">User management services are operating normally.</p>
          </div>
        </div>
      </div>
    </div>
  );
}