package com.referral.outreach.service.impl;

import com.referral.outreach.dto.TemplateRequest;
import com.referral.outreach.dto.TemplateResponse;
import com.referral.outreach.entity.EmailTemplate;
import com.referral.outreach.entity.User;
import com.referral.outreach.exception.DuplicateTemplateException;
import com.referral.outreach.exception.ResourceNotFoundException;
import com.referral.outreach.repository.TemplateRepository;
import com.referral.outreach.security.SecurityUtils;
import com.referral.outreach.service.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {

    private final TemplateRepository templateRepository;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public TemplateResponse createTemplate(TemplateRequest request) {
        User user = securityUtils.getAuthenticatedUser();
        log.info("Creating email template with name: {} for user: {}", request.getTemplateName(), user.getUsername());
        
        if (templateRepository.existsByUserAndTemplateName(user, request.getTemplateName())) {
            throw new DuplicateTemplateException("Template with name " + request.getTemplateName() + " already exists");
        }

        EmailTemplate template = EmailTemplate.builder()
                .templateName(request.getTemplateName())
                .subject(request.getSubject())
                .body(request.getBody())
                .user(user)
                .build();

        EmailTemplate savedTemplate = templateRepository.save(template);
        log.info("Created template successfully with ID: {}", savedTemplate.getId());
        return mapToResponse(savedTemplate);
    }

    @Override
    @Transactional
    public TemplateResponse updateTemplate(Long id, TemplateRequest request) {
        User user = securityUtils.getAuthenticatedUser();
        log.info("Updating email template ID: {} for user: {}", id, user.getUsername());
        
        EmailTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with ID: " + id));

        if (!template.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Template not found with ID: " + id);
        }

        if (templateRepository.existsByUserAndTemplateNameAndIdNot(user, request.getTemplateName(), id)) {
            throw new DuplicateTemplateException("Another template with name " + request.getTemplateName() + " already exists");
        }

        template.setTemplateName(request.getTemplateName());
        template.setSubject(request.getSubject());
        template.setBody(request.getBody());

        EmailTemplate updatedTemplate = templateRepository.save(template);
        log.info("Updated template ID: {} successfully", id);
        return mapToResponse(updatedTemplate);
    }

    @Override
    @Transactional(readOnly = true)
    public TemplateResponse getTemplateById(Long id) {
        User user = securityUtils.getAuthenticatedUser();
        log.info("Fetching template ID: {} for user: {}", id, user.getUsername());
        
        EmailTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with ID: " + id));

        if (!template.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Template not found with ID: " + id);
        }

        return mapToResponse(template);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateResponse> getAllTemplates() {
        User user = securityUtils.getAuthenticatedUser();
        log.info("Fetching all templates for user: {}", user.getUsername());
        return templateRepository.findByUser(user).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteTemplate(Long id) {
        User user = securityUtils.getAuthenticatedUser();
        log.info("Deleting template ID: {} for user: {}", id, user.getUsername());
        
        EmailTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with ID: " + id));

        if (!template.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Template not found with ID: " + id);
        }

        templateRepository.delete(template);
        log.info("Deleted template ID: {} successfully", id);
    }

    private TemplateResponse mapToResponse(EmailTemplate template) {
        return TemplateResponse.builder()
                .id(template.getId())
                .templateName(template.getTemplateName())
                .subject(template.getSubject())
                .body(template.getBody())
                .createdTimestamp(template.getCreatedTimestamp())
                .updatedTimestamp(template.getUpdatedTimestamp())
                .build();
    }
}
