package com.ucpaas.sms.mapper.message;

import java.util.Map;

import org.springframework.stereotype.Repository;

@Repository
public interface CommonMapper {
	String getMostAgentNumForMonth(String agentIdPre);

	Map<String, Object> querySmsMailProp(String id);

	Map<String, Object> getSysParams(String paramKey);

}