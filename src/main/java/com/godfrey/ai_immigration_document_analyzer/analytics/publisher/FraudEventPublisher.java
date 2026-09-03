package com.godfrey.ai_immigration_document_analyzer.analytics.publisher;

import com.godfrey.ai_immigration_document_analyzer.analytics.event.FraudEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FraudEventPublisher {

    private final ApplicationEventPublisher publisher;

    public void publish(FraudEvent event) {
        log.info("Publishing fraud event: {}", event.getRuleId());
        publisher.publishEvent(event);
    }
}