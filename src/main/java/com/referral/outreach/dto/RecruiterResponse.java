package com.referral.outreach.dto;

import com.referral.outreach.entity.RecruiterStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecruiterResponse {
    private Long id;
    private String name;
    private String email;
    private String title;
    private String company;
    private RecruiterStatus status;
    private Integer contactSet;
    private LocalDateTime lastContactedDate;
}
