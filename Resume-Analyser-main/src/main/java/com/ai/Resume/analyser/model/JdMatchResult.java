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
@Table(name = "jd_match_result", indexes = {
        @Index(name = "idx_jdmatch_email", columnList = "email"),
        @Index(name = "idx_jdmatch_email_date", columnList = "email, matchedAt DESC")
})
public class JdMatchResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String roles;

    @Column(columnDefinition = "TEXT")
    private String jobDescription;

    private int matchPercentage;

    @ElementCollection
    @Column(length = 300)
    private List<String> matchedSkills;

    @ElementCollection
    @Column(length = 300)
    private List<String> missingSkills;

    @ElementCollection
    @Column(length = 450)
    private List<String> strengths;

    @ElementCollection
    @Column(length = 450)
    private List<String> gaps;

    @ElementCollection
    @Column(length = 450)
    private List<String> recommendations;

    @Convert(converter = JdCategoryBreakdownConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, JdMatchResponse.CategoryScore> categoryBreakdown;

    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    private Date matchedAt;
}
