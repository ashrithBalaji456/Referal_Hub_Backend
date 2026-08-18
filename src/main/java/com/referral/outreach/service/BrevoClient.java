package com.referral.outreach.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BrevoClient {

    private final ObjectMapper objectMapper;

    @Value("${brevo.api.key:xkeysib-mock-key}")
    private String apiKey;

    @Value("${brevo.from.email:ashrithbalajigudla@gmail.com}")
    private String fromEmail;

    @Value("${brevo.from.name:Ashrith Balaji}")
    private String fromName;

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public String sendEmail(String toEmail, String toName, String subject, String textContent, String htmlContent, String attachmentFileName, String base64AttachmentContent) throws Exception {
        log.info("Sending email through Brevo HTTP API to recipient: {} <{}>", toName, toEmail);

        Map<String, Object> payload = new HashMap<>();
        
        // Sender
        Map<String, String> sender = new HashMap<>();
        sender.put("name", fromName != null && !fromName.isBlank() ? fromName : "Referral Hub");
        sender.put("email", fromEmail);
        payload.put("sender", sender);

        // Recipient
        List<Map<String, String>> toList = new ArrayList<>();
        Map<String, String> recipient = new HashMap<>();
        recipient.put("email", toEmail);
        if (toName != null && !toName.isBlank()) {
            recipient.put("name", toName);
        }
        toList.add(recipient);
        payload.put("to", toList);

        // Subject & Body
        payload.put("subject", subject);
        if (textContent != null) {
            payload.put("textContent", textContent);
        }
        if (htmlContent != null) {
            payload.put("htmlContent", htmlContent);
        } else if (textContent != null) {
            payload.put("htmlContent", "<p>" + textContent.replace("\n", "<br/>") + "</p>");
        }

        // Attachments
        if (attachmentFileName != null && base64AttachmentContent != null) {
            List<Map<String, String>> attachments = new ArrayList<>();
            Map<String, String> attachment = new HashMap<>();
            attachment.put("name", attachmentFileName);
            attachment.put("content", base64AttachmentContent);
            attachments.add(attachment);
            payload.put("attachment", attachments);
            log.info("Attached file: {} (Base64 length: {} chars)", attachmentFileName, base64AttachmentContent.length());
        }

        String jsonPayload = objectMapper.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BREVO_API_URL))
                .timeout(Duration.ofSeconds(15))
                .header("accept", "application/json")
                .header("api-key", apiKey)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        log.info("Executing Brevo HTTP API call -> From: {}, To: {}, Subject: {}", fromEmail, toEmail, subject);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            Map<?, ?> responseMap = objectMapper.readValue(response.body(), Map.class);
            String messageId = responseMap != null && responseMap.containsKey("messageId")
                    ? String.valueOf(responseMap.get("messageId"))
                    : "brevo_sent_" + System.currentTimeMillis();
            log.info("Email successfully sent through Brevo API. Recipient: {}, Brevo Message ID: {}", toEmail, messageId);
            return messageId;
        } else {
            log.error("Brevo API HTTP Error Status {}: {}", response.statusCode(), response.body());
            throw new RuntimeException("Brevo API delivery failed (Status " + response.statusCode() + "): " + response.body());
        }
    }
}
