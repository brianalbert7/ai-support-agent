package org.brian.aisupportagent.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String REQUEST_ID_MDC_KEY = "requestId";

    private static final Logger log = LoggerFactory.getLogger(RequestCorrelationFilter.class);
    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9._-]{1,100}");
    private static final String HEALTH_PATH = "/actuator/health";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = resolveRequestId(request.getHeader(REQUEST_ID_HEADER));
        long startNanos = System.nanoTime();
        boolean requestFailed = false;

        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            filterChain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException | Error exception) {
            requestFailed = true;
            throw exception;
        } finally {
            if (!request.getRequestURI().startsWith(HEALTH_PATH)) {
                long durationMillis = TimeUnit.NANOSECONDS.toMillis(
                        System.nanoTime() - startNanos
                );
                int responseStatus = requestFailed && response.getStatus() < 400
                        ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR
                        : response.getStatus();
                log.atInfo()
                        .addKeyValue("http.request.method", request.getMethod())
                        .addKeyValue("url.path", request.getRequestURI())
                        .addKeyValue("http.response.status_code", responseStatus)
                        .addKeyValue("event.duration_ms", durationMillis)
                        .log(
                                "HTTP {} {} completed with status {} in {} ms",
                                request.getMethod(),
                                request.getRequestURI(),
                                responseStatus,
                                durationMillis
                        );
            }
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }

    private String resolveRequestId(String suppliedRequestId) {
        if (suppliedRequestId != null && SAFE_REQUEST_ID.matcher(suppliedRequestId).matches()) {
            return suppliedRequestId;
        }
        return UUID.randomUUID().toString();
    }
}
