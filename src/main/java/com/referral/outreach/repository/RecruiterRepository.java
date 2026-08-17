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

    @Query("SELECT r FROM Recruiter r WHERE r.status = :status AND " +
           "(r.lastContactedDate IS NULL OR r.lastContactedDate < :cooldownDate) AND " +
           "(:targetSet IS NULL OR r.contactSet = :targetSet) AND " +
           "(:isAllGroup = true OR " +
           " ('HR' IN :targetGroups AND (LOWER(r.title) LIKE '%hr%' OR LOWER(r.title) LIKE '%human%' OR LOWER(r.title) LIKE '%recruiter%' OR LOWER(r.title) LIKE '%talent%')) OR " +
           " ('LEAD' IN :targetGroups AND (LOWER(r.title) LIKE '%head%' OR LOWER(r.title) LIKE '%lead%' OR LOWER(r.title) LIKE '%manager%' OR LOWER(r.title) LIKE '%director%' OR LOWER(r.title) LIKE '%vp%' OR LOWER(r.title) LIKE '%president%' OR LOWER(r.title) LIKE '%founder%')) OR " +
           " ('BPO' IN :targetGroups AND LOWER(r.title) LIKE '%bpo%') OR " +
           " ('SALES' IN :targetGroups AND (LOWER(r.title) LIKE '%sales%' OR LOWER(r.title) LIKE '%bdm%' OR LOWER(r.title) LIKE '%mis%' OR LOWER(r.title) LIKE '%marketing%')) OR " +
           " ('TECHNICAL' IN :targetGroups AND (LOWER(r.title) LIKE '%tech%' OR LOWER(r.title) LIKE '%java%' OR LOWER(r.title) LIKE '%developer%' OR LOWER(r.title) LIKE '%sde%' OR LOWER(r.title) LIKE '%engineer%' OR LOWER(r.title) LIKE '%sdet%')) OR " +
           " ('NON_IT' IN :targetGroups AND (LOWER(r.title) LIKE '%non it%' OR LOWER(r.title) LIKE '%non-it%'))" +
           ")")
    List<Recruiter> findEligibleRecruitersFiltered(
            @Param("status") RecruiterStatus status,
            @Param("cooldownDate") LocalDateTime cooldownDate,
            @Param("targetSet") Integer targetSet,
            @Param("targetGroups") List<String> targetGroups,
            @Param("isAllGroup") boolean isAllGroup
    );
}
