package com.ucpaas.sms.api;

import com.google.common.collect.Maps;
import com.jsmsframework.common.dto.JsmsPage;
import com.jsmsframework.common.dto.ResultVO;
import com.jsmsframework.common.entity.JsmsNoticeList;
import com.jsmsframework.common.enums.NoticeStatus;
import com.jsmsframework.common.enums.NoticeTop;
import com.jsmsframework.common.enums.WebId;
import com.jsmsframework.common.service.JsmsNoticeListService;
import com.jsmsframework.user.service.JsmsAccountService;
import com.ucpaas.sms.common.entity.PageContainer;
import com.ucpaas.sms.common.entity.R;
import com.ucpaas.sms.common.util.Collections3;
import com.ucpaas.sms.dto.ClientSuccessRateRealtimeDTO;
import com.ucpaas.sms.service.common.AgentIndexService;
import com.ucpaas.sms.service.customer.AccountManageService;
import com.ucpaas.sms.service.finance.FinanceManageService;
import com.ucpaas.sms.util.StringUtils;
import com.ucpaas.sms.util.web.AuthorityUtils;
import com.ucpaas.sms.util.web.ControllerUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by lpjLiu on 2017/5/31.
 */
@RestController
@RequestMapping("/api")
@Api(value = "首页", description = "首页")
public class ApiIndexController {

	/**
	 * 日志对象
	 */
	protected Logger logger = LoggerFactory.getLogger(ApiIndexController.class);

	@Autowired
	private AgentIndexService agentIndexService;

	@Autowired
	private FinanceManageService financeManageService;

	@Autowired
	private AccountManageService accountManageService;
	@Autowired
	private JsmsAccountService jsmsAccountService;
	@Autowired
	private JsmsNoticeListService jsmsNoticeListService;

	/**
	 * 获取主页的客户列表
	 * 
	 */
	@GetMapping("/index/clients")
	@ApiOperation(value = "index/clients", notes = "首页查看OEM代理商余额不足子客户", response = R.class)
	@ApiImplicitParams({
			@ApiImplicitParam(name = "pageRowCount", value = "行数", dataType = "String", paramType = "query"),
			@ApiImplicitParam(name = "currentPage", value = "页数", dataType = "String", paramType = "query")})
	public R clients(String pageRowCount, String currentPage, HttpServletRequest request) {
		Map<String, String> params = ControllerUtils.buildQueryMap(pageRowCount, currentPage, request);

		PageContainer page = agentIndexService.queryCustomerForBalanceLack(params);
		return R.ok("获取客户列表成功", page);
	}

	/**
	 * 获取主页的代理商下属客户个数
	 */
	@GetMapping("/index/clientnum")
	@ApiOperation(value = "查看OEM代理商下属客户个数", notes = "首页查看OEM代理商下属客户个数",tags = "首页", response = ResultVO.class)
	public ResultVO clientNum(HttpServletRequest request) {
		Map<String,Object> params = new HashMap();
		params.put("agentId", AuthorityUtils.getLoginAgentId(request));
		int clientnum = jsmsAccountService.count(params);
		return ResultVO.successDefault(clientnum);
	}

	/**
	 *获取主页的公告详情
	 *
	 */
	@GetMapping("/agent/notice/detail")
	@ApiOperation(value = "首页公告详情", notes = "首页公告详情", response = R.class)
	@ApiImplicitParam(name = "noticeId", value = "公告ID", dataType = "String", paramType = "query",required = true)
	public R agentNoticeDetail(String noticeId,HttpServletRequest request) {
		if(StringUtils.isBlank(noticeId)){
			return R.error("公告Id不能为空!");
		}
		JsmsNoticeList jsmsNoticeList=jsmsNoticeListService.getContentById(StringUtils.toInteger(noticeId));
		return R.ok("获取首页公告详情成功", jsmsNoticeList);
	}

	/**
	 * 获取主页公告更多
	 * @param jsmsPage
	 * @param request
	 * @return
	 */
	@PostMapping("/agent/noticePage")
	@ApiOperation(value = "首页公告更多", notes = "首页公告更多",  response = JsmsPage.class)
	public JsmsPage<JsmsNoticeList> agentNoticePage(JsmsPage<JsmsNoticeList> jsmsPage,String condition, HttpServletRequest request) {
		Map<String,Object> params = new HashMap();
		params.put("status",  NoticeStatus.已发布.getValue());
		params.put("webId",  WebId.OEM代理商平台.getValue());
		params.put("condition",  condition);
		jsmsPage.setOrderByClause("release_time DESC");
		jsmsPage.setParams(params);
		jsmsPage = jsmsNoticeListService.queryPageList(jsmsPage);
		return jsmsPage;
	}

