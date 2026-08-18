package com.referral.outreach.service;

import com.referral.outreach.entity.*;
import com.referral.outreach.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
    private BrevoClient brevoClient;

    private Recruiter recruiter;
    private EmailTemplate template;
    private Resume resume;
    private File tempResumeFile;

    @BeforeEach
    public void setup() throws Exception {
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
                .title("HR Head")
                .company("Amazon")
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

        when(brevoClient.sendEmail(anyString(), any(), anyString(), anyString(), any(), any(), any()))
                .thenReturn("brevo_msg_123");
    }

    @Test
    public void testSendOutreachEmail_Success() throws Exception {
        mailService.sendOutreachEmail(recruiter.getId(), template.getId(), resume.getId(), null);

        // Verify brevoClient.sendEmail was called
        verify(brevoClient, times(1)).sendEmail(anyString(), any(), anyString(), anyString(), any(), any(), any());

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
    public void testSendOutreachEmail_Failure() throws Exception {
        // Force brevoClient.sendEmail to throw exception
        doThrow(new RuntimeException("Brevo API rate limit exceeded"))
                .when(brevoClient).sendEmail(anyString(), any(), anyString(), anyString(), any(), any(), any());

        boolean success = mailService.sendOutreachEmail(recruiter.getId(), template.getId(), resume.getId(), null);
        assertFalse(success);

        // Verify history was saved with FAILED
        List<EmailHistory> historyList = emailHistoryRepository.findAll();
        assertEquals(1, historyList.size());
        EmailHistory history = historyList.get(0);
        assertEquals(EmailHistoryStatus.FAILED, history.getStatus());
        assertEquals("Brevo API rate limit exceeded", history.getErrorMessage());

        // Verify recruiter last contacted date was NOT set
        Recruiter updatedRecruiter = recruiterRepository.findById(recruiter.getId()).get();
        assertNull(updatedRecruiter.getLastContactedDate());
    }
}
