package com.ucpaas.sms.mapper.message;

import java.util.List;
import java.util.Map;

import com.jsmsframework.product.entity.JsmsOemProductInfo;
import org.springframework.stereotype.Repository;

import com.ucpaas.sms.model.Menu;

@Repository
public interface ProductMapper {
	List<Map<String, Object>> query(Map<String, Object> params);

	Integer queryCount(Map<String, Object> params);

	String getMostNum(Map<String, Object> params);

	List<JsmsOemProductInfo> findOemProductInfo();
}