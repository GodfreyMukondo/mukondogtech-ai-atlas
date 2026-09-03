package com.godfrey.ai_immigration_document_analyzer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long documentId;

    private String fraudType;

    private String severity;

    private LocalDateTime detectedAt;
}