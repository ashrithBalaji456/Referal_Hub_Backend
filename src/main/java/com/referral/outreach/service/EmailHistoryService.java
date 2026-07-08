package com.referral.outreach.service;

import com.referral.outreach.dto.EmailHistoryResponse;
import com.referral.outreach.entity.EmailHistoryStatus;
import java.time.LocalDateTime;
import java.util.List;

public interface EmailHistoryService {
    List<EmailHistoryResponse> getFilteredHistory(
            Long recruiterId,
            String company,
            EmailHistoryStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate
    );
}
