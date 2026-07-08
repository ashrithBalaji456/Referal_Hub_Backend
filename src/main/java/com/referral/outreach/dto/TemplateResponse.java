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
public class TemplateResponse {
    private Long id;
    private String templateName;
    private String subject;
    private String body;
    private LocalDateTime createdTimestamp;
    private LocalDateTime updatedTimestamp;
}
