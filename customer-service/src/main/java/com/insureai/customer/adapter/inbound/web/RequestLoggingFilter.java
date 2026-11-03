package com.insureai.customer.adapter.inbound.web;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Writes one summary line per request: INFO normally, WARN for 4xx responses and slow requests, ERROR for 5xx.
 * Query strings, headers and bodies are deliberately not logged.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final long SLOW_REQUEST_MILLIS = 2_000;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        long startNanos = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;
            int status = response.getStatus();
            boolean slow = elapsedMillis >= SLOW_REQUEST_MILLIS;
            Level level = status >= 500 ? Level.ERROR : (status >= 400 || slow) ? Level.WARN : Level.INFO;
            LOGGER.atLevel(level)
                .setMessage("{} {} -> {} ({} ms){}")
                .addArgument(request.getMethod())
                .addArgument(request.getRequestURI())
                .addArgument(status)
                .addArgument(elapsedMillis)
                .addArgument(slow ? " slow request" : "")
                .addKeyValue("http.request.method", request.getMethod())
                .addKeyValue("url.path", request.getRequestURI())
                .addKeyValue("http.response.status_code", status)
                .addKeyValue("durationMs", elapsedMillis)
                .log();
        }
    }
}
