package com.ucpaas.sms.api;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.jsmsframework.common.dto.Result;
import com.jsmsframework.common.enums.CodeEnum;
import com.jsmsframework.common.enums.PaymentType;
import com.jsmsframework.finance.dto.JsmsAgentCreditRecordDTO;
import com.jsmsframework.finance.entity.JsmsAgentBalanceAlarm;
import com.jsmsframework.finance.service.JsmsAgentBalanceAlarmService;
import com.jsmsframework.finance.service.JsmsAgentCreditRecordService;
import com.jsmsframework.user.entity.JsmsAgentInfo;
import com.jsmsframework.user.service.JsmsAgentInfoService;
import com.ucpaas.sms.common.entity.PageContainer;
import com.ucpaas.sms.common.entity.R;
import com.ucpaas.sms.model.Excel;
import com.ucpaas.sms.service.finance.FinanceManageService;
import com.ucpaas.sms.service.util.ConfigUtils;
import com.ucpaas.sms.util.DateUtils;
import com.ucpaas.sms.util.RegexUtils;
import com.ucpaas.sms.util.StringUtils;
import com.ucpaas.sms.util.file.ExcelUtils;
import com.ucpaas.sms.util.file.FileUtils;
import com.ucpaas.sms.util.web.AuthorityUtils;
import com.ucpaas.sms.util.web.ControllerUtils;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Created by lpjLiu on 2017/5/31.
 */
@RestController
@RequestMapping("/api/finance")
public class ApiFinanceController {

	/**
	 * 日志对象
	 */
	protected Logger logger = LoggerFactory.getLogger(ApiFinanceController.class);

	@Autowired
	private FinanceManageService financeManageService;

	@Autowired
	private JsmsAgentBalanceAlarmService agentBalanceAlarmService;


	@Autowired
	private JsmsAgentInfoService agentInfoService;


	@Autowired
	private JsmsAgentCreditRecordService jsmsAgentCreditRecordService;


	/**
	 * 获取我的账单
	 */
	@PostMapping("/bill/self")
	@ApiOperation(value = "/api/finance/bill/self", notes = "我的账单", response = R.class)
	@ApiImplicitParams({
			@ApiImplicitParam(name = "order_id", value = "订单号", dataType = "String", paramType = "query"),
			@ApiImplicitParam(name = "payment_type", value = "业务类型", dataType = "String", paramType = "query"),
			@ApiImplicitParam(name = "financial_type", value = "财务类型", dataType = "String", paramType = "query"),
			@ApiImplicitParam(name = "pageRowCount", value = "行数", dataType = "String", paramType = "query"),
			@ApiImplicitParam(name = "currentPage", value = "页数", dataType = "String", paramType = "query")})
	public R billSelf(String order_id, String payment_type, String financial_type, String pageRowCount,
					  String currentPage, HttpServletRequest request) {
		Map<String, String> params = ControllerUtils.buildQueryMap(pageRowCount, currentPage, request);
		if (StringUtils.isNotBlank(order_id)) {
			params.put("order_id", order_id);
		}

		if (StringUtils.isNotBlank(payment_type)) {
			params.put("payment_type", payment_type);
		}

		if (StringUtils.isNotBlank(financial_type)) {
			params.put("financial_type", financial_type);
		}

		String realName = AuthorityUtils.getLoginRealName(request);
		String adminId = AuthorityUtils.getLoginUserId(request).toString();

		PageContainer page = financeManageService.queryAgentBalanceBillList(params, realName, adminId);
		return R.ok("获取我的账单成功", page);
	}

	/**
	 * 获取我的账单信息数据
	 */
	@GetMapping("/bill/data")
	public R billSelfData(HttpServletRequest request) {
		Map<String, Object> data = financeManageService.queryAgentAccountInfo(ControllerUtils.buildQueryMap(request));
		return R.ok("获取成功", data);
	}

