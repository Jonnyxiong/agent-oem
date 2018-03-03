package com.ucpaas.sms.service.finance;

import com.jsmsframework.common.enums.ProductType;
import com.jsmsframework.order.entity.JsmsOemAgentOrder;
import com.jsmsframework.order.enums.OEMAgentOrderType;
import com.jsmsframework.order.service.JsmsOemAgentOrderService;
import com.ucpaas.sms.common.entity.PageContainer;
import com.ucpaas.sms.dao.MessageMasterDao;
import com.ucpaas.sms.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class FinanceManageServiceImpl implements FinanceManageService {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(FinanceManageServiceImpl.class);

	@Autowired
	private MessageMasterDao masterDao;
	@Autowired
	private JsmsOemAgentOrderService jsmsOemAgentOrderService;

	@Override
	public PageContainer queryAgentBalanceBillList(Map<String, String> params, String realName, String adminId) {
		PageContainer pageContainer = masterDao.getSearchPage("finance.queryAgentBalanceBillList",
				"finance.queryAgentBalanceBillListCount", params);
		dealAgentBalanceBillAdminId(realName, adminId, pageContainer.getList());
		return pageContainer;
	}

	@Override
	public List<Map<String, Object>> queryAgentBalanceBillListForAll(Map<String, String> params, String realName,
																	 String adminId) {
		List<Map<String, Object>> list = masterDao.getSearchList("finance.queryAgentBalanceBillListForAll", params);
		dealAgentBalanceBillAdminId(realName, adminId, list);
		return list;
	}

	private void dealAgentBalanceBillAdminId(String realName, String adminId, List<Map<String, Object>> list) {
		if (null != list && list.size() > 0) {
			for (Map<String, Object> map : list) {
				Object obj = map.get("admin_id");
				if (obj == null) {
					continue;
				}

				String admin_id = obj.toString();
				if (StringUtils.isBlank(admin_id)) {
					continue;
				}

				if (adminId.equals(admin_id)) {
					map.put("admin_name", realName);
				} else {
					map.put("admin_name", "系统");
				}
			}
		}
	}

	@Override
	public PageContainer queryAgentRebateBillList(Map<String, String> params) {
		return masterDao.getSearchPage("finance.queryAgentRebateBillList", "finance.queryAgentRebateBillListCount",
				params);
	}

	@Override
	public List<Map<String, Object>> queryAgentRebateBillListForAll(Map<String, String> params) {
		return masterDao.getSearchList("finance.queryAgentRebateBillListForAll", params);
	}

	@Override
	public PageContainer queryAgentOrderInfoList(Map<String, String> params) {
		return masterDao.getSearchPage("finance.queryAgentOrderInfoList", "finance.queryAgentOrderInfoListCount",
				params);
	}

	@Override
	public List<Map<String, Object>> queryAgentOrderInfoListForAll(Map<String, String> params) {
		return masterDao.getSearchList("finance.queryAgentOrderInfoListForAll", params);
	}

	@Override
	public PageContainer queryOemAgentPoolList(Map<String, String> params) {
		return masterDao.getSearchPage("finance.queryOemAgentPoolList", "finance.queryOemAgentPoolListCount", params);
	}

	@Override
	public PageContainer queryOemClientPoolList(Map<String, String> params) {
		return masterDao.getSearchPage("finance.queryOemClientPoolList", "finance.queryOemClientPoolListCount", params);
	}

	@Override
	public Map<String, Object> queryAgentAccountInfo(Map<String, String> params) {
		return masterDao.getOneInfo("finance.queryAgentAccountInfo", params);
	}

	@Override
	public Map<String, Object> queryAgentAccountStatistics(Map<String, String> params) {
		Map<String, Object> data = masterDao.getOneInfo("finance.queryAgentAccountStatistics", params);
		List<JsmsOemAgentOrder> oemAgentOrders = jsmsOemAgentOrderService.getSumByOrderType(OEMAgentOrderType.OEM代理商回退, Integer.parseInt(params.get("agent_id")));
		if (data == null)
			data = new HashMap<>();
		if (data.get("hy_total_purchase_number") == null)
			data.put("hy_total_purchase_number", 0);
		if (data.get("hy_remain_rebate_number") == null)
			data.put("hy_remain_rebate_number", 0);

		if (data.get("yx_total_purchase_number") == null)
			data.put("yx_total_purchase_number", 0);
		if (data.get("yx_remain_rebate_number") == null)
			data.put("yx_remain_rebate_number", BigDecimal.ZERO.setScale(2,BigDecimal.ROUND_DOWN));

		if (data.get("gj_total_purchase_amount") == null)
			data.put("gj_total_purchase_amount", 0);
		if (data.get("gj_remain_rebate_amount") == null)
			data.put("gj_remain_rebate_amount", 0);

		if (data.get("yzm_total_purchase_number") == null)
			data.put("yzm_total_purchase_number", 0);
		if (data.get("yzm_remain_rebate_number") == null)
			data.put("yzm_remain_rebate_number", 0);

		if (data.get("tz_total_purchase_number") == null)
			data.put("tz_total_purchase_number", 0);
		if (data.get("tz_remain_rebate_number") == null)
			data.put("tz_remain_rebate_number", 0);

		/**
		 * 卖出数量 - 回退数量
		 * 国际不能回退
		 */
		for (JsmsOemAgentOrder oemAgentOrder : oemAgentOrders) {

			Integer orderNumber = oemAgentOrder.getOrderNumber() == null ? 0 :oemAgentOrder.getOrderNumber();
			if(ProductType.行业.getValue().equals(oemAgentOrder.getProductType())){
				data.put("hy_remain_rebate_number", (Integer)data.get("hy_remain_rebate_number") - orderNumber);
			}else if(ProductType.营销.getValue().equals(oemAgentOrder.getProductType())){
				data.put("yx_remain_rebate_number", (Integer)data.get("yx_remain_rebate_number") - orderNumber);
			}else if(ProductType.验证码.getValue().equals(oemAgentOrder.getProductType())){
				data.put("yzm_remain_rebate_number", (Integer)data.get("yzm_remain_rebate_number") - orderNumber);
			}else if(ProductType.通知.getValue().equals(oemAgentOrder.getProductType())){
				data.put("tz_remain_rebate_number", (Integer)data.get("tz_remain_rebate_number") - orderNumber);
			}


		}

		return data;
	}

	@Override
	public Map<String, Object> queryAgentPoolRemainNum(Map<String, String> params) {
		Map<String, Object> agentPoolRemainNumData =  masterDao.getOneInfo("finance.queryAgentPoolRemainNum", params);
		if (agentPoolRemainNumData == null)
			agentPoolRemainNumData = new HashMap<>();
		if (agentPoolRemainNumData.get("hy_remain_num") == null)
			agentPoolRemainNumData.put("hy_remain_num", 0);
		if (agentPoolRemainNumData.get("yx_remain_num") == null)
			agentPoolRemainNumData.put("yx_remain_num", 0);
		if (agentPoolRemainNumData.get("yzm_remain_num") == null)
			agentPoolRemainNumData.put("yzm_remain_num", 0);
		if (agentPoolRemainNumData.get("tz_remain_num") == null)
			agentPoolRemainNumData.put("tz_remain_num", 0);
		if (agentPoolRemainNumData.get("gj_remain_amount") == null)
			agentPoolRemainNumData.put("gj_remain_amount", BigDecimal.ZERO.setScale(0,BigDecimal.ROUND_DOWN));
		return agentPoolRemainNumData;
	}

	@Override
	public Map<String, Object> queryAgentPoolRemainNumFordueTime(Map<String, String> params) {
		Map<String, Object> agent_pool_due_time_data = masterDao.getOneInfo("finance.queryAgentPoolRemainNumFordueTime", params);
		if (agent_pool_due_time_data == null)
			agent_pool_due_time_data = new HashMap<>();
		if (agent_pool_due_time_data.get("hy_remain_num") == null)
			agent_pool_due_time_data.put("hy_remain_num", 0);
		if (agent_pool_due_time_data.get("yx_remain_num") == null)
			agent_pool_due_time_data.put("yx_remain_num", 0);
		if (agent_pool_due_time_data.get("yzm_remain_num") == null)
			agent_pool_due_time_data.put("yzm_remain_num", 0);
		if (agent_pool_due_time_data.get("tz_remain_num") == null)
			agent_pool_due_time_data.put("tz_remain_num", 0);
		if (agent_pool_due_time_data.get("gj_remain_amount") == null)
			agent_pool_due_time_data.put("gj_remain_amount", BigDecimal.ZERO.setScale(0,BigDecimal.ROUND_DOWN));

		return agent_pool_due_time_data;
	}

	/**
	 * @param params
	 * @return
	 * @Title: queryCustomerForBalanceDetails
	 * @Description:查询OEM代理商所有短信产品区分过期与否
	 * @return: Map<String,Object>
	 */
	@Override
	public List<Map<String, Object>> queryCustomerForBalanceDetails(Map params) {



		return masterDao.getSearchList("finance.queryCustomerForBalanceDetails", params);
	}

	@Override
	public Map<String, Object> queryCustomerForBalanceDetailTotal(Map params) {
		return masterDao.getOneInfo("finance.queryCustomerForBalanceDetailTotal", params);
	}

	@Override
	public PageContainer querypurchaseHistoryList(Map<String, String> params) {
		return masterDao.getSearchPage("finance.querypurchaseHistoryList", "finance.querypurchaseHistoryListCount", params);
	}

	@Override
	public List<Map<String, Object>> querypurchaseHistoryForAll(Map<String, String> params) {
		return masterDao.getSearchList("finance.querypurchaseHistoryForAll", params);
	}
}