	/**
	 * 获取主页的公告
	 *
	 */
	@GetMapping("/agent/notice")
	@ApiOperation(value = "首页公告列表", notes = "首页公告列表", response = R.class)
	public R agentNotice(HttpServletRequest request) {
		Map<String, Object> dataParams = Maps.newHashMap();
		int count=8;//默认最多8条
		dataParams.put("status", NoticeStatus.已发布.getValue());
		dataParams.put("webId", WebId.OEM代理商平台.getValue());
		dataParams.put("isTop",NoticeTop.是.getValue().toString());
		dataParams.put("limit",count);
		List<JsmsNoticeList> topNoticeList=jsmsNoticeListService.queryListAll(dataParams);
		if(topNoticeList.size()<count){
			dataParams.put("isTop",NoticeTop.否.getValue().toString());
			dataParams.put("limit",count-topNoticeList.size());
			List<JsmsNoticeList> notNoticeList=jsmsNoticeListService.queryListAll(dataParams);
			if(!Collections3.isEmpty(notNoticeList)){
				topNoticeList.addAll(notNoticeList);
			}
		}

		return R.ok("获取首页公告成功", topNoticeList);
	}

	/**
	 * 获取主页的代理商下属客户个数
	 */
	@GetMapping("/index/dataoftoday")
	@ApiOperation(value = "今日提交量", notes = "查看OEM代理商下属客户今日提交",tags = "首页", response = ClientSuccessRateRealtimeDTO.class)
	@ApiImplicitParam(name = "clientId", value = "子客户ID", dataType = "string", paramType = "query")
	@ResponseBody
	public ClientSuccessRateRealtimeDTO dataOfToday(HttpServletRequest request,String clientId) {
		Map<String,Object> params = new HashMap();
		params.put("agentId", AuthorityUtils.getLoginAgentId(request));
        String agentId = AuthorityUtils.getLoginAgentId(request);
        ClientSuccessRateRealtimeDTO clientSuccessRateRealtimeDTO = agentIndexService.dataOfToday( Integer.parseInt(agentId),clientId);
        return clientSuccessRateRealtimeDTO;
	}

	/**
	 * 获取主页的相关数据
	 * 
	 */
	@GetMapping("/index/data")
	@ApiOperation(value = "index/data", notes = "首页", response = R.class)
	public R data(HttpServletRequest request) {
		// 查询参数
		Map<String, String> params = Maps.newHashMap();
		String agentId = AuthorityUtils.getLoginAgentId(request);
		params.put("agent_id", agentId);

		// 返回参数
		Map<String, Object> defaultData = Maps.newHashMap();
		Map<String, Object> data;
		Map<String, Object> agent_account_data;
		Map<String, Object> agent_pool_due_time_data;

		data = financeManageService.queryAgentPoolRemainNum(params);

		agent_account_data = financeManageService.queryAgentAccountInfo(params);
		agent_pool_due_time_data = financeManageService.queryAgentPoolRemainNumFordueTime(params);


		defaultData.put("data", data);
		defaultData.put("agent_account_data", agent_account_data);
		defaultData.put("agent_pool_due_time_data", agent_pool_due_time_data);

		defaultData.put("total_client_num", agentIndexService.queryAgentClientNum(params));
		defaultData.put("sixMonths_client_num", agentIndexService.querySixMonthsAgentClientNum(params));
		logger.info("图形数据为={}",agentIndexService.querySixMonthsAgentClientNum(params));
		// 返回是否有测试帐号
		boolean canOpenTestCount = accountManageService.canOpenTestCount(agentId);
		defaultData.put("canCreateTestAccount", canOpenTestCount);

		return R.ok("获取主页数据成功", defaultData);
	}

	/**
	 * 获取主页的短信池（短信过期）
	 * 
	 */
	@GetMapping("/agent/pools")
	public R agentPools(String pageRowCount, String currentPage, HttpServletRequest request) {
		Map<String, String> params = ControllerUtils.buildQueryMap(pageRowCount, currentPage, request);

		// 登录
		PageContainer page = financeManageService.queryOemAgentPoolList(params);
		return R.ok("获取代理商池成功", page);
	}




