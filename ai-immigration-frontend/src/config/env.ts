
/**
 * ============================================================================
 * ENVIRONMENT CONFIGURATION
 * ============================================================================
 *
 * Centralized access to Vite environment variables.
 *
 * IMPORTANT:
 *
 * Never access:
 *
 *   import.meta.env.VITE_*
 *
 * directly inside components, hooks, services, or pages.
 *
 * Use:
 *
 *   import env from "@/config/env";
 *
 * This keeps environment configuration:
 *
 *   - centralized
 *   - validated
 *   - type-safe
 *   - production-safe
 *   - environment-aware
 *
 * ============================================================================
 *
 * SECURITY
 * ============================================================================
 *
 * Vite exposes VITE_* variables to the browser.
 *
 * NEVER store secrets in VITE_* variables.
 *
 * NEVER put the following here:
 *
 *   - JWT secrets
 *   - database passwords
 *   - AWS secret access keys
 *   - private API keys
 *   - SMTP passwords
 *   - encryption keys
 *   - OAuth client secrets
 *   - service-account credentials
 *
 * AI monitoring credentials and sensitive monitoring data must be handled
 * by the backend.
 *
 * ============================================================================
 */

/**
 * ============================================================================
 * SUPPORTED ENVIRONMENTS
 * ============================================================================
 */

export type EnvironmentName =
  | "development"
  | "production"
  | "test";

/**
 * ============================================================================
 * ENVIRONMENT CONFIGURATION INTERFACE
 * ============================================================================
 */

export interface EnvironmentConfig {
  /**
   * Current application environment.
   */
  NODE_ENV: EnvironmentName;

  /**
   * Main backend API URL.
   *
   * Example:
   *
   * https://api.example.com/api
   */
  API_URL: string;

  /**
   * Application display name.
   */
  APP_NAME: string;

  /**
   * Application version.
   */
  APP_VERSION: string;

  /**
   * Enables application analytics.
   */
  ENABLE_ANALYTICS: boolean;

  /**
   * Enables frontend debugging.
   *
   * Should normally be false in production.
   */
  ENABLE_DEBUG: boolean;

  /**
   * Enables the AI monitoring frontend feature.
   *
   * This is only a frontend feature flag.
   *
   * Actual monitoring must be performed by the backend/infrastructure.
   */
  AI_MONITORING_ENABLED: boolean;

  /**
   * AI monitoring API URL.
   *
   * If a dedicated monitoring API URL is not supplied,
   * the main API URL is used.
   */
  AI_MONITORING_API_URL: string;

  /**
   * Enables automatic polling of AI monitoring metrics.
   */
  AI_MONITORING_AUTO_REFRESH: boolean;

  /**
   * Interval between AI monitoring metric refreshes.
   *
   * Unit:
   * milliseconds
   */
  AI_MONITORING_REFRESH_INTERVAL_MS: number;

  /**
   * Maximum time allowed for an AI monitoring API request.
   *
   * Unit:
   * milliseconds
   */
  AI_MONITORING_REQUEST_TIMEOUT_MS: number;
}

/**
 * ============================================================================
 * VITE ENVIRONMENT TYPES
 * ============================================================================
 *
 * These declarations allow TypeScript to understand the environment variables
 * used by this application.
 */

interface ImportMetaEnv {
  readonly MODE: string;
  readonly DEV: boolean;
  readonly PROD: boolean;

  readonly VITE_API_URL?: string;
  readonly VITE_APP_NAME?: string;
  readonly VITE_APP_VERSION?: string;

  readonly VITE_ENABLE_ANALYTICS?: string;
  readonly VITE_ENABLE_DEBUG?: string;

  readonly VITE_AI_MONITORING_ENABLED?: string;
  readonly VITE_AI_MONITORING_API_URL?: string;
  readonly VITE_AI_MONITORING_AUTO_REFRESH?: string;
  readonly VITE_AI_MONITORING_REFRESH_INTERVAL_MS?: string;
  readonly VITE_AI_MONITORING_REQUEST_TIMEOUT_MS?: string;
}

/**
 * ============================================================================
 * ENVIRONMENT VARIABLE HELPERS
 * ============================================================================
 */

/**
 * Retrieves a required environment variable.
 *
 * There is intentionally NO localhost fallback.
 *
 * This prevents accidentally deploying a production frontend that silently
 * communicates with localhost.
 */
function requireEnv(key: string): string {
  const value =
    import.meta.env[
      key as keyof ImportMetaEnv
    ];

  if (
    value === undefined ||
    value === null ||
    String(value).trim() === ""
  ) {
    throw new Error(
      `Missing required environment variable: ${key}`,
    );
  }

  return String(value).trim();
}

/**
 * Retrieves an optional environment variable.
 *
 * Returns undefined when the variable is not configured.
 */
function optionalEnv(
  key: string,
): string | undefined {
  const value =
    import.meta.env[
      key as keyof ImportMetaEnv
    ];

  if (
    value === undefined ||
    value === null ||
    String(value).trim() === ""
  ) {
    return undefined;
  }

  return String(value).trim();
}

