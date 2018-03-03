package com.ucpaas.sms.mapper.message;

import com.jsmsframework.user.entity.JsmsAccount;
import com.ucpaas.sms.model.AuditLog;
import com.ucpaas.sms.model.OauthPic;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface AccountMapper {

	Integer queryAgentClientNum(Map params);

	List<Map<String, Object>> querySixMonthsAgentClientNum(Map params);

	Integer queryCustomerForBalanceLackCount(Map params);

	List<Map<String, Object>> queryCustomerForBalanceLack(Map params);

	int getTestCount(String agentId);

	String getRealName(String clientId);

	OauthPic getCerInfo(String clientId);

	String getNotPassRemark(String clientId);

	String getAuditLogByAgentId(String agentId);

	int addCerInfo(OauthPic oauthPic);

	int updateCerInfo(OauthPic oauthPic);

	int updateAccWithCer(OauthPic oauthPic);

	int insertAuditLog(AuditLog auditLog);

	int updateOauthDate(String clientId);

	List<Map<String, Object>> queryCustomerForBalanceByType(Map params);

	String queryCustomerForBalanceByTypeCount(Map params);

	List<JsmsAccount> getSearchList(String agentId);


}