package com.insureai.customer.adapter.inbound.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import tools.jackson.databind.ObjectMapper;

@Configuration
public class CustomerSecurityConfiguration {
    @Bean
    SecurityFilterChain customerSecurityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        ProblemDetailSecurityHandler problemHandler = new ProblemDetailSecurityHandler(objectMapper);
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/customers")
                    .hasAuthority("SCOPE_customer:create")
                .requestMatchers(HttpMethod.GET, "/api/v1/customers/**")
                    .hasAuthority("SCOPE_customer:read")
                .anyRequest().denyAll())
            .oauth2ResourceServer(resourceServer -> resourceServer
                .jwt(Customizer.withDefaults())
                .authenticationEntryPoint(problemHandler)
                .accessDeniedHandler(problemHandler))
            .build();
    }

    @Bean
    JwtDecoder customerJwtDecoder(
        @Value("${security.jwt.issuer-uri}") String issuerUri,
        @Value("${security.jwt.audience}") String audience
    ) {
        JwtDecoder decoder = JwtDecoders.fromIssuerLocation(issuerUri);
        ((NimbusJwtDecoder) decoder)
            .setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuerUri),
                jwt -> jwt.getAudience().contains(audience)
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                        "invalid_token",
                        "Required audience is missing",
                        null
                    ))
            ));
        return decoder;
    }
}
