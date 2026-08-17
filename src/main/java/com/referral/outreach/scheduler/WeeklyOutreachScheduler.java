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

        // Find active campaigns
        List<Campaign> activeCampaigns = campaignRepository.findAllByIsEnabledTrue();

        if (activeCampaigns.isEmpty()) {
            log.warn("Scheduler executed but no active campaigns were found. Skipping outreach.");
            return;
        }

        for (Campaign activeCampaign : activeCampaigns) {
            if (activeCampaign.getEmailTemplate() == null || activeCampaign.getResume() == null) {
                log.warn("Active campaign ID: {} is missing template or resume association. Skipping outreach.", 
                        activeCampaign.getId());
                continue;
            }

            // Find eligible recruiters
            LocalDateTime cooldownLimit = LocalDateTime.now().minusDays(cooldownDays);
            String groupStr = activeCampaign.getTargetTitleGroup() != null ? activeCampaign.getTargetTitleGroup() : "ALL";
            List<String> groups = java.util.Arrays.stream(groupStr.split(","))
                    .map(String::trim)
                    .collect(java.util.stream.Collectors.toList());
            boolean isAllGroup = groups.contains("ALL") || groups.isEmpty();

            List<Recruiter> eligibleRecruiters = recruiterRepository.findEligibleRecruitersFiltered(
                    RecruiterStatus.ACTIVE, 
                    cooldownLimit,
                    activeCampaign.getTargetSet(),
                    groups,
                    isAllGroup
            );

            log.info("Found {} eligible active recruiters for campaign: '{}' (ID: {}) belonging to user: {} with cooldown limit: {}", 
                    eligibleRecruiters.size(), activeCampaign.getName(), activeCampaign.getId(), 
                    activeCampaign.getUser() != null ? activeCampaign.getUser().getUsername() : "System", cooldownLimit);

            if (eligibleRecruiters.isEmpty()) {
                log.info("No eligible recruiters to contact for campaign '{}' (ID: {}).", activeCampaign.getName(), activeCampaign.getId());
                continue;
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
                    boolean success = mailService.sendOutreachEmail(
                            recruiter.getId(),
                            activeCampaign.getEmailTemplate().getId(),
                            activeCampaign.getResume().getId(),
                            activeCampaign.getId()
                    );
                    if (success) {
                        successCount++;
                        log.info("Successfully processed outreach for recruiter: {} ({})", recruiter.getName(), recruiter.getEmail());
                    } else {
                        failureCount++;
                        log.warn("Failed to process outreach for recruiter: {} ({})", recruiter.getName(), recruiter.getEmail());
                    }

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
}