	/**
	 * 获取我的账单-导出
	 */
	@PostMapping("/bill/self/export")
	@ApiOperation(value = "/api/finance/bill/self/export", notes = "我的账单-导出", response = R.class)
	@ApiImplicitParams({
			@ApiImplicitParam(name = "order_id", value = "订单号", dataType = "String", paramType = "query"),
			@ApiImplicitParam(name = "payment_type", value = "业务类型", dataType = "String", paramType = "query"),
			@ApiImplicitParam(name = "financial_type", value = "财务类型", dataType = "String", paramType = "query")
			})
	public void billSelfExport(String order_id, String payment_type, String financial_type, HttpServletRequest request,
							   HttpServletResponse response) {
		String filePath = ConfigUtils.temp_file_dir + "/我的账单" + DateUtils.getDate("yyyyMMddHHmmss") + ".xls";
		Excel excel = new Excel();
		excel.setFilePath(filePath);
		excel.setTitle("我的账单");

		Map<String, String> params = ControllerUtils.buildQueryMap(request);

		StringBuffer buffer = new StringBuffer();
		buffer.append("查询条件：");
		if (StringUtils.isNotBlank(order_id)) {
			params.put("order_id", order_id);

			buffer.append("  订单编号：");
			buffer.append(order_id);
			buffer.append(";");
		}

		if (StringUtils.isNotBlank(payment_type)) {
			params.put("payment_type", payment_type);

			String payment_type_name = PaymentType.getDescByValue(Integer.valueOf(payment_type));
			/*
			switch (payment_type) {
				case "0":
					payment_type_name = "充值";
					break;
				case "1":
					payment_type_name = "扣减";
					break;
				case "2":
					payment_type_name = "佣金转余额";
					break;
				case "3":
					payment_type_name = "购买产品包";
					break;
				case "4":
					payment_type_name = "退款";
					break;
				case "5":
					payment_type_name = "赠送";
					break;
				case "6":
					payment_type_name = "后付费客户消耗";
					break;
				case "7":
					payment_type_name = "回退条数";
					break;
				case "8":
					payment_type_name = "后付费客户条数返还";
				case "9":
					payment_type_name = "增加授信";
				case "10":
					payment_type_name = "降低授信";
			}*/
			buffer.append("  业务类型：");
			buffer.append(payment_type_name);
			buffer.append(";");
		}

		if (StringUtils.isNotBlank(financial_type)) {
			params.put("financial_type", financial_type);

			buffer.append("  财务类型：");
			buffer.append("0".equals(financial_type) ? "入账" : "出账");
			buffer.append(";");
		}

		excel.addRemark(buffer.toString());

		excel.addHeader(20, "业务单号", "id");
		excel.addHeader(20, "财务类型", "financial_type_str");
		excel.addHeader(20, "业务类型", "payment_type_str");
		excel.addHeader(20, "金额(元)", "amount_str");
		excel.addHeader(20, "订单编号", "order_id");
		excel.addHeader(20, "操作者", "admin_name");
		excel.addHeader(50, "备注", "remark");
		excel.addHeader(20, "创建时间", "create_time");

		String realName = AuthorityUtils.getLoginRealName(request);
		String adminId = AuthorityUtils.getLoginUserId(request).toString();
		excel.setDataList(financeManageService.queryAgentBalanceBillListForAll(params, realName, adminId));

		if (ExcelUtils.exportExcel(excel)) {
			FileUtils.download(filePath, response);
			FileUtils.delete(filePath);
		} else {
			ControllerUtils.renderText(response, "导出Excel文件失败，请联系管理员");
		}
	}

	/**
	 * 获取返点账单
	 */
	@GetMapping("/bill/rebate")
	public R billRebate(String order_id, String payment_type, String financial_type, String pageRowCount,
						String currentPage, HttpServletRequest request) {
		Map<String, String> params = ControllerUtils.buildQueryMap(pageRowCount, currentPage, request);
		if (StringUtils.isNotBlank(order_id)) {
			params.put("id", order_id);
		}

		if (StringUtils.isNotBlank(payment_type)) {
			params.put("payment_type", payment_type);
		}

		if (StringUtils.isNotBlank(financial_type)) {
			params.put("financial_type", financial_type);
		}

		PageContainer page = financeManageService.queryAgentRebateBillList(params);
		return R.ok("获取返点账单成功", page);
	}

