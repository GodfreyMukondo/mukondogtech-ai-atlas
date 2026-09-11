import API from "./axios";

import type {
  Application,
  ApplicationSubmitRequest,
} from "../types/application";

/**
 * ============================================================================
 * APPLICATION API CLIENT
 * ============================================================================
 *
 * Centralized HTTP client for authenticated immigration application
 * operations.
 *
 * Authentication:
 * ----------------------------------------------------------------------------
 * Authentication is handled centrally by ./axios.
 *
 * The frontend MUST NOT send userId. The backend resolves the authenticated
 * user from the JWT.
 *
 * Endpoints:
 * ----------------------------------------------------------------------------
 *
 * POST /api/applications
 * GET  /api/applications
 * GET  /api/applications/{applicationId}
 *
 * Submission contract:
 * ----------------------------------------------------------------------------
 *
 * Supporting documents must already have been uploaded via
 * documentApi.uploadDocumentApi() (POST /api/documents/upload), which
 * returns each document's ID. Submit this request referencing those IDs.
 * ============================================================================
 */


const APPLICATION_API_BASE_PATH = "/applications";


/**
 * ============================================================================
 * API RESPONSE CONTRACT
 * ============================================================================
 *
 * Supports both an enveloped { data, message, success } response and a
 * direct response body, matching documentApi's convention.
 * ============================================================================
 */

interface ApiResponse<T> {
  data: T;
  message?: string;
  success?: boolean;
}


const unwrapApiResponse = <T>(
  responseData: ApiResponse<T> | T,
): T => {

  if (
    responseData !== null &&
    typeof responseData === "object" &&
    "data" in responseData
  ) {

    return (
      responseData as ApiResponse<T>
    ).data;
  }

  return responseData as T;
};


/**
 * ============================================================================
 * APPLICATION VALIDATION
 * ============================================================================
 */

type UnknownRecord = Record<string, unknown>;

const isApplication = (
  value: unknown,
): value is Application => {

  if (
    value === null ||
    typeof value !== "object"
  ) {

    return false;
  }

  const record = value as UnknownRecord;

  return (
    typeof record.id === "number" &&
    Number.isInteger(record.id) &&
    record.id > 0 &&
    typeof record.fullName === "string" &&
    typeof record.status === "string"
  );
};

const isApplicationArray = (
  value: unknown,
): value is Application[] => {

  return (
    Array.isArray(value) &&
    value.every(isApplication)
  );
};


/**
 * ============================================================================
 * SUBMIT APPLICATION REQUEST VALIDATION
 * ============================================================================
 */

const validateSubmitRequest = (
  request: ApplicationSubmitRequest,
): void => {

  if (!request) {

    throw new Error(
      "Application data is required.",
    );
  }

  if (
    !request.fullName ||
    !request.fullName.trim()
  ) {

    throw new Error(
      "Full name is required.",
    );
  }

  if (
    !request.email ||
    !request.email.trim()
  ) {

    throw new Error(
      "Email is required.",
    );
  }

  if (
    !request.country ||
    !request.country.trim()
  ) {

    throw new Error(
      "Country is required.",
    );
  }

  if (
    !request.visaType ||
    !request.visaType.trim()
  ) {

    throw new Error(
      "Visa type is required.",
    );
  }

  if (
    !Array.isArray(request.documentIds) ||
    request.documentIds.length === 0
  ) {

    throw new Error(
      "At least one supporting document is required.",
    );
  }
};


/**
 * ============================================================================
 * SUBMIT APPLICATION
 * ============================================================================
 *
 * POST /api/applications
 * ============================================================================
 */

export const submitApplicationApi = async (
  request: ApplicationSubmitRequest,
): Promise<Application> => {

  validateSubmitRequest(request);

  const response =
    await API.post<
      ApiResponse<Application> | Application
    >(
      APPLICATION_API_BASE_PATH,
      request,
    );

  const application =
    unwrapApiResponse<Application>(
      response.data,
    );

  if (!isApplication(application)) {

    throw new Error(
      "The application was submitted, but the server returned an invalid response.",
    );
  }

  return application;
};


/**
 * ============================================================================
 * GET MY APPLICATIONS
 * ============================================================================
 *
 * GET /api/applications
 * ============================================================================
 */

export const getMyApplicationsApi =
  async (): Promise<Application[]> => {

    const response =
      await API.get<
        ApiResponse<Application[]> | Application[]
      >(
        APPLICATION_API_BASE_PATH,
      );

    const applications =
      unwrapApiResponse<Application[]>(
        response.data,
      );

    if (!isApplicationArray(applications)) {

      throw new Error(
        "The server returned an invalid applications response.",
      );
    }

    return applications;
  };


/**
 * ============================================================================
 * GET SINGLE APPLICATION
 * ============================================================================
 *
 * GET /api/applications/{applicationId}
 * ============================================================================
 */

export const getApplicationApi = async (
  id: number,
): Promise<Application> => {

  if (
    !Number.isInteger(id) ||
    id <= 0
  ) {

    throw new Error(
      "Invalid application ID.",
    );
  }

  const response =
    await API.get<
      ApiResponse<Application> | Application
    >(
      `${APPLICATION_API_BASE_PATH}/${id}`,
    );

  const application =
    unwrapApiResponse<Application>(
      response.data,
    );

  if (!isApplication(application)) {

    throw new Error(
      "The server returned an invalid application response.",
    );
  }

  return application;
};


/**
 * ============================================================================
 * DEFAULT API OBJECT
 * ============================================================================
 */

const applicationApi = {
  submitApplication: submitApplicationApi,
  getMyApplications: getMyApplicationsApi,
  getApplication: getApplicationApi,
};


export default applicationApi;
