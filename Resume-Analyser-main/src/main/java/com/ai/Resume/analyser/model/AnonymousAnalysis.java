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
@Table(name = "anonymous_analysis", indexes = {
        @Index(name = "idx_anon_hash", columnList = "contentHash", unique = true)
})
public class AnonymousAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String contentHash;

    @Column(nullable = false)
    private String roles;

    private int score;
    private int atsoptimizationscore;

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
    @Column(updatable = false)
    private Date createdAt;
}
