package com.ucpaas.sms.dao;

import javax.annotation.Resource;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

/**
 * ucpaas_message_statistics 主库DAO
 *
 */
@Repository
public class AccessMasterDao extends BaseDao {

	@Override
	@Resource(name = "access_master_sqlSessionTemplate")
	protected void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {
		this.sqlSessionTemplate = sqlSessionTemplate;
	}

}
