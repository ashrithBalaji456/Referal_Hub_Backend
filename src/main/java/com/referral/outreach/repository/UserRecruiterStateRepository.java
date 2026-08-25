package com.referral.outreach.repository;

import com.referral.outreach.entity.Recruiter;
import com.referral.outreach.entity.User;
import com.referral.outreach.entity.UserRecruiterState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRecruiterStateRepository extends JpaRepository<UserRecruiterState, Long> {
    List<UserRecruiterState> findByUser(User user);
    Optional<UserRecruiterState> findByUserAndRecruiter(User user, Recruiter recruiter);
    Optional<UserRecruiterState> findByUserAndRecruiterId(User user, Long recruiterId);
}