	/**
	 * 获取返点账单-导出
	 */
	@PostMapping("/bill/rebate/export")
	public void billRebateExport(String order_id, String payment_type, String financial_type,
								 HttpServletRequest request, HttpServletResponse response) {
		String filePath = ConfigUtils.temp_file_dir + "/返点账单" + DateUtils.getDate("yyyyMMddHHmmss") + ".xls";

		Excel excel = new Excel();
		excel.setFilePath(filePath);
		excel.setTitle("返点账单");

		Map<String, String> params = ControllerUtils.buildQueryMap(request);

		StringBuffer buffer = new StringBuffer();
		buffer.append("查询条件：");
		if (StringUtils.isNotBlank(order_id)) {
			params.put("id", order_id);

			buffer.append("  订单编号：");
			buffer.append(order_id);
			buffer.append(";");
		}

		if (StringUtils.isNotBlank(payment_type)) {
			params.put("payment_type", payment_type);

			buffer.append("  业务类型：");
			buffer.append("0".equals(financial_type) ? "返点收入" : "抵扣");
			buffer.append(";");
		}

		if (StringUtils.isNotBlank(financial_type)) {
			params.put("financial_type", financial_type);

			buffer.append("  财务类型：");
			buffer.append("0".equals(financial_type) ? "入账" : "出账");
			buffer.append(";");
		}

		excel.addRemark(buffer.toString());

		excel.addHeader(20, "业务单号", "id");
		excel.addHeader(20, "订单编号", "order_id");
		excel.addHeader(20, "业务类型", "payment_type_str");
		excel.addHeader(20, "财务类型", "financial_type_str");
		excel.addHeader(20, "金额（元）", "amount_str");
		excel.addHeader(20, "备注", "remark");
		excel.addHeader(20, "创建时间", "create_time");

		excel.setDataList(financeManageService.queryAgentRebateBillListForAll(params));

		if (ExcelUtils.exportExcel(excel)) {
			FileUtils.download(filePath, response);
			FileUtils.delete(filePath);
		} else {
			ControllerUtils.renderText(response, "导出Excel文件失败，请联系管理员");
		}
	}

	/**
	 * 获取短信账单
	 */
	@ResponseBody
	@PostMapping("/bill/sms")
	@ApiOperation(value = "/api/finance/bill/sms", notes = "短信账单", response = R.class)
	@ApiImplicitParams({
			@ApiImplicitParam(name = "orderInfo", value = "子账户ID/子账户名称/订单编号", dataType = "String", paramType = "query"),
			@ApiImplicitParam(name = "start_time_day", value = "开始时间", dataType = "String", paramType = "query"),
			@ApiImplicitParam(name = "end_time_day", value = "结束时间", dataType = "String", paramType = "query"),
			@ApiImplicitParam(name = "order_type", value = "订单类型", dataType = "String", paramType = "query"),
			@ApiImplicitParam(name = "pageRowCount", value = "行数", dataType = "String", paramType = "query"),
			@ApiImplicitParam(name = "currentPage", value = "页数", dataType = "String", paramType = "query")})
	public R billSms(@RequestBody String param,HttpServletRequest request) {
		JSONObject jo=new JSONObject();
		//如果页面传的是json字符串，用下列方式解析
		Map<String, String> param1=(Map<String, String> )jo.parse(param); //string转map
		Map<String, String> params = ControllerUtils.buildQueryMap(String.valueOf(param1.get("pageRowCount")), String.valueOf(param1.get("currentPage")), request);
		if (StringUtils.isNotBlank(param1.get("orderInfo"))) {
			params.put("orderInfo", param1.get("orderInfo"));
		}

		if (StringUtils.isNotBlank(param1.get("order_type"))) {
			params.put("order_type", param1.get("order_type"));
		}

		if (StringUtils.isNotBlank(param1.get("start_time_day"))) {
			params.put("start_time_day", param1.get("start_time_day"));
		}

		if (StringUtils.isNotBlank(param1.get("end_time_day"))) {
			params.put("end_time_day", param1.get("end_time_day") + " 23:59:59");
		}

		PageContainer page = financeManageService.queryAgentOrderInfoList(params);
		return R.ok("获取短信账单成功", page);
	}

	/**
	 * 获取短信账单信息数据
	 */
	@GetMapping("/bill/sms/data")
	public R billSmsData(HttpServletRequest request) {
		Map<String, String> params = ControllerUtils.buildQueryMap(request);
		Map<String, Object> data = financeManageService.queryAgentAccountStatistics(params);
		Map<String, Object> agentPoolRemainNumData = financeManageService.queryAgentPoolRemainNum(params);

		Map<String, Object> resultData = Maps.newHashMap();
		resultData.put("data", data);
		resultData.put("agentPoolRemainNumData", agentPoolRemainNumData);
		return R.ok("获取成功", resultData);
	}