	/**
	 * 代理商短信池区分过期与否
	 */
	@PostMapping("/agent/newpools")
	@ApiOperation(value = "agent/newpools", notes = "首页查看OEM代理商短信池区分过期与否", response = R.class)
	@ApiImplicitParams({
			@ApiImplicitParam(name = "product_type", value = "产品类型", dataType = "String", paramType = "query"),
			@ApiImplicitParam(name = "operator_code", value = "运营商", dataType = "String", paramType = "query")})
	public R agentPoolsNew(String product_type,String operator_code,  HttpServletRequest request){
		// 查询参数
		Map<String, String> params = Maps.newHashMap();
		String agentId = AuthorityUtils.getLoginAgentId(request);
		params.put("agent_id", agentId);
		params.put("product_type", product_type);
		params.put("operator_code", operator_code);
		// 返回参数
		Map<String, Object> defaultData = Maps.newHashMap();
		List<Map<String, Object>> data;
		List<Map<String, Object>> newDate = new ArrayList<>();

		data=financeManageService.queryCustomerForBalanceDetails(params);
        if(!data.isEmpty()){
        	for(int i =0;i<data.size();i++){
        		String accountBalance = String.valueOf(data.get(i).get("account_balance"));
        		if(new BigDecimal(accountBalance.substring(0,accountBalance.length()-1)).compareTo(BigDecimal.ZERO)==1){
					newDate.add(data.get(i));
				}
			}
		}
		StringBuffer total=new StringBuffer();

		Map<String,Object> total1 =financeManageService.queryCustomerForBalanceDetailTotal(params);
		total.append(((BigDecimal)total1.get("num")).setScale(0,BigDecimal.ROUND_HALF_UP).toString()+"条");
		if((((BigDecimal)total1.get("mon")).compareTo(BigDecimal.valueOf(0.00))!=0)){
			total.append("/"+total1.get("mon").toString()+"元");
		}
		defaultData.put("data",newDate);
		defaultData.put("total",total);
		return R.ok("获取代理商短信池成功",defaultData);
	}


	/**
	 * 获取主页的客户列表
	 *
	 */
	@GetMapping("/index/clientsmsdetail")
	@ApiOperation(value = "index/clientsmsdetail", notes = "首页查看OEM代理商余额不足子客户对应剩余量", response = R.class)
	@ApiImplicitParams({
			@ApiImplicitParam(name = "clientid", value = "子客户Id", dataType = "String", paramType = "query"),
			@ApiImplicitParam(name = "product_type", value = "产品类型", dataType = "String", paramType = "query")})
	public R clientsmsdetail(String clientid, String product_type, HttpServletRequest request) {
		// 查询参数
		Map<String, String> params = Maps.newHashMap();
		//String agentId = AuthorityUtils.getLoginAgentId(request);
		params.put("clientid", clientid);
		params.put("product_type", product_type);
		Map<String, Object> defaultData = Maps.newHashMap();
		List<Map<String, Object>> data;
		String sum;
		 data = agentIndexService.queryCustomerForBalanceByType(params);
		 sum=agentIndexService.queryClientBlanceNum(params);
		 defaultData.put("data",data);
		 defaultData.put("sum",sum);

		return R.ok("获取子客户对应产品类型列表成功", defaultData);
	}


	/**
	 * 获取一周提交量曲线图数据
	 */
	@PostMapping("/WeekSubmitNumber")
	@ApiOperation(value = "一周提交量", notes = "获取一周提交量曲线图数据", response = R.class)
	@ApiImplicitParams({
			@ApiImplicitParam(name = "clientid", value = "子客户Id(可空)", dataType = "String", paramType = "query")
	})
	public R WeekSubmitNumber(@RequestParam(value = "clientid", required = false) String clientid , HttpServletRequest request) {
		try {
			String agentId = AuthorityUtils.getLoginAgentId(request);
			if (StringUtils.isBlank(agentId)) {
				R.error("请先登录");
			}
			Map<String, Integer> data = agentIndexService.queryWeekSubmitNumber(agentId, clientid);
			return R.ok("获取一周提交量曲线图数据成功", data);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			return R.error("服务器正在检修...");
		}
	}

}
