package com.be4fe_admin_aurora_performance.dto.auth;

import lombok.Data;

@Data
public class LogoutRequest {
    private String refreshToken;
}
