import { errorService } from "../services/errorService";

/**
 * errorService.getMessage() has a hardcoded, document-specific 404 message
 * ("The requested document could not be found."), which would be
 * misleading on pathway/assessment screens. This resolves the two
 * status codes that need domain-specific wording here and defers to the
 * shared service for everything else (500s, network errors, etc.), without
 * modifying the shared service itself.
 */
export function getAssessmentErrorMessage(error: unknown): string {

  const status = errorService.getStatus(error);

  if (status === 404) {
    return "That pathway or assessment could not be found.";
  }

  if (status === 403) {
    return "You are not authorized to view this assessment.";
  }

  return errorService.getMessage(error);
}
