package com.be4fe_admin_aurora_performance.dto.lpm;

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
public class LpmResponse {
    private String result;
    private String errorCode;
    private LpmDto lpm;

    public static LpmResponse success(LpmDto lpm) {
        return LpmResponse.builder().result(RESULT_OK).lpm(lpm).build();
    }

    public static LpmResponse error(ErrorCode errorCode) {
        return LpmResponse.builder().result(RESULT_KO).errorCode(errorCode.getCode()).build();
    }
}
