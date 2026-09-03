package com.godfrey.ai_immigration_document_analyzer.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {


    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(
                Map.of(
                        "status", "UP",
                        "service", "ai-immigration-document-analyzer",
                        "timestamp", Instant.now()
                )
        );
    }

    @GetMapping("/live")
    public ResponseEntity<Map<String, Object>> liveness() {
        return ResponseEntity.ok(
                Map.of(
                        "status", "ALIVE"
                )
        );
    }

    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> readiness() {
        return ResponseEntity.ok(
                Map.of(
                        "status", "READY"
                )
        );
    }


}
