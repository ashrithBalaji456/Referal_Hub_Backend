package com.referral.outreach.service;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmtpClient {

    private final JavaMailSender mailSender;

    @Value("${brevo.from.email:ashrithbalajigudla@gmail.com}")
    private String defaultFromEmail;

    @Value("${brevo.from.name:Gudla Ashrith Balaji}")
    private String defaultFromName;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    public String sendEmail(String toEmail, String toName, String subject, String textContent, String htmlContent, String attachmentFileName, String base64AttachmentContent) throws Exception {
        log.info("Sending email through SMTP to recipient: {} <{}>", toName, toEmail);

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        String fromEmail = (smtpUsername != null && !smtpUsername.isBlank()) ? smtpUsername : defaultFromEmail;
        String fromName = (defaultFromName != null && !defaultFromName.isBlank()) ? defaultFromName : "Referral Hub";

        if (fromName != null && !fromName.isBlank()) {
            helper.setFrom(new InternetAddress(fromEmail, fromName));
        } else {
            helper.setFrom(fromEmail);
        }

        if (toName != null && !toName.isBlank()) {
            helper.setTo(new InternetAddress(toEmail, toName));
        } else {
            helper.setTo(toEmail);
        }

        helper.setSubject(subject);

        if (htmlContent != null && !htmlContent.isBlank()) {
            helper.setText(textContent != null ? textContent : "", htmlContent);
        } else if (textContent != null) {
            helper.setText(textContent, "<p>" + textContent.replace("\n", "<br/>") + "</p>");
        }

        if (attachmentFileName != null && !attachmentFileName.isBlank() && base64AttachmentContent != null && !base64AttachmentContent.isBlank()) {
            byte[] attachmentBytes = Base64.getMimeDecoder().decode(base64AttachmentContent);
            helper.addAttachment(attachmentFileName, new ByteArrayResource(attachmentBytes));
            log.info("Attached file via SMTP: {} (size: {} bytes)", attachmentFileName, attachmentBytes.length);
        }

        mailSender.send(mimeMessage);

        String messageId = mimeMessage.getMessageID();
        if (messageId == null || messageId.isBlank()) {
            messageId = "smtp_" + System.currentTimeMillis();
        }

        log.info("Email successfully sent through SMTP. Recipient: {}, Message ID: {}", toEmail, messageId);
        return messageId;
    }
}
