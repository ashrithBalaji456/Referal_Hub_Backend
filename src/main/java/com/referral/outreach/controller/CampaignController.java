package com.referral.outreach.controller;

import com.referral.outreach.dto.CampaignRequest;
import com.referral.outreach.dto.CampaignResponse;
import com.referral.outreach.dto.PreviewResponse;
import com.referral.outreach.service.CampaignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;
    private final com.referral.outreach.scheduler.WeeklyOutreachScheduler weeklyOutreachScheduler;

    @PostMapping("/trigger-scheduler")
    public ResponseEntity<java.util.Map<String, String>> triggerScheduler() {
        log.info("REST request to trigger weekly scheduler run immediately");
        new Thread(() -> {
            weeklyOutreachScheduler.executeWeeklyOutreach();
        }).start();
        return ResponseEntity.ok(java.util.Map.of("message", "Weekly outreach run initiated successfully"));
    }

    @PostMapping
    public ResponseEntity<CampaignResponse> createCampaign(@Valid @RequestBody CampaignRequest request) {
        log.info("REST request to create campaign: {}", request.getName());
        CampaignResponse response = campaignService.createCampaign(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CampaignResponse> updateCampaign(
            @PathVariable Long id,
            @Valid @RequestBody CampaignRequest request) {
        log.info("REST request to update campaign ID: {}", id);
        CampaignResponse response = campaignService.updateCampaign(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CampaignResponse> getCampaignById(@PathVariable Long id) {
        log.info("REST request to get campaign ID: {}", id);
        CampaignResponse response = campaignService.getCampaignById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<CampaignResponse>> getAllCampaigns() {
        log.info("REST request to get all campaigns");
        List<CampaignResponse> response = campaignService.getAllCampaigns();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCampaign(@PathVariable Long id) {
        log.info("REST request to delete campaign ID: {}", id);
        campaignService.deleteCampaign(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/enable")
    public ResponseEntity<CampaignResponse> enableCampaign(@PathVariable Long id) {
        log.info("REST request to enable campaign ID: {}", id);
        CampaignResponse response = campaignService.enableCampaign(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/disable")
    public ResponseEntity<CampaignResponse> disableCampaign(@PathVariable Long id) {
        log.info("REST request to disable campaign ID: {}", id);
        CampaignResponse response = campaignService.disableCampaign(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<PreviewResponse> previewEmail(
            @PathVariable Long id,
            @RequestParam Long recruiterId) {
        log.info("REST request to preview email for campaign: {} and recruiter: {}", id, recruiterId);
        PreviewResponse response = campaignService.previewCampaignEmail(id, recruiterId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/trigger")
    public ResponseEntity<Void> triggerCampaign(
            @PathVariable Long id,
            @RequestParam Long recruiterId) {
        log.info("REST request to manually trigger campaign: {} for recruiter: {}", id, recruiterId);
        campaignService.triggerCampaignManually(id, recruiterId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/trigger-batch")
    public ResponseEntity<java.util.Map<String, Object>> triggerCampaignBatch(
            @PathVariable Long id,
            @RequestParam(defaultValue = "50") int limit) {
        log.info("REST request to trigger batch campaign: {} with limit: {}", id, limit);
        int triggeredCount = campaignService.triggerCampaignBatch(id, limit);
        return ResponseEntity.ok(java.util.Map.of(
                "message", "Outreach queued successfully",
                "count", triggeredCount
        ));
    }
}
