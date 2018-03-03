package com.ucpaas.sms.service.customer;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import com.jsmsframework.common.enums.ClientAlarmType;
import com.jsmsframework.common.enums.ProductType;
import com.jsmsframework.common.util.JsonUtil;
import com.jsmsframework.finance.entity.JsmsClientBalanceAlarm;
import com.jsmsframework.finance.entity.JsmsOemAgentAccountStatistics;
import com.jsmsframework.finance.exception.JsmsClientBalanceAlarmException;
import com.jsmsframework.finance.service.JsmsClientBalanceAlarmService;
import com.jsmsframework.finance.service.JsmsOemAgentAccountStatisticsService;
import com.jsmsframework.order.dto.OemClientRechargeRollBackDTO;
import com.jsmsframework.order.entity.JsmsOemAgentOrder;
import com.jsmsframework.order.entity.JsmsOemAgentPool;
import com.jsmsframework.order.entity.JsmsOemClientOrder;
import com.jsmsframework.order.entity.JsmsOemClientPool;
import com.jsmsframework.order.enums.OEMAgentOrderType;
import com.jsmsframework.order.enums.OrderType;
import com.jsmsframework.order.exception.JsmsOemAgentOrderException;
import com.jsmsframework.order.exception.JsmsOemAgentPoolException;
import com.jsmsframework.order.service.JsmsOemAgentOrderService;
import com.jsmsframework.order.service.JsmsOemAgentPoolService;
import com.jsmsframework.order.service.JsmsOemClientOrderService;
import com.jsmsframework.order.service.JsmsOemClientPoolService;
import com.jsmsframework.user.entity.JsmsAccount;
import com.jsmsframework.user.entity.JsmsClientInfoExt;
import com.jsmsframework.user.entity.JsmsOauthPic;
import com.jsmsframework.user.finance.service.SelfOpenAccountService;
import com.jsmsframework.user.service.JsmsAccountService;
import com.jsmsframework.user.service.JsmsClientInfoExtService;
import com.ucpaas.sms.common.entity.PageContainer;
import com.ucpaas.sms.common.entity.R;
import com.ucpaas.sms.common.util.Collections3;
import com.ucpaas.sms.dao.AccessMasterDao;
import com.ucpaas.sms.dao.MessageMasterDao;
import com.ucpaas.sms.dto.ClientBalanceAlarmPo;
import com.ucpaas.sms.exception.BusinessException;
import com.ucpaas.sms.mapper.message.AccountMapper;
import com.ucpaas.sms.mapper.message.CommonMapper;
import com.ucpaas.sms.mapper.message.UserMapper;
import com.ucpaas.sms.model.AgentInfo;
import com.ucpaas.sms.model.AuditLog;
import com.ucpaas.sms.model.OauthPic;
import com.ucpaas.sms.model.initstatic.StaticInitVariable;
import com.ucpaas.sms.service.common.CommonSeqService;
import com.ucpaas.sms.service.common.CommonService;
import com.ucpaas.sms.service.sms.SmsManageService;
import com.ucpaas.sms.service.util.AgentUtils;
import com.ucpaas.sms.service.util.ConfigUtils;
import com.ucpaas.sms.service.util.EmailUtils;
import com.ucpaas.sms.service.util.OrderUtils;
import com.ucpaas.sms.util.DateUtil;
import com.ucpaas.sms.util.DateUtils;
import com.ucpaas.sms.util.SecurityUtils;
import com.ucpaas.sms.util.StringUtils;
import com.ucpaas.sms.util.security.Des3Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@Transactional
public class CustomerManageServiceImpl implements CustomerManageService {

	private static final Logger logger = LoggerFactory.getLogger(CustomerManageServiceImpl.class);

	@Autowired
	private MessageMasterDao masterDao;

	@Autowired
	private AccessMasterDao accessMasterDao;

	@Autowired
	private CommonSeqService commonSeqService;

	@Autowired
	private CommonService commonService;

	@Autowired
	private SmsManageService smsManageService;

	@Autowired
	private AccountMapper accountMapper;

	@Autowired
	private UserMapper userMapper;

	@Autowired
	private CommonMapper commonMapper;

	@Autowired
    private JsmsOemAgentPoolService jsmsOemAgentPoolService;

	@Autowired
    private JsmsOemAgentOrderService jsmsOemAgentOrderService;

	@Autowired
    private JsmsOemClientPoolService jsmsOemClientPoolService;

	@Autowired
    private JsmsOemClientOrderService jsmsOemClientOrderService;

	@Autowired
    private JsmsOemAgentAccountStatisticsService jsmsOemAgentAccountStatisticsService;

	@Autowired
    private JsmsAccountService jsmsAccountService;

	@Autowired
	private JsmsClientInfoExtService jsmsClientInfoExtService;

	@Autowired
	private SelfOpenAccountService selfOpenAccountService;

	@Autowired
	private JsmsClientBalanceAlarmService jsmsClientBalanceAlarmService;


	@Override
	public Map<String, Object> getAgentId(Long admin_id) {
		return masterDao.getOneInfo("customer.getAgentId", admin_id);
	}

	@Override
	public int updateClientOauthDate(String clientId) {
		// 更新客户的认证时间
		int updateOauthDate = accountMapper.updateOauthDate(clientId);
		return updateOauthDate;
	}

	@Override
	@Deprecated
	public Map<String, Object> saveAcc(Map<String, String> params) {
		Map<String, Object> data = new HashMap<String, Object>();
		String password = UUID.randomUUID().toString().replace("-", "").substring(4, 12);
		String identify = (String) commonService.getSysParams("DEFAULT_IDENTIFY").get("param_value");// 获取系统参数,默认的identify

		params.put("id", UUID.randomUUID().toString().replace("-", ""));// 添加主键,32位随机字符串
		params.put("clientid", commonSeqService.getOrAddId());// 添加用户id(从公用序列中取,如果没有则生成后再取)
		params.put("password", password);// 初始密码(随机八位数字和字母组合)
		params.put("status", "1");// 添加用户状态：0：注册未激活，1：注册完成，5：冻结，6：注销，7：锁定',

		if (StringUtils.isNotBlank(params.get("isTestAccount"))) {
			params.put("oauth_status", "3");// 认证状态，2：待认证 ，3：证件已认证(正常)，4：认证不通过
			params.put("agent_owned", "1");
		} else {
			params.put("agent_owned", "0");
			params.put("oauth_status", "2");// 认证状态，2：待认证 ，3：证件已认证(正常)，4：认证不通过
		}
		params.put("client_level", "2");// 用户等级，1：普通客户（6－8位用户扩展），2：中小企业大型企业（4－5位用户扩展），3：大型企业（2－3位用户扩展）
		params.put("needreport", "0");// 是否需要状态报告，0：不需要，1：需要简单状态报告，2：需要透传状态报告
		params.put("needmo", "0");// 是否需要上行，0：不需要，1：需要
		// Mod by lpjLiu 2017-07-21 开户默认显示 审核关键字
		// params.put("needaudit", "1");// 是否需要审核，0：不需要，1：营销需要，2：全部需要，3：关键字审核
		params.put("needaudit", "3");

		params.put("ip", "*");// 验证IP
		params.put("nodenum", "1");// 连接节点数
		params.put("needextend", "0");// 是否支持自扩展，0：不支持，1：支持'
		params.put("signextend", "0");// 是否支持签名对应签名端口，0：不支持，1：支持',
		params.put("smsfrom", "6");// 短信协议类型
		params.put("isoverratecharge", "0");// 是否超频计费，0：不需要，1：需要',
		params.put("identify", identify);// 标识，取值范围【0,9】 在配置文件中配置

		params.put("paytype", "0"); // 付费类型，0：预付费，1：后付费；默认是0
		logger.debug("代理商平台--客户管理 ,新用户开户参数 ：" + params);

		logger.debug("检查参数(邮件和手机号是否已经被注册)开始======================================");

		Map<String, Object> checkInAccountTable = masterDao.selectOne("customer.accountApplyCheckInAcc", params);
		if (null != checkInAccountTable) {
			String email = Objects.toString(checkInAccountTable.get("email"), "");
			String mobile = Objects.toString(checkInAccountTable.get("mobile"), "");
			if (StringUtils.isNoneBlank(email)) {
				data.put("result", "fail");
				data.put("msg", "邮箱已经被注册");
				return data;
			}
			if (StringUtils.isNoneBlank(mobile)) {
				data.put("result", "fail");
				data.put("msg", "手机已经被注册");
				return data;
			}
		}

		logger.debug("检查参数(邮件和手机号是否已经被注册)结束======================================");

		{// 设置端口扩展类型和用户端口
			String extendtype = "6";
			params.put("extendtype", extendtype);

			String extendport = null;
			try {
				extendport = this.getExtendportForOpenAcc(extendtype);
			} catch (Exception e) {
				e.printStackTrace();
				data.put("result", "fail");
				data.put("msg", "操作失败");
				return data;
			}
			params.put("extendport", extendport);

		}

		// 添加params数据到数据库
		logger.debug("插入账户数据及更新clientid状态开始======================================");
		int saveNum = masterDao.insert("customer.saveAcc", params);
		boolean updateIdStatus = commonSeqService.updateClientIdStatus(params.get("clientid"));
		logger.debug("插入账户数据及更新clientid状态结束======================================");
		JsmsClientInfoExt clientInfoExt = new JsmsClientInfoExt();
		clientInfoExt.setClientid(params.get("clientid"));
		clientInfoExt.setParentId(null);
		clientInfoExt.setRemark(null);
		clientInfoExt.setUpdator(Long.parseLong( params.get("admin_id")));
		clientInfoExt.setUpdateDate(Calendar.getInstance().getTime());
		clientInfoExt.setWebPassword(params.get("password"));
		int addExtCount = jsmsClientInfoExtService.insert(clientInfoExt);
		String admin_id = params.get("admin_id");
		AgentInfo agent_info_map = AgentUtils.queryAgentInfoByAdminId(admin_id);
		String agent_id = agent_info_map.getAgentId();
		String agent_name = agent_info_map.getAgentName();
		String agent_mobile = agent_info_map.getMobile();

		// 自动认证，赠送测试条数
		{
			if (StringUtils.isNotBlank(params.get("isTestAccount"))) {
				Map<String, String> authParams = Maps.newHashMap();
				authParams.put("agent_id", agent_id);
				authParams.put("clientid", params.get("clientid"));
				authParams.put("adminId", admin_id);

				Map<String, Object> authData = autoAuthAccount(authParams);
				if (authData.get("result").toString().equals("fail")) {
					return data;
				}

				String realName = params.get("realname");
				giveShortMessage(params.get("clientid"), admin_id, realName);

				data.put("password", SecurityUtils.encryptMD5(password));
			}
		}

		// 添加成功,发送邮件给客户
		if (saveNum > 0 && updateIdStatus && addExtCount>0) {

			logger.debug("给客户发送邮件开始======================================");
			// String vUrl = ConfigUtils.client_site_oem_url;//获取客户公用站点地址
			String vUrl = this.queryClientSiteOemUrl(agent_id);

			Integer mailId = 100014;

			Map<String, Object> mail = commonMapper.querySmsMailProp("100014");// 获取邮箱模板,100014为客户开户通用模板

			// 发送开户邮件到邮箱
			String body = (String) mail.get("text");
			body = body.replace("vUrl", vUrl); //
			// body = body.replace("vid", params.get("clientid"));
			// body = body.replace("vmobile", params.get("mobile"));
			// body = body.replace("vemail", params.get("email"));
			body = body.replace("vpassword", password); //
			body = body.replace("agent_name", agent_name);
			body = body.replace("agent_mobile", agent_mobile);
			boolean sendEmail = EmailUtils.sendHtmlEmail(params.get("email"), (String) mail.get("subject"), body);
			// 根据添加结果封装返回信息数据
			data.put("clientid", params.get("clientid"));
			data.put("result", "success");
			data.put("msg", "操作成功");
			data.put("sendEmail", sendEmail ? "信息已发送至邮箱。" : "邮件发送失败！");

			logger.debug("给客户发送邮件结束======================================");
		} else if (saveNum == 0 && !updateIdStatus) {
			// 根据添加结果封装返回信息数据
			data.put("result", "fail");
			data.put("msg", "操作失败");
		} else {
			throw new RuntimeException("数据未同步更新");
		}
		return data;
	}

