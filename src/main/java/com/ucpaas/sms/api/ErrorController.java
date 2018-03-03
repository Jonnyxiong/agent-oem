package com.ucpaas.sms.api;

import com.alibaba.fastjson.JSON;
import com.ucpaas.sms.common.annotation.IgnoreAuth;
import com.ucpaas.sms.common.base.BaseController;
import com.ucpaas.sms.common.entity.R;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Controller
public class ErrorController extends BaseController {

	@IgnoreAuth
	@RequestMapping(path = "/error")
	public String handle(HttpServletRequest request, HttpServletResponse response) {
		R r = R.error(Integer.valueOf(request.getAttribute("javax.servlet.error.status_code").toString()), "");
		// 如果是json格式的ajax请求
		String accept = request.getHeader("accept");
		String requestedWith = request.getHeader("X-Requested-With");
		if ((StringUtils.isNoneBlank(accept) && accept.contains("application/json"))
				|| (StringUtils.isNoneBlank(requestedWith)	&& requestedWith.contains("XMLHttpRequest"))) {
			response.setContentType("application/json;charset=utf-8");
			try {

				PrintWriter writer = response.getWriter();
				writer.write(JSON.toJSONString(r));
				writer.flush();
			} catch (IOException e1) {
				logger.error("请求失败写入错误\n url{} 消息{}", request.getRequestURI(), e1);
			}
			return null;
		} else {// 如果是普通请求

			return "/index";

			/*
			 * String result = null; switch ((Integer) r.get("code")) { case
			 * 403: result = "error/403"; break; case 404: result = "error/404";
			 * // result = "forward:/agent/index/view"; break; default: result =
			 * "error/500"; break; } return result;
			 */
		}
	}
}
