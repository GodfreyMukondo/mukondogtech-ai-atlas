package com.godfrey.ai_immigration_document_analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    /**
     * Sends an email notification.
     *
     * @param to recipient email
     * @param subject email subject
     * @param body email body
     */
    public void send(
            String to,
            String subject,
            String body
    ) {
        validateInput(to, subject, body);

        try {
            log.info(
                    "Sending email | to={} | subject={}",
                    to,
                    subject
            );

            SimpleMailMessage message = buildMessage(
                    normalize(to),
                    subject.trim(),
                    body.trim()
            );

            mailSender.send(message);

            log.info("Email sent successfully to={}", to);

        } catch (MailException ex) {
            log.error(
                    "Failed to send email | to={} | subject={}",
                    to,
                    subject,
                    ex
            );

            throw new RuntimeException(
                    "Failed to send email",
                    ex
            );
        }
    }

    /**
     * Builds the email message.
     */
    private SimpleMailMessage buildMessage(
            String to,
            String subject,
            String body
    ) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(senderEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        return message;
    }

    /**
     * Input validation.
     */
    private void validateInput(
            String to,
            String subject,
            String body
    ) {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException(
                    "Recipient email cannot be empty"
            );
        }

        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException(
                    "Email subject cannot be empty"
            );
        }

        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException(
                    "Email body cannot be empty"
            );
        }
    }

    /**
     * Normalize email.
     */
    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}