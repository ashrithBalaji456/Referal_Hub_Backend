package com.referral.outreach.service.impl;

import com.referral.outreach.entity.*;
import com.referral.outreach.exception.MailSendingException;
import com.referral.outreach.exception.ResourceNotFoundException;
import com.referral.outreach.repository.*;
import com.referral.outreach.service.MailService;
import com.referral.outreach.util.TemplateParser;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;
    private final RecruiterRepository recruiterRepository;
    private final TemplateRepository templateRepository;
    private final ResumeRepository resumeRepository;
    private final CampaignRepository campaignRepository;
    private final EmailHistoryRepository emailHistoryRepository;

    @Value("${app.candidate-name:Gudla Ashrith Balaji}")
    private String candidateName;

    @Override
    @Transactional
    public void sendOutreachEmail(Long recruiterId, Long templateId, Long resumeId, Long campaignId) {
        Recruiter recruiter = recruiterRepository.findById(recruiterId)
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter not found with ID: " + recruiterId));

        EmailTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with ID: " + templateId));

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with ID: " + resumeId));

        Campaign campaign = null;
        if (campaignId != null) {
            campaign = campaignRepository.findById(campaignId)
                    .orElse(null);
        }

        String roleName = recruiter.getRoleCategory() == RoleCategory.JAVA_BACKEND_DEVELOPER 
                ? "Java Backend Developer" 
                : "Spring Boot Developer";

        String compiledSubject = TemplateParser.compile(
                template.getSubject(),
                recruiter.getName(),
                recruiter.getCompany(),
                candidateName,
                roleName
        );

        String compiledBody = TemplateParser.compile(
                template.getBody(),
                recruiter.getName(),
                recruiter.getCompany(),
                candidateName,
                roleName
        );

        log.info("Attempting to send email to recruiter: {} ({}) for company: {}", 
                recruiter.getName(), recruiter.getEmail(), recruiter.getCompany());

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(recruiter.getEmail());
            helper.setSubject(compiledSubject);
            helper.setText(compiledBody, false); // Sends plain text

            File file = new File(resume.getFilePath());
            if (!file.exists()) {
                throw new ResourceNotFoundException("Physical resume file not found at " + resume.getFilePath());
            }
            FileSystemResource fileResource = new FileSystemResource(file);
            helper.addAttachment(resume.getOriginalFilename(), fileResource);

            mailSender.send(mimeMessage);

            // Log Success
            recruiter.setLastContactedDate(LocalDateTime.now());
            recruiterRepository.save(recruiter);

            EmailHistory history = EmailHistory.builder()
                    .recruiter(recruiter)
                    .campaign(campaign)
                    .recipientEmail(recruiter.getEmail())
                    .subjectUsed(compiledSubject)
                    .status(EmailHistoryStatus.SUCCESS)
                    .build();
            emailHistoryRepository.save(history);

            log.info("Email sent successfully to: {}", recruiter.getEmail());

        } catch (Exception ex) {
            log.error("Failed to send email to: {}", recruiter.getEmail(), ex);

            // Log Failure
            EmailHistory history = EmailHistory.builder()
                    .recruiter(recruiter)
                    .campaign(campaign)
                    .recipientEmail(recruiter.getEmail())
                    .subjectUsed(compiledSubject)
                    .status(EmailHistoryStatus.FAILED)
                    .errorMessage(ex.getMessage())
                    .build();
            emailHistoryRepository.save(history);

            throw new MailSendingException("Failed to send email to: " + recruiter.getEmail(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String previewEmailBody(Long recruiterId, Long templateId) {
        Recruiter recruiter = recruiterRepository.findById(recruiterId)
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter not found with ID: " + recruiterId));

        EmailTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with ID: " + templateId));

        String roleName = recruiter.getRoleCategory() == RoleCategory.JAVA_BACKEND_DEVELOPER 
                ? "Java Backend Developer" 
                : "Spring Boot Developer";

        return TemplateParser.compile(
                template.getBody(),
                recruiter.getName(),
                recruiter.getCompany(),
                candidateName,
                roleName
        );
    }

    @Override
    @Transactional(readOnly = true)
    public String previewEmailSubject(Long recruiterId, Long templateId) {
        Recruiter recruiter = recruiterRepository.findById(recruiterId)
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter not found with ID: " + recruiterId));

        EmailTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with ID: " + templateId));

        String roleName = recruiter.getRoleCategory() == RoleCategory.JAVA_BACKEND_DEVELOPER 
                ? "Java Backend Developer" 
                : "Spring Boot Developer";

        return TemplateParser.compile(
                template.getSubject(),
                recruiter.getName(),
                recruiter.getCompany(),
                candidateName,
                roleName
        );
    }
}
