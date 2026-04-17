package com.be4fe_admin_aurora_performance.dto.session;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionUserDto {

    private String id;
    private String nome;
    private String cognome;
    private String codiceFiscale;
    private String codiceIstat;
    private String ruolo;
    private Map<String, Object> assegnazioni;
}
