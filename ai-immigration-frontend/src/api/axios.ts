import axios, {
  type AxiosError,
  type InternalAxiosRequestConfig,
} from "axios";

import { appConfig } from "../config/appConfig";

/**
 * ============================================================
 * CENTRAL AXIOS API CLIENT
 * ============================================================
 *
 * Responsibilities:
 * - Central API configuration
 * - JWT authentication
 * - Request URL normalization
 * - Request timeout configuration
 * - Centralized error handling
 * - Development diagnostics
 *
 * Authentication:
 * - JWT stored in localStorage["token"]
 * - Authorization: Bearer <token>
 * - Cookies are not used
 * - withCredentials remains false
 * ============================================================
 */

/**
 * Normalize the configured API base URL.
 *
 * Example:
 * http://localhost:8080/api/
 *
 * becomes:
 * http://localhost:8080/api
 */
function normalizeBaseURL(value: string): string {
  const baseURL = value?.trim() ?? "";

  if (!baseURL) {
    return "";
  }

  return baseURL.replace(/\/+$/, "");
}

const API_BASE_URL = normalizeBaseURL(appConfig.api.baseURL);

/**
 * ============================================================
 * AXIOS INSTANCE
 * ============================================================
 */

const API = axios.create({
  baseURL: API_BASE_URL,

  timeout: appConfig.api.timeout,

  withCredentials: false,

  headers: {
    Accept: "application/json",
    "Content-Type": "application/json",
  },
});

/**
 * ============================================================
 * PUBLIC AUTH ENDPOINTS
 * ============================================================
 */

const publicAuthEndpoints = [
  "/auth/register",
  "/auth/login",
  "/auth/verify-email",
  "/auth/forgot-password",
  "/auth/reset-password",
] as const;

/**
 * ============================================================
 * REQUEST URL NORMALIZATION
 * ============================================================
 *
 * Prevents:
 *
 * http://localhost:8080/api/api/chat
 *
 * when baseURL already contains /api.
 *
 * API.post("/chat")
 * becomes:
 *
 * http://localhost:8080/api/chat
 */
function normalizeRequestURL(
  url?: string,
): string | undefined {
  if (typeof url !== "string") {
    return url;
  }

  if (!url.startsWith("/api/")) {
    return url;
  }

  if (
    API_BASE_URL
      .toLowerCase()
      .endsWith("/api")
  ) {
    return url.substring("/api".length);
  }

  return url;
}

/**
 * ============================================================
 * JWT TOKEN
 * ============================================================
 */

function getStoredToken(): string | null {
  try {
    const token = localStorage.getItem("token");

    if (!token) {
      return null;
    }

    const normalizedToken = token.trim();

    return normalizedToken || null;
  } catch (error) {
    if (import.meta.env.DEV) {
      console.warn(
        "Unable to access localStorage while reading authentication token.",
        error,
      );
    }

    return null;
  }
}

/**
 * ============================================================
 * PUBLIC ENDPOINT CHECK
 * ============================================================
 */

function isPublicAuthEndpoint(
  requestURL: string,
): boolean {
  const pathname =
    requestURL.split("?")[0];

  return publicAuthEndpoints.some(
    (endpoint) =>
      pathname === endpoint ||
      pathname.startsWith(`${endpoint}/`),
  );
}

/**
 * ============================================================
 * DEBUG URL
 * ============================================================
 */

function buildDebugURL(
  config: InternalAxiosRequestConfig,
): string {
  const baseURL = config.baseURL ?? "";
  const url = config.url ?? "";

  if (!baseURL) {
    return url;
  }

  return `${baseURL.replace(
    /\/+$/,
    "",
  )}/${url.replace(
    /^\/+/,
    "",
  )}`;
}

/**
 * ============================================================
 * REQUEST INTERCEPTOR
 * ============================================================
 */

API.interceptors.request.use(
  (config) => {
    /**
     * Normalize:
     *
     * /api/chat
     *
     * to:
     *
     * /chat
     *
     * when baseURL already contains /api.
     */
    config.url = normalizeRequestURL(
      config.url,
    );

    const requestURL =
      config.url ?? "";

    const isPublicEndpoint =
      isPublicAuthEndpoint(
        requestURL,
      );

    /**
     * Add JWT to protected requests.
     */
    if (!isPublicEndpoint) {
      const token =
        getStoredToken();

      if (token && config.headers) {
        config.headers.set(
          "Authorization",
          `Bearer ${token}`,
        );
      }
    }

    /**
     * Development diagnostics.
     *
     * NEVER log the JWT itself.
     */
    if (import.meta.env.DEV) {
      console.log(
        "API REQUEST",
        {
          method:
            config.method?.toUpperCase(),

          baseURL:
            config.baseURL,

          url:
            config.url,

          fullURL:
            buildDebugURL(config),

          authorization:
            Boolean(
              config.headers?.Authorization,
            ),

          timeout:
            config.timeout,
        },
      );
    }

    return config;
  },
  (error) =>
    Promise.reject(error),
);

/**
 * ============================================================
 * RESPONSE INTERCEPTOR
 * ============================================================
 */

API.interceptors.response.use(
  (response) => response,

  (error: AxiosError) => {
    /**
     * Cancellation is expected during:
     * - component unmount
     * - route change
     * - request replacement
     */
    if (
      isAxiosCancellation(error)
    ) {
      return Promise.reject(error);
    }

    const config =
      error.config;

    const url =
      config?.url;

    const method =
      config?.method?.toUpperCase();

    /**
     * ========================================================
     * SERVER RESPONSE
     * ========================================================
     */

    if (error.response) {
      const status =
        error.response.status;

      if (status === 400) {
        console.warn(
          "API bad request",
          {
            url,
            method,
            status,
            response:
              error.response.data,
          },
        );
      }

      if (status === 401) {
        console.warn(
          "Unauthorized API request",
          {
            url,
            method,
            status,
          },
        );

        /**
         * Do NOT automatically clear localStorage.
         *
         * AuthContext should handle authentication
         * lifecycle and logout.
         */
      }

      if (status === 403) {
        console.warn(
          "Forbidden API request",
          {
            url,
            method,
            status,
            response:
              error.response.data,
          },
        );
      }

      if (status === 404) {
        console.warn(
          "API endpoint not found",
          {
            url,
            method,
            status,
          },
        );
      }

      if (status >= 500) {
        console.error(
          "API server error",
          {
            url,
            method,
            status,
            response:
              error.response.data,
          },
        );
      }
    }

    /**
     * ========================================================
     * NETWORK ERROR
     * ========================================================
     *
     * Example:
     *
     * ERR_CONNECTION_REFUSED
     *
     * This means the browser could not establish
     * a connection with the backend.
     */
    else if (error.request) {
      console.error(
        "API network error",
        {
          url,
          method,
          message:
            error.message,

          baseURL:
            config?.baseURL,

          fullURL:
            config
              ? buildDebugURL(config)
              : undefined,
        },
      );
    }

    /**
     * ========================================================
     * UNKNOWN CLIENT ERROR
     * ========================================================
     */

    else {
      console.error(
        "API request configuration error",
        {
          message:
            error.message,
        },
      );
    }

    return Promise.reject(error);
  },
);

/**
 * ============================================================
 * AXIOS CANCELLATION
 * ============================================================
 */

function isAxiosCancellation(
  error: AxiosError,
): boolean {
  return (
    error.code ===
      "ERR_CANCELED" ||
    error.name ===
      "CanceledError"
  );
}

/**
 * ============================================================
 * EXPORT
 * ============================================================
 */

export default API;