package com.godfrey.ai_immigration_document_analyzer.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "document_embeddings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long documentId;

    @Lob
    private String contentChunk;

    @Lob
    private String embeddingJson;
}