package com.ucpaas.sms.util;

import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtil {

	private static Logger logger = LoggerFactory.getLogger(DateUtil.class);

	/**
	 * 变量：日期格式化类型 - 格式:yyyy/MM/dd
	 * 
	 * @since 1.0
	 */
	public static final int DEFAULT = 0;

	/**
	 * 变量：日期格式化类型 - 格式:yyyy/MM
	 * 
	 * @since 1.0
	 */
	public static final int YM = 1;

	/**
	 * 变量：日期格式化类型 - 格式:yyyy-MM-dd
	 * 
	 * @since 1.0
	 */
	public static final int YMR_SLASH = 11;

	/**
	 * 变量：日期格式化类型 - 格式:yyyyMMdd
	 * 
	 * @since 1.0
	 */
	public static final int NO_SLASH = 2;

	/**
	 * 变量：日期格式化类型 - 格式:yyyyMM
	 * 
	 * @since 1.0
	 */
	public static final int YM_NO_SLASH = 3;

	/**
	 * 变量：日期格式化类型 - 格式:yyyy/MM/dd HH:mm:ss
	 * 
	 * @since 1.0
	 */
	public static final int DATE_TIME = 4;

	/**
	 * 变量：日期格式化类型 - 格式:yyyyMMddHHmmss
	 * 
	 * @since 1.0
	 */
	public static final int DATE_TIME_NO_SLASH = 5;

	/**
	 * 变量：日期格式化类型 - 格式:yyyy/MM/dd HH:mm
	 * 
	 * @since 1.0
	 */
	public static final int DATE_HM = 6;

	/**
	 * 变量：日期格式化类型 - 格式:HH:mm:ss
	 * 
	 * @since 1.0
	 */
	public static final int TIME = 7;

	/**
	 * 变量：日期格式化类型 - 格式:HH:mm
	 * 
	 * @since 1.0
	 */
	public static final int HM = 8;

	/**
	 * 变量：日期格式化类型 - 格式:HHmmss
	 * 
	 * @since 1.0
	 */
	public static final int LONG_TIME = 9;
	/**
	 * 变量：日期格式化类型 - 格式:HHmm
	 * 
	 * @since 1.0
	 */

	public static final int SHORT_TIME = 10;

	/**
	 * 变量：日期格式化类型 - 格式:yyyy-MM-dd HH:mm:ss
	 * 
	 * @since 1.0
	 */
	public static final int DATE_TIME_LINE = 12;

	/**
	 * 变量：日期格式化类型 - 格式:yyyy
	 * 
	 * @since 1.0
	 */
	public static final int DATE_TIME_YEAR = 13;

	public static String dateToStr(Date date) {
		return dateToStr(date, "yyyy/MM/dd");
	}
	public static long getTime(String time) throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		long times = sdf.parse(time).getTime();
		return times;
	}
	public static String dateToStr(Date date, int type) {
		switch (type) {
			case DEFAULT :
				return dateToStr(date);
			case YM :
				return dateToStr(date, "yyyy/MM");
			case NO_SLASH :
				return dateToStr(date, "yyyyMMdd");
			case YMR_SLASH :
				return dateToStr(date, "yyyy-MM-dd");
			case YM_NO_SLASH :
				return dateToStr(date, "yyyyMM");
			case DATE_TIME :
				return dateToStr(date, "yyyy/MM/dd HH:mm:ss");
			case DATE_TIME_NO_SLASH :
				return dateToStr(date, "yyyyMMddHHmmss");
			case DATE_HM :
				return dateToStr(date, "yyyy/MM/dd HH:mm");
			case TIME :
				return dateToStr(date, "HH:mm:ss");
			case HM :
				return dateToStr(date, "HH:mm");
			case LONG_TIME :
				return dateToStr(date, "HHmmss");
			case SHORT_TIME :
				return dateToStr(date, "HHmm");
			case DATE_TIME_LINE :
				return dateToStr(date, "yyyy-MM-dd HH:mm:ss");
			case DATE_TIME_YEAR :
				return dateToStr(date, "yyyy");
			default :
				throw new IllegalArgumentException("Type undefined : " + type);
		}
	}

	public static Date getCurrentDate() {
		return new Date();
	}
	@SuppressWarnings("static-access")
	public static String getYstdDate() {
		Calendar cl = Calendar.getInstance();
		cl.add(cl.DATE, -1);
		return dateToStr(cl.getTime(), NO_SLASH);
	}
	@SuppressWarnings("static-access")
	public static String getLastYear() {
		Calendar cl = Calendar.getInstance();
		cl.add(cl.YEAR, -1);
		return dateToStr(cl.getTime(), DATE_TIME_YEAR);
	}

	public static long compare(Date date1, Date date2, long interval) {
		return date2.getTime() + interval - date1.getTime();
	}

	public static String dateToStr(Date date, String pattern) {
		DateTime dateTime = new DateTime(date);
		return dateTime.toString(pattern);

	}

	public static String changeStringPattern(String dateString, String oldPattern, String newPattern, Date defaultDate) {

		Date date = stringToDate(dateString, oldPattern);
		if (date == null) {
			date = defaultDate;
		}
		if (date != null) {
			return dateToStr(date, newPattern);
		} else {
			return null;
		}
	}

	public static Date stringToDate(String dateString, String parsePattern) {

		Date date = null;
		try {
			date = DateUtils.parseDate(dateString, parsePattern);
		} catch (ParseException e) {

			logger.error("字符串转换成时间出错 dateString = {} , parsePatters = {}", dateString, parsePattern);

		}

		return date;

	}

	/**
	 * 获取当前时间
	 * 
	 * @return 日期格式化类型 - 格式:yyyy-MM-dd HH:mm:ss
	 */
	public static String getToday() {

		return dateToStr(new Date(), DATE_TIME_LINE);
	}
	
	/**
	 * beforeOrAfterDayNumber 负数取当前日期的前N天
	 * beforeOrAfterDayNumber 正数取当前日期的后N天
	 * @param beforeOrAfterDayNumber
	 * @return
	 */
	public static Date getDateFromToday(int beforeOrAfterDayNumber){
		Calendar cal = Calendar.getInstance();//使用默认时区和语言环境获得一个日历。    
		cal.add(Calendar.DAY_OF_MONTH, beforeOrAfterDayNumber);//取当前日期的后一天. 
		return cal.getTime();
	}
	/**
	 * beforeOrAfterDayNumber 负数取当前日期的前N天
	 * beforeOrAfterDayNumber 正数取当前日期的后N天
	 * @param beforeOrAfterDayNumber
	 * @return int 返回int类型的日期 yyyyMMdd
	 */
	public static int getDateFromTodayInInt(int beforeOrAfterDayNumber){
		Calendar cal = Calendar.getInstance();//使用默认时区和语言环境获得一个日历。    
		cal.add(Calendar.DAY_OF_MONTH, beforeOrAfterDayNumber);//取当前日期的后一天. 
		String before90Str = DateUtil.dateToStr(cal.getTime(), "yyyyMMdd");
		return Integer.valueOf(before90Str);
	}
	
	public static Integer getYeasterDay(Integer yyyyMMdd) {
		if(yyyyMMdd == null )
			return null;
		Calendar endCal = Calendar.getInstance();
		endCal.set(yyyyMMdd/10000, yyyyMMdd%10000/100-1, yyyyMMdd%10000%100);
		endCal.add(Calendar.DATE, -1);
		return Integer.valueOf(dateToStr(endCal.getTime(), "yyyyMMdd"));
	}
	
	
	public static void main(String[] args) {
//		Date date = new Date();
//		System.out.println(dateToStr(date,2));
//		System.out.println(dateToStr(date,"yyMMdd"));
//		System.out.println("----------------->");
//		System.out.println(dateToStr(date,10));
//		System.out.println(dateToStr(date,2)+dateToStr(date,10));
//		
//		System.out.println("---------------->");
//		System.out.println(dateToStr(date,"yyMMdd")+dateToStr(date,"HHmm"));
//		
//		String str = "0010";
//		System.out.println(Integer.valueOf(str));
		System.out.println(getDateFromToday(30));
	}
	
	

	
}
