package com.ai.Resume.analyser.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JdMatchResponse {
    private int matchPercentage;
    private List<String> matchedSkills;
    private List<String> missingSkills;
    private List<String> strengths;
    private List<String> gaps;
    private List<String> recommendations;
    private Map<String, CategoryScore> categoryBreakdown;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategoryScore {
        private int score;
        private int maxScore;
        private String status;
    }
}