	/**
	 * 获取短信账单-导出
	 */
	@PostMapping("/bill/sms/export")
	@ApiOperation(value = "/api/finance/bill/sms/export", notes = "短信账单-导出", response = R.class)
	@ApiImplicitParams({
			@ApiImplicitParam(name = "orderInfo", value = "子账户ID/子账户名称/订单编号", dataType = "String", paramType = "query"),
			@ApiImplicitParam(name = "start_time_day", value = "开始时间", dataType = "String", paramType = "query"),
			@ApiImplicitParam(name = "end_time_day", value = "结束时间", dataType = "String", paramType = "query"),
			@ApiImplicitParam(name = "order_type", value = "订单类型", dataType = "String", paramType = "query"),
			@ApiImplicitParam(name = "start_time_day", value = "开始时间", dataType = "String", paramType = "query"),
			@ApiImplicitParam(name = "end_time_day", value = "结束时间", dataType = "String", paramType = "query")
			})
	public void billSmsExport(String orderInfo, String order_type,String start_time_day,String end_time_day,  HttpServletRequest request,
							  HttpServletResponse response) {
		String filePath = ConfigUtils.temp_file_dir + "/短信账单" + DateUtils.getDate("yyyyMMddHHmmss") + ".xls";
		Excel excel = new Excel();
		excel.setFilePath(filePath);
		excel.setTitle("短信账单");

		Map<String, String> params = ControllerUtils.buildQueryMap(request);
		StringBuffer buffer = new StringBuffer();
		buffer.append("查询条件：");
		if (StringUtils.isNotBlank(orderInfo)) {
			params.put("orderInfo", orderInfo);

			buffer.append("  订单编号，子账户ID，子账户名称：");
			buffer.append(orderInfo);
			buffer.append(";");
		}
		if (StringUtils.isNotBlank(start_time_day)) {
			params.put("start_time_day", start_time_day + " 00:00:00");

			buffer.append("  创建时间：");
			buffer.append(start_time_day + " 00:00:00");
			buffer.append(";");
		}
		if (StringUtils.isNotBlank(end_time_day)) {
			params.put("end_time_day", end_time_day + " 23:59:59");

			buffer.append("  结束时间：");
			buffer.append(end_time_day + " 23:59:59");
			buffer.append(";");
		}
		if (StringUtils.isNotBlank(order_type)) {
			params.put("order_type", order_type);

			String order_type_name = null;
			switch (order_type) {
				case "0":
					order_type_name = "短信购买";
					break;
				case "1":
					order_type_name = "子账户充值";
					break;
				default:
					order_type_name = "子账户回退";
			}
			buffer.append("  订单类型：");
			buffer.append(order_type_name);
			buffer.append(";");
		}

		excel.addRemark(buffer.toString());

		excel.addHeader(20, "订单编号", "order_id");
		excel.addHeader(20, "子账户名称", "names");
		excel.addHeader(20, "子账户ID", "client_id");
		excel.addHeader(20, "产品代码", "product_code");
		excel.addHeader(20, "产品名称", "product_name");
		excel.addHeader(20, "产品类型", "product_type_str");
		excel.addHeader(20, "运营商", "operator_code_str");
		excel.addHeader(20, "区域", "area_code_str");
		excel.addHeader(20, "短信单价（元）", "unit_price");
		excel.addHeader(20, "订单金额（元）", "order_amount");
		excel.addHeader(20, "短信金额变动", "sms_balance_change_amount");
		excel.addHeader(20, "订单类型", "order_type_str");
		excel.addHeader(20, "创建时间", "create_time");
		excel.addHeader(20, "到期时间", "due_time");

		List<Map<String, Object>> list = financeManageService.queryAgentOrderInfoListForAll(params);
		buffer.setLength(0);
		for (Map item : list) {
			String type = item.get("product_type").toString();
			String money = item.get("sms_balance_change_amount").toString();
			buffer.append(item.get("sms_balance_change_flag").toString());
			if (!"2".equals(type)) {
				int test = Integer.parseInt(money);
				buffer.append(test);
			} else {
				buffer.append(money);
			}
			buffer.append(item.get("sms_balance_change_amount_unit").toString());
			item.put("sms_balance_change_amount", buffer.toString());
			buffer.setLength(0);
		}
		excel.setDataList(list);
		if (ExcelUtils.exportExcel(excel)) {
			FileUtils.download(filePath, response);
			FileUtils.delete(filePath);
		} else {
			ControllerUtils.renderText(response, "导出Excel文件失败，请联系管理员");
		}
	}



