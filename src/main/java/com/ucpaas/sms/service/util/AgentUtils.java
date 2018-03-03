package com.ucpaas.sms.service.util;

import java.util.Map;

import org.apache.commons.lang3.math.NumberUtils;

import com.ucpaas.sms.common.entity.PageContainer;
import com.ucpaas.sms.common.util.SpringContextUtils;
import com.ucpaas.sms.mapper.message.AgentInfoMapper;
import com.ucpaas.sms.model.AgentInfo;
import com.ucpaas.sms.service.admin.AuthorityService;

/**
 * Created by liulipengju on 2017/5/12. 公共的
 */
public class AgentUtils {
	private static AgentInfoMapper agentInfoMapper = SpringContextUtils.getBean(AgentInfoMapper.class);
	private static AuthorityService authorityService = SpringContextUtils.getBean(AuthorityService.class);

	public static AgentInfo queryAgentInfoByAdminId(String adminId) {
		return agentInfoMapper.getByUserId(adminId);
	}

	public static Map<String, Object> isAuthority(int roleId, String url) {
		return authorityService.isAuthority(roleId, url);
	}

	// public static Map<String, Object> queryAgentClientNum(Map<String, String>
	// params) {
	// return masterDao.selectOne("agent.index.queryAgentClientNum", params);
	// }

	public static void buildPageLimitParams(Map params, Integer totalCount, PageContainer p) {
		if (totalCount > 0) {
			String pageRowCountS = params.get("pageRowCount") != null ? params.get("pageRowCount").toString() : null;
			if (NumberUtils.isDigits(pageRowCountS)) {
				int pageRowCount = Integer.parseInt(pageRowCountS);
				if (pageRowCount > 0) {
					p.setPageRowCount(pageRowCount);
				}
			}
			int totalPage = totalCount / p.getPageRowCount() + (totalCount % p.getPageRowCount() == 0 ? 0 : 1);
			p.setTotalPage(totalPage);

			String currentPageS = params.get("currentPage") != null ? params.get("currentPage").toString() : null;
			if (NumberUtils.isDigits(currentPageS)) {
				int currentPage = Integer.parseInt(currentPageS);
				if (currentPage > 0 && currentPage <= totalPage) {
					p.setCurrentPage(currentPage);
				}
			}

			int startRow = (p.getCurrentPage() - 1) * p.getPageRowCount(); // 分页开始行号
			int rows = p.getPageRowCount();// 分页返回行数
			params.put("limit", "LIMIT " + startRow + ", " + rows);
		}
	}
}
