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
  user: "bg-blue-100 text-blue-700",
  admin: "bg-purple-100 text-purple-700",
  super_admin: "bg-red-100 text-red-700",
};

const statusStyles: Record<UserStatus, string> = {
  active: "bg-green-100 text-green-700",
  suspended: "bg-red-100 text-red-700",
  pending: "bg-yellow-100 text-yellow-700",
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
      <div className="rounded-lg border p-8 text-center">
        Loading users...
      </div>
    );
  }

  if (!users.length) {
    return (
      <div className="rounded-lg border p-8 text-center">
        No users found.
      </div>
    );
  }

  return (
    <div className="overflow-hidden rounded-xl border bg-white shadow-sm">
      <div className="overflow-x-auto">
        <table className="min-w-full">
          <thead className="border-b bg-gray-50">
            <tr>
              <th className="px-4 py-3 text-left text-sm font-semibold">
                User
              </th>

              <th className="px-4 py-3 text-left text-sm font-semibold">
                Role
              </th>

              <th className="px-4 py-3 text-left text-sm font-semibold">
                Status
              </th>

              <th className="px-4 py-3 text-left text-sm font-semibold">
                Documents
              </th>

              <th className="px-4 py-3 text-left text-sm font-semibold">
                Joined
              </th>

              <th className="px-4 py-3 text-left text-sm font-semibold">
                Last Login
              </th>

              <th className="px-4 py-3 text-right text-sm font-semibold">
                Actions
              </th>
            </tr>
          </thead>

          <tbody>
            {users.map((user) => (
              <tr
                key={user.id}
                className="border-b hover:bg-gray-50"
              >
                <td className="px-4 py-4">
                  <div>
                    <p className="font-medium">
                      {user.fullName}
                    </p>

                    <p className="text-sm text-gray-500">
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

                <td className="px-4 py-4">
                  {user.documentsCount}
                </td>

                <td className="px-4 py-4">
                  {formatDate(user.createdAt)}
                </td>

                <td className="px-4 py-4">
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
                      className="rounded border px-3 py-1 text-sm hover:bg-gray-100"
                    >
                      View
                    </button>

                    {user.status === "active" ? (
                      <button
                        onClick={() =>
                          onSuspendUser?.(user)
                        }
                        className="rounded border border-red-300 px-3 py-1 text-sm text-red-600 hover:bg-red-50"
                      >
                        Suspend
                      </button>
                    ) : (
                      <button
                        onClick={() =>
                          onActivateUser?.(user)
                        }
                        className="rounded border border-green-300 px-3 py-1 text-sm text-green-600 hover:bg-green-50"
                      >
                        Activate
                      </button>
                    )}

                    <button
                      onClick={() =>
                        onDeleteUser?.(user)
                      }
                      className="rounded border border-red-300 px-3 py-1 text-sm text-red-600 hover:bg-red-50"
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