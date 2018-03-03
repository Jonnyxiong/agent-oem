package com.ucpaas.sms.mapper.message;

import org.springframework.stereotype.Repository;

import com.ucpaas.sms.model.Log;

@Repository
public interface LogMapper {
	int addLog(Log log);
}