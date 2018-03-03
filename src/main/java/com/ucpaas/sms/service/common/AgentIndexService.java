package com.ucpaas.sms.service.common;

import com.ucpaas.sms.common.entity.PageContainer;
import com.ucpaas.sms.dto.ClientSuccessRateRealtimeDTO;

import java.util.List;
import java.util.Map;

public interface AgentIndexService {

	/**
	 * @Title: queryCustomerForBalanceLack
	 * @Description: 查询余额不足的客户
	 * @param params
	 * @return
	 * @return: PageContainer
	 */
	PageContainer queryCustomerForBalanceLack(Map<String, String> params);

	/**
	 * @Title: queryAgentClientNum
	 * @Description: 查询代理商的客户数量
	 * @param params
	 * @return
	 * @return: Integer
	 */
	Integer queryAgentClientNum(Map<String, String> params);

	/**
	 * @Title: querySixMonthsAgentClientNum
	 * @Description: 查询六个月的代理商的客户数量
	 * @param params
	 * @return
	 * @return: List<Map<String,Object>>
	 */
	List<Map<String, Object>> querySixMonthsAgentClientNum(Map<String, String> params);


	String queryClientBlanceNum(Map<String, String> params);

	/**
	 * @param params
	 * @return
	 * @Title: queryCustomerForBalanceByType
	 * @Description: 查询子客户详情剩余短信量
	 * @return: List<Map<String,Object>>
	 */
	List<Map<String, Object>> queryCustomerForBalanceByType(Map<String, String> params);

	/**
	 * 查询今日客户提交数据
	 * @param clientIds
	 * @param agentId
	 * @return
	 */
	ClientSuccessRateRealtimeDTO dataOfToday(Integer agentId,String clientIds);

	/**
	 * @Description: 获取一周提交量曲线图数据
	 * @Author: tanjiangqiang
	 * @Date: 2017/12/12 - 17:04
	 * @param agentId 代理商id
	 * @param clientid 子客户id
	 *
	 */
	Map<String, Integer> queryWeekSubmitNumber(String agentId, String clientid);
}
