package com.ai.Resume.analyser.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "analysis_result", indexes = {
        @Index(name = "idx_analysis_email", columnList = "email"),
        @Index(name = "idx_analysis_email_date", columnList = "email, analyzedAt DESC"),
        @Index(name = "idx_analysis_score", columnList = "score")
})
public class AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    private int score;
    private int atsoptimizationscore;
    private String roles;

    @ElementCollection
    @Column(length = 450)
    private List<String> pros;

    @ElementCollection
    @Column(length = 450)
    private List<String> cons;

    @ElementCollection
    @Column(length = 450)
    private List<String> suggestions;

    @Convert(converter = AtsBreakdownConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, Integer> atsbreakdown;

    // Phase 8: Multi-dimensional weighted scoring
    @Convert(converter = AtsBreakdownConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, Integer> dimensionScores;

    @Convert(converter = DimensionExplanationsConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, String> dimensionExplanations;

    @ElementCollection
    @Column(length = 300)
    private List<String> matchedSkills;

    @ElementCollection
    @Column(length = 300)
    private List<String> missingSkills;

    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    private Date analyzedAt;
}
