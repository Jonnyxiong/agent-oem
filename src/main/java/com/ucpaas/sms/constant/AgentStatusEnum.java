package com.ucpaas.sms.constant;

public enum AgentStatusEnum {
	
	注册未激活("0","注册未激活"),
	注册完成("1","注册未激活"),
	冻结("5","注册未激活"),
	注销("6","注册未激活");
	
	private String value;
	private String desc;
	
	private AgentStatusEnum(String value, String desc) {
		this.value = value;
		this.desc = desc;
	}

	public String getValue() {
		return value;
	}

	public String getDesc() {
		return desc;
	}


}
