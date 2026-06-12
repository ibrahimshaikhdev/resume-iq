package com.ai.Resume.analyser.controller;

import com.ai.Resume.analyser.model.*;
import com.ai.Resume.analyser.repository.AnalysisJobRepository;
import com.ai.Resume.analyser.service.AsyncAnalysisService;
import com.ai.Resume.analyser.service.GroqClient;
import com.ai.Resume.analyser.service.HealthService;
import com.ai.Resume.analyser.service.PerformanceMetricsService;
import com.ai.Resume.analyser.service.PdfExportService;
import com.ai.Resume.analyser.service.PublicAnalysisService;
import com.ai.Resume.analyser.service.RecruiterSimulationService;
import com.ai.Resume.analyser.service.ResumeAnalysisService;
import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("resumeAnalyserCore/service/v1")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.HEAD})
public class ResumeController {

    @Autowired
    private ResumeAnalysisService analysisService;

    @Autowired
    private AsyncAnalysisService asyncAnalysisService;

    @Autowired
    private AnalysisJobRepository analysisJobRepository;

    @Autowired
    private PdfExportService pdfExportService;

    @Autowired
    private PerformanceMetricsService metricsService;

    @Autowired
    private HealthService healthService;

    @Autowired
    private RecruiterSimulationService simulationService;

    @Autowired
    private PublicAnalysisService publicAnalysisService;

    @Autowired
    private GroqClient groqClient;

    // ==================== DIAGNOSTICS ====================

    // Open http://localhost:8081/resumeAnalyserCore/service/v1/public/ai-status in a browser.
    @GetMapping("/public/ai-status")
    public Map<String, Object> aiStatus() {
        Map<String, Object> status = new java.util.LinkedHashMap<>();
        status.put("groqEnabled", groqClient.isConfigured());
        status.put("activeEngine", groqClient.isConfigured() ? "Groq AI (real analysis)" : "Offline heuristic (keyword-based)");
        status.put("lastRealUploadEngine", PublicAnalysisService.lastEngine);
        status.put("lastRealUploadError", PublicAnalysisService.lastError);
        try {
            String reply = groqClient.complete(
                    "You are a test. Reply with strict JSON only.",
                    "Return JSON: {\"ok\": true, \"msg\": \"groq is working\"}");
            status.put("liveTest", "SUCCESS");
            status.put("groqReply", reply);
        } catch (Exception e) {
            status.put("liveTest", "FAILED");
            status.put("error", e.getMessage());
        }
        return status;
    }

    // ==================== ASYNC JOB ENDPOINTS (Phase 5) ====================

    @PostMapping("/jobs/analyze")
    public Map<String, Object> submitAnalysisJob(@RequestParam String roles, @RequestParam MultipartFile file) throws IOException {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        AnalysisJob job = new AnalysisJob();
        job.setEmail(email);
        job.setRoles(roles);
        job.setStatus(AnalysisJob.JobStatus.QUEUED);
        job.setJobType("ANALYSIS");
        AnalysisJob savedJob = analysisJobRepository.save(job);

        asyncAnalysisService.processAnalysis(savedJob.getId(), roles, file.getBytes(), file.getOriginalFilename());

        return Map.of(
                "jobId", savedJob.getId(),
                "status", "QUEUED",
                "message", "Analysis job submitted successfully"
        );
    }

    @PostMapping("/jobs/jdMatch")
    public Map<String, Object> submitJdMatchJob(@RequestParam String roles, @RequestParam MultipartFile file, @RequestParam String jobDescription) throws IOException {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        AnalysisJob job = new AnalysisJob();
        job.setEmail(email);
        job.setRoles(roles);
        job.setStatus(AnalysisJob.JobStatus.QUEUED);
        job.setJobType("JD_MATCH");
        AnalysisJob savedJob = analysisJobRepository.save(job);

        asyncAnalysisService.processJdMatch(savedJob.getId(), roles, file.getBytes(), jobDescription);

        return Map.of(
                "jobId", savedJob.getId(),
                "status", "QUEUED",
                "message", "JD match job submitted successfully"
        );
    }

