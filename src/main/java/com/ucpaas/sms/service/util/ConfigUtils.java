package com.ucpaas.sms.service.util;

import com.jsmsframework.common.dto.JsmsStaticInitVariable;
import com.jsmsframework.common.enums.WebId;
import com.jsmsframework.finance.service.JsmsOnlinePaymentService;
import com.jsmsframework.finance.util.OnlinePaymentUtil;
import com.jsmsframework.sms.send.service.JsmsAsyncSend;
import com.ucpaas.sms.model.initstatic.StaticInitVariable;
import com.ucpaas.sms.service.common.CommonService;
import com.ucpaas.sms.service.customer.CustomerManageService;
import com.ucpaas.sms.service.sms.ProductService;
import com.ucpaas.sms.service.sms.SmsManageService;
import com.ucpaas.sms.util.DateUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 系统配置工具类
 *
 * @author xiejiaan
 */
@Component
public class ConfigUtils {
	private static final Logger logger = LoggerFactory.getLogger(ConfigUtils.class);

	@Autowired
	private ProductService agentProductService;

	@Autowired
	private CommonService commonService;

	@Autowired
	private SmsManageService smsManageService;

	@Autowired
	private CustomerManageService customerManageService;

	@Autowired
	private JsmsOnlinePaymentService jsmsOnlinePaymentService;

	@Autowired
	private JsmsAsyncSend jsmsAsyncSend;

	/**
	 * 运行环境：development（开发）、devtest（开发测试）、ClientSuccessRateRealtimeDTO（测试）、production（线上）
	 */
	public static String spring_profiles_active;
	/**
	 * 是否自动登录
	 */
	public static boolean is_auto_login;

	/**
	 * 配置文件路径
	 */
	public static String config_file_path;

	/**
	 * UEdiotr配置文件路径
	 */
	public static String ueditor_config_file_path;

	/**
	 * smsp-access短信请求URL , contenttype = json
	 */
	public static String smsp_access_url_json;
	/**
	 * smsp-access短信请求URL , contenttype = form
	 */
	public static String smsp_access_url_form;
	/**
	 * smsp-access_tim 定时短信请求URL
	 */
	public static String smsp_access_tim_url;

	/**
	 * smsp-access短信请求clientid
	 */
	public static String smsp_access_clientid;

	/**
	 * smsp-access短信请求password
	 */
	public static String smsp_access_password;

	/**
	 * 重置密码路径
	 */
	public static String smap_resetpwd_url;

	/**
	 * 代理商公用路径(代理商保存上传的证件图片)
	 */
	public static String client_oauth_pic;

	// /**
	// * 客户公用路径(客户保存上传的证件图片)
	// */
	// public static String client_oauth_pic;

	/**
	 * 公用的代理商服务器站点地址
	 */
	public static String agent_site_url;

	/**
	 * 公用的代理商服务器站点地址
	 */
	public static String client_site_url;

	/**
	 * oem客户端的地址
	 */
	public static String client_site_oem_url;

	/**
	 * 平台标志 ,用于区分客户OEM平台!
	 */
	public static String platform_order_identify;

	/**
	 * oem代理商平台下单标识3
	 */
	public static String platform_oem_agent_order_identify;

	public static String environmentFlag;

	public static  String current_tomcat_data_dir;

	/**
	 * <pre>
	 * 接口地址：刷新前台缓存信息
	 *
	 * 主账号key=main:[sid]
	 * 应用key=app:[appSid]
	 * 子账户key=client:[clientNumber]
	 * 白名单key=wl:[appSid]
	 * 短信模板key=tl:[templateId]
	 * </pre>
	 */
	public static String interface_url_flush;

	/**
	 * 接口地址：app审核通过后，分配短信号码
	 */
	public static String interface_url_getMsgNbr;

	/**
	 * rest接口的域名
	 */
	public static String rest_domain;
	/**
	 * rest接口的版本
	 */
	public static String rest_version;
	/**
	 * 前台站点的域名
	 */
	public static String ucpaas_domain;
	/**
	 * 文件本地保存路径
	 */
	public static String temp_file_dir;
	/**
	 * 文件服务器下载接口
	 */
	public static String file_download_url;

	/**
	 * 图片服务器地址
	 */
	public static String smsp_img_url;

	/**
	 * 系统版本号
	 */
	public static String system_version;

	/**
	 * 系统web_id
	 */
	public static String web_id;
	/**
	 * nav_backcolor
	 */
	public static String nav_backcolor;
	/**
	 * nav_text_color
	 */
	public static String nav_text_color;
	/**
	 * oem_domain_name
	 */
	public static String oem_domain_name;
	/**
	 * default_copyright
	 */
	public static String default_copyright;
	/**
	 * oem_agent_domain_name
	 */
	public static String oem_agent_domain_name;
	/**
	 * Excel最大导入数据数量
	 */
	public static String excel_max_import_num;
	/**
	 * agent_id 最小值
	 */
	public static String min_agent_id;

	/**
	 * 系统id
	 */
	public static String system_id;

	/**
	 * epay的商户id
	 */
	public static String epay_merId;
	/**
	 * epay的密钥
	 */
	public static String epay_key;
	/**
	 * epay的notify_url
	 */
	public static String notify_url;

	/**
	 * epay的return_url
	 */
	public static String return_url;
	/**
	 * 支付接口
	 */
	public static String pay_url;

