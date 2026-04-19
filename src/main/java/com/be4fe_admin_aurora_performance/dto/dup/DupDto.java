package com.be4fe_admin_aurora_performance.dto.dup;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DupDto {
    private Long id;
    private String codiceIstat;
    private String codice;
    private Integer anno;
    private String titolo;
    private String descrizione;
    private String sezione;
    private String stato;
    private LocalDate dataApprovazione;
    private String createdAt;
}
