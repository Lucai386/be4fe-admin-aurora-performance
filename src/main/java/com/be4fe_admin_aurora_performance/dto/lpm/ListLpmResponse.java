package com.be4fe_admin_aurora_performance.dto.lpm;

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
public class ListLpmResponse {
    private String result;
    private String errorCode;
    private List<LpmDto> lpm;

    public static ListLpmResponse success(List<LpmDto> lpm) {
        return ListLpmResponse.builder().result(RESULT_OK).lpm(lpm).build();
    }

    public static ListLpmResponse error(ErrorCode errorCode) {
        return ListLpmResponse.builder().result(RESULT_KO).errorCode(errorCode.getCode()).build();
    }
}
