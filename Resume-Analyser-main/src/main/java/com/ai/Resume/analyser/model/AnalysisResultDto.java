package com.ai.Resume.analyser.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisResultDto {

    private int score;
    private int atsoptimizationscore;
    private List<String> pros;
    private List<String> cons;
    private List<String> suggestions;
    private List<Job> jobs;

    @JsonProperty("atsBreakdown")
    private Map<String, Integer> atsbreakdown;

    // Phase 8: Multi-dimensional weighted scoring
    private Map<String, Integer> dimensionScores;
    private Map<String, String> dimensionExplanations;

    private List<String> matchedSkills;
    private List<String> missingSkills;
}
