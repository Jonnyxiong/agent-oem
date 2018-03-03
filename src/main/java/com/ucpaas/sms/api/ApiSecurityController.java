package com.ucpaas.sms.api;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Maps;
import com.ucpaas.sms.common.annotation.IgnoreAuth;
import com.ucpaas.sms.common.entity.R;
import com.ucpaas.sms.model.User;
import com.ucpaas.sms.service.admin.AdminService;
import com.ucpaas.sms.service.common.CommonService;
import com.ucpaas.sms.util.StringUtils;
import com.ucpaas.sms.util.web.AuthorityUtils;
import com.ucpaas.sms.util.web.ControllerUtils;

/**
 * Created by lpjLiu on 2017/5/31.
 */
@RestController
@RequestMapping("/api")
public class ApiSecurityController {

	/**
	 * 日志对象
	 */
	protected Logger logger = LoggerFactory.getLogger(ApiSecurityController.class);

	@Autowired
	CommonService commonService;

	@Autowired
	private AdminService adminService;

	/**
	 * 发送重置密码邮件
	 * 
	 * @param email
	 * @param captcha
	 */
	@IgnoreAuth
	@PostMapping("/password/email")
	public R passwordSendEmail(String email, String captcha, HttpServletRequest request) {
		R r;
		try {
			Map<String, String> params = Maps.newHashMap();
			params.put("email", email);
			params.put("captcha", captcha);

			Object cache = ControllerUtils.getSessionAttribute(request, "randCheckCode");
			cache = cache == null ? "" : cache;
			params.put("randCheckCode", cache.toString());

			// 登录
			Map data = commonService.submitEmail(params);

			r = "success".equals(data.get("result").toString()) ? R.ok(data.get("msg").toString())
					: R.error(data.get("msg").toString());
		} catch (Exception e) {
			logger.error("找回密码时发送重置密码邮件失败\n email{} 消息{}", email, e);
			r = R.error("服务器异常,正在检修中...");
		}

		return r;
	}

	/**
	 * 重置密码
	 * 
	 * @param email
	 * @param password
	 */
	@IgnoreAuth
	@PostMapping("/password/reset")
	public R doPasswordReset(String email, String password, HttpServletRequest request) {
		R r;
		if (StringUtils.isBlank(email) || StringUtils.isBlank(password)) {
			r = R.error("参数错误");
			return r;
		}

		try {
			User user = new User();
			user.setEmail(email);
			user.setPassword(password);

			// 登录
			Map data = adminService.confirmResetPwd(user);

			r = "success".equals(data.get("result").toString()) ? R.ok(data.get("msg").toString())
					: R.error(data.get("msg").toString());
		} catch (Exception e) {
			logger.error("重置密码失败\n email{} 消息{}", email, e);
			r = R.error("服务器异常,正在检修中...");
		}

		return r;
	}

	/**
	 * 修改密码
	 * 
	 * @param oldPassword
	 * @param newPassword
	 */
	@PostMapping("/password/save")
	public R doPasswordMod(String oldPassword, String newPassword, HttpServletRequest request) {
		R r;
		if (StringUtils.isBlank(oldPassword) || StringUtils.isBlank(newPassword)) {
			r = R.error("参数错误");
			return r;
		}
		try {
			User user = new User(AuthorityUtils.getLoginUserId(request).toString());
			user.setPassword(newPassword);
			user.setOldPassword(oldPassword);
			boolean checkPwd = adminService.checkPassword(user); // 检验原始密码是否正确

			if (!checkPwd) {
				r = R.error("原密码错误");
			} else {
				boolean result = adminService.updateSecurityInfo(user); // 修改密码
				r = result ? R.ok("操作成功") : R.error("操作失败");
			}

		} catch (Exception e) {
			logger.error("修改密码失败\n 消息{}", e);
			r = R.error("服务器异常,正在检修中...");
		}

		return r;
	}
}