	// 测试短信赠送
	private void giveShortMessage(String client_id, String admin_id, String realName) {

		logger.debug("测试短信赠送,方法为--->{},参数为---->{}", "GiveShortMessage", client_id);

		Map<String, Object> clientIdParams = new HashMap<>();
		clientIdParams.put("clientid", client_id);

		String agent_id = masterDao.getOneInfo("sms.getAgentIdByClientId", clientIdParams);

		Map<String, Object> agentIdParams = new HashMap<>();
		agentIdParams.put("agent_id", agent_id);

		int agent_type = masterDao.getOneInfo("sms.getAgentTypeByAgentId", agentIdParams);

		// 品牌代理/销售代理的客户,不赠送测试短信(只有OEM代理商赠送短信)
		if (agent_type != 5) {
			logger.debug("代理商类型为--->{}，不是oem代理商，不能赠送短信", agent_type);
			return;
		}

		// 不赠送短信产品
		Map<String, Object> oemDataMap = masterDao.getOneInfo("sms.getOemDataConfig", agentIdParams);

		if (oemDataMap == null || oemDataMap.get("id") == null) {
			logger.debug("代理商：{}------->没有对应的oem资料", agent_id);
			return;
		}

		String oemDataId = oemDataMap.get("id").toString();

		if (oemDataMap.get("test_product_id") == null || oemDataMap.get("test_sms_number") == null
				|| "0".equals(oemDataMap.get("test_sms_number").toString())) {
			logger.debug("oemid:{}资料----->为空、或者条数--->为空、或者-->条数", oemDataId);
			return;
		}

		String test_product_id = oemDataMap.get("test_product_id").toString();
		String test_sms_number = oemDataMap.get("test_sms_number").toString();
		// 测试产品包已经下架或者已经过期
		Map<String, Object> productIdMap = new HashMap<>();
		productIdMap.put("product_id", test_product_id);

		Map<String, Object> oemProductInfoMap = masterDao.getOneInfo("sms.getOemProductInfoByProductId", productIdMap);
		int productStatus = (int) oemProductInfoMap.get("status"); // 状态，0：待上架，1：已上架，2：已下架
		String unit_price = oemProductInfoMap.get("unit_price").toString();
		String product_type = oemProductInfoMap.get("product_type").toString();// 产品类型,0:行业,1:营销,2:国际
		String due_time = oemProductInfoMap.get("due_time").toString();
		String product_id = oemProductInfoMap.get("product_id").toString();
		String product_code = oemProductInfoMap.get("product_code").toString();
		String product_name = oemProductInfoMap.get("product_name").toString();

		if (productStatus != 1) {
			logger.debug("产品id:{}--------->为待上架或者已下架", test_product_id);
			return;
		}

		// 满足赠送短信的条件
		logger.debug("满足赠送短信的条件=============================================================");

		Date now = new Date();

		Map<String, Object> agentAccount = masterDao.getOneInfo("sms.getAgentAccountByAgentId", agentIdParams);
		String balance = agentAccount.get("balance").toString();
		BigDecimal oldBgBalance = new BigDecimal(balance);

		BigDecimal bgTestNum = new BigDecimal(test_sms_number);
		BigDecimal bgUnitPrice = new BigDecimal(unit_price);
		BigDecimal bgAmount = bgTestNum.multiply(bgUnitPrice);

		BigDecimal newBgBalance = oldBgBalance.add(bgAmount);

		// 生成余额账单(代理商入账)
		Map<String, Object> agentBalanceBillParams = new HashMap<>();
		agentBalanceBillParams.put("id", null);
		agentBalanceBillParams.put("agent_id", agent_id);
		agentBalanceBillParams.put("payment_type", "5");// 业务类型，0：充值，1：扣减，2：佣金转余额，3：购买产品包，4：退款，5：赠送
		agentBalanceBillParams.put("financial_type", "0");// 财务类型，0：入账，1：出账
		agentBalanceBillParams.put("amount", bgAmount.toString());

		agentBalanceBillParams.put("balance", newBgBalance.toString());
		agentBalanceBillParams.put("create_time", now);
		agentBalanceBillParams.put("order_id", null); // 充值操作订单id为null
		agentBalanceBillParams.put("admin_id", admin_id);
		agentBalanceBillParams.put("client_id", client_id);
		agentBalanceBillParams.put("remark", "赠送短信充值");

		int i = masterDao.insert("sms.createAgentBalanceBill", agentBalanceBillParams);
		if (i <= 0) {
			throw new BusinessException("赠送短信，生成余额入账账单失败");
		}

		// 判断OEM代理商短信池(t_sms_oem_agent_pool)是否存在记录(获取agent_pool_id)
		String agent_pool_id = null;

		Map<String, Object> agentPoolParams = new HashMap<>();
		agentPoolParams.put("agent_id", agent_id);
		agentPoolParams.put("product_type", product_type); // 产品类型，0：行业，1：营销，2：国际
		agentPoolParams.put("due_time", due_time); // 到期时间
		agentPoolParams.put("unit_price", unit_price);
		agentPoolParams.put("status", "0"); // 状态，0：正常，1：停用

		agentPoolParams.put("operator_code", "0"); //运营商
		agentPoolParams.put("area_code", "0"); // 区域

		Map<String, Object> params = masterDao.getOneInfo("sms.getAgentPoolIdByCondition", agentPoolParams);
		if (params != null && params.get("agent_pool_id") != null) {
			agent_pool_id = params.get("agent_pool_id").toString();
		} else {

			Map<String, Object> agentPoolMap = new HashMap<>();
			agentPoolMap.put("agent_pool_id", null);
			agentPoolMap.put("agent_id", agent_id);
			agentPoolMap.put("product_type", product_type);
			agentPoolMap.put("due_time", due_time);
			agentPoolMap.put("status", "0"); // 状态，0：正常，1：停用

			agentPoolMap.put("remain_number", "0");
			agentPoolMap.put("unit_price", unit_price);
			agentPoolMap.put("remain_amount", null);
			agentPoolMap.put("update_time", now);
			agentPoolMap.put("remark", null);

			int j = masterDao.insert("sms.createOemAgentPool", agentPoolMap);
			if (j <= 0) {
				throw new BusinessException("生成代理商短信池记录失败！");
			}
			agent_pool_id = agentPoolMap.get("agent_pool_id").toString();
		}

		// 生成代理商订单(购买记录)
		Map<String, Object> oemAgentOrderMapForBuy = new HashMap<>();

		String orderId = OrderUtils.getAgentOrderId().toString();
		oemAgentOrderMapForBuy.put("order_id", orderId);
		oemAgentOrderMapForBuy.put("order_no", orderId);
		oemAgentOrderMapForBuy.put("order_type", 0); // 订单类型，0：OEM代理商购买，1：OEM代理商分发，2：OEM代理商回退
		oemAgentOrderMapForBuy.put("product_id", product_id);
		oemAgentOrderMapForBuy.put("product_code", product_code);

		oemAgentOrderMapForBuy.put("product_type", product_type);
		oemAgentOrderMapForBuy.put("product_name", product_name);
		oemAgentOrderMapForBuy.put("unit_price", unit_price);
		oemAgentOrderMapForBuy.put("order_number", test_sms_number); // 赠送的短信条数
		oemAgentOrderMapForBuy.put("order_amount", bgAmount.toString());

		oemAgentOrderMapForBuy.put("product_price", "0");
		oemAgentOrderMapForBuy.put("agent_id", agent_id);
		oemAgentOrderMapForBuy.put("client_id", client_id);
		oemAgentOrderMapForBuy.put("name", "云之讯");
		oemAgentOrderMapForBuy.put("agent_pool_id", agent_pool_id);

		oemAgentOrderMapForBuy.put("due_time", due_time);
		oemAgentOrderMapForBuy.put("create_time", now);
		oemAgentOrderMapForBuy.put("remark", null);

		int k = masterDao.insert("sms.insertOemAgentOrder", oemAgentOrderMapForBuy);
		if (k <= 0) {
			throw new BusinessException("生成代理商订单（购买记录）失败！");
		}

		// 生成余额账单(代理商出账)
		Map<String, Object> agentBalanceBillOutParams = new HashMap<>();
		agentBalanceBillOutParams.put("id", null);
		agentBalanceBillOutParams.put("agent_id", agent_id);
		agentBalanceBillOutParams.put("payment_type", "3");// 业务类型，0：充值，1：扣减，2：佣金转余额，3：购买产品包，4：退款，5：赠送
		agentBalanceBillOutParams.put("financial_type", "1");// 财务类型，0：入账，1：出账
		agentBalanceBillOutParams.put("amount", bgAmount.toString());

		agentBalanceBillOutParams.put("balance", oldBgBalance.toString());
		agentBalanceBillOutParams.put("create_time", now);
		agentBalanceBillOutParams.put("order_id", orderId);
		agentBalanceBillOutParams.put("admin_id", admin_id);
		agentBalanceBillOutParams.put("client_id", client_id);
		agentBalanceBillOutParams.put("remark", "赠送短信充值");

		int m = masterDao.insert("sms.createAgentBalanceBill", agentBalanceBillOutParams);
		if (m <= 0) {
			throw new BusinessException("赠送短信，生成余额出账账单失败");
		}

		// ======================================给客户充值===========================================

		// 生成代理商订单(分发记录)
		Map<String, Object> oemAgentOrderMapForDistribute = new HashMap<>();

		String orderId2 = OrderUtils.getAgentOrderId().toString();
		oemAgentOrderMapForDistribute.put("order_id", orderId2);
		oemAgentOrderMapForDistribute.put("order_no", orderId2);
		oemAgentOrderMapForDistribute.put("order_type", 1); // 订单类型，0：OEM代理商购买，1：OEM代理商分发，2：OEM代理商回退
		oemAgentOrderMapForDistribute.put("product_id", product_id);
		oemAgentOrderMapForDistribute.put("product_code", product_code);

		oemAgentOrderMapForDistribute.put("product_type", product_type);
		oemAgentOrderMapForDistribute.put("product_name", product_name);
		oemAgentOrderMapForDistribute.put("unit_price", unit_price);
		oemAgentOrderMapForDistribute.put("order_number", test_sms_number); // 赠送的短信条数
		oemAgentOrderMapForDistribute.put("order_amount", bgAmount.toString());

		oemAgentOrderMapForDistribute.put("product_price", "0");
		oemAgentOrderMapForDistribute.put("agent_id", agent_id);
		oemAgentOrderMapForDistribute.put("client_id", client_id);
		oemAgentOrderMapForDistribute.put("name", realName);
		oemAgentOrderMapForDistribute.put("agent_pool_id", agent_pool_id);

		oemAgentOrderMapForDistribute.put("due_time", due_time);
		oemAgentOrderMapForDistribute.put("create_time", now);
		oemAgentOrderMapForDistribute.put("remark", null);

		int n = masterDao.insert("sms.insertOemAgentOrder", oemAgentOrderMapForDistribute);
		if (n <= 0) {
			throw new BusinessException("生成代理商订单（分发记录）失败！");
		}

		// 判断oem客户短信池是否存在记录(获取client_pool_id)

		// 判断OEM代理商短信池(t_sms_oem_agent_pool)是否存在记录(获取agent_pool_id)
		String client_pool_id = null;

		Map<String, Object> clientPoolParams = new HashMap<>();
		clientPoolParams.put("client_id", client_id);
		clientPoolParams.put("product_type", product_type); // 产品类型，0：行业，1：营销，2：国际
		clientPoolParams.put("due_time", due_time); // 到期时间
		clientPoolParams.put("unit_price", unit_price);
		clientPoolParams.put("status", "0"); // 状态，0：正常，1：停用

		clientPoolParams.put("operator_code", "0"); //运营商
		clientPoolParams.put("area_code", "0"); // 区域

		Map<String, Object> clientPoolIdMap = masterDao.getOneInfo("sms.getClientPoolIdByCondition", clientPoolParams);
		if (clientPoolIdMap != null && clientPoolIdMap.get("client_pool_id") != null) {
			client_pool_id = clientPoolIdMap.get("client_pool_id").toString();

			Map<String, Object> updateClientPoolMap = new HashMap<>();
			updateClientPoolMap.put("client_pool_id", client_pool_id);
			updateClientPoolMap.put("test_num", test_sms_number);

			int o = masterDao.update("sms.updateClientPoolByCondition", updateClientPoolMap);
			if (o <= 0) {
				throw new BusinessException("更新客户短信池的测试条数失败！");
			}

		} else {

			Map<String, Object> clientPoolMap = new HashMap<>();
			clientPoolMap.put("client_pool_id", null);
			clientPoolMap.put("client_id", client_id);
			clientPoolMap.put("product_type", product_type);
			clientPoolMap.put("due_time", due_time);
			clientPoolMap.put("status", 0); // 状态，0：正常，1：停用

			clientPoolMap.put("total_number", test_sms_number);
			clientPoolMap.put("remain_number", test_sms_number);
			clientPoolMap.put("unit_price", unit_price);
			clientPoolMap.put("total_amount", 0);
			clientPoolMap.put("remain_amount", 0);

			clientPoolMap.put("remain_test_number", test_sms_number);
			clientPoolMap.put("update_time", now);
			clientPoolMap.put("remark", null);

			int p = masterDao.insert("sms.createOemClientPool", clientPoolMap);
			if (p <= 0) {
				throw new BusinessException("生成客户短信池失败！");
			}

			client_pool_id = clientPoolMap.get("client_pool_id").toString();
		}

		// 给客户订单增加分发记录(生成oem客户订单)
		Map<String, Object> oemClientOrderMap = new HashMap<>();
		String oemClientOrderId = getClientOrderId().toString();

		oemClientOrderMap.put("order_id", oemClientOrderId);
		oemClientOrderMap.put("order_no", oemClientOrderId);
		oemClientOrderMap.put("product_type", product_type); // 产品类型，0：行业，1：营销，2：国际
		oemClientOrderMap.put("order_type", "1"); // 订单类型，1：OEM代理商分发，2：OEM代理商回退
		oemClientOrderMap.put("order_number", test_sms_number);// 赠送的短信条数

		oemClientOrderMap.put("unit_price", unit_price);
		oemClientOrderMap.put("order_price", bgAmount.toString());
		oemClientOrderMap.put("client_id", client_id);
		oemClientOrderMap.put("agent_id", agent_id);
		oemClientOrderMap.put("client_pool_id", client_pool_id);

		oemClientOrderMap.put("due_time", due_time);
		oemClientOrderMap.put("create_time", now);
		oemClientOrderMap.put("remark", null);

		int q = masterDao.insert("sms.insertOemClientOrder", oemClientOrderMap);
		if (q <= 0) {
			throw new BusinessException("生成oem客户订单失败！");
		}

	}

	// 获取扩展端口
	private String getExtendportForOpenAcc(String extendtype) {
		String extendport = null;

		Map<String, Object> getParams = new HashMap<>();
		getParams.put("extendtype", extendtype);

		Map<String, Object> data = masterDao.getOneInfo("customer.getExtendportAssign", getParams);
		if (data == null || data.get("extendtype") == null) {
			throw new RuntimeException("没有可用的端口分配");
		}

		if (data.get("reusenumber") != null && !"".equals(data.get("reusenumber").toString())) {
			String reusenumberStr = data.get("reusenumber").toString();
			String[] reusenumberArry = reusenumberStr.split(",");

			if (reusenumberArry.length > 1) {
				String newReusenumberStr = "";
				String startStr = reusenumberArry[0];

				newReusenumberStr = newReusenumberStr + startStr;
				for (int i = 1; i < reusenumberArry.length - 1; i++) {
					newReusenumberStr = newReusenumberStr + "," + reusenumberArry[i];
				}

				// 更新重用端口
				Map<String, Object> updateParams = new HashMap<>();
				updateParams.put("reusenumber", newReusenumberStr);
				updateParams.put("extendtype", extendtype);

				updateParams.put("oldReusenumber", reusenumberStr); // 判断值是否修改(乐观锁)

				int i = this.masterDao.update("customer.updateExtendportAssign", updateParams);
				if (i == 0) {
					throw new RuntimeException("更新重用端口失败");
				}
			}

			if (reusenumberArry.length == 1) {
				Map<String, Object> updateParams = new HashMap<>();
				updateParams.put("reusenumber", "");
				updateParams.put("extendtype", extendtype);

				updateParams.put("oldReusenumber", reusenumberStr); // 判断值是否修改(乐观锁)

				int i = this.masterDao.update("customer.updateExtendportAssign", updateParams);
				if (i == 0) {
					throw new RuntimeException("更新重用端口失败");
				}
			}
			extendport = reusenumberArry[reusenumberArry.length - 1];

		} else {
			int endnumber = Integer.valueOf(data.get("endnumber").toString());
			int currentnumber = Integer.valueOf(data.get("currentnumber").toString());

			int newCurrentnumber = currentnumber + 1;
			Map<String, Object> updateParams = new HashMap<>();
			updateParams.put("extendtype", extendtype);
			updateParams.put("currentnumber", newCurrentnumber);
			updateParams.put("oldCurrentnumber", currentnumber);

			if (newCurrentnumber > endnumber) {
				updateParams.put("status", 1); // 如果当前端口已经使用完，必须修改状态为禁用
				updateParams.put("oldStatus", data.get("status"));
			}

			int i = this.masterDao.update("customer.updateExtendportAssign", updateParams);
			if (i == 0) {
				throw new RuntimeException("更新端口分配范围表失败");
			}

			extendport = currentnumber + "";
		}

		return extendport;
	}

