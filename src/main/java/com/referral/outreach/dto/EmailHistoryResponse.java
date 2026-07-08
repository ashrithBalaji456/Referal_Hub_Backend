package com.referral.outreach.dto;

import com.referral.outreach.entity.EmailHistoryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailHistoryResponse {
    private Long id;
    private Long recruiterId;
    private String recruiterName;
    private String recruiterCompany;
    private Long campaignId;
    private String campaignName;
    private String recipientEmail;
    private String subjectUsed;
    private LocalDateTime sentTimestamp;
    private EmailHistoryStatus status;
    private String errorMessage;
}
