package com.ucpaas.sms.dao;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.math.NumberUtils;
import org.mybatis.spring.SqlSessionTemplate;

import com.ucpaas.sms.common.entity.PageContainer;

/**
 * dao类的基类
 * 
 * @author xiejiaan
 */
public abstract class BaseDao {

	protected SqlSessionTemplate sqlSessionTemplate;
	
	/**
	 * 设置SqlSessionTemplate
	 * 
	 * @param sqlSessionTemplate
	 */
	protected abstract void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate);

	public SqlSessionTemplate getSqlSessionTemplate() {
		return sqlSessionTemplate;
	}

	/**
	 * 查询List
	 * 
	 * @param queryStr
	 * @param sqlParams
	 * @return
	 */
	public <T> List<T> selectList(String queryStr, Object sqlParams) {
		return sqlSessionTemplate.selectList(queryStr, sqlParams);
	}

	/**
	 * 查询List
	 * 
	 * @param queryStr
	 * @param sqlParams
	 * @return
	 */
	public List<Map<String, Object>> getSearchList(String queryStr, Object sqlParams) {
		List<Map<String, Object>> list = sqlSessionTemplate.selectList(queryStr, sqlParams);
		return list;
	}

	/**
	 * 计算列表总数
	 * 
	 * @param countStr
	 * @param sqlParams
	 * @return
	 */
	public int getSearchSize(String countStr, Object sqlParams) {
		int totalCount = 0;
		List<Map<String, Object>> list = sqlSessionTemplate.selectList(countStr, sqlParams);
		if (list.size() > 0) {
			totalCount = Integer.parseInt(list.get(0).get("totalCount").toString());
		}
		return totalCount;
	}

	/**
	 * 分页查询
	 * 
	 * @param queryStr
	 * @param countStr
	 * @param sqlParams
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public PageContainer getSearchPage(String queryStr, String countStr, Map sqlParams) {
		PageContainer p = new PageContainer();
		int totalCount = getSearchSize(countStr, sqlParams);
		p.setTotalCount(totalCount);
		if (totalCount > 0) {
			String pageRowCountS = sqlParams.get("pageRowCount") != null ? sqlParams.get("pageRowCount").toString()
					: null;
			if (NumberUtils.isDigits(pageRowCountS)) {
				int pageRowCount = Integer.parseInt(pageRowCountS);
				if (pageRowCount > 0) {
					p.setPageRowCount(pageRowCount);
				}
			}
			int totalPage = totalCount / p.getPageRowCount() + (totalCount % p.getPageRowCount() == 0 ? 0 : 1);
			p.setTotalPage(totalPage);

			String currentPageS = sqlParams.get("currentPage") != null ? sqlParams.get("currentPage").toString() : null;
			if (NumberUtils.isDigits(currentPageS)) {
				int currentPage = Integer.parseInt(currentPageS);
				if (currentPage > 0 && currentPage <= totalPage) {
					p.setCurrentPage(currentPage);
				}
			}

			int startRow = (p.getCurrentPage() - 1) * p.getPageRowCount(); // 分页开始行号
			int rows = p.getPageRowCount();// 分页返回行数
			sqlParams.put("limit", "LIMIT " + startRow + ", " + rows);

			List<Map<String, Object>> list = getSearchList(queryStr, sqlParams);
			for (Map<String, Object> map : list) {
				map.put("rownum", ++startRow);
				Object orderId = map.get("order_id");
				Object remark = map.get("remark");
				if (orderId == null || "".equals(orderId)){
					map.put("order_id","-");
				}
				if (remark == null || "".equals(remark)){
					map.put("remark","-");
				}
			}
			p.setList(list);
		}
		return p;
	}

	/**
	 * 插入SQL执行
	 * 
	 * @param insertStr
	 * @param sqlParams
	 * @return
	 */
	public int insert(String insertStr, Object sqlParams) {
		return sqlSessionTemplate.insert(insertStr, sqlParams);
	}

	/**
	 * 更新SQL执行
	 * 
	 * @param updateStr
	 * @param sqlParams
	 * @return
	 */
	public int update(String updateStr, Object sqlParams) {
		return sqlSessionTemplate.update(updateStr, sqlParams);
	}

	/**
	 * 删除SQL执行
	 * 
	 * @param updateStr
	 * @param sqlParams
	 * @return
	 */
	public int delete(String updateStr, Object sqlParams) {
		return sqlSessionTemplate.delete(updateStr, sqlParams);
	}

	/**
	 * 根据条件返回单个对象
	 * 
	 * @param queryStr
	 * @param sqlParams
	 * @return
	 */
	public <T> T getOneInfo(String queryStr, Object sqlParams) {
		return sqlSessionTemplate.selectOne(queryStr, sqlParams);
	}

	
	/**
	 * 查询单个对象
	 * 
	 * @param sqlId
	 * @param sqlParams
	 * @return
	 */
	public <T> T selectOne(String sqlId, Object sqlParams) {
		return sqlSessionTemplate.selectOne(sqlId, sqlParams);
	}
}
