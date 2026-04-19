package com.be4fe_admin_aurora_performance.dto.lpm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLpmRequest {
    private String titolo;
    private String descrizione;
    private Integer annoInizioMandato;
    private Integer annoFineMandato;
    private Integer priorita;
}
