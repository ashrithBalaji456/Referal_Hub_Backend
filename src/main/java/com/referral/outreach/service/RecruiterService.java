package com.referral.outreach.service;

import com.referral.outreach.dto.RecruiterRequest;
import com.referral.outreach.dto.RecruiterResponse;
import com.referral.outreach.entity.RecruiterStatus;
import java.util.List;

public interface RecruiterService {
    RecruiterResponse createRecruiter(RecruiterRequest request);
    RecruiterResponse updateRecruiter(Long id, RecruiterRequest request);
    RecruiterResponse getRecruiterById(Long id);
    List<RecruiterResponse> getAllRecruiters();
    void deleteRecruiter(Long id);
    RecruiterResponse updateStatus(Long id, RecruiterStatus status);
}