/**
 * Parses a boolean environment variable.
 *
 * Supported true values:
 *
 *   - true
 *   - 1
 *   - yes
 *   - on
 *
 * Supported false values:
 *
 *   - false
 *   - 0
 *   - no
 *   - off
 */
function booleanEnv(
  value: string | undefined,
  defaultValue = false,
): boolean {
  if (
    value === undefined ||
    value === null ||
    value.trim() === ""
  ) {
    return defaultValue;
  }

  const normalizedValue =
    value.trim().toLowerCase();

  if (
    normalizedValue === "true" ||
    normalizedValue === "1" ||
    normalizedValue === "yes" ||
    normalizedValue === "on"
  ) {
    return true;
  }

  if (
    normalizedValue === "false" ||
    normalizedValue === "0" ||
    normalizedValue === "no" ||
    normalizedValue === "off"
  ) {
    return false;
  }

  throw new Error(
    `Invalid boolean environment value: "${value}". ` +
      `Expected true, false, 1, 0, yes, no, on, or off.`,
  );
}

/**
 * Parses a positive integer environment variable.
 */
function positiveIntegerEnv(
  key: string,
  defaultValue?: number,
): number {
  const rawValue = optionalEnv(key);

  if (
    rawValue === undefined ||
    rawValue === ""
  ) {
    if (defaultValue !== undefined) {
      return defaultValue;
    }

    throw new Error(
      `Missing required environment variable: ${key}`,
    );
  }

  const parsedValue = Number(rawValue);

  if (
    !Number.isInteger(parsedValue) ||
    parsedValue <= 0
  ) {
    throw new Error(
      `Invalid environment variable "${key}": "${rawValue}". ` +
        `Expected a positive integer.`,
    );
  }

  return parsedValue;
}

/**
 * ============================================================================
 * ENVIRONMENT VALIDATION
 * ============================================================================
 */

/**
 * Resolves the current application environment.
 */
function getEnvironmentName(): EnvironmentName {
  const mode = String(
    import.meta.env.MODE ?? "",
  ).trim();

  if (
    mode === "development" ||
    mode === "production" ||
    mode === "test"
  ) {
    return mode;
  }

  throw new Error(
    `Unsupported application environment: "${mode}". ` +
      `Expected development, production, or test.`,
  );
}

/**
 * Validates an HTTP/HTTPS URL.
 *
 * Trailing slashes are removed so API clients do not accidentally create
 * URLs such as:
 *
 * https://api.example.com/api//users
 */
function validateUrl(
  value: string,
  variableName: string,
): string {
  const normalizedUrl = value
    .trim()
    .replace(/\/+$/, "");

  let parsedUrl: URL;

  try {
    parsedUrl = new URL(normalizedUrl);
  } catch {
    throw new Error(
      `Invalid ${variableName}: "${value}". ` +
        `Expected a valid HTTP or HTTPS URL.`,
    );
  }

  if (
    parsedUrl.protocol !== "http:" &&
    parsedUrl.protocol !== "https:"
  ) {
    throw new Error(
      `${variableName} must use HTTP or HTTPS.`,
    );
  }

  /**
   * Production applications must communicate with APIs over HTTPS.
   *
   * This check intentionally allows HTTP in development and test.
   */
  if (
    import.meta.env.MODE === "production" &&
    parsedUrl.protocol !== "https:"
  ) {
    throw new Error(
      `${variableName} must use HTTPS in production.`,
    );
  }

  return normalizedUrl;
}

/**
 * Validates the main API URL.
 */
function validateApiUrl(
  value: string,
): string {
  return validateUrl(
    value,
    "VITE_API_URL",
  );
}

/**
 * Validates the AI monitoring API URL.
 */
function validateAiMonitoringApiUrl(
  value: string,
): string {
  return validateUrl(
    value,
    "VITE_AI_MONITORING_API_URL",
  );
}

/**
 * Validates application name.
 */
function validateAppName(
  value: string,
): string {
  const appName = value.trim();

  if (!appName) {
    throw new Error(
      "VITE_APP_NAME cannot be empty.",
    );
  }

  return appName;
}

/**
 * Validates semantic application version.
 *
 * Examples:
 *
 *   1.0.0
 *   1.2.3
 *   2.0.0-beta.1
 *   2.0.0+build.10
 */
function validateAppVersion(
  value: string,
): string {
  const version = value.trim();

  const semanticVersionPattern =
    /^\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?(?:\+[0-9A-Za-z.-]+)?$/;

  if (
    !semanticVersionPattern.test(version)
  ) {
    throw new Error(
      `Invalid VITE_APP_VERSION: "${value}". ` +
        `Expected a semantic version such as 1.0.0.`,
    );
  }

  return version;
}

/**
 * Validates AI monitoring refresh interval.
 *
 * Prevents accidental aggressive polling.
 */
function validateMonitoringRefreshInterval(
  value: number,
): number {
  const minimumInterval = 5_000;

  if (value < minimumInterval) {
    throw new Error(
      "VITE_AI_MONITORING_REFRESH_INTERVAL_MS " +
        `must be at least ${minimumInterval} milliseconds.`,
    );
  }

  return value;
}

