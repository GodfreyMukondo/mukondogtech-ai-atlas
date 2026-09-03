import axios, {
  type AxiosError,
} from "axios";


/**
 * ============================================================================
 * APPLICATION ERROR SERVICE
 * ============================================================================
 *
 * Responsibilities:
 *
 * - Normalize unknown errors
 * - Extract safe HTTP information
 * - Provide user-friendly messages
 * - Log development diagnostics
 * - Prepare future monitoring integration
 *
 * ============================================================================
 */


export interface AppError {

  message: string;

  code?: string;

  status?: number;

  details?: unknown;

  timestamp: string;

  stack?: string;
}


interface BackendErrorResponse {

  message?: string;

  error?: string;

  detail?: string;

  timestamp?: string;

  status?: number;
}


class ErrorService {


  // ==========================================================================
  // NORMALIZE
  // ==========================================================================

  normalize(
    error: unknown,
  ): AppError {

    const timestamp =
      new Date().toISOString();


    if (
      axios.isAxiosError(
        error
      )
    ) {

      const axiosError =
        error as AxiosError<BackendErrorResponse>;


      const responseData =
        axiosError.response?.data;


      const message =
        responseData?.message ??
        responseData?.detail ??
        responseData?.error ??
        axiosError.message ??
        "A network request failed.";


      return {

        message,

        code:
          axiosError.code,

        status:
          axiosError.response?.status,

        details:
          responseData,

        timestamp,

        stack:
          import.meta.env.DEV
            ? axiosError.stack
            : undefined,
      };
    }


    if (
      error instanceof Error
    ) {

      return {

        message:
          error.message ||
          "An unexpected error occurred.",

        timestamp,

        stack:
          import.meta.env.DEV
            ? error.stack
            : undefined,
      };
    }


    if (
      typeof error === "object" &&
      error !== null
    ) {

      const err =
        error as Record<
          string,
          unknown
        >;


      return {

        message:
          typeof err.message === "string"
            ? err.message
            : "An unexpected error occurred.",

        code:
          typeof err.code === "string"
            ? err.code
            : undefined,

        status:
          typeof err.status === "number"
            ? err.status
            : undefined,

        details:
          err.details,

        timestamp,
      };
    }


    return {

      message:
        typeof error === "string" &&
        error.trim()
          ? error
          : "An unexpected error occurred.",

      timestamp,
    };
  }


  // ==========================================================================
  // LOG
  // ==========================================================================

  log(
    error: unknown,
    context = "Application",
  ): AppError {

    const appError =
      this.normalize(
        error
      );


    if (
      import.meta.env.DEV
    ) {

      console.error(
        "[Application Error]",
        {
          ...appError,
          context,
          environment:
            import.meta.env.MODE,
        },
      );
    }


    /**
     * Production monitoring can be integrated here.
     *
     * Example:
     *
     * Sentry.captureException(error);
     *
     * Do not send sensitive document contents,
     * JWTs, passwords, or uploaded file contents
     * to monitoring systems.
     */


    return appError;
  }


  // ==========================================================================
  // USER MESSAGE
  // ==========================================================================

  getMessage(
    error: unknown,
    fallback =
      "Something went wrong. Please try again.",
  ): string {

    const normalized =
      this.normalize(
        error
      );


    if (
      normalized.status === 401
    ) {

      return "Your session has expired. Please sign in again.";
    }


    if (
      normalized.status === 403
    ) {

      return "You are not authorized to perform this action.";
    }


    if (
      normalized.status === 404
    ) {

      return "The requested document could not be found.";
    }


    if (
      normalized.status === 413
    ) {

      return "The uploaded file is too large.";
    }


    if (
      normalized.status !== undefined &&
      normalized.status >= 500
    ) {

      return "The server encountered an error. Please try again later.";
    }


    return (
      normalized.message ||
      fallback
    );
  }


  // ==========================================================================
  // HTTP ERROR
  // ==========================================================================

  isHttpError(
    error: unknown,
  ): boolean {

    return axios.isAxiosError(
      error
    );
  }


  // ==========================================================================
  // HTTP STATUS
  // ==========================================================================

  getStatus(
    error: unknown,
  ): number | undefined {

    if (
      !axios.isAxiosError(
        error
      )
    ) {

      return undefined;
    }


    return error.response?.status;
  }
}


export const errorService =
  new ErrorService();


export default errorService;