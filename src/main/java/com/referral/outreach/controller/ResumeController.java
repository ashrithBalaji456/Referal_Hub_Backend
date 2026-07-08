package com.referral.outreach.controller;

import com.referral.outreach.dto.ResumeResponse;
import com.referral.outreach.service.ResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeResponse> uploadResume(@RequestParam("file") MultipartFile file) {
        log.info("REST request to upload resume: {}", file.getOriginalFilename());
        ResumeResponse response = resumeService.uploadResume(file);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResumeResponse> getResumeById(@PathVariable Long id) {
        log.info("REST request to get resume metadata ID: {}", id);
        ResumeResponse response = resumeService.getResumeById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ResumeResponse>> getAllResumes() {
        log.info("REST request to get all resumes");
        List<ResumeResponse> response = resumeService.getAllResumes();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResume(@PathVariable Long id) {
        log.info("REST request to delete resume ID: {}", id);
        resumeService.deleteResume(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<ResumeResponse> markAsActive(@PathVariable Long id) {
        log.info("REST request to mark resume ID: {} as active", id);
        ResumeResponse response = resumeService.markAsActive(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    public ResponseEntity<ResumeResponse> getActiveResume() {
        log.info("REST request to get active resume");
        ResumeResponse response = resumeService.getActiveResume();
        return ResponseEntity.ok(response);
    }
}
