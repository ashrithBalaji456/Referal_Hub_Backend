package com.referral.outreach.controller;

import com.referral.outreach.entity.CandidateProfile;
import com.referral.outreach.service.CandidateProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/candidate-profile")
@RequiredArgsConstructor
public class CandidateProfileController {

    private final CandidateProfileService service;

    @GetMapping
    public ResponseEntity<CandidateProfile> getProfile() {
        log.info("REST request to get Candidate Profile");
        return ResponseEntity.ok(service.getProfile());
    }

    @PutMapping
    public ResponseEntity<CandidateProfile> saveProfile(@RequestBody CandidateProfile profile) {
        log.info("REST request to save/update Candidate Profile");
        return ResponseEntity.ok(service.saveProfile(profile));
    }
}