	/**
	 * @Title: queryClientSiteOemUrl
	 * @Description: 查询oem客户端对应的地址
	 * @return
	 * @return: String
	 */
	public String queryClientSiteOemUrl(String agent_id) {
		String vUrl = null;

		Map<String, Object> oem_data_config_map = this.masterDao.getOneInfo("customer.queryOemDataConfigByAgentId",
				agent_id);
		if (oem_data_config_map != null && oem_data_config_map.get("domain_name") != null) {
			vUrl = "http://" + oem_data_config_map.get("domain_name").toString();
		} else {
			vUrl = ConfigUtils.client_site_oem_url;
		}
		return vUrl;
	}

	@Override
	public boolean validateAcc(Map<String, String> params) {
		Map<String, Object> data = new HashMap<String, Object>();
		data = masterDao.getOneInfo("customer.validateAcc", params);
		return (long) data.get("count") > 0 ? false : true;
	}

	@Override
	public Map<String, Object> getDetailInfo(Map<String, String> params) {
		return masterDao.getOneInfo("customer.getDetailInfo", params);
	}

	@Override
	public Map<String, Object> resetPsd(Map<String, String> params) {
		Map<String, Object> data = new HashMap<String, Object>();
		String password = UUID.randomUUID().toString().replace("-", "").substring(4, 12);
		params.put("password", password);// 密码不加密
		logger.debug("修改密码开始======================================");
		int result = masterDao.update("customer.resetPsd", params);
		logger.debug("修改密码结束======================================");
		if (result > 0) {

			logger.debug("给客户发送邮件开始======================================");
			String admin_id = params.get("admin_id");
			AgentInfo agentInfo = AgentUtils.queryAgentInfoByAdminId(admin_id);
			String agent_id = agentInfo.getAgentId();
			String agent_name = agentInfo.getAgentName();

			// String vUrl = ConfigUtils.client_site_oem_url;//获取oem客户平台地址
			String vUrl = this.queryClientSiteOemUrl(agent_id);

			Map<String, Object> mail = commonMapper.querySmsMailProp("100016");// 获取邮箱模板,100016为代理商用户修改密码通用模板
			String email = params.get("email");
			if (StringUtils.isBlank(email)) {
				data.put("result", "success");
				data.put("msg", "密码重置成功,");
				data.put("sendEmail", "邮件发送失败,请联系客服");
			} else {
				// 发送开户邮件到邮箱
				String body = (String) mail.get("text");
				body = body.replace("vUrl", vUrl);
				body = body.replace("vid", params.get("email"));
				body = body.replace("vpassword", password);
				body = body.replace("agent_name", agent_name);

				String subject = (String) mail.get("subject");
				subject = subject.replace("agent_name", agent_name);

				boolean sendEmail = EmailUtils.sendHtmlEmail(params.get("email"), subject, body);
				data.put("result", "success");
				data.put("msg", "密码重置成功,");
				data.put("sendEmail", sendEmail ? "信息已发送至邮箱" : "邮件发送失败,请联系客服");

				logger.debug("给客户发送邮件结束======================================");
			}
		} else {
			data.put("result", "fail");
			data.put("msg", "密码重置失败,请联系客服");
			data.put("sendEmail", "");
		}
		return data;
	}

	@Override
	public PageContainer queryCustomerInfo(Map<String, String> params) {
		PageContainer page = masterDao.getSearchPage("customer.queryCustomerInfo", "customer.queryCustomerInfoCount",
				params);
		String url = this.queryClientSiteOemUrl(params.get("agent_id"));
		List<JsmsClientBalanceAlarm>  list = new ArrayList<>();
		StringBuilder ym = new StringBuilder();
		List<Map<String, Object>> result = new ArrayList();
		List<Map<String, Object>> result1 = new ArrayList();
		List<Map<String, Object>> result2 = new ArrayList();
		Map<String, Object> alarmParams = new HashMap<>();
		int flag =0;
		int rowNum = 1;
		int index =0;
		for (Map map : page.getList()) {
			ym.setLength(0);
			ym.append(url);
			String pwd = map.get("password") != null ? map.get("password").toString() : null;
			String mobile = map.get("mobile") != null ? map.get("mobile").toString() : null;
			String clientId = map.get("client_id") != null ? map.get("client_id").toString() : null;
			String agentOwned = map.get("agent_owned") != null ? map.get("agent_owned").toString() : null;
			map.put("oemUrl", "");
			if (StringUtils.isBlank(pwd) || StringUtils.isBlank(agentOwned) || !"1".equals(agentOwned)
					|| StringUtils.isBlank(mobile) || StringUtils.isBlank(clientId)) {
				continue;
			}

			// 客户id====手机号码====密码
			StringBuilder sb = new StringBuilder();
			sb.append(clientId).append("====").append(mobile).append("====").append(pwd).append("====")
					.append(Calendar.getInstance().getTimeInMillis());
			String encStr = Des3Utils.encodeDes3(sb.toString());
			ym.append("/console").append("?oemAuthToken=").append(encStr);
			map.remove("password");
			map.put("oemUrl", ym.toString());
		}
		if(("1").equals(params.get("isRechargeRollback"))){//充值回退业务
			for (int i =0;i<page.getList().size();i++) {
				index+=1;
				Map map = page.getList().get(i);
				if(com.ucpaas.sms.common.util.StringUtils.isNotBlank(map.get("client_id").toString())){
					alarmParams.put("clientid",map.get("client_id").toString());
					alarmParams.put("yxAlarmNumber",Integer.parseInt(String.valueOf(map.get("yx_remain_num"))));
					alarmParams.put("gjAlarmAmount",new BigDecimal(String.valueOf(map.get("gj_remain_num"))));
					alarmParams.put("yzmAlarmNumber",Integer.parseInt(String.valueOf(map.get("co_remain_num")))+Integer.parseInt(String.valueOf(map.get("hy_remain_num"))));
					alarmParams.put("tzAlarmNumber",Integer.parseInt(String.valueOf(map.get("no_remain_num")))+Integer.parseInt(String.valueOf(map.get("hy_remain_num"))));
					flag = jsmsClientBalanceAlarmService.isAlarm(alarmParams);
				}
				if(flag>0){
					JsmsClientBalanceAlarm jsmsClientBalanceAlarm = new JsmsClientBalanceAlarm();
					jsmsClientBalanceAlarm.setClientid(map.get("client_id").toString());
					list = jsmsClientBalanceAlarmService.findListAlarm(jsmsClientBalanceAlarm,null);
					if(!list.isEmpty()){
						for(int j=0;j<list.size();j++){
							if(list.get(j)!=null){
								if(list.get(j).getAlarmType().intValue()==1){
									if(Integer.parseInt(String.valueOf(alarmParams.get("yzmAlarmNumber")))<=list.get(j).getAlarmNumber()){
										map.put("yzmAlarm","1");
									}else{
										map.put("yzmAlarm","0");
									}
								}else if(list.get(j).getAlarmType().intValue() ==2){
									if(Integer.parseInt(String.valueOf(alarmParams.get("tzAlarmNumber")))<=list.get(j).getAlarmNumber()){
										map.put("tzAlarm","1");
									}else{
										map.put("tzAlarm","0");
									}
								}else if(list.get(j).getAlarmType().intValue() ==3){
									if(Integer.parseInt(String.valueOf(alarmParams.get("yxAlarmNumber")))<=list.get(j).getAlarmNumber()){
										map.put("yxAlarm","1");
									}else{
										map.put("yxAlarm","0");
									}
								}else if(list.get(j).getAlarmType().intValue()==4){
									if(new BigDecimal(String.valueOf(alarmParams.get("gjAlarmAmount"))).compareTo(list.get(j).getAlarmAmount())!=1){
										map.put("gjAlarm","1");
									}else{
										map.put("gjAlarm","0");
									}
								}
							}
						}
						if(String.valueOf(map.get("yzmAlarm")).equals("1")||String.valueOf(map.get("tzAlarm")).equals("1")){
							map.put("hyAlarm","1");
						}else{
                            map.put("hyAlarm","0");
                        }
					}
					map.put("rownum",(page.getCurrentPage() - 1)*page.getPageRowCount() + rowNum);
					result1.add(map);
					++rowNum;
				}
			}
			if(index == page.getList().size()){
				for (int i =0;i<page.getList().size();i++) {
					Map map = page.getList().get(i);
					if(com.ucpaas.sms.common.util.StringUtils.isNotBlank(map.get("client_id").toString())){
						alarmParams.put("clientid",map.get("client_id").toString());
						alarmParams.put("yxAlarmNumber",Integer.parseInt(String.valueOf(map.get("yx_remain_num"))));
						alarmParams.put("gjAlarmAmount",new BigDecimal(String.valueOf(map.get("gj_remain_num"))));
						alarmParams.put("yzmAlarmNumber",Integer.parseInt(String.valueOf(map.get("co_remain_num")))+Integer.parseInt(String.valueOf(map.get("hy_remain_num"))));
						alarmParams.put("tzAlarmNumber",Integer.parseInt(String.valueOf(map.get("no_remain_num")))+Integer.parseInt(String.valueOf(map.get("hy_remain_num"))));
						flag = jsmsClientBalanceAlarmService.isAlarm(alarmParams);
					}
					if(flag==0){
						map.put("rownum",(page.getCurrentPage() - 1)*page.getPageRowCount() + rowNum);
						map.put("tzAlarm","0");
						map.put("yzmAlarm","0");
						map.put("hyAlarm","0");
						map.put("gjAlarm","0");
						map.put("yxAlarm","0");
						result2.add(map);
						++rowNum;
					}
				}
			}
			result.addAll(result1);
			result.addAll(result2);
			page.setList(result);
		}
		return page;
	}

	@Override
	public PageContainer queryCustomerConsumeReport(Map<String, String> params) {
		PageContainer page=new PageContainer();
//		if("1".equals(params.get("paytype").toString())){
//			page=accessMasterDao.getSearchPage("customer.queryCustomerConsumeReport1",
//					"customer.queryCustomerConsumeReportCount1", params);
//		}else {
//			page=accessMasterDao.getSearchPage("customer.queryCustomerConsumeReport",
//					"customer.queryCustomerConsumeReportCount", params);
//		}
		page=accessMasterDao.getSearchPage("customer.queryCustomerConsumeReport1",
				"customer.queryCustomerConsumeReportCount1", params);
		return page;
	}



	@Override
	public PageContainer querycustomerConsumeEveryReport(Map<String, String> params) {

		PageContainer page=new PageContainer();
//		if("1".equals(params.get("paytype").toString())){//后付费只有短信维度
//			page=accessMasterDao.getSearchPage("customer.querycustomerConsumeEveryReport1",
//					"customer.querycustomerConsumeEveryReportCount1", params);
//		}else {
//			page=accessMasterDao.getSearchPage("customer.querycustomerConsumeEveryReport",
//					"customer.querycustomerConsumeEveryReportCount", params);
//		}
		page=accessMasterDao.getSearchPage("customer.querycustomerConsumeEveryReport1",
				"customer.querycustomerConsumeEveryReportCount1", params);
		return page;
	}

	/**
	 * @param params
	 * @Title: queryCustomerConsumeReport4Export
	 * @Description: 查询客户短信报表下载
	 * @return:List
	 */
	@Override
	public List<Map<String, Object>> queryCustomerConsumeReport4Export(Map<String, String> params) {
		List<Map<String,Object>> list=new ArrayList<Map<String,Object>>();
//		if("1".equals(params.get("paytype").toString())){//后付费只有短信维度
//			list=accessMasterDao.getSearchList("customer.queryCustomerConsumeReport1", params);
//		}else {
//			list=accessMasterDao.getSearchList("customer.queryCustomerConsumeReport", params);
//		}
		list=accessMasterDao.getSearchList("customer.queryCustomerConsumeReport1", params);
		return list;
	}

	/**
	 * @param params
	 * @Title: queryCustomerConsumeReport4Export
	 * @Description: 查询客户每日报表下载
	 * @return:List
	 */
	@Override
	public List<Map<String, Object>> querycustomerConsumeEveryReport4Export(Map<String, String> params) {
		List<Map<String,Object>> list=new ArrayList<Map<String,Object>>();
//		if("1".equals(params.get("paytype").toString())){//后付费只有短信维度
//			list=accessMasterDao.getSearchList("customer.querycustomerConsumeEveryReport1", params);
//		}else {
//			list=accessMasterDao.getSearchList("customer.querycustomerConsumeEveryReport", params);
//		}
		list=accessMasterDao.getSearchList("customer.querycustomerConsumeEveryReport1", params);
		return list;

	}

	@Override
	public List<Map<String, Object>> queryCustomerInfoForAll(Map<String, String> params) {
		List<Map<String, Object>> list = new ArrayList<>();
		if(String.valueOf(params.get("isRechargeRollback")).equals("1")){
			params.put("pageRowCount", String.valueOf(masterDao.getSearchSize("customer.queryCustomerInfoCount",params)));
			params.put("currentPage", "1");
			PageContainer page = masterDao.getSearchPage("customer.queryCustomerInfo", "customer.queryCustomerInfoCount",
					params);
			list = page.getList();
		}else{
			list = masterDao.getSearchList("customer.queryCustomerInfoForAll", params);
		}
		return list;
	}

	@Override
	public Map<String, Object> editStatus(Map<String, String> params) {
		Map<String, Object> data = new HashMap<String, Object>();
		int updateNum = this.masterDao.update("customer.editStatus", params);
		if (updateNum > 0) {
			data.put("result", "success");
			data.put("msg", "操作成功");
		} else {
			data.put("result", "fail");
			data.put("msg", "操作失败");
		}
		return data;
	}

	@Override
	public Map<String, Object> editCustomerRemark(Map<String, String> params) {

		Map<String, Object> data = new HashMap<String, Object>();
		int updateNum = this.masterDao.update("customer.editCustomerRemark", params);
		if (updateNum > 0) {
			data.put("result", "success");
			data.put("msg", "修改成功");
		} else {
			data.put("result", "fail");
			data.put("msg", "修改失败");
		}
		return data;
	}

	@Override
	public List<Map<String, Object>> queryCommonSmsInfo(Map<String, Object> params) {
		return this.masterDao.getSearchList("customer.queryCommonSmsInfo", params);
	}

	@Override
	public List<Map<String, Object>> queryInterSmsInfo(Map<String, Object> params) {
		return this.masterDao.getSearchList("customer.queryInterSmsInfo", params);
	}

	@Override
	public List<Map<String, Object>> queryCommonSmsInfoForClient(Map<String, Object> params) {
		return this.masterDao.getSearchList("customer.queryCommonSmsInfoForClient", params);
	}

	@Override
	public List<Map<String, Object>> queryInterSmsInfoForClient(Map<String, Object> params) {
		return this.masterDao.getSearchList("customer.queryInterSmsInfoForClient", params);
	}

