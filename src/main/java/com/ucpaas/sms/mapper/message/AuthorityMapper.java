package com.ucpaas.sms.mapper.message;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

@Repository
public interface AuthorityMapper {
	int isAuthorityMenuId(Map params);

	List<Map<String, Object>> getSelectMenu(String[] parentIds);

	Map<String, Object> getParentIds(Map params);

	boolean existsMenuUrl(String url);
}