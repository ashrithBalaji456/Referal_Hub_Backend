package com.referral.outreach.service.impl;

import com.referral.outreach.entity.*;
import com.referral.outreach.exception.MailSendingException;
import com.referral.outreach.exception.ResourceNotFoundException;
import com.referral.outreach.repository.*;
import com.referral.outreach.service.BrevoClient;
import com.referral.outreach.service.MailService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.referral.outreach.util.TemplateParser;
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

    private final BrevoClient brevoClient;
    private final RecruiterRepository recruiterRepository;
    private final TemplateRepository templateRepository;
    private final ResumeRepository resumeRepository;
    private final CampaignRepository campaignRepository;
    private final EmailHistoryRepository emailHistoryRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

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

        log.info("Sending email through Brevo to recruiter: {} ({}) for company: {}", 
                recruiter.getName(), recruiter.getEmail(), recruiter.getCompany());

        try {
            String attachmentName = null;
            String base64Content = null;

            if (resume != null && resume.getFilePath() != null) {
                File file = new File(resume.getFilePath());
                if (file.exists()) {
                    byte[] fileBytes = Files.readAllBytes(file.toPath());
                    base64Content = java.util.Base64.getEncoder().encodeToString(fileBytes);
                    attachmentName = resume.getOriginalFilename();
                    log.info("Attached resume: {} (size: {} bytes)", resume.getOriginalFilename(), fileBytes.length);
                } else {
                    log.warn("Physical resume file not found at {}. Sending email without resume attachment.", resume.getFilePath());
                }
            } else {
                log.info("No active resume attached to campaign. Sending email without resume attachment.");
            }

            String messageId = brevoClient.sendEmail(
                    recruiter.getEmail(),
                    recruiter.getName(),
                    compiledSubject,
                    compiledBody,
                    null,
                    attachmentName,
                    base64Content
            );

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

            log.info("Email sent successfully through Brevo. Recipient: {}, Message ID: {}", recruiter.getEmail(), messageId);
            return true;

        } catch (Exception ex) {
            log.error("Brevo email delivery failed for recipient: {} (Company: {}). Root cause: {}", 
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

        User user = template.getUser();
        if (user == null) {
            org.springframework.security.core.Authentication auth = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof com.referral.outreach.security.UserPrincipal) {
                com.referral.outreach.security.UserPrincipal principal = (com.referral.outreach.security.UserPrincipal) auth.getPrincipal();
                user = userRepository.findById(principal.getId()).orElse(null);
            }
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

        User user = template.getUser();
        if (user == null) {
            org.springframework.security.core.Authentication auth = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof com.referral.outreach.security.UserPrincipal) {
                com.referral.outreach.security.UserPrincipal principal = (com.referral.outreach.security.UserPrincipal) auth.getPrincipal();
                user = userRepository.findById(principal.getId()).orElse(null);
            }
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
    @Transactional
    public void sendPasswordResetEmail(String recipientEmail, String token) {
        log.info("Sending password reset email to: {}", recipientEmail);

        User user = userRepository.findByEmail(recipientEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + recipientEmail));

        String resetUrl = "https://referal-hub-frontend.vercel.app/reset-password?token=" + token;
        String subject = "Password Reset Request - Referral Hub";
        String body = "Hello,\n\nYou requested a password reset for your Referral Hub account.\n\n"
                + "Please click the link below to reset your password:\n"
                + resetUrl + "\n\n"
                + "If you did not request this, please ignore this email.\n\n"
                + "Regards,\nReferral Hub Team";

        try {
            brevoClient.sendEmail(recipientEmail, user.getUsername(), subject, body, null, null, null);
            log.info("Password reset email successfully sent through Brevo to: {}", recipientEmail);
        } catch (Exception ex) {
            log.error("Failed to send password reset email to: {}", recipientEmail, ex);
            throw new MailSendingException("Failed to send password reset email: " + ex.getMessage(), ex);
        }
    }
}
