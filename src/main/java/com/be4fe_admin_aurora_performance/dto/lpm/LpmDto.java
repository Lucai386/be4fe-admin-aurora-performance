package com.be4fe_admin_aurora_performance.dto.lpm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LpmDto {
    private Long id;
    private String codiceIstat;
    private Integer annoInizioMandato;
    private Integer annoFineMandato;
    private String titolo;
    private String descrizione;
    private String stato;
    private Integer priorita;
    private Integer progresso;
    private Integer responsabileId;
    private Long dupId;
    private String dupTitolo;
    private String createdAt;
}
