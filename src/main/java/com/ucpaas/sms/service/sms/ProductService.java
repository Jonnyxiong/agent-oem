package com.ucpaas.sms.service.sms;

import com.ucpaas.sms.common.entity.PageContainer;

import java.util.Map;

/**
 * 代理商-产品管理
 * 
 * @author zenglb
 */
public interface ProductService {

	PageContainer query(Map<String, String> params);

	String getTheMostNumForMinute(String orderIdPre);

	void fixOemAgentProduct();
}
