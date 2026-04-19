package com.be4fe_admin_aurora_performance.dto.dup;

import java.util.List;

import com.be4fe_admin_aurora_performance.enums.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.be4fe_admin_aurora_performance.enums.AppConstants.RESULT_OK;
import static com.be4fe_admin_aurora_performance.enums.AppConstants.RESULT_KO;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListDupResponse {
    private String result;
    private String errorCode;
    private List<DupDto> dup;

    public static ListDupResponse success(List<DupDto> dup) {
        return ListDupResponse.builder().result(RESULT_OK).dup(dup).build();
    }

    public static ListDupResponse error(ErrorCode errorCode) {
        return ListDupResponse.builder().result(RESULT_KO).errorCode(errorCode.getCode()).build();
    }
}
