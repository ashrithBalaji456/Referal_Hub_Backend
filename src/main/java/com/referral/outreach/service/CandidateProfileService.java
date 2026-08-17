package com.referral.outreach.service;

import com.referral.outreach.entity.CandidateProfile;

public interface CandidateProfileService {
    CandidateProfile getProfile();
    CandidateProfile saveProfile(CandidateProfile profile);
}
