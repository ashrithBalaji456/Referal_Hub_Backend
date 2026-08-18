package com.referral.outreach.service.impl;

import com.referral.outreach.entity.*;
import com.referral.outreach.exception.MailSendingException;
import com.referral.outreach.exception.ResourceNotFoundException;
import com.referral.outreach.repository.*;
import com.referral.outreach.service.MailService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.referral.outreach.service.CandidateProfileService;
import com.referral.outreach.util.TemplateParser;
import com.resend.Resend;
import com.resend.services.emails.model.Attachment;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final Resend resend;
    private final RecruiterRepository recruiterRepository;
    private final TemplateRepository templateRepository;
    private final ResumeRepository resumeRepository;
    private final CampaignRepository campaignRepository;
    private final EmailHistoryRepository emailHistoryRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Value("${resend.from.email:onboarding@resend.dev}")
    private String fromEmail;

    @Value("${app.candidate-name:Gudla Ashrith Balaji}")
    private String candidateName;

    @Value("${app.target-role:Java Backend Developer}")
    private String targetRole;

    private java.util.Map<String, String> getCandidateVariables(User user) {
        java.util.Map<String, String> vars = new java.util.HashMap<>();
        
        CandidateProfile profile = null;
        if (user != null) {
            profile = candidateProfileRepository.findByUser(user).orElse(null);
        }
        
        String nameVal = profile != null ? profile.getFullName() : null;
        if (nameVal == null || nameVal.trim().isEmpty()) {
            nameVal = candidateName;
        }
        String roleVal = profile != null ? profile.getRoleName() : null;
        if (roleVal == null || roleVal.trim().isEmpty()) {
            roleVal = targetRole;
        }
        
        vars.put("candidateName", nameVal);
        vars.put("roleName", roleVal);
        vars.put("email", (profile != null && profile.getEmail() != null) ? profile.getEmail() : "ashrithbalajigudla@gmail.com");
        vars.put("linkedin", (profile != null && profile.getLinkedinUrl() != null) ? profile.getLinkedinUrl() : "");
        vars.put("github", (profile != null && profile.getGithubUrl() != null) ? profile.getGithubUrl() : "");
        vars.put("phoneNumber", (profile != null && profile.getPhoneNumber() != null) ? profile.getPhoneNumber() : "");
        vars.put("location", (profile != null && profile.getLocation() != null) ? profile.getLocation() : "");
        
        vars.put("candidate_name", nameVal);
        vars.put("role_name", roleVal);
        vars.put("linkedinUrl", (profile != null && profile.getLinkedinUrl() != null) ? profile.getLinkedinUrl() : "");
        vars.put("githubUrl", (profile != null && profile.getGithubUrl() != null) ? profile.getGithubUrl() : "");
        vars.put("phone", (profile != null && profile.getPhoneNumber() != null) ? profile.getPhoneNumber() : "");
        
        if (profile != null && profile.getCustomFieldsJson() != null && !profile.getCustomFieldsJson().isEmpty()) {
            try {
                java.util.Map<String, String> customMap = objectMapper.readValue(
                        profile.getCustomFieldsJson(),
                        new TypeReference<java.util.Map<String, String>>() {}
                );
                if (customMap != null) {
                    vars.putAll(customMap);
                }
            } catch (Exception e) {
                log.error("Failed to parse custom fields JSON", e);
            }
        }
        
        return vars;
    }

    @Override
    @Transactional
    public boolean sendOutreachEmail(Long recruiterId, Long templateId, Long resumeId, Long campaignId) {
        Recruiter recruiter = recruiterRepository.findById(recruiterId)
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter not found with ID: " + recruiterId));

        EmailTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with ID: " + templateId));

        Resume resume = null;
        if (resumeId != null) {
            resume = resumeRepository.findById(resumeId).orElse(null);
        }

        Campaign campaign = null;
        if (campaignId != null) {
            campaign = campaignRepository.findById(campaignId).orElse(null);
        }

        // Determine user context safely (campaign owner or authenticated context)
        User user = null;
        if (campaign != null) {
            user = campaign.getUser();
        }
        if (user == null) {
            org.springframework.security.core.Authentication auth = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof com.referral.outreach.security.UserPrincipal) {
                com.referral.outreach.security.UserPrincipal principal = (com.referral.outreach.security.UserPrincipal) auth.getPrincipal();
                user = userRepository.findById(principal.getId()).orElse(null);
            }
        }
        if (user == null && template.getUser() != null) {
            user = template.getUser();
        }

        java.util.Map<String, String> vars = getCandidateVariables(user);

        String compiledSubject = TemplateParser.compile(
                template.getSubject(),
                recruiter.getName(),
                recruiter.getCompany(),
                vars
        );

        String compiledBody = TemplateParser.compile(
                template.getBody(),
                recruiter.getName(),
                recruiter.getCompany(),
                vars
        );

        log.info("Sending email through Resend to recruiter: {} ({}) for company: {}", 
                recruiter.getName(), recruiter.getEmail(), recruiter.getCompany());

        try {
            Attachment attachment = null;
            if (resume != null && resume.getFilePath() != null) {
                File file = new File(resume.getFilePath());
                if (file.exists()) {
                    byte[] fileBytes = Files.readAllBytes(file.toPath());
                    String base64Content = java.util.Base64.getEncoder().encodeToString(fileBytes);
                    attachment = Attachment.builder()
                            .fileName(resume.getOriginalFilename())
                            .content(base64Content)
                            .build();
                    log.info("Attached resume: {} (size: {} bytes)", resume.getOriginalFilename(), fileBytes.length);
                } else {
                    log.warn("Physical resume file not found at {}. Sending email without resume attachment.", resume.getFilePath());
                }
            } else {
                log.info("No active resume attached to campaign. Sending email without resume attachment.");
            }

            CreateEmailOptions.Builder optionsBuilder = CreateEmailOptions.builder()
                    .from(fromEmail)
                    .to(recruiter.getEmail())
                    .subject(compiledSubject)
                    .text(compiledBody);

            if (attachment != null) {
                optionsBuilder.attachments(attachment);
            }

            CreateEmailOptions options = optionsBuilder.build();

            log.info("Executing Resend API call -> From: {}, To: {}, Subject: {}", fromEmail, recruiter.getEmail(), compiledSubject);
            CreateEmailResponse response = resend.emails().send(options);
            String resendId = response != null ? response.getId() : "unknown";

            log.info("Email successfully sent through Resend. Recipient: {}, Resend ID: {}", recruiter.getEmail(), resendId);

            // Log Success
            recruiter.setLastContactedDate(LocalDateTime.now());
            recruiterRepository.save(recruiter);

            EmailHistory history = EmailHistory.builder()
                    .recruiter(recruiter)
                    .campaign(campaign)
                    .recipientEmail(recruiter.getEmail())
                    .subjectUsed(compiledSubject)
                    .status(EmailHistoryStatus.SUCCESS)
                    .user(user)
                    .build();
            emailHistoryRepository.save(history);

            return true;

        } catch (Exception ex) {
            log.error("Resend email delivery failed for recipient: {} (Company: {}). Root cause: {}", 
                    recruiter.getEmail(), recruiter.getCompany(), ex.getMessage(), ex);

            EmailHistory history = EmailHistory.builder()
                    .recruiter(recruiter)
                    .campaign(campaign)
                    .recipientEmail(recruiter.getEmail())
                    .subjectUsed(compiledSubject)
                    .status(EmailHistoryStatus.FAILED)
                    .errorMessage(ex.getMessage())
                    .user(user)
                    .build();
            emailHistoryRepository.save(history);

            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String previewEmailBody(Long recruiterId, Long templateId) {
        Recruiter recruiter = recruiterRepository.findById(recruiterId)
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter not found with ID: " + recruiterId));

        EmailTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with ID: " + templateId));

        User user = null;
        org.springframework.security.core.Authentication auth = 
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof com.referral.outreach.security.UserPrincipal) {
            com.referral.outreach.security.UserPrincipal principal = (com.referral.outreach.security.UserPrincipal) auth.getPrincipal();
            user = userRepository.findById(principal.getId()).orElse(null);
        }
        if (user == null && template.getUser() != null) {
            user = template.getUser();
        }

        java.util.Map<String, String> vars = getCandidateVariables(user);

        return TemplateParser.compile(
                template.getBody(),
                recruiter.getName(),
                recruiter.getCompany(),
                vars
        );
    }

    @Override
    @Transactional(readOnly = true)
    public String previewEmailSubject(Long recruiterId, Long templateId) {
        Recruiter recruiter = recruiterRepository.findById(recruiterId)
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter not found with ID: " + recruiterId));

        EmailTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with ID: " + templateId));

        User user = null;
        org.springframework.security.core.Authentication auth = 
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof com.referral.outreach.security.UserPrincipal) {
            com.referral.outreach.security.UserPrincipal principal = (com.referral.outreach.security.UserPrincipal) auth.getPrincipal();
            user = userRepository.findById(principal.getId()).orElse(null);
        }
        if (user == null && template.getUser() != null) {
            user = template.getUser();
        }

        java.util.Map<String, String> vars = getCandidateVariables(user);

        return TemplateParser.compile(
                template.getSubject(),
                recruiter.getName(),
                recruiter.getCompany(),
                vars
        );
    }

    @Override
    public void sendPasswordResetEmail(String recipientEmail, String token) {
        log.info("Sending password reset email to: {}", recipientEmail);
        try {
            String resetLink = "http://localhost:5173/reset-password?token=" + token;
            log.info("==================================================================");
            log.info("PASSWORD RESET LINK GENERATED FOR [{}]:", recipientEmail);
            log.info("--> {}", resetLink);
            log.info("==================================================================");
            
            String htmlContent = "<div style='font-family: sans-serif; max-width: 500px; margin: 0 auto; padding: 20px; border: 1px solid #e2e8f0; border-radius: 8px;'>" +
                    "<h2 style='color: #4f46e5; margin-bottom: 20px;'>Password Reset Request</h2>" +
                    "<p>Hello,</p>" +
                    "<p>We received a request to reset your password for your Outreach Portal account. Click the button below to set a new password:</p>" +
                    "<div style='text-align: center; margin: 30px 0;'>" +
                    "<a href='" + resetLink + "' style='background-color: #4f46e5; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block;'>Reset Password</a>" +
                    "</div>" +
                    "<p>If you did not request this, you can safely ignore this email.</p>" +
                    "<hr style='border: 0; border-top: 1px solid #e2e8f0; margin: 20px 0;' />" +
                    "<p style='font-size: 12px; color: #64748b;'>If the button above does not work, copy and paste this URL into your browser:</p>" +
                    "<p style='font-size: 12px; color: #64748b; word-break: break-all;'>" + resetLink + "</p>" +
                    "</div>";
            
            CreateEmailOptions options = CreateEmailOptions.builder()
                    .from(fromEmail)
                    .to(recipientEmail)
                    .subject("Outreach Portal - Password Reset Request")
                    .html(htmlContent)
                    .build();

            CreateEmailResponse response = resend.emails().send(options);
            log.info("Password reset email sent via Resend to: {}, response ID: {}", recipientEmail, response != null ? response.getId() : "null");
            log.info("Password reset email sent successfully to: {}", recipientEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}. Error: {}", recipientEmail, e.getMessage());
            throw new MailSendingException("Failed to send password reset email: " + e.getMessage(), e);
        }
    }
}
