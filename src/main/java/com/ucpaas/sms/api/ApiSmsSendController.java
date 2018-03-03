package com.ucpaas.sms.api;

import com.jsmsframework.audit.entity.JsmsAutoTemplate;
import com.jsmsframework.common.dto.JsmsPage;
import com.jsmsframework.common.dto.ResultVO;
import com.jsmsframework.common.entity.JsmsExcel;
import com.jsmsframework.common.enums.*;
import com.jsmsframework.common.enums.smsSend.SmsSendFileType;
import com.jsmsframework.common.util.BeanUtil;
import com.jsmsframework.common.util.FileUtils;
import com.jsmsframework.common.util.PageExportUtil;
import com.jsmsframework.sms.send.dto.JsmsAccessSmsDTO;
import com.jsmsframework.sms.send.dto.JsmsAccessTimerSmsDTO;
import com.jsmsframework.sms.send.dto.TimerSendTaskDTO;
import com.jsmsframework.sms.send.entity.JsmsSubmitProgress;
import com.jsmsframework.sms.send.entity.JsmsTimerSendTask;
import com.jsmsframework.sms.send.service.JsmsSendService;
import com.jsmsframework.sms.send.service.JsmsSubmitProgressService;
import com.jsmsframework.sms.send.service.JsmsTimerSendPhonesService;
import com.jsmsframework.sms.send.service.JsmsTimerSendTaskService;
import com.jsmsframework.sms.send.util.JsmsSendParam;
import com.jsmsframework.user.audit.service.JsmsUserAutoTemplateService;
import com.jsmsframework.user.entity.JsmsAccount;
import com.jsmsframework.user.service.JsmsAccountService;
import com.ucpaas.sms.dto.JsmsAutoTemplateDTO;
import com.ucpaas.sms.service.sms.SmsSendService;
import com.ucpaas.sms.service.util.ConfigUtils;
import com.ucpaas.sms.util.JsonUtils;
import com.ucpaas.sms.util.PageConvertUtil;
import com.ucpaas.sms.util.security.Des3Utils;
import com.ucpaas.sms.util.web.AuthorityUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by dylan on 2017/11/24.
 */
@RestController
@RequestMapping("/api/send")
@Api(value = "短信发送", description = "短信发送API")
public class ApiSmsSendController {

	/**
	 * 日志对象
	 */
	private Logger logger = LoggerFactory.getLogger(ApiSmsSendController.class);
	@Autowired
	private SmsSendService sendService;
	@Autowired
	private JsmsAccountService jsmsAccountService;
	@Autowired
	private JsmsUserAutoTemplateService jsmsUserAutoTemplateService;
	@Autowired
	private JsmsSendService jsmsSendService;
	@Autowired
	private JsmsTimerSendPhonesService jsmsTimerSendPhonesService;
	@Autowired
	private JsmsTimerSendTaskService jsmsTimerSendTaskService;
	@Autowired
	private JsmsSendParam jsmsSendParam;
	@Autowired
	private JsmsSubmitProgressService jsmsSubmitProgressService;

	@PostMapping("/getclients")
	@ApiOperation(value = "获取当前代理下属子账号", notes = "获取子账号",tags = "短信服务", response = ResultVO.class)
	@ApiImplicitParam(name = "getAll", value = "是否所有客户,1 是获取所有", dataType = "int", paramType = "query")
	public ResultVO getClients(HttpServletRequest request, String getAll) {
		Map<String, Object> params = new HashMap<>();
		params.put("agentId",AuthorityUtils.getLoginAgentId(request));
		if(StringUtils.isBlank(getAll) || !"1".equals(getAll)){
			params.put("smsfrom", SmsFrom.HTTPS.getValue());
		}
		List<JsmsAccount> jsmsAccounts = jsmsAccountService.queryAll(params);
		List<ClientIdAndName> accountInfo = new ArrayList<>();
		for (JsmsAccount jsmsAccount : jsmsAccounts) {
			accountInfo.add(this.new ClientIdAndName(jsmsAccount.getClientid(),jsmsAccount.getName()));
		}
		return ResultVO.successDefault(accountInfo);
	}

