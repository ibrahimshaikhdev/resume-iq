package com.ai.Resume.analyser.service;

import com.ai.Resume.analyser.exception.ResumeAnalysisException;
import com.ai.Resume.analyser.model.*;
import com.ai.Resume.analyser.repository.AnalysisResultRepository;
import com.ai.Resume.analyser.repository.AnonymousAnalysisRepository;
import com.ai.Resume.analyser.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Service
public class PublicAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(PublicAnalysisService.class);

    @Value("${genKey}")
    private String genKey;

    @Autowired
    private AnonymousAnalysisRepository anonymousAnalysisRepository;

    @Autowired
    private AnalysisResultRepository analysisResultRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private Tika tika;

    @Autowired
    private GroqClient groqClient;

    // Diagnostics: records which engine ran on the most recent analysis and why.
    public static volatile String lastEngine = "none yet";
    public static volatile String lastError = null;

    public String computeContentHash(byte[] fileBytes, String roles) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(combine(fileBytes, roles.getBytes(StandardCharsets.UTF_8)));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute content hash", e);
        }
    }

    private byte[] combine(byte[] a, byte[] b) {
        byte[] combined = new byte[a.length + b.length];
        System.arraycopy(a, 0, combined, 0, a.length);
        System.arraycopy(b, 0, combined, a.length, b.length);
        return combined;
    }

    public AnonymousAnalysis analyzePublic(byte[] fileBytes, String roles) {
        String contentHash = computeContentHash(fileBytes, roles);

        // Check cache first - ensures consistent scores
        return anonymousAnalysisRepository.findByContentHash(contentHash)
                .orElseGet(() -> performAnalysis(fileBytes, roles, contentHash));
    }

    private AnonymousAnalysis performAnalysis(byte[] fileBytes, String roles, String contentHash) {
        try {
            log.info("Performing public analysis for hash={}", contentHash);
            String extracted = tika.parseToString(new ByteArrayInputStream(fileBytes));

            AnalysisResultDto resultDto = null;

            // Try Groq AI first
            try {
                String results = callGeminiForAnalysis(extracted, roles);

                if (results.startsWith("```")) {
                    int firstBrace = results.indexOf("{");
                    int lastBrace = results.lastIndexOf("}");
                    if (firstBrace != -1 && lastBrace != -1) {
                        results = results.substring(firstBrace, lastBrace + 1);
                    }
                }

                ObjectMapper objectMapper = new ObjectMapper();
                resultDto = objectMapper.readValue(results, AnalysisResultDto.class);
                lastEngine = "Groq AI";
                lastError = null;
            } catch (Exception geminiError) {
                lastEngine = "Offline (Groq failed)";
                lastError = geminiError.getClass().getSimpleName() + ": " + geminiError.getMessage();
                log.warn("Groq API failed, using mock analysis: {}", lastError);
                resultDto = generateMockAnalysis(extracted, roles);
            }

            AnonymousAnalysis analysis = new AnonymousAnalysis();
            analysis.setContentHash(contentHash);
            analysis.setRoles(roles);
            analysis.setScore(resultDto.getScore());
            analysis.setAtsoptimizationscore(resultDto.getAtsoptimizationscore());
            analysis.setPros(resultDto.getPros());
            analysis.setCons(resultDto.getCons());
            analysis.setSuggestions(resultDto.getSuggestions());
            analysis.setAtsbreakdown(resultDto.getAtsbreakdown());
            analysis.setDimensionScores(resultDto.getDimensionScores());
            analysis.setDimensionExplanations(resultDto.getDimensionExplanations());
            analysis.setMatchedSkills(resultDto.getMatchedSkills());
            analysis.setMissingSkills(resultDto.getMissingSkills());

            return anonymousAnalysisRepository.save(analysis);

        } catch (Exception e) {
            log.error("Public analysis failed for hash={}: {}", contentHash, e.getMessage(), e);
            throw new ResumeAnalysisException("Analysis failed. Please try again.", e);
        }
    }

    private int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(v, hi));
    }

    public AnalysisResultDto generateMockAnalysis(String resumeText, String roles) {
        String lower = resumeText.toLowerCase();

        int len = resumeText.trim().length();

        // Role relevance: how many of the stated role keywords actually appear
        int roleHits = 0, roleTotal = 0;
        for (String kw : roles.toLowerCase().split("[,\\s]+")) {
            if (kw.length() > 2) { roleTotal++; if (lower.contains(kw)) roleHits++; }
        }

        // Technical keyword coverage
        int techHits = 0;
        for (String tk : new String[]{"java", "python", "javascript", "typescript", "react", "angular", "vue",
                "node", "spring", "django", "flask", "sql", "mysql", "postgres", "mongodb", "aws", "azure", "gcp",
                "docker", "kubernetes", "git", "api", "rest", "html", "css", "linux", "c++", "c#", "machine learning"}) {
            if (lower.contains(tk)) techHits++;
        }

        // Resume structure signals
        int sectionHits = 0;
        for (String s : new String[]{"summary", "experience", "education", "skills", "projects", "certifications", "work", "employment"}) {
            if (lower.contains(s)) sectionHits++;
        }
        boolean looksLikeResume = len >= 300 && sectionHits >= 2;

        // ATS Breakdown (relevance-aware, low baselines so junk cannot score high)
        Map<String, Integer> atsBreakdown = new LinkedHashMap<>();
        atsBreakdown.put("keywordMatch", clamp(roleTotal > 0 ? (int) Math.round(15.0 * roleHits / roleTotal) : 4, 0, 15));

        int formatScore = 3;
        if (lower.contains("experience") || lower.contains("education")) formatScore += 4;
        if (lower.contains("skills")) formatScore += 3;
        if (looksLikeResume) formatScore += 3;
        atsBreakdown.put("formatting", clamp(formatScore, 0, 15));

        int readScore = 2;
        if (len > 500) readScore += 4;
        if (len > 1000) readScore += 5;
        if (len > 2000) readScore += 2;
        atsBreakdown.put("readability", clamp(readScore, 0, 15));

        atsBreakdown.put("sectionClarity", clamp(sectionHits * 2, 0, 15));

        int relevanceScore = Math.min(techHits * 2, 12) + Math.min(roleHits * 2, 3);
        atsBreakdown.put("contentRelevance", clamp(relevanceScore, 0, 15));

        int contactScore = 0;
        if (lower.contains("@") || lower.contains("email")) contactScore += 4;
        if (lower.contains("phone") || lower.matches("(?s).*\\d{10}.*")) contactScore += 3;
        if (lower.contains("linkedin") || lower.contains("github")) contactScore += 3;
        atsBreakdown.put("contactInfo", clamp(contactScore, 0, 10));

        int grammarScore = looksLikeResume ? 11 : 4;
        if (len < 200) grammarScore = 2;
        atsBreakdown.put("grammar", clamp(grammarScore, 0, 15));

        int totalScore = atsBreakdown.values().stream().mapToInt(Integer::intValue).sum();
        // If the upload doesn't even look like a resume, cap the score hard.
        int overallScore = looksLikeResume ? totalScore : Math.min(totalScore, 35);
        int atsScore = clamp((int) Math.round(
                (atsBreakdown.get("keywordMatch") * 2 + atsBreakdown.get("formatting") * 2 + atsBreakdown.get("readability")
                        + atsBreakdown.get("sectionClarity") + atsBreakdown.get("contentRelevance") * 2
                        + atsBreakdown.get("contactInfo") + atsBreakdown.get("grammar")) / 145.0 * 100), 0, 100);
        if (!looksLikeResume) atsScore = Math.min(atsScore, 35);

        // Dimension scores
        Map<String, Integer> dimensionScores = new LinkedHashMap<>();
        dimensionScores.put("semanticRelevance", Math.max(overallScore - 10, 0));
        dimensionScores.put("technicalSkillCoverage", Math.max(overallScore - 15, 0));
        dimensionScores.put("experienceAlignment", Math.max(overallScore - 5, 0));
        dimensionScores.put("achievementQuality", Math.max(overallScore - 20, 0));
        dimensionScores.put("keywordDensity", atsBreakdown.getOrDefault("keywordMatch", 0) * 6);
        dimensionScores.put("formattingConfidence", atsBreakdown.getOrDefault("formatting", 0) * 6);

        Map<String, String> dimensionExplanations = new LinkedHashMap<>();
        dimensionExplanations.put("semanticRelevance", "Resume content partially aligns with " + roles);
        dimensionExplanations.put("technicalSkillCoverage", "Some technical skills match role requirements");
        dimensionExplanations.put("experienceAlignment", "Experience section demonstrates relevant background");
        dimensionExplanations.put("achievementQuality", "Achievements could be more quantifiable");
        dimensionExplanations.put("keywordDensity", "Moderate keyword presence for target role");
        dimensionExplanations.put("formattingConfidence", "Formatting is ATS-friendly with clear sections");

        // Skills
        List<String> matchedSkills = new ArrayList<>();
        for (String skill : new String[]{"Java", "Python", "JavaScript", "React", "Spring", "SQL", "Git", "HTML", "CSS", "REST", "API", "AWS", "Docker", "Agile", "Scrum"}) {
            if (lower.contains(skill.toLowerCase())) matchedSkills.add(skill);
        }

        List<String> missingSkills = new ArrayList<>();
        for (String skill : new String[]{"Kubernetes", "CI/CD", "Microservices", "Testing", "System Design", "Data Structures"}) {
            if (!lower.contains(skill.toLowerCase())) missingSkills.add(skill);
        }

        // Pros, cons, suggestions
        List<String> pros = new ArrayList<>();
        pros.add("Resume contains relevant keywords for the target role");
        pros.add("Good use of section headings for ATS parseability");
        if (lower.contains("experience")) pros.add("Work experience section is present and structured");
        if (lower.contains("education")) pros.add("Education details are clearly listed");

        List<String> cons = new ArrayList<>();
        cons.add("Consider adding more quantifiable achievements");
        cons.add("Some role-specific keywords could be added for better ATS matching");
        if (!lower.contains("linkedin")) cons.add("LinkedIn profile URL is missing");

        List<String> suggestions = new ArrayList<>();
        suggestions.add("Add metrics and numbers to demonstrate impact in previous roles");
        suggestions.add("Tailor your summary section to match the target role: " + roles);
        suggestions.add("Include relevant certifications if applicable");

        AnalysisResultDto dto = new AnalysisResultDto();
        dto.setScore(overallScore);
        dto.setAtsoptimizationscore(atsScore);
        dto.setPros(pros);
        dto.setCons(cons);
        dto.setSuggestions(suggestions);
        dto.setAtsbreakdown(atsBreakdown);
        dto.setDimensionScores(dimensionScores);
        dto.setDimensionExplanations(dimensionExplanations);
        dto.setMatchedSkills(matchedSkills);
        dto.setMissingSkills(missingSkills);

        return dto;
    }

    public AnalysisResult claimAnalysis(String contentHash, String email) {
        AnonymousAnalysis anon = anonymousAnalysisRepository.findByContentHash(contentHash)
                .orElse(null);

        if (anon == null) {
            return null;
        }

        // Check if user already has this analysis (avoid duplicates)
        var existingResults = analysisResultRepository.findByEmailOrderByAnalyzedAtDesc(email);
        for (AnalysisResult existing : existingResults) {
            if (existing.getRoles().equals(anon.getRoles()) && existing.getScore() == anon.getScore()) {
                return existing;
            }
        }

        // Transfer to user's account
        AnalysisResult result = new AnalysisResult();
        result.setEmail(email);
        result.setScore(anon.getScore());
        result.setAtsoptimizationscore(anon.getAtsoptimizationscore());
        result.setRoles(anon.getRoles());
        result.setPros(anon.getPros());
        result.setCons(anon.getCons());
        result.setSuggestions(anon.getSuggestions());
        result.setAtsbreakdown(anon.getAtsbreakdown());
        result.setDimensionScores(anon.getDimensionScores());
        result.setDimensionExplanations(anon.getDimensionExplanations());
        result.setMatchedSkills(anon.getMatchedSkills());
        result.setMissingSkills(anon.getMissingSkills());

        AnalysisResult saved = analysisResultRepository.save(result);

        // Update user's previousResults flag
        User user = userRepository.findById(email).orElse(null);
        if (user != null) {
            user.setPreviousResults(true);
            userRepository.save(user);
        }

        return saved;
    }

    private String callGeminiForAnalysis(String extracted, String roles) throws InterruptedException {
        String instruction = (
                "You are now an advanced enterprise-grade ATS resume checker. Your task is to analyze the given resume strictly based on industry-level ATS standards and evaluate it for the specified roles. The evaluation should be moderate to strict (not lenient). A resume should only receive a score between 90 and 100 if it is nearly perfect across all aspects and the content is highly relevant to the specified roles. If any section content is irrelevant to the role, give zero points for that section.\n" +
                "\nBefore analyzing, ensure the roles and resume content match each other and that the resume content is actual content of a real resume (refer: 1. rules and instructions). If it is unrelated, simply treat it as irrelevant content and follow the instructions for irrelevant content. " +
                "Analyze this resume for roles: " + roles + "\n" +
                "Resume Content:\n" +
                "\n" +
                "Rules and Instructions:\n" +
                "1. Evaluation Categories and Score Allocation (Total 100 points, conditional on role relevance):\n" +
                "- Contact Information (name, email, phone, LinkedIn/GitHub) - 15 points (always scored if present)\n" +
                "- Professional Summary / Objective - 10 points (only score if aligned with role)\n" +
                "- Skills (hard skills, tools, technologies) - 7 points (zero if skills not relevant to role)\n" +
                "- Education (degree, college, graduation year) - 10 points (score only if relevant for role)\n" +
                "- Achievements / Projects (relevant and measurable) - 15 points (zero if not relevant to role)\n" +
                "- Keywords / ATS readiness - 10 points (score only for role-relevant keywords)\n" +
                "- Formatting / Presentation - 5 points (always scored if well formatted)\n" +
                "- No grammatical or spelling mistakes (deduct 5 points if any) - 10 points\n" +
                "- Basic resume evaluation (must meet ATS parsing requirements) - 10 points (score only if structured properly for role content)\n" +
                "- Professional structure and proper layout - 5 points (always scored if proper layout)\n" +
                "- Skills matched with roles - 8 points (zero if skills do not match role)\n" +
                "\n" +
                "2. ATS Optimization Score (0-100):\n" +
                "- Score separately based on ATS parsing readiness, keyword usage, readability, section clarity, lack of graphics/tables, content relevance, and alignment with target role.\n" +
                "- If resume contains irrelevant content for the role, give 0 for the atsoptimizationscore.\n" +
                "\n" +
                "3. Scoring Philosophy:\n" +
                "- Be strict with scoring.\n" +
                "- A resume should only score 90-100 if nearly flawless and fully relevant to the role.\n" +
                "- If any section content is irrelevant to the role, assign zero points for that section.\n" +
                "- 50-89: Resume is partially relevant but may lack keywords, formatting, or role alignment.\n" +
                "- Below 50: Resume has significant relevance or ATS issues.\n" +
                "\n" +
                "4. Evaluation Criteria (industrial ATS rules, all relevance-dependent):\n" +
                "- Proper headings: Contact Information, Summary, Skills, Education, Experience, Projects, Achievements.\n" +
                "- Bullet points for readability.\n" +
                "- No images, graphics, or tables that disrupt ATS parsing.\n" +
                "- Chronological or functional structure.\n" +
                "- Action-oriented language in achievements.\n" +
                "- Only include role-relevant keywords; irrelevant keywords give zero points.\n" +
                "- Balanced hard skills (technical) and soft skills relevant to role.\n" +
                "- Professional formatting: consistent fonts, bold section titles, simple layout.\n" +
                "- Concise, measurable content; no long irrelevant descriptions.\n" +
                "- No spelling or grammar mistakes.\n" +
                "- Education and work history clearly structured with dates and relevant to role.\n" +
                "\n" +
                "5. ATS Sub-Score Breakdown (0-score for each, based on ATS parsing readiness):\n" +
                "- keywordMatch: How well resume keywords match the target role (0-15)\n" +
                "- formatting: Formatting quality and ATS parseability (0-15)\n" +
                "- readability: Content readability and clarity (0-15)\n" +
                "- sectionClarity: Clear section headings and organization (0-15)\n" +
                "- contentRelevance: How relevant the content is to the target role (0-15)\n" +
                "- contactInfo: Contact information completeness (0-10)\n" +
                "- grammar: Grammar and spelling quality (0-15)\n" +
                "\n" +
                "6. Skills Analysis:\n" +
                "- Extract ALL technical and professional skills mentioned in the resume.\n" +
                "- Compare against what the target role typically requires.\n" +
                "- matchedSkills: skills found in resume that match the role requirements.\n" +
                "- missingSkills: skills typically required for the role but NOT found in the resume.\n" +
                "- Be comprehensive: include programming languages, frameworks, tools, platforms, methodologies, soft skills, certifications.\n" +
                "\n" +
                "7. Irrelevant content:\n" +
                "- If the resume is completely irrelevant to the role, return score and atsoptimizationscore as 0, empty arrays for pros, cons, suggestions, matchedSkills, missingSkills, and empty object for atsBreakdown.\n" +
                "\n" +
                "8. Multi-Dimensional Weighted Scoring (Phase 8):\n" +
                "Evaluate the resume across 6 dimensions. Each dimension scores 0-100.\n" +
                "- semanticRelevance: How well the resume content aligns with the target role's domain and responsibilities (0-100)\n" +
                "- technicalSkillCoverage: Coverage of required technical skills for the role (0-100)\n" +
                "- experienceAlignment: How well the candidate's experience matches the role requirements (0-100)\n" +
                "- achievementQuality: Quality and measurability of achievements and projects (0-100)\n" +
                "- keywordDensity: Density of role-relevant keywords throughout the resume (0-100)\n" +
                "- formattingConfidence: Confidence in ATS parsing and formatting quality (0-100)\n\n" +
                "For each dimension, provide a brief human-readable explanation (max 100 chars) highlighting the key insight.\n" +
                "Example: \"Backend expertise underrepresented for this role\" or \"Strong keyword alignment with job requirements\"\n\n" +
                "9. Output Format:\n" +
                "Return strict raw JSON only (alphanumeric only, no symbols, no commentary). Response structure:\n" +
                "{\n" +
                "  \"score\": number,\n" +
                "  \"atsoptimizationscore\": number,\n" +
                "  \"pros\": [array of strings](String length <275(chars)),\n" +
                "  \"cons\": [array of strings](String length <275(chars)),\n" +
                "  \"suggestions\": [array of strings](String length <275(chars)),\n" +
                "  \"atsBreakdown\": {\n" +
                "    \"keywordMatch\": number,\n" +
                "    \"formatting\": number,\n" +
                "    \"readability\": number,\n" +
                "    \"sectionClarity\": number,\n" +
                "    \"contentRelevance\": number,\n" +
                "    \"contactInfo\": number,\n" +
                "    \"grammar\": number\n" +
                "  },\n" +
                "  \"dimensionScores\": {\n" +
                "    \"semanticRelevance\": number,\n" +
                "    \"technicalSkillCoverage\": number,\n" +
                "    \"experienceAlignment\": number,\n" +
                "    \"achievementQuality\": number,\n" +
                "    \"keywordDensity\": number,\n" +
                "    \"formattingConfidence\": number\n" +
                "  },\n" +
                "  \"dimensionExplanations\": {\n" +
                "    \"semanticRelevance\": \"string (max 100 chars)\",\n" +
                "    \"technicalSkillCoverage\": \"string (max 100 chars)\",\n" +
                "    \"experienceAlignment\": \"string (max 100 chars)\",\n" +
                "    \"achievementQuality\": \"string (max 100 chars)\",\n" +
                "    \"keywordDensity\": \"string (max 100 chars)\",\n" +
                "    \"formattingConfidence\": \"string (max 100 chars)\"\n" +
                "  },\n" +
                "  \"matchedSkills\": [array of strings],\n" +
                "  \"missingSkills\": [array of strings]\n" +
                "}\n"
        );

        int maxRetries = 3;
        int attempts = 0;
        while (attempts < maxRetries) {
            try {
                return groqClient.complete(instruction, extracted);
            } catch (Exception e) {
                attempts++;
                if (attempts >= maxRetries) {
                    throw new ResumeAnalysisException("AI analysis failed after " + maxRetries + " attempts", e);
                }
                Thread.sleep(1500);
            }
        }
        throw new ResumeAnalysisException("AI analysis failed");
    }
}
