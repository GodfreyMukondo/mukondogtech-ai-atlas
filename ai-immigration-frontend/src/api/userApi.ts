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
 * Backend contract (UserController):
 *
 *   GET    /api/admin/users?search=&role=&status=&page=&size=
 *   GET    /api/admin/users/statistics
 *   GET    /api/admin/users/export
 *   GET    /api/admin/users/{id}
 *   POST   /api/admin/users
 *   PUT    /api/admin/users/{id}
 *   PATCH  /api/admin/users/{id}/activate
 *   PATCH  /api/admin/users/{id}/suspend
 *   DELETE /api/admin/users/{id}
 *
 * IMPORTANT:
 *
 * GET /admin/users returns a Spring Data `Page<UserManagementResponse>`
 * envelope ({ content, totalElements, totalPages, number, size, ... }),
 * NOT a bare array. getUsers() below returns that page as-is; callers
 * must read `.content` for the row data and `.totalElements` for the
 * real total (the current page's length is not the total user count).
 * ======================================================
 */

export type UserRole = "USER" | "ADMIN";
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

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface GetUsersParams {
  search?: string;
  role?: UserRole;
  status?: UserStatus;
  page?: number;
  size?: number;
}

export interface UserStatistics {
  totalUsers: number;
  activeUsers: number;
  disabledUsers: number;
  admins: number;
}

export interface MutationResult {
  message?: string;
}

export interface UpdateUserData {
  fullName?: string;
  email?: string;
  password?: string;
  phone?: string;
  country?: string;
  role?: UserRole;
}

type UnknownRecord = Record<string, unknown>;

/**
 * The backend never omits any of these fields, but the response is
 * normalized defensively rather than trusted blindly, since it crosses
 * a network boundary.
 */
function normalizeUser(value: unknown): AdminUser {
  const record = (value ?? {}) as UnknownRecord;

  return {
    id: String(record.id ?? ""),
    name: typeof record.name === "string" ? record.name : "Unnamed user",
    email: typeof record.email === "string" ? record.email : "",
    phone: typeof record.phone === "string" ? record.phone : "",
    country: typeof record.country === "string" ? record.country : "",
    role: record.role === "ADMIN" ? "ADMIN" : "USER",
    status:
      record.status === "SUSPENDED" || record.status === "PENDING"
        ? record.status
        : "ACTIVE",
    applications:
      typeof record.applications === "number" ? record.applications : 0,
    joined: typeof record.joined === "string" ? record.joined : "",
  };
}

/**
 * GET /admin/users
 * Fetches a page of users for the admin user management screen.
 *
 * Filtering (search/role/status) and pagination are both performed by
 * the backend, since the underlying dataset is not assumed to fit in a
 * single page.
 */
export async function getUsers(
  params: GetUsersParams = {},
): Promise<PageResponse<AdminUser>> {
  const response = await API.get<PageResponse<unknown>>("/admin/users", {
    params: {
      search: params.search?.trim() || undefined,
      role: params.role,
      status: params.status,
      page: params.page ?? 0,
      size: params.size ?? 20,
    },
  });

  const page = response.data;

  return {
    content: Array.isArray(page?.content)
      ? page.content.map(normalizeUser)
      : [],
    totalElements:
      typeof page?.totalElements === "number" ? page.totalElements : 0,
    totalPages: typeof page?.totalPages === "number" ? page.totalPages : 0,
    number: typeof page?.number === "number" ? page.number : 0,
    size: typeof page?.size === "number" ? page.size : params.size ?? 20,
  };
}

/**
 * GET /admin/users/statistics
 * Aggregate counts across ALL users, not just the current page.
 */
export async function getUserStatistics(): Promise<UserStatistics> {
  const response = await API.get<UserStatistics>("/admin/users/statistics");
  return response.data;
}

/**
 * GET /admin/users/export
 * Downloads every user as a CSV file and saves it to the browser's
 * downloads folder.
 */
export async function exportUsers(): Promise<void> {
  /**
   * The shared `API` instance defaults every request's Accept header to
   * "application/json" (axios.ts). This endpoint's @GetMapping declares
   * produces = "text/csv", so without overriding Accept here, Spring's
   * content negotiation rejects the request with
   * HttpMediaTypeNotAcceptableException before the controller method ever
   * runs - which the global exception handler's catch-all then reports as
   * a generic 500 instead of the real 406 cause.
   */
  const response = await API.get("/admin/users/export", {
    responseType: "blob",
    headers: {
      Accept: "text/csv",
    },
  });

  const disposition = response.headers?.["content-disposition"] as
    | string
    | undefined;

  const fileNameMatch = disposition?.match(/filename="?([^"]+)"?/);

  const fileName = fileNameMatch?.[1] ?? "users-export.csv";

  const blobUrl = URL.createObjectURL(response.data as Blob);

  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();

  URL.revokeObjectURL(blobUrl);
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

/**
 * GET /admin/users/:id
 */
export async function getUserById(userId: string): Promise<AdminUser> {
  const response = await API.get(`/admin/users/${userId}`);
  return normalizeUser(response.data);
}

/**
 * PUT /admin/users/:id
 */
export async function updateUser(
  userId: string,
  data: UpdateUserData,
): Promise<AdminUser> {
  const payload: UpdateUserData = { ...data };

  if (!payload.password) {
    delete payload.password;
  }

  const response = await API.put(`/admin/users/${userId}`, payload);
  return normalizeUser(response.data);
}

export async function createUser(data: {
  fullName: string;
  email: string;
  password: string;
  role: UserRole;
  phone?: string;
  country?: string;
}): Promise<AdminUser> {

  const response = await API.post(
    "/admin/users",
    data
  );


  return normalizeUser(response.data);

}
