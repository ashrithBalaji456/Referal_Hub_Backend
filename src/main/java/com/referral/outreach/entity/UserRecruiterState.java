package com.referral.outreach.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "user_recruiter_state",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "recruiter_id"})}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRecruiterState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recruiter_id", nullable = false)
    private Recruiter recruiter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecruiterStatus status;

    @Column(name = "last_contacted_date")
    private LocalDateTime lastContactedDate;

    @Column(name = "custom_title")
    private String customTitle;

    @Column(name = "custom_company")
    private String customCompany;

    @Column(name = "is_added", nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private Boolean isAdded = true;
}
