package com.referral.outreach.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiter_id", nullable = false)
    private Recruiter recruiter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(name = "subject_used", nullable = false)
    private String subjectUsed;

    @Column(name = "sent_timestamp", nullable = false)
    private LocalDateTime sentTimestamp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailHistoryStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        if (this.sentTimestamp == null) {
            this.sentTimestamp = LocalDateTime.now();
        }
    }
}
