package com.be4fe_admin_aurora_performance.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.be4fe_admin_aurora_performance.client.CoreApiClient;
import com.be4fe_admin_aurora_performance.dto.admin.SwitchEnteRequest;
import com.be4fe_admin_aurora_performance.dto.admin.SwitchEnteResponse;
import com.be4fe_admin_aurora_performance.dto.session.SessionInfoDto;
import com.be4fe_admin_aurora_performance.dto.session.SessionUserDto;
import com.be4fe_admin_aurora_performance.enums.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final CoreApiClient coreApiClient;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME
            .withZone(ZoneId.systemDefault());

    public SwitchEnteResponse switchEnte(Authentication authentication, SwitchEnteRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Switch ente request without valid authentication");
            return SwitchEnteResponse.error(ErrorCode.SESSION_INVALID);
        }

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String keycloakId = jwt.getSubject();

        Optional<Map> userOpt = coreApiClient.getUserByKeycloakId(keycloakId);
        if (userOpt.isEmpty()) {
            log.warn("User not found in Core for keycloakId: {}", keycloakId);
            return SwitchEnteResponse.error(ErrorCode.USER_NOT_FOUND);
        }

        Map<String, Object> user = userOpt.get();

        if (!"AD".equals(user.get("ruolo"))) {
            log.warn("Non-admin user {} attempted to switch ente", user.get("id"));
            return SwitchEnteResponse.error(ErrorCode.NOT_AUTHORIZED);
        }

        String codiceIstat = request.getCodiceIstat();
        if (codiceIstat == null || codiceIstat.isBlank()) {
            return SwitchEnteResponse.error(ErrorCode.CODICE_ISTAT_REQUIRED);
        }

        Long userId = getLong(user, "id");
        Optional<Map> updatedOpt = coreApiClient.patchCodiceIstat(userId, codiceIstat.trim());

        Map<String, Object> updatedUser = updatedOpt.orElse(user);
        // Ensure codiceIstat is reflected in the response
        updatedUser.put("codiceIstat", codiceIstat.trim());

        log.info("Admin user {} switched to codiceIstat: {}", userId, codiceIstat);

        SessionUserDto userDto = buildUserDto(updatedUser, jwt);
        SessionInfoDto sessionDto = buildSessionDto(jwt);

        return SwitchEnteResponse.success(userDto, sessionDto);
    }

    private SessionUserDto buildUserDto(Map<String, Object> user, Jwt jwt) {
        return SessionUserDto.builder()
                .id(user.get("id") != null ? user.get("id").toString() : null)
                .nome(getString(user, "nome") != null ? getString(user, "nome") : jwt.getClaimAsString("given_name"))
                .cognome(getString(user, "cognome") != null ? getString(user, "cognome") : jwt.getClaimAsString("family_name"))
                .codiceFiscale(getString(user, "codiceFiscale"))
                .codiceIstat(getString(user, "codiceIstat"))
                .ruolo(getString(user, "ruolo"))
                .assegnazioni(null)
                .build();
    }

    private SessionInfoDto buildSessionDto(Jwt jwt) {
        Instant issuedAt = jwt.getIssuedAt();
        Instant expiresAt = jwt.getExpiresAt();

        return SessionInfoDto.builder()
                .issuedAt(issuedAt != null ? ISO_FORMATTER.format(issuedAt) : null)
                .expiresAt(expiresAt != null ? ISO_FORMATTER.format(expiresAt) : null)
                .build();
    }

    private static Long getLong(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return null;
        if (v instanceof Long l) return l;
        if (v instanceof Integer i) return i.longValue();
        if (v instanceof Number n) return n.longValue();
        return Long.parseLong(v.toString());
    }

    private static String getString(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString() : null;
    }
}
