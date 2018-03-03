package com.ucpaas.sms.dto;

import com.jsmsframework.stats.entity.JsmsClientSuccessRateRealtime;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;
@ApiModel(value = "ClientSuccessRateRealtimeDTO", description = "今日提交量")
public class ClientSuccessRateRealtimeDTO extends JsmsClientSuccessRateRealtime {

	@ApiModelProperty(value = "今日提交量")
	private Integer submitTotal;
	// 发送中 ==> 0
	@ApiModelProperty(value = "发送中")
	private String sending;
	// 发送成功 ==> 3
	@ApiModelProperty(value = "发送成功")
	private String sendSuccess;
	/*
	 * // 拦截 ==> 5+7+8+9+10 // 拦截 ==> 1 - private String intercept;
	 */
	// 未知 ==> 1
	@ApiModelProperty(value = "未知")
	private String unknown;
	// 发送失败 ==> 4+6
	@ApiModelProperty(value = "发送失败")
	private String sendFail;

	public Integer getSubmitTotal() {
		if(submitTotal != null){
			return submitTotal;
		}
		if (getSendTotal() == null) {
			submitTotal = 0;
		}else{
			submitTotal = getSendTotal();
		}
		return submitTotal;
	}

	public void setSubmitTotal(Integer submitTotal) {
		this.submitTotal = submitTotal;
	}

	public String getSending() {
        sending = checkByZero(getNosend(), getSubmitTotal());
        return sending;
    }

	public void setSending(String sending) {
		this.sending = sending;
	}

	public String getSendSuccess() {
        sendSuccess = checkByZero(getReallySuccessTotal(), getSubmitTotal());
		return sendSuccess;
	}

	public void setSendSuccess(String sendSuccess) {
		this.sendSuccess = sendSuccess;
	}

	/*
	 * public String getIntercept() { return intercept; }
	 * 
	 * public void setIntercept(String intercept) { this.intercept = intercept; }
	 */

	public String getUnknown() {
        unknown = checkByZero(getFakeSuccessFail(), getSubmitTotal());
		return unknown;
	}

	public void setUnknown(String unknown) {
		this.unknown = unknown;
	}

	public String getSendFail() {
	    int temp = (getSendFailToatl() == null ? 0:getSendFailToatl()) + (getReallyFailTotal() == null ? 0:getReallyFailTotal());
        sendFail = checkByZero(temp, getSubmitTotal());
        return sendFail;
	}

	public void setSendFail(String sendFail) {
		this.sendFail = sendFail;
	}

	private String checkByZero(Integer num, Integer byNum) {
		if (byNum == null || byNum.equals(0)) {
			return "--";
		} else {
		    if (num == null){
		        num = 0;
            }
			return new BigDecimal(num).divide(new BigDecimal(byNum),4,BigDecimal.ROUND_HALF_DOWN).multiply(new BigDecimal("100"))
					.setScale(2,BigDecimal.ROUND_HALF_DOWN).toString();
		}
	}

	public ClientSuccessRateRealtimeDTO () {
	}

	public ClientSuccessRateRealtimeDTO(Integer sendTotal, Integer nosend,Integer reallySuccessTotal, Integer fakeSuccessFail, Integer sendFailToatl,Integer reallyFailTotal) {
		setSendTotal(sendTotal);
		setNosend(nosend);
		setReallySuccessTotal(reallySuccessTotal);
		setFakeSuccessFail(fakeSuccessFail);
		setSendFailToatl(sendFailToatl);
		setReallyFailTotal(reallyFailTotal);
	}
}
