import API from "./axios";

/**
 * ======================================================
 * USER MANAGEMENT API
 * ======================================================
 *
 * All requests go through the shared `API` axios instance
 * (src/api/axios.ts), so JWT auth, base URL, and error
 * logging are already handled centrally.
 *
 * Endpoint paths below follow the same convention as
 * adminApi.ts (/admin/*). Adjust the path strings to match
 * your actual backend routes if they differ.
 * ======================================================
 */

export type UserRole = "USER" | "REVIEWER" | "OFFICER" | "ADMIN";
export type UserStatus = "ACTIVE" | "PENDING" | "SUSPENDED";

export interface AdminUser {
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

export interface ExportUsersResult {
  message?: string;
  downloadUrl?: string;
}

export interface SyncUsersResult {
  message?: string;
  syncedCount?: number;
}

export interface MutationResult {
  message?: string;
}

/**
 * GET /admin/users
 * Fetches the full user list for the admin user management page.
 */
export async function getUsers(): Promise<AdminUser[]> {
  const response = await API.get<AdminUser[]>("/admin/users");
  return response.data;
}

/**
 * POST /admin/users/export
 * Kicks off a user export and returns a downloadable file URL.
 */
export async function exportUsers(): Promise<ExportUsersResult> {
  const response = await API.post<ExportUsersResult>("/admin/users/export");
  return response.data;
}

/**
 * POST /admin/users/sync
 * Triggers a sync of user records with any upstream identity provider.
 */
export async function syncUsers(): Promise<SyncUsersResult> {
  const response = await API.post<SyncUsersResult>("/admin/users/sync");
  return response.data;
}

/**
 * PATCH /admin/users/:id/suspend
 */
export async function suspendUser(userId: string): Promise<MutationResult> {
  const response = await API.patch<MutationResult>(
    `/admin/users/${userId}/suspend`
  );
  return response.data;
}

/**
 * PATCH /admin/users/:id/activate
 */
export async function activateUser(userId: string): Promise<MutationResult> {
  const response = await API.patch<MutationResult>(
    `/admin/users/${userId}/activate`
  );
  return response.data;
}

/**
 * DELETE /admin/users/:id
 */
export async function deleteUser(userId: string): Promise<MutationResult> {
  const response = await API.delete<MutationResult>(`/admin/users/${userId}`);
  return response.data;
}

export async function createUser(data: {
  fullName: string;
  email: string;
  password: string;
  role: "USER" | "ADMIN";
}) {

  const response = await API.post(
    "/admin/users",
    data
  );


  return response.data;

}