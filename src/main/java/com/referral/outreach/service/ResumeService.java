package com.referral.outreach.service;

import com.referral.outreach.dto.ResumeResponse;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface ResumeService {
    ResumeResponse uploadResume(MultipartFile file);
    ResumeResponse getResumeById(Long id);
    List<ResumeResponse> getAllResumes();
    void deleteResume(Long id);
    ResumeResponse markAsActive(Long id);
    ResumeResponse getActiveResume();
}