	@Override
	public String getClientTheMostNumForMinute(String orderIdPre) {
		Map<String, Object> sqlParams = new HashMap<>();
		sqlParams.put("orderIdPre", orderIdPre);
		String numStr = masterDao.getOneInfo("customer.getClientTheMostNumForMinute", sqlParams);
		return numStr;
	}

	@Override
    @Deprecated
	public Map<String, Object> confirmRecharge(Map<String, String> params) {

		logger.debug("oem代理商平台-客户管理-充值开始======================================");

		Map<String, Object> data = new HashMap<String, Object>();

		// 检查充值的数量是否充足
		String agent_id = params.get("agent_id");
		String client_id = params.get("client_id");

		Map<String, Object> sms_acc_map = this.masterDao.getOneInfo("customer.querySmsAccount", params);
		String clientName = sms_acc_map.get("name").toString();

		String product_type = params.get("product_type").toString();
		// String due_time = params.get("due_time").toString();
		BigDecimal updateNum = new BigDecimal(params.get("updateNum").toString());
		BigDecimal remainNum = null;

		logger.debug("充值校验开始===================================记录为一条,前端校验");
		// Mod by lpjLiu 2017-05-23 查询的时候可能存在多条，需要SUM进行求和
		Map<String, Object> remain_map = this.masterDao.getOneInfo("customer.queryAgentPoolRemainNum", params);
		if (!"2".equals(product_type)) {
			// 普通产品
			remainNum = new BigDecimal(remain_map.get("remain_number").toString());
		} else {
			// 国际产品
			remainNum = new BigDecimal(remain_map.get("remain_amount").toString());
		}
		if (updateNum.compareTo(remainNum) == 1) {
			data.put("result", "fail");
			data.put("msg", "您充值的数量大于剩余的数量，请重新充值!");
			return data;
		}

		logger.debug("充值校验结束======================================");

		// 生成代理商订单
		Date nowdate = new Date();
		String order_type = "1";// 0：OEM代理商购买，1：OEM代理商分发，2：OEM代理商回退

//		// Add by lpjLiu 查询出代理商短信池，并按单价由高到低排序
//		List<Map<String, Object>> list = null;
//		if (!"2".equals(product_type)) {
//			list = this.masterDao.getSearchList("customer.queryAgentPoolRemainNumOrderByUnitPrice", params);
//		}

		StringBuffer agent_order_no = new StringBuffer("");
		StringBuffer client_order_no = new StringBuffer("");
//		BigDecimal count = updateNum; // 已处理数量
//		boolean unDone = true;
//		while (unDone) {
//			// Add by lpjLiu 根据代理商池的单价重新设置参数
//			if (!"2".equals(product_type)) {
//				BigDecimal tempNum = null;
//				for (Map<String, Object> item : list) {
//					Boolean used = (Boolean) item.get("used");
//					if (used != null && used) {
//						continue;
//					}
//
//					// 获取当前池的条数
//					BigDecimal remainNumber = new BigDecimal(item.get("remain_number").toString());
//
//					// 跳过为空若已经使用
//					if (remainNumber.compareTo(BigDecimal.ZERO) <= 0) {
//						item.put("used", true);
//						continue;
//					}
//
//					if (remainNumber.compareTo(count) >= 0) {
//						tempNum = count;
//					} else {
//						tempNum = remainNumber;
//					}
//
//					// 减去已处理数量
//					count = count.subtract(tempNum);
//
//					// 放入下面需要处理的参数
//					params.put("unit_price", item.get("unit_price").toString());
//					params.put("updateNum", tempNum.toString());
//					item.put("used", true);
//					break;
//				}
//			}

			// 减少代理商的短信池
			logger.debug("减少代理商短信池开始======================================");
			// Add by lpjLiu 2017-05-23 获取代理商短信池的Id
			StringBuffer agent_pool_id_sb = new StringBuffer("");
			this.updateAgentPoolRemainNum(params, "reduce", agent_pool_id_sb);
			logger.debug("减少代理商短信池结束======================================");

			// 重新设置
			logger.debug("生成代理商订单开始======================================");
			this.createAgentOrder(params, agent_id, nowdate, client_id, clientName, order_type, agent_order_no,
					agent_pool_id_sb);
			logger.debug("生成代理商订单结束======================================");

			// 增加客户的短信池
			logger.debug("增加客户短信池开始======================================");
			// Add by lpjLiu 2017-05-23 获取客户短信池的Id
			StringBuffer client_pool_id_sb = new StringBuffer("");
			this.addClientPoolRemainNum(params, nowdate, client_pool_id_sb);
			logger.debug("增加客户短信池结束======================================");

			// 生成客户订单
			logger.debug("生成客户订单开始======================================");
			this.createClientOrder(params, nowdate, order_type, client_order_no, client_pool_id_sb);
			logger.debug("生成客户订单结束======================================");

//			// 国际短信不需要循环处理，普通短信判断处理完成
//			if ("2".equals(product_type) || count.compareTo(BigDecimal.ZERO) <= 0) {
//				unDone = false;
//			}
//		}

		// 更新代理商账户统计表
		logger.debug("更新代理商账户统计表开始======================================");
		this.updateOemAgentAccountStatistics(agent_id, updateNum, product_type,0);
		logger.debug("更新代理商账户统计表结束======================================");

		logger.debug("oem代理商平台-客户管理-充值结束======================================");
		data.put("result", "success");
		data.put("msg", "充值成功!");

		return data;
	}


	@Override
    @Transactional(value = "message_master")
	public Map<String, Object> oemClientRecharge(OemClientRechargeRollBackDTO dto) {

		logger.debug("【OEM代理商客户充值】充值开始----------------------------");

		Map<String, Object> result = new HashMap<>();

		// 确定OEM代理商的短信池
        List<JsmsOemAgentPool> jsmsOemAgentPoolList = this.confirmOemAgentPool(dto.getAgentId(), 0, dto.getProductType(), dto.getOperatorCode(),
																		dto.getAreaCode(), dto.getDueTime(), dto.getUnitPrice());
        // 检查充值的数量是否充足
        if (!oemClientRecharegePreCheck(dto, jsmsOemAgentPoolList)){
            result.put("result", "fail");
            result.put("msg", "您充值的数量大于剩余的数量，请重新充值!");
            logger.debug("【OEM代理商客户充值】余额校验不通过，充值结束----------------------------");
            return result;
        }

		dto.setUpdateTime(new Date());
        // 存在多个OEM代理商短信池的时候取最后创建的那个短信池
        Long agentPoolId = jsmsOemAgentPoolList.get(jsmsOemAgentPoolList.size() - 1).getAgentPoolId();
		// 扣减OEM代理商的短信池
        this.updateForReduceAgentPoolRemainNum(dto, jsmsOemAgentPoolList);

        // 创建OEM代理商分发订单
        this.createOemAgentOrder(dto, agentPoolId, OEMAgentOrderType.OEM代理商分发.getValue());

        // 确定要充值的OEM客户短信池，不存在则创建
        List<JsmsOemClientPool> jsmsOemClientPoolList = this.confirmOemClientPool(dto.getClientId(), 0, dto.getProductType(), dto.getOperatorCode(),
                dto.getAreaCode(), dto.getDueTime(), dto.getUnitPrice());

        Long clientPoolId = jsmsOemClientPoolList.get(jsmsOemClientPoolList.size() - 1).getClientPoolId();
        // OEM客户短信池充值操作
        this.updateForAddClientPoolRemainNum(clientPoolId, dto);

        // 创建OEM客户购买订单
        this.createOemClientOrder(clientPoolId, dto, OrderType.充值.getValue());

        // 更新OEM代理商累计购买条数统计表
        this.updateOemAgentAccountStatistics(dto.getAgentId(), dto.getUpdateNum(), dto.getProductType());

        logger.debug("【OEM代理商客户充值】充值成功，结束----------------------------");
		result.put("result", "success");
		result.put("msg", "充值成功!");

		return result;
	}


    @Override
    @Transactional(value = "message_master")
    public R oemClientRecharge(List<OemClientRechargeRollBackDTO> dtoList) {

        logger.debug("【OEM代理商客户充值】充值开始----------------------------");

        for (OemClientRechargeRollBackDTO dto :dtoList) {
            // 确定OEM代理商的短信池
            List<JsmsOemAgentPool> jsmsOemAgentPoolList = this.confirmOemAgentPool(dto.getAgentId(), 0, dto.getProductType(), dto.getOperatorCode(),
                    dto.getAreaCode(), dto.getDueTime(), dto.getUnitPrice());
            // 检查充值的数量是否充足
            if (!oemClientRecharegePreCheck(dto, jsmsOemAgentPoolList)){
                logger.debug("【OEM代理商客户充值】余额校验不通过，充值结束----------------------------");
                return R.error("您充值的数量大于剩余的数量，请重新充值!");
            }

            dto.setUpdateTime(new Date());
            // 存在多个OEM代理商短信池的时候取最后创建的那个短信池
            Long agentPoolId = jsmsOemAgentPoolList.get(jsmsOemAgentPoolList.size() - 1).getAgentPoolId();
            // 扣减OEM代理商的短信池
            this.updateForReduceAgentPoolRemainNum(dto, jsmsOemAgentPoolList);

            // 创建OEM代理商分发订单
            this.createOemAgentOrder(dto, agentPoolId, OEMAgentOrderType.OEM代理商分发.getValue());

            // 确定要充值的OEM客户短信池，不存在则创建
            List<JsmsOemClientPool> jsmsOemClientPoolList = this.confirmOemClientPool(dto.getClientId(), 0, dto.getProductType(), dto.getOperatorCode(),
                    dto.getAreaCode(), dto.getDueTime(), dto.getUnitPrice());

            Long clientPoolId = jsmsOemClientPoolList.get(jsmsOemClientPoolList.size() - 1).getClientPoolId();
            // OEM客户短信池充值操作
            this.updateForAddClientPoolRemainNum(clientPoolId, dto);

            // 创建OEM客户购买订单
            this.createOemClientOrder(clientPoolId, dto, OrderType.充值.getValue());

            // 更新OEM代理商累计购买条数统计表
            this.updateOemAgentAccountStatistics(dto.getAgentId(), dto.getUpdateNum(), dto.getProductType());

        }

        logger.debug("【OEM代理商客户充值】充值成功，结束----------------------------");

        return R.ok("充值成功");
    }


	/**
	 * 更新 t_sms_oem_agent_account_statistics 表的累计卖出数量
	 * @param agentId
	 * @param purchaseNum
	 * @param productType
	 * @return
	 */
    private JsmsOemAgentAccountStatistics updateOemAgentAccountStatistics(Integer agentId, BigDecimal purchaseNum, Integer productType) {

        JsmsOemAgentAccountStatistics insertOrUpdateObj = new JsmsOemAgentAccountStatistics();
        insertOrUpdateObj.setAgentId(agentId);

        if(ProductType.行业.getValue().equals(productType)){
            insertOrUpdateObj.setHyRemainRebateNumber(purchaseNum.intValue());
        }else if(ProductType.营销.getValue().equals(productType)){
            insertOrUpdateObj.setYxRemainRebateNumber(purchaseNum.intValue());
        }else if(ProductType.国际.getValue().equals(productType)){
            insertOrUpdateObj.setGjRemainRebateAmount(purchaseNum);
        }else if(ProductType.验证码.getValue().equals(productType)){
            insertOrUpdateObj.setYzmRemainRebateNumber(purchaseNum.intValue());
        }else if(ProductType.通知.getValue().equals(productType)){
            insertOrUpdateObj.setTzRemainRebateNumber(purchaseNum.intValue());
        }

        JsmsOemAgentAccountStatistics oemAgentAccountStatistics = jsmsOemAgentAccountStatisticsService.getByAgentId(agentId);

        if (oemAgentAccountStatistics == null) {
            int insert = jsmsOemAgentAccountStatisticsService.insert(insertOrUpdateObj);
            logger.debug("【OEM代理商客户充值】增加OEM代理商帐户统计表 t_sms_oem_agent_account_statistics.insert = {} , insertObj --> {}",insert, JsonUtil.toJson(insertOrUpdateObj));
            return insertOrUpdateObj;
        } else {
            int update = jsmsOemAgentAccountStatisticsService.updateForAddPurchaseNumber(insertOrUpdateObj);
            logger.debug("【OEM代理商客户充值】更新OEM代理商帐户统计表 t_sms_oem_agent_account_statistics.update = {} , updateObj --> {}",update, JsonUtil.toJson(insertOrUpdateObj));
            return insertOrUpdateObj;
        }
    }


    private void createOemClientOrder(Long clientPoolId, OemClientRechargeRollBackDTO params, Integer orderType) {

        JsmsOemClientOrder jsmsOemClientOrder = new JsmsOemClientOrder();

        Long oemClientOrderId = this.getClientOrderId();
        jsmsOemClientOrder.setOrderId(oemClientOrderId);
        if(OrderType.充值.getValue().equals(orderType)){
            jsmsOemClientOrder.setOrderType(OrderType.充值.getValue());
        }else {
            jsmsOemClientOrder.setOrderType(OrderType.回退.getValue());
        }
        jsmsOemClientOrder.setOrderNo(oemClientOrderId);
        jsmsOemClientOrder.setClientPoolId(clientPoolId);
        jsmsOemClientOrder.setProductType(params.getProductType());
        jsmsOemClientOrder.setClientId(params.getClientId());
        jsmsOemClientOrder.setAgentId(params.getAgentId());
        jsmsOemClientOrder.setDueTime(params.getDueTime());
        jsmsOemClientOrder.setOperatorCode(params.getOperatorCode());
        jsmsOemClientOrder.setAreaCode(params.getAreaCode());
        jsmsOemClientOrder.setCreateTime(params.getUpdateTime());
        jsmsOemClientOrder.setRemark(null);

        if(ProductType.国际.getValue().equals(params.getProductType())) {
            jsmsOemClientOrder.setOrderNumber(null);
            jsmsOemClientOrder.setOrderPrice(params.getUpdateNum());
        }else {
            jsmsOemClientOrder.setOrderNumber(params.getUpdateNum().intValue());
            if(params.getUnitPrice() == null) {
                jsmsOemClientOrder.setUnitPrice(BigDecimal.ZERO);
            } else {
                jsmsOemClientOrder.setUnitPrice(params.getUnitPrice());
            }
        }

        this.jsmsOemClientOrderService.insert(jsmsOemClientOrder);

        logger.debug("【OEM代理商客户充值】:生成OEM客户订单{}成功，订单类型为{}", oemClientOrderId, OrderType.getDescByValue(orderType));
    }

