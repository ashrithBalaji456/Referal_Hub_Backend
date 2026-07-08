package com.referral.outreach.scheduler;

import com.referral.outreach.entity.Campaign;
import com.referral.outreach.entity.Recruiter;
import com.referral.outreach.entity.RecruiterStatus;
import com.referral.outreach.repository.CampaignRepository;
import com.referral.outreach.repository.RecruiterRepository;
import com.referral.outreach.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyOutreachScheduler {

    private final CampaignRepository campaignRepository;
    private final RecruiterRepository recruiterRepository;
    private final MailService mailService;

    @Value("${app.scheduler.cooldown-days:30}")
    private int cooldownDays;

    @Scheduled(cron = "${app.scheduler.cron:0 0 9 * * MON}")
    public void executeWeeklyOutreach() {
        log.info("Weekly outreach scheduler started at {}", LocalDateTime.now());

        // Find active campaign
        Campaign activeCampaign = campaignRepository.findByIsEnabledTrue()
                .orElse(null);

        if (activeCampaign == null) {
            log.warn("Scheduler executed but no active campaign was found. Skipping outreach.");
            return;
        }

        if (activeCampaign.getEmailTemplate() == null || activeCampaign.getResume() == null) {
            log.warn("Active campaign ID: {} is missing template or resume association. Skipping outreach.", 
                    activeCampaign.getId());
            return;
        }

        // Find eligible recruiters
        LocalDateTime cooldownLimit = LocalDateTime.now().minusDays(cooldownDays);
        List<Recruiter> eligibleRecruiters = recruiterRepository.findEligibleRecruiters(
                RecruiterStatus.ACTIVE, 
                cooldownLimit
        );

        log.info("Found {} eligible active recruiters for campaign: '{}' (ID: {}) with cooldown limit: {}", 
                eligibleRecruiters.size(), activeCampaign.getName(), activeCampaign.getId(), cooldownLimit);

        if (eligibleRecruiters.isEmpty()) {
            log.info("No eligible recruiters to contact. Outreach complete.");
            return;
        }

        int successCount = 0;
        int failureCount = 0;

        for (Recruiter recruiter : eligibleRecruiters) {
            try {
                // Check if campaign was disabled mid-flight
                Campaign current = campaignRepository.findById(activeCampaign.getId()).orElse(null);
                if (current == null || !current.isEnabled()) {
                    log.info("Active campaign ID: {} was disabled. Aborting scheduler outreach.", activeCampaign.getId());
                    break;
                }

                // Send email
                mailService.sendOutreachEmail(
                        recruiter.getId(),
                        activeCampaign.getEmailTemplate().getId(),
                        activeCampaign.getResume().getId(),
                        activeCampaign.getId()
                );
                successCount++;
                log.info("Successfully processed outreach for recruiter: {} ({})", recruiter.getName(), recruiter.getEmail());

                // 2-second rate-limiting delay between dispatches
                Thread.sleep(2000);
            } catch (InterruptedException ie) {
                log.warn("Scheduler outreach thread interrupted.");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ex) {
                failureCount++;
                log.error("Failed to send outreach email to recruiter: {} ({}). Continuing loop.", 
                        recruiter.getName(), recruiter.getEmail(), ex);
            }
        }

        log.info("Weekly outreach campaign '{}' (ID: {}) finished. Eligible: {}, Success: {}, Failures: {}",
                activeCampaign.getName(), activeCampaign.getId(), eligibleRecruiters.size(), successCount, failureCount);
    }
}
