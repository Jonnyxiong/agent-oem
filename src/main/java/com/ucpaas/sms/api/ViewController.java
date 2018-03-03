package com.ucpaas.sms.api;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.code.kaptcha.Producer;
import com.ucpaas.sms.common.annotation.IgnoreAuth;
import com.ucpaas.sms.util.Encodes;
import com.ucpaas.sms.util.SecurityUtils;
import com.ucpaas.sms.util.web.ControllerUtils;

/**
 * Created by lpjLiu on 2017/5/31.
 */
@Controller
public class ViewController {

	/**
	 * 日志对象
	 */
	protected Logger logger = LoggerFactory.getLogger(ViewController.class);

	@Autowired
	private Producer producer;

	/**
	 * 验证码获取
	 * 
	 * @param request
	 * @param response
	 */
	@IgnoreAuth
	@RequestMapping("/api/captcha.jpg")
	public void login(HttpServletRequest request, HttpServletResponse response){

		response.setHeader("Cache-Control", "no-store, no-cache");
		response.setContentType("image/jpeg");

		// 生成文字验证码
		String text = producer.createText();
		// 生成图片验证码
		BufferedImage image = producer.createImage(text);
		// 保存到session
		request.getSession().setAttribute("randCheckCode", text);
		ServletOutputStream out;
		try {
			out = response.getOutputStream();
			ImageIO.write(image, "jpg", out);
		} catch (IOException e) {
			logger.error("验证码写入失败\n 消息{}", request.getRequestURI(), e);
		}
	}

	@IgnoreAuth
	@RequestMapping(value = { "/forget"})
	public String forget(HttpServletRequest request) {
		return "index";
	}

	@RequestMapping(value = { "/", "/login", "/login.html", "/login.jsp" })
	public String login(HttpServletRequest request) {
		//return "login";
		return "index";
	}

	@RequestMapping("/index")
	public String index() {
		//return "forward:/oem_agent/index/view";
		return "index";
	}

	// 跳到密码修改页面
	/*
	 * @RequestMapping("/agent/common/modifyPwd") public String modifyPwd() {
	 * return "common/modifyPwd"; }
	 */

	// 跳到重置密码页面
	@IgnoreAuth
	@RequestMapping("/agent/common/resetPwd")
	public String resetPwd(@RequestParam Map<String, String> params, Model model) {
		ControllerUtils.buildMapParams(params);
		try {
			String encrypt = Encodes.decodeBase64String(params.get("email"));
			String[] data = encrypt.split("&");
			logger.debug("密码重置链接中的参数 ---->{}", encrypt);
			String oldTime = Encodes.decodeBase64String(data[1]);
			if (!data[2].equals(SecurityUtils.encryptMD5(data[0] + oldTime))) {
				return "redirect:/404";
			}
			// 链接有效期 10分钟
			if (new Date().getTime() > (Long.parseLong(oldTime) + 600 * 1000)) {
				model.addAttribute("overdue", true);
			}
			model.addAttribute("email", data[0]);
			return "common/resetPwd";
		} catch (Exception e) {
			logger.debug("重置密码路径解析错误  参数 --->{} \r\n 错误信息 :{}", params, e);
			return "redirect:/404";
		}
	}
}
