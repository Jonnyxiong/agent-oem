package com.ucpaas.sms.common.entity;

import org.apache.poi.ss.formula.functions.T;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 分页查询容器
 * 
 * @author xiejiaan
 */
public class PageContainer implements Serializable {
	private static final long serialVersionUID = 8976331697912229579L;
	private List<T> entityList = new ArrayList<T>();// 查询结果

	private List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();// 查询结果
	private Integer currentPage = 1; // 当前页号
	private Integer totalPage = 1; // 总页数
	private Integer totalCount = 0;// 总行数
	private Integer pageRowCount = 30; // 默认每页显示行数
	private Integer[] pageRowArray = { 10, 30, 50, 100 };// 每页显示行数下拉框

	public List<T> getEntityList() {
		return entityList;
	}

	public void setEntityList(List<T> entityList) {
		this.entityList = entityList;
	}

	public List<Map<String, Object>> getList() {
		return list;
	}

	public void setList(List<Map<String, Object>> list) {
		this.list = list;
	}

	public Integer getCurrentPage() {
		return currentPage;
	}

	public void setCurrentPage(Integer currentPage) {
		this.currentPage = currentPage;
	}

	public Integer getTotalPage() {
		return totalPage;
	}

	public void setTotalPage(Integer totalPage) {
		this.totalPage = totalPage;
	}

	public Integer getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(Integer totalCount) {
		this.totalCount = totalCount;
	}

	public Integer getPageRowCount() {
		return pageRowCount;
	}

	public void setPageRowCount(Integer pageRowCount) {
		this.pageRowCount = pageRowCount;
	}

	public Integer[] getPageRowArray() {
		return pageRowArray;
	}

	public void setPageRowArray(Integer[] pageRowArray) {
		this.pageRowArray = pageRowArray;
	}
	
}
