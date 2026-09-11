import React from "react";

export type UserRole =
  | "user"
  | "admin"
  | "super_admin";

export type UserStatus =
  | "active"
  | "suspended"
  | "pending";

export interface AdminUser {
  id: string;
  fullName: string;
  email: string;
  role: UserRole;
  status: UserStatus;
  documentsCount: number;
  createdAt: string;
  lastLogin?: string;
}

interface AdminUserTableProps {
  users: AdminUser[];
  loading?: boolean;
  onViewUser?: (user: AdminUser) => void;
  onSuspendUser?: (user: AdminUser) => void;
  onActivateUser?: (user: AdminUser) => void;
  onDeleteUser?: (user: AdminUser) => void;
}

const roleStyles: Record<UserRole, string> = {
  user: "bg-blue-400/10 text-blue-300",
  admin: "bg-purple-400/10 text-purple-300",
  super_admin: "bg-red-400/10 text-red-300",
};

const statusStyles: Record<UserStatus, string> = {
  active: "bg-emerald-400/10 text-emerald-300",
  suspended: "bg-red-400/10 text-red-300",
  pending: "bg-[#C6A15B]/10 text-[#C6A15B]",
};

const formatDate = (date: string) =>
  new Date(date).toLocaleDateString();

const AdminUserTable: React.FC<AdminUserTableProps> = ({
  users,
  loading,
  onViewUser,
  onSuspendUser,
  onActivateUser,
  onDeleteUser,
}) => {
  if (loading) {
    return (
      <div className="rounded-lg border border-white/10 bg-white/5 p-8 text-center text-slate-300">
        Loading users...
      </div>
    );
  }

  if (!users.length) {
    return (
      <div className="rounded-lg border border-white/10 bg-white/5 p-8 text-center text-slate-300">
        No users found.
      </div>
    );
  }

  return (
    <div className="overflow-hidden rounded-xl border border-white/10 bg-white/5 backdrop-blur-xl shadow-lg shadow-black/20">
      <div className="overflow-x-auto">
        <table className="min-w-full">
          <thead className="border-b border-white/10 bg-white/5">
            <tr>
              <th className="px-4 py-3 text-left text-sm font-semibold text-slate-200">
                User
              </th>

              <th className="px-4 py-3 text-left text-sm font-semibold text-slate-200">
                Role
              </th>

              <th className="px-4 py-3 text-left text-sm font-semibold text-slate-200">
                Status
              </th>

              <th className="px-4 py-3 text-left text-sm font-semibold text-slate-200">
                Documents
              </th>

              <th className="px-4 py-3 text-left text-sm font-semibold text-slate-200">
                Joined
              </th>

              <th className="px-4 py-3 text-left text-sm font-semibold text-slate-200">
                Last Login
              </th>

              <th className="px-4 py-3 text-right text-sm font-semibold text-slate-200">
                Actions
              </th>
            </tr>
          </thead>

          <tbody>
            {users.map((user) => (
              <tr
                key={user.id}
                className="border-b border-white/10 hover:bg-white/[0.06]"
              >
                <td className="px-4 py-4">
                  <div>
                    <p className="font-medium text-white">
                      {user.fullName}
                    </p>

                    <p className="text-sm text-slate-400">
                      {user.email}
                    </p>
                  </div>
                </td>

                <td className="px-4 py-4">
                  <span
                    className={`rounded-full px-3 py-1 text-xs font-medium ${roleStyles[user.role]}`}
                  >
                    {user.role}
                  </span>
                </td>

                <td className="px-4 py-4">
                  <span
                    className={`rounded-full px-3 py-1 text-xs font-medium ${statusStyles[user.status]}`}
                  >
                    {user.status}
                  </span>
                </td>

                <td className="px-4 py-4 text-slate-300">
                  {user.documentsCount}
                </td>

                <td className="px-4 py-4 text-slate-300">
                  {formatDate(user.createdAt)}
                </td>

                <td className="px-4 py-4 text-slate-300">
                  {user.lastLogin
                    ? formatDate(user.lastLogin)
                    : "Never"}
                </td>

                <td className="px-4 py-4">
                  <div className="flex justify-end gap-2">
                    <button
                      onClick={() =>
                        onViewUser?.(user)
                      }
                      className="rounded border border-white/15 px-3 py-1 text-sm text-slate-200 hover:bg-white/10"
                    >
                      View
                    </button>

                    {user.status === "active" ? (
                      <button
                        onClick={() =>
                          onSuspendUser?.(user)
                        }
                        className="rounded border border-red-400/30 px-3 py-1 text-sm text-red-300 hover:bg-red-400/10"
                      >
                        Suspend
                      </button>
                    ) : (
                      <button
                        onClick={() =>
                          onActivateUser?.(user)
                        }
                        className="rounded border border-emerald-400/30 px-3 py-1 text-sm text-emerald-300 hover:bg-emerald-400/10"
                      >
                        Activate
                      </button>
                    )}

                    <button
                      onClick={() =>
                        onDeleteUser?.(user)
                      }
                      className="rounded border border-red-400/30 px-3 py-1 text-sm text-red-300 hover:bg-red-400/10"
                    >
                      Delete
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default AdminUserTable;