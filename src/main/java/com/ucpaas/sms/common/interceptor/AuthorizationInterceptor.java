/**
 * lpjLiu 2017-04-25
 */
package com.ucpaas.sms.common.interceptor;

import com.ucpaas.sms.common.annotation.IgnoreAuth;
import com.ucpaas.sms.model.AgentInfo;
import com.ucpaas.sms.service.util.AgentUtils;
import com.ucpaas.sms.service.util.ConfigUtils;
import com.ucpaas.sms.util.web.AuthorityUtils;
import com.ucpaas.sms.util.web.ControllerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 权限验证
 */
public class AuthorizationInterceptor extends HandlerInterceptorAdapter {
	private final Logger logger = LoggerFactory.getLogger(AuthorizationInterceptor.class);
	private static Set<String> whitelist = null;

	{
		whitelist = new HashSet<String>();

		// 登录
		whitelist.add("/login");
		whitelist.add("/login.html");
		whitelist.add("/login.jsp");
		whitelist.add("/favicon.ico");

		// 公共
		whitelist.add("/agent/common");
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		// 获取访问路径
		String accessUrl = request.getRequestURI().toString();

		if (check(request, accessUrl, handler)) {
			return true;
		} else {
			// 如果数据库用户信息，跳到登录页面
			if (request.getHeader("x-requested-with") != null
					&& request.getHeader("x-requested-with").equalsIgnoreCase("XMLHttpRequest")) {
				// ajax请求，直接让session超时，跳到登录页面
				response.setHeader("sessionstatus", "timeout");
				// 设置错误码401
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
				// 设置状态码401
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			} else {
				// 普通请求，跳到登录页面
				response.sendRedirect(request.getContextPath() + "/");
			}
		}

		return false;
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
		if (modelAndView != null) {
			// 放入项目全路径
			modelAndView.addObject("ctx", ControllerUtils.getAllContextPath(request));
			modelAndView.addObject("contextPath", ControllerUtils.getContextPath(request));
		}
	}

	private boolean check(HttpServletRequest request, String accessUrl, Object handler) {
		if (ConfigUtils.is_auto_login && !AuthorityUtils.isLogin(request)) {
			AuthorityUtils.setAutoLoginUser(request);
			logger.debug("自动登录");
		}

		// 获取资源路径，跳过，不处理
		if (accessUrl.equals("/") || accessUrl.contains("assets/") || accessUrl.contains("static/")) {
			return true;
		}

		// 循环判断访问路径是否包含了这个地址
		Boolean canAccess = false;
		for (String url : whitelist) {
			if (accessUrl.contains(url)) {
				canAccess = true;
				break;
			}
		}

		if (canAccess) {
			return true;
		}

		// 如果有@IgnoreAuth注解，则不验证
		IgnoreAuth annotation = null;
		if (handler instanceof HandlerMethod) {
			annotation = ((HandlerMethod) handler).getMethodAnnotation(IgnoreAuth.class);
		}

		if (annotation != null) {
			return true;
		}

		// 判断代理商是否已经被冻结或者注销
		Long userId = AuthorityUtils.getLoginUserId(request);
		if (userId != null) {
			AgentInfo agentInfo = AgentUtils.queryAgentInfoByAdminId(userId.toString());
			if ("5".equals(agentInfo.getStatus()) || "6".equals(agentInfo.getStatus())) {
				// 如果代理商已经被冻结或者失效，让session失效
				AuthorityUtils.setLogoutUser(request);
			}
		}

		if (AuthorityUtils.isLogin(request)) {
			if (accessUrl.contains("swagger-ui.html") || accessUrl.contains("webjars") || accessUrl.contains("v2/api-docs")) {
				if (ConfigUtils.environmentFlag.equals("development") || ConfigUtils.environmentFlag.equals("devtest")) {
					return true;
				}else {
					return false;
				}
			}

			int roleId = AuthorityUtils.getLoginRoleId(request);
			Map<String, Object> data = AgentUtils.isAuthority(roleId, accessUrl);
			if (data.get("result").toString().equals("1")) {// 是否有访问权限
				return true;
			}

			// 首页特殊处理
			if ("/oem_agent/index/view".equals(accessUrl)) {
				return true;
			}
		}

		logger.debug("没有访问权限：reqUrl={}", accessUrl);

		return false;
	}
}
