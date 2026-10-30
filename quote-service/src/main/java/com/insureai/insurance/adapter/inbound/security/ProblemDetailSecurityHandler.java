package com.insureai.insurance.adapter.inbound.security;

import java.io.IOException;
import java.net.URI;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import tools.jackson.databind.ObjectMapper;
import com.insureai.insurance.adapter.inbound.web.ApiProblem;

/**
 * Renders 401 and 403 responses as Problem Details while keeping the standard
 * {@code WWW-Authenticate} challenge headers.
 */
class ProblemDetailSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProblemDetailSecurityHandler.class);

    private final ObjectMapper objectMapper;
    private final BearerTokenAuthenticationEntryPoint bearerEntryPoint = new BearerTokenAuthenticationEntryPoint();
    private final BearerTokenAccessDeniedHandler bearerAccessDeniedHandler = new BearerTokenAccessDeniedHandler();

    ProblemDetailSecurityHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException exception
    ) throws IOException {
        LOGGER.warn("Authentication failed for {} {}: {}", request.getMethod(), request.getRequestURI(), exception.getMessage());
        bearerEntryPoint.commence(request, response, exception);
        write(
            request,
            response,
            ApiProblem.of(
                HttpStatus.UNAUTHORIZED,
                "Authentication required",
                "A valid bearer token is required to access this resource.",
                "UNAUTHENTICATED"
            )
        );
    }

    @Override
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException exception
    ) throws IOException {
        logAccessDenied(request);
        bearerAccessDeniedHandler.handle(request, response, exception);
        write(
            request,
            response,
            ApiProblem.of(
                HttpStatus.FORBIDDEN,
                "Access denied",
                "The provided credentials do not allow this operation.",
                "ACCESS_DENIED"
            )
        );
    }

    private static void logAccessDenied(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwt) {
            LOGGER.warn(
                "Access denied for {} {}: client={} authorities={}",
                request.getMethod(),
                request.getRequestURI(),
                jwt.getToken().getClaimAsString("azp"),
                jwt.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                    .filter(authority -> authority.startsWith("SCOPE_")).sorted().toList()
            );
        } else {
            LOGGER.warn("Access denied for {} {}", request.getMethod(), request.getRequestURI());
        }
    }

    private void write(HttpServletRequest request, HttpServletResponse response, ProblemDetail problem)
        throws IOException {
        problem.setInstance(URI.create(request.getRequestURI()));
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
