package com.ucpaas.sms.util.web;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

/**
 * ControllerUtils.
 * 
 * 实现获取Request/Response/Session与绕过jsp/freemaker直接输出文本的简化函数.
 * 
 * @author calvin
 */
public class ControllerUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(ControllerUtils.class);

	// -- header 常量定义 --//
	private static final String HEADER_ENCODING = "encoding";
	private static final String HEADER_NOCACHE = "no-cache";
	private static final String DEFAULT_ENCODING = "UTF-8";
	private static final boolean DEFAULT_NOCACHE = true;
	private static final long EXPIRES_SECONDS = 3600L;// 缓存时间(单位为秒)

	/**
	 * 设置HttpSession中Attribute的简化函数.
	 */
	public static void setSessionAttribute(HttpServletRequest request, String name, Object value) {
		HttpSession session = request.getSession(false);
		session.setAttribute(name, value);
	}

	/**
	 * 取得HttpSession中Attribute的简化函数.
	 */
	public static Object getSessionAttribute(HttpServletRequest request, String name) {
		HttpSession session = request.getSession(false);
		return (session != null ? session.getAttribute(name) : null);
	}

	/**
	 * 删除HttpSession中Attribute的简化函数.
	 */
	public static void removeSessionAttribute(HttpServletRequest request, String name) {
		HttpSession session = request.getSession(false);
		session.removeAttribute(name);
	}

	/**
	 * 取得HttpRequest中Parameter的简化方法.
	 */
	public static String getParameter(HttpServletRequest request, String name) {
		return request.getParameter(name);
	}

	/**
	 * 取得HttpRequest中Parameter的简化方法.
	 */
	public static String getParameterTrim(HttpServletRequest request, String name) {
		return StringUtils.trim(getParameter(request, name));
	}

	/**
	 * 设置HttpRequest中Parameter的简化方法.
	 * 
	 * @param name
	 * @param value
	 */
	public static void setAttribute(HttpServletRequest request, String name, Object value) {
		request.setAttribute(name, value);
	}

	/**
	 * 获取提交的表单数据，多个值用,分割
	 * 
	 * @return
	 */
	public static Map<String, String> getFormData(HttpServletRequest request) {
		Map<String, String> formData = new HashMap<String, String>();
		String value;
		for (Map.Entry<String, String[]> map : request.getParameterMap().entrySet()) {
			value = StringUtils.join(map.getValue(), ",");
			if (StringUtils.isNotBlank(value)) {
				formData.put(map.getKey(), value.trim());
			}
		}

		LOGGER.debug("\n\nformData-------------------------" + formData + "\n");
		return formData;
	}

	/**
	 * 获取请求的源页面url
	 * 
	 * @return
	 */
	public static String getReferer(HttpServletRequest request) {
		return request.getHeader("referer");
	}

	/**
	 * 获取当前Web应用路径
	 * 
	 * @return
	 */
	public static String getContextPath(HttpServletRequest request) {
		return request.getContextPath();
	}

	/**
	 * 获取请求的url
	 * 
	 * @return
	 */
	public static String getRequestURI(HttpServletRequest request) {
		return request.getRequestURI();
	}

	/**
	 * 获取访问者的IP地址
	 * 
	 * @return
	 */
	public static String getClientIP(HttpServletRequest request) {
		String ip = request.getHeader("X-Real-IP");
		if (ip == null || ip.equals("")) {
			ip = request.getRemoteAddr();
		}
		return ip;
	}

	// -- 绕过jsp/freemaker直接输出文本的函数 --//
	/**
	 * 直接输出内容的简便函数.
	 * 
	 * eg. render("text/plain", "hello", "encoding:GBK"); render("text/plain",
	 * "hello", "no-cache:false"); render("text/plain", "hello", "encoding:GBK",
	 * "no-cache:false");
	 * 
	 * @param headers
	 *            可变的header数组，目前接受的值为"encoding:"或"no-cache:",默认值分别为UTF-8和true.
	 */
	public static void render(HttpServletResponse resp, final String contentType, final String content,
			final String... headers) {
		HttpServletResponse response = initResponseHeader(resp, contentType, headers);
		try {
			response.getWriter().write(content);
			response.getWriter().flush();
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	/**
	 * 直接输出文本.
	 * 
	 * @see #render(HttpServletResponse, String, String, String...)
	 */
	public static void renderText(HttpServletResponse resp, final String text, final String... headers) {
		render(resp, ServletUtils.TEXT_TYPE, text, headers);
	}

	/**
	 * 直接输出HTML.
	 * 
	 * @see #render(HttpServletResponse, String, String, String...)
	 */
	public static void renderHtml(HttpServletResponse resp, final String html, final String... headers) {
		render(resp, ServletUtils.HTML_TYPE, html, headers);
	}

	/**
	 * 直接输出XML.
	 * 
	 * @see #render(HttpServletResponse, String, String, String...)
	 */
	public static void renderXml(HttpServletResponse resp, final String xml, final String... headers) {
		render(resp, ServletUtils.XML_TYPE, xml, headers);
	}

	/**
	 * 直接输出JSON.
	 * 
	 * @param jsonString
	 *            json字符串.
	 * @see #render(HttpServletResponse, String, String, String...)
	 */
	public static void renderJson(HttpServletResponse resp, final String jsonString, final String... headers) {
		render(resp, ServletUtils.JSON_TYPE, jsonString, headers);
	}

	/**
	 * 直接输出JSON,使用Jackson转换Java对象.
	 * 
	 * @param data
	 *            可以是List<POJO>, POJO[], POJO, 也可以Map名值对.
	 * @see #render(HttpServletResponse, String, String, String...)
	 */
	public static void renderJson(final Object data, final String... headers) {
		String jsonString = JSON.toJSONString(data);
		renderJson(jsonString, headers);
	}

	/**
	 * 直接输出支持跨域Mashup的JSONP.
	 * 
	 * @param callbackName
	 *            callback函数名.
	 * @param object
	 *            Java对象,可以是List<POJO>, POJO[], POJO ,也可以Map名值对, 将被转化为json字符串.
	 */
	public static void renderJsonp(HttpServletResponse resp, final String callbackName, final Object object,
			final String... headers) {
		String jsonString = JSON.toJSONString(object);

		String data = new StringBuilder().append(callbackName).append("(").append(jsonString).append(");").toString();

		// 渲染Content-Type为javascript的返回内容,输出结果为javascript语句,
		// 如callback197("{html:'Hello World!!!'}");
		render(resp, ServletUtils.JS_TYPE, data, headers);
	}

	/**
	 * 分析并设置contentType与headers.
	 */
	public static HttpServletResponse initResponseHeader(HttpServletResponse response, final String contentType,
			final String... headers) {
		// 分析headers参数
		String encoding = DEFAULT_ENCODING;
		boolean noCache = DEFAULT_NOCACHE;
		long expiresSeconds = EXPIRES_SECONDS;
		for (String header : headers) {
			String headerName = StringUtils.substringBefore(header, ":");
			String headerValue = StringUtils.substringAfter(header, ":");

			if (StringUtils.equalsIgnoreCase(headerName, HEADER_ENCODING)) {
				encoding = headerValue;
			} else if (StringUtils.equalsIgnoreCase(headerName, HEADER_NOCACHE)) {
				noCache = Boolean.parseBoolean(headerValue);
			} else if (StringUtils.equalsIgnoreCase(headerName, "expiresSeconds")) {
				expiresSeconds = Long.parseLong(headerValue);
			} else {
				throw new IllegalArgumentException(headerName + "不是一个合法的header类型");
			}
		}

		// 设置headers参数
		String fullContentType = contentType + ";charset=" + encoding;
		response.setContentType(fullContentType);
		if (noCache) {
			ServletUtils.setDisableCacheHeader(response);
		} else {
			ServletUtils.setExpiresHeader(response, expiresSeconds);
		}

		return response;
	}

	public static String getAllContextPath(HttpServletRequest request) {
		if (null == request)
			return null;

		return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
				+ request.getContextPath();
	}

	public static void buildMapParams(Map<String, String> params) {
		Set<String> set = params.keySet();
		for (Iterator<String> iterator = set.iterator(); iterator.hasNext();) {
			Object obj = (Object) iterator.next();
			Object value = (Object) params.get(obj);
			String str = (String) value;
			if (StringUtils.isBlank(str)) {
				iterator.remove();
			}
		}
	}

	public static Map<String, String> buildQueryMap(String pageRowCount, String currentPage) {
		Map<String, String> params = Maps.newHashMap();
		params.put("pageRowCount", pageRowCount);
		params.put("currentPage", currentPage);
		return params;
	}

	public static Map<String, String> buildQueryMap(String pageRowCount, String currentPage,
			HttpServletRequest request) {
		Map<String, String> params = buildQueryMap(pageRowCount, currentPage);
		params.put("agent_id", AuthorityUtils.getLoginAgentId(request));
		return params;
	}

	public static Map<String, String> buildQueryMap(HttpServletRequest request) {
		Map<String, String> params = Maps.newHashMap();
		params.put("agent_id", AuthorityUtils.getLoginAgentId(request));
		return params;
	}
}
