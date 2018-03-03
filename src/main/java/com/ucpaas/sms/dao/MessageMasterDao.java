package com.ucpaas.sms.dao;

import javax.annotation.Resource;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

/**
 * ucpaas_message主库DAO
 *
 */
@Repository
public class MessageMasterDao extends BaseDao {

	@Override
	@Resource(name = "message_sqlSessionTemplate")
	protected void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {
		this.sqlSessionTemplate = sqlSessionTemplate;
	}

}
