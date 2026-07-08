package com.referral.outreach.controller;

import com.referral.outreach.dto.EmailHistoryResponse;
import com.referral.outreach.entity.EmailHistoryStatus;
import com.referral.outreach.service.EmailHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/email-history")
@RequiredArgsConstructor
public class EmailHistoryController {

    private final EmailHistoryService emailHistoryService;

    @GetMapping
    public ResponseEntity<List<EmailHistoryResponse>> getFilteredHistory(
            @RequestParam(required = false) Long recruiterId,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) EmailHistoryStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        log.info("REST request to get filtered email history");
        List<EmailHistoryResponse> response = emailHistoryService.getFilteredHistory(
                recruiterId, company, status, startDate, endDate
        );
        return ResponseEntity.ok(response);
    }
}
