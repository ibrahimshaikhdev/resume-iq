package com.ai.Resume.analyser.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "recruiter_simulation", indexes = {
        @Index(name = "idx_sim_email", columnList = "email"),
        @Index(name = "idx_sim_email_date", columnList = "email, simulatedAt DESC")
})
public class RecruiterSimulation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String roles;

    // Startup Recruiter
    private int startupScore;
    @Column(length = 500)
    private String startupFeedback;
    @Column(length = 500)
    private String startupPerspective;

    // Enterprise Recruiter
    private int enterpriseScore;
    @Column(length = 500)
    private String enterpriseFeedback;
    @Column(length = 500)
    private String enterprisePerspective;

    // Product Company Recruiter
    private int productScore;
    @Column(length = 500)
    private String productFeedback;
    @Column(length = 500)
    private String productPerspective;

    // Engineering Manager
    private int engineeringManagerScore;
    @Column(length = 500)
    private String engineeringManagerFeedback;
    @Column(length = 500)
    private String engineeringManagerPerspective;

    @ElementCollection
    @Column(length = 300)
    private List<String> commonStrengths;

    @ElementCollection
    @Column(length = 300)
    private List<String> commonWeaknesses;

    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    private Date simulatedAt;
}
