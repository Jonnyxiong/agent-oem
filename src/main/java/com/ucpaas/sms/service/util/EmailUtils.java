package com.ucpaas.sms.service.util;

import com.ucpaas.sms.common.util.SpringContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.internet.MimeMessage;

/**
 * Created by lpjLiu on 2017/6/14.
 */
public class EmailUtils {

	private static final Logger logger = LoggerFactory.getLogger(EmailUtils.class);

	private static JavaMailSender javaMailSender = SpringContextUtils.getBean(JavaMailSender.class);

	/**
	 * 发送者
	 */
	private static final String from = "admin@ucpaas.com";

	public static boolean sendTextEmail(String to, String subject, String body) {
		logger.debug("发送文本格式的Email【开始】：to={}, subject={}, body={}", to, subject, body);
		try {
			SimpleMailMessage msg = new SimpleMailMessage();
			msg.setFrom(from);
			msg.setTo(to.split(","));
			msg.setSubject(subject);
			msg.setText(body);
			javaMailSender.send(msg);

			logger.debug("发送文本格式的Email【成功】：to={}, subject={}, body={}", to, subject, body);
			return true;
		} catch (Throwable e) {
			logger.error("发送文本格式的Email【失败】：to=" + to + ", subject=" + subject + ", body=" + body, e);
		}
		return false;
	}

	public static boolean sendHtmlEmail(String to, String subject, String body) {
		logger.debug("发送html格式的Email【开始】：to={}, subject={}, body={}", to, subject, body);
		try {
			MimeMessage msg = javaMailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(msg, false, "utf-8");
			helper.setFrom(from);
			helper.setTo(to.split(","));
			helper.setSubject(subject);
			helper.setText(body, true);
			javaMailSender.send(msg);

			logger.debug("发送html格式的Email【成功】：to={}, subject={}, body={}", to, subject, body);
			return true;
		} catch (Throwable e) {
			logger.error("发送html格式的Email【失败】：to=" + to + ", subject=" + subject + ", body=" + body, e);
		}
		return false;
	}
}