    private void updateForAddClientPoolRemainNum(Long clientPoolId, OemClientRechargeRollBackDTO dto) {
	    jsmsOemClientPoolService.updateForAddClientPoolRemainNum(clientPoolId, dto.getUpdateNum(), dto.getProductType(), dto.getUpdateTime());
        logger.debug("【OEM代理商客户充值】:充值OEM客户短信池{}，充值{}条数（金额），产品类型为{}", clientPoolId, dto.getUpdateNum(), ProductType.getDescByValue(dto.getProductType()));
    }


    private List<JsmsOemClientPool> confirmOemClientPool(String clientId, Integer status, Integer productType, Integer operatorCode, Integer areaCode,
                                      Date dueTime, BigDecimal unitPrice) {

        JsmsOemClientPool poolInfo = new JsmsOemClientPool();
        poolInfo.setClientId(clientId);
        poolInfo.setProductType(productType);
        poolInfo.setDueTime(dueTime);
        poolInfo.setOperatorCode(operatorCode);
        poolInfo.setAreaCode(areaCode);
        poolInfo.setUnitPrice(unitPrice);
        poolInfo.setStatus(0);
        List<JsmsOemClientPool> jsmsOemClientPoolList = jsmsOemClientPoolService.getListByClientPoolInfo(poolInfo);

        // 如果OEM客户的短信池存在直接返回；不存在则创建一个
        if(jsmsOemClientPoolList != null && jsmsOemClientPoolList.size() > 0) {
            return jsmsOemClientPoolList;
        }else {
            if(ProductType.国际.getValue().equals(productType)) {
                poolInfo.setTotalNumber(null);
                poolInfo.setRemainNumber(null);
                poolInfo.setTotalAmount(BigDecimal.ZERO);
                poolInfo.setRemainAmount(BigDecimal.ZERO);
            }else {
                poolInfo.setTotalNumber(0);
                poolInfo.setRemainNumber(0);
                poolInfo.setTotalAmount(null);
                poolInfo.setRemainAmount(null);
            }
            poolInfo.setUpdateTime(dueTime);
            poolInfo.setRemark(null);

            jsmsOemClientPoolService.insert(poolInfo);
            List<JsmsOemClientPool> list = new ArrayList<>();
            list.add(jsmsOemClientPoolService.getByClientPoolId(poolInfo.getClientPoolId()));
            return list;
        }

    }

    private void updateForReduceAgentPoolRemainNum(OemClientRechargeRollBackDTO dto, List<JsmsOemAgentPool> jsmsOemAgentPoolList) {
		BigDecimal updateNum = dto.getUpdateNum();
		for (JsmsOemAgentPool jsmsOemAgentPool : jsmsOemAgentPoolList) {
            Long agentPoolId = jsmsOemAgentPool.getAgentPoolId();
            BigDecimal reduceNum = jsmsOemAgentPoolService.updateForReduceAgentPoolRemainNum(agentPoolId, updateNum, dto.getProductType(), dto.getUpdateTime());

			updateNum = updateNum.subtract(reduceNum);
            logger.debug("【OEM代理商客户充值】:扣减OEM代理商短信池{}，扣减{}条数（金额），产品类型为{}", agentPoolId, reduceNum, ProductType.getDescByValue(dto.getProductType()));
            // 一个池没有扣完继续扣减下一个
			if(updateNum.compareTo(BigDecimal.ZERO) != 0){
			    logger.debug("【OEM代理商客户充值】扣减一个短信池后未扣完需要扣减的条数，继续扣减下一个短信池");
			    continue;
            }else {
			    break;
            }

		}

		if(updateNum.compareTo(BigDecimal.ZERO) != 0){
		    logger.warn("【OEM代理商客户充值】扣减OEM代理商短信池数量时剩余数量不够扣，执行回滚");
		    throw new JsmsOemAgentPoolException("您充值的数量大于剩余的数量，请重新充值!");
        }

    }

    private void updateForAddAgentPoolRemainNum(OemClientRechargeRollBackDTO dto, Long agentPoolId) {
	    jsmsOemAgentPoolService.updateForAddAgentPoolRemainNum(agentPoolId, dto.getUpdateNum(), dto.getProductType(), dto.getUpdateTime());
        logger.debug("【OEM代理商客户回退】:回退OEM代理商短信池{}，增加{}条数（金额），产品类型为{}", agentPoolId, dto.getUpdateNum(), ProductType.getDescByValue(dto.getProductType()));
    }


    /**
     * 创建OEM代理商订单
     * @param dto
     * @param agentPoolId
     * @param oemAgentOrderType
     */
    private void createOemAgentOrder(OemClientRechargeRollBackDTO dto, Long agentPoolId, Integer oemAgentOrderType) {


        // 生成订单号
        Long oemAgentOrderId = OrderUtils.getAgentOrderId();

        // 生成订单信息
        JsmsOemAgentOrder oemAgentPurchaseOrder = new JsmsOemAgentOrder();
        oemAgentPurchaseOrder.setOrderId(oemAgentOrderId);
        if(OEMAgentOrderType.OEM代理商分发.getValue().equals(oemAgentOrderType)){
            oemAgentPurchaseOrder.setOrderType(OEMAgentOrderType.OEM代理商分发.getValue());
        }else if(OEMAgentOrderType.OEM代理商回退.getValue().equals(oemAgentOrderType)){
            oemAgentPurchaseOrder.setOrderType(OEMAgentOrderType.OEM代理商回退.getValue());
        }else {
            throw new JsmsOemAgentOrderException("当前方法只能创建‘OEM代理商分发’或者‘OEM代理商回退’订单");
        }
        oemAgentPurchaseOrder.setProductId(null); // 订单类型为0时有值
        oemAgentPurchaseOrder.setProductCode(null);
        oemAgentPurchaseOrder.setProductName(null);
        oemAgentPurchaseOrder.setUnitPrice(null);
        oemAgentPurchaseOrder.setOrderNo(oemAgentOrderId);
        oemAgentPurchaseOrder.setAgentPoolId(agentPoolId);
        oemAgentPurchaseOrder.setCreateTime(dto.getUpdateTime());
        oemAgentPurchaseOrder.setRemark(null);
        JsmsAccount jsmsAccount = jsmsAccountService.getByClientId(dto.getClientId());
        oemAgentPurchaseOrder.setName(jsmsAccount.getName());

        // 将页面信息放到生成的订单中
        oemAgentPurchaseOrder.setProductType(dto.getProductType());
        oemAgentPurchaseOrder.setOperatorCode(dto.getOperatorCode());
        oemAgentPurchaseOrder.setAreaCode(dto.getAreaCode());
        oemAgentPurchaseOrder.setAgentId(dto.getAgentId());
        oemAgentPurchaseOrder.setClientId(dto.getClientId());
        oemAgentPurchaseOrder.setDueTime(dto.getDueTime());

        if(ProductType.国际.getValue().equals(dto.getProductType())){
            oemAgentPurchaseOrder.setOrderNumber(null);
            oemAgentPurchaseOrder.setOrderAmount(dto.getUpdateNum());
            oemAgentPurchaseOrder.setProductPrice(dto.getUpdateNum());
        }else{
            oemAgentPurchaseOrder.setOrderNumber(dto.getUpdateNum().intValue());
            oemAgentPurchaseOrder.setProductPrice(null);
            if(dto.getUnitPrice() != null){
                oemAgentPurchaseOrder.setUnitPrice(dto.getUnitPrice());
            }else {
                oemAgentPurchaseOrder.setUnitPrice(BigDecimal.ZERO);
            }
            // 算出订单金额
            oemAgentPurchaseOrder.setOrderAmount(dto.getUpdateNum().multiply(dto.getUnitPrice()));
        }

        this.jsmsOemAgentOrderService.insert(oemAgentPurchaseOrder);


        logger.debug("【OEM代理商客户充值】:生成OEM代理订单{}成功，订单类型={}", oemAgentOrderId, OEMAgentOrderType.getDescByValue(oemAgentOrderType));
    }



    /**
     * 根据充值信息确认要操作的OEM代理商短信池
     * @param agentId
     * @param status
     * @param productType
     * @param operatorCode
     * @param areaCode
     * @param dueTime
     * @param unitPrice
     * @return
     */
	private List<JsmsOemAgentPool> confirmOemAgentPool(Integer agentId, Integer status, Integer productType, Integer operatorCode, Integer areaCode,
                                                 Date dueTime, BigDecimal unitPrice) {
        JsmsOemAgentPool poolInfo = new JsmsOemAgentPool();
        poolInfo.setAgentId(agentId);
        poolInfo.setStatus(status);
        poolInfo.setProductType(productType);
        poolInfo.setOperatorCode(operatorCode);
        poolInfo.setAreaCode(areaCode);
        poolInfo.setDueTime(dueTime);
        poolInfo.setUnitPrice(unitPrice);

        return jsmsOemAgentPoolService.getListByAgentPoolInfo(poolInfo);
    }

    private boolean oemClientRecharegePreCheck(OemClientRechargeRollBackDTO rechargeInfo, List<JsmsOemAgentPool> jsmsOemAgentPoolList) {
		BigDecimal remainNum = BigDecimal.ZERO;
        Integer productType = rechargeInfo.getProductType();
        for (JsmsOemAgentPool oemAgentPool : jsmsOemAgentPoolList) {
			if (ProductType.国际.getValue().equals(productType)) {
				// 国际产品
                remainNum = remainNum.add(oemAgentPool.getRemainAmount());
			} else {
				// 普通产品
                remainNum = remainNum.add(new BigDecimal(oemAgentPool.getRemainNumber()));
			}
		}

        if (rechargeInfo.getUpdateNum().compareTo(remainNum) == 1) {
            return false;
        }
        return true;
    }

    @Override
    @Transactional(value = "message_master")
    public Map<String, Object> oemClientRollback(OemClientRechargeRollBackDTO dto) {
        Map<String, Object> data = new HashMap<>();


        logger.debug("【OEM代理商客户回退】回退开始----------------------------");
        List<JsmsOemClientPool> jsmsOemClientPoolList = this.confirmOemClientPool(dto.getClientId(), 0, dto.getProductType(), dto.getOperatorCode(),
                dto.getAreaCode(), dto.getDueTime(), dto.getUnitPrice());

        // 检查回退的数量是否大于客户剩余的数量
        if(!this.oemClientRollbackPreCheck(dto, jsmsOemClientPoolList)){
            data.put("result", "fail");
            data.put("msg", "您回退的数量大于客户剩余的数量，请重新填写数量!");
            logger.debug("【OEM代理商客户回退】余额校验不通过，回退结束----------------------------");
            return data;
        }

		dto.setUpdateTime(new Date());
        // 确定OEM代理商的短信池（如果存在多个池子取大的那个池子）
        List<JsmsOemAgentPool> jsmsOemAgentPoolList = this.confirmOemAgentPool(dto.getAgentId(), 0, dto.getProductType(),
                dto.getOperatorCode(), dto.getAreaCode(), dto.getDueTime(), dto.getUnitPrice());
        Long agentPoolId = jsmsOemAgentPoolList.get(jsmsOemAgentPoolList.size() - 1).getAgentPoolId();
        // 回退OEM代理商短信池条数
        this.updateForAddAgentPoolRemainNum(dto, agentPoolId);

        // 生成OEM代理商回退订单
        this.createOemAgentOrder(dto, agentPoolId, OEMAgentOrderType.OEM代理商回退.getValue());

        Long clientPoolId = jsmsOemClientPoolList.get(jsmsOemClientPoolList.size() - 1).getClientPoolId();
        // 扣减OEM客户的短信池剩余数量（国际为剩余金额）
        this.updateForReduceClientPoolRemainNum(jsmsOemClientPoolList, dto);

        // 生产OEM客户回退订单
        this.createOemClientOrder(clientPoolId, dto, OrderType.回退.getValue());

        data.put("result", "success");
        data.put("msg", "回退成功!");

        return data;
    }

    @Override
    @Transactional(value = "message_master")
    public R oemClientRollback(List<OemClientRechargeRollBackDTO> dtoList) {
	    for (OemClientRechargeRollBackDTO dto : dtoList) {
            List<JsmsOemClientPool> jsmsOemClientPoolList = this.confirmOemClientPool(dto.getClientId(), 0, dto.getProductType(), dto.getOperatorCode(),
                    dto.getAreaCode(), dto.getDueTime(), dto.getUnitPrice());

            // 检查回退的数量是否大于客户剩余的数量
            if(!this.oemClientRollbackPreCheck(dto, jsmsOemClientPoolList)){
                logger.debug("【OEM代理商客户回退】余额校验不通过，回退结束---------"+ JsonUtil.toJson(dto) + ",客户短信池:" + JsonUtil.toJson(jsmsOemClientPoolList));
                return R.error("您回退的数量大于客户剩余的数量，请重新填写数量!");
            }

            dto.setUpdateTime(new Date());
            // 确定OEM代理商的短信池（如果存在多个池子取大的那个池子）
            List<JsmsOemAgentPool> jsmsOemAgentPoolList = this.confirmOemAgentPool(dto.getAgentId(), 0, dto.getProductType(),
                    dto.getOperatorCode(), dto.getAreaCode(), dto.getDueTime(), dto.getUnitPrice());
            Long agentPoolId = jsmsOemAgentPoolList.get(jsmsOemAgentPoolList.size() - 1).getAgentPoolId();
            // 回退OEM代理商短信池条数
            this.updateForAddAgentPoolRemainNum(dto, agentPoolId);

            // 生成OEM代理商回退订单
            this.createOemAgentOrder(dto, agentPoolId, OEMAgentOrderType.OEM代理商回退.getValue());

            Long clientPoolId = jsmsOemClientPoolList.get(jsmsOemClientPoolList.size() - 1).getClientPoolId();
            // 扣减OEM客户的短信池剩余数量（国际为剩余金额）
            this.updateForReduceClientPoolRemainNum(jsmsOemClientPoolList, dto);

            // 生产OEM客户回退订单
            this.createOemClientOrder(clientPoolId, dto, OrderType.回退.getValue());
        }
        return R.ok("回退成功");
    }

    private void updateForReduceClientPoolRemainNum(List<JsmsOemClientPool> jsmsOemClientPoolList, OemClientRechargeRollBackDTO dto) {

        BigDecimal remainNeedToReduce = dto.getUpdateNum();
        Integer productType = dto.getProductType();
        Date updateTime = dto.getUpdateTime();
        for (JsmsOemClientPool jsmsOemClientPool : jsmsOemClientPoolList) {
            Long clientPoolId = jsmsOemClientPool.getClientPoolId();
            BigDecimal reduceNum = jsmsOemClientPoolService.updateForReduceClientPoolRemainNum(clientPoolId, remainNeedToReduce, productType, updateTime);

            remainNeedToReduce = remainNeedToReduce.subtract(reduceNum);
            logger.debug("【OEM代理商客户回退】:扣减OEM客户短信池{}，扣减{}条数（金额），产品类型为{}", clientPoolId, reduceNum, ProductType.getDescByValue(productType));
            if(remainNeedToReduce.compareTo(BigDecimal.ZERO) != 0){
                continue;
            }else{
                break;
            }
        }


        if(remainNeedToReduce.compareTo(BigDecimal.ZERO) != 0){
            throw new JsmsOemAgentPoolException("您回退的数量大于客户剩余的数量，请重新填写数量!");
        }
    }

