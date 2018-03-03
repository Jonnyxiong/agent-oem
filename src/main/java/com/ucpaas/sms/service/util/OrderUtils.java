package com.ucpaas.sms.service.util;

import com.ucpaas.sms.model.initstatic.StaticInitVariable;
import com.ucpaas.sms.util.DateUtils;
import com.ucpaas.sms.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * Created by lpjLiu on 2017/6/14.
 */
public class OrderUtils {

	private static final Logger logger = LoggerFactory.getLogger(OrderUtils.class);

	// 组装orderID
	public static synchronized Long getAgentOrderId() {

		Date date = new Date();
		int num = 0;

		String orderIdPre = DateUtils.formatDate(date, "yyMMdd") + DateUtils.formatDate(date, "HHmm")
				+ ConfigUtils.platform_oem_agent_order_identify;// oem代理商订单标识3

		if (orderIdPre.equals(StaticInitVariable.OEM_AGENT_ORDERID_PRE)) {
			num = StaticInitVariable.OEM_AGENT_ORDER_NUM;
			StaticInitVariable.OEM_AGENT_ORDER_NUM = num + 1;
		} else {
			StaticInitVariable.OEM_AGENT_ORDERID_PRE = orderIdPre;
			num = 1;
			StaticInitVariable.OEM_AGENT_ORDER_NUM = num + 1;
		}

		// 拼成订单号
		String orderIdStr = orderIdPre + StringUtils.addZeroForNum(num, 4, "0");
		Long orderId = Long.valueOf(orderIdStr);

		System.out.println("生成的代理商orderId------------->" + orderId);
		logger.debug("生成的代理商orderId------------->" + orderId);

		return orderId;
	}
}
