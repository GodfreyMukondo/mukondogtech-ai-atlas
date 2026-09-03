package com.godfrey.ai_immigration_document_analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlmService {

    private final ChatClient chatClient;

    /**
     * Sends a prompt to the configured AI model.
     */
    public String ask(String prompt) {

        if (!StringUtils.hasText(prompt)) {
            throw new IllegalArgumentException(
                    "Prompt cannot be empty"
            );
        }

        log.info(
                "Sending prompt to AI model. promptLength={}",
                prompt.length()
        );

        try {

            String response =
                    chatClient
                            .prompt()
                            .user(prompt)
                            .call()
                            .content();

            if (!StringUtils.hasText(response)) {

                log.warn(
                        "AI model returned an empty response"
                );

                return "";
            }

            return response.trim();

        } catch (Exception ex) {

            log.error(
                    "AI model request failed",
                    ex
            );

            throw new IllegalStateException(
                    "Failed to generate AI response",
                    ex
            );
        }
    }

    /**
     * Generates a summary of extracted document text.
     */
    public String generateSummary(
            String extractedText
    ) {

        validateText(
                extractedText,
                "Extracted document text"
        );

        String prompt = """
                You are an immigration document analysis assistant.

                Summarize the following document clearly and concisely.

                Requirements:
                - Identify the document type if possible.
                - Highlight important information.
                - Preserve important dates, names, reference numbers and
                  requirements when present.
                - Do not invent information.
                - Clearly indicate if information is missing.

                Document:
                %s
                """.formatted(extractedText.trim());

        return ask(prompt);
    }

    /**
     * Analyzes a document for potential fraud indicators.
     */
    public String analyzeFraudRisk(
            String extractedText
    ) {

        validateText(
                extractedText,
                "Extracted document text"
        );

        String prompt = """
                You are an immigration document fraud-analysis assistant.

                Analyze the following document for potential fraud indicators.

                Return:
                1. Risk level: LOW, MEDIUM, or HIGH
                2. Potential indicators
                3. Explanation
                4. Recommended verification steps

                Important:
                - Do not state that a document is fraudulent unless there
                  is sufficient evidence.
                - Clearly distinguish suspicious indicators from proof of fraud.
                - Do not invent information.

                Document:
                %s
                """.formatted(extractedText.trim());

        return ask(prompt);
    }

    /**
     * Answers an immigration question using document context.
     */
    public String answerImmigrationQuestion(
            String question,
            String documentContext
    ) {

        validateText(
                question,
                "Question"
        );

        String safeContext =
                StringUtils.hasText(documentContext)
                        ? documentContext.trim()
                        : "No document context is available.";

        String prompt = """
                You are an AI immigration assistant.

                Answer the user's question using the provided document
                context.

                Rules:
                - Use the document context when relevant.
                - Do not invent facts.
                - If the document does not contain enough information,
                  clearly say so.
                - Do not present legal advice as a guaranteed outcome.

                Document context:
                %s

                User question:
                %s
                """.formatted(
                safeContext,
                question.trim()
        );

        return ask(prompt);
    }

    private void validateText(
            String value,
            String fieldName
    ) {

        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(
                    fieldName + " cannot be empty"
            );
        }
    }
}