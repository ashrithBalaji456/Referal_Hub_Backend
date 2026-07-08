package com.referral.outreach.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.referral.outreach.dto.CampaignRequest;
import com.referral.outreach.entity.*;
import com.referral.outreach.repository.CampaignRepository;
import com.referral.outreach.repository.RecruiterRepository;
import com.referral.outreach.repository.ResumeRepository;
import com.referral.outreach.repository.TemplateRepository;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class CampaignControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private RecruiterRepository recruiterRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JavaMailSender mailSender;

    private EmailTemplate savedTemplate;
    private Resume savedResume;
    private Recruiter savedRecruiter;
    private File tempResumeFile;

    @BeforeEach
    public void setup() throws IOException {
        campaignRepository.deleteAll();
        templateRepository.deleteAll();
        resumeRepository.deleteAll();
        recruiterRepository.deleteAll();

        // Create temporary PDF file to simulate resume
        tempResumeFile = File.createTempFile("test_resume", ".pdf");
        try (FileWriter writer = new FileWriter(tempResumeFile)) {
            writer.write("dummy content");
        }

        savedTemplate = templateRepository.save(EmailTemplate.builder()
                .templateName("My Template")
                .subject("Referral for {{roleName}}")
                .body("Hi {{recruiterName}}, I want to apply for a role at {{companyName}}.")
                .build());

        savedResume = resumeRepository.save(Resume.builder()
                .filename(tempResumeFile.getName())
                .originalFilename("resume.pdf")
                .filePath(tempResumeFile.getAbsolutePath())
                .fileSize(100L)
                .contentType("application/pdf")
                .isActive(true)
                .build());

        savedRecruiter = recruiterRepository.save(Recruiter.builder()
                .name("Bob Smith")
                .email("bob@company.com")
                .company("Microsoft")
                .roleCategory(RoleCategory.SPRING_BOOT_DEVELOPER)
                .status(RecruiterStatus.ACTIVE)
                .build());

        MimeMessage mockMimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mockMimeMessage);
    }

    @Test
    public void testCreateCampaign_Success() throws Exception {
        CampaignRequest request = CampaignRequest.builder()
                .name("Fall Campaign")
                .templateId(savedTemplate.getId())
                .resumeId(savedResume.getId())
                .isEnabled(true)
                .build();

        mockMvc.perform(post("/api/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Fall Campaign")))
                .andExpect(jsonPath("$.enabled", is(true)))
                .andExpect(jsonPath("$.templateName", is("My Template")));
    }

    @Test
    public void testEnableCampaign_Success() throws Exception {
        Campaign camp1 = campaignRepository.save(Campaign.builder()
                .name("Camp 1")
                .emailTemplate(savedTemplate)
                .resume(savedResume)
                .isEnabled(true)
                .build());

        Campaign camp2 = campaignRepository.save(Campaign.builder()
                .name("Camp 2")
                .emailTemplate(savedTemplate)
                .resume(savedResume)
                .isEnabled(false)
                .build());

        mockMvc.perform(patch("/api/campaigns/" + camp2.getId() + "/enable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled", is(true)));

        // Verify in DB that other campaigns are disabled
        Campaign updated1 = campaignRepository.findById(camp1.getId()).get();
        Campaign updated2 = campaignRepository.findById(camp2.getId()).get();

        assertFalse(updated1.isEnabled());
        assertTrue(updated2.isEnabled());
    }

    @Test
    public void testPreviewEmail_Success() throws Exception {
        Campaign campaign = campaignRepository.save(Campaign.builder()
                .name("Spring Campaign")
                .emailTemplate(savedTemplate)
                .resume(savedResume)
                .isEnabled(true)
                .build());

        mockMvc.perform(get("/api/campaigns/" + campaign.getId() + "/preview")
                        .param("recruiterId", savedRecruiter.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipientEmail", is("bob@company.com")))
                .andExpect(jsonPath("$.subject", is("Referral for Spring Boot Developer")))
                .andExpect(jsonPath("$.body", is("Hi Bob Smith, I want to apply for a role at Microsoft.")));
    }

    @Test
    public void testTriggerCampaign_Success() throws Exception {
        Campaign campaign = campaignRepository.save(Campaign.builder()
                .name("Spring Campaign")
                .emailTemplate(savedTemplate)
                .resume(savedResume)
                .isEnabled(true)
                .build());

        mockMvc.perform(post("/api/campaigns/" + campaign.getId() + "/trigger")
                        .param("recruiterId", savedRecruiter.getId().toString()))
                .andExpect(status().isOk());
    }
}