	@PostMapping("/getmologs")
	@ApiOperation(value = "用户回复记录", notes = "上行记录",tags = "短信服务", response = JsmsPage.class)
	@ApiImplicitParams({ @ApiImplicitParam(name = "page", value = "当前页码", dataType = "int", paramType = "query"),
			@ApiImplicitParam(name = "rows", value = "每页行数", dataType = "int", paramType = "query"),
			@ApiImplicitParam(name = "clientid", value = "子账号ID", dataType = "string", required = true, paramType = "query"),
			@ApiImplicitParam(name = "startReceivedate", value = "开始时间", dataType = "string", paramType = "query"),
			@ApiImplicitParam(name = "endReceivedate", value = "结束时间", dataType = "string", paramType = "query"),
			@ApiImplicitParam(name = "phone", value = "手机号码", dataType = "string", paramType = "query"),
			@ApiImplicitParam(name = "searchContent", value = "回复内容", dataType = "string", paramType = "query"),
			@ApiImplicitParam(name = "params", value = "参数", dataType = "int", paramType = "query"),
			@ApiImplicitParam(name = "jsmsPage", value = "非必须", dataType = "int", paramType = "query") })
	public ResultVO getAccessMologs(HttpServletRequest request, @RequestParam Map<String, String> params, JsmsPage jsmsPage) {
		String startReceivedate = params.get("startReceivedate");
		String endReceivedate = params.get("endReceivedate");
		String clientid = params.get("clientid");
		ResultVO resultVO = checkReceiveTime(startReceivedate,endReceivedate,clientid);
		if(resultVO != null && resultVO.isFailure()){
			return resultVO;
		}
		String dateSuffix = StringUtils.replace(StringUtils.substring(endReceivedate, 0, 7),"-","");
		params.put("dateSuffix", dateSuffix);
		jsmsPage.setParams(params);
		jsmsPage.setOrderByClause("receivedate DESC");
		sendService.queryMoList(jsmsPage);
		return ResultVO.successDefault(jsmsPage);
	}

	@PostMapping("/getrecords")
	@ApiOperation(value = "发送记录", notes = "短信发送记录",tags = "短信服务", response = ResultVO.class)
	@ApiImplicitParams({ @ApiImplicitParam(name = "page", value = "当前页码", dataType = "int", paramType = "query"),
			@ApiImplicitParam(name = "rows", value = "每页行数", dataType = "int", paramType = "query"),
			@ApiImplicitParam(name = "clientid", value = "子账号ID", dataType = "string", required = true, paramType = "query"),
			@ApiImplicitParam(name = "stateList", value = "发送状态", dataType = "string", paramType = "query"),
			@ApiImplicitParam(name = "beginDate", value = "开始时间", dataType = "string", paramType = "query"),
			@ApiImplicitParam(name = "endDate", value = "结束时间", dataType = "string", paramType = "query"),
			@ApiImplicitParam(name = "phone", value = "手机号码", dataType = "string", paramType = "query"),
			@ApiImplicitParam(name = "searchContent", value = "发送内容", dataType = "string", paramType = "query"),
			@ApiImplicitParam(name = "params", value = "参数", dataType = "int", paramType = "query"),
			@ApiImplicitParam(name = "jsmsPage", value = "非必须", dataType = "int", paramType = "query") })
	public ResultVO getSendRecords(HttpServletRequest request, @RequestParam Map<String, String> params, JsmsPage jsmsPage) {
		String beginDate = params.get("beginDate");
		String endDate = params.get("endDate");
		String clientid = params.get("clientid");
		ResultVO resultVO = checkSendRecordParam(beginDate,endDate,clientid);
		if(resultVO != null && resultVO.isFailure()){
			return resultVO;
		}
		jsmsPage.setParams(params);
		jsmsPage.setOrderByClause(" date DESC ");

		try {
			sendService.querySendRecordList(jsmsPage);
		} catch (Exception e) {
			logger.error("查询客户记录异常 ----> ",e);
		}
		return ResultVO.successDefault(jsmsPage);
	}

