package com.ucpaas.sms.service.customer;

import com.ucpaas.sms.model.OauthPic;

import java.util.Map;

public interface AccountManageService {

	/**
	 * 获取测试帐号的数量
	 * 
	 * @param agentId
	 * @return
	 */
	public boolean canOpenTestCount(String agentId);

	/**
	 * 获取客户的资质信息
	 * 
	 * @param clientId
	 * @return
	 */
	public OauthPic getClientCerInfo(String clientId);

	/**
	 * 添加资质
	 * 
	 * @param oauthPic
	 * @return
	 */
	public Map<String, Object> addClientCerInfo(OauthPic oauthPic);

	public Map<String, Object> modClientCerInfo(OauthPic oauthPic);
}
