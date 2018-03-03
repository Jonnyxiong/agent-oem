package com.ucpaas.sms.model;

import java.util.Date;

import com.ucpaas.sms.common.base.BaseEntity;

/**
 * Created by lpjLiu on 2017/6/12.
 */
public class ClientIdSequence extends BaseEntity {

	private String clientId; // '客户账号(6位,首位a-z,末位0-9,其余为0-z)',
	private String status; // '客户账号使用状态,0:未使用,1:已使用',
	private String lock; // '是否被锁定，0：否 1：是',
	private Date lockStartTime; // '锁定开始时间',
	private String clientIdType; // 'clientid类型：0 归属于代理商的客户的账号，1 直客的账号',

	public ClientIdSequence() {

	}

	public ClientIdSequence(String id) {
		super(id);
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getLock() {
		return lock;
	}

	public void setLock(String lock) {
		this.lock = lock;
	}

	public Date getLockStartTime() {
		return lockStartTime;
	}

	public void setLockStartTime(Date lockStartTime) {
		this.lockStartTime = lockStartTime;
	}

	public String getClientIdType() {
		return clientIdType;
	}

	public void setClientIdType(String clientIdType) {
		this.clientIdType = clientIdType;
	}
}
