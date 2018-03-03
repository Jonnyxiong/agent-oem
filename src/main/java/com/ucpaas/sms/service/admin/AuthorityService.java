package com.ucpaas.sms.service.admin;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.ucpaas.sms.common.entity.PageContainer;

/**
 * 管理员中心-权限管理
 * 
 * @author xiejiaan
 */
public interface AuthorityService {

	/**
	 * 判断角色是否对url有访问权限<br/>
	 * 有权限的条件：1.没有配置菜单；2.分配了菜单，且菜单和角色状态是1
	 * 
	 * @param roleId
	 * @param url
	 * @return 当前选中的菜单
	 */
	Map<String, Object> isAuthority(int roleId, String url);

	/**
	 * 判断当前角色是否对menuId有访问权限
	 * 
	 * @param menuId
	 * @return
	 */
	boolean isAuthority(int roleId, int menuId);

}
