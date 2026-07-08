package com.referral.outreach.repository;

import com.referral.outreach.entity.Recruiter;
import com.referral.outreach.entity.RecruiterStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecruiterRepository extends JpaRepository<Recruiter, Long> {

    Optional<Recruiter> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    List<Recruiter> findByStatus(RecruiterStatus status);

    @Query("SELECT r FROM Recruiter r WHERE r.status = :status AND " +
           "(r.lastContactedDate IS NULL OR r.lastContactedDate < :cooldownDate)")
    List<Recruiter> findEligibleRecruiters(
            @Param("status") RecruiterStatus status,
            @Param("cooldownDate") LocalDateTime cooldownDate
    );
}
