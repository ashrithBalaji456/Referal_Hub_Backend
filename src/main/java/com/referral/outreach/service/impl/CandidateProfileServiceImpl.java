package com.referral.outreach.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.referral.outreach.entity.CandidateProfile;
import com.referral.outreach.entity.User;
import com.referral.outreach.repository.CandidateProfileRepository;
import com.referral.outreach.security.SecurityUtils;
import com.referral.outreach.service.CandidateProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateProfileServiceImpl implements CandidateProfileService {

    private final CandidateProfileRepository repository;
    private final SecurityUtils securityUtils;
    private final ObjectMapper objectMapper;

    @Value("${app.candidate-name:Gudla Ashrith Balaji}")
    private String defaultCandidateName;

    @Value("${app.target-role:Java Backend Developer}")
    private String defaultTargetRole;

    @Override
    @Transactional
    public CandidateProfile getProfile() {
        User user = securityUtils.getAuthenticatedUser();
        return repository.findByUser(user).orElseGet(() -> {
            Map<String, String> defaults = new HashMap<>();
            String defaultJson = "{}";
            try {
                defaultJson = objectMapper.writeValueAsString(defaults);
            } catch (Exception e) {
                log.error("Failed to write default custom fields map to JSON", e);
            }
            CandidateProfile newProfile = CandidateProfile.builder()
                    .fullName(defaultCandidateName)
                    .roleName(defaultTargetRole)
                    .email(user.getEmail())
                    .linkedinUrl("linkedin.com/in/ashrith-balaji-gudla-5768302a8/")
                    .githubUrl("")
                    .phoneNumber("")
                    .location("")
                    .customFieldsJson(defaultJson)
                    .user(user)
                    .build();
            return repository.save(newProfile);
        });
    }

    @Override
    @Transactional
    public CandidateProfile saveProfile(CandidateProfile profile) {
        User user = securityUtils.getAuthenticatedUser();
        CandidateProfile existing = repository.findByUser(user).orElse(null);
        if (existing != null) {
            existing.setFullName(profile.getFullName());
            existing.setRoleName(profile.getRoleName());
            existing.setEmail(profile.getEmail());
            existing.setLinkedinUrl(profile.getLinkedinUrl());
            existing.setGithubUrl(profile.getGithubUrl());
            existing.setPhoneNumber(profile.getPhoneNumber());
            existing.setLocation(profile.getLocation());
            existing.setCustomFieldsJson(profile.getCustomFieldsJson());
            return repository.save(existing);
        } else {
            profile.setUser(user);
            return repository.save(profile);
        }
    }
}
