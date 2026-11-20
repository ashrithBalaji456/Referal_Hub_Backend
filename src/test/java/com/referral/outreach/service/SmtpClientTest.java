package com.referral.outreach.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SmtpClientTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private SmtpClient smtpClient;

    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(smtpClient, "defaultFromEmail", "sender@example.com");
        ReflectionTestUtils.setField(smtpClient, "defaultFromName", "Ashrith Balaji");
        ReflectionTestUtils.setField(smtpClient, "smtpUsername", "sender@example.com");

        Session session = Session.getInstance(new Properties());
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(session));
    }

    @Test
    public void testSendEmail_WithoutAttachment() throws Exception {
        String messageId = smtpClient.sendEmail(
                "recruiter@company.com",
                "Jane Doe",
                "Outreach Subject",
                "Hello Recruiter",
                "<p>Hello Recruiter</p>",
                null,
                null
        );

        assertNotNull(messageId);
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    public void testSendEmail_WithAttachment() throws Exception {
        String samplePdfBase64 = Base64.getEncoder().encodeToString("Sample PDF Binary".getBytes());

        String messageId = smtpClient.sendEmail(
                "recruiter@company.com",
                "Jane Doe",
                "Outreach Subject",
                "Hello Recruiter",
                "<p>Hello Recruiter</p>",
                "resume.pdf",
                samplePdfBase64
        );

        assertNotNull(messageId);
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
}
