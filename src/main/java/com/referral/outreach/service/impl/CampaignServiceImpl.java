package com.referral.outreach.service.impl;

import com.referral.outreach.dto.CampaignRequest;
import com.referral.outreach.dto.CampaignResponse;
import com.referral.outreach.dto.PreviewResponse;
import com.referral.outreach.entity.Campaign;
import com.referral.outreach.entity.EmailTemplate;
import com.referral.outreach.entity.Recruiter;
import com.referral.outreach.entity.Resume;
import com.referral.outreach.exception.ResourceNotFoundException;
import com.referral.outreach.repository.CampaignRepository;
import com.referral.outreach.repository.RecruiterRepository;
import com.referral.outreach.repository.TemplateRepository;
import com.referral.outreach.repository.ResumeRepository;
import com.referral.outreach.repository.EmailHistoryRepository;
import com.referral.outreach.service.CampaignService;
import com.referral.outreach.service.MailService;
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

    @Value("${app.scheduler.cooldown-days:30}")
    private int cooldownDays;

    @Override
    @Transactional
    public CampaignResponse createCampaign(CampaignRequest request) {
        log.info("Creating campaign: {}", request.getName());
        EmailTemplate template = templateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with ID: " + request.getTemplateId()));

        Resume resume = resumeRepository.findById(request.getResumeId())
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with ID: " + request.getResumeId()));

        Campaign campaign = Campaign.builder()
                .name(request.getName())
                .emailTemplate(template)
                .resume(resume)
                .isEnabled(request.isEnabled())
                .build();

        Campaign saved = campaignRepository.save(campaign);
        
        if (saved.isEnabled()) {
            campaignRepository.disableOthers(saved.getId());
        }

        log.info("Created campaign ID: {} successfully", saved.getId());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public CampaignResponse updateCampaign(Long id, CampaignRequest request) {
        log.info("Updating campaign ID: {}", id);
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + id));

        EmailTemplate template = templateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with ID: " + request.getTemplateId()));

        Resume resume = resumeRepository.findById(request.getResumeId())
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with ID: " + request.getResumeId()));

        campaign.setName(request.getName());
        campaign.setEmailTemplate(template);
        campaign.setResume(resume);
        campaign.setEnabled(request.isEnabled());

        Campaign updated = campaignRepository.save(campaign);

        if (updated.isEnabled()) {
            campaignRepository.disableOthers(updated.getId());
        }

        log.info("Updated campaign ID: {} successfully", id);
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public CampaignResponse getCampaignById(Long id) {
        log.info("Fetching campaign ID: {}", id);
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + id));
        return mapToResponse(campaign);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CampaignResponse> getAllCampaigns() {
        log.info("Fetching all campaigns");
        return campaignRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteCampaign(Long id) {
        log.info("Deleting campaign ID: {}", id);
        if (!campaignRepository.existsById(id)) {
            throw new ResourceNotFoundException("Campaign not found with ID: " + id);
        }
        emailHistoryRepository.deleteByCampaignId(id);
        campaignRepository.deleteById(id);
        log.info("Deleted campaign ID: {} successfully", id);
    }

    @Override
    @Transactional
    public CampaignResponse enableCampaign(Long id) {
        log.info("Enabling campaign ID: {}", id);
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + id));

        campaignRepository.disableOthers(id);
        campaign.setEnabled(true);
        Campaign updated = campaignRepository.save(campaign);
        log.info("Campaign ID: {} enabled successfully", id);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public CampaignResponse disableCampaign(Long id) {
        log.info("Disabling campaign ID: {}", id);
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + id));

        campaign.setEnabled(false);
        Campaign updated = campaignRepository.save(campaign);
        log.info("Campaign ID: {} disabled successfully", id);
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public PreviewResponse previewCampaignEmail(Long campaignId, Long recruiterId) {
        log.info("Generating email preview for campaign ID: {} and recruiter ID: {}", campaignId, recruiterId);
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + campaignId));

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
    @Transactional
    public void triggerCampaignManually(Long campaignId, Long recruiterId) {
        log.info("Manually triggering campaign ID: {} for recruiter ID: {}", campaignId, recruiterId);
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + campaignId));

        mailService.sendOutreachEmail(
                recruiterId, 
                campaign.getEmailTemplate().getId(), 
                campaign.getResume().getId(), 
                campaignId
        );
        log.info("Manually triggered campaign ID: {} completed successfully", campaignId);
    }

    @Override
    @Transactional
    public int triggerCampaignBatch(Long campaignId, int limit) {
        log.info("Batch triggering campaign ID: {} with limit: {}", campaignId, limit);
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + campaignId));

        if (campaign.getEmailTemplate() == null || campaign.getResume() == null) {
            throw new IllegalArgumentException("Campaign is missing template or resume association");
        }

        LocalDateTime cooldownLimit = LocalDateTime.now().minusDays(cooldownDays);
        List<Recruiter> eligibleRecruiters = recruiterRepository.findEligibleRecruiters(
                com.referral.outreach.entity.RecruiterStatus.ACTIVE, 
                cooldownLimit
        );

        List<Recruiter> batch = eligibleRecruiters.stream()
                .limit(limit)
                .collect(Collectors.toList());

        if (batch.isEmpty()) {
            return 0;
        }

        // Run the dispatches asynchronously in a background thread to prevent HTTP blocking
        new Thread(() -> {
            log.info("Starting background batch outreach of size {} for campaign ID: {}", batch.size(), campaignId);
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

                    mailService.sendOutreachEmail(
                            recruiter.getId(),
                            campaign.getEmailTemplate().getId(),
                            campaign.getResume().getId(),
                            campaignId
                    );
                    successCount++;

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
        }).start();

        return batch.size();
    }

    private CampaignResponse mapToResponse(Campaign campaign) {
        return CampaignResponse.builder()
                .id(campaign.getId())
                .name(campaign.getName())
                .templateId(campaign.getEmailTemplate().getId())
                .templateName(campaign.getEmailTemplate().getTemplateName())
                .resumeId(campaign.getResume().getId())
                .resumeFilename(campaign.getResume().getOriginalFilename())
                .isEnabled(campaign.isEnabled())
                .createdTimestamp(campaign.getCreatedTimestamp())
                .build();
    }
}
