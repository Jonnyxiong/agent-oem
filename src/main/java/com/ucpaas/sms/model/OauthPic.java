package com.ucpaas.sms.model;

import com.ucpaas.sms.common.base.BaseEntity;

import java.util.Date;

/**
 * Created by lpjLiu on 2017/6/12.
 */
public class OauthPic extends BaseEntity {
	private String agentId;
	private String clientId;
	private String idNbr;
	private String imgUrl;

	private String idType;
	private String oauthType; // 认证类型： 1、代理商资质认证 2、客户资质认证
	private String status;
	private Date createDate;
	private Date updateDate;
	private String oauthStatus; // 认证状态，2：待认证 ，3：证件已认证(正常)，4：认证不通过
	private String realName;
	private String clientType; // 用户类型，1：个人用户，2：企业用户
	private String reason;

	public OauthPic() {

	}

	public OauthPic(String id) {
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

	public String getIdNbr() {
		return idNbr;
	}

	public void setIdNbr(String idNbr) {
		this.idNbr = idNbr;
	}

	public String getImgUrl() {
		return imgUrl;
	}

	public void setImgUrl(String imgUrl) {
		this.imgUrl = imgUrl;
	}

	public String getIdType() {
		return idType;
	}

	public void setIdType(String idType) {
		this.idType = idType;
	}

	public String getOauthType() {
		return oauthType;
	}

	public void setOauthType(String oauthType) {
		this.oauthType = oauthType;
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

	public Date getUpdateDate() {
		return updateDate;
	}

	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

	public String getOauthStatus() {
		return oauthStatus;
	}

	public void setOauthStatus(String oauthStatus) {
		this.oauthStatus = oauthStatus;
	}

	public String getRealName() {
		return realName;
	}

	public void setRealName(String realName) {
		this.realName = realName;
	}

	public String getClientType() {
		return clientType;
	}

	public void setClientType(String clientType) {
		this.clientType = clientType;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}
}
