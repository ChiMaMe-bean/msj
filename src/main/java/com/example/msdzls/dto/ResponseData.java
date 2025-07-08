package com.example.msdzls.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 统一API响应格式
 */
@Data
@AllArgsConstructor
public class ResponseData {
    private boolean success; // 是否成功
    private String message;  // 返回消息

    public static ResponseData success(String msg) {
        return new ResponseData(true, msg);
    }

    public static ResponseData error(String msg) {
        return new ResponseData(false, msg);
    }
}
