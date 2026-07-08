package com.referral.outreach.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignResponse {
    private Long id;
    private String name;
    private Long templateId;
    private String templateName;
    private Long resumeId;
    private String resumeFilename;
    private boolean isEnabled;
    private LocalDateTime createdTimestamp;
}