/**
 * Validates monitoring request timeout.
 */
function validateMonitoringRequestTimeout(
  value: number,
): number {
  const minimumTimeout = 1_000;

  if (value < minimumTimeout) {
    throw new Error(
      "VITE_AI_MONITORING_REQUEST_TIMEOUT_MS " +
        `must be at least ${minimumTimeout} milliseconds.`,
    );
  }

  return value;
}

/**
 * ============================================================================
 * ENVIRONMENT CONFIGURATION
 * ============================================================================
 *
 * Everything consumed by the frontend is resolved here.
 */

const NODE_ENV =
  getEnvironmentName();

/**
 * Main backend API.
 *
 * This is deliberately required.
 *
 * There is NO hardcoded localhost fallback.
 */
const API_URL =
  validateApiUrl(
    requireEnv("VITE_API_URL"),
  );

/**
 * AI monitoring feature flag.
 */
const AI_MONITORING_ENABLED =
  booleanEnv(
    optionalEnv(
      "VITE_AI_MONITORING_ENABLED",
    ),
    true,
  );

/**
 * AI monitoring API.
 *
 * A dedicated monitoring URL is optional.
 *
 * If it is not supplied, the existing backend API URL
 * is reused.
 *
 * This avoids duplicating configuration when AI monitoring
 * is served by the same backend.
 */
const configuredAiMonitoringApiUrl =
  optionalEnv(
    "VITE_AI_MONITORING_API_URL",
  );

const AI_MONITORING_API_URL =
  configuredAiMonitoringApiUrl
    ? validateAiMonitoringApiUrl(
        configuredAiMonitoringApiUrl,
      )
    : API_URL;

/**
 * Automatic monitoring refresh.
 */
const AI_MONITORING_AUTO_REFRESH =
  booleanEnv(
    optionalEnv(
      "VITE_AI_MONITORING_AUTO_REFRESH",
    ),
    true,
  );

/**
 * Monitoring polling interval.
 *
 * The default is intentionally configuration-safe rather than a URL,
 * credential, hostname, or other deployment-specific value.
 */
const AI_MONITORING_REFRESH_INTERVAL_MS =
  validateMonitoringRefreshInterval(
    positiveIntegerEnv(
      "VITE_AI_MONITORING_REFRESH_INTERVAL_MS",
      30_000,
    ),
  );

/**
 * Monitoring request timeout.
 */
const AI_MONITORING_REQUEST_TIMEOUT_MS =
  validateMonitoringRequestTimeout(
    positiveIntegerEnv(
      "VITE_AI_MONITORING_REQUEST_TIMEOUT_MS",
      10_000,
    ),
  );

/**
 * ============================================================================
 * FINAL CONFIGURATION
 * ============================================================================
 */

export const env: EnvironmentConfig = {
  NODE_ENV,

  API_URL,

  APP_NAME: validateAppName(
    requireEnv("VITE_APP_NAME"),
  ),

  APP_VERSION:
    validateAppVersion(
      requireEnv("VITE_APP_VERSION"),
    ),

  ENABLE_ANALYTICS:
    booleanEnv(
      optionalEnv(
        "VITE_ENABLE_ANALYTICS",
      ),
      false,
    ),

  ENABLE_DEBUG:
    booleanEnv(
      optionalEnv(
        "VITE_ENABLE_DEBUG",
      ),
      false,
    ),

  AI_MONITORING_ENABLED,

  AI_MONITORING_API_URL,

  AI_MONITORING_AUTO_REFRESH,

  AI_MONITORING_REFRESH_INTERVAL_MS,

  AI_MONITORING_REQUEST_TIMEOUT_MS,
};

/**
 * ============================================================================
 * DEVELOPMENT SAFETY
 * ============================================================================
 *
 * Debugging should normally be disabled in production.
 */

if (
  env.NODE_ENV === "production" &&
  env.ENABLE_DEBUG
) {
  console.warn(
    `[${env.APP_NAME}] Debug mode is enabled in production.`,
  );
}

/**
 * ============================================================================
 * DEVELOPMENT INFORMATION
 * ============================================================================
 *
 * Never print secrets or sensitive environment values.
 *
 * Only non-sensitive configuration metadata is logged.
 */

if (
  env.NODE_ENV === "development" &&
  env.ENABLE_DEBUG
) {
  console.info(
    `[${env.APP_NAME}] Environment configuration loaded.`,
  );

  console.info({
    environment: env.NODE_ENV,
    apiConfigured: Boolean(
      env.API_URL,
    ),
    aiMonitoringEnabled:
      env.AI_MONITORING_ENABLED,
    aiMonitoringAutoRefresh:
      env.AI_MONITORING_AUTO_REFRESH,
    aiMonitoringRefreshInterval:
      env.AI_MONITORING_REFRESH_INTERVAL_MS,
  });
}

/**
 * ============================================================================
 * DEFAULT EXPORT
 * ============================================================================
 */

export default env;
