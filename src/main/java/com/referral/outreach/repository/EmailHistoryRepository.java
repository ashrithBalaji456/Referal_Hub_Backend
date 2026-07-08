package com.referral.outreach.repository;

import com.referral.outreach.entity.EmailHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailHistoryRepository extends JpaRepository<EmailHistory, Long>, JpaSpecificationExecutor<EmailHistory> {
    void deleteByCampaignId(Long campaignId);
}
