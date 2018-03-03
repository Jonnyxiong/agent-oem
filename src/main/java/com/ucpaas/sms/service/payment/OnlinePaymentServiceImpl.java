package com.ucpaas.sms.service.payment;

import com.jsmsframework.common.dto.JsmsPage;
import com.jsmsframework.common.dto.R;
import com.jsmsframework.common.enums.PaymentMode;
import com.jsmsframework.common.enums.PaymentState;
import com.jsmsframework.common.enums.WebId;
import com.jsmsframework.common.util.StringUtils;
import com.jsmsframework.finance.dto.JsmsOnlinePaymentDTO;
import com.jsmsframework.finance.entity.JsmsOnlinePayment;
import com.jsmsframework.finance.exception.JsmsOnlinePaymentException;
import com.jsmsframework.finance.service.JsmsOnlinePaymentService;
import com.jsmsframework.finance.util.OnlinePaymentUtil;
import com.ucpaas.sms.service.util.ConfigUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
@Transactional
public class OnlinePaymentServiceImpl implements OnlinePaymentService {

    @Autowired
    JsmsOnlinePaymentService jsmsOnlinePaymentService;

    /**
     * 创建一个待支付的在线支付订单
     */
    @Override
    public R createOrder(BigDecimal paymentAmount, Integer paymentMode, String agentId, Long adminId) {

        //校验支付方式
        if (!(PaymentMode.ZHI_FU_BAO.getKey() == paymentMode || PaymentMode.WEI_XIN.getKey() == paymentMode)) {
            throw new JsmsOnlinePaymentException("支付方式错误");
        }
        //校验支付金额（最多一百万）
        if (paymentAmount.compareTo(new BigDecimal("1000000")) == 1 || paymentAmount.compareTo(new BigDecimal("50")) == -1) {
            throw new JsmsOnlinePaymentException("超出充值金额范围（50~1000000）");
        }
        //充值金额整数校验
        String el = "^\\d*[1-9]\\d*$";
        Pattern pattern = Pattern.compile(el);
        Matcher m = pattern.matcher(paymentAmount.toString());
        if (!m.matches()) {
            throw new JsmsOnlinePaymentException("充值金额只能输入整数");
        }

        //生成支付订单id
        Date createTime = new Date();
        String paymentId = OnlinePaymentUtil.getPaymentId(createTime, WebId.OEM代理商平台.getValue().toString(), ConfigUtils.system_id);

        R r = jsmsOnlinePaymentService.createOrder(paymentAmount, paymentMode, paymentId, agentId, adminId, createTime);
        return r;
    }

    /**
     * 获取订单信息
     *
     * @param paymentId 订单号
     * @return
     */
    @Override
    public R getPaymentInfo(String paymentId) {
        if (StringUtils.isEmpty(paymentId)) {
            throw new JsmsOnlinePaymentException("支付订单id为空");
        }
        R r = jsmsOnlinePaymentService.getPaymentInfoAndEpayInfo(paymentId, ConfigUtils.epay_merId, ConfigUtils.notify_url, ConfigUtils.return_url,
                ConfigUtils.pay_url, ConfigUtils.environmentFlag, ConfigUtils.epay_key);
        return r;
    }

    /**
     * 立即支付
     * @param paymentId 订单号
     * @param adminId   用户id
     * @return
     */
    @Override
    public R paySubmit(String paymentId, Long adminId) {
        Date now = new Date();
        R r = jsmsOnlinePaymentService.checkBeforePaySubmit(paymentId, now);
        //校验通过
        if (r.getCode() == 0) {
            Integer newPaymentState = PaymentState.SUBMIT.getValue();
            String effMinute = jsmsOnlinePaymentService.getEffMinute(false); //获取支付有效时间
            r = jsmsOnlinePaymentService.updatePaymentStateToSubmit(paymentId, PaymentState.WEI_ZHI_FU.getValue(), newPaymentState, now, effMinute, adminId);
        }
        return r;
    }

