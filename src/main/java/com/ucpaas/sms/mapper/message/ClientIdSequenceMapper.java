package com.ucpaas.sms.mapper.message;

import java.util.List;
import java.util.Map;

import com.ucpaas.sms.model.ClientIdSequence;
import org.springframework.stereotype.Repository;

import com.ucpaas.sms.model.Menu;

@Repository
public interface ClientIdSequenceMapper {
	ClientIdSequence getUnusedMin();

	ClientIdSequence getUnusedRandom();

	int lock(String clientId);

	String getMax();

	int batchAdd(List<String> list);

	int updateStatus(String clientId);

}