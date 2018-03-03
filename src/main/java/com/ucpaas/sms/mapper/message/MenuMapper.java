package com.ucpaas.sms.mapper.message;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import com.ucpaas.sms.model.Menu;

@Repository
public interface MenuMapper {
	List<Menu> getRoleMenu(Map<String, Object> params);
}