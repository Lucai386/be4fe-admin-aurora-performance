package com.be4fe_admin_aurora_performance.dto.dup;

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
public class DupResponse {
    private String result;
    private String errorCode;
    private DupDto dup;

    public static DupResponse success(DupDto dup) {
        return DupResponse.builder().result(RESULT_OK).dup(dup).build();
    }

    public static DupResponse error(ErrorCode errorCode) {
        return DupResponse.builder().result(RESULT_KO).errorCode(errorCode.getCode()).build();
    }
}
