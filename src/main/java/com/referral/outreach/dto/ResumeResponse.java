package com.referral.outreach.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeResponse {
    private Long id;
    private String filename;
    private String originalFilename;
    private Long fileSize;
    private String contentType;
    private boolean isActive;
    private LocalDateTime uploadedTimestamp;
}
