package com.example.msdzls.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.msdzls.dto.ApiResponse;
import com.example.msdzls.dto.ResponseData;
import com.example.msdzls.entity.Account;
import com.example.msdzls.entity.Activity;
import com.example.msdzls.exception.ApiException;
import com.example.msdzls.exception.BusinessException;
import com.example.msdzls.repository.AccountRepository;
import com.example.msdzls.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Optional;

/**
 * 核心业务服务 - 处理4399助力逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HelpService {
    // 数据库访问组件
    private final AccountRepository accountRepo;
    private final ActivityRepository activityRepo;

    // HTTP请求组件
    private final RestTemplate restTemplate;

    // 事务模板（用于嵌套事务）
    private final TransactionTemplate transactionTemplate;

    // 活动天数配置（格式示例：119-130）
    @Value("${msdzls.active-days:119-130}")
    private String activeDays;

    /**
     * 处理助力请求主方法
     * @param userCode 用户输入的12位助力码
     * @return 处理结果（成功/失败信息）
     */
    @Transactional(rollbackFor = Exception.class) // 任何异常都回滚事务
    public ResponseData processHelpRequest(String userCode) {
        try {
            // === 阶段1：基础校验 ===
            // 自动获取当前日期在一年中的第几天（如5月6日=126）
            int currentDay = LocalDate.now().getDayOfYear();

            // 校验1：是否在活动时间段内
            if (!isWithinActiveDays(currentDay)) {
                log.warn("[安全警告] 非活动时间访问：day={}", currentDay);
                return ResponseData.error("活动暂未开放");
            }

            // 校验2：助力码格式是否正确（16进制+12位）
            if (!isValidCode(userCode)) {
                return ResponseData.error("请输入正确的12位助力码");
            }

            // 校验3：该码是否已被全局使用
            if (activityRepo.existsByHelpCodeAndDayAndTypeConflict(userCode, currentDay)) {
                return ResponseData.error("该助力码已被使用");
            }

            // === 阶段2：获取可用账号 ===
            // 使用悲观锁查询可用账号（防止并发重复）
            Account helper = accountRepo.findAvailableHelper(currentDay, userCode)
                    .orElseThrow(() -> new BusinessException("暂时没有可用账号，请稍后再试"));

            // === 阶段3：调用4399接口 ===
            ApiResponse apiResponse = call4399ApiSafely(userCode, helper.getCookies());
            if (!apiResponse.isSuccess()) {
                // 接口调用失败不记录助力码
                log.warn("[接口异常] 4399接口调用失败：{}", apiResponse.getErrorMsg());
                throw new ApiException("助力失败：" + apiResponse.getErrorMsg());
            }

            // === 阶段4：保存记录 ===
            saveActivityRecords(helper, userCode, currentDay);

            // === 阶段5：获取返回码 ===
            String returnCode = getAvailableReturnCode(currentDay, userCode);

            return ResponseData.success("助力成功！请为此码助力：" + returnCode);
        } catch (BusinessException e) {
            // 已知业务异常直接返回给前端
            return ResponseData.error(e.getMessage());
        } catch (Exception e) {
            // 系统异常记录日志
            log.error("[系统异常] 处理助力请求失败：", e);
            return ResponseData.error("系统繁忙，请稍后重试");
        }
    }

    // ==================== 内部工具方法 ====================

    /**
     * 校验当前是否在活动时间段内
     * @param day 当前年中日（如126）
     */
    private boolean isWithinActiveDays(int day) {
        // 解析配置的天数范围（示例："119-130"）
        String[] daysRange = activeDays.split("-");
        int startDay = Integer.parseInt(daysRange[0]);
        int endDay = Integer.parseInt(daysRange[1]);

        // 判断当前是否在有效期内
        return day >= startDay && day <= endDay;
    }

    /**
     * 校验助力码格式合法性
     * @param code 用户输入的助力码
     */
    private boolean isValidCode(String code) {
        return code != null
                && code.length() == 12
                && code.matches("^[0-9A-Fa-f]{12}$"); // 16进制校验
    }

    /**
     * 带重试机制的4399接口调用（最多重试3次）
     * @param code 助力码
     * @param cookies 账号cookies
     */
    private ApiResponse call4399ApiSafely(String code, String cookies) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", cookies);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("_AJAX_", "1");

        int retryCount = 0;
        while (retryCount < 3) {
            try {
                // 获取原始字节响应
                ResponseEntity<byte[]> response = restTemplate.exchange(
                        "http://my.4399.com/zhuanti/msdzls/msj-ajaxBindCode",
                        HttpMethod.POST,
                        new HttpEntity<>(params, headers),
                        byte[].class // 接收原始字节数组
                );

                // 强制使用UTF-8解码
                String rawBody = new String(response.getBody(), StandardCharsets.UTF_8);

                // 调试输出原始字节和转换后的字符串
                log.debug("原始字节：[{}]", HexFormat.of().formatHex(response.getBody()));
                log.debug("转换后内容：{}", rawBody);

                // 解析JSON
                JSONObject json = parseJson(rawBody); // 使用已解码的字符串

                if (json == null) {
                    return ApiResponse.error("无效的响应格式");
                }

                if (json.getInteger("status") != 1) {
                    String msg = json.getString("msg");
                    // 二次编码验证（如果服务器返回的是双重编码）
                    if (!isValidUTF8(msg)) {
                        msg = new String(msg.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                    }
                    return ApiResponse.error(msg);
                }

                return ApiResponse.success();
            } catch (ResourceAccessException e) {
                log.warn("网络异常，第{}次重试...", retryCount + 1);
                retryCount++;
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            } catch (Exception e) {
                log.error("接口调用异常", e);
                return ApiResponse.error("系统异常");
            }
        }
        return ApiResponse.error("接口请求超时");
    }

    // UTF-8有效性验证方法
    private boolean isValidUTF8(String str) {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);

        try {
            ByteBuffer buffer = ByteBuffer.wrap(str.getBytes(StandardCharsets.UTF_8));
            decoder.decode(buffer);
            return true;
        } catch (CharacterCodingException e) {
            return false;
        }
    }

    /**
     * 保存助力记录（使用嵌套事务保证数据一致性）
     * @param helper 助力者账号
     * @param helpCode 被助力码
     * @param day 当前活动日
     */
    private void saveActivityRecords(Account helper, String helpCode, int day) {
        // 保存助力者记录（类型0）
        Activity helperRecord = new Activity();
        helperRecord.setAccountId(helper.getId());
        helperRecord.setDayNumber(day);
        helperRecord.setType(0);
        helperRecord.setHelpCode(helpCode);
        activityRepo.save(helperRecord);

        // 在新事务中保存被助力记录（类型1）
        transactionTemplate.execute(status -> {
            // 获取可用被助力账号（同样带锁）
            Account helped = accountRepo.findAvailableHelped(day)
                    .orElseThrow(() -> new BusinessException("无可用被助力账号"));

            Activity helpedRecord = new Activity();
            helpedRecord.setAccountId(helped.getId());
            helpedRecord.setDayNumber(day);
            helpedRecord.setType(1);
            helpedRecord.setHelpCode(helpCode);
            activityRepo.save(helpedRecord);

            return helped.getCode();
        });
    }

    /**
     * 获取可返回的助力码（防重复）
     * @param day 当前活动日
     * @param usedCode 已使用的码
     */
    private String getAvailableReturnCode(int day, String usedCode) {
        log.debug("正在查询可用返回码：usedCode={}, day={}", usedCode, day);
        return accountRepo.findByCodeNotUsed(usedCode, day)
                .map(Account::getCode)
                .orElseThrow(() -> new BusinessException("暂时没有可用助力码"));
    }

    /**
     * 解析JSON响应（示例实现）
     */
    private JSONObject parseJson(String body) {
        try {
            return JSON.parseObject(body);
        } catch (Exception e) {
            log.warn("[数据异常] JSON解析失败：{}", body);
            return null;
        }
    }
}