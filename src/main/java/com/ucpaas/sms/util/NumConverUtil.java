/**    
 * @Title: NumConverUtil.java  
 * @Package: com.ucpaas.sms.util  
 * @Description: TODO()  
 * @author: Niu.T    
 * @date: 2016年9月3日 下午3:30:52  
 * @version: V1.0    
 */
package com.ucpaas.sms.util;

/**  
 * @ClassName: NumConverUtil  
 * @Description: 数字转换工具类  
 * @author: Niu.T 
 * @date: 2016年9月3日 下午3:30:52  
 */
public class NumConverUtil {
	/**
	 * @Description: 10进制转换成36进制
	 * @author: Niu.T 
	 * @date: 2016年9月3日 下午3:31:34  
	 * @param i
	 * @param str 需要传入空字符串
	 * @return: String
	 */
	public static String converTo36HEX(long i ){
		return converTo36HEX(i,"");
	}
	private static String converTo36HEX(long i,String str ){
		if(i / 36 > 0){
			if(i % 36 > 9) str = (char)(i % 36 + 87) + str;
			else str = i % 36 + str;
			return converTo36HEX(i/36,str);
		}else{
			if(i % 36 > 9) str = (char)(i % 36 + 87) + str;
			else str = i % 36 + str;
			return str ;
		}
	}
	/**
	 * @Description: 36进制转换为10进制
	 * @author: Niu.T 
	 * @date: 2016年9月3日 下午3:32:01  
	 * @param str
	 * @return: long
	 */
	public static long converToDecimal(String str){
		char[] c = str.toCharArray();
		long x = 0;
		for (int i = 0; i < c.length; i++) {
			x += (long) Math.pow(36, c.length - 1 - i) * (c[i] > 96 ? c[i] - 87 : c[i] - 48 );
		}
		return x;
	}	
}
