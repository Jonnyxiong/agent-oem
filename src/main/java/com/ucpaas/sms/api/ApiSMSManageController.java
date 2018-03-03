package com.ucpaas.sms.api;

import com.google.common.collect.Maps;
import com.jsmsframework.common.dto.ResultVO;
import com.jsmsframework.common.util.JsonUtil;
import com.jsmsframework.order.product.exception.JsmsOEMAgentOrderProductException;
import com.ucpaas.sms.common.entity.PageContainer;
import com.ucpaas.sms.common.entity.R;
import com.ucpaas.sms.dto.PurchaseOrderVO;
import com.ucpaas.sms.model.AgentInfo;
import com.ucpaas.sms.service.common.AgentIndexService;
import com.ucpaas.sms.service.sms.SmsManageService;
import com.ucpaas.sms.service.util.AgentUtils;
import com.ucpaas.sms.util.StringUtils;
import com.ucpaas.sms.util.web.AuthorityUtils;
import com.ucpaas.sms.util.web.ControllerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by lpjLiu on 2017/5/31.
 */
@RestController
@RequestMapping("/api/sms")
public class ApiSMSManageController {

	/**
	 * 日志对象
	 */
	protected Logger logger = LoggerFactory.getLogger(ApiSMSManageController.class);

	@Autowired
	private AgentIndexService agentIndexService;

	@Autowired
	private SmsManageService smsManageService;

	/**
	 * 获取国际短信价格列表
	 *
	 */
	@GetMapping("/gj/prices")
	public R getGjPrices(String condition, String pageRowCount, String currentPage, HttpServletRequest request) {
		Map<String, String> params = ControllerUtils.buildQueryMap(pageRowCount, currentPage);
		if (StringUtils.isNotBlank(condition)) {
			params.put("condition", condition);
		}

		PageContainer page = smsManageService.queryInterShortMessagePrice(params);
		return R.ok("获取国际短信价格列表成功", page);
	}

	/**
	 * 获取折扣信息
	 */
	@GetMapping("/data")
	public R getData(HttpServletRequest request) {
		Map<String, Object> data = Maps.newHashMap();

		// 获取代理商id及鉴权状态
		AgentInfo agentInfo = AgentUtils.queryAgentInfoByAdminId(AuthorityUtils.getLoginUserId(request).toString());
		String agent_id = agentInfo.getAgentId();

		// 获取产品（行业、营销、国际）折扣率
		Map<String, String> params_agent_id = Maps.newHashMap();
		params_agent_id.put("agent_id", agent_id);

		Map<String, Object> disPro = smsManageService.queryDiscountRateForProduct(params_agent_id);
		data.putAll(disPro);

		// 获取产品（国际产品）全局折扣率
		Map<String, String> params2 = Maps.newHashMap();
		params2.put("param_key", "OEM_GJ_SMS_DISCOUNT");
		Map<String, Object> disOver = smsManageService.queryDiscountRateForOverall(params2);
		data.putAll(disOver);

		// 获取可以使用的返点余额
		Map<String, Object> rebate_income_map = smsManageService.queryRebateBalance(params_agent_id);

		// 获取可以使用的返点折扣率
		Map<String, Object> rebate_use_radio_map = smsManageService.queryOffsetRebateRate(params_agent_id);

		BigDecimal rebate_income = new BigDecimal(rebate_income_map.get("rebate_income").toString());
		BigDecimal rebate_use_radio = new BigDecimal(rebate_use_radio_map.get("rebate_use_radio").toString());
		BigDecimal rebate_useable = rebate_income.multiply(rebate_use_radio);

		if (rebate_useable.compareTo(BigDecimal.ZERO) == 0) {
			data.put("rebate_useable", 0);
		} else {
			data.put("rebate_useable", rebate_useable.toString());
		}

		// 设置认证状态
		data.put("oauth_status", agentInfo.getOauthStatus());

		return R.ok("获取代理商折扣信息成功", data);
	}

