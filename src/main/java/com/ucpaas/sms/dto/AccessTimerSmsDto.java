package com.ucpaas.sms.dto;

import com.jsmsframework.sms.send.po.JsmsAccessTimerSms;

/**
 * Created by xiongfenglin on 2018/1/11.
 *
 * @author: xiongfenglin
 */
public class AccessTimerSmsDto {
    private JsmsAccessTimerSms jsmsAccessTimerSms;
    //定时任务ID
    private String taskId;
    //总计费条数
    private Integer chargeNumTotal;
    //是否确认提交  0第一次提交    1再次提交
    private String submitFlag;

    public JsmsAccessTimerSms getJsmsAccessTimerSms() {
        return jsmsAccessTimerSms;
    }

    public void setJsmsAccessTimerSms(JsmsAccessTimerSms jsmsAccessTimerSms) {
        this.jsmsAccessTimerSms = jsmsAccessTimerSms;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public Integer getChargeNumTotal() {
        return chargeNumTotal;
    }

    public void setChargeNumTotal(Integer chargeNumTotal) {
        this.chargeNumTotal = chargeNumTotal;
    }

    public String getSubmitFlag() {
        return submitFlag;
    }

    public void setSubmitFlag(String submitFlag) {
        this.submitFlag = submitFlag;
    }
}
