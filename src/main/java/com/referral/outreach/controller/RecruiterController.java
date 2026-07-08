package com.referral.outreach.controller;

import com.referral.outreach.dto.RecruiterRequest;
import com.referral.outreach.dto.RecruiterResponse;
import com.referral.outreach.entity.RecruiterStatus;
import com.referral.outreach.service.RecruiterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/recruiters")
@RequiredArgsConstructor
public class RecruiterController {

    private final RecruiterService recruiterService;

    @PostMapping
    public ResponseEntity<RecruiterResponse> createRecruiter(@Valid @RequestBody RecruiterRequest request) {
        log.info("REST request to create recruiter: {}", request.getEmail());
        RecruiterResponse response = recruiterService.createRecruiter(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecruiterResponse> updateRecruiter(
            @PathVariable Long id,
            @Valid @RequestBody RecruiterRequest request) {
        log.info("REST request to update recruiter ID: {}", id);
        RecruiterResponse response = recruiterService.updateRecruiter(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecruiterResponse> getRecruiterById(@PathVariable Long id) {
        log.info("REST request to get recruiter ID: {}", id);
        RecruiterResponse response = recruiterService.getRecruiterById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<RecruiterResponse>> getAllRecruiters() {
        log.info("REST request to get all recruiters");
        List<RecruiterResponse> response = recruiterService.getAllRecruiters();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecruiter(@PathVariable Long id) {
        log.info("REST request to delete recruiter ID: {}", id);
        recruiterService.deleteRecruiter(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<RecruiterResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam RecruiterStatus status) {
        log.info("REST request to update status of recruiter ID: {} to {}", id, status);
        RecruiterResponse response = recruiterService.updateStatus(id, status);
        return ResponseEntity.ok(response);
    }
}
