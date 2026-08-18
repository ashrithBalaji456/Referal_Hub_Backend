package com.referral.outreach.repository;

import com.referral.outreach.entity.Resume;
import com.referral.outreach.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {

    List<Resume> findByUser(User user);

    Optional<Resume> findByIsActiveTrue();
    List<Resume> findAllByIsActiveTrue();
    
    Optional<Resume> findByUserAndIsActiveTrue(User user);
    List<Resume> findAllByUserAndIsActiveTrue(User user);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Resume r SET r.isActive = false WHERE r.user.id = :userId")
    void deactivateAllResumes(Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Resume r SET r.isActive = false WHERE r.id != :id AND r.user.id = :userId")
    void deactivateOthers(Long id, Long userId);
}
