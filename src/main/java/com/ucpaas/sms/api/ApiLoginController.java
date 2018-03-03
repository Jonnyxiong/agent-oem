package com.ucpaas.sms.api;

import com.ucpaas.sms.common.annotation.IgnoreAuth;
import com.ucpaas.sms.common.entity.R;
import com.ucpaas.sms.model.AgentInfo;
import com.ucpaas.sms.model.Menu;
import com.ucpaas.sms.service.common.AgentIndexService;
import com.ucpaas.sms.service.common.CommonService;
import com.ucpaas.sms.service.util.AgentUtils;
import com.ucpaas.sms.util.web.AuthorityUtils;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * Created by lpjLiu on 2017/5/31.
 */
@RestController
@RequestMapping("/api")
public class ApiLoginController {

	/**
	 * 日志对象
	 */
	protected Logger logger = LoggerFactory.getLogger(ApiLoginController.class);

	@Autowired
	private CommonService commonService;

	@Autowired
	private AgentIndexService agentIndexService;

	/**
	 * 登录
	 * 
	 * @param username
	 * @param password
	 */
	@IgnoreAuth
	@PostMapping("/login")
	public R login(String username, String password, HttpServletRequest request, HttpSession session) {
		R r;
		if (AuthorityUtils.isLogin(request)) {// 已登录
			r = R.ok("已登录", AuthorityUtils.getLoginRealName(request));
			return r;
		}

		try {
			// 登录
			Map data = commonService.login(username, password);
			// 记录登录信息
			Map<String, Object> resultData = (Map<String, Object>) data.get("resultData");
			if (resultData != null) {
				Long userId = (long) resultData.get("userId");

				// 查询代理商Id
				AgentInfo agentInfo = AgentUtils.queryAgentInfoByAdminId(userId.toString());
				String agentId = agentInfo.getAgentId();
				session.setAttribute("agentId",agentId);
				AuthorityUtils.setLoginUser(request, userId, resultData.get("userName").toString(),
						(Integer) resultData.get("roleId"), (Integer) resultData.get("webId"),
						(Menu) resultData.get("menu"), agentId);
			}

			r = "success".equals(data.get("result").toString())
					? R.ok(data.get("msg").toString(), resultData.get("userName"))
					: R.error(data.get("msg").toString());
		} catch (Exception e) {
			logger.error("登录失败\n username{} 消息{}", username, e);
			r = R.error("服务器异常,正在检修中...");
		}

		return r;
	}

	/**
	 * 获取菜单
	 * 
	 */
	@GetMapping("/menu")
	public R menu(HttpServletRequest request) {
		Menu menu = AuthorityUtils.getLoginMenu(request);
		return R.ok("获取菜单成功", menu);
	}

	/**
	 * 登出
	 * 
	 */
	@PostMapping("/logout")
	public R login(HttpServletRequest request) {
		AuthorityUtils.setLogoutUser(request);
		logger.debug("退出登录成功");
		return R.ok("退出登录成功");
	}

	/**
	 * 登录是否超时
	 * 
	 */
	@IgnoreAuth
	@GetMapping("/login/timeout")
	public Map<String, Object> isExpire(HttpServletRequest request) {
		boolean flag = AuthorityUtils.isLogin(request);
		return R.ok("获取信息成功", flag);
	}
}
