package com.ucpaas.sms.util.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.ucpaas.sms.constant.SysConstant;
import com.ucpaas.sms.constant.UserConstant;
import com.ucpaas.sms.model.Menu;
import com.ucpaas.sms.service.util.ConfigUtils;

/**
 * 权限控制工具类
 * 
 */
public class AuthorityUtils {

	/**
	 * 当前登录用户的sid、roleId保存在session中的key
	 */
	private static final String LOGIN_USER_ID = "LOGIN_USER_ID";

	private static final String LOGIN_USER_REALNAME = "LOGIN_USER_REALNAME";

	private static final String LOGIN_ROLE_ID = "LOGIN_ROLE_ID";

	private static final String LOGIN_WEB_ID = "LOGIN_WEB_ID";

	/**
	 * Add by lpjLiu 2017-04-25 菜单放入Session
	 */
	private static final String LOGIN_USER_MENU = "LOGIN_USER_MENU";
	private static final String LOGIN_AGENT_ID = "LOGIN_AGENT_ID";

	/**
	 * 保存自动登录用户的sid、roleId
	 * 
	 * @param request
	 */
	public static void setAutoLoginUser(HttpServletRequest request) {
		setLoginUser(request, SysConstant.SUPER_ADMIN_USER_ID, SysConstant.SUPER_ADMIN_USER_REALNAME,
				UserConstant.ROLE_SUPER_ADMIN);
	}

	/**
	 * 保存当前登录用户的sid、roleId
	 * 
	 * @param request
	 * @param userId
	 * @param userName
	 * @param roleId
	 */
	public static void setLoginUser(HttpServletRequest request, Long userId, String userName, Integer roleId) {
		HttpSession session = request.getSession();
		session.setAttribute(LOGIN_USER_ID, userId);
		session.setAttribute(LOGIN_USER_REALNAME, userName);
		session.setAttribute(LOGIN_ROLE_ID, roleId);
	}

	/**
	 * Add by lpjLiu 2017-04-25 保存当前登录用户的sid、roleId,menu
	 * 
	 * @param request
	 * @param userId
	 * @param userName
	 * @param roleId
	 * @param webId
	 * @param menu
	 */
	public static void setLoginUser(HttpServletRequest request, Long userId, String userName, Integer roleId,
			Integer webId, Menu menu, String agentId) {
		HttpSession session = request.getSession();
		session.setAttribute(LOGIN_USER_ID, userId);
		session.setAttribute(LOGIN_USER_REALNAME, userName);
		session.setAttribute(LOGIN_ROLE_ID, roleId);
		session.setAttribute(LOGIN_WEB_ID, webId);
		session.setAttribute(LOGIN_USER_MENU, menu);
		session.setAttribute(LOGIN_AGENT_ID, agentId);
	}

	/**
	 * 获取当前登录用户的id
	 * 
	 * @param request
	 * @return
	 */
	public static Long getLoginUserId(HttpServletRequest request) {
		Long id = null;
		Object obj = request.getSession().getAttribute(LOGIN_USER_ID);
		if (obj != null) {
			id = (Long) obj;
		}
		return id;
	}

	/**
	 * @Title: getLoginWebId
	 * @Description: 获取当前登录用户的webID
	 * @param request
	 * @return
	 * @return: Integer
	 */
	public static Integer getLoginWebId(HttpServletRequest request) {
		Integer webId = null;
		Object obj = request.getSession().getAttribute(LOGIN_WEB_ID);
		if (obj != null) {
			webId = (Integer) obj;
		}
		return webId;
	}

	/**
	 * 获取当前登录用户的roleId
	 * 
	 * @param request
	 * @return
	 */
	public static Integer getLoginRoleId(HttpServletRequest request) {
		Integer roleId = null;
		Object obj = request.getSession().getAttribute(LOGIN_ROLE_ID);
		if (obj != null) {
			roleId = Integer.parseInt(obj.toString());
		}
		return roleId;
	}

	/**
	 * 获取当前登录用户的菜单
	 *
	 * @param request
	 * @return
	 */
	public static Menu getLoginMenu(HttpServletRequest request) {
		Menu menu = null;
		Object obj = request.getSession().getAttribute(LOGIN_USER_MENU);
		if (obj != null) {
			menu = (Menu)obj;
		}
		return menu;
	}

	/**
	 * 获取当前登录用户的代理商ID
	 *
	 * @param request
	 * @return
	 */
	public static String getLoginAgentId(HttpServletRequest request) {
		String agentId = null;
		Object obj = request.getSession().getAttribute(LOGIN_AGENT_ID);
		if (obj != null) {
			agentId = obj.toString();
		}
		return agentId;
	}

	/**
	 * 退出当前登录用户
	 * 
	 * @param request
	 */
	public static void setLogoutUser(HttpServletRequest request) {
		HttpSession session = request.getSession();
		session.removeAttribute(LOGIN_USER_ID);
		session.removeAttribute(LOGIN_USER_REALNAME);
		session.removeAttribute(LOGIN_ROLE_ID);
		session.removeAttribute(LOGIN_WEB_ID);
		session.removeAttribute(LOGIN_USER_MENU);
	}

	/**
	 * 当前登录的用户名
	 * 
	 * @return
	 */
	public static final String getLoginRealName(HttpServletRequest request) {
		return (String) request.getSession(true).getAttribute(LOGIN_USER_REALNAME);
	}

	/**
	 * 判断当前是否已登录
	 * 
	 * @param request
	 * @return
	 */
	public static boolean isLogin(HttpServletRequest request) {
		Long sid = getLoginUserId(request);
		Integer webId = getLoginWebId(request);
		if (sid != null && webId != null && webId.toString().equals(ConfigUtils.web_id)) {// 已经登录
			return true;
		}
		return false;
	}

}
