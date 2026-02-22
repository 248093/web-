package top.lyh.controller;

import cn.hutool.core.lang.UUID;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import top.lyh.common.ResultDTO;
import top.lyh.config.AliPayConfig;
import top.lyh.entity.pojo.GiftTransaction;
import top.lyh.entity.pojo.SysUser;
import top.lyh.service.GiftTransactionService;
import top.lyh.service.SysUserService;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
public class AliPayController {
    private static final String GATEWAY_URL = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";
    private static final String FORMAT = "JSON";
    private static final String CHARSET = "UTF-8";
    private static final String SIGN_TYPE = "RSA2";

    @Resource
    private AliPayConfig aliPayConfig;

    @Resource
    private GiftTransactionService transactionService;
    @Resource
    private SysUserService sysUserService;

    @PostMapping("/pay")
    public void pay(@RequestBody GiftTransaction aliPay, HttpServletResponse httpResponse) throws IOException {
        // 设置响应编码
        httpResponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
        httpResponse.setContentType("text/html;charset=UTF-8");

        PrintWriter writer = httpResponse.getWriter();

        try {
            aliPay.setPaymentType(GiftTransaction.PaymentType.alipay);
            aliPay.setTraceNo(UUID.randomUUID().toString());

            // 参数校验
            if (aliPay.getTraceNo() == null || aliPay.getAmount() == null || aliPay.getSubject() == null) {
                writer.write("参数不能为空");
                writer.flush();
                return;
            }

            Subject subject = SecurityUtils.getSubject();
            if (!subject.isAuthenticated()) {
                writer.write("请先登录");
                writer.flush();
                return;
            }

            SysUser currentUser = (SysUser) subject.getPrincipal();

            // ========== 1. 创建订单，插入数据库 ==========
            aliPay.setTransactionNo(aliPay.getTraceNo());
            aliPay.setPaymentType(GiftTransaction.PaymentType.alipay);
            aliPay.setStatus(GiftTransaction.TransactionStatus.pending);
            aliPay.setUserId(currentUser.getId());
            aliPay.setCreatedAt(LocalDateTime.now());

            // 插入数据库
            transactionService.save(aliPay);
            log.info("订单已创建，ID：{}，交易号：{}，用户Id：{}", aliPay.getId(), aliPay.getTransactionNo(), aliPay.getUserId());

            // ========== 2. 调用支付宝支付接口 ==========
            AlipayClient alipayClient = new DefaultAlipayClient(
                    GATEWAY_URL,
                    aliPayConfig.getAppId(),
                    aliPayConfig.getAppPrivateKey(),
                    FORMAT,
                    CHARSET,
                    aliPayConfig.getAlipayPublicKey(),
                    SIGN_TYPE
            );

            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            request.setNotifyUrl(aliPayConfig.getNotifyUrl());

            JSONObject bizContent = new JSONObject();
            bizContent.put("out_trade_no", aliPay.getTraceNo());
            bizContent.put("total_amount", aliPay.getAmount().toString());
            bizContent.put("subject", aliPay.getSubject());
            bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
            request.setBizContent(bizContent.toString());

            String form = alipayClient.pageExecute(request).getBody();

            // 确保返回的 HTML 使用 UTF-8 编码
            byte[] htmlBytes = form.getBytes(StandardCharsets.UTF_8);
            httpResponse.setContentLength(htmlBytes.length);
            writer.write(new String(htmlBytes, StandardCharsets.UTF_8));
            writer.flush();

        } catch (AlipayApiException e) {
            log.error("支付宝支付异常", e);
            writer.write("支付异常：" + e.getMessage());
            writer.flush();
        } catch (Exception e) {
            log.error("系统异常", e);
            writer.write("系统异常：" + e.getMessage());
            writer.flush();
        } finally {
            writer.close();
        }
    }

