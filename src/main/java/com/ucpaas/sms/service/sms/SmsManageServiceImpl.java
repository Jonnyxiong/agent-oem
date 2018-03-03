package com.ucpaas.sms.service.sms;

import com.jsmsframework.common.dto.JsmsPage;
import com.jsmsframework.common.dto.ResultVO;
import com.jsmsframework.common.entity.JsmsAgentClientParam;
import com.jsmsframework.common.enums.ProductType;
import com.jsmsframework.common.service.JsmsAgentClientParamService;
import com.jsmsframework.common.util.BeanUtil;
import com.jsmsframework.order.entity.JsmsOemAgentOrder;
import com.jsmsframework.order.product.service.JsmsOEMAgentOrderProductService;
import com.jsmsframework.product.entity.JsmsOemAgentProduct;
import com.jsmsframework.product.service.JsmsOemAgentProductService;
import com.jsmsframework.product.service.JsmsOemProductInfoService;
import com.jsmsframework.user.entity.JsmsAgentInfo;
import com.jsmsframework.user.service.JsmsAgentInfoService;
import com.ucpaas.sms.common.entity.PageContainer;
import com.ucpaas.sms.dao.MessageMasterDao;
import com.ucpaas.sms.dto.OemProductInfoVO;
import com.ucpaas.sms.dto.PurchaseOrderVO;
import com.ucpaas.sms.service.util.OrderUtils;
import com.ucpaas.sms.util.DateUtils;
import com.ucpaas.sms.util.PageConvertUtil;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@Transactional
public class SmsManageServiceImpl implements SmsManageService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SmsManageServiceImpl.class);

	@Autowired
	private MessageMasterDao masterDao;

	@Autowired
	private JsmsOemProductInfoService jsmsOemProductInfoService;

	@Autowired
	private JsmsAgentClientParamService jsmsAgentClientParamService;
	@Autowired
	private JsmsAgentInfoService jsmsAgentInfoService;
	@Autowired
	private JsmsOEMAgentOrderProductService jsmsOEMAgentOrderProductService;
	@Autowired
	private JsmsOemAgentProductService jsmsOemAgentProductService;

	@Override
	public PageContainer queryInterShortMessagePrice(Map<String, String> params) {
		return masterDao.getSearchPage("sms.queryInterShortMessagePrice",
				"sms.queryInterShortMessagePriceCount", params);
	}

	@Override
	public PageContainer querySmsPurchaseProduct(Map<String, String> params) {
        params.put("status","1");
        params.put("isShow","1");
        params.put("dueTimeAfter", DateTime.now().toString("yyyy-MM-dd HH:mm:ss"));
        JsmsPage jsmsPage = PageConvertUtil.paramToPage(params);
        jsmsOemProductInfoService.queryList(jsmsPage);
        List<OemProductInfoVO> result = new ArrayList();
        int rowNum = 1;
        int flag =0;
        JsmsAgentClientParam oem_gj_sms_discount = jsmsAgentClientParamService.getByParamKey("OEM_GJ_SMS_DISCOUNT");

		JsmsOemAgentProduct tempParam = new JsmsOemAgentProduct();

        for ( Object object : jsmsPage.getData()) {
			flag+=1;
            OemProductInfoVO temp = new OemProductInfoVO();
            BeanUtil.copyProperties(object,temp);
			temp.setRowNum((jsmsPage.getPage() - 1)*jsmsPage.getRows() + rowNum);
            temp.setOEM_GJ_SMS_DISCOUNT(oem_gj_sms_discount.getParamValue());
			tempParam.setAgentId(Integer.parseInt(params.get("agentId")));
			tempParam.setProductId(temp.getProductId());
			JsmsOemAgentProduct oemAgentProduct = jsmsOemAgentProductService.getByAgentIdAndProductId(tempParam);
			if(oemAgentProduct != null){
				temp.setDiscountPrice(oemAgentProduct.getDiscountPrice());
				temp.setGjSmsDiscount(oemAgentProduct.getGjSmsDiscount());
				if(temp.getProductType().intValue()== ProductType.国际.getValue().intValue()){//国际
					if(temp.getGjSmsDiscount().compareTo(new BigDecimal(1))!=0){
						temp.setFlag("1");
						result.add(temp);
					}
				}else{//非国际
					if(temp.getDiscountPrice().compareTo(temp.getUnitPrice())!=0){
						temp.setFlag("1");
						result.add(temp);
					}
				}

				++rowNum;
			}
		}
		if(flag==jsmsPage.getData().size()){
			for ( Object object : jsmsPage.getData()) {
				OemProductInfoVO temp = new OemProductInfoVO();
				BeanUtil.copyProperties(object,temp);
				temp.setRowNum((jsmsPage.getPage() - 1)*jsmsPage.getRows() + rowNum);
				temp.setOEM_GJ_SMS_DISCOUNT(oem_gj_sms_discount.getParamValue());
				tempParam.setAgentId(Integer.parseInt(params.get("agentId")));
				tempParam.setProductId(temp.getProductId());
				JsmsOemAgentProduct oemAgentProduct = jsmsOemAgentProductService.getByAgentIdAndProductId(tempParam);
				if(oemAgentProduct == null){
					temp.setFlag("0");
					result.add(temp);
					++rowNum;
				}else{
					temp.setDiscountPrice(oemAgentProduct.getDiscountPrice());
					temp.setGjSmsDiscount(oemAgentProduct.getGjSmsDiscount());
					if(temp.getProductType().intValue()==ProductType.国际.getValue().intValue()){//国际
						if(temp.getGjSmsDiscount().compareTo(new BigDecimal(1))==0){
							temp.setFlag("0");
							result.add(temp);
						}
					}else{//非国际
						if(temp.getDiscountPrice().compareTo(temp.getUnitPrice())==0){
							temp.setFlag("0");
							result.add(temp);
						}
					}
					++rowNum;
				}
			}
		}
		jsmsPage.setData(result);
        return PageConvertUtil.pageToContainer(jsmsPage);

        /*return masterDao.getSearchPage("sms.querySmsPurchaseProduct", "sms.querySmsPurchaseProductCount",
				params);*/
	}

	@Override
	public Map<String, Object> queryDiscountRateForProduct(Map<String, String> params) {
		Map<String, Object> map = masterDao.getOneInfo("sms.queryDiscountRateForProduct", params);
		if (map == null)
			map = new HashMap<>();
		// 行业短信折扣率
		if (map.get("hy_sms_discount") == null)
			map.put("hy_sms_discount", 1);
		// 营销短信折扣率
		if (map.get("yx_sms_discount") == null)
			map.put("yx_sms_discount", 1);
		// 国际短信折扣率
		if (map.get("gj_sms_discount") == null)
			map.put("gj_sms_discount", 1);

		return map;
	}

	@Override
	public Map<String, Object> queryDiscountRateForOverall(Map<String, String> params) {
		Map<String, Object> map = masterDao.getOneInfo("sms.queryDiscountRateForOverall", params);
		if (map == null)
			map = new HashMap<>();
		if (map.get("param_value") == null) {
			map.put("over_discount", 1);
		} else {
			map.put("over_discount", map.get("param_value"));
		}
		return map;
	}

	@Override
	public Map<String, Object> queryRebateBalance(Map<String, String> params) {
		Map<String, Object> map = masterDao.getOneInfo("sms.queryRebateBalance", params);
		if (map == null)
			map = new HashMap<>();
		if (map.get("rebate_income") == null)
			map.put("rebate_income", 0);
		return map;
	}

	@Override
	public Map<String, Object> queryOffsetRebateRate(Map<String, String> params) {
		Map<String, Object> map = masterDao.getOneInfo("sms.queryOffsetRebateRate", params);
		if (map == null)
			map = new HashMap<>();
		if (map.get("rebate_use_radio") == null)
			map.put("rebate_use_radio", 0);
		return map;
	}

	@Override
	public Map<String, Object> queryAgentAccount(Map<String, String> params) {
		Map<String, Object> map = masterDao.getOneInfo("sms.queryAgentAccount", params);
		if (map == null)
			map = new HashMap<>();
		if (map.get("balance") == null)
			map.put("balance", 0);
		if (map.get("credit_balance") == null)
			map.put("credit_balance", 0);
		if (map.get("accumulated_consume") == null)
			map.put("accumulated_consume", 0);
		if (map.get("accumulated_rebate_pay") == null)
			map.put("accumulated_rebate_pay", 0);
		return map;
	}

	@Override
    @Transactional
	public ResultVO confirmPurchaseOrder(List<PurchaseOrderVO> purchaseOrderList, Long adminId) {

        JsmsAgentInfo agentInfo = jsmsAgentInfoService.getByAdminId(adminId);
        if(agentInfo == null && !agentInfo.getOauthStatus().equals(3)){
            return ResultVO.failure("代理商尚未认证！请先认证...");
        }

        JsmsOemAgentOrder jsmsOemAgentOrder = new JsmsOemAgentOrder();
        Long orderId = OrderUtils.getAgentOrderId();
        jsmsOemAgentOrder.setOrderId(orderId);
        jsmsOemAgentOrder.setOrderNo(orderId);
        boolean newOrderId = false;

        ResultVO resultVO = null;
        for (PurchaseOrderVO purchaseOrder : purchaseOrderList) {
            if(newOrderId){
                jsmsOemAgentOrder.setOrderId(OrderUtils.getAgentOrderId());
            }
            newOrderId = true;
            resultVO = jsmsOEMAgentOrderProductService.purchaseOrder(agentInfo.getAgentId(),agentInfo.getRebateUseRadio(), purchaseOrder.getProductId(), purchaseOrder.getPurchaseNum(), adminId, jsmsOemAgentOrder);
        }

        return resultVO;
    }
    @Override
	public Map<String, Object> confirmSubmitOrder(Map<String, String> params) {

		LOGGER.debug("oem代理商平台-短信购买-确认下单开始======================================");

		Map<String, Object> data = new HashMap<>();

		String product_id = params.get("product_id");
		String agent_id = params.get("agent_id");
		BigDecimal num = new BigDecimal(params.get("num").toString());



		JsmsOemAgentProduct tempParam = new JsmsOemAgentProduct();

		tempParam.setAgentId(Integer.valueOf(agent_id));
		tempParam.setProductId(Integer.valueOf(product_id));
		JsmsOemAgentProduct oemAgentProduct = jsmsOemAgentProductService.getByAgentIdAndProductId(tempParam);

		Map<String, Object> productInfo = masterDao.getOneInfo("sms.queryOemProductInfo", params);


		String product_type = productInfo.get("product_type").toString();
		if (!"2".equals(product_type)) {
			if(oemAgentProduct!=null)
				productInfo.put("unit_price",oemAgentProduct.getDiscountPrice());
		}



		// Add by lpjLiu 2017-06-05 检查订单是否已经到期，到期的不可继续购买
		Date due_time = DateUtils.parseDate(productInfo.get("due_time"));
		Date date = new Date();
		if (date.getTime() > due_time.getTime())
		{
			data.put("result", "fail");
			data.put("msg", "产品已过期!");
			return data;
		}

		// 求出账号余额和授信额度
		Map<String, Object> agentAccount = this.queryAgentAccount(params);
		BigDecimal balance = new BigDecimal(agentAccount.get("balance").toString());
		BigDecimal credit_balance = new BigDecimal(agentAccount.get("credit_balance").toString());

		// 求出可以使用的返点余额
		Map<String, Object> rebate_balance_map = this.queryRebateBalance(params);
		// 返点金额
		BigDecimal rebate_income = new BigDecimal(rebate_balance_map.get("rebate_income").toString());
		Map<String, Object> rebate_rate_map = this.queryOffsetRebateRate(params);

		// 抵扣的返点比例
		BigDecimal rebate_use_radio = new BigDecimal(rebate_rate_map.get("rebate_use_radio").toString());

		// 使用的返点金额
		BigDecimal rebate_useable = rebate_income.multiply(rebate_use_radio);

		// 是否使用返点的标识
		StringBuffer use_rebat_amount_flag = new StringBuffer("");

		// 订单金额 = 折扣价*数量(折扣之后总价)
		BigDecimal order_amount = null;
		StringBuffer order_amount_sb = new StringBuffer("");

		// 订单使用了余额多少钱
		BigDecimal use_balance = null;
		StringBuffer use_balance_sb = new StringBuffer("");

		// 检查订单
		StringBuffer flag_sb = new StringBuffer("");

		LOGGER.debug("检查订单开始========================================================");
		this.checkOrder(params, data, product_id, agent_id, product_type, num, rebate_useable, use_rebat_amount_flag,
				balance, credit_balance, flag_sb, order_amount_sb, use_balance_sb);
		LOGGER.debug("检查订单结束========================================================");

		use_balance = new BigDecimal(use_balance_sb.toString());
		order_amount = new BigDecimal(order_amount_sb.toString());

		if ("false".equals(flag_sb.toString())) {
			return data;
		}

		System.out.println("-------------------->" + use_balance);

		Date nowDate = new Date();

		// 代理商短信池
		LOGGER.debug("更新代理商短信池开始========================================================");
		// Add by lpjLiu 2017-05-22 短信池ID
		StringBuffer agent_pool_id = new StringBuffer("");

		// Add by lpjLiu 2017-05-23 短信池需要填充单价，并返回池ID
		this.updateOemAgentPool(productInfo, params, nowDate, agent_pool_id);
		LOGGER.debug("更新代理商短信池结束========================================================");

		// 生成订单
		LOGGER.debug("创建订单开始========================================================");
		// 生成规则和原来的一样，只是换了订单的标识4
		StringBuffer order_id = new StringBuffer("");

		// Add by lpjLiu 2017-05-22 普通短信需要设置单价、订单金额，order_no需要填充订单ID字段
		this.createAgentOrder(productInfo, params, order_amount, agent_id, nowDate, order_id, agent_pool_id);
		LOGGER.debug("创建订单结束========================================================");

		// 生成余额账单
		LOGGER.debug("生成余额账单开始========================================================");
		String admin_id = params.get("admin_id").toString();
		// 剩余的余额
		BigDecimal remain_balance = balance.subtract(use_balance);
		this.insertAgentBalanceBill(nowDate, order_id, admin_id, agent_id, use_balance, remain_balance, product_type,
				order_amount);
		LOGGER.debug("生成余额账单结束========================================================");

		// 更新代理商的账户
		// 判断是否使用返点
		if ("no".equals(use_rebat_amount_flag.toString())) {
			rebate_useable = BigDecimal.ZERO;
			LOGGER.debug("该笔订单没有使用返点========================================================");
		} else {
			LOGGER.debug("该笔订单使用返点比例为：=======" + rebate_use_radio);
			LOGGER.debug("该笔订单使用返点金额为=========" + rebate_useable);
		}

		LOGGER.debug("更新账户余额开始========================================================");
		this.updateAgentAccount(agent_id, use_balance, rebate_useable, order_amount, rebate_useable);
		LOGGER.debug("更新账户余额结束========================================================");

		// 剩余的返点金额
		BigDecimal remain_rebat_amount = rebate_income.subtract(rebate_useable);

		// 生成返点账单
		if (rebate_useable.compareTo(BigDecimal.ZERO) == 1) {
			LOGGER.debug("生成返点账单开始========================================================");
			this.insertAgentRebateBill(nowDate, agent_id, order_id, rebate_useable, remain_rebat_amount);
			LOGGER.debug("生成返点账单结束========================================================");
		}

		// 生累计充值记录
		LOGGER.debug("生成累计充值记录开始========================================================");
		this.updateOemAgentAccountStatistics(agent_id, num, product_type);
		LOGGER.debug("生成累计充值记录结束========================================================");

		LOGGER.debug("oem代理商平台-短信购买-确认下单结束======================================");
		data.put("result", "success");
		data.put("msg", "购买成功!");

		return data;
	}

	/**
	 * lpjLiu 更新订单的代理商池ID
	 * 
	 * @param order_id
	 * @param agent_pool_id
	 */
	private void updateOemAgentOrder(StringBuffer order_id, StringBuffer agent_pool_id) {
		// 更新
		Map<String, Object> params = new HashMap<>();
		params.put("order_id", order_id.toString());
		params.put("agent_pool_id", agent_pool_id.toString());
		this.masterDao.update("sms.updateOemAgentOrder", params);
	}

	private void checkOrder(Map<String, String> params, Map<String, Object> data, String product_id, String agent_id,
			String product_type, BigDecimal num, BigDecimal rebate_useable, StringBuffer use_rebat_amount_flag,
			BigDecimal balance, BigDecimal credit_balance, StringBuffer flag, StringBuffer order_amount_sb,
			StringBuffer use_balance_sb) {

		// 余额+授信
		BigDecimal bc = balance.add(credit_balance);

		// 求出折扣之后的价格
		BigDecimal price_discount = this.queryPriceForDiscount(product_id, agent_id);

		// 计算 订单金额 = 折扣价*数量
		order_amount_sb.append(price_discount.multiply(num).toString());

		BigDecimal order_amount_big = new BigDecimal(order_amount_sb.toString());

		// 区分普通产品、国际产品
		BigDecimal remain_price = BigDecimal.ZERO;
		if (!"2".equals(product_type)) { // 普通产品
			// 折扣之后的价格*数量 - 可以使用的返点
			if (order_amount_big.compareTo(new BigDecimal("10")) == 1
					|| order_amount_big.compareTo(new BigDecimal("10")) == 0) {
				remain_price = price_discount.multiply(num).subtract(rebate_useable);
				use_rebat_amount_flag.append("yes");
			} else {
				remain_price = price_discount.multiply(num);
				use_rebat_amount_flag.append("no");
			}
		} else { // 国际产品
			// 折扣之后的价格*数量
			remain_price = price_discount.multiply(num);
			use_rebat_amount_flag.append("no");
		}

		use_balance_sb.append(remain_price.toString());

		if (remain_price.compareTo(bc) == 1) {
			data.put("result", "fail");
			data.put("msg", "您的账户余额不足，请调整购买短信数量或者金额!");
			flag.append("false");
		}
	}

	// 创建订单
	private void createAgentOrder(Map<String, Object> productInfo, Map<String, String> order_params,
			BigDecimal order_amount, String agent_id, Date nowdate, StringBuffer order_id, StringBuffer agent_pool_id) {

		String product_type = productInfo.get("product_type").toString();

		Map<String, Object> agent_order_map = new HashMap<>();

		String order_id_str = OrderUtils.getAgentOrderId().toString();

		order_id = order_id.append(order_id_str);

		agent_order_map.put("order_id", order_id_str);
		agent_order_map.put("order_type", 0); // 0：OEM代理商购买，1：OEM代理商分发，2：OEM代理商回退
		agent_order_map.put("product_id", productInfo.get("product_id"));
		agent_order_map.put("product_code", productInfo.get("product_code"));
		agent_order_map.put("product_type", product_type);

		// Add by lpjLiu 2017-05-23 order_no
		// 需要填充订单ID字段，不区分类型，目前代理商还没有用到这个字段，后面可能会加（一次性购买多个短信、国际短信）
		agent_order_map.put("order_no", order_id_str);

		// Add by lpjLiu 2017-05-23 添加订单的短信池ID
		agent_order_map.put("agent_pool_id", agent_pool_id.toString());

		agent_order_map.put("product_name", productInfo.get("product_name"));
		if (!"2".equals(product_type)) {
			agent_order_map.put("unit_price", productInfo.get("unit_price"));
			agent_order_map.put("order_number", order_params.get("num"));
		} else {
			agent_order_map.put("unit_price", null);
			agent_order_map.put("order_number", null);
		}

		agent_order_map.put("order_amount", order_amount);

		// 国际产品为页面填写的购买价格，普通产品为null
		if ("2".equals(product_type)) {
			agent_order_map.put("product_price", order_params.get("num")); // 国际产品的
		} else {
			agent_order_map.put("product_price", null);
		}

		agent_order_map.put("agent_id", agent_id);
		agent_order_map.put("client_id", "00000"); // 订单类型为1、2时填用户帐号，订单类型为0时填'00000'
		agent_order_map.put("name", "云之讯"); // 订单类型为1、2时填用户名称，订单类型为0时填'云之讯'

		agent_order_map.put("due_time", productInfo.get("due_time"));
		agent_order_map.put("create_time", nowdate);
		agent_order_map.put("remark", null);

		this.masterDao.insert("sms.createOrder", agent_order_map);
	}



	// 获取定的那前缀
	@Override
	public String getTheMostNumForMinute(String orderIdPre) {
		Map<String, Object> sqlParams = new HashMap<>();
		sqlParams.put("orderIdPre", orderIdPre);
		String numStr = masterDao.getOneInfo("sms.getTheMostNumForMinute", sqlParams);
		return numStr;
	}

	// 修改代理商短信池
	private void updateOemAgentPool(Map<String, Object> productInfo, Map<String, String> order_params, Date nowdate,
			StringBuffer agent_pool_id) {
		String product_type = productInfo.get("product_type").toString();

		Map<String, Object> query_params = new HashMap<>();
		query_params.put("agent_id", order_params.get("agent_id"));
		query_params.put("product_type", product_type);
		query_params.put("due_time", productInfo.get("due_time"));

		// Add by lpjLiu 2017-05-23 非国际短信，根据单价去查询
		BigDecimal unit_price = BigDecimal.ZERO;
		if (!"2".equals(product_type)) {
			Object obj = productInfo.get("unit_price");
			if (obj != null) {
				unit_price = new BigDecimal(obj.toString());
			}
			query_params.put("unit_price", unit_price);
		}

		Map<String, Object> oem_agent_pool_map = this.masterDao.getOneInfo("sms.queryOemAgentPool", query_params);
		if (oem_agent_pool_map == null) {
			// 插入
			Map<String, Object> insert_oem_agent_pool_map = new HashMap<>();

			// Add by lpjLiu 2017-05-23 得到自增长的ID字段
			insert_oem_agent_pool_map.put("agent_pool_id", null);
			insert_oem_agent_pool_map.put("agent_id", order_params.get("agent_id"));
			insert_oem_agent_pool_map.put("product_type", product_type);
			insert_oem_agent_pool_map.put("due_time", productInfo.get("due_time"));
			insert_oem_agent_pool_map.put("status", 0); // 状态，0：正常，1：停用
			if (!"2".equals(product_type)) {
				insert_oem_agent_pool_map.put("remain_number", order_params.get("num"));
				insert_oem_agent_pool_map.put("remain_amount", 0);

				// Add by lpjLiu 2017-05-23 增加单价
				insert_oem_agent_pool_map.put("unit_price", unit_price);
			} else {
				insert_oem_agent_pool_map.put("remain_number", 0);
				insert_oem_agent_pool_map.put("remain_amount", order_params.get("num"));// 国际产品，买多少，短信池就增加多少
			}

			insert_oem_agent_pool_map.put("update_time", nowdate);
			insert_oem_agent_pool_map.put("remark", null);

			this.masterDao.insert("sms.insertOemAgentPool", insert_oem_agent_pool_map);

			// Add by lpjLiu 2017-05-23 得到自增长的ID字段
			agent_pool_id.append(insert_oem_agent_pool_map.get("agent_pool_id"));
		} else {
			// 更新
			Map<String, Object> update_oem_agent_pool_map = new HashMap<>();
			update_oem_agent_pool_map.put("agent_pool_id", oem_agent_pool_map.get("agent_pool_id"));
			BigDecimal order_num = new BigDecimal(order_params.get("num"));
			if (!"2".equals(product_type)) {// 非国际产品
				BigDecimal old_number = new BigDecimal(oem_agent_pool_map.get("remain_number").toString());
				update_oem_agent_pool_map.put("remain_number", old_number.add(order_num));
			} else {// 国际产品
				BigDecimal old_amount = new BigDecimal(oem_agent_pool_map.get("remain_amount").toString());

				update_oem_agent_pool_map.put("remain_amount", old_amount.add(order_num));
			}
			update_oem_agent_pool_map.put("update_time", nowdate);
			this.masterDao.update("sms.updateOemAgentPool", update_oem_agent_pool_map);

			// Add by lpjLiu 2017-05-23 得到自增长的ID字段
			agent_pool_id.append(oem_agent_pool_map.get("agent_pool_id"));
		}
	}

	// 生成余额账单
	private void insertAgentBalanceBill(Date nowdate, StringBuffer order_id, String admin_id, String agent_id,
			BigDecimal use_balance, BigDecimal remain_balance, String product_type, BigDecimal order_amount) {

		Map<String, Object> insert_agent_balance_bill_map = new HashMap<>();
		insert_agent_balance_bill_map.put("agent_id", agent_id);
		// 购买产品包都属于3
		insert_agent_balance_bill_map.put("payment_type", 3);// 业务类型，0：充值，1：扣减，2：佣金转余额，3：购买产品包，4：余额转结算
		// 退款，5：赠送
		insert_agent_balance_bill_map.put("financial_type", 1);// '财务类型，0：入账，1：出账',
		insert_agent_balance_bill_map.put("amount", use_balance); // 金额，使用金额
		insert_agent_balance_bill_map.put("balance", remain_balance);// 剩余余额

		insert_agent_balance_bill_map.put("create_time", nowdate);
		insert_agent_balance_bill_map.put("order_id", order_id.toString());
		insert_agent_balance_bill_map.put("admin_id", admin_id);

		// oem 不用填写 client_id
		insert_agent_balance_bill_map.put("client_id", null);

		String remark = null;
		if ("0".equals(product_type)) {
			remark = "购买验证码/通知短信";
		} else if ("1".equals(product_type)) {
			remark = "购买会员营销短信";
		} else {
			remark = "购买国际短信，实际订单价格为：" + order_amount + "元";
		}

		insert_agent_balance_bill_map.put("remark", remark);

		this.masterDao.insert("sms.insertAgentBalanceBill", insert_agent_balance_bill_map);
	}

	// 更新代理商账户表(更新 账户余额、返点余额、累计消费、累计返点支出)
	private void updateAgentAccount(String agent_id, BigDecimal change_remain_balance, BigDecimal change_remain_rebate,
			BigDecimal change_accumulated_consume, BigDecimal change_accumulated_rebate_pay) {

		Map<String, Object> update_agent_account_map = new HashMap<>();
		update_agent_account_map.put("agent_id", agent_id);
		update_agent_account_map.put("balance", change_remain_balance);
		update_agent_account_map.put("rebate_income", change_remain_rebate);
		update_agent_account_map.put("accumulated_consume", change_accumulated_consume); // 累计消费
		update_agent_account_map.put("accumulated_rebate_pay", change_accumulated_rebate_pay); // 累计返点
		this.masterDao.update("sms.updateAgentAccount", update_agent_account_map);
	}

	// 生成返点账单
	private void insertAgentRebateBill(Date nowdate, String agent_id, StringBuffer order_id,
			BigDecimal use_rebat_amount, BigDecimal remain_rebat_amount) {

		Map<String, Object> insert_agent_rebate_bill_map = new HashMap<>();
		insert_agent_rebate_bill_map.put("agent_id", agent_id);
		insert_agent_rebate_bill_map.put("payment_type", 1); // '业务类型,0:返点收入,1:抵扣'
		insert_agent_rebate_bill_map.put("financial_type", 1);// '财务类型,0:入账,1:出账'
		insert_agent_rebate_bill_map.put("order_id", order_id.toString());

		insert_agent_rebate_bill_map.put("amount", use_rebat_amount); // 使用的返点金额
		insert_agent_rebate_bill_map.put("balance", remain_rebat_amount); // 剩余的返点余额
		insert_agent_rebate_bill_map.put("create_time", nowdate);
		insert_agent_rebate_bill_map.put("remark", "购买短信");

		this.masterDao.insert("sms.insertAgentRebateBill", insert_agent_rebate_bill_map);
	}

	// 更新代理商账户统计表
	private void updateOemAgentAccountStatistics(String agent_id, BigDecimal purchase_num, String product_type) {

		Map<String, Object> num_map = this.masterDao.getOneInfo("sms.queryOemAgentAccountStatisticsCountForAgent",
				agent_id);
		int num = Integer.valueOf(num_map.get("num").toString()).intValue();

		if (num == 0) {
			Map<String, Object> insert_agent_account_statistics_map = new HashMap<>();
			insert_agent_account_statistics_map.put("agent_id", agent_id);
			insert_agent_account_statistics_map.put("hy_remain_rebate_number", 0);
			insert_agent_account_statistics_map.put("yx_remain_rebate_number", 0);
			insert_agent_account_statistics_map.put("gj_remain_rebate_amount", 0);

			if ("0".equals(product_type)) {
				insert_agent_account_statistics_map.put("hy_total_purchase_number", purchase_num);
				insert_agent_account_statistics_map.put("yx_total_purchase_number", 0);
				insert_agent_account_statistics_map.put("gj_total_purchase_amount", 0);
			} else if ("1".equals(product_type)) {
				insert_agent_account_statistics_map.put("hy_total_purchase_number", 0);
				insert_agent_account_statistics_map.put("yx_total_purchase_number", purchase_num);
				insert_agent_account_statistics_map.put("gj_total_purchase_amount", 0);
			} else {
				insert_agent_account_statistics_map.put("hy_total_purchase_number", 0);
				insert_agent_account_statistics_map.put("yx_total_purchase_number", 0);
				insert_agent_account_statistics_map.put("gj_total_purchase_amount", purchase_num);
			}
			this.masterDao.insert("sms.insertOemAgentAccountStatistics", insert_agent_account_statistics_map);
		} else {

			Map<String, Object> update_agent_account_statistics_map = new HashMap<>();
			update_agent_account_statistics_map.put("agent_id", agent_id);
			if ("0".equals(product_type)) {
				update_agent_account_statistics_map.put("hy_total_purchase_number", purchase_num);
			} else if ("1".equals(product_type)) {
				update_agent_account_statistics_map.put("yx_total_purchase_number", purchase_num);
			} else {
				update_agent_account_statistics_map.put("gj_total_purchase_amount", purchase_num);
			}

			this.masterDao.update("sms.addOemAgentAccountStatistics", update_agent_account_statistics_map);
		}
	}

	// 获取产品折扣之后的价格(对于普通产品是折后的价格，对于国际产品是 2个折扣率相乘)
	private BigDecimal queryPriceForDiscount(String product_id, String agent_id) {

		BigDecimal price_discount = BigDecimal.ZERO;

		Map<String, Object> productInfo = masterDao.getOneInfo("sms.queryOemProductInfo", product_id);
		String product_type = productInfo.get("product_type").toString();

		Map<String, String> agent_id_map = new HashMap<>();
		agent_id_map.put("agent_id", agent_id);
		Map<String, Object> discount_map = this.queryDiscountRateForProduct(agent_id_map);
		BigDecimal hy_sms_discount = new BigDecimal(discount_map.get("hy_sms_discount").toString());
		BigDecimal yx_sms_discount = new BigDecimal(discount_map.get("yx_sms_discount").toString());
		BigDecimal gj_sms_discount = new BigDecimal(discount_map.get("gj_sms_discount").toString());

		Map<String, String> paramkeyMap = new HashMap<>();
		paramkeyMap.put("param_key", "OEM_GJ_SMS_DISCOUNT");
		Map<String, Object> discount_over_map = this.queryDiscountRateForOverall(paramkeyMap);
		BigDecimal over_discount = new BigDecimal(discount_over_map.get("over_discount").toString());

		BigDecimal unit_price = null;
		if ("0".equals(product_type)) {
			// 行业短信
			unit_price = new BigDecimal(productInfo.get("unit_price").toString());
			price_discount = unit_price.multiply(hy_sms_discount);
		} else if ("1".equals(product_type)) {
			// 营销短信
			unit_price = new BigDecimal(productInfo.get("unit_price").toString());
			price_discount = unit_price.multiply(yx_sms_discount);
		} else {
			// 国际短信
			price_discount = gj_sms_discount.multiply(over_discount);
		}

		return price_discount;
	}

}
