package com.godfrey.ai_immigration_document_analyzer.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChatResponse {

    private String answer;

    private List<String> sources;

    private double confidence;

    private String sessionId;
}