package com.referral.outreach.service;

public interface MailService {
    void sendOutreachEmail(Long recruiterId, Long templateId, Long resumeId, Long campaignId);
    String previewEmailBody(Long recruiterId, Long templateId);
    String previewEmailSubject(Long recruiterId, Long templateId);
}
