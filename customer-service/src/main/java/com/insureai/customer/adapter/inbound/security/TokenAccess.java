package com.insureai.customer.adapter.inbound.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

/**
 * Scope and role rules shared by the security configuration.
 *
 * <p>Tokens issued to the customer-facing web client carry every scope the client may use, so for those tokens the
 * scope alone does not say who is calling. A signed-in customer must not reach the agent APIs, so for web-client
 * tokens the agent endpoints also require the realm role {@code agent}. Service clients (client credentials) are
 * not affected.
 */
final class TokenAccess {
    private TokenAccess() {
    }

    /** Maps scopes to SCOPE_x and realm roles to ROLE_x. */
    static JwtAuthenticationConverter converter() {
        JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
        Converter<Jwt, Collection<GrantedAuthority>> withRoles = jwt -> {
            List<GrantedAuthority> authorities = new ArrayList<>(scopes.convert(jwt));
            for (String role : realmRoles(jwt)) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }
            return authorities;
        };
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(withRoles);
        return converter;
    }

    /** Agent API: the scope, plus the agent role when the token was issued to the web client. */
    static AuthorizationManager<RequestAuthorizationContext> agentApi(String scope, String webClientId) {
        return (authentication, context) -> {
            var auth = authentication.get();
            if (!(auth instanceof JwtAuthenticationToken token) || !has(auth.getAuthorities(), "SCOPE_" + scope)) {
                return new AuthorizationDecision(false);
            }
            boolean webToken = webClientId.equals(token.getToken().getClaimAsString("azp"));
            return new AuthorizationDecision(!webToken || has(auth.getAuthorities(), "ROLE_agent"));
        };
    }

    /** Self-service API: the me:* scope and the customer role. */
    static AuthorizationManager<RequestAuthorizationContext> selfService(String scope) {
        return (authentication, context) -> {
            var auth = authentication.get();
            return new AuthorizationDecision(auth != null
                && has(auth.getAuthorities(), "SCOPE_" + scope)
                && has(auth.getAuthorities(), "ROLE_customer"));
        };
    }

    private static boolean has(Collection<? extends GrantedAuthority> authorities, String name) {
        return authorities.stream().anyMatch(authority -> name.equals(authority.getAuthority()));
    }

    @SuppressWarnings("unchecked")
    private static List<String> realmRoles(Jwt jwt) {
        Object realmAccess = jwt.getClaim("realm_access");
        if (realmAccess instanceof Map<?, ?> map && map.get("roles") instanceof List<?> roles) {
            return (List<String>) roles;
        }
        return List.of();
    }
}
