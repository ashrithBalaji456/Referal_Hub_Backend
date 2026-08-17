package com.referral.outreach.repository;

import com.referral.outreach.entity.Campaign;
import com.referral.outreach.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    List<Campaign> findByUser(User user);

    Optional<Campaign> findByIsEnabledTrue();
    List<Campaign> findAllByIsEnabledTrue();
    Optional<Campaign> findByUserAndIsEnabledTrue(User user);

    boolean existsByResumeId(Long resumeId);
    boolean existsByUserAndResumeId(User user, Long resumeId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Campaign c SET c.isEnabled = false WHERE c.id != :id AND c.user.id = :userId")
    void disableOthers(Long id, Long userId);
}
