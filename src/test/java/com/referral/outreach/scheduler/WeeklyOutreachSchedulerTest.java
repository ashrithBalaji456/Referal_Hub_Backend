package com.referral.outreach.scheduler;

import com.referral.outreach.entity.*;
import com.referral.outreach.repository.CampaignRepository;
import com.referral.outreach.repository.RecruiterRepository;
import com.referral.outreach.repository.ResumeRepository;
import com.referral.outreach.repository.TemplateRepository;
import com.referral.outreach.service.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class WeeklyOutreachSchedulerTest {

    @Autowired
    private WeeklyOutreachScheduler scheduler;

    @Autowired
    private RecruiterRepository recruiterRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @MockBean
    private MailService mailService;

    private Recruiter eligible1;
    private Recruiter cooldownRecruiter;
    private Recruiter eligible2;
    private Recruiter inactiveRecruiter;
    private Campaign activeCampaign;

    @BeforeEach
    public void setup() {
        recruiterRepository.deleteAll();
        campaignRepository.deleteAll();
        templateRepository.deleteAll();
        resumeRepository.deleteAll();

        // 1. Eligible - never contacted
        eligible1 = recruiterRepository.save(Recruiter.builder()
                .name("Eligible 1")
                .email("eligible1@test.com")
                .company("Comp A")
                .roleCategory(RoleCategory.JAVA_BACKEND_DEVELOPER)
                .status(RecruiterStatus.ACTIVE)
                .build());

        // 2. Cooldown - contacted 10 days ago (cooldown is 30 days)
        cooldownRecruiter = recruiterRepository.save(Recruiter.builder()
                .name("Cooldown")
                .email("cooldown@test.com")
                .company("Comp B")
                .roleCategory(RoleCategory.SPRING_BOOT_DEVELOPER)
                .status(RecruiterStatus.ACTIVE)
                .lastContactedDate(LocalDateTime.now().minusDays(10))
                .build());

        // 3. Eligible - contacted 40 days ago
        eligible2 = recruiterRepository.save(Recruiter.builder()
                .name("Eligible 2")
                .email("eligible2@test.com")
                .company("Comp C")
                .roleCategory(RoleCategory.JAVA_BACKEND_DEVELOPER)
                .status(RecruiterStatus.ACTIVE)
                .lastContactedDate(LocalDateTime.now().minusDays(40))
                .build());

        // 4. Inactive - never contacted
        inactiveRecruiter = recruiterRepository.save(Recruiter.builder()
                .name("Inactive")
                .email("inactive@test.com")
                .company("Comp D")
                .roleCategory(RoleCategory.SPRING_BOOT_DEVELOPER)
                .status(RecruiterStatus.INACTIVE)
                .build());

        EmailTemplate template = templateRepository.save(EmailTemplate.builder()
                .templateName("Template")
                .subject("Sub")
                .body("Body")
                .build());

        Resume resume = resumeRepository.save(Resume.builder()
                .filename("resume.pdf")
                .originalFilename("resume.pdf")
                .filePath("./uploads/resume.pdf")
                .fileSize(100L)
                .contentType("application/pdf")
                .isActive(true)
                .build());

        activeCampaign = campaignRepository.save(Campaign.builder()
                .name("Active Campaign")
                .emailTemplate(template)
                .resume(resume)
                .isEnabled(true)
                .build());
    }

    @Test
    public void testExecuteWeeklyOutreach_FiltersRecruitersAndContinuesOnFailure() {
        // Mock sendOutreachEmail to throw for eligible1 and succeed for eligible2
        doThrow(new RuntimeException("Mail server error"))
                .when(mailService)
                .sendOutreachEmail(eq(eligible1.getId()), anyLong(), anyLong(), eq(activeCampaign.getId()));

        doNothing()
                .when(mailService)
                .sendOutreachEmail(eq(eligible2.getId()), anyLong(), anyLong(), eq(activeCampaign.getId()));

        // Run Scheduler
        scheduler.executeWeeklyOutreach();

        // Verify sendOutreachEmail was called ONLY for the two eligible recruiters
        verify(mailService, times(1))
                .sendOutreachEmail(eq(eligible1.getId()), anyLong(), anyLong(), eq(activeCampaign.getId()));

        verify(mailService, times(1))
                .sendOutreachEmail(eq(eligible2.getId()), anyLong(), anyLong(), eq(activeCampaign.getId()));

        // Verify cooldown or inactive recruiters were NOT emailed
        verify(mailService, never())
                .sendOutreachEmail(eq(cooldownRecruiter.getId()), anyLong(), anyLong(), anyLong());

        verify(mailService, never())
                .sendOutreachEmail(eq(inactiveRecruiter.getId()), anyLong(), anyLong(), anyLong());
    }
}