    private boolean oemClientRollbackPreCheck(OemClientRechargeRollBackDTO dto, List<JsmsOemClientPool> jsmsOemClientPoolList) {

        BigDecimal remainNum = BigDecimal.ZERO;
        Integer productType = dto.getProductType();
        for (JsmsOemClientPool jsmsOemClientPool : jsmsOemClientPoolList) {
            BigDecimal temp;
            if(ProductType.国际.getValue().equals(productType)) {
                temp = jsmsOemClientPool.getRemainAmount() == null ? BigDecimal.ZERO : jsmsOemClientPool.getRemainAmount();
                remainNum = remainNum.add(temp);
            }else {
                temp = jsmsOemClientPool.getRemainNumber() == null ? BigDecimal.ZERO : new BigDecimal(jsmsOemClientPool.getRemainNumber());
                remainNum = remainNum.add(temp);
            }
        }

        if (dto.getUpdateNum().compareTo(remainNum) == 1) {
            return false;
        }
        return true;
    }

    @Override
	public Map<String, Object> confirmRollback(Map<String, String> params) {
		Map<String, Object> data = new HashMap<String, Object>();

		logger.debug("oem代理商平台-客户管理-回退开始======================================");

		// 检查充值的数量是否充足
		String agent_id = params.get("agent_id");
		String client_id = params.get("client_id");

		Map<String, Object> sms_acc_map = this.masterDao.getOneInfo("customer.querySmsAccount", params);
		String clientName = sms_acc_map.get("name").toString();

		String product_type = params.get("product_type").toString();
		// String due_time = params.get("due_time").toString();
		BigDecimal updateNum = new BigDecimal(params.get("updateNum").toString());
		BigDecimal remainNum = null;

		logger.debug("回退校验开始======================================");
		Map<String, Object> remain_map = this.masterDao.getOneInfo("customer.queryClientPoolRemainNum", params);
		if (remain_map == null)
			remain_map = new HashMap<>();
		if (remain_map.get("remain_number") == null)
			remain_map.put("remain_number", 0);
		if (Integer.parseInt(remain_map.get("remain_number").toString()) < 0)
			remain_map.put("remain_number", 0);
		if (remain_map.get("remain_amount") == null)
			remain_map.put("remain_amount", 0);

		if (!"2".equals(product_type)) {
			// 普通产品
			remainNum = new BigDecimal(remain_map.get("remain_number").toString());
		} else {
			// 国际产品
			remainNum = new BigDecimal(remain_map.get("remain_amount").toString());
		}
		if (updateNum.compareTo(remainNum) == 1) {
			data.put("result", "fail");
			data.put("msg", "您回退的数量大于客户剩余的数量，请重新填写数量!");
			return data;
		}
		logger.debug("回退校验结束======================================");

		// 生成代理商订单
		Date nowdate = new Date();
		String order_type = "2";// 0：OEM代理商购买，1：OEM代理商分发，2：OEM代理商回退

		// Add by lpjLiu 查询出客户短信池，并按单价由高到低排序
//		List<Map<String, Object>> list = null;
//		if (!"2".equals(product_type)) {
//			list = this.masterDao.getSearchList("customer.queryClientPoolRemainNumOrderByUnitPrice", params);
//		}

		StringBuffer agent_order_no = new StringBuffer("");
		StringBuffer client_order_no = new StringBuffer("");

//		BigDecimal count = updateNum; // 已处理数量
//		boolean unDone = true;
//		while (unDone) {
//			// Add by lpjLiu 根据客户池的单价重新设置参数
//			if (!"2".equals(product_type)) {
//				BigDecimal tempNum = null;
//				for (Map<String, Object> item : list) {
//					Boolean used = (Boolean) item.get("used");
//					if (used != null && used) {
//						continue;
//					}
//
//					// 获取当前池的条数
//					BigDecimal remainNumber = new BigDecimal(item.get("remain_number").toString());
//
//					// 跳过为空若已经使用
//					if (remainNumber.compareTo(BigDecimal.ZERO) <= 0) {
//						item.put("used", true);
//						continue;
//					}
//
//					if (remainNumber.compareTo(count) >= 0) {
//						tempNum = count;
//					} else {
//						tempNum = remainNumber;
//					}
//
//					// 减去已处理数量
//					count = count.subtract(tempNum);
//
//					// 放入下面需要处理的参数
//					params.put("unit_price", item.get("unit_price").toString());
//					params.put("updateNum", tempNum.toString());
//					item.put("used", true);
//					break;
//				}
//			}

			// 增加代理商的短信池
			logger.debug("添加代理商短信池开始======================================");
			// Add by lpjLiu 2017-05-23 获取代理商短信池的Id
			StringBuffer agent_pool_id_sb = new StringBuffer("");
			this.updateAgentPoolRemainNum(params, "add", agent_pool_id_sb);
			logger.debug("添加代理商短信池结束======================================");

			logger.debug("生成代理商订单开始======================================");
			this.createAgentOrder(params, agent_id, nowdate, client_id, clientName, order_type, agent_order_no,
					agent_pool_id_sb);
			logger.debug("生成代理商订单结束======================================");

			// 减少客户的短信池
			logger.debug("减少客户短信池开始======================================");
			// Add by lpjLiu 2017-05-23 获取客户短信池的Id
			StringBuffer client_pool_id_sb = new StringBuffer("");
			this.reduceClientPoolRemainNum(params, nowdate, client_pool_id_sb);
			logger.debug("减少客户短信池结束======================================");

			// 生成客户订单
			logger.debug("生成客户订单开始======================================");
			this.createClientOrder(params, nowdate, order_type, client_order_no, client_pool_id_sb);
			logger.debug("生成客户订单结束======================================");


//		// 更新代理商账户统计表
//		logger.debug("更新代理商账户统计表开始======================================");
//		this.updateOemAgentAccountStatistics(agent_id, updateNum, product_type,1);
//		logger.debug("更新代理商账户统计表结束======================================");

		// 国际短信不需要循环处理，普通短信判断处理完成
//			if ("2".equals(product_type) || count.compareTo(BigDecimal.ZERO) <= 0) {
//				unDone = false;
//			}
//		}

		logger.debug("oem代理商平台-客户管理-回退结束======================================");

		data.put("result", "success");
		data.put("msg", "回退成功!");

		return data;
	}

	// 增加或者减少代理商短信池的数量
    @Deprecated
	private void updateAgentPoolRemainNum(Map<String, String> order_params, String flag, StringBuffer agent_pool_id) {
		Map<String, Object> agent_pool_map = new HashMap<>();
		String product_type = order_params.get("product_type");

		agent_pool_map.put("agent_id", order_params.get("agent_id"));
		agent_pool_map.put("product_type", product_type);
		agent_pool_map.put("due_time", order_params.get("due_time"));
		agent_pool_map.put("unit_price", order_params.get("unit_price"));
		agent_pool_map.put("operator_code",order_params.get("operator_code"));
		agent_pool_map.put("area_code",order_params.get("area_code"));
		if (!"2".equals(product_type)) {
			agent_pool_map.put("remain_number", order_params.get("updateNum"));
			agent_pool_map.put("remain_amount", null);

			// Add: lpjLiu 2017-05-23 增加单价查询条件
			agent_pool_map.put("unit_price", order_params.get("unit_price"));
		} else {
			agent_pool_map.put("remain_amount", null);
			agent_pool_map.put("remain_amount", order_params.get("updateNum"));
		}
		if ("add".equals(flag)) {
			this.masterDao.update("customer.addAgentPoolRemainNum", agent_pool_map);
		} else {
			this.masterDao.update("customer.reduceAgentPoolRemainNum", agent_pool_map);
		}

		// Add: lpjLiu 2017-05-23 查询代理商短信池Id
		Map<String, Object> result = this.masterDao.selectOne("customer.queryAgentPoolId", agent_pool_map);
		agent_pool_id.append(result.get("agent_pool_id"));
	}

	// 增加客户短信池的数量
    @Deprecated
	private void addClientPoolRemainNum(Map<String, String> order_params, Date nowdate, StringBuffer client_pool_id) {
		String product_type = order_params.get("product_type");

		// Add by lpjLiu 2017-05-23 若是国际短信删除这个条件, 普通短信增加单价纬度查询
		BigDecimal unit_price = BigDecimal.ZERO;
		Object obj = order_params.get("unit_price");
		if ("2".equals(product_type)) {
			if (obj != null) {
				order_params.remove("unit_price");
			}
		} else {
			if (obj == null) {
				order_params.put("unit_price", unit_price.toString());
			}
		}

		Map<String, Object> num_map = this.masterDao.getOneInfo("customer.queryClientPoolNum", order_params);
		int num = Integer.valueOf(num_map.get("num").toString());
		if (num == 0) {
			// 插入数据
			Map<String, Object> client_pool_map = new HashMap<>();
			client_pool_map.put("client_id", order_params.get("client_id"));
			client_pool_map.put("product_type", product_type);
			client_pool_map.put("due_time", order_params.get("due_time"));
			client_pool_map.put("operator_code",order_params.get("operator_code"));
			client_pool_map.put("area_code",order_params.get("area_code"));
			client_pool_map.put("status", 0);// '状态，0：正常，1：停用',

			// Add by lpjLiu 2017-05-23 获取客户短信池ID
			client_pool_map.put("client_pool_id", null);

			if (!"2".equals(product_type)) {
				// 普通产品
				client_pool_map.put("total_number", order_params.get("updateNum")); // '普通短信累计总条数，单位：条'
				client_pool_map.put("remain_number", order_params.get("updateNum"));// '普通短信剩余条数，单位：条'
				client_pool_map.put("total_amount", null);// '国际短信累计总金额，单位：元'
				client_pool_map.put("remain_amount", null);// '国际短信剩余金额，单位：元'

				// Add by lpjLiu 2017-05-23 增加单价
				client_pool_map.put("unit_price", order_params.get("unit_price"));
			} else {
				// 国际产品
				client_pool_map.put("total_number", null); // '普通短信累计总条数，单位：条'
				client_pool_map.put("remain_number", null);// '普通短信剩余条数，单位：条'
				client_pool_map.put("total_amount", order_params.get("updateNum"));// '国际短信累计总金额，单位：元'
				client_pool_map.put("remain_amount", order_params.get("updateNum"));// '国际短信剩余金额，单位：元'
			}

			client_pool_map.put("update_time", nowdate);
			client_pool_map.put("remark", null);

			this.masterDao.insert("customer.insertOemClientPool", client_pool_map);

			// Add by lpjLiu 2017-05-23 获取客户短信池ID
			client_pool_id.append(client_pool_map.get("client_pool_id"));
		} else {
			// 更新数据
			Map<String, Object> add_client_pool_map = new HashMap<>();

			add_client_pool_map.put("client_id", order_params.get("client_id"));
			add_client_pool_map.put("product_type", product_type);
			add_client_pool_map.put("due_time", order_params.get("due_time"));
			add_client_pool_map.put("operator_code",order_params.get("operator_code"));
			add_client_pool_map.put("area_code",order_params.get("area_code"));
			// Add by lpjLiu 2017-05-23 状态置为正常
			add_client_pool_map.put("status", 0);

			if (!"2".equals(product_type)) {
				// 普通产品
				add_client_pool_map.put("total_number", order_params.get("updateNum")); // '普通短信累计总条数，单位：条'
				add_client_pool_map.put("remain_number", order_params.get("updateNum"));// '普通短信剩余条数，单位：条'
				add_client_pool_map.put("total_amount", null);// '国际短信累计总金额，单位：元'
				add_client_pool_map.put("remain_amount", null);// '国际短信剩余金额，单位：元'

				// Add by lpjLiu 2017-05-23 增加单价
				add_client_pool_map.put("unit_price", order_params.get("unit_price"));
			} else {
				// 国际产品
				add_client_pool_map.put("total_number", null); // '普通短信累计总条数，单位：条'
				add_client_pool_map.put("remain_number", null);// '普通短信剩余条数，单位：条'
				add_client_pool_map.put("total_amount", order_params.get("updateNum"));// '国际短信累计总金额，单位：元'
				add_client_pool_map.put("remain_amount", order_params.get("updateNum"));// '国际短信剩余金额，单位：元'
			}

			add_client_pool_map.put("update_time", nowdate);

			this.masterDao.update("customer.addclientPoolRemainNum", add_client_pool_map);

			// Add by lpjLiu 2017-05-23 获取客户短信池ID
			Map<String, Object> result = this.masterDao.getOneInfo("customer.queryClientPoolId", add_client_pool_map);
			client_pool_id.append(result.get("client_pool_id"));
		}
	}

	// 减少客户短信池的数量
	private void reduceClientPoolRemainNum(Map<String, String> order_params, Date nowdate,
			StringBuffer client_pool_id) {

		String product_type = order_params.get("product_type");
		Map<String, Object> add_client_pool_map = new HashMap<>();

		add_client_pool_map.put("client_id", order_params.get("client_id"));
		add_client_pool_map.put("product_type", product_type);
		add_client_pool_map.put("due_time", order_params.get("due_time"));
		add_client_pool_map.put("operator_code",order_params.get("operator_code"));
		add_client_pool_map.put("area_code",order_params.get("area_code"));
		// Add by lpjLiu 2017-05-23 若是国际短信删除这个条件, 普通短信增加单价纬度查询
		BigDecimal unit_price = BigDecimal.ZERO;
		Object obj = order_params.get("unit_price");
		if ("2".equals(product_type)) {
			if (obj != null) {
				order_params.remove("unit_price");
			}
		} else {
			if (obj == null) {
				order_params.put("unit_price", unit_price.toString());
			}
		}

		if (!"2".equals(product_type)) {
			// 普通产品
			add_client_pool_map.put("total_number", order_params.get("updateNum")); // '普通短信累计总条数，单位：条'
			add_client_pool_map.put("remain_number", order_params.get("updateNum"));// '普通短信剩余条数，单位：条'
			add_client_pool_map.put("total_amount", null);// '国际短信累计总金额，单位：元'
			add_client_pool_map.put("remain_amount", null);// '国际短信剩余金额，单位：元'

			// Add by lpjLiu 2017-05-23 增加单价
			add_client_pool_map.put("unit_price", order_params.get("unit_price"));
		} else {
			// 国际产品
			add_client_pool_map.put("total_number", null); // '普通短信累计总条数，单位：条'
			add_client_pool_map.put("remain_number", null);// '普通短信剩余条数，单位：条'
			add_client_pool_map.put("total_amount", order_params.get("updateNum"));// '国际短信累计总金额，单位：元'
			add_client_pool_map.put("remain_amount", order_params.get("updateNum"));// '国际短信剩余金额，单位：元'
		}

		add_client_pool_map.put("update_time", nowdate);

		int updateNum = this.masterDao.update("customer.reduceclientPoolRemainNum", add_client_pool_map);
		if (updateNum == 0) {
			throw new RuntimeException("剩余的数量不足，不能回退");
		}

		// Add by lpjLiu 2017-05-23 获取客户短信池ID
		Map<String, Object> result = this.masterDao.getOneInfo("customer.queryClientPoolId", add_client_pool_map);
		client_pool_id.append(result.get("client_pool_id"));
	}

