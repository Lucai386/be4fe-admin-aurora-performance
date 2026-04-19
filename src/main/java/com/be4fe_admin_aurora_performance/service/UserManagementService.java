package com.be4fe_admin_aurora_performance.service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.be4fe_admin_aurora_performance.client.CoreApiClient;
import com.be4fe_admin_aurora_performance.dto.admin.CreateUserRequest;
import com.be4fe_admin_aurora_performance.dto.admin.CreateUserResponse;
import com.be4fe_admin_aurora_performance.dto.admin.DeleteUserResponse;
import com.be4fe_admin_aurora_performance.dto.admin.ListUsersResponse;
import com.be4fe_admin_aurora_performance.dto.admin.UpdateUserRequest;
import com.be4fe_admin_aurora_performance.dto.admin.UserDto;
import static com.be4fe_admin_aurora_performance.enums.AppConstants.MSG_CANNOT_DELETE_ADMIN;
import static com.be4fe_admin_aurora_performance.enums.AppConstants.MSG_CANNOT_DELETE_OTHER_ENTE;
import static com.be4fe_admin_aurora_performance.enums.AppConstants.MSG_CF_EXISTS;
import static com.be4fe_admin_aurora_performance.enums.AppConstants.MSG_KEYCLOAK_ERROR;
import static com.be4fe_admin_aurora_performance.enums.AppConstants.MSG_NO_ENTE;
import static com.be4fe_admin_aurora_performance.enums.AppConstants.MSG_USER_NOT_AUTHORIZED;
import static com.be4fe_admin_aurora_performance.enums.AppConstants.MSG_USER_NOT_FOUND;
import com.be4fe_admin_aurora_performance.enums.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserManagementService {

    private final CoreApiClient coreApiClient;
    private final KeycloakAdminService keycloakAdminService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ─── List users ───────────────────────────────────────────────────────────

    public ListUsersResponse listUsers(Authentication authentication) {
        Map<String, Object> admin = getAdminUser(authentication);
        if (admin == null) {
            return ListUsersResponse.error(ErrorCode.NOT_AUTHORIZED, MSG_USER_NOT_AUTHORIZED);
        }

        String codiceIstat = getString(admin, "codiceIstat");
        if (codiceIstat == null || codiceIstat.isBlank()) {
            return ListUsersResponse.error(ErrorCode.NO_ENTE, MSG_NO_ENTE);
        }

        Long adminId = getLong(admin, "id");
        List<Map> users = coreApiClient.getUsersByCodiceIstat(codiceIstat.trim());

        List<UserDto> userDtos = users.stream()
                .filter(u -> !getLong(u, "id").equals(adminId))
                .map(this::toDto)
                .toList();

        log.info("Listed {} users for codiceIstat: {}", userDtos.size(), codiceIstat);
        return ListUsersResponse.success(userDtos);
    }

    // ─── Create user ──────────────────────────────────────────────────────────

    public CreateUserResponse createUser(Authentication authentication, CreateUserRequest request) {
        Map<String, Object> admin = getAdminUser(authentication);
        if (admin == null) {
            return CreateUserResponse.error(ErrorCode.NOT_AUTHORIZED, MSG_USER_NOT_AUTHORIZED);
        }

        String codiceIstat = getString(admin, "codiceIstat");
        if (codiceIstat == null || codiceIstat.isBlank()) {
            return CreateUserResponse.error(ErrorCode.NO_ENTE, MSG_NO_ENTE);
        }

        // Verify CF uniqueness within the ente
        String cfUpper = request.getCodiceFiscale().toUpperCase();
        List<Map> existing = coreApiClient.getUsersByCodiceIstat(codiceIstat.trim());
        boolean cfExists = existing.stream()
                .anyMatch(u -> cfUpper.equals(getString(u, "codiceFiscale")));
        if (cfExists) {
            return CreateUserResponse.error(ErrorCode.CF_EXISTS, MSG_CF_EXISTS);
        }

        // Create in Keycloak
        String keycloakId = keycloakAdminService.createUser(
                request.getEmail(),
                request.getNome(),
                request.getCognome(),
                request.getPassword()
        );
        if (keycloakId == null) {
            return CreateUserResponse.error(ErrorCode.KEYCLOAK_ERROR, MSG_KEYCLOAK_ERROR);
        }

        // Create in Core
        Map<String, Object> payload = Map.of(
                "keycloakId", keycloakId,
                "nome", request.getNome(),
                "cognome", request.getCognome(),
                "email", request.getEmail(),
                "codiceFiscale", cfUpper,
                "codiceIstat", codiceIstat.trim(),
                "ruolo", request.getRuolo()
        );
        Optional<Map> savedOpt = coreApiClient.saveUser(payload);
        if (savedOpt.isEmpty()) {
            log.error("Failed to save user in Core after Keycloak creation for {}", request.getEmail());
            return CreateUserResponse.error(ErrorCode.KEYCLOAK_ERROR, MSG_KEYCLOAK_ERROR);
        }

        log.info("Created user {} ({}) for ente {}", savedOpt.get().get("id"), request.getEmail(), codiceIstat);
        return CreateUserResponse.success(toDto(savedOpt.get()));
    }

    // ─── Update user ──────────────────────────────────────────────────────────

    public CreateUserResponse updateUser(Authentication authentication, Integer userId, UpdateUserRequest request) {
        Map<String, Object> admin = getAdminUser(authentication);
        if (admin == null) {
            return CreateUserResponse.error(ErrorCode.NOT_AUTHORIZED, MSG_USER_NOT_AUTHORIZED);
        }

        String codiceIstat = getString(admin, "codiceIstat");
        if (codiceIstat == null || codiceIstat.isBlank()) {
            return CreateUserResponse.error(ErrorCode.NO_ENTE, MSG_NO_ENTE);
        }

        Optional<Map> userOpt = coreApiClient.getUserById(userId.longValue());
        if (userOpt.isEmpty()) {
            return CreateUserResponse.error(ErrorCode.USER_NOT_FOUND, MSG_USER_NOT_FOUND);
        }

        Map<String, Object> existingUser = userOpt.get();
        if (!codiceIstat.trim().equals(getString(existingUser, "codiceIstat"))) {
            return CreateUserResponse.error(ErrorCode.NOT_AUTHORIZED, MSG_CANNOT_DELETE_OTHER_ENTE);
        }

        Map<String, Object> payload = new java.util.HashMap<>();
        if (request.getNome() != null) payload.put("nome", request.getNome());
        if (request.getCognome() != null) payload.put("cognome", request.getCognome());
        if (request.getEmail() != null) payload.put("email", request.getEmail());
        if (request.getRuolo() != null) payload.put("ruolo", request.getRuolo());

        Optional<Map> updatedOpt = coreApiClient.updateUser(userId.longValue(), payload);
        if (updatedOpt.isEmpty()) {
            return CreateUserResponse.error(ErrorCode.INTERNAL_ERROR, "Errore aggiornamento utente");
        }

        log.info("Updated user {} for ente {}", userId, codiceIstat);
        return CreateUserResponse.success(toDto(updatedOpt.get()));
    }

    // ─── Delete user ──────────────────────────────────────────────────────────

    public DeleteUserResponse deleteUser(Authentication authentication, Integer userId) {
        Map<String, Object> admin = getAdminUser(authentication);
        if (admin == null) {
            return DeleteUserResponse.error(ErrorCode.NOT_AUTHORIZED, MSG_USER_NOT_AUTHORIZED);
        }

        String codiceIstat = getString(admin, "codiceIstat");
        if (codiceIstat == null || codiceIstat.isBlank()) {
            return DeleteUserResponse.error(ErrorCode.NO_ENTE, MSG_NO_ENTE);
        }

        Optional<Map> userOpt = coreApiClient.getUserById(userId.longValue());
        if (userOpt.isEmpty()) {
            return DeleteUserResponse.error(ErrorCode.USER_NOT_FOUND, MSG_USER_NOT_FOUND);
        }

        Map<String, Object> userToDelete = userOpt.get();

        if (!codiceIstat.trim().equals(getString(userToDelete, "codiceIstat"))) {
            return DeleteUserResponse.error(ErrorCode.NOT_AUTHORIZED, MSG_CANNOT_DELETE_OTHER_ENTE);
        }

        if ("AD".equals(getString(userToDelete, "ruolo"))) {
            return DeleteUserResponse.error(ErrorCode.CANNOT_DELETE_ADMIN, MSG_CANNOT_DELETE_ADMIN);
        }

        String keycloakId = getString(userToDelete, "keycloakId");
        if (keycloakId != null) {
            boolean keycloakDeleted = keycloakAdminService.deleteUser(keycloakId);
            if (!keycloakDeleted) {
                log.warn("Could not delete user {} from Keycloak, proceeding with Core deletion", keycloakId);
            }
        }

        coreApiClient.deleteUser(userId.longValue());
        log.info("Deleted user {} from ente {}", userId, codiceIstat);

        return DeleteUserResponse.success();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Map<String, Object> getAdminUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String keycloakId = jwt.getSubject();

        Optional<Map> userOpt = coreApiClient.getUserByKeycloakId(keycloakId);
        if (userOpt.isEmpty()) return null;

        Map<String, Object> user = userOpt.get();
        String ruolo = getString(user, "ruolo");
        if (!"AD".equals(ruolo) && !"SC".equals(ruolo)) return null;

        String ci = getString(user, "codiceIstat");
        if (ci != null && !ci.isBlank()) CoreApiClient.setTenant(ci.trim());

        return user;
    }

    private UserDto toDto(Map<String, Object> user) {
        return UserDto.builder()
                .id(getInt(user, "id"))
                .nome(getString(user, "nome"))
                .cognome(getString(user, "cognome"))
                .email(getString(user, "email"))
                .codiceFiscale(getString(user, "codiceFiscale"))
                .codiceIstat(getString(user, "codiceIstat"))
                .ruolo(getString(user, "ruolo"))
                .createdAt(getString(user, "createdAt"))
                .build();
    }

    private static String getString(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString() : null;
    }

    private static Integer getInt(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return null;
        if (v instanceof Integer i) return i;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(v.toString()); } catch (NumberFormatException e) { return null; }
    }

    private static Long getLong(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return null;
        if (v instanceof Long l) return l;
        if (v instanceof Integer i) return i.longValue();
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(v.toString()); } catch (NumberFormatException e) { return null; }
    }
}
