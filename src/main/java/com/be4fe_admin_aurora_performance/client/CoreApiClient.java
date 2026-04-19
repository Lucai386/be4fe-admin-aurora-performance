package com.be4fe_admin_aurora_performance.client;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Client HTTP per comunicare con core-aurora-performance.
 *
 * <p>Ottiene access_token tramite client_credentials da Keycloak
 * e lo include come Bearer token in ogni richiesta verso il Core.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CoreApiClient {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    public static void setTenant(String codiceIstat) { CURRENT_TENANT.set(codiceIstat); }
    public static void clearTenant() { CURRENT_TENANT.remove(); }

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${services.core-url}")
    private String coreBaseUrl;

    @Value("${keycloak.auth-server-url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${spring.security.oauth2.client.registration.core-client.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.core-client.client-secret}")
    private String clientSecret;

    // ─── Token cache ──────────────────────────────────────────────────────────

    private volatile String cachedToken;
    private volatile long tokenExpiresAt = 0;

    private synchronized String getServiceToken() {
        long now = System.currentTimeMillis() / 1000;
        if (cachedToken != null && now < tokenExpiresAt - 30) {
            return cachedToken;
        }
        String tokenUrl = keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String body = "grant_type=client_credentials&client_id=" + clientId + "&client_secret=" + clientSecret;
        ResponseEntity<Map> resp = restTemplate.postForEntity(tokenUrl, new HttpEntity<>(body, headers), Map.class);
        if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
            cachedToken = (String) resp.getBody().get("access_token");
            Integer expiresIn = (Integer) resp.getBody().getOrDefault("expires_in", 300);
            tokenExpiresAt = now + expiresIn;
        }
        return cachedToken;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private HttpHeaders authHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(getServiceToken());
        h.setContentType(MediaType.APPLICATION_JSON);
        String tenant = CURRENT_TENANT.get();
        if (tenant != null && !tenant.isBlank()) {
            h.set("X-Tenant-Id", tenant);
        }
        return h;
    }

    private <T> Optional<T> getOne(String path, Class<T> type) {
        try {
            ResponseEntity<T> resp = restTemplate.exchange(
                    coreBaseUrl + path, HttpMethod.GET, new HttpEntity<>(authHeaders()), type);
            return Optional.ofNullable(resp.getBody());
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    private <T> List<T> getList(String path, ParameterizedTypeReference<List<T>> typeRef) {
        try {
            ResponseEntity<List<T>> resp = restTemplate.exchange(
                    coreBaseUrl + path, HttpMethod.GET, new HttpEntity<>(authHeaders()), typeRef);
            return resp.getBody() != null ? resp.getBody() : Collections.emptyList();
        } catch (Exception e) {
            log.error("GET {} failed: {}", path, e.getMessage());
            return Collections.emptyList();
        }
    }

    private <T> Optional<T> post(String path, Object body, Class<T> type) {
        try {
            ResponseEntity<T> resp = restTemplate.exchange(
                    coreBaseUrl + path, HttpMethod.POST,
                    new HttpEntity<>(body, authHeaders()), type);
            return Optional.ofNullable(resp.getBody());
        } catch (Exception e) {
            log.error("POST {} failed: {}", path, e.getMessage());
            return Optional.empty();
        }
    }

    private <T> Optional<T> put(String path, Object body, Class<T> type) {
        try {
            ResponseEntity<T> resp = restTemplate.exchange(
                    coreBaseUrl + path, HttpMethod.PUT,
                    new HttpEntity<>(body, authHeaders()), type);
            return Optional.ofNullable(resp.getBody());
        } catch (Exception e) {
            log.error("PUT {} failed: {}", path, e.getMessage());
            return Optional.empty();
        }
    }

    private <T> Optional<T> patch(String path, Object body, Class<T> type) {
        try {
            ResponseEntity<T> resp = restTemplate.exchange(
                    coreBaseUrl + path, HttpMethod.PATCH,
                    new HttpEntity<>(body, authHeaders()), type);
            return Optional.ofNullable(resp.getBody());
        } catch (Exception e) {
            log.error("PATCH {} failed: {}", path, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * @return true se eliminato con successo (204), false se ha figli (409) o altro errore
     */
    private boolean deleteReturningStatus(String path) {
        try {
            ResponseEntity<Void> resp = restTemplate.exchange(
                    coreBaseUrl + path, HttpMethod.DELETE,
                    new HttpEntity<>(authHeaders()), Void.class);
            return resp.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException e) {
            log.warn("DELETE {} returned status {}", path, e.getStatusCode().value());
            return false;
        } catch (Exception e) {
            log.warn("DELETE {} failed: {}", path, e.getMessage());
            return false;
        }
    }

    private void delete(String path) {
        deleteReturningStatus(path);
    }

    // ─── Users ────────────────────────────────────────────────────────────────

    public Optional<Map> getUserByKeycloakId(String keycloakId) {
        return getOne("/internal/users/keycloak/" + keycloakId, Map.class);
    }

    public Optional<Map> getUserById(Long id) {
        return getOne("/internal/users/" + id, Map.class);
    }

    public List<Map> getUsersByCodiceIstat(String codiceIstat) {
        return getList("/internal/users?codiceIstat=" + codiceIstat,
                new ParameterizedTypeReference<>() {});
    }

    public Optional<Map> saveUser(Map<String, Object> user) {
        return post("/internal/users", user, Map.class);
    }

    public Optional<Map> updateUser(Long id, Map<String, Object> user) {
        return put("/internal/users/" + id, user, Map.class);
    }

    public Optional<Map> patchCodiceIstat(Long id, String codiceIstat) {
        return patch("/internal/users/" + id + "/codice-istat",
                Map.of("codiceIstat", codiceIstat), Map.class);
    }

    public void deleteUser(Long id) {
        delete("/internal/users/" + id);
    }

    // ─── Strutture ────────────────────────────────────────────────────────────

    public List<Map> getStrutture(String codiceIstat) {
        return getList("/internal/strutture?codiceIstat=" + codiceIstat,
                new ParameterizedTypeReference<>() {});
    }

    public Optional<Map> getStrutturaById(Integer id) {
        return getOne("/internal/strutture/" + id, Map.class);
    }

    public Optional<Map> createStruttura(Map<String, Object> payload) {
        return post("/internal/strutture", payload, Map.class);
    }

    public Optional<Map> updateStruttura(Integer id, Map<String, Object> payload) {
        return put("/internal/strutture/" + id, payload, Map.class);
    }

    /**
     * @return true se eliminata, false se ha figli (409) o altro errore
     */
    public boolean deleteStruttura(Integer id) {
        return deleteReturningStatus("/internal/strutture/" + id);
    }

    public Optional<Map> addStaffToStruttura(Integer strutturaId, Map<String, Object> payload) {
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(
                    coreBaseUrl + "/internal/strutture/" + strutturaId + "/staff",
                    HttpMethod.POST, new HttpEntity<>(payload, authHeaders()), Map.class);
            return Optional.ofNullable(resp.getBody());
        } catch (HttpClientErrorException e) {
            log.warn("POST staff to struttura {} returned status {}", strutturaId, e.getStatusCode().value());
            return Optional.empty();
        } catch (Exception e) {
            log.error("POST staff to struttura {} failed: {}", strutturaId, e.getMessage());
            return Optional.empty();
        }
    }

    public void removeStaffFromStruttura(Integer strutturaId, Long userId) {
        delete("/internal/strutture/" + strutturaId + "/staff/" + userId);
    }

    public List<Map> getUtentiStruttura(Integer strutturaId) {
        return getList("/internal/strutture/" + strutturaId + "/utenti",
                new ParameterizedTypeReference<>() {});
    }

    // ─── DUP ──────────────────────────────────────────────────────────────────

    public List<Map> getDup(String codiceIstat) {
        return getList("/internal/dup?codiceIstat=" + codiceIstat,
                new ParameterizedTypeReference<>() {});
    }

    public Optional<Map> getDupById(Long id) {
        return getOne("/internal/dup/" + id, Map.class);
    }

    public Optional<Map> createDup(Map<String, Object> payload) {
        return post("/internal/dup", payload, Map.class);
    }

    public Optional<Map> updateDup(Long id, Map<String, Object> payload) {
        return put("/internal/dup/" + id, payload, Map.class);
    }

    public boolean deleteDup(Long id) {
        return deleteReturningStatus("/internal/dup/" + id);
    }

    // ─── LPM ──────────────────────────────────────────────────────────────────

    public List<Map> getLpm(String codiceIstat) {
        return getList("/internal/lpm?codiceIstat=" + codiceIstat,
                new ParameterizedTypeReference<>() {});
    }

    public Optional<Map> getLpmById(Long id) {
        return getOne("/internal/lpm/" + id, Map.class);
    }

    public Optional<Map> createLpm(Map<String, Object> payload) {
        return post("/internal/lpm", payload, Map.class);
    }

    public Optional<Map> updateLpm(Long id, Map<String, Object> payload) {
        return put("/internal/lpm/" + id, payload, Map.class);
    }

    public boolean deleteLpm(Long id) {
        return deleteReturningStatus("/internal/lpm/" + id);
    }

    // ─── Obiettivi ────────────────────────────────────────────────────────────

    public List<Map> getObiettivi(String codiceIstat, Long utenteId) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(coreBaseUrl + "/internal/obiettivi");
        if (codiceIstat != null) builder.queryParam("codiceIstat", codiceIstat);
        if (utenteId != null) builder.queryParam("utenteId", utenteId);
        return getList(builder.build().toUriString().replace(coreBaseUrl, ""),
                new ParameterizedTypeReference<>() {});
    }

    // ─── Attività ─────────────────────────────────────────────────────────────

    public List<Map> getAttivita(Long progettoId, Long utenteId, Long strutturaId) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(coreBaseUrl + "/internal/attivita");
        if (progettoId != null) builder.queryParam("progettoId", progettoId);
        if (utenteId != null) builder.queryParam("utenteId", utenteId);
        if (strutturaId != null) builder.queryParam("strutturaId", strutturaId);
        return getList(builder.build().toUriString().replace(coreBaseUrl, ""),
                new ParameterizedTypeReference<>() {});
    }

    // ─── Enti ─────────────────────────────────────────────────────────────────

    public List<Map> getEnti() {
        return getList("/internal/enti", new ParameterizedTypeReference<>() {});
    }

    public Optional<Map> createEnte(String codiceIstat, String nome) {
        return post("/internal/enti", Map.of("codiceIstat", codiceIstat, "nome", nome), Map.class);
    }
}
