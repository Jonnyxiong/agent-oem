package com.ucpaas.sms.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

public class RegexUtils {
	private static final Logger LOG = LoggerFactory.getLogger(RegexUtils.class);
	/**
	 * 验证邮箱地址是否正确
	 * 
	 * @param email
	 * @return
	 */
	public static boolean checkEmail(String email) {
		boolean flag = false;
		try {
			String check = "^([a-zA-Z0-9_-])+@([a-zA-Z0-9_-])+((\\.[a-zA-Z0-9_-]{2,3}){1,2})$";
			Pattern regex = Pattern.compile(check);
			Matcher matcher = regex.matcher(email);
			flag = matcher.matches();
		} catch (Throwable e) {
			LOG.error("验证邮箱地址错误", e);
			flag = false;
		}

		return flag;
	}

	/**
	 * 验证手机号码
	 * 
	 * @param mobiles
	 * @return
	 */
	public static boolean isMobileNO(String mobiles) {
		boolean flag = false;
		try {
			Pattern p = Pattern.compile("^((13[0-9])|(15[^4,\\D])|(18[0,5-9]))\\d{8}$");
			Matcher m = p.matcher(mobiles);
			flag = m.matches();
		} catch (Throwable e) {
			LOG.error("验证手机号码错误", e);
			flag = false;
		}
		return flag;
	}
	
	public static boolean isMobile(String patterns){
		boolean flag = false;
		List<String> regexList = new ArrayList<>();
		regexList.add("^13\\d{9}$");
		regexList.add("^14[5|7|9]\\d{8}$");
		regexList.add("^15[0|1|2|3|5|6|7|8|9]\\d{8}$");
		regexList.add("^18\\d{9}$");
		regexList.add("^170[0|1|2|3|4|5|6|7|8|9]\\d{7}$");
		regexList.add("^17[1|5|6|7|8]\\d{8}$");
		regexList.add("^173\\d{8}$");
		for (String regex : regexList) {
			try {
				Pattern p = Pattern.compile(regex);
				Matcher m = p.matcher(patterns);
				flag = m.matches();
				if(flag){
					break ;
				}
			} catch (Exception e) {
				flag = false;
				continue;
			}
		}
		return flag;
	}
	
	public static boolean isOverSeaMobile(String phone){
		if(StringUtils.isEmpty(phone)){
			return false ;
		}
		if(phone.startsWith("00") && phone.length()>10){
			return true ;
		}
		return false ;
	}
}
