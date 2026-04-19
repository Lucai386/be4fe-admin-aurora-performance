package com.be4fe_admin_aurora_performance.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.be4fe_admin_aurora_performance.dto.auth.LoginRequest;
import com.be4fe_admin_aurora_performance.dto.auth.LoginResponse;
import com.be4fe_admin_aurora_performance.dto.auth.LogoutRequest;
import com.be4fe_admin_aurora_performance.dto.auth.RefreshTokenRequest;
import com.be4fe_admin_aurora_performance.dto.common.ApiResponse;
import com.be4fe_admin_aurora_performance.service.AdminAuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
@Slf4j
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Admin login attempt: {}", request.getUsername());
        LoginResponse response = adminAuthService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Login effettuato con successo"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = adminAuthService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Token rinnovato con successo"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody LogoutRequest request) {
        adminAuthService.logout(request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Logout effettuato con successo"));
    }
}
