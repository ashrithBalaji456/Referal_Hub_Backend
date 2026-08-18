package com.referral.outreach.service.impl;

import com.referral.outreach.dto.CampaignRequest;
import com.referral.outreach.dto.CampaignResponse;
import com.referral.outreach.dto.PreviewResponse;
import com.referral.outreach.entity.Campaign;
import com.referral.outreach.entity.EmailTemplate;
import com.referral.outreach.entity.Recruiter;
import com.referral.outreach.entity.Resume;
import com.referral.outreach.entity.User;
import com.referral.outreach.exception.ResourceNotFoundException;
import com.referral.outreach.exception.MailSendingException;
import com.referral.outreach.repository.CampaignRepository;
import com.referral.outreach.repository.RecruiterRepository;
import com.referral.outreach.repository.TemplateRepository;
import com.referral.outreach.repository.ResumeRepository;
import com.referral.outreach.repository.EmailHistoryRepository;
import com.referral.outreach.service.CampaignService;
import com.referral.outreach.service.MailService;
import com.referral.outreach.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignServiceImpl implements CampaignService {

    private final CampaignRepository campaignRepository;
    private final TemplateRepository templateRepository;
    private final ResumeRepository resumeRepository;
    private final RecruiterRepository recruiterRepository;
    private final MailService mailService;
    private final EmailHistoryRepository emailHistoryRepository;
    private final SecurityUtils securityUtils;

    @Value("${app.scheduler.cooldown-days:30}")
    private int cooldownDays;

    @Override
    @Transactional
    public CampaignResponse createCampaign(CampaignRequest request) {
        User user = securityUtils.getAuthenticatedUser();
        log.info("Creating campaign: {} for user: {}", request.getName(), user.getUsername());
        
        EmailTemplate template = templateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with ID: " + request.getTemplateId()));

        if (!template.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Template not found with ID: " + request.getTemplateId());
        }

        Resume resume = null;
        if (request.getResumeId() != null) {
            resume = resumeRepository.findById(request.getResumeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Resume not found with ID: " + request.getResumeId()));

            if (!resume.getUser().getId().equals(user.getId())) {
                throw new ResourceNotFoundException("Resume not found with ID: " + request.getResumeId());
            }
        }

        Campaign campaign = Campaign.builder()
                .name(request.getName())
                .emailTemplate(template)
                .resume(resume)
                .isEnabled(request.isEnabled())
                .targetSet(request.getTargetSet())
                .targetTitleGroup(request.getTargetTitleGroup() != null ? request.getTargetTitleGroup() : "ALL")
                .user(user)
                .build();

        Campaign saved = campaignRepository.save(campaign);
        
        if (saved.isEnabled()) {
            campaignRepository.disableOthers(saved.getId(), user.getId());
        }

        log.info("Created campaign ID: {} successfully", saved.getId());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public CampaignResponse updateCampaign(Long id, CampaignRequest request) {
        User user = securityUtils.getAuthenticatedUser();
        log.info("Updating campaign ID: {} for user: {}", id, user.getUsername());
        
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + id));

        if (!campaign.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Campaign not found with ID: " + id);
        }

        EmailTemplate template = templateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with ID: " + request.getTemplateId()));

        if (!template.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Template not found with ID: " + request.getTemplateId());
        }

        Resume resume = null;
        if (request.getResumeId() != null) {
            resume = resumeRepository.findById(request.getResumeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Resume not found with ID: " + request.getResumeId()));

            if (!resume.getUser().getId().equals(user.getId())) {
                throw new ResourceNotFoundException("Resume not found with ID: " + request.getResumeId());
            }
        }

        campaign.setName(request.getName());
        campaign.setEmailTemplate(template);
        campaign.setResume(resume);
        campaign.setEnabled(request.isEnabled());
        campaign.setTargetSet(request.getTargetSet());
        campaign.setTargetTitleGroup(request.getTargetTitleGroup() != null ? request.getTargetTitleGroup() : "ALL");

        Campaign updated = campaignRepository.save(campaign);

        if (updated.isEnabled()) {
            campaignRepository.disableOthers(updated.getId(), user.getId());
        }

        log.info("Updated campaign ID: {} successfully", id);
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public CampaignResponse getCampaignById(Long id) {
        User user = securityUtils.getAuthenticatedUser();
        log.info("Fetching campaign ID: {} for user: {}", id, user.getUsername());
        
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + id));

        if (!campaign.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Campaign not found with ID: " + id);
        }

        return mapToResponse(campaign);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CampaignResponse> getAllCampaigns() {
        User user = securityUtils.getAuthenticatedUser();
        log.info("Fetching all campaigns for user: {}", user.getUsername());
        return campaignRepository.findByUser(user).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteCampaign(Long id) {
        User user = securityUtils.getAuthenticatedUser();
        log.info("Deleting campaign ID: {} for user: {}", id, user.getUsername());
        
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + id));

        if (!campaign.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Campaign not found with ID: " + id);
        }

        emailHistoryRepository.deleteByCampaignIdAndUserId(id, user.getId());
        campaignRepository.delete(campaign);
        log.info("Deleted campaign ID: {} successfully", id);
    }

    @Override
    @Transactional
    public CampaignResponse enableCampaign(Long id) {
        User user = securityUtils.getAuthenticatedUser();
        log.info("Enabling campaign ID: {} for user: {}", id, user.getUsername());
        
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + id));

        if (!campaign.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Campaign not found with ID: " + id);
        }

        campaignRepository.disableOthers(id, user.getId());
        campaign.setEnabled(true);
        Campaign updated = campaignRepository.save(campaign);
        log.info("Campaign ID: {} enabled successfully", id);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public CampaignResponse disableCampaign(Long id) {
        User user = securityUtils.getAuthenticatedUser();
        log.info("Disabling campaign ID: {} for user: {}", id, user.getUsername());
        
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + id));

        if (!campaign.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Campaign not found with ID: " + id);
        }

        campaign.setEnabled(false);
        Campaign updated = campaignRepository.save(campaign);
        log.info("Campaign ID: {} disabled successfully", id);
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public PreviewResponse previewCampaignEmail(Long campaignId, Long recruiterId) {
        User user = securityUtils.getAuthenticatedUser();
        log.info("Generating email preview for campaign ID: {} and recruiter ID: {} by user: {}", campaignId, recruiterId, user.getUsername());
        
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + campaignId));

        if (!campaign.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Campaign not found with ID: " + campaignId);
        }

        Recruiter recruiter = recruiterRepository.findById(recruiterId)
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter not found with ID: " + recruiterId));

        String subject = mailService.previewEmailSubject(recruiterId, campaign.getEmailTemplate().getId());
        String body = mailService.previewEmailBody(recruiterId, campaign.getEmailTemplate().getId());

        return PreviewResponse.builder()
                .recipientEmail(recruiter.getEmail())
                .subject(subject)
                .body(body)
                .build();
    }

    @Override
    public void triggerCampaignManually(Long campaignId, Long recruiterId) {
        User user = securityUtils.getAuthenticatedUser();
        log.info("Manually triggering campaign ID: {} for recruiter ID: {} by user: {}", campaignId, recruiterId, user.getUsername());
        
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + campaignId));

        if (!campaign.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Campaign not found with ID: " + campaignId);
        }

        try {
            boolean success = mailService.sendOutreachEmail(
                    recruiterId, 
                    campaign.getEmailTemplate().getId(), 
                    campaign.getResume() != null ? campaign.getResume().getId() : null, 
                    campaignId
            );
            if (!success) {
                com.referral.outreach.entity.EmailHistory latestHistory = emailHistoryRepository
                        .findTopByCampaignIdAndRecruiterIdOrderBySentTimestampDesc(campaignId, recruiterId)
                        .orElse(null);
                String errorMsg = (latestHistory != null && latestHistory.getErrorMessage() != null) 
                        ? latestHistory.getErrorMessage() 
                        : "Resend API delivery failed";
                throw new MailSendingException("Failed to send outreach email to recruiter ID: " + recruiterId + ". Resend Error: " + errorMsg);
            }
            log.info("Manually triggered campaign ID: {} completed successfully", campaignId);
        } catch (MailSendingException mse) {
            throw mse;
        } catch (Exception ex) {
            log.error("Failed to execute manual campaign trigger for campaign ID: {} and recruiter ID: {}", campaignId, recruiterId, ex);
            throw new MailSendingException("Failed to send outreach email through Resend: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional
    public int triggerCampaignBatch(Long campaignId, int limit) {
        User user = securityUtils.getAuthenticatedUser();
        log.info("Batch triggering campaign ID: {} with limit: {} by user: {}", campaignId, limit, user.getUsername());
        
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + campaignId));

        if (!campaign.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Campaign not found with ID: " + campaignId);
        }

        if (campaign.getEmailTemplate() == null) {
            throw new IllegalArgumentException("Campaign is missing email template association");
        }

        LocalDateTime cooldownLimit = LocalDateTime.now().minusDays(cooldownDays);
        String groupStr = campaign.getTargetTitleGroup() != null ? campaign.getTargetTitleGroup() : "ALL";
        List<String> groups = java.util.Arrays.stream(groupStr.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
        boolean isAllGroup = groups.contains("ALL") || groups.isEmpty();

        List<Recruiter> eligibleRecruiters = recruiterRepository.findEligibleRecruitersFiltered(
                com.referral.outreach.entity.RecruiterStatus.ACTIVE, 
                cooldownLimit,
                campaign.getTargetSet(),
                groups,
                isAllGroup
        );

        List<Recruiter> batch = eligibleRecruiters.stream()
                .limit(limit)
                .collect(Collectors.toList());

        if (batch.isEmpty()) {
            return 0;
        }

        org.springframework.security.core.context.SecurityContext securityContext = 
                org.springframework.security.core.context.SecurityContextHolder.getContext();

        // Run the dispatches asynchronously in a background thread to prevent HTTP blocking
        new Thread(() -> {
            try {
                if (securityContext != null) {
                    org.springframework.security.core.context.SecurityContextHolder.setContext(securityContext);
                }
                log.info("Starting background batch outreach of size {} for campaign ID: {} by user: {}", batch.size(), campaignId, user.getUsername());
                int successCount = 0;
                int failureCount = 0;
                for (Recruiter recruiter : batch) {
                    try {
                        // Check if campaign was disabled mid-flight
                        Campaign current = campaignRepository.findById(campaignId).orElse(null);
                        if (current == null || !current.isEnabled()) {
                            log.info("Campaign ID: {} was disabled or deleted. Aborting batch outreach.", campaignId);
                            break;
                        }

                        boolean success = mailService.sendOutreachEmail(
                                recruiter.getId(),
                                campaign.getEmailTemplate().getId(),
                                campaign.getResume() != null ? campaign.getResume().getId() : null,
                                campaignId
                        );
                        if (success) {
                            successCount++;
                        } else {
                            failureCount++;
                        }

                        // 2-second rate-limiting delay between dispatches
                        Thread.sleep(2000);
                    } catch (InterruptedException ie) {
                        log.warn("Batch outreach thread interrupted.");
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception ex) {
                        failureCount++;
                        log.error("Failed to send batch outreach email to recruiter: {} ({})", 
                                recruiter.getName(), recruiter.getEmail(), ex);
                    }
                }
                log.info("Finished background batch outreach for campaign ID: {}. Success: {}, Failures: {}", 
                        campaignId, successCount, failureCount);
            } finally {
                org.springframework.security.core.context.SecurityContextHolder.clearContext();
            }
        }).start();

        return batch.size();
    }

    private CampaignResponse mapToResponse(Campaign campaign) {
        return CampaignResponse.builder()
                .id(campaign.getId())
                .name(campaign.getName())
                .templateId(campaign.getEmailTemplate() != null ? campaign.getEmailTemplate().getId() : null)
                .templateName(campaign.getEmailTemplate() != null ? campaign.getEmailTemplate().getTemplateName() : null)
                .resumeId(campaign.getResume() != null ? campaign.getResume().getId() : null)
                .resumeFilename(campaign.getResume() != null ? campaign.getResume().getOriginalFilename() : null)
                .isEnabled(campaign.isEnabled())
                .targetSet(campaign.getTargetSet())
                .targetTitleGroup(campaign.getTargetTitleGroup())
                .createdTimestamp(campaign.getCreatedTimestamp())
                .build();
    }
}
