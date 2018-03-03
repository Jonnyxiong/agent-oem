package com.ucpaas.sms.dto;

import java.io.Serializable;

/**
 * 发送短信验证码
 *
 * @outhor tanjiangqiang
 * @create 2017-11-30 18:42
 */
public class SmsCodeVO implements Serializable {


    private String mobile;
    private String clientId;
    private String sendType;
    private String verifyCode;
    private String password;

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getSendType() {
        return sendType;
    }

    public void setSendType(String sendType) {
        this.sendType = sendType;
    }

    public String getVerifyCode() {
        return verifyCode;
    }

    public void setVerifyCode(String verifyCode) {
        this.verifyCode = verifyCode;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}