	@PostMapping("/exportrecords")
	@ApiOperation(value = "发送记录", notes = "短信发送记录",tags = "短信服务", response = ResultVO.class)
	@ApiImplicitParams({ @ApiImplicitParam(name = "page", value = "当前页码", dataType = "int", paramType = "query"),
			@ApiImplicitParam(name = "rows", value = "每页行数", dataType = "int", paramType = "query"),
			@ApiImplicitParam(name = "clientid", value = "子账号ID", dataType = "string", required = true, paramType = "query"),
			@ApiImplicitParam(name = "stateList", value = "发送状态", dataType = "string", paramType = "query"),
			@ApiImplicitParam(name = "_stateList", value = "发送状态文字描述", dataType = "string", paramType = "query"),
			@ApiImplicitParam(name = "beginDate", value = "开始时间", dataType = "string", paramType = "query"),
			@ApiImplicitParam(name = "endDate", value = "结束时间", dataType = "string", paramType = "query"),
			@ApiImplicitParam(name = "phone", value = "手机号码", dataType = "string", paramType = "query"),
			@ApiImplicitParam(name = "searchContent", value = "发送内容", dataType = "string", paramType = "query"),
			@ApiImplicitParam(name = "params", value = "参数", dataType = "int", paramType = "query"),
			@ApiImplicitParam(name = "jsmsPage", value = "非必须", dataType = "int", paramType = "query") })
	public ResultVO exportSendRecords(HttpServletResponse response, @RequestParam Map<String, String> params, JsmsPage jsmsPage) {
		String beginDate = params.get("beginDate");
		String endDate = params.get("endDate");
		String clientid = params.get("clientid");
		ResultVO resultVO = checkSendRecordParam(beginDate,endDate,clientid);
		if(resultVO != null && resultVO.isFailure()){
			return resultVO;
		}
		jsmsPage.setParams(params);
		jsmsPage.setOrderByClause(" date DESC ");
		StringBuilder filePath = new StringBuilder(ConfigUtils.temp_file_dir);
		if(!ConfigUtils.temp_file_dir.endsWith("/")){
			filePath.append("/");
		}
		filePath.append(params.get("clientid"))  // todo 客户名称
				.append("短信发送记录").append(".xls").append("$$$").append(UUID.randomUUID());
		String path = filePath.toString();
		JsmsExcel excel = new JsmsExcel();
		StringBuilder condition = new StringBuilder("查询条件 -> ")
				.append("子账户：").append(clientid).append("；")
				.append("发送状态：").append(params.get("_stateList")).append("；")
				.append("发送开始时间：").append(params.get("beginDate")).append("；")
				.append("发送结束时间：").append(params.get("endDate")).append("；")
				.append("手机号码：").append(params.get("phone")).append("；")
				.append("发送内容：").append(params.get("searchContent"));
		excel.addRemark(condition.toString());
		excel.setFilePath(path);
		excel.setTitle("短信发送记录");// todo 客户名称
		excel.addHeader(30, "手机号", "phone");
		excel.addHeader(50, "发送内容", "content");
		excel.addHeader(50, "发送状态", "stateStr");
		excel.addHeader(20, "状态码", "errorcodeStr");
		excel.addHeader(20, "发送时间", "dateStr");
		excel.addHeader(20, "计费条数", "chargeNum");
		try {
			resultVO = PageExportUtil.instance().exportPage(sendService,jsmsPage,excel,"querySendRecordList");
		} catch (Exception e) {
			logger.error("导出客户记录异常 ----> ",e);
			resultVO = ResultVO.failure("导出异常，请稍后再试");
		}
		if(resultVO != null && resultVO.isSuccess()){
			FileUtils.download("短信发送记录.xls", (String) resultVO.getData(),response);
			FileUtils.delete(path);
			return ResultVO.successDefault("下载成功");
		}else {
			return resultVO;
		}
	}

	private ResultVO checkSendRecordParam(String beginDate, String endDate, String clientid) {
		if(StringUtils.isBlank(beginDate)){
			return ResultVO.failure("请选择发送时间，且只能查询近三个月内记录");
		}else{
			try {
				DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
				DateTime start = DateTime.parse(beginDate,format);
				DateTime end = DateTime.parse(endDate,format);
				if(start.compareTo(DateTime.now().minusMonths(3)) < 0){
					return ResultVO.failure("只能查询近三个月内记录");
				}
				if(start.getDayOfYear() != end.getDayOfYear() || start.getYear() != end.getYear() ){
					return ResultVO.failure("日期范围只能选择同一天");
				}
			} catch (IllegalArgumentException e) {
				logger.error("时间解析错误 ---> ",e);
				return ResultVO.failure("时间格式错误");
			}
		}
		if(StringUtils.isBlank(clientid)){
			return ResultVO.failure("请选择账户");
		}
		return null;
	}


	private ResultVO checkReceiveTime(String startReceivedate,String endReceivedate,String clientid){
		if(StringUtils.isBlank(startReceivedate)){
			return ResultVO.failure("请选择回复时间，且只能查询近三个月内记录");
		}else{
			DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
			try {
				DateTime startDateTime = DateTime.parse(startReceivedate,format);
				if(startDateTime.compareTo(DateTime.now().minusMonths(3)) < 0){
					return ResultVO.failure("只能查询近三个月内记录");
				}
				String startMonth = startDateTime.toString("yyyyMM");
				String endMonth = StringUtils.replace(StringUtils.substring(endReceivedate, 0, 7),"-","");
				if (!startMonth.equals(endMonth)){
					return ResultVO.failure("回复时间的时间段必须在同一个月内");
				}
			} catch (IllegalArgumentException e) {
				logger.error("时间解析错误 ---> ",e);
				return ResultVO.failure("时间格式错误");
			}
		}
		if(StringUtils.isBlank(clientid)){
			return ResultVO.failure("请选择账户");
		}
		return null;
	}


