package com.ucpaas.sms.model;

import java.util.Date;

import com.ucpaas.sms.common.base.BaseEntity;

/**
 * Created by lpjLiu on 2017/6/12.
 */
public class User extends BaseEntity {

	private String userName; // 昵称(保留)',
	private String email; // '邮件',
	private String password; // '密码',
	private String oldPassword;
	private String userType; // '用户类型
								// 1:系统管理员，保留字段\r\n配置t_sms_dict.param_type=user_type',
	private String status; // '用户状态：0: 禁用
							// 1:正常，\r\n配置t_sms_dict.param_type=user_status',
	private String mobile; // '手机',
	private String realName; // '真实姓名',
	private Date createDate;
	private Date updateDate;
	private Integer loginTimes;
	private Integer webId; // 'web应用ID：1短信调度系统 2代理商平台 3运营平台 4OEM代理商平台',

	public User() {
	}

	public User(String id) {
		super(id);
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getOldPassword() {
		return oldPassword;
	}

	public void setOldPassword(String oldPassword) {
		this.oldPassword = oldPassword;
	}

	public String getUserType() {
		return userType;
	}

	public void setUserType(String userType) {
		this.userType = userType;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public String getRealName() {
		return realName;
	}

	public void setRealName(String realName) {
		this.realName = realName;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public Date getUpdateDate() {
		return updateDate;
	}

	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

	public Integer getLoginTimes() {
		return loginTimes;
	}

	public void setLoginTimes(Integer loginTimes) {
		this.loginTimes = loginTimes;
	}

	public Integer getWebId() {
		return webId;
	}

	public void setWebId(Integer webId) {
		this.webId = webId;
	}
}
