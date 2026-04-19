package com.be4fe_admin_aurora_performance.dto.dup;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDupRequest {
    private Integer anno;
    private String titolo;
    private String descrizione;
    private String sezione;
}