	@PostMapping("/exportmologs")
	@ApiOperation(value = "下载用户回复记录", notes = "下载上行记录",tags = "短信服务", response = JsmsPage.class)
	@ApiImplicitParams({ @ApiImplicitParam(name = "page", value = "当前页码", dataType = "int", paramType = "query"),
			@ApiImplicitParam(name = "rows", value = "每页行数", dataType = "int", paramType = "query"),
			@ApiImplicitParam(name = "clientid", value = "子账号ID", dataType = "string", required = true, paramType = "query"),
			@ApiImplicitParam(name = "startReceivedate", value = "开始时间", dataType = "string", paramType = "query"),
			@ApiImplicitParam(name = "endReceivedate", value = "结束时间", dataType = "string", paramType = "query"),
			@ApiImplicitParam(name = "phone", value = "手机号码", dataType = "string", paramType = "query"),
			@ApiImplicitParam(name = "searchContent", value = "回复内容", dataType = "string", paramType = "query"),
			@ApiImplicitParam(name = "params", value = "参数", dataType = "int", paramType = "query"),
			@ApiImplicitParam(name = "jsmsPage", value = "非必须", dataType = "int", paramType = "query") })
	public ResultVO exportAccessMologs(HttpServletResponse response, @RequestParam Map<String, String> params, JsmsPage jsmsPage) {
		String startReceivedate = params.get("startReceivedate");
		String endReceivedate = params.get("endReceivedate");
		String clientid = params.get("clientid");
		ResultVO resultVO = checkReceiveTime(startReceivedate,endReceivedate,clientid);
		if(resultVO != null && resultVO.isFailure()){
			return resultVO;
		}
		String dateSuffix = StringUtils.replace(StringUtils.substring(endReceivedate, 0, 7),"-","");
		params.put("dateSuffix", dateSuffix);
		jsmsPage.setParams(params);
		jsmsPage.setOrderByClause("receivedate DESC");

		StringBuilder filePath = new StringBuilder(ConfigUtils.temp_file_dir);
		if(!ConfigUtils.temp_file_dir.endsWith("/")){
			filePath.append("/");
		}
		filePath.append(params.get("clientid"))  // todo 客户名称
				.append("用户回复记录").append(".xls").append("$$$").append(UUID.randomUUID());
		String path = filePath.toString();
		JsmsExcel excel = new JsmsExcel();
		StringBuilder condition = new StringBuilder("查询条件 -> ")
				.append("子账户：").append(clientid).append("；")
				.append("回复开始时间：").append(params.get("startReceivedate")).append("；")
				.append("回复结束时间：").append(params.get("endReceivedate")).append("；")
				.append("手机号码：").append(params.get("phone")).append("；")
				.append("回复内容：").append(params.get("searchContent"));
		excel.addRemark(condition.toString());
		excel.setFilePath(path);
		excel.setTitle("用户回复记录");// todo 客户名称
		excel.addHeader(30, "手机号", "phone");
		excel.addHeader(50, "回复内容", "content");
		excel.addHeader(20, "回复时间", "receivedateStr");
		resultVO = PageExportUtil.instance().exportPage(sendService,jsmsPage,excel,"queryMoList");
		if(resultVO.isSuccess()){
			FileUtils.download("用户回复记录.xls", (String) resultVO.getData(),response);
			FileUtils.delete(path);
			return ResultVO.successDefault("下载成功");
		}else {
			return resultVO;
		}
	}

	private class ClientIdAndName{
		private String clientid;
		private String name;

		public ClientIdAndName(String clientid, String name) {
			this.clientid = clientid;
			this.name = name;
		}

		public String getClientid() {
			return clientid;
		}

