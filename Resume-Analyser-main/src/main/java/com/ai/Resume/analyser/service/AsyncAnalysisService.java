package com.ai.Resume.analyser.service;

import com.ai.Resume.analyser.exception.ResourceNotFoundException;
import com.ai.Resume.analyser.exception.ResumeAnalysisException;
import com.ai.Resume.analyser.model.*;
import com.ai.Resume.analyser.repository.AnalysisJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ai.Resume.analyser.repository.AnalysisResultRepository;
import com.ai.Resume.analyser.repository.JdMatchResultRepository;
import com.ai.Resume.analyser.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class AsyncAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AsyncAnalysisService.class);
    private static final long ANALYSIS_TIMEOUT_MS = 180000; // 3 minutes

    @Value("${genKey}")
    private String genKey;

    @Autowired
    private AnalysisJobRepository analysisJobRepository;

    @Autowired
    private AnalysisResultRepository analysisResultRepository;

    @Autowired
    private JdMatchResultRepository jdMatchResultRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private Tika tika;

    @Autowired
    private PerformanceMetricsService metricsService;

    @Autowired
    private PublicAnalysisService publicAnalysisService;

    @Autowired
    private GroqClient groqClient;

    @Async("analysisExecutor")
    public CompletableFuture<Void> processAnalysis(Long jobId, String roles, byte[] fileBytes, String fileName) {
        AnalysisJob job = analysisJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        try {
            long startTime = System.currentTimeMillis();
            log.info("Starting async analysis jobId={}, email={}, roles={}", jobId, job.getEmail(), roles);
            job.setStatus(AnalysisJob.JobStatus.PROCESSING);
            job.setStartedAt(new Date());
            analysisJobRepository.save(job);

            // Stage 1: Document parsing
            String extracted = tika.parseToString(new ByteArrayInputStream(fileBytes));

            // Stage 2: AI Scoring
            job.setStatus(AnalysisJob.JobStatus.SCORING);
            analysisJobRepository.save(job);

            AnalysisResultDto resultDto;
            try {
                String results = callGeminiForAnalysis(extracted, roles);

                if (results.startsWith("```")) {
                    int firstBrace = results.indexOf("{");
                    int lastBrace = results.lastIndexOf("}");
                    if (firstBrace != -1 && lastBrace != -1) {
                        results = results.substring(firstBrace, lastBrace + 1);
                    }
                }

                resultDto = new ObjectMapper().readValue(results, AnalysisResultDto.class);
            } catch (Exception aiError) {
                log.warn("Gemini API failed for jobId={}, falling back to heuristic analysis: {}", jobId, aiError.getMessage());
                resultDto = publicAnalysisService.generateMockAnalysis(extracted, roles);
            }

            // Stage 3: Recommendations parsing
            job.setStatus(AnalysisJob.JobStatus.RECOMMENDATIONS);
            analysisJobRepository.save(job);

            if (resultDto.getScore() != 0) {
                AnalysisResult processedData = new AnalysisResult();
                processedData.setEmail(job.getEmail());
                processedData.setScore(resultDto.getScore());
                processedData.setAtsoptimizationscore(resultDto.getAtsoptimizationscore());
                processedData.setRoles(roles);
                processedData.setPros(resultDto.getPros());
                processedData.setCons(resultDto.getCons());
                processedData.setSuggestions(resultDto.getSuggestions());
                processedData.setAtsbreakdown(resultDto.getAtsbreakdown());
                processedData.setDimensionScores(resultDto.getDimensionScores());
                processedData.setDimensionExplanations(resultDto.getDimensionExplanations());
                processedData.setMatchedSkills(resultDto.getMatchedSkills());
                processedData.setMissingSkills(resultDto.getMissingSkills());
                analysisResultRepository.save(processedData);

                User user = userRepository.findById(job.getEmail()).orElse(null);
                if (user != null) {
                    user.setPreviousResults(true);
                    userRepository.save(user);
                }
            }

            // Stage 4: Completed
            long duration = System.currentTimeMillis() - startTime;
            job.setStatus(AnalysisJob.JobStatus.COMPLETED);
            job.setCompletedAt(new Date());
            analysisJobRepository.save(job);
            metricsService.recordAnalysisTime(duration);
            log.info("Async analysis completed jobId={}, email={}, duration={}ms", jobId, job.getEmail(), duration);

        } catch (Exception e) {
            log.error("Async analysis failed jobId={}, email={}, error={}", jobId, job.getEmail(), e.getMessage(), e);
            handleJobFailure(job, e);
        }

        return CompletableFuture.completedFuture(null);
    }

    @Async("analysisExecutor")
    public CompletableFuture<Void> processJdMatch(Long jobId, String roles, byte[] fileBytes, String jobDescription) {
        AnalysisJob job = analysisJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        try {
            long startTime = System.currentTimeMillis();
            log.info("Starting async JD match jobId={}, email={}, roles={}", jobId, job.getEmail(), roles);
            job.setStatus(AnalysisJob.JobStatus.PROCESSING);
            job.setStartedAt(new Date());
            analysisJobRepository.save(job);

            // Stage 1: Document parsing
            String extracted = tika.parseToString(new ByteArrayInputStream(fileBytes));

            // Stage 2: AI Matching
            job.setStatus(AnalysisJob.JobStatus.SCORING);
            analysisJobRepository.save(job);

            // Stage 2 continued: try AI, fall back to offline heuristic matcher
            JdMatchResponse matchResponse;
            try {
                String results = callGeminiForJdMatch(extracted, roles, jobDescription);
                if (results.startsWith("```")) {
                    int firstBrace = results.indexOf("{");
                    int lastBrace = results.lastIndexOf("}");
                    if (firstBrace != -1 && lastBrace != -1) {
                        results = results.substring(firstBrace, lastBrace + 1);
                    }
                }
                matchResponse = new ObjectMapper().readValue(results, JdMatchResponse.class);
            } catch (Exception aiError) {
                log.warn("Groq JD match failed for jobId={}, falling back to heuristic matcher: {}", jobId, aiError.getMessage());
                matchResponse = generateMockJdMatch(extracted, roles, jobDescription);
            }

            // Stage 3: Results parsing
            job.setStatus(AnalysisJob.JobStatus.RECOMMENDATIONS);
            analysisJobRepository.save(job);

            JdMatchResult matchResult = new JdMatchResult();
            matchResult.setEmail(job.getEmail());
            matchResult.setRoles(roles);
            matchResult.setJobDescription(jobDescription);
            matchResult.setMatchPercentage(matchResponse.getMatchPercentage());
            matchResult.setMatchedSkills(matchResponse.getMatchedSkills());
            matchResult.setMissingSkills(matchResponse.getMissingSkills());
            matchResult.setStrengths(matchResponse.getStrengths());
            matchResult.setGaps(matchResponse.getGaps());
            matchResult.setRecommendations(matchResponse.getRecommendations());
            matchResult.setCategoryBreakdown(matchResponse.getCategoryBreakdown());
            jdMatchResultRepository.save(matchResult);

            // Stage 4: Completed
            job.setStatus(AnalysisJob.JobStatus.COMPLETED);
            job.setCompletedAt(new Date());
            analysisJobRepository.save(job);

        } catch (Exception e) {
            handleJobFailure(job, e);
        }

        return CompletableFuture.completedFuture(null);
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
                "7. Irrelevant or non-resume content:\n" +
                "- If the resume is completely irrelevant to the role, return score and atsoptimizationscore as 0, empty arrays for pros, cons, suggestions, matchedSkills, missingSkills, and empty object for atsBreakdown.\n" +
                "- If the uploaded content is NOT an actual resume/CV (e.g. it is gibberish, a single word like 'hi', random text, or any document that is clearly not a resume), treat it the same way: return score 0, atsoptimizationscore 0, all atsBreakdown values 0, empty arrays, and put a single suggestion: 'Upload a valid resume document.'\n" +
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

    private JdMatchResponse generateMockJdMatch(String resumeText, String roles, String jobDescription) {
        String resume = resumeText.toLowerCase();
        String jd = jobDescription.toLowerCase();

        String[] vocab = {
                "java", "python", "javascript", "typescript", "c++", "c#", "go", "ruby", "php", "kotlin", "swift", "scala",
                "react", "angular", "vue", "node", "express", "spring boot", "spring", "django", "flask", "fastapi", ".net", "laravel",
                "mysql", "postgresql", "postgres", "mongodb", "redis", "oracle", "sql", "nosql", "elasticsearch",
                "aws", "azure", "gcp", "docker", "kubernetes", "jenkins", "terraform", "ci/cd", "linux", "nginx",
                "git", "github", "gitlab", "jira", "rest", "graphql", "microservices", "kafka", "rabbitmq",
                "html", "css", "tailwind", "bootstrap", "sass", "redux", "next.js",
                "agile", "scrum", "testing", "junit", "selenium", "cypress", "tdd",
                "machine learning", "data analysis", "pandas", "numpy", "tensorflow", "pytorch",
                "communication", "leadership", "teamwork", "problem solving"
        };

        List<String> required = new ArrayList<>();
        for (String s : vocab) {
            if (jd.contains(s) && !required.contains(s)) required.add(s);
        }
        if (required.isEmpty()) {
            for (String kw : roles.toLowerCase().split("[,\\s]+")) {
                if (kw.length() > 2) required.add(kw);
            }
        }

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String s : required) {
            if (resume.contains(s)) matched.add(cap(s));
            else missing.add(cap(s));
        }

        int total = matched.size() + missing.size();
        int skillPct = total > 0 ? (int) Math.round(100.0 * matched.size() / total) : 50;

        boolean hasExp = resume.contains("experience") || resume.contains("worked") || resume.matches("(?s).*\\b(19|20)\\d{2}\\b.*");
        boolean hasEdu = resume.contains("education") || resume.contains("bachelor") || resume.contains("degree") || resume.contains("university");

        int overall = (int) Math.round(skillPct * 0.6 + (hasExp ? 20 : 8) + (hasEdu ? 10 : 4));
        overall = Math.max(0, Math.min(overall, 100));

        List<String> strengths = new ArrayList<>();
        if (!matched.isEmpty()) strengths.add("Resume covers " + matched.size() + " of " + total + " key skills from the job description");
        if (hasExp) strengths.add("Relevant work experience is present");
        if (hasEdu) strengths.add("Educational background is clearly stated");
        if (strengths.isEmpty()) strengths.add("Resume is parseable and structured for ATS");

        List<String> gaps = new ArrayList<>();
        for (int i = 0; i < Math.min(missing.size(), 6); i++) gaps.add("Missing or under-represented: " + missing.get(i));
        if (gaps.isEmpty()) gaps.add("No major skill gaps detected against this job description");

        List<String> recommendations = new ArrayList<>();
        if (!missing.isEmpty()) {
            recommendations.add("Add these skills if you have them: " + String.join(", ", missing.subList(0, Math.min(missing.size(), 6))));
        }
        recommendations.add("Mirror keywords from the job description in your summary and skills sections");
        recommendations.add("Quantify achievements with metrics relevant to the target role: " + roles);

        Map<String, JdMatchResponse.CategoryScore> breakdown = new LinkedHashMap<>();
        breakdown.put("technicalSkills", cat(Math.round(skillPct / 100.0 * 25), 25));
        breakdown.put("experience", cat(hasExp ? 16 : 7, 20));
        breakdown.put("education", cat(hasEdu ? 12 : 6, 15));
        breakdown.put("softSkills", cat(resume.contains("communication") || resume.contains("team") ? 8 : 4, 10));
        breakdown.put("domainKnowledge", cat(Math.round(skillPct / 100.0 * 15), 15));
        breakdown.put("keywords", cat(Math.round(skillPct / 100.0 * 15), 15));

        JdMatchResponse resp = new JdMatchResponse();
        resp.setMatchPercentage(overall);
        resp.setMatchedSkills(matched);
        resp.setMissingSkills(missing);
        resp.setStrengths(strengths);
        resp.setGaps(gaps);
        resp.setRecommendations(recommendations);
        resp.setCategoryBreakdown(breakdown);
        return resp;
    }

    private JdMatchResponse.CategoryScore cat(long score, int max) {
        int s = (int) Math.max(0, Math.min(score, max));
        String status = s >= max * 0.75 ? "strong" : s >= max * 0.5 ? "moderate" : s > 0 ? "weak" : "missing";
        return new JdMatchResponse.CategoryScore(s, max, status);
    }

    private String cap(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String callGeminiForJdMatch(String extracted, String roles, String jobDescription) throws InterruptedException {
        String instruction = (
                "You are an advanced ATS and recruitment intelligence system. Your task is to compare a candidate's resume against a specific job description and provide a detailed match analysis.\n\n" +
                "Target Role: " + roles + "\n\n" +
                "Job Description:\n" + jobDescription + "\n\n" +
                "Resume Content:\n\n" +
                "Rules and Instructions:\n" +
                "1. Compare the resume against the specific job description provided (not generic role requirements).\n" +
                "2. Extract required skills, qualifications, and experience from the job description.\n" +
                "3. Check which of those requirements are present in the resume.\n" +
                "4. Be strict: partial matches should be marked as gaps unless clearly demonstrated.\n" +
                "4a. CRITICAL: If the job description is empty, gibberish, or does NOT describe an actual job (e.g. 'blah blah', random characters, 'hi', or a few meaningless words with no real skills/requirements), then it contains zero requirements. In that case return matchPercentage 0, all category scores 0 with status 'missing', empty arrays for matchedSkills/strengths, and a single recommendation: 'Provide a real job description to get a meaningful match.' Do NOT award points for formatting or a good resume when the JD itself is meaningless.\n\n" +
                "5. Scoring Categories (total 100 points):\n" +
                "- technicalSkills: Programming languages, frameworks, tools, technologies mentioned in JD (0-25)\n" +
                "- experience: Years and type of experience required by JD (0-20)\n" +
                "- education: Degree, certifications, qualifications from JD (0-15)\n" +
                "- softSkills: Communication, leadership, teamwork etc. from JD (0-10)\n" +
                "- domainKnowledge: Industry-specific knowledge, methodologies from JD (0-15)\n" +
                "- keywords: Specific keywords and phrases from JD found in resume (0-15)\n\n" +
                "6. For each category, set status as:\n" +
                "- \"strong\": score >= 75% of max\n" +
                "- \"moderate\": score >= 50% of max\n" +
                "- \"weak\": score >= 25% of max\n" +
                "- \"missing\": score < 25% of max\n\n" +
                "7. Output Format:\n" +
                "Return strict raw JSON only. No markdown, no commentary.\n" +
                "{\n" +
                "  \"matchPercentage\": number (0-100, weighted average of all categories),\n" +
                "  \"matchedSkills\": [array of strings - skills found in both resume and JD],\n" +
                "  \"missingSkills\": [array of strings - skills required by JD but missing from resume],\n" +
                "  \"strengths\": [array of strings - areas where candidate strongly matches JD],\n" +
                "  \"gaps\": [array of strings - specific gaps relative to this JD],\n" +
                "  \"recommendations\": [array of strings - actionable suggestions to improve match for this specific JD],\n" +
                "  \"categoryBreakdown\": {\n" +
                "    \"technicalSkills\": {\"score\": number, \"maxScore\": 25, \"status\": \"strong|moderate|weak|missing\"},\n" +
                "    \"experience\": {\"score\": number, \"maxScore\": 20, \"status\": \"strong|moderate|weak|missing\"},\n" +
                "    \"education\": {\"score\": number, \"maxScore\": 15, \"status\": \"strong|moderate|weak|missing\"},\n" +
                "    \"softSkills\": {\"score\": number, \"maxScore\": 10, \"status\": \"strong|moderate|weak|missing\"},\n" +
                "    \"domainKnowledge\": {\"score\": number, \"maxScore\": 15, \"status\": \"strong|moderate|weak|missing\"},\n" +
                "    \"keywords\": {\"score\": number, \"maxScore\": 15, \"status\": \"strong|moderate|weak|missing\"}\n" +
                "  }\n" +
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
                    throw new ResumeAnalysisException("AI JD matching failed after " + maxRetries + " attempts", e);
                }
                Thread.sleep(1500);
            }
        }
        throw new ResumeAnalysisException("AI JD matching failed");
    }

    private void handleJobFailure(AnalysisJob job, Exception e) {
        job.setRetryCount(job.getRetryCount() + 1);

        if (job.getRetryCount() < job.getMaxRetries()) {
            // Retry: reset status to QUEUED
            job.setStatus(AnalysisJob.JobStatus.QUEUED);
            job.setErrorMessage("Retry " + job.getRetryCount() + " of " + job.getMaxRetries() + ": " + e.getMessage());
            analysisJobRepository.save(job);

            // Re-trigger processing
            if ("ANALYSIS".equals(job.getJobType())) {
                // For analysis jobs, we need the original file bytes which we don't have here
                // Mark as failed with retry info - frontend can resubmit
                job.setStatus(AnalysisJob.JobStatus.FAILED);
                job.setErrorMessage("Processing failed after " + job.getRetryCount() + " attempts. Please resubmit. Error: " + e.getMessage());
            } else {
                job.setStatus(AnalysisJob.JobStatus.FAILED);
                job.setErrorMessage("Processing failed after " + job.getRetryCount() + " attempts. Please resubmit. Error: " + e.getMessage());
            }
        } else {
            job.setStatus(AnalysisJob.JobStatus.FAILED);
            job.setErrorMessage("Analysis failed after " + job.getMaxRetries() + " attempts: " + e.getMessage());
        }

        job.setCompletedAt(new Date());
        analysisJobRepository.save(job);
    }

    public JobStatusResponse getJobStatus(Long jobId, String email) {
        AnalysisJob job = analysisJobRepository.findByIdAndEmail(jobId, email)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        JobStatusResponse response = new JobStatusResponse();
        response.setJobId(job.getId());
        response.setStatus(job.getStatus().name());
        response.setJobType(job.getJobType());
        response.setRoles(job.getRoles());
        response.setErrorMessage(job.getErrorMessage());
        response.setRetryCount(job.getRetryCount());
        response.setCreatedAt(job.getCreatedAt());
        response.setStartedAt(job.getStartedAt());
        response.setCompletedAt(job.getCompletedAt());
        response.setUpdatedAt(job.getUpdatedAt());

        // If completed, include the result
        if (job.getStatus() == AnalysisJob.JobStatus.COMPLETED) {
            if ("ANALYSIS".equals(job.getJobType())) {
                AnalysisResult latest = analysisResultRepository.findFirstByEmailOrderByAnalyzedAtDesc(email).orElse(null);
                if (latest != null) {
                    AnalysisResultDto dto = new AnalysisResultDto(
                            latest.getScore(),
                            latest.getAtsoptimizationscore(),
                            latest.getPros(),
                            latest.getCons(),
                            latest.getSuggestions(),
                            null, // jobs fetched separately
                            latest.getAtsbreakdown(),
                            latest.getDimensionScores(),
                            latest.getDimensionExplanations(),
                            latest.getMatchedSkills(),
                            latest.getMissingSkills()
                    );
                    response.setResult(dto);
                }
            }
        }

        return response;
    }
}
