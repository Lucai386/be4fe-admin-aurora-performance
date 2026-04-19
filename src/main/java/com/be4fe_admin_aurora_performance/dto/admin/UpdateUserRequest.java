package com.be4fe_admin_aurora_performance.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    @Size(max = 50, message = "Il nome non può superare 50 caratteri")
    private String nome;

    @Size(max = 50, message = "Il cognome non può superare 50 caratteri")
    private String cognome;

    @Email(message = "Email non valida")
    private String email;

    @Pattern(regexp = "^(SC|DR|CS|CP|DB)$", message = "Ruolo non valido")
    private String ruolo;
}
