/**
 * ============================================================================
 * CENTRAL API CLIENT
 * ============================================================================
 *
 * Axios client used by the React frontend to communicate with the
 * Spring Boot backend.
 *
 * Responsibilities:
 * - Centralize API base URL configuration
 * - Attach JWT authentication automatically
 * - Apply request timeout
 * - Provide consistent JSON headers
 * - Handle authentication failures
 * - Handle authorization failures
 * - Avoid duplicating Axios configuration across pages
 * - Provide development diagnostics
 *
 * Authentication architecture:
 *
 * React
 *   |
 *   | Authorization: Bearer <JWT>
 *   v
 * Axios
 *   |
 *   v
 * Spring Boot
 *   |
 *   v
 * JwtAuthFilter
 *
 * ============================================================================
 */

import axios from "axios";

import type {
  AxiosError,
  InternalAxiosRequestConfig,
} from "axios";

/* ============================================================================
 * ENVIRONMENT CONFIGURATION
 * ========================================================================== */

/**
 * Main Spring Boot API URL.
 *
 * Development:
 *
 *     VITE_API_URL=http://localhost:8080/api
 *
 * Production:
 *
 *     VITE_API_URL=https://your-production-api-domain/api
 */
const API_BASE_URL =
  import.meta.env.VITE_API_URL?.trim() ||
  "http://localhost:8080/api";

/**
 * API request timeout.
 *
 * Defaults to 30 seconds when VITE_API_TIMEOUT is not configured.
 */
const API_TIMEOUT =
  Number(
    import.meta.env.VITE_API_TIMEOUT
  ) || 30000;

/**
 * Frontend debug mode.
 */
const ENABLE_DEBUG =
  String(
    import.meta.env.VITE_ENABLE_DEBUG
  ).toLowerCase() === "true";

/* ============================================================================
 * CONFIGURATION VALIDATION
 * ========================================================================== */

if (!API_BASE_URL) {
  console.error(
    "[API] VITE_API_URL is not configured."
  );
}

if (
  !Number.isFinite(API_TIMEOUT) ||
  API_TIMEOUT <= 0
) {
  console.warn(
    "[API] Invalid VITE_API_TIMEOUT. Falling back to 30000ms."
  );
}

/* ============================================================================
 * AXIOS INSTANCE
 * ========================================================================== */

const api = axios.create({

  /*
   * Central backend URL.
   *
   * Example:
   *
   * VITE_API_URL=http://localhost:8080/api
   *
   * api.get("/profile/me")
   *
   * becomes:
   *
   * http://localhost:8080/api/profile/me
   */
  baseURL: API_BASE_URL,

  /*
   * Prevent requests from hanging indefinitely.
   */
  timeout:
    Number.isFinite(API_TIMEOUT) &&
    API_TIMEOUT > 0
      ? API_TIMEOUT
      : 30000,

  /*
   * Default headers.
   *
   * Individual requests such as multipart/form-data uploads can override
   * Content-Type when necessary.
   */
  headers: {
    Accept:
      "application/json",

    "Content-Type":
      "application/json",
  },

  /*
   * JWT authentication is sent through:
   *
   * Authorization: Bearer <JWT>
   *
   * Therefore browser cookies are not required by this client.
   */
  withCredentials: false,
});

/* ============================================================================
 * TOKEN MANAGEMENT
 * ========================================================================== */

/**
 * Retrieve the currently stored JWT.
 *
 * The existing authentication architecture may use either:
 *
 *     token
 *
 * or:
 *
 *     accessToken
 *
 * Both are supported to avoid breaking existing authentication code.
 */
function getAccessToken(): string | null {

  try {

    const token =
      localStorage.getItem(
        "token"
      );

    if (token) {
      return token;
    }

    const accessToken =
      localStorage.getItem(
        "accessToken"
      );

    if (accessToken) {
      return accessToken;
    }

    return null;

  } catch (error) {

    if (ENABLE_DEBUG) {
      console.warn(
        "[API] Unable to access localStorage:",
        error
      );
    }

    return null;
  }
}

