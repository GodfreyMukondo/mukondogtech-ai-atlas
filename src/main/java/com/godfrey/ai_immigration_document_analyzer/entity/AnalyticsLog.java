package com.godfrey.ai_immigration_document_analyzer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "analytics")
@Getter
@Setter
public class AnalyticsLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String eventType;

    private String country;

    private LocalDateTime createdAt;
}