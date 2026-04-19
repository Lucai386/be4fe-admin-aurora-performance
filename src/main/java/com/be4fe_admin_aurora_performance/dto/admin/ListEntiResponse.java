package com.be4fe_admin_aurora_performance.dto.admin;

import java.util.List;

import static com.be4fe_admin_aurora_performance.enums.AppConstants.RESULT_KO;
import static com.be4fe_admin_aurora_performance.enums.AppConstants.RESULT_OK;
import com.be4fe_admin_aurora_performance.enums.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListEntiResponse {

    private String result;
    private String errorCode;
    private List<EnteDto> enti;

    public static ListEntiResponse success(List<EnteDto> enti) {
        return ListEntiResponse.builder()
                .result(RESULT_OK)
                .enti(enti)
                .build();
    }

    public static ListEntiResponse error(ErrorCode errorCode) {
        return ListEntiResponse.builder()
                .result(RESULT_KO)
                .errorCode(errorCode.getCode())
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnteDto {
        private String codiceIstat;
        private String nome;
        private long numUtenti;
        private long numStrutture;
    }
}
