package com.ucpaas.sms.constant;

public enum LogEnum {
	管理中心("1"),
	系统配置("2"),
	用户配置("3"),
	短信通道质量实时监控("4"),
	短信通道历史记录查询("5"),
	短信发送记录("6"),
	验证质量实时监控("7"),
	验证质量历史记录监控("8"),
	验证消费实时监控("9"),
	验证消费历史记录("10"),
	短信报表("11"),
	销售统计报表("12"),
	账户管理("13"),
	短信审核("14"),
	通道管理("15"),
	产品管理("16"),
	财务管理("17"),
	账户信息管理("18"),
	代理商账户管理("19");


    private final String value;

    LogEnum(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
}
