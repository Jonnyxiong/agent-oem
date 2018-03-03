package com.ucpaas.sms.exception;

public class BusinessException extends RuntimeException {

	private static final long serialVersionUID = -567645347617112416L;

	private String errorCode;

	private String message;

	public String getErrorCode() {
		return errorCode;
	}

	public String getMessage() {
		return message;
	}

	// 两个参数都包含
	public BusinessException(String errorCode, String message) {
		super();
		this.errorCode = errorCode;
		this.message = message;
	}

	// 只包含消息
	public BusinessException(String message) {
		super();
		this.message = message;
	}

	// 空的构造函数
	public BusinessException() {
		super();
	}

}
