package com.referral.outreach.service.impl;

import com.referral.outreach.dto.EmailHistoryResponse;
import com.referral.outreach.entity.EmailHistory;
import com.referral.outreach.entity.EmailHistoryStatus;
import com.referral.outreach.repository.EmailHistoryRepository;
import com.referral.outreach.service.EmailHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailHistoryServiceImpl implements EmailHistoryService {

    private final EmailHistoryRepository emailHistoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<EmailHistoryResponse> getFilteredHistory(
            Long recruiterId,
            String company,
            EmailHistoryStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        log.info("Fetching filtered email history: recruiterId={}, company={}, status={}, startDate={}, endDate={}",
                recruiterId, company, status, startDate, endDate);

        Specification<EmailHistory> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (recruiterId != null) {
                predicates.add(cb.equal(root.get("recruiter").get("id"), recruiterId));
            }
            if (company != null && !company.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("recruiter").get("company")), "%" + company.toLowerCase() + "%"));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("sentTimestamp"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("sentTimestamp"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return emailHistoryRepository.findAll(spec).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private EmailHistoryResponse mapToResponse(EmailHistory history) {
        return EmailHistoryResponse.builder()
                .id(history.getId())
                .recruiterId(history.getRecruiter().getId())
                .recruiterName(history.getRecruiter().getName())
                .recruiterCompany(history.getRecruiter().getCompany())
                .campaignId(history.getCampaign() != null ? history.getCampaign().getId() : null)
                .campaignName(history.getCampaign() != null ? history.getCampaign().getName() : null)
                .recipientEmail(history.getRecipientEmail())
                .subjectUsed(history.getSubjectUsed())
                .sentTimestamp(history.getSentTimestamp())
                .status(history.getStatus())
                .errorMessage(history.getErrorMessage())
                .build();
    }
}