	@PostMapping("/agentBalanceAlarm/edit")
	public Result<JsmsAgentBalanceAlarm> agentBalanceAlarmEdit(String id,String agentId,String alarmAmount,String alarmPhone, HttpServletRequest request) {

		JsmsAgentBalanceAlarm agentBalanceAlarm = new JsmsAgentBalanceAlarm();
		if(agentId==null||!agentId.equals(AuthorityUtils.getLoginAgentId(request)))
			return new Result<>(false, CodeEnum.FAIL,null,"代理商ID不正确");

		if(StringUtils.isEmpty(alarmAmount)){
			return new Result<>(false, CodeEnum.FAIL,null,"告警阀值不能为空");
		}
		try{
			agentBalanceAlarm.setAlarmAmount(new BigDecimal(alarmAmount));
			if(agentBalanceAlarm.getAlarmAmount().compareTo(BigDecimal.ZERO)==-1){
				return new Result<>(false, CodeEnum.FAIL,null,"告警阀值不能为负数");
			}
			if(agentBalanceAlarm.getAlarmAmount().compareTo(new BigDecimal("1000000"))==1){
				return new Result<>(false, CodeEnum.FAIL,null,"告警阀值不能大于100W");
			}
		}catch (Exception e){
			return new Result<>(false, CodeEnum.FAIL,null,"告警阀值只能为数字");
		}

		if(StringUtils.isEmpty(alarmPhone)){
			return new Result<>(false, CodeEnum.FAIL,null,"接收告警手机号不能为空");
		}
		if(alarmPhone.length() > 1000){
			return new Result<>(false, CodeEnum.FAIL, null, "手机号字符总长度不能超过1000");
		}
		String[] phoneArr = alarmPhone.split(",");
		/*if(phoneArr.length > 2){
			return new Result<>(false, CodeEnum.FAIL, null, "最多只能设置两个手机号");
		}*/
		for (String s : phoneArr) {
			if(!RegexUtils.isMobile(s)){
				return new Result<>(false, CodeEnum.FAIL, null, "手机号码格式错误");
			}
		}
		agentBalanceAlarm.setAgentId(Integer.valueOf(agentId));
		agentBalanceAlarm.setAlarmPhone(alarmPhone);
		int i = agentBalanceAlarmService.insertOrUpdate(agentBalanceAlarm);
		return new Result<>(true, CodeEnum.SUCCESS,null,"操作成功");
	}

	@GetMapping("/agentBalanceAlarm/get")
	public Result<JsmsAgentBalanceAlarm> agentBalanceAlarmGet(HttpServletRequest request) {


		Integer agentId = Integer.valueOf(AuthorityUtils.getLoginAgentId(request));
		JsmsAgentBalanceAlarm agentBalanceAlarm = agentBalanceAlarmService.getByAgentId(agentId);
		if(agentBalanceAlarm==null) {
			JsmsAgentInfo agentInfo  = agentInfoService.getByAgentId(agentId);
			agentBalanceAlarm = new JsmsAgentBalanceAlarm();
			agentBalanceAlarm.setAgentId(agentId);
			agentBalanceAlarm.setAlarmAmount(BigDecimal.ZERO);
			agentBalanceAlarm.setReminderNumber(0);
			agentBalanceAlarm.setAlarmPhone(agentInfo.getMobile());
		}
		return new Result<>(true, CodeEnum.SUCCESS,agentBalanceAlarm,"操作成功");
	}


