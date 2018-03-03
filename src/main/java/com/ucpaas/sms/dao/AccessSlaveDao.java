package com.ucpaas.sms.dao;

import javax.annotation.Resource;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

/**
 * ucpaas_message_statistics从库DAO
 *
 */
@Repository
public class AccessSlaveDao extends BaseDao {

	@Override
	@Resource(name = "access_slave_sqlSessionTemplate")
	protected void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {
		this.sqlSessionTemplate = sqlSessionTemplate;
	}

}