    /**
     * 取消支付
     * @param paymentId 订单号
     * @param adminId   用户id
     * @return
     */
    @Override
    public R cancelPay(String paymentId, Long adminId) {
        if (StringUtils.isEmpty(paymentId)) {
            throw new JsmsOnlinePaymentException("支付订单id为空");
        }
        JsmsOnlinePayment jsmsOnlinePayment = jsmsOnlinePaymentService.getByPaymentId(paymentId);
        if (jsmsOnlinePayment == null) {
            throw new JsmsOnlinePaymentException("支付订单不存在");
        }
        //取消支付时，当前订单的状态必须为未支付且 支付提交截止时间>当前时间
        Integer paymentState = jsmsOnlinePayment.getPaymentState();
        Date submitDeadline = jsmsOnlinePayment.getSubmitDeadline();
        //当前时间
        Date now = new Date();
        if (!(PaymentState.WEI_ZHI_FU.getValue().equals(paymentState) && submitDeadline.getTime() > now.getTime())) {
            throw new JsmsOnlinePaymentException("取消支付失败，当前支付状态不是未支付或已超过支付提交截止时间，请刷新页面后重试");
        }
        Integer oldPaymentState = PaymentState.WEI_ZHI_FU.getValue();
        Integer newPaymentState = PaymentState.CANCEL.getValue();
        R r = jsmsOnlinePaymentService.updatePaymentState(paymentId, oldPaymentState, newPaymentState, adminId);
        return r;
    }

    /**
     * 查询在线支付订单状态
     * @param paymentId 订单号
     * @return
     */
    @Override
    public R getOnlinePaymentState(String paymentId) {
        if (StringUtils.isEmpty(paymentId)) {
            throw new JsmsOnlinePaymentException("支付订单id为空");
        }
        R r = jsmsOnlinePaymentService.getOnlinePaymentState(paymentId);
        return r;
    }

    /**
     * 获取微信支付地址
     * @param paymentId 订单号
     * @param adminId   用户id
     * @return
     */
    @Override
    public R getPaymentAddrForWeChat(String paymentId, Long adminId) {
        Date now = new Date();
        R r = jsmsOnlinePaymentService.checkBeforePaySubmit(paymentId, now);
        if (r.getCode() == 0) {
            r = jsmsOnlinePaymentService.getPaymentAddrForWeChat(paymentId, ConfigUtils.epay_merId, ConfigUtils.notify_url, ConfigUtils.return_url,
                    ConfigUtils.pay_url, ConfigUtils.environmentFlag, ConfigUtils.epay_key, now, adminId);
        }
        return r;
    }

    /**
     * 支付订单列表数据加载
     * @param jsmsPage
     * @return
     */
    @Override
    public JsmsPage queryPayOrder(JsmsPage jsmsPage,Integer searchState) {

        if (PaymentState.WEI_ZHI_FU.getValue().equals(searchState)) {
            jsmsPage = jsmsOnlinePaymentService.queryPayOrder(jsmsPage, PaymentState.WEI_ZHI_FU);
        } else if (PaymentState.CANCEL.getValue().equals(searchState)) {
            jsmsPage = jsmsOnlinePaymentService.queryPayOrder(jsmsPage, PaymentState.CANCEL);
        } else {
            jsmsPage = jsmsOnlinePaymentService.queryPayOrder(jsmsPage);
        }

        List<JsmsOnlinePaymentDTO> list = jsmsPage.getData();

        for (JsmsOnlinePaymentDTO jsmsOnlinePaymentDTO : list) {
            //支付截止时间
            Date submitDeadline = jsmsOnlinePaymentDTO.getSubmitDeadline();
            //支付状态
            Integer paymentState = jsmsOnlinePaymentDTO.getPaymentState();
            Date now = new Date();
            if(submitDeadline != null){
                //如果payment_state= 未支付 & submit_deadline < now 时,页面显示为未支付
                if(submitDeadline.before(now) && paymentState.equals(PaymentState.WEI_ZHI_FU.getValue())){
                    jsmsOnlinePaymentDTO.setPaymentState(PaymentState.CANCEL.getValue());
                }
            }
        }

        return jsmsPage;
    }

}
