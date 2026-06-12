package com.ai.Resume.analyser.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecruiterSimulationDto {

    private ReviewerPerspective startup;
    private ReviewerPerspective enterprise;
    private ReviewerPerspective product;
    private ReviewerPerspective engineeringManager;
    private List<String> commonStrengths;
    private List<String> commonWeaknesses;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ReviewerPerspective {
        private int score;
        private String feedback;
        private String perspective;
    }
}
