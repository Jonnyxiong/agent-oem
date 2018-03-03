package com.ucpaas.sms.model;

import java.util.Date;

import com.ucpaas.sms.common.base.BaseEntity;

/**
 * Created by lpjLiu on 2017/6/12.
 */
public class AuditLog extends BaseEntity {
	private String agentId;
	private String clientId;
	private String adminId;
	private String auditType; // 审核类型，1：代理商认证，2：客户认证
	private String status; // 状态，0：审核不通过，1：审核通过',
	private Date createDate;
	private String remark;

	public AuditLog() {

	}

	public AuditLog(String id) {
		super(id);
	}

	public String getAgentId() {
		return agentId;
	}

	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getAdminId() {
		return adminId;
	}

	public void setAdminId(String adminId) {
		this.adminId = adminId;
	}

	public String getAuditType() {
		return auditType;
	}

	public void setAuditType(String auditType) {
		this.auditType = auditType;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}
}