	/**
	 * 初始化
	 */
	@PostConstruct
	public void init() {
		String path = ConfigUtils.class.getClassLoader().getResource("").getPath();
		config_file_path = path + "system.properties";

		logger.info("\n\n-------------------------【smsp-agent-oem, starting...】\n加载配置文件：\n{}\n", config_file_path);
		initValue();

		logger.info("\n\n-------------------------【smsp-agent-oem_v{}, {} 环境服务器启动】\n配置文件加载完毕\n", system_version,spring_profiles_active);

		this.initOrderIdPre();
		this.initAgentIdPre();
		this.initAgentOrderIdPreForOem();
		this.initClientOrderIdPreForOem();
		this.initPaymentIdPre();
		this.jsmsAsyncSend.initSendThread(smsp_access_url_json,smsp_access_url_form,current_tomcat_data_dir,null);
	}

	/**
	 * 初始化配置项的值
	 */
	private void initValue() {
		Field[] fields = ConfigUtils.class.getFields();
		Object fieldValue = null;
		String name = null, value = null, tmp = null;
		Class<?> type = null;
		Pattern pattern = Pattern.compile("\\$\\{(.+?)\\}");
		Matcher matcher = null;
		try {
			Properties properties = new Properties();
			properties.load(new FileInputStream(config_file_path));

			for (Field field : fields) {
				name = field.getName();
				value = properties.getProperty(name);
				if (StringUtils.isNotBlank(value)) {
					matcher = pattern.matcher(value);
					while (matcher.find()) {
						tmp = properties.getProperty(matcher.group(1));
						if (StringUtils.isBlank(tmp)) {
							logger.error("配置{}存在其它配置{}，请检查您的配置文件", name, matcher.group(1));
						}
						value = value.replace(matcher.group(0), tmp);
					}

					type = field.getType();
					if (String.class.equals(type)) {
						fieldValue = value;
					} else if (Integer.class.equals(type)) {
						fieldValue = Integer.valueOf(value);
					} else if (Boolean.class.equals(type)) {
						fieldValue = Boolean.valueOf(value);
					} else {
						fieldValue = value;
					}
					field.set(this, fieldValue);
				}
				logger.info("加载配置：{}={}", name, field.get(this));
			}
		} catch (Throwable e) {
			logger.error("初始化配置项的值失败：" + name + "=" + value, e);
		}
	}

	private void initOrderIdPre() {

		Date date = new Date();
		int num = 0;

		// 后面的1代表代理商下单
		String orderIdPre = DateUtils.formatDate(date, "yyMMdd") + DateUtils.formatDate(date, "HHmm")
				+ ConfigUtils.platform_order_identify;// 1 ,
		// platform_order_identify区分各个平台(客户与OEM)
		// ;
		String numStr = agentProductService.getTheMostNumForMinute(orderIdPre);
		if (numStr == null) {
			num = 1;
		} else {
			num = Integer.valueOf(numStr) + 1;
		}

		StaticInitVariable.ORDERID_PRE = orderIdPre;
		StaticInitVariable.ORDER_NUM = num;

	}

	private void initAgentOrderIdPreForOem() {

		Date date = new Date();
		int num = 0;
		// 后面的1代表代理商下单
		String orderIdPre = DateUtils.formatDate(date, "yyMMdd") + DateUtils.formatDate(date, "HHmm")
				+ ConfigUtils.platform_oem_agent_order_identify;// oem订单标识3
		String numStr = smsManageService.getTheMostNumForMinute(orderIdPre);
		if (numStr == null) {
			num = 1;
		} else {
			num = Integer.valueOf(numStr) + 1;
		}

		StaticInitVariable.OEM_AGENT_ORDERID_PRE = orderIdPre;
		StaticInitVariable.OEM_AGENT_ORDER_NUM = num;
	}

	private void initClientOrderIdPreForOem() {

		Date date = new Date();
		int num = 0;
		// 后面的1代表代理商下单
		String orderIdPre = DateUtils.formatDate(date, "yyMMdd") + DateUtils.formatDate(date, "HHmm")
				+ ConfigUtils.platform_oem_agent_order_identify;// oem订单标识3
		String numStr = customerManageService.getClientTheMostNumForMinute(orderIdPre);
		if (numStr == null) {
			num = 1;
		} else {
			num = Integer.valueOf(numStr) + 1;
		}

		StaticInitVariable.OEM_CLIENT_ORDERID_PRE = orderIdPre;
		StaticInitVariable.OEM_CLIENT_ORDER_NUM = num;
	}

	private void initAgentIdPre() {

		Date date = new Date();
		int num;
		String agentIdPre = DateUtils.formatDate(date, "yyyyMM");
		String numStr = commonService.getMostAgentNumForMonth(agentIdPre);
		if (numStr == null) {
			num = Integer.parseInt(min_agent_id);
		} else {
			if(Integer.valueOf(numStr)>=Integer.parseInt(min_agent_id)&&Integer.valueOf(numStr)<9999){
				num = Integer.valueOf(numStr) + 1;
			}else if(Integer.valueOf(numStr)<Integer.parseInt(min_agent_id)){
				num = Integer.parseInt(min_agent_id);
			}else{
				num =-1;
			}
		}
		JsmsStaticInitVariable.AGENTID_PRE = agentIdPre;
		JsmsStaticInitVariable.AGENT_NUM = num;
	}

	/**
	 * 初始化paymentId后缀
	 */
	private void initPaymentIdPre(){
		Date date = new Date();
		int num = 0;
		// 后面的1代表代理商下单
		String paymentIdPre = "Z"+ WebId.OEM代理商平台.getValue() + ConfigUtils.system_id + DateUtils.formatDate(date, "yyMMddHHmm");
		String numStr = jsmsOnlinePaymentService.getPaymentIdMostNum(paymentIdPre);
		if (numStr == null) {
			num = 0;
		} else {
			num = Integer.valueOf(numStr) + 1;
		}
		OnlinePaymentUtil.PAYMENTID_NUM = num;
		OnlinePaymentUtil.PAYMENTID_PRE = paymentIdPre;
	}


}