		public void setClientid(String clientid) {
			this.clientid = clientid;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@GetMapping("/downloadtemplate")
	@ApiOperation(value = "下载手机号导入模板", notes = "下载手机号导入模板",tags = "短信服务")
	public void downloadTemplate(HttpServletRequest request, HttpServletResponse response) throws IOException {

		String path = request.getServletContext().getRealPath("/template/sms-template.xlsx");
		FileUtils.download("手机号导入模板.xlsx",path,response);
	}
	@GetMapping("/downloadtemplateCSV")
	@ApiOperation(value = "下载手机号导入模板", notes = "下载手机号导入模板",tags = "短信服务")
	public void downloadtemplateCSV(HttpServletRequest request, HttpServletResponse response) throws IOException {

		String path = request.getServletContext().getRealPath("/template/sms-template.csv");
		FileUtils.download("手机号导入模板.csv",path,response);
	}
	@GetMapping("/downloadtemplateTXT")
	@ApiOperation(value = "下载手机号导入模板", notes = "下载手机号导入模板",tags = "短信服务")
	public void downloadTemplateTXT(HttpServletRequest request, HttpServletResponse response) throws IOException {

		String path = request.getServletContext().getRealPath("/template/sms-template.txt");
		FileUtils.download("手机号导入模板.txt",path,response);
	}

	@PostMapping("/importMobile")
	@ApiOperation(value = "手机号导入", notes = "Excel手机号导入",tags = "短信服务", response = ResultVO.class,consumes = "application/octet-stream")
//	public ResultVO importMobile(@RequestParam("excel") CommonsMultipartFile excel, HttpServletRequest request)
	public ResultVO importMobile(@RequestParam("excel") MultipartFile excel, HttpServletRequest request)
			throws Exception {
		ResultVO result;
		long statTimeMillis = System.currentTimeMillis();
		try {
			// logger.debug("clientid={}正在批量导入手机号", userInfo.getClientId());
			logger.debug("clientid={}正在批量导入手机号");
			result = sendService.importMobile((CommonsMultipartFile) excel);
		} catch (Exception e) {
			logger.error("短信号码批量导入失败 ", e);
			result = ResultVO.failure("短信号码批量导入失败，请联系客服");
		}
		long endTimeMillis = System.currentTimeMillis();
		long tasteTimeMills = endTimeMillis - statTimeMillis;

		logger.debug("clientid={}批量导入手机号导入完成, 耗时={} result={}", tasteTimeMills, JsonUtils.toJson(result));
		return result;
	}

	/**
	 * 查询所有审核通过用户级别的智能模板
	 * @param jsmsPage
	 * @param request
	 * @param params
	 * @return
	 */
	@PostMapping("/autoTemplateList")
	@ResponseBody
	@ApiOperation(value = "查询所有审核通过用户级别的智能模板", notes = "查询所有审核通过用户级别的智能模板",tags = "短信服务",response = JsmsPage.class)
	@ApiImplicitParams({ @ApiImplicitParam(name = "page", value = "当前页码", dataType = "int", paramType = "query"),
			@ApiImplicitParam(name = "rows", value = "每页行数", dataType = "int", paramType = "query"),
			@ApiImplicitParam(name = "clientId", value = "子账号ID", dataType = "string", required = true, paramType = "query"),
			@ApiImplicitParam(name = "smsType", value = "模板属性", dataType = "int", paramType = "query"),
			@ApiImplicitParam(name = "params", value = "参数", dataType = "int", paramType = "query"),
			@ApiImplicitParam(name = "applicationScenarios", value = "模板ID/签名/内容", dataType = "string", paramType = "query") })
	public JsmsPage<JsmsAutoTemplate> autoTemplateList(JsmsPage<JsmsAutoTemplate> jsmsPage, HttpServletRequest request,
													   @RequestParam Map<String, Object> params) {
		//params.put("templateLevel","1");
		params.put("state", AutoTemplateStatus.审核通过.getValue());
		jsmsPage.setParams(params);
		jsmsPage.setOrderByClause("a.create_time DESC");
		jsmsPage = jsmsUserAutoTemplateService.findListOfAutoTemplate(jsmsPage, WebId.OEM代理商平台.getValue(), AutoTemplateLevel.用户级别);
		return jsmsPage;
	}

	/**
	 * 查询所有审核通过通用级别的智能模板
	 * @param jsmsPage
	 * @param params
	 * @return
	 */
	@PostMapping("/autoTemplateCommonList")
	@ResponseBody
	@ApiOperation(value = "查询所有审核通过通用级别的智能模板", notes = "查询所有审核通过通用级别的智能模板",tags = "短信服务",response = JsmsPage.class)
	@ApiImplicitParams({ @ApiImplicitParam(name = "page", value = "当前页码", dataType = "int", paramType = "query"),
			@ApiImplicitParam(name = "rows", value = "每页行数", dataType = "int", paramType = "query"),
			@ApiImplicitParam(name = "smsType", value = "模板属性", dataType = "int", paramType = "query"),
			@ApiImplicitParam(name = "params", value = "参数", dataType = "int", paramType = "query"),
			@ApiImplicitParam(name = "applicationScenarios", value = "内容", dataType = "string", paramType = "query") })
	public JsmsPage<JsmsAutoTemplate> overAllTemplateQuery(JsmsPage jsmsPage,@RequestParam Map<String, Object> params){
		//params.put("clientId","'*'");
		//params.put("templateLevel","0");//通用模板
		int rowNum =0;
		params.put("state", AutoTemplateStatus.审核通过.getValue());
		jsmsPage.setParams(params);
		jsmsPage.setOrderByClause("a.create_time DESC");
		jsmsPage = jsmsUserAutoTemplateService.findListOfAutoTemplate(jsmsPage, WebId.OEM代理商平台.getValue(), AutoTemplateLevel.全局级别);
		List<JsmsAutoTemplateDTO> list = new ArrayList<>();
		for (Object temp : jsmsPage.getData()) {
			rowNum =rowNum+1;
			JsmsAutoTemplateDTO jsmsAutoTemplateDTO = new JsmsAutoTemplateDTO();
			try {
				BeanUtils.copyProperties(jsmsAutoTemplateDTO,temp);
			} catch (IllegalAccessException e) {
				logger.error("BeanUtils 属性转换异常 --> {}",e);
			} catch (InvocationTargetException e) {
				logger.error("BeanUtils 属性转换异常 --> {}",e);
			}
			jsmsAutoTemplateDTO.setOrderNo(rowNum);
			list.add(jsmsAutoTemplateDTO);
		}
		jsmsPage.setData(list);
		return jsmsPage;
	}

	/**
	 * 短信发送接口
	 * @param request
	 * @param jsmsAccessSmsDTO
	 * @return
	 */
	@PostMapping("/sending")
	@ResponseBody
	@ApiOperation(value = "OEM短信发送接口", notes = "OEM短信发送接口",tags = "短信服务", response = ResultVO.class)
	public ResultVO sending( HttpServletRequest request,@RequestBody JsmsAccessSmsDTO jsmsAccessSmsDTO) {
		String agentId = AuthorityUtils.getLoginAgentId(request);

		ResultVO resultVO = null;
		try {
			resultVO = sendService.oemSmsSend(jsmsAccessSmsDTO, Integer.parseInt(agentId));
		} catch (Exception e) {
			logger.error("OEM短信发送异常，{} ---------> {}",e.getMessage(),e);
			resultVO = ResultVO.failure("短信发送超时，请稍后再试！");
		}
		return resultVO;
	}

	/**
	 * 短信定时发送接口
	 * @param request
	 * @param jsmsAccessTimerSmsDTO
	 * @return
	 */
	@PostMapping("/send_time_sms")
	@ResponseBody
	@ApiOperation(value = "OEM短信定时发送接口", notes = "OEM短信定时发送接口",tags = "短信服务", response = ResultVO.class)
	public ResultVO sendTim(HttpServletRequest request, @RequestBody JsmsAccessTimerSmsDTO jsmsAccessTimerSmsDTO) {
		String agentId = AuthorityUtils.getLoginAgentId(request);
		ResultVO resultVO = null;
		try {
			jsmsAccessTimerSmsDTO.setSubmittype("1");
			resultVO = sendService.oemSmsTimSend4BigFile(jsmsAccessTimerSmsDTO,Integer.parseInt(agentId));
		} catch (Exception e) {
			logger.error("OEM短信定时发送异常，{} ---------> {}",e.getMessage(),e);
			resultVO = ResultVO.failure("短信定时发送超时，请稍后再试！");
		}
		return resultVO;
	}

	@RequestMapping(path="/sendTask",method = RequestMethod.POST)
	@ResponseBody
	@ApiOperation(value = "发送任务查询", notes = "发送任务查询",tags = "短信服务", response = JsmsPage.class)
	@ApiImplicitParams({ @ApiImplicitParam(name = "page", value = "当前页码", dataType = "int", paramType = "query"),
			@ApiImplicitParam(name = "rows", value = "每页行数", dataType = "int", paramType = "query"),
			@ApiImplicitParam(name = "clientId", value = "发送账户", dataType = "String", paramType = "query"),
			@ApiImplicitParam(name = "creater", value = "创建者", dataType = "String", paramType = "query"),
			@ApiImplicitParam(name = "createTimeStart", value = "开始时间", dataType = "string", paramType = "query"),
			@ApiImplicitParam(name = "createTimeEnd", value = "结束时间", dataType = "string", paramType = "query"),
			@ApiImplicitParam(name = "id", value = "任务ID", dataType = "int", paramType = "query"),
			@ApiImplicitParam(name = "smstype", value = "短信类型", dataType = "int", paramType = "query"),
			@ApiImplicitParam(name = "sign", value = "短信签名", dataType = "string", paramType = "query"),
			@ApiImplicitParam(name = "content", value = "短信内容", dataType = "string", paramType = "query"),
			@ApiImplicitParam(name = "params", value = "参数", dataType = "int", paramType = "query")})
	public JsmsPage sendTaskQuery(@RequestParam Map<String, String> params,HttpServletRequest request,JsmsPage jsmsPage){
		String agentId = AuthorityUtils.getLoginAgentId(request);

		try {
			params.put("agentId",agentId);
			jsmsPage.setParams(params);
			sendService.querySubmitProgressList(jsmsPage);
		} catch (Exception e) {
			logger.error("查询短信发送进度异常 ---> {}", e);
		}

		return jsmsPage;
	}
	/**
	 * 定时短信入口/路由
	 * @param mv
	 * @return
	 */
	@RequestMapping(path="/smsTimerSend/list",method = RequestMethod.GET )
	public ModelAndView smsTimerSend( ModelAndView mv){
		mv.setViewName("smsTimerSend/list");
		return mv;
	}

	/**
	 * 获取定时短信内容
	 * @param params
	 * @return
	 */
	@RequestMapping(path="/smsTimerSend/list",method = RequestMethod.POST)
	@ResponseBody
	@ApiOperation(value = "定时短信查询", notes = "定时短信查询",tags = "定时短信", response = JsmsPage.class)
	@ApiImplicitParams({ @ApiImplicitParam(name = "page", value = "当前页码", dataType = "int", paramType = "query"),
			@ApiImplicitParam(name = "rows", value = "每页行数", dataType = "int", paramType = "query"),
			@ApiImplicitParam(name = "clientId", value = "发送账户", dataType = "string", paramType = "query"),
			@ApiImplicitParam(name = "status", value = "任务状态", dataType = "int", paramType = "query"),
			@ApiImplicitParam(name = "selectTimeFlag", value = "时间选择", dataType = "int", paramType = "query"),
			@ApiImplicitParam(name = "timeStart", value = "开始时间", dataType = "string", paramType = "query"),
			@ApiImplicitParam(name = "timeEnd", value = "结束时间", dataType = "string", paramType = "query"),
			@ApiImplicitParam(name = "taskID", value = "任务ID", dataType = "String", paramType = "query"),
			@ApiImplicitParam(name = "smstype", value = "短信类型", dataType = "int", paramType = "query"),
			@ApiImplicitParam(name = "sign", value = "短信签名", dataType = "string", paramType = "query"),
			@ApiImplicitParam(name = "params", value = "参数", dataType = "int", paramType = "query"),
			@ApiImplicitParam(name = "content", value = "短信内容", dataType = "string", paramType = "query")})
	public ResultVO smsTimerSendQuery( @RequestBody Map<String, String> params,HttpSession session){
		Set<String> clientIds = new HashSet<>();
		String agentId = String.valueOf(session.getAttribute("agentId"));
		params.put("agentId",agentId);
		/*params.put("submittype", String.valueOf(TaskSubmitTypeEnum.代理商.getValue()));*/
		if(com.jsmsframework.common.util.StringUtils.isBlank(params.get("smstype"))){
			params.remove("smstype");
		}
		if(com.jsmsframework.common.util.StringUtils.isBlank(params.get("status"))){
			params.remove("status");
		}

		params.put("currentPage",params.get("page"));
		params.put("pageRowCount",params.get("rows"));
		JsmsPage jsmsPage = PageConvertUtil.paramToPage(params);
		jsmsPage.setParams(params);
		jsmsPage.setParams(params);
		String clientId = String.valueOf(params.get("clientId"));
		if (StringUtils.isNotBlank(clientId)) {
			clientIds.add(clientId);
			jsmsPage.getParams().put("clientIds",clientIds);
		}
		jsmsPage.setOrderByClause(" submit_time DESC,task_id DESC");
		try {
			sendService.smsTimerSendQuery(jsmsPage, WebId.OEM代理商平台,agentId);
		} catch (Exception e) {
			logger.error("查询定时短信异常 ----> ",e);
		}
		return ResultVO.successDefault(jsmsPage);
	}
	@RequestMapping(path="/getAllPhone/list",method = RequestMethod.POST)
	@ResponseBody
	@ApiOperation(value = "查看号码", notes = "查看号码",tags = "定时短信", response = JsmsPage.class)
	@ApiImplicitParams({
			@ApiImplicitParam(name = "uid", value = "uid", dataType = "int", paramType = "query"),
			@ApiImplicitParam(name = "submitTime", value = "提交时间", dataType = "string", paramType = "query")})
	public ResultVO getAllPhone( String uid,String submitTime){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		List<String> phoneList = new ArrayList<>();
		ResultVO resultVO = new ResultVO();
		try {
			phoneList = jsmsTimerSendPhonesService.getAllPhoneOfListByUid(sdf.parse(submitTime),uid);
		} catch (Exception e) {
			logger.error("查询号码异常 ----> ",e);
		}
		resultVO.setCode(Code.SUCCESS);
		resultVO.setData(org.apache.commons.lang3.StringUtils.join(phoneList, ","));
		return resultVO;
	}
	@RequestMapping(path="/checkSubmitTime",method = RequestMethod.POST)
	@ResponseBody
	@ApiOperation(value = "校验时间", notes = "定时短信",tags = "定时短信")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "submitTime", value = "提交时间", dataType = "string", paramType = "query")
	})
	public ResultVO checkSubmitTime(String submitTime){
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date submitTimes = null;
		try {
			submitTimes = df.parse(submitTime);
		} catch (ParseException e) {
			logger.debug("日期格式化异常",e);
		}
		java.util.Date date=new Date();
		long l=submitTimes.getTime()-date.getTime();
		long day=l/(24*60*60*1000);
		long hour=(l/(60*60*1000)-day*24);
		long min=((l/(60*1000))-day*24*60-hour*60);
		long s=(l/1000-day*24*60*60-hour*60*60-min*60);
		if(l<= jsmsSendParam.getSendCFG(JsmsSendParam.TimeSendCFG.取消发送最小间隔)*60*1000){
			return ResultVO.failure(Code.REDIRECT,"本短信即将发送，无法取消！");
		}else{
			return ResultVO.successDefault(Code.SUCCESS,"本短信将在"+day+"天"+hour+"小时"+min+"分"+s+"秒后发送,确认取消此定时短信？");
		}
	}

	@RequestMapping(path="/undoSend",method = RequestMethod.POST)
	@ResponseBody
	@ApiOperation(value = "取消发送", notes = "定时短信",tags = "定时短信", response = JsmsPage.class)
	@ApiImplicitParams({
			@ApiImplicitParam(name = "taskID", value = "任务ID", dataType = "int", paramType = "query"),
			@ApiImplicitParam(name = "submitTime", value = "提交时间", dataType = "string", paramType = "query")
	})
	public ResultVO cancelTask( String uid,String submitTime,HttpSession session) throws ParseException {
		ResultVO resultVO = checkSubmitTime(submitTime);
		if(resultVO != null && resultVO.getCode().equals(Code.SUCCESS.getValue())){
			DateTime date = new DateTime();
			String agentId = String.valueOf(session.getAttribute("agentId"));
			return jsmsTimerSendTaskService.cancelSendTask(uid,date,agentId);
		}else{
			return ResultVO.failure(Code.REDIRECT,"本短信即将发送，无法取消！");
		}
	}
	@PostMapping("/parsefile")
	@ApiOperation(value = "手机号文件导入", notes = "手机号文件导入",tags = "短信服务", response = ResultVO.class,consumes = "application/octet-stream")
	@ResponseBody
	public ResultVO parseFile(String filePath, HttpServletRequest request) {
		if(StringUtils.isBlank(filePath)){
			return ResultVO.failure(Code.OPT_ERR,"文件路径不存在");
		}
		ResultVO result;
		String sysPath;
		if (ConfigUtils.current_tomcat_data_dir.endsWith("/")) {
			sysPath = ConfigUtils.current_tomcat_data_dir.substring(0, ConfigUtils.current_tomcat_data_dir.lastIndexOf("/"));
		} else if (ConfigUtils.current_tomcat_data_dir.endsWith("\\")) {
			sysPath = ConfigUtils.current_tomcat_data_dir.substring(0, ConfigUtils.current_tomcat_data_dir.lastIndexOf("\\"));
		} else {
			sysPath = ConfigUtils.current_tomcat_data_dir;
		}
		try {
			result = jsmsSendService.parseMobileFile(ConfigUtils.file_download_url, filePath, ConfigUtils.temp_file_dir, sysPath);

		} catch (Exception e) {
			logger.debug("导入文件异常 ---> {}",e);
			result = ResultVO.failure("请求超时,请稍后再试...");
		}
		return result;
	}

	@RequestMapping(path="/edit",method = RequestMethod.POST)
	@ResponseBody
	@ApiOperation(value = "编辑", notes = "定时短信",tags = "定时短信", response = JsmsPage.class)
	@ApiImplicitParams({
			@ApiImplicitParam(name = "uid", value = "任务ID", dataType = "int", paramType = "query")
	})
	public ResultVO edit( String uid) {
		if(StringUtils.isBlank(uid)){
			return ResultVO.failure(Code.OPT_ERR_UNAUTHORIZED, "参数缺失");
		}
		Map params = new HashMap();
		params.put("serviceId", uid);
		List<JsmsSubmitProgress> list = jsmsSubmitProgressService.findList(params);
		TimerSendTaskDTO dto = new TimerSendTaskDTO();
		params.put("uid", uid);
		List<JsmsTimerSendTask> jsmsTimerSendTaskList = jsmsTimerSendTaskService.findList(params);
		if(jsmsTimerSendTaskList !=null && jsmsTimerSendTaskList.size() > 0){
			JsmsTimerSendTask sendTask = jsmsTimerSendTaskList.get(0);
			BeanUtil.copyProperties(sendTask,dto);
			if (!list.isEmpty()){
				JsmsSubmitProgress jsmsSubmitProgress = list.get(0);
				if (! SmsSendFileType.号码池.getValue().equals(jsmsSubmitProgress.getFileType())) {
					dto.setFileType(jsmsSubmitProgress.getFileType());
					dto.setImportFilePath(Des3Utils.encodeDes3(jsmsSubmitProgress.getImportFilePath()));
					dto.setSubmitTotal(jsmsSubmitProgress.getSubmitTotal());
					dto.setErrNum(jsmsSubmitProgress.getErrNum());
					dto.setRepeatNum(jsmsSubmitProgress.getRepeatNum());

					String importFilePath = jsmsSubmitProgress.getImportFilePath();
					Pattern pattern = Pattern.compile("(?<=\\$\\$\\$).+");
					Matcher matcher = pattern.matcher(importFilePath);

					String temp = null;
					while(matcher.find()){
						temp = matcher.group();
						matcher = pattern.matcher(temp);
					}
					if(StringUtils.isNotEmpty(temp)){
						dto.setFileName(temp);
					}

					return ResultVO.successDefault(Code.SUCCESS,dto);
				}else{
					String phoneStr = jsmsTimerSendPhonesService.getAllPhoneByUid(sendTask.getSubmitTime(),uid);
					dto.setPhoneStr(phoneStr);
				}
			}else{
				String phoneStr = jsmsTimerSendPhonesService.getAllPhoneByUid(sendTask.getSubmitTime(),uid);
				dto.setPhoneStr(phoneStr);
			}
			return ResultVO.successDefault(Code.SUCCESS,dto);
		}else{
			return ResultVO.failure(Code.OPT_ERR,"未找到记录，请刷新后重试！");
		}
	}


	public static void main(String[] args) {
		String importFilePath = "/opt/paas/sms-oauthPic/file/201801/2016120005$$$191fc48da670fc52461dd405d4d7e498$$$6w (12121) 123(1).xls";
//		String importFilePath = "191fc48da670fc52461dd405d4d7e498$$$6w (12121) 123(1).xls";
		Pattern pattern = Pattern.compile("(?<=\\$\\$\\$).+");
		Matcher matcher = pattern.matcher(importFilePath);
		while(matcher.find()){
			String temp = matcher.group();
			System.out.println(temp);
			matcher = pattern.matcher(temp);
		}
	}
}
