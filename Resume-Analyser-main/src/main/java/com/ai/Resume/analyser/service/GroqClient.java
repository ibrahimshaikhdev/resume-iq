package com.ai.Resume.analyser.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Thin client for Groq's OpenAI-compatible Chat Completions API.
 * Free tier (no credit card) on llama-3.1-8b-instant allows ~14,400 requests/day,
 * which comfortably covers 1,000+ resume analyses per day.
 *
 * Callers should treat a thrown exception as "AI unavailable" and fall back to
 * the offline heuristic engine, so analysis never hard-fails.
 */
@Component
public class GroqClient {

    private static final Logger log = LoggerFactory.getLogger(GroqClient.class);
    private static final String ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";

    @Value("${groqKey:}")
    private String groqKey;

    @Value("${groqModel:llama-3.1-8b-instant}")
    private String groqModel;

    /** Key resolved at startup from properties, env var, or the .env file directly. */
    private String resolvedKey;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @PostConstruct
    public void init() {
        resolvedKey = resolveKey();
        if (isConfigured()) {
            log.info("==== Groq AI is ENABLED (model={}) ====", groqModel);
        } else {
            log.warn("==== Groq AI is DISABLED (no key found) - using OFFLINE engine. Put GROQ_KEY in .env (project root) to enable real AI. ====");
        }
    }

    /** Resolve the key from (1) Spring property, (2) OS env var, (3) the .env file directly. */
    private String resolveKey() {
        if (valid(groqKey)) return groqKey.trim();

        String env = System.getenv("GROQ_KEY");
        if (valid(env)) return env.trim();

        try {
            java.nio.file.Path p = java.nio.file.Path.of(".env");
            if (java.nio.file.Files.exists(p)) {
                for (String line : java.nio.file.Files.readAllLines(p)) {
                    String t = line.trim();
                    if (t.startsWith("GROQ_KEY=")) {
                        String v = t.substring("GROQ_KEY=".length()).trim();
                        if (valid(v)) return v;
                    }
                }
            }
        } catch (Exception ignored) {
            // best-effort only
        }
        return null;
    }

    private boolean valid(String k) {
        return k != null && !k.isBlank() && !k.equalsIgnoreCase("test");
    }

    /** True only when a real key has been configured (not blank / not the placeholder). */
    public boolean isConfigured() {
        return valid(resolvedKey);
    }

    /**
     * Sends a system instruction + user content and returns the model's raw text reply.
     * Requests JSON output. Throws on any failure (missing key, non-200, network error).
     */
    public String complete(String systemPrompt, String userContent) throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException("Groq API key not configured");
        }

        ObjectNode root = mapper.createObjectNode();
        root.put("model", groqModel);
        root.put("temperature", 0);
        root.put("max_tokens", 8000); // allow the full JSON report; default is too small and truncates it
        root.putObject("response_format").put("type", "json_object");

        ArrayNode messages = root.putArray("messages");
        ObjectNode sys = messages.addObject();
        sys.put("role", "system");
        sys.put("content", systemPrompt);
        ObjectNode usr = messages.addObject();
        usr.put("role", "user");
        usr.put("content", userContent);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .header("Authorization", "Bearer " + resolvedKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(root)))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Groq API returned " + response.statusCode() + ": " + response.body());
        }

        JsonNode node = mapper.readTree(response.body());
        JsonNode choices = node.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw new RuntimeException("Groq API returned no choices: " + response.body());
        }
        return choices.get(0).path("message").path("content").asText();
    }
}