	/**
	 * 获取产品列表
	 *
	 */
	@GetMapping("/products")
	public R getProductsBak(String pageRowCount, String currentPage, HttpServletRequest request) {
		Map<String, String> params = ControllerUtils.buildQueryMap(pageRowCount, currentPage);
		// 查询产品列表
		PageContainer page = smsManageService.querySmsPurchaseProduct(params);
		return R.ok("获取产品列表成功", page);
	}
	/**
	 * 获取产品列表
	 *
	 */
	@PostMapping("/products")
//	public R getProducts(String pageRowCount, String currentPage,HttpServletRequest request) {
	public R getProducts(@RequestParam Map params,HttpServletRequest request) {
		/*Map<String, String> params = ControllerUtils.buildQueryMap(pageRowCount, currentPage);*/
		// 查询产品列表
		AgentInfo agentInfo = AgentUtils.queryAgentInfoByAdminId(AuthorityUtils.getLoginUserId(request).toString());

		params.put("agentId", agentInfo.getAgentId());
		params.put("orderByClause", "price desc,gjdiscount desc,create_time DESC");
		PageContainer page = smsManageService.querySmsPurchaseProduct(params);
		return R.ok("获取产品列表成功", page);
	}

	/**
	 * 添加订单
	 * 
	 * @param product_id
	 * @param num
	 */
	@PostMapping("/order/add")
	public R addOrder(String product_id, String num, HttpServletRequest request) {
		R r;
		try {
			String adminId = AuthorityUtils.getLoginUserId(request).toString();
			AgentInfo agentInfo = AgentUtils.queryAgentInfoByAdminId(adminId);
			if (!"3".equals(agentInfo.getOauthStatus())) {
				r = R.error("代理商还未认证！");
				return r;
			}

			Map<String, String> params = Maps.newHashMap();
			params.put("product_id", product_id);
			params.put("num", num);
			params.put("admin_id", adminId); // 获取用户登录的id
			params.put("agent_id", agentInfo.getAgentId());

			Map data = smsManageService.confirmSubmitOrder(params);
			r = "success".equals(data.get("result").toString()) ? R.ok(data.get("msg").toString())
					: R.error(data.get("msg").toString());
		} catch (Exception e) {
			logger.error("购买产品包失败\n product_id{} 消息{}", product_id, e);
			r = R.error("服务器异常,正在检修中...");
		}

		return r;
	}

	/**
	 * 添加订单
	 */
	@PostMapping("/order/jsmsAdd")
	public ResultVO addJsmsOrder(@RequestBody List<PurchaseOrderVO> purchaseOrderList, HttpServletRequest request) {
		ResultVO result = null;
		try {
			Iterator<PurchaseOrderVO> purchaseOrderVOIterator =  purchaseOrderList.iterator();
			//删除购买数量小于1的产品
			while (purchaseOrderVOIterator.hasNext()) {
				PurchaseOrderVO purchaseOrderVO = purchaseOrderVOIterator.next();
				if (purchaseOrderVO.getPurchaseNum().compareTo(BigDecimal.ZERO) < 1) {
					purchaseOrderVOIterator.remove();
				}
			}
			if (purchaseOrderList.isEmpty()) {
				logger.error("购买的产品和数量为空" + JsonUtil.toJson(purchaseOrderList));
				return ResultVO.failure("请选择要购买的产品");
			}

			Long adminId = AuthorityUtils.getLoginUserId(request);

			result = smsManageService.confirmPurchaseOrder(purchaseOrderList,adminId);

		} catch (JsmsOEMAgentOrderProductException e) {
			logger.debug("购买订单失败:{} ----------------------------> {}",e.getMessage(),e);
			return ResultVO.failure(e.getMessage());
		}catch (Exception e) {
			logger.error("购买产品包失败:{} , purchaseOrder --> {},----------------------------> {}",e.getMessage(), JsonUtil.toJson(purchaseOrderList),e);
			return ResultVO.failure("服务器异常,正在检修中...");
		}
		return result;
	}

}