	// 创建订单
    @Deprecated
	private void createAgentOrder(Map<String, String> order_params, String agent_id, Date nowdate, String clientid,
			String clientName, String order_type, StringBuffer agent_order_no, StringBuffer agent_pool_id) {

		String product_type = order_params.get("product_type").toString();

		Map<String, Object> agent_order_map = new HashMap<>();

		Long order_id_str = OrderUtils.getAgentOrderId();

		agent_order_map.put("order_id", order_id_str);
		agent_order_map.put("order_type", order_type); // 0：OEM代理商购买，1：OEM代理商分发，2：OEM代理商回退
		agent_order_map.put("product_id", null); // 订单类型为0时有值
		agent_order_map.put("product_code", null);
		agent_order_map.put("product_type", product_type);
		agent_order_map.put("operator_code",order_params.get("operator_code"));
		agent_order_map.put("area_code",order_params.get("area_code"));
		agent_order_map.put("product_name", null);
		agent_order_map.put("unit_price", null);

		// Add by lpjLiu 2017-05-23 给订单编号填值
		if (agent_order_no.length() <= 0) {
			agent_order_no.append(order_id_str);
		}
		agent_order_map.put("order_no", agent_order_no.toString());

		// Add by lpjLiu 2017-05-23 设置客户池Id
		agent_order_map.put("agent_pool_id", agent_pool_id.toString());

		if (!"2".equals(product_type)) {
			agent_order_map.put("order_number", order_params.get("updateNum"));
			// agent_order_map.put("order_amount", null);

			// OEM代理商国际产品包优惠前的价格,以此价格送入代理商短信池
			agent_order_map.put("product_price", null);

			// Add by lpjLiu 2017-05-23 给订单编号填值
			Object obj = order_params.get("unit_price");
			if (obj == null) {
				order_params.put("unit_price", BigDecimal.ZERO.toString());
			}
			agent_order_map.put("unit_price", order_params.get("unit_price"));

			// 算出订单金额
			BigDecimal order_amount = new BigDecimal(agent_order_map.get("order_number").toString())
					.multiply(new BigDecimal(agent_order_map.get("unit_price").toString()));
			agent_order_map.put("order_amount", order_amount);

		} else {
			agent_order_map.put("order_number", null);
			agent_order_map.put("order_amount", order_params.get("updateNum"));
			agent_order_map.put("product_price", order_params.get("updateNum")); // 国际产品的
		}

		agent_order_map.put("agent_id", agent_id);
		agent_order_map.put("client_id", clientid); // 订单类型为1、2时填用户帐号，订单类型为0时填'00000'
		agent_order_map.put("name", clientName); // 订单类型为1、2时填用户名称，订单类型为0时填'云之讯'

		agent_order_map.put("due_time", order_params.get("due_time"));
		agent_order_map.put("create_time", nowdate);
		agent_order_map.put("remark", null);

		this.masterDao.insert("sms.createOrder", agent_order_map);
	}

	// 创建客户订单
	private void createClientOrder(Map<String, String> order_params, Date nowdate, String order_type,
			StringBuffer client_order_no, StringBuffer client_pool_id) {
		Map<String, Object> client_order_map = new HashMap<>();
		String product_type = order_params.get("product_type");

		Long order_id = this.getClientOrderId();
		client_order_map.put("order_id", order_id);
		client_order_map.put("product_type", product_type);
		client_order_map.put("order_type", order_type); // '订单类型，1：OEM代理商分发，2：OEM代理商回退'

		// Add by lpjLiu 2017-05-23 给订单编号填值
		if (client_order_no.length() <= 0) {
			client_order_no.append(order_id);
		}
		client_order_map.put("order_no", client_order_no.toString());

		// Add by lpjLiu 2017-05-23 设置客户池Id
		client_order_map.put("client_pool_id", client_pool_id.toString());

		if (!"2".equals(product_type)) {
			// 普通产品
			client_order_map.put("order_number", order_params.get("updateNum"));// '普通订单短信条数，单位：条'
			// client_order_map.put("order_price", null);// '国际订单买价，单位：元'

			// Add by lpjLiu 2017-05-23 增加单价
			Object obj = order_params.get("unit_price");
			if (obj == null) {
				order_params.put("unit_price", BigDecimal.ZERO.toString());
			}
			client_order_map.put("unit_price", order_params.get("unit_price"));

			// 算出订单金额
			BigDecimal order_amount = new BigDecimal(client_order_map.get("order_number").toString())
					.multiply(new BigDecimal(client_order_map.get("unit_price").toString()));
			client_order_map.put("order_amount", order_amount);
		} else {
			client_order_map.put("order_number", null);// '普通订单短信条数，单位：条'
			client_order_map.put("order_price", order_params.get("updateNum"));// '国际订单买价，单位：元'
		}

		client_order_map.put("client_id", order_params.get("client_id"));// '用户帐号'
		client_order_map.put("agent_id", order_params.get("agent_id"));// '所属代理商id'
		client_order_map.put("due_time", order_params.get("due_time"));// '到期时间'
		client_order_map.put("operator_code",order_params.get("operator_code"));
		client_order_map.put("area_code",order_params.get("area_code"));
		client_order_map.put("create_time", nowdate);// '创建时间'
		client_order_map.put("remark", null);// '备注'

		this.masterDao.insert("customer.createClientOrder", client_order_map);
	}

	// 组装orderID
	private synchronized Long getClientOrderId() {

		Date date = new Date();
		int num = 0;
		String orderIdPre = DateUtils.formatDate(date, "yyMMdd") + DateUtils.formatDate(date, "HHmm")
				+ ConfigUtils.platform_oem_agent_order_identify;// oem代理商订单标识3

		if (orderIdPre.equals(StaticInitVariable.OEM_CLIENT_ORDERID_PRE)) {
			num = StaticInitVariable.OEM_CLIENT_ORDER_NUM;
			StaticInitVariable.OEM_CLIENT_ORDER_NUM = num + 1;
		} else {
			StaticInitVariable.OEM_CLIENT_ORDERID_PRE = orderIdPre;
			num = 1;
			StaticInitVariable.OEM_CLIENT_ORDER_NUM = num + 1;
		}

		// 拼成订单号
		String orderIdStr = orderIdPre + StringUtils.addZeroForNum(num, 4, "0");
		Long orderId = Long.valueOf(orderIdStr);

		System.out.println("生成的客户的订单id:==========" + orderId);
		logger.debug("生成的客户的订单id:==========" + orderId);

		return orderId;
	}

	// 更新代理商账户统计表
	@Deprecated
	private void updateOemAgentAccountStatistics(String agent_id, BigDecimal updateNum, String product_type,Integer orpeatre) {

		Map<String, Object> update_agent_account_statistics_map = new HashMap<>();
		update_agent_account_statistics_map.put("agent_id", agent_id);
		update_agent_account_statistics_map.put("orpeatre",orpeatre);
		if ("0".equals(product_type)) {
			update_agent_account_statistics_map.put("hy_remain_rebate_number", updateNum);
		} else if ("1".equals(product_type)) {
			update_agent_account_statistics_map.put("yx_remain_rebate_number", updateNum);
		}else if ("3".equals(product_type)) {
			update_agent_account_statistics_map.put("co_remain_rebate_number", updateNum);
		}else if ("4".equals(product_type)) {
			update_agent_account_statistics_map.put("no_remain_rebate_number", updateNum);
		}else {
			update_agent_account_statistics_map.put("gj_remain_rebate_amount", updateNum);
		}

		this.masterDao.update("sms.addOemAgentAccountStatistics", update_agent_account_statistics_map);
	}

	@Override
	public Map<String, Object> autoAuthAccount(Map<String, String> params) {
		String agentId = params.get("agent_id");

		// 获取代理商的认证信息
		Map<String, Object> data = userMapper.getCerInfoSource(agentId);

		OauthPic oauthPic = new OauthPic();
		oauthPic.setAgentId(data.get("agent_id").toString());
		oauthPic.setClientId(params.get("clientid").toString());
		Object obj = data.get("id_nbr");
		if (obj != null) {
			oauthPic.setIdNbr(data.get("id_nbr").toString());
		}

		obj = data.get("img_url");
		if (obj != null) {
			oauthPic.setImgUrl(data.get("img_url").toString());
		}

		obj = data.get("id_type");
		if (obj != null) {
			oauthPic.setIdType(data.get("id_type").toString());
		}

		oauthPic.setUpdateDate(Calendar.getInstance().getTime());
		oauthPic.setOauthType("2");

		// 添加资质认证信息到数据库
		int saveNum = accountMapper.addCerInfo(oauthPic);

		// 查询代理商认证审核记录的adminId
		String opt = accountMapper.getAuditLogByAgentId(oauthPic.getAgentId());

		// 添加审核认证信息
		AuditLog auditLog = new AuditLog();
		auditLog.setAdminId(opt);
		auditLog.setAgentId(oauthPic.getAgentId());
		auditLog.setClientId(oauthPic.getClientId());
		auditLog.setAuditType("2");
		auditLog.setStatus("1");
		auditLog.setCreateDate(Calendar.getInstance().getTime());
		auditLog.setRemark("自有客户自动认证审核");
		int auditLogNum = accountMapper.insertAuditLog(auditLog);

		if (saveNum <= 0 || auditLogNum <= 0) {
			data.put("msg", "操作失败");
			data.put("result", "fail");
		} else {
			data.put("msg", "操作成功");
			data.put("result", "success");
		}

		return data;
	}

	/**
	 * @param params
	 * @return
	 * @Title: queryInterSmsInfo
	 * @Description: 查询代理商短信信息
	 * @return: List<Map<String,Object>>
	 */
	@Override
	public List<Map<String, Object>> querySmsInfo(Map<String, Object> params) {
		return this.masterDao.getSearchList("customer.querySmsInfo", params);
	}

	@Override
    public Map queryCustomerConsumeReportTotal(Map<String, Object> formData)  {


        Map subtotal = accessMasterDao.selectOne("customer.queryCustomerConsumeReportTotal", formData);



        if(subtotal==null||subtotal.size()==0){
            return zeroMap();
        }
        return subtotal;
    }


    @Override
    public Map querycustomerConsumeEveryReportTotal(Map<String, Object> formData)  {

//        int startDate = (int) (formData.get("start_time_day")==null?0:formData.get("start_time_day"));
//        int endDate = (int) (formData.get("end_time_day")==null?0:formData.get("end_time_day"));
//        if(startDate>endDate){
//            return zeroMap();
//        }
//        try {
//            formatStatisticFormData(formData, startDate, endDate,false);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        Map subtotal =  accessMasterDao.selectOne("customer.querycustomerConsumeEveryReportTotal", formData);



        if(subtotal==null||subtotal.size()==0){
            return zeroMap();
        }
        return subtotal;
    }


    public void formatStatisticFormData(Map<String, Object> formData, int startDate, int endDate,boolean addOneday) throws Exception {
        int before90Int = DateUtil.getDateFromTodayInInt(-90);
        if(startDate<before90Int){
            throw new Exception("开始时间不得超过90天之前");
        }

        if(startDate==0){
            throw new Exception("请输入开始时间");
        }
        if(endDate==0){
            endDate = Integer.valueOf(DateUtil.dateToStr(new Date(), "yyyyMMdd"));
        }

        if(addOneday){
            Calendar endCal = Calendar.getInstance();
            endCal.set(endDate/10000, endDate%10000/100-1, endDate%10000%100);
            endCal.add(Calendar.DATE, 1);
            endDate = Integer.valueOf(DateUtil.dateToStr(endCal.getTime(), "yyyyMMdd"));
            formData.put("start_time_day", startDate);
            formData.put("end_time_day", endDate);
        }
    }


    public Map zeroMap(){
        Map subtotal = new HashMap<>();
        BigDecimal num_all_total = BigDecimal.ZERO;
        BigDecimal num_sucs_total = BigDecimal.ZERO;
        BigDecimal num_fail_total = BigDecimal.ZERO;
        BigDecimal num_known_total = BigDecimal.ZERO;
        BigDecimal num_pending_total = BigDecimal.ZERO;
        BigDecimal chargeTotal_total = BigDecimal.ZERO;
        BigDecimal intercept_total = BigDecimal.ZERO;

        subtotal.put("num_all_total",num_all_total);
        subtotal.put("chargeTotal_total",chargeTotal_total);
        subtotal.put("num_sucs_total",num_sucs_total );
        subtotal.put("num_known_total",num_known_total);
        subtotal.put("num_fail_total",num_fail_total);
        subtotal.put("num_pending_total",num_pending_total);
        subtotal.put("num_intercept_total",intercept_total);

        return subtotal;
    }

	@Override
	public List<JsmsAccount> queryCustomerConsumeReport(String agentId) {
		 return accountMapper.getSearchList(agentId);
	}

	@Override
	public PageContainer queryAll(Map<String, String> params) {
		PageContainer searchPage = new PageContainer();
		String obj =  params.get("account_id");
		JsmsAccount jsmsAccount = jsmsAccountService.getByClientId(obj);
		Integer identify = jsmsAccount.getIdentify();
		String d =  params.get("data");
		String data  = d.replaceAll("-", "");
		String table = "t_sms_access_"+identify+"_"+ data;
		params.put("table", table);
		String string = params.get("send_status");
		params.put("state",string);
		try {
			 searchPage = accessMasterDao.getSearchPage("customer.queryCustomerSendRecord", "customer.queryAll", params);
		}catch (Exception e){
			logger.debug("当前查询时间对应的流水表不存在！ 流水表  --> {}  ",table);
		}finally {
			return searchPage;
		}
	}

	@Override
	public List<Map<String, Object>> querySmsRecord4Excel(Map<String, String> params) {

		// TODO Auto-generated method stub
		List<Map<String, Object>> list = new ArrayList<>();
		String obj = (String) params.get("account_id");
		JsmsAccount jsmsAccount = jsmsAccountService.getByClientId(obj);
		Integer identify = jsmsAccount.getIdentify();
		String d = (String) params.get("data");
		String data  = d.replaceAll("-", "");
		String table = "t_sms_access_"+identify+"_"+ d;
		params.put("table", table);
		try {
			list = accessMasterDao.getSearchList("customer.querySmsRecord4Excell", params);
		}catch (Exception e){
			logger.debug("当前导出数据对应的流水表不存在！ 流水表  --> {}  ",table);
		}finally {
			return list;
		}
		//List<Map<String, Object>> list = accessMasterDao.getSearchList("customer.querySmsRecord4Excell", params);
		//return list;
	
	}

