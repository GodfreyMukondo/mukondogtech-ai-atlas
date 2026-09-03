package com.godfrey.ai_immigration_document_analyzer.messaging;

import com.godfrey.ai_immigration_document_analyzer.events.DocumentUploadedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocumentEventProducer {


    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC = "document.uploaded";

    public void publish(DocumentUploadedEvent event) {
        kafkaTemplate.send(TOPIC, event);
    }


}
