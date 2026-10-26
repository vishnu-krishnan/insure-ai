package com.insureai.insurance.adapter.inbound.web;

import java.util.Map;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

/**
 * Builds the service's RFC 9457 error bodies: standard fields plus a stable {@code code}
 * and the request's {@code correlationId}.
 */
public final class ApiProblem {
    private ApiProblem() {
    }

    public static ProblemDetail of(HttpStatus status, String title, String detail, String code) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        enrich(problem, code);
        return problem;
    }

    public static ResponseEntity<ProblemDetail> response(HttpStatus status, String title, String detail, String code) {
        return ResponseEntity.status(status).body(of(status, title, detail, code));
    }

    static void enrich(ProblemDetail problem, String code) {
        Map<String, Object> properties = problem.getProperties();
        if (properties == null || !properties.containsKey("code")) {
            problem.setProperty("code", code);
        }
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (correlationId != null && (properties == null || !properties.containsKey("correlationId"))) {
            problem.setProperty("correlationId", correlationId);
        }
    }
}