	@GetMapping("/creditHistories/list")
	public Result<List<JsmsAgentCreditRecordDTO>> creditHistories(HttpServletRequest request) {
		Integer agentId = Integer.valueOf(AuthorityUtils.getLoginAgentId(request));
		List<JsmsAgentCreditRecordDTO> dto = jsmsAgentCreditRecordService.creditHistories(agentId);
		return new Result<>(true, CodeEnum.SUCCESS,dto,"操作成功");
	}
	/**
	 * 获取账户消费记录   查询
	 */
	@PostMapping("/bill/purchaseHistory")
	@ApiOperation(value = "/api/finance/bill/purchaseHistory", notes = "账户消费", response = R.class)
	@ApiImplicitParams({
			@ApiImplicitParam(name = "clientid", value = "客户ID", dataType = "String", paramType = "query"),
			@ApiImplicitParam(name = "realname", value = "客户名称", dataType = "String", paramType = "query"),
			@ApiImplicitParam(name = "order_type", value = "操作类型", dataType = "String", paramType = "query"),
			@ApiImplicitParam(name = "product_type", value = "产品类型", dataType = "String", paramType = "query"),
			@ApiImplicitParam(name = "start_time_day", value = "开始时间", dataType = "String", paramType = "query"),
			@ApiImplicitParam(name = "end_time_day", value = "结束时间", dataType = "String", paramType = "query"),
			@ApiImplicitParam(name = "pageRowCount", value = "行数", dataType = "String", paramType = "query"),
			@ApiImplicitParam(name = "currentPage", value = "页数", dataType = "String", paramType = "query")})
	public R billPurchaseHistory(String clientid, String realname, String pageRowCount, Integer order_type, Integer product_type, String currentPage, String start_time_day, String end_time_day,
								 HttpServletRequest request, HttpSession session) {
		Map<String, String> params = ControllerUtils.buildQueryMap(pageRowCount, currentPage, request);
		if (StringUtils.isNotBlank(clientid)) {
			params.put("clientid", clientid);
		}

		if (StringUtils.isNotBlank(realname)) {
			params.put("realname", realname);
		}
		if (product_type!=null) {
			params.put("product_type", String.valueOf(product_type));
		}

		if (order_type!=null) {
			params.put("order_type", String.valueOf(order_type));
		}else {
			params.put("order_type", String.valueOf(3));
		}

		if (StringUtils.isNotBlank(start_time_day)) {
			params.put("start_time_day", start_time_day + " 00:00:00");
		}

		if (StringUtils.isNotBlank(end_time_day)) {
			params.put("end_time_day", end_time_day + " 23:59:59");
		}
		params.put("agent_id",session.getAttribute("agentId").toString());
		PageContainer page = financeManageService.querypurchaseHistoryList(params);
		return R.ok("获取客户消费记录成功", page);
	}
	/**
	 * 账户消费记录-导出
	 */
	@PostMapping("/bill/purchaseHistory/export")
	@ApiOperation(value = "/api/finance/bill/purchaseHistory/export", notes = "账户消费记录-导出", response = R.class)
	@ApiImplicitParams({
			@ApiImplicitParam(name = "clientid", value = "子账户ID", dataType = "String", paramType = "query"),
			@ApiImplicitParam(name = "realname", value = "子账户名称", dataType = "String", paramType = "query"),
			@ApiImplicitParam(name = "order_type", value = "操作类型", dataType = "String", paramType = "query"),
			@ApiImplicitParam(name = "product_type", value = "产品类型", dataType = "String", paramType = "query"),
			@ApiImplicitParam(name = "start_time_day", value = "开始时间", dataType = "String", paramType = "query"),
			@ApiImplicitParam(name = "end_time_day", value = "结束时间", dataType = "String", paramType = "query")
			})
	public void billPurchaseHistoryExport(@RequestParam Map<String,String> params, String start_time_day, String end_time_day, HttpServletRequest request,
										  HttpServletResponse response,HttpSession session) {
		String filePath = ConfigUtils.temp_file_dir + "/子账户消费记录" + DateUtils.getDate("yyyyMMddHHmmss") + ".xls";
		Excel excel = new Excel();
		excel.setFilePath(filePath);
		if (StringUtils.isNotBlank(start_time_day)) {
			params.put("start_time_day", start_time_day + " 00:00:00");
		}

		if (StringUtils.isNotBlank(end_time_day)) {
			params.put("end_time_day", end_time_day + " 23:59:59");
		}

		if (StringUtils.isNotBlank(String.valueOf(params.get("order_type")))) {
			params.put("order_type", String.valueOf(params.get("order_type")));
		}else {
			params.put("order_type", String.valueOf(3));
		}
		params.put("agent_id",session.getAttribute("agentId").toString());
		excel.setTitle("子账户消费记录");
		excel.addHeader(20, "子账户ID", "client_id");
		excel.addHeader(20, "子账户名称", "name");
		excel.addHeader(20, "操作类型", "orderTypeStr");
		excel.addHeader(20, "产品类型", "productTypeStr");
		excel.addHeader(20, "运营商类型", "operatorCodeStr");
		excel.addHeader(20, "区域", "areaCodeStr");
		excel.addHeader(50, "单价(元)", "unit_price");
		excel.addHeader(20, "到期日期", "due_time");
		excel.addHeader(20, "短信数量", "order_number");
		excel.addHeader(20, "消费日期", "consumer_date");
		excel.addHeader(20, "操作日期", "create_time");

		excel.setDataList(financeManageService.querypurchaseHistoryForAll(params));

		if (ExcelUtils.exportExcel(excel)) {
			FileUtils.download(filePath, response);
			FileUtils.delete(filePath);
		} else {
			ControllerUtils.renderText(response, "导出Excel文件失败，请联系管理员");
		}
	}

}
