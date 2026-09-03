package com.godfrey.ai_immigration_document_analyzer.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChatRequest {

    @NotBlank(message = "Question is required")
    @Size(min = 2, max = 2000)
    private String question;

    private String sessionId;

    public ChatRequest() {}

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}