	@Override
	public int getCustomerOfAlarm(Map<String, String> params) {
		params.put("pageRowCount", String.valueOf(masterDao.getSearchSize("customer.queryCustomerInfoCount",params)));
		params.put("currentPage", "1");
		PageContainer page = masterDao.getSearchPage("customer.queryCustomerInfo", "customer.queryCustomerInfoCount",
				params);
		Map<String, Object> alarmParams = new HashMap<>();
		int flag =0;
		int index =0;
			for (int i =0;i<page.getList().size();i++) {
				Map map = page.getList().get(i);
				if(com.ucpaas.sms.common.util.StringUtils.isNotBlank(map.get("client_id").toString())){
					alarmParams.put("clientid",map.get("client_id").toString());
					alarmParams.put("yxAlarmNumber",Integer.parseInt(String.valueOf(map.get("yx_remain_num"))));
					alarmParams.put("gjAlarmAmount",new BigDecimal(String.valueOf(map.get("gj_remain_num"))));
					alarmParams.put("yzmAlarmNumber",Integer.parseInt(String.valueOf(map.get("co_remain_num")))+Integer.parseInt(String.valueOf(map.get("hy_remain_num"))));
					alarmParams.put("tzAlarmNumber",Integer.parseInt(String.valueOf(map.get("no_remain_num")))+Integer.parseInt(String.valueOf(map.get("hy_remain_num"))));
					flag = jsmsClientBalanceAlarmService.isAlarm(alarmParams);
				}
				if(flag>0){
					index+=1;
				}
			}
		return index;
	}


	@Override
	@Transactional("message_master")
	public JsmsAccount addUser(JsmsAccount jsmsAccount, JsmsOauthPic oauthPic, Long adminId) {
		// 设置用户端口和扩展类型
		jsmsAccount.setExtendtype(6);
		jsmsAccount.setExtendport(this.getExtendportForOpenAcc(jsmsAccount.getExtendtype().toString()));


		//获取客户发送邮件模版
//		String vUrl = this.queryClientSiteOemUrl(jsmsAccount.getAgentId().toString());
//		Map<String, Object> customerEail =  commonMapper.querySmsMailProp("100014");// 获取邮箱模板,100014为客户开户通用模板
//		//内容
//		String customerMailBoby = (String) customerEail.get("text");
//		// 完善您的个人资料地址
//		customerMailBoby = customerMailBoby.replace("vUrl", vUrl);
//		//主题
//		String customerMailSubject = (String)customerEail.get("subject");

		//获取开户后自动发送邮件到归属销售、产品运营、通道运营、审批客服，邮件模版
		Map<String, Object> remindMail =  commonMapper.querySmsMailProp("100024");
		String remindMailBoby = (String)remindMail.get("text");
		String remindMailSubject = (String)remindMail.get("subject");
		Long buyAgentOrderId = OrderUtils.getAgentOrderId();
		//OEM代理商分发订单
		Long distributeAgentOrderId = OrderUtils.getAgentOrderId();
		//oem客户订单
		Long oemClientOrderId = getClientOrderId();

		return selfOpenAccountService.addUser(jsmsAccount, oauthPic, adminId, remindMailBoby, remindMailSubject, buyAgentOrderId, distributeAgentOrderId, oemClientOrderId);
	}


	@Override
	@Transactional("message_master")
	public R saveClientBalanceAlarm(ClientBalanceAlarmPo clientBalanceAlarm) {
		if (clientBalanceAlarm == null || com.ucpaas.sms.common.util.StringUtils.isBlank(clientBalanceAlarm.getClientid())) {
			return R.error("客户ID不能为空");
		}
		// 查询客户
		JsmsAccount jsmsAccount = jsmsAccountService.getByClientId(clientBalanceAlarm.getClientid());
		if (jsmsAccount == null) {
			return R.error("客户不存在");
		}
		if (StringUtils.isNotBlank(clientBalanceAlarm.getAlarmEmail())
				&& clientBalanceAlarm.getAlarmEmail().length() > 1000) {
			return R.error("客户余额提醒邮件最长1000");
		}
		if (StringUtils.isNotBlank(clientBalanceAlarm.getAlarmPhone())
				&& clientBalanceAlarm.getAlarmPhone().length() > 1000) {
			return R.error("客户余额提醒手机号最长1000");
		}

		// 查询客户余额配置
		JsmsClientBalanceAlarm queryCba = new JsmsClientBalanceAlarm();
		queryCba.setClientid(clientBalanceAlarm.getClientid());
		List<JsmsClientBalanceAlarm> clientBalanceAlarms = jsmsClientBalanceAlarmService.findList(queryCba);

		int count;

		if (Collections3.isEmpty(clientBalanceAlarms)) {
			// 构造验证码
			JsmsClientBalanceAlarm yzm = buildJsmsClientBalanceAlarm(clientBalanceAlarm, clientBalanceAlarm.getYzmAlarmNumber(),
					null, ClientAlarmType.验证码.getValue());
			// 构造通知
			JsmsClientBalanceAlarm tz = buildJsmsClientBalanceAlarm(clientBalanceAlarm, clientBalanceAlarm.getTzAlarmNumber(),
					null, ClientAlarmType.通知.getValue());
			// 构造营销
			JsmsClientBalanceAlarm yx = buildJsmsClientBalanceAlarm(clientBalanceAlarm, clientBalanceAlarm.getYxAlarmNumber(),
					null, ClientAlarmType.营销.getValue());
			// 构造国际
			JsmsClientBalanceAlarm gj = buildJsmsClientBalanceAlarm(clientBalanceAlarm, null,
					clientBalanceAlarm.getGjAlarmAmount(), ClientAlarmType.国际.getValue());
			List<JsmsClientBalanceAlarm> list = new ArrayList<>();
			list.add(yzm);
			list.add(tz);
			list.add(yx);
			list.add(gj);
			logger.debug("客户余额提醒设置添加 {}", JSON.toJSONString(list));
			count = jsmsClientBalanceAlarmService.insertBatch(list);
			if (count != 0 && count != list.size()) {
				count = 0;
			}
		} else {
			logger.debug("客户余额提醒设置修改：余额提醒设置 {}  原始设置 {}", JSON.toJSONString(clientBalanceAlarm), JSON.toJSONString(clientBalanceAlarms));
			// 1. 修改验证码
			JsmsClientBalanceAlarm balanceAlarm = getJsmsClientBalanceAlarmByType(clientBalanceAlarms, ClientAlarmType.验证码.getValue().intValue());
			if (balanceAlarm == null) {
				logger.debug("客户余额提醒设置修改失败，验证码类型的余额提醒设置为空");
				throw new JsmsClientBalanceAlarmException("验证码类型的余额提醒设置修改失败");
			}
			if (!balanceAlarm.getAlarmPhone().equals(clientBalanceAlarm.getAlarmPhone())
					|| !balanceAlarm.getAlarmEmail().equals(clientBalanceAlarm.getAlarmEmail())
					|| !balanceAlarm.getAlarmNumber().equals(clientBalanceAlarm.getYzmAlarmNumber())) {
				balanceAlarm.setUpdateTime(Calendar.getInstance().getTime());
				balanceAlarm.setAlarmPhone(clientBalanceAlarm.getAlarmPhone());
				balanceAlarm.setAlarmEmail(clientBalanceAlarm.getAlarmEmail());
				balanceAlarm.setAlarmNumber(clientBalanceAlarm.getYzmAlarmNumber());
				balanceAlarm.setReminderNumber(1);
				logger.debug("客户余额提醒设置修改验证码: {}", JSON.toJSONString(balanceAlarm));
				count = jsmsClientBalanceAlarmService.updateSelective(balanceAlarm);
				if (count == 0) {
					logger.debug("客户余额提醒设置修改验证码失败，更新数据库条数为0");
					throw new JsmsClientBalanceAlarmException("验证码类型的余额提醒设置修改失败");
				}
			}
			// 2. 修改通知
			balanceAlarm = getJsmsClientBalanceAlarmByType(clientBalanceAlarms, ClientAlarmType.通知.getValue().intValue());
			if (balanceAlarm == null) {
				logger.debug("客户余额提醒设置修改失败，通知类型的余额提醒设置为空");
				throw new JsmsClientBalanceAlarmException("通知类型的余额提醒设置修改失败");
			}
			if (!balanceAlarm.getAlarmPhone().equals(clientBalanceAlarm.getAlarmPhone())
					|| !balanceAlarm.getAlarmEmail().equals(clientBalanceAlarm.getAlarmEmail())
					|| !balanceAlarm.getAlarmNumber().equals(clientBalanceAlarm.getTzAlarmNumber())) {
				balanceAlarm.setUpdateTime(Calendar.getInstance().getTime());
				balanceAlarm.setAlarmPhone(clientBalanceAlarm.getAlarmPhone());
				balanceAlarm.setAlarmEmail(clientBalanceAlarm.getAlarmEmail());
				balanceAlarm.setAlarmNumber(clientBalanceAlarm.getTzAlarmNumber());
				balanceAlarm.setReminderNumber(1);
				logger.debug("客户余额提醒设置修改通知: {}", JSON.toJSONString(balanceAlarm));
				count = jsmsClientBalanceAlarmService.updateSelective(balanceAlarm);
				if (count == 0) {
					logger.debug("客户余额提醒设置修改通知失败，更新数据库条数为0");
					throw new JsmsClientBalanceAlarmException("通知类型的余额提醒设置修改失败");
				}
			}
			// 3. 修改营销
			balanceAlarm = getJsmsClientBalanceAlarmByType(clientBalanceAlarms, ClientAlarmType.营销.getValue().intValue());
			if (balanceAlarm == null) {
				logger.debug("客户余额提醒设置修改失败，营销类型的余额提醒设置为空");
				throw new JsmsClientBalanceAlarmException("营销类型的余额提醒设置修改失败");
			}
			if (!balanceAlarm.getAlarmPhone().equals(clientBalanceAlarm.getAlarmPhone())
					|| !balanceAlarm.getAlarmEmail().equals(clientBalanceAlarm.getAlarmEmail())
					|| !balanceAlarm.getAlarmNumber().equals(clientBalanceAlarm.getYxAlarmNumber())) {
				balanceAlarm.setUpdateTime(Calendar.getInstance().getTime());
				balanceAlarm.setAlarmPhone(clientBalanceAlarm.getAlarmPhone());
				balanceAlarm.setAlarmEmail(clientBalanceAlarm.getAlarmEmail());
				balanceAlarm.setAlarmNumber(clientBalanceAlarm.getYxAlarmNumber());
				balanceAlarm.setReminderNumber(1);
				logger.debug("客户余额提醒设置修改营销: {}", JSON.toJSONString(balanceAlarm));
				count = jsmsClientBalanceAlarmService.updateSelective(balanceAlarm);
				if (count == 0) {
					logger.debug("客户余额提醒设置修改营销失败，更新数据库条数为0");
					throw new JsmsClientBalanceAlarmException("营销类型的余额提醒设置修改失败");
				}
			}
			// 4. 修改国际
			balanceAlarm = getJsmsClientBalanceAlarmByType(clientBalanceAlarms, ClientAlarmType.国际.getValue().intValue());
			if (balanceAlarm == null) {
				logger.debug("客户余额提醒设置修改失败，国际短信的余额提醒设置为空");
				throw new JsmsClientBalanceAlarmException("国际短信的余额提醒设置修改失败");
			}
			if (!balanceAlarm.getAlarmPhone().equals(clientBalanceAlarm.getAlarmPhone())
					|| !balanceAlarm.getAlarmEmail().equals(clientBalanceAlarm.getAlarmEmail())
					|| (balanceAlarm.getAlarmAmount().compareTo(clientBalanceAlarm.getGjAlarmAmount()) != 0)) {
				balanceAlarm.setUpdateTime(Calendar.getInstance().getTime());
				balanceAlarm.setAlarmPhone(clientBalanceAlarm.getAlarmPhone());
				balanceAlarm.setAlarmEmail(clientBalanceAlarm.getAlarmEmail());
				balanceAlarm.setAlarmAmount(clientBalanceAlarm.getGjAlarmAmount());
				balanceAlarm.setReminderNumber(1);
				logger.debug("客户余额提醒设置修改国际短信: {}", JSON.toJSONString(balanceAlarm));
				count = jsmsClientBalanceAlarmService.updateSelective(balanceAlarm);
				if (count == 0) {
					logger.debug("客户余额提醒设置修改国际短信失败，更新数据库条数为0");
					throw new JsmsClientBalanceAlarmException("国际短信的余额提醒设置修改失败");
				}
			}
			count = 1;
		}
		return count == 0 ? R.error("客户余额提醒设置失败") : R.ok("客户余额提醒设置成功");
	}

	@Override
	public List<Map<String, Object>> total(Map<String, String> params) {
		return accessMasterDao.getSearchList("customer.total",params);
	}


	private JsmsClientBalanceAlarm buildJsmsClientBalanceAlarm(ClientBalanceAlarmPo clientBalanceAlarmPo, Integer alarmNumber, BigDecimal alarmAmount, int type) {
		if (clientBalanceAlarmPo == null) {
			return null;
		}
		JsmsClientBalanceAlarm jsmsClientBalanceAlarm = new JsmsClientBalanceAlarm();
		jsmsClientBalanceAlarm.setClientid(clientBalanceAlarmPo.getClientid());
		jsmsClientBalanceAlarm.setAlarmPhone(clientBalanceAlarmPo.getAlarmPhone());
		jsmsClientBalanceAlarm.setAlarmEmail(clientBalanceAlarmPo.getAlarmEmail());
		jsmsClientBalanceAlarm.setAlarmType(type);
		jsmsClientBalanceAlarm.setAlarmNumber(alarmNumber);
		jsmsClientBalanceAlarm.setAlarmAmount(alarmAmount);
		jsmsClientBalanceAlarm.setReminderNumber(1);
		jsmsClientBalanceAlarm.setResetTime(Calendar.getInstance().getTime());
		jsmsClientBalanceAlarm.setCreateTime(jsmsClientBalanceAlarm.getResetTime());
		jsmsClientBalanceAlarm.setUpdateTime(jsmsClientBalanceAlarm.getResetTime());
		return jsmsClientBalanceAlarm;
	}

	private JsmsClientBalanceAlarm getJsmsClientBalanceAlarmByType(List<JsmsClientBalanceAlarm> clientBalanceAlarms, int type) {
		JsmsClientBalanceAlarm result = null;
		for (JsmsClientBalanceAlarm clientBalanceAlarm : clientBalanceAlarms) {
			if (clientBalanceAlarm.getAlarmType() != null && clientBalanceAlarm.getAlarmType().intValue() == type) {
				result = clientBalanceAlarm;
				break;
			}
		}
		return result;
	}

}
