package com.be4fe_admin_aurora_performance.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.be4fe_admin_aurora_performance.dto.dup.CreateDupRequest;
import com.be4fe_admin_aurora_performance.dto.dup.DupResponse;
import com.be4fe_admin_aurora_performance.dto.dup.ListDupResponse;
import com.be4fe_admin_aurora_performance.dto.lpm.CreateLpmRequest;
import com.be4fe_admin_aurora_performance.dto.lpm.ListLpmResponse;
import com.be4fe_admin_aurora_performance.dto.lpm.LpmResponse;
import com.be4fe_admin_aurora_performance.service.LpmDupService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class LpmDupController {

    private final LpmDupService lpmDupService;

    // ─── LPM ──────────────────────────────────────────────────────────────────

    @GetMapping("/lpm")
    public ResponseEntity<ListLpmResponse> listLpm(Authentication authentication) {
        log.info("Listing LPM for authenticated admin");
        return ResponseEntity.ok(lpmDupService.listLpm(authentication));
    }

    @PostMapping("/lpm")
    public ResponseEntity<LpmResponse> createLpm(
            Authentication authentication,
            @RequestBody CreateLpmRequest request) {
        log.info("Creating new LPM: {}", request.getTitolo());
        return ResponseEntity.ok(lpmDupService.createLpm(authentication, request));
    }

    @DeleteMapping("/lpm/{id}")
    public ResponseEntity<LpmResponse> deleteLpm(
            Authentication authentication,
            @PathVariable Long id) {
        log.info("Deleting LPM: {}", id);
        return ResponseEntity.ok(lpmDupService.deleteLpm(authentication, id));
    }

    // ─── DUP ──────────────────────────────────────────────────────────────────

    @GetMapping("/dup")
    public ResponseEntity<ListDupResponse> listDup(Authentication authentication) {
        log.info("Listing DUP for authenticated admin");
        return ResponseEntity.ok(lpmDupService.listDup(authentication));
    }

    @PostMapping("/dup")
    public ResponseEntity<DupResponse> createDup(
            Authentication authentication,
            @RequestBody CreateDupRequest request) {
        log.info("Creating new DUP: {}", request.getTitolo());
        return ResponseEntity.ok(lpmDupService.createDup(authentication, request));
    }

    @DeleteMapping("/dup/{id}")
    public ResponseEntity<DupResponse> deleteDup(
            Authentication authentication,
            @PathVariable Long id) {
        log.info("Deleting DUP: {}", id);
        return ResponseEntity.ok(lpmDupService.deleteDup(authentication, id));
    }
}
