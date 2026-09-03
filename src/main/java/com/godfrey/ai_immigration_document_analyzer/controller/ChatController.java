package com.godfrey.ai_immigration_document_analyzer.controller;

import com.godfrey.ai_immigration_document_analyzer.dto.request.ChatRequest;
import com.godfrey.ai_immigration_document_analyzer.dto.response.ChatResponse;
import com.godfrey.ai_immigration_document_analyzer.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) throws Exception {
        return chatService.ask(request.getQuestion(), request.getSessionId());
    }
}