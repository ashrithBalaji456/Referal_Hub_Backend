package com.referral.outreach.controller;

import com.referral.outreach.entity.*;
import com.referral.outreach.repository.CampaignRepository;
import com.referral.outreach.repository.EmailHistoryRepository;
import com.referral.outreach.repository.RecruiterRepository;
import com.referral.outreach.repository.ResumeRepository;
import com.referral.outreach.repository.TemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class EmailHistoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmailHistoryRepository emailHistoryRepository;

    @Autowired
    private RecruiterRepository recruiterRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private com.referral.outreach.repository.UserRepository userRepository;

    private Recruiter recruiter1;
    private Recruiter recruiter2;
    private Campaign campaign;

    @BeforeEach
    public void setup() {
        emailHistoryRepository.deleteAll();
        campaignRepository.deleteAll();
        recruiterRepository.deleteAll();
        templateRepository.deleteAll();
        resumeRepository.deleteAll();
        userRepository.deleteAll();

        // Seed and authenticate test user
        User testUser = userRepository.save(User.builder()
                .username("testuser")
                .email("testuser@gmail.com")
                .password("password")
                .build());

        com.referral.outreach.security.UserPrincipal principal = com.referral.outreach.security.UserPrincipal.create(testUser);
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth = 
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        principal, null, java.util.Collections.emptyList());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

        recruiter1 = recruiterRepository.save(Recruiter.builder()
                .name("Alice")
                .email("alice@netflix.com")
                .title("Recruitment Specialist")
                .company("Netflix")
                .status(RecruiterStatus.ACTIVE)
                .build());

        recruiter2 = recruiterRepository.save(Recruiter.builder()
                .name("Bob")
                .email("bob@meta.com")
                .title("HR Manager")
                .company("Meta")
                .status(RecruiterStatus.ACTIVE)
                .build());

        EmailTemplate template = templateRepository.save(EmailTemplate.builder()
                .templateName("Template")
                .subject("Sub")
                .body("Body")
                .user(testUser)
                .build());

        Resume resume = resumeRepository.save(Resume.builder()
                .filename("resume.pdf")
                .originalFilename("resume.pdf")
                .filePath("./uploads/resume.pdf")
                .fileSize(100L)
                .contentType("application/pdf")
                .isActive(true)
                .user(testUser)
                .build());

        campaign = campaignRepository.save(Campaign.builder()
                .name("Campaign")
                .emailTemplate(template)
                .resume(resume)
                .isEnabled(true)
                .user(testUser)
                .build());

        // Save some history entries
        emailHistoryRepository.save(EmailHistory.builder()
                .recruiter(recruiter1)
                .campaign(campaign)
                .recipientEmail("alice@netflix.com")
                .subjectUsed("Netflix Subject")
                .sentTimestamp(LocalDateTime.now().minusDays(5))
                .status(EmailHistoryStatus.SUCCESS)
                .user(testUser)
                .build());

        emailHistoryRepository.save(EmailHistory.builder()
                .recruiter(recruiter2)
                .campaign(campaign)
                .recipientEmail("bob@meta.com")
                .subjectUsed("Meta Subject")
                .sentTimestamp(LocalDateTime.now().minusDays(1))
                .status(EmailHistoryStatus.FAILED)
                .errorMessage("SMTP Error")
                .user(testUser)
                .build());
    }

    @Test
    public void testGetAllHistory_NoFilters() throws Exception {
        mockMvc.perform(get("/api/email-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    public void testGetHistory_FilterByCompany() throws Exception {
        mockMvc.perform(get("/api/email-history")
                        .param("company", "Netflix"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].recruiterCompany", is("Netflix")));
    }

    @Test
    public void testGetHistory_FilterByStatus() throws Exception {
        mockMvc.perform(get("/api/email-history")
                        .param("status", "FAILED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is("FAILED")))
                .andExpect(jsonPath("$[0].errorMessage", is("SMTP Error")));
    }

    @Test
    public void testGetHistory_FilterByDateRange() throws Exception {
        // filter for entries in last 3 days
        String startDate = LocalDateTime.now().minusDays(3).toString();
        String endDate = LocalDateTime.now().plusDays(1).toString();

        mockMvc.perform(get("/api/email-history")
                        .param("startDate", startDate)
                        .param("endDate", endDate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].recipientEmail", is("bob@meta.com")));
    }
}
