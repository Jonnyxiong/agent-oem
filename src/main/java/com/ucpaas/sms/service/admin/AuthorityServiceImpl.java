package com.ucpaas.sms.service.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ucpaas.sms.mapper.message.AuthorityMapper;

/**
 * 管理员中心-权限管理
 * 
 * @author xiejiaan
 */
@Service
@Transactional
public class AuthorityServiceImpl implements AuthorityService {
	@Autowired
	private AuthorityMapper authorityMapper;

	@Override
	public Map<String, Object> isAuthority(int roleId, String url) {
		Map<String, Object> data = isAuthorityEntry(roleId, url);
		return data;
	}

	private Map<String, Object> isAuthorityEntry(int roleId, String url) {
		Map<String, Object> data = new HashMap<String, Object>();

		boolean existsMenuUrl = authorityMapper.existsMenuUrl(url);
		if (!existsMenuUrl) {// 有权限的条件：1.没有配置菜单；2.分配了菜单，且菜单和角色状态是1
			data.put("result", 1);
			return data;
		}
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("menu_url", url);
		params.put("role_id", roleId);
		Map<String, Object> info = authorityMapper.getParentIds(params);// 查询父菜单
		if (info != null) {
			String[] parentIds = info.get("parent_ids").toString().split(",");
			List<Map<String, Object>> data2 = authorityMapper.getSelectMenu(parentIds);// 查询当前选中的菜单
			Map<String, Object> selectMenu = new HashMap<String, Object>();
			for (Map<String, Object> map : data2) {
				selectMenu.put("menu" + map.get("level") + "_id", map.get("menu_id"));
			}

			data.put("result", 1);
			data.put("select_menu", selectMenu);
			return data;
		}
		data.put("result", 0);
		return data;
	}

	@Override
	public boolean isAuthority(int roleId, int menuId) {
		Boolean data = null;
		if (data == null) {
			data = isAuthorityEntry(roleId, menuId);
		}
		return data;
	}

	public Boolean isAuthorityEntry(Integer roleId, Integer menuId) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("role_id", roleId);
		params.put("menu_id", menuId);
		int i = authorityMapper.isAuthorityMenuId(params);
		return i > 0 ? true : false;
	}

}