/**
 * Clear authentication tokens.
 *
 * This does not perform navigation.
 *
 * Authentication state remains the responsibility of AuthContext and
 * ProtectedRoute.
 */
function clearAuthenticationTokens(): void {

  try {

    localStorage.removeItem(
      "token"
    );

    localStorage.removeItem(
      "accessToken"
    );

  } catch (error) {

    if (ENABLE_DEBUG) {
      console.warn(
        "[API] Unable to clear authentication tokens:",
        error
      );
    }
  }
}

/* ============================================================================
 * REQUEST INTERCEPTOR
 * ========================================================================== */

api.interceptors.request.use(

  (
    config: InternalAxiosRequestConfig
  ) => {

    /*
     * ========================================================================
     * JWT AUTHENTICATION
     * ========================================================================
     */

    const token =
      getAccessToken();

    if (token) {

      config.headers.Authorization =
        `Bearer ${token}`;
    }

    /*
     * ========================================================================
     * ACCEPT HEADER
     * ========================================================================
     */

    config.headers.Accept =
      "application/json";

    /*
     * ========================================================================
     * DEBUG LOGGING
     * ========================================================================
     */

    if (ENABLE_DEBUG) {

      console.debug(
        "[API REQUEST]",
        {
          method:
            config.method?.toUpperCase(),

          url:
            config.url,

          baseURL:
            config.baseURL,
        }
      );
    }

    return config;
  },

  (error) => {

    if (ENABLE_DEBUG) {

      console.error(
        "[API REQUEST ERROR]",
        error
      );
    }

    return Promise.reject(
      error
    );
  }
);

/* ============================================================================
 * RESPONSE INTERCEPTOR
 * ========================================================================== */

api.interceptors.response.use(

  /*
   * ========================================================================
   * SUCCESS
   * ========================================================================
   */

  (response) => {

    if (ENABLE_DEBUG) {

      console.debug(
        "[API RESPONSE]",
        {
          status:
            response.status,

          url:
            response.config.url,
        }
      );
    }

    return response;
  },

  /*
   * ========================================================================
   * ERROR
   * ========================================================================
   */

  async (
    error: AxiosError
  ) => {

    const status =
      error.response?.status;

    /*
     * ======================================================================
     * DEBUG ERROR LOGGING
     * ======================================================================
     */

    if (ENABLE_DEBUG) {

      console.error(
        "[API RESPONSE ERROR]",
        {
          status,

          url:
            error.config?.url,

          message:
            error.message,
        }
      );
    }

    /*
     * ======================================================================
     * 401 UNAUTHORIZED
     * ======================================================================
     *
     * The JWT is missing, expired or invalid.
     *
     * We notify AuthContext instead of navigating directly.
     *
     * This prevents the API layer from becoming coupled to React Router.
     */

    if (
      status === 401
    ) {

      clearAuthenticationTokens();

      window.dispatchEvent(
        new Event(
          "auth:unauthorized"
        )
      );

      /*
       * Existing ProfilePage / AuthContext code may already listen for:
       *
       *     auth:logout
       *
       * Keep this event available for the existing authentication
       * architecture.
       */

      window.dispatchEvent(
        new Event(
          "auth:logout"
        )
      );
    }

    /*
     * ======================================================================
     * 403 FORBIDDEN
     * ======================================================================
     *
     * The user is authenticated but does not have sufficient permission.
     *
     * IMPORTANT:
     *
     * Do NOT log the user out on 403.
     */

    if (
      status === 403
    ) {

      window.dispatchEvent(
        new Event(
          "auth:forbidden"
        )
      );
    }

    /*
     * ======================================================================
     * NETWORK ERROR
     * ======================================================================
     */

    if (
      !error.response &&
      ENABLE_DEBUG
    ) {

      console.error(
        "[API] Network/server connection failure.",
        {
          message:
            error.message,

          url:
            error.config?.url,
        }
      );
    }

    return Promise.reject(
      error
    );
  }
);

/* ============================================================================
 * EXPORT
 * ========================================================================== */

export default api;