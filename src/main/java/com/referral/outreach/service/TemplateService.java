package com.referral.outreach.service;

import com.referral.outreach.dto.TemplateRequest;
import com.referral.outreach.dto.TemplateResponse;
import java.util.List;

public interface TemplateService {
    TemplateResponse createTemplate(TemplateRequest request);
    TemplateResponse updateTemplate(Long id, TemplateRequest request);
    TemplateResponse getTemplateById(Long id);
    List<TemplateResponse> getAllTemplates();
    void deleteTemplate(Long id);
}
