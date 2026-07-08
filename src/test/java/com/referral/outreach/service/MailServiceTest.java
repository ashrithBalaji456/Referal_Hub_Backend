package com.referral.outreach.service;

import com.referral.outreach.entity.*;
import com.referral.outreach.exception.MailSendingException;
import com.referral.outreach.repository.*;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class MailServiceTest {

    @Autowired
    private MailService mailService;

    @Autowired
    private RecruiterRepository recruiterRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private EmailHistoryRepository emailHistoryRepository;

    @MockBean
    private JavaMailSender mailSender;

    private Recruiter recruiter;
    private EmailTemplate template;
    private Resume resume;
    private File tempResumeFile;

    @BeforeEach
    public void setup() throws IOException {
        recruiterRepository.deleteAll();
        templateRepository.deleteAll();
        resumeRepository.deleteAll();
        emailHistoryRepository.deleteAll();

        // Create temporary PDF file to simulate resume
        tempResumeFile = File.createTempFile("test_resume", ".pdf");
        try (FileWriter writer = new FileWriter(tempResumeFile)) {
            writer.write("dummy content");
        }

        recruiter = recruiterRepository.save(Recruiter.builder()
                .name("Alice Recruiter")
                .email("alice@company.com")
                .company("Amazon")
                .roleCategory(RoleCategory.JAVA_BACKEND_DEVELOPER)
                .status(RecruiterStatus.ACTIVE)
                .build());

        template = templateRepository.save(EmailTemplate.builder()
                .templateName("Referral Request")
                .subject("Referral request for {{roleName}}")
                .body("Hello {{recruiterName}}, please check my profile for {{companyName}}.")
                .build());

        resume = resumeRepository.save(Resume.builder()
                .filename(tempResumeFile.getName())
                .originalFilename("actual_resume.pdf")
                .filePath(tempResumeFile.getAbsolutePath())
                .fileSize(100L)
                .contentType("application/pdf")
                .isActive(true)
                .build());

        // Setup mock JavaMailSender to return a valid MimeMessage
        MimeMessage mockMimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mockMimeMessage);
    }

    @Test
    public void testSendOutreachEmail_Success() {
        mailService.sendOutreachEmail(recruiter.getId(), template.getId(), resume.getId(), null);

        // Verify mailSender.send() was called
        verify(mailSender, times(1)).send(any(MimeMessage.class));

        // Verify history was saved with SUCCESS
        List<EmailHistory> historyList = emailHistoryRepository.findAll();
        assertEquals(1, historyList.size());
        EmailHistory history = historyList.get(0);
        assertEquals(EmailHistoryStatus.SUCCESS, history.getStatus());
        assertEquals("alice@company.com", history.getRecipientEmail());
        assertEquals("Referral request for Java Backend Developer", history.getSubjectUsed());
        assertNull(history.getErrorMessage());

        // Verify recruiter last contacted date was set
        Recruiter updatedRecruiter = recruiterRepository.findById(recruiter.getId()).get();
        assertNotNull(updatedRecruiter.getLastContactedDate());
    }

    @Test
    public void testSendOutreachEmail_Failure() {
        // Force mailSender to throw exception
        doThrow(new RuntimeException("SMTP Server offline")).when(mailSender).send(any(MimeMessage.class));

        assertThrows(MailSendingException.class, () -> {
            mailService.sendOutreachEmail(recruiter.getId(), template.getId(), resume.getId(), null);
        });

        // Verify history was saved with FAILED
        List<EmailHistory> historyList = emailHistoryRepository.findAll();
        assertEquals(1, historyList.size());
        EmailHistory history = historyList.get(0);
        assertEquals(EmailHistoryStatus.FAILED, history.getStatus());
        assertEquals("SMTP Server offline", history.getErrorMessage());

        // Verify recruiter last contacted date was NOT set
        Recruiter updatedRecruiter = recruiterRepository.findById(recruiter.getId()).get();
        assertNull(updatedRecruiter.getLastContactedDate());
    }
}
