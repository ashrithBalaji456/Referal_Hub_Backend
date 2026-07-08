package com.referral.outreach.repository;

import com.referral.outreach.entity.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    Optional<Campaign> findByIsEnabledTrue();

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Campaign c SET c.isEnabled = false WHERE c.id != :id")
    void disableOthers(Long id);
}
