package com.referral.outreach.dto;

import com.referral.outreach.entity.RecruiterStatus;
import com.referral.outreach.entity.RoleCategory;
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
    private String company;
    private RoleCategory roleCategory;
    private RecruiterStatus status;
    private LocalDateTime lastContactedDate;
}
