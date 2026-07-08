package com.referral.outreach.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recruiter")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recruiter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String company;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_category", nullable = false)
    private RoleCategory roleCategory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecruiterStatus status;

    @Column(name = "last_contacted_date")
    private LocalDateTime lastContactedDate;
}
