package com.be4fe_admin_aurora_performance.service;

import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.be4fe_admin_aurora_performance.client.CoreApiClient;
import com.be4fe_admin_aurora_performance.dto.auth.LoginRequest;
import com.be4fe_admin_aurora_performance.dto.auth.LoginResponse;
import com.be4fe_admin_aurora_performance.dto.auth.LogoutRequest;
import com.be4fe_admin_aurora_performance.dto.auth.RefreshTokenRequest;
import com.be4fe_admin_aurora_performance.exception.AuthenticationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAuthService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final CoreApiClient coreApiClient;

    private static final Set<String> ADMIN_KEYCLOAK_ROLES = Set.of("admin", "segretario_comunale");

    @Value("${keycloak.auth-server-url}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret:}")
    private String clientSecret;

    public LoginResponse login(LoginRequest request) {
        try {
            String tokenUrl = keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "password");
            body.add("client_id", clientId);
            body.add("username", request.getUsername());
            body.add("password", request.getPassword());
            if (clientSecret != null && !clientSecret.isBlank()) body.add("client_secret", clientSecret);

            ResponseEntity<String> response = restTemplate.exchange(
                    tokenUrl, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

            JsonNode tokenResponse = objectMapper.readTree(response.getBody());

            String accessToken = tokenResponse.get("access_token").asText();
            String refreshToken = tokenResponse.get("refresh_token").asText();

            // Validate user has admin role
            Set<String> roles = extractRolesFromToken(accessToken);
            boolean isAdmin = roles.stream().anyMatch(ADMIN_KEYCLOAK_ROLES::contains);
            if (!isAdmin) {
                log.warn("Login denied: user {} has no admin role (roles={})", request.getUsername(), roles);
                throw new AuthenticationException("NOT_ADMIN", "Accesso riservato agli amministratori");
            }

            // Sync user in core (best-effort)
            try {
                syncUserInCore(accessToken);
            } catch (Exception e) {
                log.warn("syncUserInCore failed (non-blocking): {}", e.getMessage());
            }

            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(tokenResponse.get("expires_in").asLong())
                    .refreshExpiresIn(tokenResponse.get("refresh_expires_in").asLong())
                    .scope(tokenResponse.has("scope") ? tokenResponse.get("scope").asText() : "")
                    .build();

        } catch (AuthenticationException e) {
            throw e;
        } catch (HttpClientErrorException e) {
            log.error("Keycloak auth failed: {}", e.getStatusCode());
            throw new AuthenticationException("AUTH_FAILED", "Credenziali non valide");
        } catch (Exception e) {
            log.error("Login error", e);
            throw new AuthenticationException("AUTH_ERROR", "Errore durante l'autenticazione");
        }
    }

    public LoginResponse refreshToken(RefreshTokenRequest request) {
        try {
            String tokenUrl = keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "refresh_token");
            body.add("client_id", clientId);
            body.add("refresh_token", request.getRefreshToken());
            if (clientSecret != null && !clientSecret.isBlank()) body.add("client_secret", clientSecret);

            ResponseEntity<String> response = restTemplate.exchange(
                    tokenUrl, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

            JsonNode tokenResponse = objectMapper.readTree(response.getBody());

            return LoginResponse.builder()
                    .accessToken(tokenResponse.get("access_token").asText())
                    .refreshToken(tokenResponse.get("refresh_token").asText())
                    .tokenType("Bearer")
                    .expiresIn(tokenResponse.get("expires_in").asLong())
                    .refreshExpiresIn(tokenResponse.get("refresh_expires_in").asLong())
                    .scope(tokenResponse.has("scope") ? tokenResponse.get("scope").asText() : "")
                    .build();

        } catch (HttpClientErrorException e) {
            throw new AuthenticationException("REFRESH_FAILED", "Refresh token non valido o scaduto");
        } catch (Exception e) {
            log.error("Refresh token error", e);
            throw new AuthenticationException("REFRESH_ERROR", "Errore durante il refresh del token");
        }
    }

    public void logout(LogoutRequest request) {
        try {
            if (request.getRefreshToken() == null) return;
            String logoutUrl = keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/logout";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", clientId);
            body.add("refresh_token", request.getRefreshToken());
            if (clientSecret != null && !clientSecret.isBlank()) body.add("client_secret", clientSecret);

            restTemplate.exchange(logoutUrl, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
        } catch (Exception e) {
            log.warn("Logout warning: {}", e.getMessage());
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Set<String> extractRolesFromToken(String accessToken) throws Exception {
        String[] parts = accessToken.split("\\.");
        if (parts.length < 2) return Set.of();
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
        JsonNode claims = objectMapper.readTree(payload);
        Set<String> roles = new HashSet<>();
        if (claims.has("realm_access") && claims.get("realm_access").has("roles")) {
            claims.get("realm_access").get("roles").forEach(r -> roles.add(r.asText()));
        }
        return roles;
    }

    @SuppressWarnings("unchecked")
    private void syncUserInCore(String accessToken) throws Exception {
        String[] parts = accessToken.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
        JsonNode claims = objectMapper.readTree(payload);

        String keycloakId = claims.get("sub").asText();
        String firstName = claims.has("given_name") ? claims.get("given_name").asText() : null;
        String lastName = claims.has("family_name") ? claims.get("family_name").asText() : null;
        String email = claims.has("email") ? claims.get("email").asText() : null;

        Set<String> roles = extractRolesFromToken(accessToken);
        String ruolo = resolveRuolo(roles);

        Map<String, Object> userPayload = new HashMap<>();
        userPayload.put("keycloakId", keycloakId);
        if (firstName != null) userPayload.put("nome", firstName);
        if (lastName != null) userPayload.put("cognome", lastName);
        if (email != null) userPayload.put("email", email);
        if (ruolo != null) userPayload.put("ruolo", ruolo);

        Optional<Map> existingOpt = coreApiClient.getUserByKeycloakId(keycloakId);

        if (existingOpt.isPresent()) {
            Map<String, Object> existing = existingOpt.get();
            Object userId = existing.get("id");
            if (userId != null) {
                Map<String, Object> merged = new HashMap<>(existing);
                merged.putAll(userPayload);
                coreApiClient.updateUser(Long.parseLong(userId.toString()), merged);
            }
        } else {
            coreApiClient.saveUser(userPayload);
        }
    }

    private String resolveRuolo(Set<String> roles) {
        if (roles.contains("admin")) return "AD";
        if (roles.contains("segretario_comunale")) return "SC";
        return null;
    }
}
