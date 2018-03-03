package com.ucpaas.sms.service.sms;

import com.jsmsframework.common.dto.ResultVO;
import com.ucpaas.sms.common.entity.PageContainer;
import com.ucpaas.sms.dto.PurchaseOrderVO;

import java.util.List;
import java.util.Map;

public interface SmsManageService {

	/**
	 * @Title: queryInterShortMessagePrice
	 * @Description: 查询国际短信价格
	 * @param params
	 * @return
	 * @return: PageContainer
	 */
	PageContainer queryInterShortMessagePrice(Map<String, String> params);

	/**
	 * @Title: querySmsPurchaseProduct
	 * @Description: 查询产品信息
	 * @param params
	 * @return
	 * @return: PageContainer
	 */
	PageContainer querySmsPurchaseProduct(Map<String, String> params);

	/**
	 * @Title: queryDiscountRateForProduct
	 * @Description: 从t_sms_oem_data_config，查询（行业、营销、国际）折扣率
	 * @param params
	 * @return
	 * @return: Map<String,Object>
	 */
	Map<String, Object> queryDiscountRateForProduct(Map<String, String> params);

	/**
	 * @Title: queryDiscountRateForOverall
	 * @Description: 从表t_sms_agent_client_param,查询国际产品的全局折扣率
	 * @return
	 * @return: Map<String,Object>
	 */
	Map<String, Object> queryDiscountRateForOverall(Map<String, String> params);

	/**
	 * @Title: queryRebateBalance
	 * @Description: 查询返点余额
	 * @param params
	 * @return
	 * @return: Map<String,Object>
	 */
	Map<String, Object> queryRebateBalance(Map<String, String> params);

	/**
	 * @Title: queryOffsetRebateRate
	 * @Description: 查询抵消的返点比例
	 * @param params
	 * @return
	 * @return: Map<String,Object>
	 */
	Map<String, Object> queryOffsetRebateRate(Map<String, String> params);

	/**
	 * @Title: queryAgentAccount
	 * @Description: 查询代理商的账号信息
	 * @param params
	 * @return
	 * @return: Map<String,Object>
	 */
	Map<String, Object> queryAgentAccount(Map<String, String> params);

	/**
	 * @Title: confirmSubmitOrder
	 * @Description: 确认下单
	 * @param params
	 *            产品id、产品数量、产品类型
	 * @return
	 * @return: Map<String,Object>
	 */
	Map<String, Object> confirmSubmitOrder(Map<String, String> params);
	ResultVO confirmPurchaseOrder(List<PurchaseOrderVO> purchaseOrderList, Long adminId);

	/**
	 * @Title: getTheMostNumForMinute
	 * @Description: 获取订单前缀
	 * @param orderIdPre
	 * @return
	 * @return: String
	 */
	String getTheMostNumForMinute(String orderIdPre);

}
