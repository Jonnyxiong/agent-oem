package com.ucpaas.sms.service.common;

import java.util.Map;

/**
 * 公共业务
 * 
 * @author xiejiaan
 */
public interface CommonService {

	String getMostAgentNumForMonth(String agentIdPre);

	Map<String,Object> submitEmail(Map<String,String> params);

	/**
	 * 用户登录
	 * 
	 * @param username
	 * @param password
	 *            未加密字符串
	 * @return
	 */
	Map<String, Object> login(String username, String password);

	/**
	 * @Description: 获取系统参数
	 * @author: Niu.T 
	 * @date: 2016年10月14日 下午3:54:12  
	 * @param paramKey
	 * @return: Map<String,Object>
	 */
	Map<String,Object> getSysParams(String paramKey);
}
