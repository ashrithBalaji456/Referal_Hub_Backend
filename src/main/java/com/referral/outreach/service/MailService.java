package com.referral.outreach.service;

public interface MailService {
    boolean sendOutreachEmail(Long recruiterId, Long templateId, Long resumeId, Long campaignId);
    String previewEmailBody(Long recruiterId, Long templateId);
    String previewEmailSubject(Long recruiterId, Long templateId);
    void sendPasswordResetEmail(String recipientEmail, String token);
}
