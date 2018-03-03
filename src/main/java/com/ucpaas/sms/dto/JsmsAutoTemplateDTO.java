package com.ucpaas.sms.dto;
import com.jsmsframework.audit.entity.JsmsAutoTemplate;

/**
 * Created by xiongfenglin on 2017/10/21.
 *
 * @author: xiongfenglin
 */
public class JsmsAutoTemplateDTO extends JsmsAutoTemplate {
    private String adminName; // 操作者名称

    public String getAdminName() {
        return adminName;
    }

    public void setAdminName(String adminName) {
        this.adminName = adminName;
    }
    private String smsTypeStr;
    private String templateTypeStr;
    private String stateStr;
    private String updateTimeStr;
    private String createTimeStr;
    private String userName;//创建者名称
    private int orderNo;
    private int isCreateor;

    public int getIsCreateor() {
        return isCreateor;
    }

    public void setIsCreateor(int isCreateor) {
        this.isCreateor = isCreateor;
    }

    public int getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(int orderNo) {
        this.orderNo = orderNo;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUpdateTimeStr() {
        return updateTimeStr;
    }

    public void setUpdateTimeStr(String updateTimeStr) {
        this.updateTimeStr = updateTimeStr;
    }

    public String getCreateTimeStr() {
        return createTimeStr;
    }

    public void setCreateTimeStr(String createTimeStr) {
        this.createTimeStr = createTimeStr;
    }

    public String getSmsTypeStr() {
        return smsTypeStr;
    }

    public void setSmsTypeStr(String smsTypeStr) {
        this.smsTypeStr = smsTypeStr;
    }

    public String getTemplateTypeStr() {
        return templateTypeStr;
    }

    public void setTemplateTypeStr(String templateTypeStr) {
        this.templateTypeStr = templateTypeStr;
    }

    public String getStateStr() {
        return stateStr;
    }

    public void setStateStr(String stateStr) {
        this.stateStr = stateStr;
    }
}
