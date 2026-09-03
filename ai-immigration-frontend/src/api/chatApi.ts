import API from "./axios";

/**
 * ============================================================
 * CHAT API
 * ============================================================
 *
 * Backend endpoint:
 *
 * POST /api/chat
 *
 * Axios baseURL:
 *
 * http://localhost:8080/api
 *
 * Therefore the request must use:
 *
 * POST /chat
 *
 * Final URL:
 *
 * http://localhost:8080/api/chat
 * ============================================================
 */

export interface ChatResponse {
  answer: string;
  sources: string[];
  confidence: number;
  sessionId: string;
}

export interface ChatRequest {
  question: string;
  sessionId?: string;
}

/**
 * ============================================================
 * ASK QUESTION
 * ============================================================
 */

export const askQuestion = async (
  question: string,
  sessionId?: string,
): Promise<ChatResponse> => {
  const normalizedQuestion =
    question.trim();

  if (!normalizedQuestion) {
    throw new Error(
      "Question cannot be empty.",
    );
  }

  const normalizedSessionId =
    sessionId?.trim();

  const payload: ChatRequest = {
    question:
      normalizedQuestion,
  };

  if (normalizedSessionId) {
    payload.sessionId =
      normalizedSessionId;
  }

  const response =
    await API.post<ChatResponse>(
      "/chat",
      payload,
    );

  /**
   * Defensive response validation.
   */
  if (!response.data) {
    throw new Error(
      "The chat API returned an empty response.",
    );
  }

  return response.data;
};

/**
 * ============================================================
 * CHAT ALIAS
 * ============================================================
 *
 * Kept for compatibility with existing
 * AIChatPage consumers.
 */

export const chatWithAI = async (
  message: string,
  sessionId?: string,
): Promise<ChatResponse> => {
  return askQuestion(
    message,
    sessionId,
  );
};

export default askQuestion;