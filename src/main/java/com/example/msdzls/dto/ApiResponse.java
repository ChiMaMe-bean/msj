package com.example.msdzls.dto;

import lombok.Data;

@Data
public class ApiResponse {
    private boolean success;
    private String errorMsg;

    public static ApiResponse success() {
        ApiResponse res = new ApiResponse();
        res.setSuccess(true);
        return res;
    }

    public static ApiResponse error(String msg) {
        ApiResponse res = new ApiResponse();
        res.setSuccess(false);
        res.setErrorMsg(msg);
        return res;
    }

    // 明确添加getter方法
    public boolean isSuccess() {
        return success;
    }

    public String getErrorMsg() {
        return errorMsg;
    }
}