    @PostMapping("/notify")
    @Transactional
    public ResultDTO payNotify(HttpServletRequest request) {
        log.info("=========支付宝异步回调========");

        try {
            // 获取所有参数
            Map<String, String> params = new HashMap<>();
            Map<String, String[]> requestParams = request.getParameterMap();
            for (String name : requestParams.keySet()) {
                params.put(name, request.getParameter(name));
            }

            log.info("回调参数: {}", JSONObject.toJSONString(params));

            boolean signVerified = AlipaySignature.rsaCheckV1(
                    params,
                    aliPayConfig.getAlipayPublicKey(),
                    CHARSET,
                    SIGN_TYPE
            );

            if (signVerified) {
                log.info("签名验证成功");

                String tradeStatus = params.get("trade_status");
                String outTradeNo = params.get("out_trade_no");

                if ("TRADE_SUCCESS".equals(tradeStatus)) {
                    log.info("订单 {} 支付成功", outTradeNo);

                    LambdaQueryWrapper<GiftTransaction> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(GiftTransaction::getTransactionNo, outTradeNo);
                    GiftTransaction transaction = transactionService.getOne(wrapper);

                    if (transaction != null) {
                        transaction.setStatus(GiftTransaction.TransactionStatus.success);
                        transactionService.updateById(transaction);

                        SysUser byId = sysUserService.getById(transaction.getUserId());
                        if (byId != null) {
                            byId.setAccountMoney(byId.getAccountMoney().add(transaction.getAmount()));
                            sysUserService.updateById(byId);
                        } else {
                            log.warn("用户不存在，无法充值");
                            return ResultDTO.error("用户不存在");
                        }
                        log.info("订单状态已更新为成功：{}", outTradeNo);
                    }
                    return ResultDTO.success("success");
                }
                return ResultDTO.success("success");
            } else {
                log.error("签名验证失败");
                return ResultDTO.error("failure");
            }
        } catch (Exception e) {
            log.error("处理支付宝回调异常", e);
            return ResultDTO.error("failure");
        }
    }
    /**
     * 查询订单状态（只处理待支付和失败的情况，成功状态由回调处理）
     */
    @GetMapping("/order/status")
    @Transactional
    public ResultDTO getOrderStatus(@RequestParam String orderNo) {
        log.info("查询订单状态，订单号: {}", orderNo);

        try {
            // 1. 查询本地数据库
            LambdaQueryWrapper<GiftTransaction> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(GiftTransaction::getTransactionNo, orderNo);
            GiftTransaction transaction = transactionService.getOne(wrapper);

            if (transaction == null) {
                return ResultDTO.error("订单不存在");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("orderNo", orderNo);
            result.put("amount", transaction.getAmount());
            result.put("createdAt", transaction.getCreatedAt());

            // 2. 如果已经是成功状态，直接返回
            if (transaction.getStatus() == GiftTransaction.TransactionStatus.success) {
                result.put("paid", true);
                result.put("status", "SUCCESS");
                result.put("message", "支付成功");
                return ResultDTO.success("查询成功", result);
            }

            // 3. 如果是失败状态，直接返回
            if (transaction.getStatus() == GiftTransaction.TransactionStatus.failed) {
                result.put("paid", false);
                result.put("status", "FAILED");
                result.put("message", "支付失败");
                return ResultDTO.success("查询成功", result);
            }

            // 4. 如果是待支付状态，查询支付宝
            if (transaction.getStatus() == GiftTransaction.TransactionStatus.pending) {

                // 4.1 先检查是否超时
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime expireTime = transaction.getCreatedAt().plusMinutes(2);

                if (now.isAfter(expireTime)) {
                    // 订单已超时，更新为失败状态
                    transaction.setStatus(GiftTransaction.TransactionStatus.failed);
                    transactionService.updateById(transaction);

                    result.put("paid", false);
                    result.put("status", "TIMEOUT");
                    result.put("message", "支付超时");
                    return ResultDTO.success("查询成功", result);
                }

                try {
                    AlipayClient alipayClient = new DefaultAlipayClient(
                            GATEWAY_URL,
                            aliPayConfig.getAppId(),
                            aliPayConfig.getAppPrivateKey(),
                            FORMAT,
                            CHARSET,
                            aliPayConfig.getAlipayPublicKey(),
                            SIGN_TYPE
                    );

                    AlipayTradeQueryRequest queryRequest = new AlipayTradeQueryRequest();
                    JSONObject bizContent = new JSONObject();
                    bizContent.put("out_trade_no", orderNo);
                    queryRequest.setBizContent(bizContent.toString());

                    AlipayTradeQueryResponse response = alipayClient.execute(queryRequest);

                    // 5. 处理支付宝查询结果
                    if (response.isSuccess()) {
                        String tradeStatus = response.getTradeStatus();
                        log.info("支付宝查询成功 - 订单号: {}, 状态: {}", orderNo, tradeStatus);

                        switch (tradeStatus) {
                            case "WAIT_BUYER_PAY":
                                // 等待支付 - 继续轮询
                                result.put("paid", false);
                                result.put("status", "PENDING");
                                result.put("message", "等待用户扫码支付");
                                break;

                            case "TRADE_CLOSED":
                                // 交易已关闭，更新本地状态为失败
                                transaction.setStatus(GiftTransaction.TransactionStatus.failed);
                                transactionService.updateById(transaction);
                                result.put("paid", false);
                                result.put("status", "FAILED");
                                result.put("message", "交易已关闭");
                                break;

                            case "TRADE_SUCCESS":
                            case "TRADE_FINISHED":
                                // 支付成功，更新本地状态
                                transaction.setStatus(GiftTransaction.TransactionStatus.success);
                                transaction.setTraceNo(response.getTradeNo());
                                transactionService.updateById(transaction);

                                // 更新用户余额
                                SysUser user = sysUserService.getById(transaction.getUserId());
                                if (user != null) {
                                    user.setAccountMoney(user.getAccountMoney().add(transaction.getAmount()));
                                    sysUserService.updateById(user);
                                    log.info("用户 {} 余额已更新", user.getId());
                                }

                                result.put("paid", true);
                                result.put("status", "SUCCESS");
                                result.put("message", "支付成功");
                                break;

                            default:
                                result.put("paid", false);
                                result.put("status", "PENDING");
                                result.put("message", "支付处理中");
                        }
                    } else {
                        // 支付宝查询失败（包括交易不存在）
                        String subCode = response.getSubCode();
                        String subMsg = response.getSubMsg();
                        log.info("支付宝查询失败 - 订单号: {}, subCode: {}, subMsg: {}", orderNo, subCode, subMsg);

                        if ("ACQ.TRADE_NOT_EXIST".equals(subCode)) {
                            LocalDateTime now1 = LocalDateTime.now();
                            LocalDateTime expireTime1 = transaction.getCreatedAt().plusMinutes(1);
                            if (now1.isAfter(expireTime1)){
                                transaction.setStatus(GiftTransaction.TransactionStatus.failed);
                                transactionService.updateById(transaction);
                                result.put("paid", false);
                                result.put("status", "FAILED");
                                result.put("message", "超过一分钟未扫码");
                            }else {
                                // 交易不存在 - 说明用户还没扫码，继续轮询
                                result.put("paid", false);
                                result.put("status", "PENDING");
                                result.put("message", "等待扫码支付");
                                // 可以记录扫码次数或时间
                                log.info("订单 {} 尚未被扫码", orderNo);
                            }
                        } else {
                            // 其他错误，继续轮询但记录错误
                            result.put("paid", false);
                            result.put("status", "PENDING");
                            result.put("message", "支付处理中");
                            log.warn("支付宝查询其他错误: {} - {}", subCode, subMsg);
                        }
                    }

                    return ResultDTO.success("查询成功", result);

                } catch (Exception e) {
                    log.error("查询支付宝订单状态异常", e);

                    // 支付宝查询异常，但订单未超时，继续轮询
                    result.put("paid", false);
                    result.put("status", "PENDING");
                    result.put("message", "查询支付状态中");
                    return ResultDTO.success("查询成功", result);
                }
            }

            // 6. 默认返回
            result.put("paid", false);
            result.put("status", "PENDING");
            result.put("message", "待支付");
            return ResultDTO.success("查询成功", result);

        } catch (Exception e) {
            log.error("查询订单状态异常", e);
            return ResultDTO.error("系统异常");
        }
    }
}