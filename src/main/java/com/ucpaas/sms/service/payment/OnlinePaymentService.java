package com.ucpaas.sms.service.payment;


import com.jsmsframework.common.dto.JsmsPage;
import com.jsmsframework.common.dto.R;

import java.math.BigDecimal;

public interface OnlinePaymentService {

    /**
     * 创建一个待支付的在线支付订单
     */
    R createOrder(BigDecimal paymentAmount, Integer paymentMode, String agentId, Long adminId);

    /**
     * 获取订单信息
     *
     * @param paymentId 订单号
     * @return
     */
    R getPaymentInfo(String paymentId);

    /**
     * 立即支付
     *
     * @param paymentId 订单号
     * @param adminId   用户id
     * @return
     */
    R paySubmit(String paymentId, Long adminId);

    /**
     * 取消支付
     *
     * @param paymentId 订单号
     * @param adminId   用户id
     * @return
     */
    R cancelPay(String paymentId, Long adminId);

    /**
     * 查询在线支付订单状态
     *
     * @param paymentId
     * @return
     */
    R getOnlinePaymentState(String paymentId);

    /**
     * 获取微信支付地址
     *
     * @param paymentId 订单号
     * @param adminId   用户id
     * @return
     */
    R getPaymentAddrForWeChat(String paymentId, Long adminId);

    /**
     * 支付订单列表数据加载
     *
     * @param jsmsPage
     * @return
     */
    JsmsPage queryPayOrder(JsmsPage jsmsPage,Integer paymentState);
}
