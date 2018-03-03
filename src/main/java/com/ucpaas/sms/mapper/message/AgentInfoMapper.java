package com.ucpaas.sms.mapper.message;

import com.ucpaas.sms.model.AgentInfo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface AgentInfoMapper {
	/**
	 * 修改agent_name 和 mobile
	 * 
	 * @param agentInfo
	 * @return
	 */
	int updateBaseInfo(AgentInfo agentInfo);

	/**
	 * 审核并认证成功后更新公司名称和鉴权状态
	 * 
	 * @param agentInfo
	 * @return
	 */
	int afterAuthToUpdateCompany(AgentInfo agentInfo);

	/**
	 * 获取认证失败的原因
	 * 
	 * @param adminId
	 * @return
	 */
	String getAuthFailCauseByUser(String adminId);

	/**
	 * 获取代理商的信息
	 * 
	 * @param adminId
	 * @return
	 */
	AgentInfo getByUserId(String adminId);

	/**
	 * 获取包装后的信息
	 * 
	 * @param adminId
	 * @return
	 */
	Map<String, Object> getAgentInfoAndPackage(String adminId);

	/**
	 * 查询所有OEM代理商、状态不是注销且时已认证的代理商
	 * 
	 * @return
	 */
	List<AgentInfo> findList();
}