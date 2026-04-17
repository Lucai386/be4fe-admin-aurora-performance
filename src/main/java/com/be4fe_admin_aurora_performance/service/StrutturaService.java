package com.be4fe_admin_aurora_performance.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.be4fe_admin_aurora_performance.client.CoreApiClient;
import com.be4fe_admin_aurora_performance.dto.org.AddStaffRequest;
import com.be4fe_admin_aurora_performance.dto.org.CreateStrutturaRequest;
import com.be4fe_admin_aurora_performance.dto.org.DeleteStrutturaResponse;
import com.be4fe_admin_aurora_performance.dto.org.ListStruttureResponse;
import com.be4fe_admin_aurora_performance.dto.org.StrutturaDto;
import com.be4fe_admin_aurora_performance.dto.org.StrutturaResponse;
import com.be4fe_admin_aurora_performance.dto.org.StrutturaUtentiResponse;
import com.be4fe_admin_aurora_performance.dto.org.UpdateStrutturaRequest;
import com.be4fe_admin_aurora_performance.enums.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class StrutturaService {

    private final CoreApiClient coreApiClient;

    private static final List<String> AUTHORIZED_ROLES = List.of("AD", "SC", "RA", "DR");

    // ─── List ─────────────────────────────────────────────────────────────────

    public ListStruttureResponse listStrutture(Authentication authentication) {
        Map<String, Object> admin = getAuthorizedUser(authentication);
        if (admin == null) {
            return ListStruttureResponse.error(ErrorCode.NOT_AUTHORIZED);
        }

        String codiceIstat = getString(admin, "codiceIstat");
        if (codiceIstat == null || codiceIstat.isBlank()) {
            return ListStruttureResponse.error(ErrorCode.NO_ENTE);
        }

        List<Map> strutture = coreApiClient.getStrutture(codiceIstat.trim());
        Map<Integer, Map<String, Object>> byId = strutture.stream()
                .filter(s -> s.get("id") != null)
                .collect(Collectors.toMap(s -> getInt(s, "id"), s -> s));

        List<StrutturaDto> dtos = strutture.stream()
                .map(s -> toDto(s, byId))
                .toList();

        log.info("Listed {} strutture for codiceIstat: {}", dtos.size(), codiceIstat);
        return ListStruttureResponse.success(dtos);
    }

    // ─── Create ───────────────────────────────────────────────────────────────

    public StrutturaResponse createStruttura(Authentication authentication, CreateStrutturaRequest request) {
        Map<String, Object> admin = getAuthorizedUser(authentication);
        if (admin == null) {
            return StrutturaResponse.error(ErrorCode.NOT_AUTHORIZED);
        }

        String codiceIstat = getString(admin, "codiceIstat");
        if (codiceIstat == null || codiceIstat.isBlank()) {
            return StrutturaResponse.error(ErrorCode.NO_ENTE);
        }

        // Validate parent belongs to same ente
        if (request.getIdParent() != null) {
            Optional<Map> parent = coreApiClient.getStrutturaById(request.getIdParent());
            if (parent.isEmpty()) {
                return StrutturaResponse.error(ErrorCode.PARENT_NOT_FOUND);
            }
            String parentIstat = getString(parent.get(), "codiceIstatComune");
            if (!codiceIstat.trim().equals(parentIstat)) {
                return StrutturaResponse.error(ErrorCode.PARENT_DIFFERENT_ENTE);
            }
        }

        // Validate responsabile belongs to same ente
        if (request.getIdResponsabile() != null) {
            Optional<Map> responsabile = coreApiClient.getUserById(request.getIdResponsabile().longValue());
            if (responsabile.isEmpty()) {
                return StrutturaResponse.error(ErrorCode.RESPONSABILE_NOT_FOUND);
            }
            String respIstat = getString(responsabile.get(), "codiceIstat");
            if (!codiceIstat.trim().equals(respIstat)) {
                return StrutturaResponse.error(ErrorCode.RESPONSABILE_DIFFERENT_ENTE);
            }
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("nome", request.getNome());
        payload.put("codiceIstatComune", codiceIstat.trim());
        payload.put("tipo", request.getTipo());
        payload.put("idParent", request.getIdParent());
        payload.put("idResponsabile", request.getIdResponsabile());
        payload.put("ruoloLabel", request.getRuoloLabel());
        payload.put("colore", request.getColore());
        payload.put("ordine", request.getOrdine() != null ? request.getOrdine() : 0);

        Optional<Map> saved = coreApiClient.createStruttura(payload);
        if (saved.isEmpty()) {
            return StrutturaResponse.error(ErrorCode.STRUTTURA_NOT_FOUND);
        }

        log.info("Created struttura {} for ente {}", saved.get().get("id"), codiceIstat);

        // Reload the full list for parent names
        List<Map> all = coreApiClient.getStrutture(codiceIstat.trim());
        Map<Integer, Map<String, Object>> byId = all.stream()
                .filter(s -> s.get("id") != null)
                .collect(Collectors.toMap(s -> getInt(s, "id"), s -> s));

        Integer newId = getInt(saved.get(), "id");
        Map<String, Object> reloaded = byId.getOrDefault(newId, saved.get());
        return StrutturaResponse.success(toDto(reloaded, byId));
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    public StrutturaResponse updateStruttura(Authentication authentication, Integer id, UpdateStrutturaRequest request) {
        Map<String, Object> admin = getAuthorizedUser(authentication);
        if (admin == null) {
            return StrutturaResponse.error(ErrorCode.NOT_AUTHORIZED);
        }

        String codiceIstat = getString(admin, "codiceIstat");
        if (codiceIstat == null || codiceIstat.isBlank()) {
            return StrutturaResponse.error(ErrorCode.NO_ENTE);
        }

        Optional<Map> existingOpt = coreApiClient.getStrutturaById(id);
        if (existingOpt.isEmpty()) {
            return StrutturaResponse.error(ErrorCode.STRUTTURA_NOT_FOUND);
        }
        Map<String, Object> existing = existingOpt.get();

        if (!codiceIstat.trim().equals(getString(existing, "codiceIstatComune"))) {
            return StrutturaResponse.error(ErrorCode.NOT_AUTHORIZED);
        }

        // Build full update payload from existing values, overriding changed fields
        Map<String, Object> payload = new HashMap<>(existing);
        // Remove JPA-managed relations that Core will re-resolve
        payload.remove("responsabile");
        payload.remove("staff");

        if (request.getNome() != null) payload.put("nome", request.getNome());
        if (request.getTipo() != null) payload.put("tipo", request.getTipo());
        if (request.getRuoloLabel() != null) payload.put("ruoloLabel", request.getRuoloLabel());
        if (request.getColore() != null) payload.put("colore", request.getColore());
        if (request.getOrdine() != null) payload.put("ordine", request.getOrdine());

        if (Boolean.TRUE.equals(request.getRemoveParent())) {
            payload.put("idParent", null);
        } else if (request.getIdParent() != null) {
            if (request.getIdParent().equals(id)) {
                return StrutturaResponse.error(ErrorCode.INVALID_PARENT);
            }
            Optional<Map> parent = coreApiClient.getStrutturaById(request.getIdParent());
            if (parent.isEmpty()) {
                return StrutturaResponse.error(ErrorCode.PARENT_NOT_FOUND);
            }
            if (!codiceIstat.trim().equals(getString(parent.get(), "codiceIstatComune"))) {
                return StrutturaResponse.error(ErrorCode.PARENT_DIFFERENT_ENTE);
            }
            payload.put("idParent", request.getIdParent());
        }

        if (Boolean.TRUE.equals(request.getRemoveResponsabile())) {
            payload.put("idResponsabile", null);
        } else if (request.getIdResponsabile() != null) {
            Optional<Map> resp = coreApiClient.getUserById(request.getIdResponsabile().longValue());
            if (resp.isEmpty()) {
                return StrutturaResponse.error(ErrorCode.RESPONSABILE_NOT_FOUND);
            }
            if (!codiceIstat.trim().equals(getString(resp.get(), "codiceIstat"))) {
                return StrutturaResponse.error(ErrorCode.RESPONSABILE_DIFFERENT_ENTE);
            }
            payload.put("idResponsabile", request.getIdResponsabile());
        }

        Optional<Map> saved = coreApiClient.updateStruttura(id, payload);
        log.info("Updated struttura {} for ente {}", id, codiceIstat);

        List<Map> all = coreApiClient.getStrutture(codiceIstat.trim());
        Map<Integer, Map<String, Object>> byId = all.stream()
                .filter(s -> s.get("id") != null)
                .collect(Collectors.toMap(s -> getInt(s, "id"), s -> s));

        Map<String, Object> reloaded = byId.getOrDefault(id, saved.orElse(existing));
        return StrutturaResponse.success(toDto(reloaded, byId));
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    public DeleteStrutturaResponse deleteStruttura(Authentication authentication, Integer id) {
        Map<String, Object> admin = getAuthorizedUser(authentication);
        if (admin == null) {
            return DeleteStrutturaResponse.error(ErrorCode.NOT_AUTHORIZED);
        }

        String codiceIstat = getString(admin, "codiceIstat");
        if (codiceIstat == null || codiceIstat.isBlank()) {
            return DeleteStrutturaResponse.error(ErrorCode.NO_ENTE);
        }

        Optional<Map> existingOpt = coreApiClient.getStrutturaById(id);
        if (existingOpt.isEmpty()) {
            return DeleteStrutturaResponse.error(ErrorCode.STRUTTURA_NOT_FOUND);
        }

        if (!codiceIstat.trim().equals(getString(existingOpt.get(), "codiceIstatComune"))) {
            return DeleteStrutturaResponse.error(ErrorCode.NOT_AUTHORIZED);
        }

        boolean deleted = coreApiClient.deleteStruttura(id);
        if (!deleted) {
            return DeleteStrutturaResponse.error(ErrorCode.HAS_CHILDREN);
        }

        log.info("Deleted struttura {} from ente {}", id, codiceIstat);
        return DeleteStrutturaResponse.success();
    }

    // ─── Add staff ────────────────────────────────────────────────────────────

    public StrutturaResponse addStaff(Authentication authentication, Integer strutturaId, AddStaffRequest request) {
        Map<String, Object> admin = getAuthorizedUser(authentication);
        if (admin == null) {
            return StrutturaResponse.error(ErrorCode.NOT_AUTHORIZED);
        }

        String codiceIstat = getString(admin, "codiceIstat");
        if (codiceIstat == null || codiceIstat.isBlank()) {
            return StrutturaResponse.error(ErrorCode.NO_ENTE);
        }

        Optional<Map> strutturaOpt = coreApiClient.getStrutturaById(strutturaId);
        if (strutturaOpt.isEmpty()) {
            return StrutturaResponse.error(ErrorCode.STRUTTURA_NOT_FOUND);
        }
        if (!codiceIstat.trim().equals(getString(strutturaOpt.get(), "codiceIstatComune"))) {
            return StrutturaResponse.error(ErrorCode.NOT_AUTHORIZED);
        }

        Optional<Map> userOpt = coreApiClient.getUserById(request.getIdUser().longValue());
        if (userOpt.isEmpty()) {
            return StrutturaResponse.error(ErrorCode.USER_NOT_FOUND);
        }
        if (!codiceIstat.trim().equals(getString(userOpt.get(), "codiceIstat"))) {
            return StrutturaResponse.error(ErrorCode.RESPONSABILE_DIFFERENT_ENTE);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", request.getIdUser());
        payload.put("ruoloStruttura", request.getRuoloStruttura());
        payload.put("ordine", request.getOrdine() != null ? request.getOrdine() : 0);

        Optional<Map> result = coreApiClient.addStaffToStruttura(strutturaId, payload);
        if (result.isEmpty()) {
            // 409 or other error (likely duplicate)
            return StrutturaResponse.error(ErrorCode.STRUTTURA_NOT_FOUND);
        }

        log.info("Added user {} to staff of struttura {}", request.getIdUser(), strutturaId);

        List<Map> all = coreApiClient.getStrutture(codiceIstat.trim());
        Map<Integer, Map<String, Object>> byId = all.stream()
                .filter(s -> s.get("id") != null)
                .collect(Collectors.toMap(s -> getInt(s, "id"), s -> s));

        Map<String, Object> reloaded = byId.getOrDefault(strutturaId, strutturaOpt.get());
        return StrutturaResponse.success(toDto(reloaded, byId));
    }

    // ─── Remove staff ─────────────────────────────────────────────────────────

    public StrutturaResponse removeStaff(Authentication authentication, Integer strutturaId, Integer userId) {
        Map<String, Object> admin = getAuthorizedUser(authentication);
        if (admin == null) {
            return StrutturaResponse.error(ErrorCode.NOT_AUTHORIZED);
        }

        String codiceIstat = getString(admin, "codiceIstat");
        if (codiceIstat == null || codiceIstat.isBlank()) {
            return StrutturaResponse.error(ErrorCode.NO_ENTE);
        }

        Optional<Map> strutturaOpt = coreApiClient.getStrutturaById(strutturaId);
        if (strutturaOpt.isEmpty()) {
            return StrutturaResponse.error(ErrorCode.STRUTTURA_NOT_FOUND);
        }
        if (!codiceIstat.trim().equals(getString(strutturaOpt.get(), "codiceIstatComune"))) {
            return StrutturaResponse.error(ErrorCode.NOT_AUTHORIZED);
        }

        coreApiClient.removeStaffFromStruttura(strutturaId, userId.longValue());
        log.info("Removed user {} from staff of struttura {}", userId, strutturaId);

        List<Map> all = coreApiClient.getStrutture(codiceIstat.trim());
        Map<Integer, Map<String, Object>> byId = all.stream()
                .filter(s -> s.get("id") != null)
                .collect(Collectors.toMap(s -> getInt(s, "id"), s -> s));

        Map<String, Object> reloaded = byId.getOrDefault(strutturaId, strutturaOpt.get());
        return StrutturaResponse.success(toDto(reloaded, byId));
    }

    // ─── Get utenti struttura ─────────────────────────────────────────────────

    public StrutturaUtentiResponse getUtentiStruttura(Authentication authentication, Integer strutturaId) {
        Map<String, Object> admin = getAuthorizedUser(authentication);
        if (admin == null) {
            return StrutturaUtentiResponse.error(ErrorCode.NOT_AUTHORIZED);
        }

        String codiceIstat = getString(admin, "codiceIstat");
        if (codiceIstat == null || codiceIstat.isBlank()) {
            return StrutturaUtentiResponse.error(ErrorCode.NO_ENTE);
        }

        Optional<Map> strutturaOpt = coreApiClient.getStrutturaById(strutturaId);
        if (strutturaOpt.isEmpty()) {
            return StrutturaUtentiResponse.error(ErrorCode.STRUTTURA_NOT_FOUND);
        }
        if (!codiceIstat.trim().equals(getString(strutturaOpt.get(), "codiceIstatComune"))) {
            return StrutturaUtentiResponse.error(ErrorCode.NOT_AUTHORIZED);
        }

        List<Map> utentiMaps = coreApiClient.getUtentiStruttura(strutturaId);
        List<StrutturaUtentiResponse.UtenteStrutturaDto> utenti = utentiMaps.stream()
                .map(u -> StrutturaUtentiResponse.UtenteStrutturaDto.builder()
                        .id(getInt(u, "id"))
                        .nome(getString(u, "nome"))
                        .cognome(getString(u, "cognome"))
                        .nomeCompleto(getString(u, "nome") + " " + getString(u, "cognome"))
                        .email(getString(u, "email"))
                        .ruolo(getString(u, "ruolo"))
                        .ruoloStruttura(getString(u, "ruoloStruttura"))
                        .build())
                .toList();

        log.info("Found {} utenti for struttura {}", utenti.size(), strutturaId);
        return StrutturaUtentiResponse.success(utenti);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private StrutturaDto toDto(Map<String, Object> struttura, Map<Integer, Map<String, Object>> byId) {
        Integer id = getInt(struttura, "id");
        Integer idParent = getInt(struttura, "idParent");

        String parentNome = null;
        if (idParent != null && byId.containsKey(idParent)) {
            parentNome = getString(byId.get(idParent), "nome");
        }

        String responsabileNome = null;
        Object respObj = struttura.get("responsabile");
        if (respObj instanceof Map) {
            Map<String, Object> resp = (Map<String, Object>) respObj;
            String nome = getString(resp, "nome");
            String cognome = getString(resp, "cognome");
            if (nome != null && cognome != null) {
                responsabileNome = nome + " " + cognome;
            }
        }

        List<StrutturaDto.StaffMemberDto> staffDtos = new ArrayList<>();
        Object staffObj = struttura.get("staff");
        if (staffObj instanceof List) {
            for (Object item : (List<?>) staffObj) {
                if (item instanceof Map) {
                    Map<String, Object> ss = (Map<String, Object>) item;
                    Object userObj = ss.get("user");
                    if (userObj instanceof Map) {
                        Map<String, Object> user = (Map<String, Object>) userObj;
                        String nome = getString(user, "nome");
                        String cognome = getString(user, "cognome");
                        staffDtos.add(StrutturaDto.StaffMemberDto.builder()
                                .id(getInt(ss, "id"))
                                .userId(getInt(user, "id"))
                                .nome(nome)
                                .cognome(cognome)
                                .nomeCompleto(nome != null && cognome != null ? nome + " " + cognome : null)
                                .ruoloStruttura(getString(ss, "ruoloStruttura"))
                                .ordine(getInt(ss, "ordine"))
                                .build());
                    }
                }
            }
        }

        return StrutturaDto.builder()
                .id(id)
                .nome(getString(struttura, "nome"))
                .tipo(getString(struttura, "tipo"))
                .idParent(idParent)
                .parentNome(parentNome)
                .idResponsabile(getInt(struttura, "idResponsabile"))
                .responsabileNome(responsabileNome)
                .ruoloLabel(getString(struttura, "ruoloLabel"))
                .colore(getString(struttura, "colore"))
                .ordine(getInt(struttura, "ordine"))
                .staff(staffDtos.isEmpty() ? null : staffDtos)
                .build();
    }

    private Map<String, Object> getAuthorizedUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String keycloakId = jwt.getSubject();

        Optional<Map> userOpt = coreApiClient.getUserByKeycloakId(keycloakId);
        if (userOpt.isEmpty()) return null;

        Map<String, Object> user = userOpt.get();
        String ruolo = getString(user, "ruolo");
        if (!AUTHORIZED_ROLES.contains(ruolo)) return null;

        return user;
    }

    private static Integer getInt(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return null;
        if (v instanceof Integer i) return i;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(v.toString()); } catch (NumberFormatException e) { return null; }
    }

    private static String getString(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString() : null;
    }
}
