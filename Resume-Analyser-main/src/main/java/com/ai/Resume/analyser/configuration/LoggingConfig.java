package com.ai.Resume.analyser.configuration;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(1)
public class LoggingConfig implements Filter {

    private static final String TRACE_ID = "traceId";
    private static final String REQUEST_ID = "requestId";
    private static final String CLIENT_IP = "clientIp";
    private static final String REQUEST_URI = "requestUri";
    private static final String REQUEST_METHOD = "requestMethod";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String traceId = UUID.randomUUID().toString().substring(0, 8);
        String requestId = UUID.randomUUID().toString().substring(0, 12);

        MDC.put(TRACE_ID, traceId);
        MDC.put(REQUEST_ID, requestId);
        MDC.put(CLIENT_IP, getClientIp(httpRequest));
        MDC.put(REQUEST_URI, httpRequest.getRequestURI());
        MDC.put(REQUEST_METHOD, httpRequest.getMethod());

        long startTime = System.currentTimeMillis();

        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = httpResponse.getStatus();

            MDC.put("statusCode", String.valueOf(status));
            MDC.put("durationMs", String.valueOf(duration));

            if (status >= 400) {
                org.slf4j.LoggerFactory.getLogger("ACCESS_LOG")
                        .warn("HTTP {} {} {} - {}ms - {}",
                                httpRequest.getMethod(),
                                httpRequest.getRequestURI(),
                                status,
                                duration,
                                getClientIp(httpRequest));
            }

            MDC.clear();
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
