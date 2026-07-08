package com.referral.outreach.repository;

import com.referral.outreach.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {

    Optional<Resume> findByIsActiveTrue();

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Resume r SET r.isActive = false")
    void deactivateAllResumes();

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Resume r SET r.isActive = false WHERE r.id != :id")
    void deactivateOthers(Long id);
}
