package com.be4fe_admin_aurora_performance.controller;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.be4fe_admin_aurora_performance.client.CoreApiClient;
import com.be4fe_admin_aurora_performance.dto.session.AdminSessionResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/admin/session")
@RequiredArgsConstructor
@Slf4j
public class AdminSessionController {

    private final CoreApiClient coreApiClient;

    @PostMapping
    public ResponseEntity<AdminSessionResponse> getSession(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(AdminSessionResponse.error("SESSION_INVALID"));
        }

        Jwt jwt = (Jwt) authentication.getPrincipal();
        Optional<Map> userOpt = coreApiClient.getUserByKeycloakId(jwt.getSubject());

        if (userOpt.isEmpty()) {
            log.warn("Admin session: user not found for keycloakId={}", jwt.getSubject());
            return ResponseEntity.status(401).body(AdminSessionResponse.error("USER_NOT_FOUND"));
        }

        Map<String, Object> user = userOpt.get();
        AdminSessionResponse.AdminSessionUser userDto = AdminSessionResponse.AdminSessionUser.builder()
                .id(user.get("id") != null ? Integer.parseInt(user.get("id").toString()) : null)
                .nome(strVal(user, "nome", jwt.getClaimAsString("given_name")))
                .cognome(strVal(user, "cognome", jwt.getClaimAsString("family_name")))
                .email(strVal(user, "email", jwt.getClaimAsString("email")))
                .codiceIstat((String) user.get("codiceIstat"))
                .ruolo((String) user.get("ruolo"))
                .build();

        return ResponseEntity.ok(AdminSessionResponse.success(userDto));
    }

    private String strVal(Map<String, Object> m, String key, String fallback) {
        Object v = m.get(key);
        return v != null ? v.toString() : fallback;
    }
}
