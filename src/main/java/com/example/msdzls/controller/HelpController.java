package com.example.msdzls.controller;

import com.example.msdzls.dto.ResponseData;
import com.example.msdzls.service.HelpService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 前端请求处理控制器
 */
@RestController
@RequestMapping("/api")
public class HelpController {
    private final HelpService helpService;

    public HelpController(HelpService helpService) {
        this.helpService = helpService;
    }

    /**
     * 处理用户提交的助力请求
     * @param code 用户输入的12位助力码
     */
    @PostMapping("/help")
    public ResponseData handleHelpRequest(@RequestParam String code) {
        return helpService.processHelpRequest(code);
    }
}
