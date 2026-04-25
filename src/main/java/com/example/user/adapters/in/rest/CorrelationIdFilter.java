package com.example.user.adapters.in.rest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Inbound adapter concern: attaches a correlation / trace ID to every request
 * so that all log lines emitted during that request can be correlated in
 * a log aggregator (ELK, Grafana Loki, etc.).
 *
 * <p>The ID is read from the {@code X-Trace-Id} request header when present
 * (allowing callers to propagate their own trace IDs), otherwise a new UUID
 * is generated. The value is echoed back in the {@code X-Trace-Id} response
 * header and placed in the SLF4J MDC so that the logback JSON encoder can
 * include it in every log record automatically.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    static final String TRACE_ID_HEADER = "X-Trace-Id";
    static final String MDC_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = Optional.ofNullable(request.getHeader(TRACE_ID_HEADER))
                .filter(v -> !v.isBlank())
                .orElse(UUID.randomUUID().toString());

        MDC.put(MDC_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);   // always clean up — thread may be reused
        }
    }
}

