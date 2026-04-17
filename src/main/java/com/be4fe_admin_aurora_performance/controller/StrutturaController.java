package com.be4fe_admin_aurora_performance.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.be4fe_admin_aurora_performance.dto.org.AddStaffRequest;
import com.be4fe_admin_aurora_performance.dto.org.CreateStrutturaRequest;
import com.be4fe_admin_aurora_performance.dto.org.DeleteStrutturaResponse;
import com.be4fe_admin_aurora_performance.dto.org.ListStruttureResponse;
import com.be4fe_admin_aurora_performance.dto.org.StrutturaResponse;
import com.be4fe_admin_aurora_performance.dto.org.StrutturaUtentiResponse;
import com.be4fe_admin_aurora_performance.dto.org.UpdateStrutturaRequest;
import com.be4fe_admin_aurora_performance.service.StrutturaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/admin/strutture")
@RequiredArgsConstructor
@Slf4j
public class StrutturaController {

    private final StrutturaService strutturaService;

    @GetMapping
    public ResponseEntity<ListStruttureResponse> listStrutture(Authentication authentication) {
        log.info("Listing strutture for authenticated admin");
        ListStruttureResponse response = strutturaService.listStrutture(authentication);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<StrutturaResponse> createStruttura(
            Authentication authentication,
            @Valid @RequestBody CreateStrutturaRequest request) {
        log.info("Creating new struttura: {}", request.getNome());
        StrutturaResponse response = strutturaService.createStruttura(authentication, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StrutturaResponse> updateStruttura(
            Authentication authentication,
            @PathVariable Integer id,
            @RequestBody UpdateStrutturaRequest request) {
        log.info("Updating struttura: {}", id);
        StrutturaResponse response = strutturaService.updateStruttura(authentication, id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DeleteStrutturaResponse> deleteStruttura(
            Authentication authentication,
            @PathVariable Integer id) {
        log.info("Deleting struttura: {}", id);
        DeleteStrutturaResponse response = strutturaService.deleteStruttura(authentication, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/staff")
    public ResponseEntity<StrutturaResponse> addStaff(
            Authentication authentication,
            @PathVariable Integer id,
            @RequestBody AddStaffRequest request) {
        log.info("Adding staff to struttura: {}", id);
        StrutturaResponse response = strutturaService.addStaff(authentication, id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/staff/{userId}")
    public ResponseEntity<StrutturaResponse> removeStaff(
            Authentication authentication,
            @PathVariable Integer id,
            @PathVariable Integer userId) {
        log.info("Removing staff {} from struttura: {}", userId, id);
        StrutturaResponse response = strutturaService.removeStaff(authentication, id, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/utenti")
    public ResponseEntity<StrutturaUtentiResponse> getUtentiStruttura(
            Authentication authentication,
            @PathVariable Integer id) {
        log.info("Getting utenti for struttura: {}", id);
        StrutturaUtentiResponse response = strutturaService.getUtentiStruttura(authentication, id);
        return ResponseEntity.ok(response);
    }
}
