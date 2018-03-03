package com.ucpaas.sms.service.finance;

import java.util.List;
import java.util.Map;

import com.ucpaas.sms.common.entity.PageContainer;

public interface FinanceManageService {
	
	
	/** 
	 * @Title: queryAgentBalanceBillList 
	 * @Description: 查询余额账单列表
	 * @param params
	 * @return
	 * @return: PageContainer
	 */
	PageContainer queryAgentBalanceBillList(Map<String, String> params,  String realName, String adminId);
	
	
	/** 
	 * @Title: queryAgentBalanceBillListForAll 
	 * @Description: 导出余额报表
	 * @param params
	 * @return
	 * @return: List<Map<String,Object>>
	 */
	List<Map<String, Object>> queryAgentBalanceBillListForAll(Map<String, String> params, String realName, String adminId);

	/** 
	 * @Title: queryRebateBillList 
	 * @Description: 查询返点账单列表
	 * @param params
	 * @return
	 * @return: PageContainer
	 */
	PageContainer queryAgentRebateBillList(Map<String, String> params);
	
	
	/** 
	 * @Title: queryAgentRebateBillListForAll 
	 * @Description: 导出返点账单列表
	 * @param params
	 * @return
	 * @return: List<Map<String,Object>>
	 */
	List<Map<String, Object>> queryAgentRebateBillListForAll(Map<String, String> params);
	
	
	/** 
	 * @Title: queryAgentOrderInfoList 
	 * @Description: 查询代理商订单列表
	 * @param params
	 * @return
	 * @return: PageContainer
	 */
	PageContainer queryAgentOrderInfoList(Map<String, String> params);
	
	/** 
	 * @Title: queryAgentOrderInfoListForAll 
	 * @Description: 导出短信订单列表
	 * @param params
	 * @return
	 * @return: List<Map<String,Object>>
	 */
	List<Map<String, Object>> queryAgentOrderInfoListForAll(Map<String, String> params);
	
	
	/** 
	 * @Title: queryOemAgentPoolList 
	 * @Description: 查询代理商短信池列表
	 * @param params
	 * @return
	 * @return: PageContainer
	 */
	PageContainer queryOemAgentPoolList(Map<String, String> params);
	
	
	/** 
	 * @Title: queryOemClientPoolList 
	 * @Description: 查询客户短信池列表
	 * @param params
	 * @return
	 * @return: PageContainer
	 */
	PageContainer queryOemClientPoolList(Map<String, String> params);
	
	
	/** 
	 * @Title: queryAgentAccountInfo 
	 * @Description: 查询代理商账户信息
	 * @param params
	 * @return
	 * @return: Map<String,Object>
	 */
	Map<String,Object> queryAgentAccountInfo(Map<String, String> params);
	
	
	
	/** 
	 * @Title: queryAgentAccountStatistics 
	 * @Description: 查询代理商账户统计表
	 * @param params
	 * @return
	 * @return: Map<String,Object>
	 */
	Map<String,Object> queryAgentAccountStatistics(Map<String, String> params);
	
	
	/** 
	 * @Title: queryAgentPoolRemainNum 
	 * @Description: 查询代理商，行业短信剩余数量、营销短信剩余数量、国际短信剩余数量
	 * @param params
	 * @return
	 * @return: Map<String,Object>
	 */
	Map<String,Object> queryAgentPoolRemainNum(Map<String, String> params);
	
	
	/** 
	 * @Title: queryAgentPoolRemainNumFordueTime 
	 * @Description: 查询代理商即将到期的剩余数量
	 * @param params
	 * @return
	 * @return: Map<String,Object>
	 */
	Map<String,Object> queryAgentPoolRemainNumFordueTime(Map<String, String> params);

	/**
	 * @Title: queryCustomerForBalanceDetails
	 * @Description:查询OEM代理商所有短信产品区分过期与否
	 * @param params
	 * @return
	 * @return: Map<String,Object>
	 */
	List<Map<String, Object>> queryCustomerForBalanceDetails(Map params);


	Map<String, Object> queryCustomerForBalanceDetailTotal(Map params);

	/**
	 * @Title: querypurchaseHistoryList
	 * @Description: 查询客户消费记录列表
	 * @param params
	 * @return
	 * @return: PageContainer
	 */
	PageContainer querypurchaseHistoryList(Map<String, String> params);

	/**
	 * @Title: querypurchaseHistoryForAll
	 * @Description: 导出客户消费记录列表
	 * @param params
	 * @return
	 * @return: List<Map<String,Object>>
	 */
	List<Map<String, Object>> querypurchaseHistoryForAll(Map<String, String> params);
	
	
	
	
	
}
