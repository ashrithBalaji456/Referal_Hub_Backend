package com.referral.outreach.dto;

import com.referral.outreach.entity.RecruiterStatus;
import com.referral.outreach.entity.RoleCategory;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecruiterRequest {

    @NotBlank(message = "Recruiter name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Company is required")
    private String company;

    @NotNull(message = "Role category is required")
    private RoleCategory roleCategory;

    @NotNull(message = "Status is required")
    private RecruiterStatus status;
}
