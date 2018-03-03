package com.ucpaas.sms.common.base;

import java.io.Serializable;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

/**
 * Entity支持类
 * 
 * @version 2014-05-16
 */
public abstract class BaseEntity<T> implements Serializable, Cloneable {

	private static final long serialVersionUID = 1L;

	private String id;

	public BaseEntity() {

	}

	public BaseEntity(String id) {
		this.setId(id);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this);
	}
}
