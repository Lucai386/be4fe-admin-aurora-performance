package com.be4fe_admin_aurora_performance.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.be4fe_admin_aurora_performance.client.CoreApiClient;
import com.be4fe_admin_aurora_performance.dto.dup.CreateDupRequest;
import com.be4fe_admin_aurora_performance.dto.dup.DupDto;
import com.be4fe_admin_aurora_performance.dto.dup.DupResponse;
import com.be4fe_admin_aurora_performance.dto.dup.ListDupResponse;
import com.be4fe_admin_aurora_performance.dto.lpm.CreateLpmRequest;
import com.be4fe_admin_aurora_performance.dto.lpm.ListLpmResponse;
import com.be4fe_admin_aurora_performance.dto.lpm.LpmDto;
import com.be4fe_admin_aurora_performance.dto.lpm.LpmResponse;
import com.be4fe_admin_aurora_performance.enums.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LpmDupService {

    private final CoreApiClient coreApiClient;

    private static final List<String> AUTHORIZED_ROLES = List.of("AD", "SC");

    // ─── LPM ──────────────────────────────────────────────────────────────────

    public ListLpmResponse listLpm(Authentication authentication) {
        Map<String, Object> admin = getAuthorizedUser(authentication);
        if (admin == null) return ListLpmResponse.error(ErrorCode.NOT_AUTHORIZED);

        String codiceIstat = getString(admin, "codiceIstat");
        if (codiceIstat == null || codiceIstat.isBlank()) return ListLpmResponse.error(ErrorCode.NO_ENTE);

        List<Map> items = coreApiClient.getLpm(codiceIstat.trim());
        List<LpmDto> dtos = items.stream().map(this::toLpmDto).toList();
        return ListLpmResponse.success(dtos);
    }

    public LpmResponse createLpm(Authentication authentication, CreateLpmRequest request) {
        Map<String, Object> admin = getAuthorizedUser(authentication);
        if (admin == null) return LpmResponse.error(ErrorCode.NOT_AUTHORIZED);

        String codiceIstat = getString(admin, "codiceIstat");
        if (codiceIstat == null || codiceIstat.isBlank()) return LpmResponse.error(ErrorCode.NO_ENTE);
        if (request.getTitolo() == null || request.getTitolo().isBlank()) return LpmResponse.error(ErrorCode.VALIDATION_ERROR);

        Map<String, Object> payload = new HashMap<>();
        payload.put("codiceIstat", codiceIstat.trim());
        payload.put("titolo", request.getTitolo());
        if (request.getDescrizione() != null) payload.put("descrizione", request.getDescrizione());
        payload.put("annoInizioMandato", request.getAnnoInizioMandato() != null ? request.getAnnoInizioMandato() : 2025);
        payload.put("annoFineMandato", request.getAnnoFineMandato() != null ? request.getAnnoFineMandato() : 2030);
        if (request.getPriorita() != null) payload.put("priorita", request.getPriorita());
        payload.put("createdBy", getInt(admin, "id"));

        Optional<Map> created = coreApiClient.createLpm(payload);
        if (created.isEmpty()) return LpmResponse.error(ErrorCode.INTERNAL_ERROR);

        log.info("LPM creata: {}", created.get().get("id"));
        return LpmResponse.success(toLpmDto(created.get()));
    }

    public LpmResponse deleteLpm(Authentication authentication, Long id) {
        Map<String, Object> admin = getAuthorizedUser(authentication);
        if (admin == null) return LpmResponse.error(ErrorCode.NOT_AUTHORIZED);

        boolean deleted = coreApiClient.deleteLpm(id);
        if (!deleted) return LpmResponse.error(ErrorCode.LPM_NOT_FOUND);

        log.info("LPM eliminata: {}", id);
        return LpmResponse.builder().result("OK").build();
    }

    // ─── DUP ──────────────────────────────────────────────────────────────────

    public ListDupResponse listDup(Authentication authentication) {
        Map<String, Object> admin = getAuthorizedUser(authentication);
        if (admin == null) return ListDupResponse.error(ErrorCode.NOT_AUTHORIZED);

        String codiceIstat = getString(admin, "codiceIstat");
        if (codiceIstat == null || codiceIstat.isBlank()) return ListDupResponse.error(ErrorCode.NO_ENTE);

        List<Map> items = coreApiClient.getDup(codiceIstat.trim());
        List<DupDto> dtos = items.stream().map(this::toDupDto).toList();
        return ListDupResponse.success(dtos);
    }

    public DupResponse createDup(Authentication authentication, CreateDupRequest request) {
        Map<String, Object> admin = getAuthorizedUser(authentication);
        if (admin == null) return DupResponse.error(ErrorCode.NOT_AUTHORIZED);

        String codiceIstat = getString(admin, "codiceIstat");
        if (codiceIstat == null || codiceIstat.isBlank()) return DupResponse.error(ErrorCode.NO_ENTE);
        if (request.getAnno() == null) return DupResponse.error(ErrorCode.DUP_ANNO_REQUIRED);
        if (request.getTitolo() == null || request.getTitolo().isBlank()) return DupResponse.error(ErrorCode.DUP_TITOLO_REQUIRED);

        Map<String, Object> payload = new HashMap<>();
        payload.put("codiceIstat", codiceIstat.trim());
        payload.put("anno", request.getAnno());
        payload.put("titolo", request.getTitolo());
        if (request.getDescrizione() != null) payload.put("descrizione", request.getDescrizione());
        payload.put("sezione", request.getSezione() != null ? request.getSezione() : "STRATEGICA");
        payload.put("createdBy", getInt(admin, "id"));

        Optional<Map> created = coreApiClient.createDup(payload);
        if (created.isEmpty()) return DupResponse.error(ErrorCode.INTERNAL_ERROR);

        log.info("DUP creato: {}", created.get().get("id"));
        return DupResponse.success(toDupDto(created.get()));
    }

    public DupResponse deleteDup(Authentication authentication, Long id) {
        Map<String, Object> admin = getAuthorizedUser(authentication);
        if (admin == null) return DupResponse.error(ErrorCode.NOT_AUTHORIZED);

        boolean deleted = coreApiClient.deleteDup(id);
        if (!deleted) return DupResponse.error(ErrorCode.DUP_HAS_PROGETTI);

        log.info("DUP eliminato: {}", id);
        return DupResponse.builder().result("OK").build();
    }

    // ─── Mapping helpers ──────────────────────────────────────────────────────

    private LpmDto toLpmDto(Map<String, Object> m) {
        return LpmDto.builder()
                .id(getLong(m, "id"))
                .codiceIstat(getString(m, "codiceIstat"))
                .annoInizioMandato(getInt(m, "annoInizioMandato"))
                .annoFineMandato(getInt(m, "annoFineMandato"))
                .titolo(getString(m, "titolo"))
                .descrizione(getString(m, "descrizione"))
                .stato(getString(m, "stato"))
                .priorita(getInt(m, "priorita"))
                .progresso(getInt(m, "progresso"))
                .responsabileId(getInt(m, "responsabileId"))
                .dupId(getLong(m, "dupId"))
                .dupTitolo(getString(m, "dupTitolo"))
                .createdAt(getString(m, "createdAt"))
                .build();
    }

    private DupDto toDupDto(Map<String, Object> m) {
        return DupDto.builder()
                .id(getLong(m, "id"))
                .codiceIstat(getString(m, "codiceIstat"))
                .codice(getString(m, "codice"))
                .anno(getInt(m, "anno"))
                .titolo(getString(m, "titolo"))
                .descrizione(getString(m, "descrizione"))
                .sezione(getString(m, "sezione"))
                .stato(getString(m, "stato"))
                .createdAt(getString(m, "createdAt"))
                .build();
    }

    // ─── Auth helper ──────────────────────────────────────────────────────────

    private Map<String, Object> getAuthorizedUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) return null;
        Jwt jwt = (Jwt) authentication.getPrincipal();
        Optional<Map> userOpt = coreApiClient.getUserByKeycloakId(jwt.getSubject());
        if (userOpt.isEmpty()) return null;
        Map<String, Object> user = userOpt.get();
        if (!AUTHORIZED_ROLES.contains(getString(user, "ruolo"))) return null;
        String ci = getString(user, "codiceIstat");
        if (ci != null && !ci.isBlank()) CoreApiClient.setTenant(ci.trim());
        return user;
    }

    private static Long getLong(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(v.toString()); } catch (NumberFormatException e) { return null; }
    }

    private static Integer getInt(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(v.toString()); } catch (NumberFormatException e) { return null; }
    }

    private static String getString(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString() : null;
    }
}
