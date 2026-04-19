package com.be4fe_admin_aurora_performance.dto.session;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSessionResponse {

    private String result;
    private String errorCode;

    private AdminSessionUser user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminSessionUser {
        private Integer id;
        private String nome;
        private String cognome;
        private String email;
        private String codiceIstat;
        private String ruolo;
    }

    public static AdminSessionResponse success(AdminSessionUser user) {
        return AdminSessionResponse.builder()
                .result("OK")
                .user(user)
                .build();
    }

    public static AdminSessionResponse error(String errorCode) {
        return AdminSessionResponse.builder()
                .result("KO")
                .errorCode(errorCode)
                .build();
    }
}
