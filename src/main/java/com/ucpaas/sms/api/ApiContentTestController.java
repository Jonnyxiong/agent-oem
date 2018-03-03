package com.ucpaas.sms.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.jsmsframework.audit.service.JsmsAuditKeywordListService;
import com.jsmsframework.channel.service.JsmsKeywordListService;
import com.jsmsframework.channel.service.JsmsWhiteKeywordChannelService;
import com.jsmsframework.common.dto.R;
import com.jsmsframework.common.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.jsmsframework.user.entity.JsmsAccount;
import com.ucpaas.sms.service.content.ContentTestService;
import com.ucpaas.sms.service.customer.CustomerManageService;
import com.ucpaas.sms.util.StringUtils;
import com.ucpaas.sms.util.web.AuthorityUtils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/api/content")
@Api(value = "/api/content", description = "短信内容测试API")
public class ApiContentTestController {
	/**
	 * 日志对象
	 */
	protected Logger logger = LoggerFactory.getLogger(ApiContentTestController.class);
	@Autowired
	private CustomerManageService customerManageService;
	@Autowired
	private JsmsKeywordListService jsmsKeywordListService;
	/**
	 * 获取代理商下面的id和名字
	 * @param request
	 * @return
	 */
	@GetMapping("/content/user")
	public R getAgentData(HttpServletRequest request) {
		String agentId = AuthorityUtils.getLoginAgentId(request);// 获取代理商id
		List<JsmsAccount> list = customerManageService.queryCustomerConsumeReport(agentId);
		return R.ok("成功", list);
	}
	@ResponseBody
	@PostMapping("/content/test")
	@ApiOperation(value = "/content/test", notes = "获取页面内容")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "content", value = "短信内容", dataType = "String", paramType = "query") })
	public String getContentTest(String content) throws Exception{
		Map<Integer,String> returnMap = new HashMap<>();
		if (StringUtils.isNotBlank(content)) {
			return JsonUtil.toJson(jsmsKeywordListService.checkKeyword(content));
		} else {
			returnMap.put(201, "输入验证短信为空");
			return JsonUtil.toJson(returnMap);
		}
	}
}
