import { useCallback, useEffect, useState } from "react";
import axios from "axios";
import {
  getUsers,
  getUserStatistics,
  type AdminUser,
  type GetUsersParams,
  type UserStatistics,
} from "../api/userApi";

interface UseUsersOptions {
  search: string;
  role: GetUsersParams["role"] | "ALL";
  status: GetUsersParams["status"] | "ALL";
  page: number;
  size: number;
}

interface UseUsersResult {
  users: AdminUser[];
  totalElements: number;
  totalPages: number;
  statistics: UserStatistics | null;
  loading: boolean;
  error: string | null;
  refresh: () => Promise<void>;
}

const DEFAULT_STATISTICS: UserStatistics = {
  totalUsers: 0,
  activeUsers: 0,
  disabledUsers: 0,
  admins: 0,
};

function toMessage(err: unknown, fallback: string): string {
  if (axios.isAxiosError(err)) {
    return (
      err.response?.data?.message ??
      `${fallback} (${err.response?.status ?? "unknown"})`
    );
  }

  return fallback;
}

/**
 * Fetches a page of the admin user list, filtered and paginated on the
 * backend (the underlying dataset is not assumed to fit client-side), plus
 * platform-wide user statistics.
 *
 * `refresh` re-pulls both the current page and the statistics, so callers
 * (e.g. after a suspend/delete/sync action) always see up-to-date counts
 * even though the affected row may have moved to a different page.
 */
export function useUsers(options: UseUsersOptions): UseUsersResult {
  const { search, role, status, page, size } = options;

  const [users, setUsers] = useState<AdminUser[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [statistics, setStatistics] = useState<UserStatistics | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchAll = useCallback(async () => {
    try {
      setError(null);

      const [usersPage, userStatistics] = await Promise.all([
        getUsers({
          search,
          role: role === "ALL" ? undefined : role,
          status: status === "ALL" ? undefined : status,
          page,
          size,
        }),
        getUserStatistics().catch((err: unknown) => {
          console.error("[useUsers] Failed to fetch statistics:", err);
          return DEFAULT_STATISTICS;
        }),
      ]);

      setUsers(usersPage.content);
      setTotalElements(usersPage.totalElements);
      setTotalPages(usersPage.totalPages);
      setStatistics(userStatistics);
    } catch (err: unknown) {
      console.error("[useUsers] Failed to fetch users:", err);
      setError(toMessage(err, "Failed to load users."));
    }
  }, [search, role, status, page, size]);

  const refresh = useCallback(async () => {
    await fetchAll();
  }, [fetchAll]);

  useEffect(() => {
    let isMounted = true;

    (async () => {
      setLoading(true);
      await fetchAll();
      if (isMounted) {
        setLoading(false);
      }
    })();

    return () => {
      isMounted = false;
    };
  }, [fetchAll]);

  return {
    users,
    totalElements,
    totalPages,
    statistics,
    loading,
    error,
    refresh,
  };
}