    @GetMapping("/jobs/{jobId}/status")
    public JobStatusResponse getJobStatus(@PathVariable Long jobId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return asyncAnalysisService.getJobStatus(jobId, email);
    }

    // ==================== EXISTING ENDPOINTS (backward compatibility) ====================

    @PostMapping("/extract")
    public String extract(@RequestParam String roles, @RequestParam MultipartFile file) throws TikaException, IOException, InterruptedException {
        return analysisService.extract(roles, file);
    }

    @GetMapping("/lastReport")
    public AnalysisResultDto lastReport() {
        return analysisService.lastReport();
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        return analysisService.logout();
    }

    @DeleteMapping("/deleteAccount")
    public ResponseEntity<String> deleteAccount() {
        return analysisService.deleteAccount();
    }

    @PostMapping("/matchJd")
    public JdMatchResponse matchJd(@RequestParam String roles, @RequestParam MultipartFile file, @RequestParam String jobDescription) throws TikaException, IOException, InterruptedException {
        return analysisService.matchJd(roles, file, jobDescription);
    }

    @GetMapping("/lastJdMatch")
    public JdMatchResponse lastJdMatch() {
        return analysisService.lastJdMatch();
    }

    @GetMapping("/history")
    public List<AnalysisResult> getAnalysisHistory() {
        return analysisService.getAnalysisHistory();
    }

    @GetMapping("/jdHistory")
    public List<JdMatchResult> getJdMatchHistory() {
        return analysisService.getJdMatchHistory();
    }

    @GetMapping("/report/{id}/pdf")
    public ResponseEntity<byte[]> downloadReportPdf(@PathVariable Long id) {
        AnalysisResult result = analysisService.getReportById(id);
        byte[] pdfBytes = pdfExportService.generateReport(result);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=resumeiq-report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @PostMapping("/isValid")
    public LoginResponse tokenValidation() {
        return analysisService.tokenValidation();
    }

    // ==================== PERFORMANCE METRICS (Phase 6) ====================

    @GetMapping("/metrics")
    public Map<String, Object> getMetrics() {
        return metricsService.getMetrics();
    }

    @PostMapping("/metrics/reset")
    public Map<String, String> resetMetrics() {
        metricsService.reset();
        return Map.of("status", "Metrics reset successfully");
    }

    // ==================== HEALTH MONITORING (Phase 7) ====================

    @GetMapping("/health")
    public Map<String, Object> getHealth() {
        return healthService.getHealth();
    }

    // ==================== RECRUITER SIMULATION (Phase 9) ====================

    @PostMapping("/simulate")
    public RecruiterSimulationDto simulateRecruiters(@RequestParam String roles, @RequestParam MultipartFile file) throws IOException, org.apache.tika.exception.TikaException {
        org.apache.tika.Tika tika = new org.apache.tika.Tika();
        String resumeText = tika.parseToString(file.getInputStream());
        return simulationService.simulate(roles, resumeText);
    }

    @GetMapping("/lastSimulation")
    public RecruiterSimulationDto getLastSimulation() {
        return simulationService.getLastSimulation();
    }

    // ==================== PUBLIC ATS SCORE (no auth required) ====================

    @PostMapping("/public/ats-score")
    public ResponseEntity<?> publicAtsScore(@RequestParam String roles, @RequestParam MultipartFile file) {
        try {
            AnonymousAnalysis result = publicAnalysisService.analyzePublic(file.getBytes(), roles);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== CLAIM ANONYMOUS RESULTS ====================

    @PostMapping("/claimAnonymous")
    public ResponseEntity<Map<String, Object>> claimAnonymous(@RequestBody Map<String, String> request) {
        String contentHash = request.get("contentHash");
        if (contentHash == null || contentHash.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "contentHash is required"));
        }

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        AnalysisResult claimed = publicAnalysisService.claimAnalysis(contentHash, email);

        if (claimed == null) {
            return ResponseEntity.ok(Map.of("claimed", false, "message", "No anonymous result found for this hash"));
        }

        return ResponseEntity.ok(Map.of("claimed", true, "resultId", claimed.getId()));
    }
}
