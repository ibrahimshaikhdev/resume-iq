package com.ai.Resume.analyser.service;

import com.ai.Resume.analyser.exception.ResumeAnalysisException;
import com.ai.Resume.analyser.model.RecruiterSimulation;
import com.ai.Resume.analyser.model.RecruiterSimulationDto;
import com.ai.Resume.analyser.repository.RecruiterSimulationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class RecruiterSimulationService {

    private static final Logger log = LoggerFactory.getLogger(RecruiterSimulationService.class);

    @Value("${genKey}")
    private String genKey;

    @Autowired
    private RecruiterSimulationRepository simulationRepository;

    @CacheEvict(value = "lastSimulation", allEntries = true)
    public RecruiterSimulationDto simulate(String roles, String resumeText) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        long startTime = System.currentTimeMillis();

        log.info("Starting recruiter simulation for user={}, roles={}", email, roles);

        try {
            Client client = Client.builder().apiKey(genKey).build();
            Content content = Content.builder()
                    .parts(Part.fromText(resumeText), Part.fromText(buildSimulationPrompt(roles)))
                    .build();

            int maxRetries = 3;
            int attempts = 0;
            String results = null;

            while (attempts < maxRetries) {
                try {
                    GenerateContentResponse response = client.models.generateContent(
                            "gemini-2.5-flash",
                            content,
                            GenerateContentConfig.builder().temperature(0.3f).build()
                    );
                    results = response.text();
                    break;
                } catch (Exception e) {
                    attempts++;
                    if (attempts >= maxRetries) {
                        throw new ResumeAnalysisException("Simulation failed after " + maxRetries + " attempts", e);
                    }
                    Thread.sleep(1500);
                }
            }

            // Clean markdown fences
            if (results.startsWith("```")) {
                int firstBrace = results.indexOf("{");
                int lastBrace = results.lastIndexOf("}");
                if (firstBrace != -1 && lastBrace != -1) {
                    results = results.substring(firstBrace, lastBrace + 1);
                }
            }

            ObjectMapper objectMapper = new ObjectMapper();
            RecruiterSimulationDto dto = objectMapper.readValue(results, RecruiterSimulationDto.class);

            // Persist to database
            RecruiterSimulation entity = new RecruiterSimulation();
            entity.setEmail(email);
            entity.setRoles(roles);

            if (dto.getStartup() != null) {
                entity.setStartupScore(dto.getStartup().getScore());
                entity.setStartupFeedback(dto.getStartup().getFeedback());
                entity.setStartupPerspective(dto.getStartup().getPerspective());
            }
            if (dto.getEnterprise() != null) {
                entity.setEnterpriseScore(dto.getEnterprise().getScore());
                entity.setEnterpriseFeedback(dto.getEnterprise().getFeedback());
                entity.setEnterprisePerspective(dto.getEnterprise().getPerspective());
            }
            if (dto.getProduct() != null) {
                entity.setProductScore(dto.getProduct().getScore());
                entity.setProductFeedback(dto.getProduct().getFeedback());
                entity.setProductPerspective(dto.getProduct().getPerspective());
            }
            if (dto.getEngineeringManager() != null) {
                entity.setEngineeringManagerScore(dto.getEngineeringManager().getScore());
                entity.setEngineeringManagerFeedback(dto.getEngineeringManager().getFeedback());
                entity.setEngineeringManagerPerspective(dto.getEngineeringManager().getPerspective());
            }

            entity.setCommonStrengths(dto.getCommonStrengths());
            entity.setCommonWeaknesses(dto.getCommonWeaknesses());

            simulationRepository.save(entity);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Recruiter simulation completed for user={}, duration={}ms", email, duration);

            return dto;

        } catch (ResumeAnalysisException e) {
            throw e;
        } catch (Exception e) {
            log.error("Recruiter simulation failed for user={}, error={}", email, e.getMessage(), e);
            throw new ResumeAnalysisException("Simulation failed: " + e.getMessage(), e);
        }
    }

    @Cacheable(value = "lastSimulation", key = "T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName()")
    public RecruiterSimulationDto getLastSimulation() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        RecruiterSimulation sim = simulationRepository.findFirstByEmailOrderBySimulatedAtDesc(email)
                .orElse(null);

        if (sim == null) {
            return null;
        }

        RecruiterSimulationDto dto = new RecruiterSimulationDto();

        RecruiterSimulationDto.ReviewerPerspective startup = new RecruiterSimulationDto.ReviewerPerspective();
        startup.setScore(sim.getStartupScore());
        startup.setFeedback(sim.getStartupFeedback());
        startup.setPerspective(sim.getStartupPerspective());
        dto.setStartup(startup);

        RecruiterSimulationDto.ReviewerPerspective enterprise = new RecruiterSimulationDto.ReviewerPerspective();
        enterprise.setScore(sim.getEnterpriseScore());
        enterprise.setFeedback(sim.getEnterpriseFeedback());
        enterprise.setPerspective(sim.getEnterprisePerspective());
        dto.setEnterprise(enterprise);

        RecruiterSimulationDto.ReviewerPerspective product = new RecruiterSimulationDto.ReviewerPerspective();
        product.setScore(sim.getProductScore());
        product.setFeedback(sim.getProductFeedback());
        product.setPerspective(sim.getProductPerspective());
        dto.setProduct(product);

        RecruiterSimulationDto.ReviewerPerspective engManager = new RecruiterSimulationDto.ReviewerPerspective();
        engManager.setScore(sim.getEngineeringManagerScore());
        engManager.setFeedback(sim.getEngineeringManagerFeedback());
        engManager.setPerspective(sim.getEngineeringManagerPerspective());
        dto.setEngineeringManager(engManager);

        dto.setCommonStrengths(sim.getCommonStrengths());
        dto.setCommonWeaknesses(sim.getCommonWeaknesses());

        return dto;
    }

    private String buildSimulationPrompt(String roles) {
        return "You are simulating how 4 different types of technical recruiters would evaluate the same resume for the role: " + roles + "\n\n" +
                "Each recruiter has a different evaluation style and priorities:\n\n" +
                "1. **Startup Recruiter**: Values versatility, hands-on skills, adaptability, side projects, open-source contributions. Cares less about formal credentials. Wants to see you can wear multiple hats and ship fast.\n\n" +
                "2. **Enterprise Recruiter**: Values structured experience, clear career progression, certifications, formal education, process adherence. Looks for stability, team size managed, and enterprise tool experience.\n\n" +
                "3. **Product Company Recruiter**: Values product thinking, user impact, metrics-driven achievements, cross-functional collaboration. Looks for evidence of shipping features that moved business metrics.\n\n" +
                "4. **Engineering Manager**: Values technical depth, system design thinking, mentoring ability, code quality awareness, and architectural decisions. Looks for evidence of technical leadership and team impact.\n\n" +
                "For each recruiter type, provide:\n" +
                "- score: 0-100 (how they would rate this resume for the target role)\n" +
                "- feedback: 1-2 sentences of what they would say to the candidate (direct, constructive)\n" +
                "- perspective: 1-2 sentences of their internal assessment (what they think but might not say directly)\n\n" +
                "Also identify:\n" +
                "- commonStrengths: strengths that ALL 4 reviewers would agree on (array of strings)\n" +
                "- commonWeaknesses: weaknesses that ALL 4 reviewers would notice (array of strings)\n\n" +
                "Output Format:\n" +
                "Return strict raw JSON only. No markdown, no commentary.\n" +
                "{\n" +
                "  \"startup\": {\"score\": number, \"feedback\": \"string\", \"perspective\": \"string\"},\n" +
                "  \"enterprise\": {\"score\": number, \"feedback\": \"string\", \"perspective\": \"string\"},\n" +
                "  \"product\": {\"score\": number, \"feedback\": \"string\", \"perspective\": \"string\"},\n" +
                "  \"engineeringManager\": {\"score\": number, \"feedback\": \"string\", \"perspective\": \"string\"},\n" +
                "  \"commonStrengths\": [array of strings],\n" +
                "  \"commonWeaknesses\": [array of strings]\n" +
                "}\n";
    }
}
