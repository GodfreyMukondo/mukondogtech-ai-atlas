import { useCallback, useEffect, useState } from "react";
import axios from "axios";
import { getUsers, type AdminUser } from "../api/userApi";

interface UseUsersResult {
  users: AdminUser[];
  loading: boolean;
  error: string | null;
  refresh: () => Promise<void>;
}

/**
 * Fetches the admin user list and exposes loading/error state plus a
 * `refresh` function so callers (e.g. after a suspend/delete/sync action)
 * can re-pull the latest data.
 */
export function useUsers(): UseUsersResult {
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchUsers = useCallback(async () => {
    try {
      setError(null);
      const data = await getUsers();
      setUsers(data);
    } catch (err: unknown) {
      console.error("[useUsers] Failed to fetch users:", err);

      const message = axios.isAxiosError(err)
        ? err.response?.data?.message ??
          `Failed to load users (${err.response?.status ?? "unknown"})`
        : "Failed to load users.";

      setError(message);
    }
  }, []);

  const refresh = useCallback(async () => {
    await fetchUsers();
  }, [fetchUsers]);

  useEffect(() => {
    let isMounted = true;

    (async () => {
      setLoading(true);
      await fetchUsers();
      if (isMounted) {
        setLoading(false);
      }
    })();

    return () => {
      isMounted = false;
    };
  }, [fetchUsers]);

  return { users, loading, error, refresh };
}