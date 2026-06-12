package com.ai.Resume.analyser.service;

import com.ai.Resume.analyser.exception.ResourceNotFoundException;
import com.ai.Resume.analyser.exception.ResumeAnalysisException;
import com.ai.Resume.analyser.model.*;
import com.ai.Resume.analyser.model.JdMatchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ai.Resume.analyser.repository.AnalysisJobRepository;
import com.ai.Resume.analyser.repository.AnalysisResultRepository;
import com.ai.Resume.analyser.repository.JdMatchResultRepository;
import com.ai.Resume.analyser.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class ResumeAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ResumeAnalysisService.class);
    private static final int MAX_RETRIES = 3;

    @Value("${genKey}")
    private String genKey;

    @Value("${application-id}")
    private String applicationId;

    @Value("${application-api-key}")
    private String applicationApiKey;

    @Autowired
    private AnalysisResultRepository analysisResultRepository;

    @Autowired
    private AnalysisJobRepository analysisJobRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdMatchResultRepository jdMatchResultRepository;

    @Autowired
    private Tika tika;

    @Autowired
    private PerformanceMetricsService metricsService;

    @CacheEvict(value = {"lastReport", "analysisHistory"}, allEntries = true)
    public String extract(String roles, MultipartFile file) throws TikaException, IOException, InterruptedException {

        long startTime = System.currentTimeMillis();
        String uname = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Starting resume analysis for user={}, roles={}", uname, roles);

        // Check for duplicate analysis: same user, same roles, recent result
        AnalysisResult existingResult = analysisResultRepository
                .findFirstByEmailOrderByAnalyzedAtDesc(uname)
                .filter(r -> r.getRoles() != null && r.getRoles().equalsIgnoreCase(roles))
                .orElse(null);

        if (existingResult != null) {
            long ageMinutes = (System.currentTimeMillis() - existingResult.getAnalyzedAt().getTime()) / 60000;
            if (ageMinutes < 30) {
                log.info("Cache hit: reusing recent analysis for user={}, roles={}, age={}min", uname, roles, ageMinutes);
                metricsService.recordCacheHit();
                return "Analysed successfully";
            }
        }
        metricsService.recordCacheMiss();
        log.info("Cache miss: proceeding with Gemini API call for user={}", uname);

        ByteArrayInputStream inpfile = new ByteArrayInputStream(file.getBytes());
        String extracted = tika.parseToString(inpfile);

        String results = null;
        Client client = Client.builder().apiKey(genKey).build();
        Content content = Content.builder().parts(Part.fromText(extracted), Part.fromText("You are now an advanced enterprise-grade ATS resume checker. Your task is to analyze the given resume strictly based on industry-level ATS standards and evaluate it for the specified roles. The evaluation should be moderate to strict (not lenient). A resume should only receive a score between 90 and 100 if it is nearly perfect across all aspects and the content is highly relevant to the specified roles. If any section content is irrelevant to the role, give zero points for that section.\n" +
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

        )).build();

        int attempts = 0;
        while (attempts < MAX_RETRIES) {
            try {
                GenerateContentResponse response = client.models.generateContent("gemini-2.5-flash", content, GenerateContentConfig.builder().temperature(0.0f).build());
                results = response.text();
                break;
            } catch (Exception e) {
                attempts++;
                if (attempts >= MAX_RETRIES) {
                    throw new ResumeAnalysisException("AI analysis failed after " + MAX_RETRIES + " attempts", e);
                }
                Thread.sleep(1500);
            }
        }

        if (results.startsWith("```")) {
            int firstBrace = results.indexOf("{");
            int lastBrace = results.lastIndexOf("}");
            if (firstBrace != -1 && lastBrace != -1) {
                results = results.substring(firstBrace, lastBrace + 1);
            }
        }

        ObjectMapper objectMapper = new ObjectMapper();
        AnalysisResultDto resultDto = objectMapper.readValue(results, AnalysisResultDto.class);
        if (resultDto.getScore() != 0) {
            AnalysisResult processedData = new AnalysisResult();
            processedData.setEmail(uname);
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
            User user = userRepository.findById(uname).orElse(null);
            if (user != null) {
                user.setPreviousResults(true);
                userRepository.save(user);
            }
            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordAnalysisTime(duration);
            metricsService.recordAiCall();
            log.info("Analysis completed for user={}, score={}, duration={}ms", uname, resultDto.getScore(), duration);
            return "Analysed successfully";
        }

        long duration = System.currentTimeMillis() - startTime;
        metricsService.recordAnalysisTime(duration);
        metricsService.recordAiCall();
        log.warn("Analysis returned zero score for user={}, duration={}ms", uname, duration);
        throw new ResumeAnalysisException("Invalid document");
    }

    @Cacheable(value = "lastReport", key = "T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName()")
    public AnalysisResultDto lastReport() {
        String uname = SecurityContextHolder.getContext().getAuthentication().getName();
        AnalysisResult analysisResult = analysisResultRepository.findFirstByEmailOrderByAnalyzedAtDesc(uname).orElse(null);
        if (analysisResult != null) {
            RestTemplate restTemplate = new RestTemplate();
            List<Job> jobs = List.of();
            String url = "https://api.adzuna.com/v1/api/jobs/in/search/1?app_id=" + applicationId + "&app_key=" + applicationApiKey + "&what=" + analysisResult.getRoles() + "&where=tamilnadu&content-type=application/json";
            try {
                JobSearchResponse response = restTemplate.getForObject(url, JobSearchResponse.class);
                if (response != null && response.getResults() != null) {
                    jobs = response.getResults();
                }
            } catch (Exception e) {
                // Job recommendations are an optional add-on; never let them break the report.
                log.warn("Adzuna job fetch failed, returning report without job recommendations: {}", e.getMessage());
            }
            return new AnalysisResultDto(
                    analysisResult.getScore(),
                    analysisResult.getAtsoptimizationscore(),
                    analysisResult.getPros(),
                    analysisResult.getCons(),
                    analysisResult.getSuggestions(),
                    jobs,
                    analysisResult.getAtsbreakdown(),
                    analysisResult.getDimensionScores(),
                    analysisResult.getDimensionExplanations(),
                    analysisResult.getMatchedSkills(),
                    analysisResult.getMissingSkills()
            );
        } else {
            throw new ResourceNotFoundException("No previous Analysis");
        }
    }

    @CacheEvict(value = {"lastJdMatch", "jdMatchHistory"}, allEntries = true)
    public JdMatchResponse matchJd(String roles, MultipartFile file, String jobDescription) throws TikaException, IOException, InterruptedException {
        long startTime = System.currentTimeMillis();
        ByteArrayInputStream inpfile = new ByteArrayInputStream(file.getBytes());
        String extracted = tika.parseToString(inpfile);

        String results = null;
        Client client = Client.builder().apiKey(genKey).build();
        Content content = Content.builder().parts(Part.fromText(extracted), Part.fromText(
                "You are an advanced ATS and recruitment intelligence system. Your task is to compare a candidate's resume against a specific job description and provide a detailed match analysis.\n\n" +
                "Target Role: " + roles + "\n\n" +
                "Job Description:\n" + jobDescription + "\n\n" +
                "Resume Content:\n\n" +
                "Rules and Instructions:\n" +
                "1. Compare the resume against the specific job description provided (not generic role requirements).\n" +
                "2. Extract required skills, qualifications, and experience from the job description.\n" +
                "3. Check which of those requirements are present in the resume.\n" +
                "4. Be strict: partial matches should be marked as gaps unless clearly demonstrated.\n\n" +
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
        )).build();

        int attempts = 0;
        while (attempts < MAX_RETRIES) {
            try {
                GenerateContentResponse response = client.models.generateContent("gemini-2.5-flash", content, GenerateContentConfig.builder().temperature(0.0f).build());
                results = response.text();
                break;
            } catch (Exception e) {
                attempts++;
                if (attempts >= MAX_RETRIES) {
                    throw new ResumeAnalysisException("AI JD matching failed after " + MAX_RETRIES + " attempts", e);
                }
                Thread.sleep(1500);
            }
        }

        if (results.startsWith("```")) {
            int firstBrace = results.indexOf("{");
            int lastBrace = results.lastIndexOf("}");
            if (firstBrace != -1 && lastBrace != -1) {
                results = results.substring(firstBrace, lastBrace + 1);
            }
        }

        ObjectMapper objectMapper = new ObjectMapper();
        JdMatchResponse matchResponse = objectMapper.readValue(results, JdMatchResponse.class);

        String uname = SecurityContextHolder.getContext().getAuthentication().getName();
        JdMatchResult matchResult = new JdMatchResult();
        matchResult.setEmail(uname);
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

        metricsService.recordAnalysisTime(System.currentTimeMillis() - startTime);
        metricsService.recordAiCall();

        return matchResponse;
    }

    @Cacheable(value = "lastJdMatch", key = "T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName()")
    public JdMatchResponse lastJdMatch() {
        String uname = SecurityContextHolder.getContext().getAuthentication().getName();
        JdMatchResult result = jdMatchResultRepository.findFirstByEmailOrderByMatchPercentageDesc(uname).orElse(null);
        if (result == null) {
            throw new ResourceNotFoundException("No previous JD match found");
        }
        return new JdMatchResponse(
                result.getMatchPercentage(),
                result.getMatchedSkills(),
                result.getMissingSkills(),
                result.getStrengths(),
                result.getGaps(),
                result.getRecommendations(),
                result.getCategoryBreakdown()
        );
    }

    @Cacheable(value = "analysisHistory", key = "T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName()")
    public List<AnalysisResult> getAnalysisHistory() {
        String uname = SecurityContextHolder.getContext().getAuthentication().getName();
        return analysisResultRepository.findByEmailOrderByAnalyzedAtDesc(uname);
    }

    @Cacheable(value = "jdMatchHistory", key = "T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName()")
    public List<JdMatchResult> getJdMatchHistory() {
        String uname = SecurityContextHolder.getContext().getAuthentication().getName();
        return jdMatchResultRepository.findByEmailOrderByMatchedAtDesc(uname);
    }

    public AnalysisResult getReportById(Long id) {
        String uname = SecurityContextHolder.getContext().getAuthentication().getName();
        AnalysisResult result = analysisResultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        if (!result.getEmail().equals(uname)) {
            throw new ResourceNotFoundException("Report not found");
        }
        return result;
    }

    public ResponseEntity<String> logout() {
        HttpHeaders headers = new HttpHeaders();
        ResponseCookie cookie = ResponseCookie.from("entrypasstoken", "").httpOnly(true).secure(false).sameSite("Strict").maxAge(0).path("/").build();
        headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
        return new ResponseEntity<>("Successfully loggedOut", headers, HttpStatus.OK);
    }

    public ResponseEntity<String> deleteAccount() {
        String uname = SecurityContextHolder.getContext().getAuthentication().getName();
        userRepository.deleteById(uname);
        analysisResultRepository.deleteByEmail(uname);
        jdMatchResultRepository.deleteAll(jdMatchResultRepository.findByEmailOrderByMatchedAtDesc(uname));
        HttpHeaders headers = new HttpHeaders();
        ResponseCookie cookie = ResponseCookie.from("entrypasstoken", "").httpOnly(true).secure(false).sameSite("Strict").maxAge(0).path("/").build();
        headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
        return new ResponseEntity<>("Account deleted successfully", headers, HttpStatus.OK);
    }

    public LoginResponse tokenValidation() {
        String name = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findById(name).orElse(null);
        return new LoginResponse(user.getUsername(), user.getPreviousResults());
    }

    public AnalysisJob saveJob(AnalysisJob job) {
        return analysisJobRepository.save(job);
    }

    public Map<String, Object> getMetrics() {
        return metricsService.getMetrics();
    }

    public void resetMetrics() {
        metricsService.reset();
    }
}
