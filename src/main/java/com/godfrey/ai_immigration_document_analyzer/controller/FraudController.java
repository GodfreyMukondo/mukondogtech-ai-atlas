package com.godfrey.ai_immigration_document_analyzer.controller;

import com.godfrey.ai_immigration_document_analyzer.service.FraudDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/fraud")
@RequiredArgsConstructor
@Slf4j
public class FraudController {


    private final FraudDetectionService fraudDetectionService;

    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyze(
            @RequestParam String text
    ) {

        log.info("Fraud analysis requested");

        boolean fraudDetected = fraudDetectionService.detect(text);

        String riskLevel = fraudDetectionService.calculateRiskLevel(text);

        return ResponseEntity.ok(
                Map.of(
                        "fraudDetected", fraudDetected,
                        "riskLevel", riskLevel,
                        "status", "SUCCESS"
                )
        );
    }


}
