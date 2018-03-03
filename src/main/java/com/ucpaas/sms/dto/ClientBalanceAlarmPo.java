package com.ucpaas.sms.dto;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 余额提醒Po
 *
 * @outhor tanjiangqiang
 * @create 2017-12-13 15:00
 */
public class ClientBalanceAlarmPo {
    // 序号ID,自增长,主键
    private Integer id;
    // 用户账号,关联t_sms_account表中clientid字段
    private String clientid;
    // 接收告警手机号,多个手机号以","分隔
    private String alarmPhone;
    //备用告警手机号，多个手机号以","分隔，只允许在运营平台显示和修改，适用于品牌客户
    private String ccAlarmPhone;
    // 告警阀值,单位:条
    private Integer alarmNumber;
    // 告警可提醒次数
    private Integer reminderNumber;
    // 告警接收邮箱多个邮箱以","分隔，适用于OEM客户
    private String alarmEmail;
    // 验证码告警阈值，单位：条，适用于OEM客户
    private Integer yzmAlarmNumber;
    // 通知告警阈值，单位：条，适用于OEM客户
    private Integer tzAlarmNumber;
    // 营销告警阈值，单位：条，适用于OEM客户
    private Integer yxAlarmNumber;
    // 国际告警阈值，单位：元，适用于OEM客户
    private BigDecimal gjAlarmAmount;
    // 告警次数充值时间
    private Date resetTime;
    // 创建时间
    private Date createTime;
    // 更新时间
    private Date updateTime;
    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id ;
    }
    public String getClientid() {
        return clientid;
    }
    public void setClientid(String clientid) {
        this.clientid = clientid ;
    }
    public String getAlarmPhone() {
        return alarmPhone;
    }
    public void setAlarmPhone(String alarmPhone) {
        this.alarmPhone = alarmPhone ;
    }
    public Integer getAlarmNumber() {
        return alarmNumber;
    }
    public void setAlarmNumber(Integer alarmNumber) {
        this.alarmNumber = alarmNumber ;
    }
    public Integer getReminderNumber() {
        return reminderNumber;
    }
    public void setReminderNumber(Integer reminderNumber) {
        this.reminderNumber = reminderNumber ;
    }
    public String getAlarmEmail() {
        return alarmEmail;
    }
    public void setAlarmEmail(String alarmEmail) {
        this.alarmEmail = alarmEmail;
    }
    public Integer getYzmAlarmNumber() {
        return yzmAlarmNumber;
    }
    public void setYzmAlarmNumber(Integer yzmAlarmNumber) {
        this.yzmAlarmNumber = yzmAlarmNumber;
    }
    public Integer getTzAlarmNumber() {
        return tzAlarmNumber;
    }
    public void setTzAlarmNumber(Integer tzAlarmNumber) {
        this.tzAlarmNumber = tzAlarmNumber;
    }
    public Integer getYxAlarmNumber() {
        return yxAlarmNumber;
    }
    public void setYxAlarmNumber(Integer yxAlarmNumber) {
        this.yxAlarmNumber = yxAlarmNumber;
    }
    public BigDecimal getGjAlarmAmount() {
        return gjAlarmAmount;
    }
    public void setGjAlarmAmount(BigDecimal gjAlarmAmount) {
        this.gjAlarmAmount = gjAlarmAmount;
    }
    public Date getResetTime() {
        return resetTime;
    }
    public void setResetTime(Date resetTime) {
        this.resetTime = resetTime ;
    }
    public Date getCreateTime() {
        return createTime;
    }
    public void setCreateTime(Date createTime) {
        this.createTime = createTime ;
    }
    public Date getUpdateTime() {
        return updateTime;
    }
    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime ;
    }
    public String getCcAlarmPhone() {
        return ccAlarmPhone;
    }
    public void setCcAlarmPhone(String ccAlarmPhone) {
        this.ccAlarmPhone = ccAlarmPhone;
    }
}