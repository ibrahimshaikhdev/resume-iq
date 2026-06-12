package com.ai.Resume.analyser.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "analysis_job", indexes = {
        @Index(name = "idx_job_email", columnList = "email"),
        @Index(name = "idx_job_status", columnList = "status"),
        @Index(name = "idx_job_email_type_date", columnList = "email, jobType, createdAt DESC")
})
public class AnalysisJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String roles;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status = JobStatus.QUEUED;

    @Column(length = 1000)
    private String errorMessage;

    private int retryCount = 0;

    @Column(nullable = false)
    private int maxRetries = 3;

    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(updatable = false)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date startedAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date completedAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    @Column(nullable = false)
    private String jobType = "ANALYSIS";

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }

    public enum JobStatus {
        QUEUED,
        PROCESSING,
        SCORING,
        RECOMMENDATIONS,
        COMPLETED,
        FAILED,
        TIMEOUT
    }
}
