package com.ucpaas.sms.api;

import com.jsmsframework.common.util.FileUtils;
import com.ucpaas.sms.common.entity.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by lpjLiu on 2017/5/31.
 */
@Api(value = "帮助中心", description = "帮助中心")
@RequestMapping("/api/help")
@Controller
public class ApiHelpController {

	/**
	 * 日志对象
	 */
	protected Logger logger = LoggerFactory.getLogger(ApiHelpController.class);

	/**
	 * 1. 下载https文档
	 */
	@ApiOperation(value = "https文档", notes = "短信https接口文档V1.0.docx",tags = "帮助中心",response = R.class)
	@GetMapping("/httpsdoc")
	public void httpsdoc(HttpServletRequest request,HttpServletResponse response) {
		String path = request.getServletContext().getRealPath("/template/sms-https-introduce.docx");
		FileUtils.download("短信https接口文档V1.0.docx",path,response);
	}

	/**
	 * 2. 下载cmpp文档
	 */
	@ApiOperation(value = "cmpp文档", notes = "短信cmpp接口文档V2.0.doc",tags = "帮助中心", response = R.class)
	@GetMapping("/cmppdoc")
	public void cmppdoc(HttpServletRequest request,HttpServletResponse response) {
		String path = request.getServletContext().getRealPath("/template/sms-cmpp-api-v2.0.doc");
		FileUtils.download("短信cmpp接口文档V2.0.doc",path,response);
	}

	/**
	 * 3. 下载sgip文档
	 */
	@ApiOperation(value = "sgip文档", notes = "短信sgip接口文档V1.2.doc",tags = "帮助中心", response = R.class)
	@GetMapping("/sgipdoc")
	public void sgipdoc(HttpServletRequest request,HttpServletResponse response) {
		String path = request.getServletContext().getRealPath("/template/sms-sgip-api-v1.2.doc");
		FileUtils.download("短信sgip接口文档V1.2.doc",path,response);
	}

	/**
	 * 4. 下载smgp文档
	 */
	@ApiOperation(value = "smgp文档", notes = "短信smgp接口文档V3.0.doc",tags = "帮助中心", response = R.class)
	@GetMapping("/smgpdoc")
	public void smgpdoc(HttpServletRequest request,HttpServletResponse response) {
		String path = request.getServletContext().getRealPath("/template/sms-smgp-api-v3.0.3.doc");
		FileUtils.download("短信smgp接口文档V3.0.doc",path,response);
	}

	/**
	 * 5. 下载smpp文档
	 */
	@ApiOperation(value = "smpp文档", notes = "短信smpp接口文档V3.4.pdf",tags = "帮助中心", response = R.class)
	@GetMapping("/smppdoc")
	public void smppdoc(HttpServletRequest request,HttpServletResponse response) {
		String path = request.getServletContext().getRealPath("/template/sms-smpp-api-v3.4.pdf");
		FileUtils.download("短信smpp接口文档V3.4.pdf",path,response);
	}

	/**
	 * 6. 下载短信模板接口文档
	 */
	@ApiOperation(value = "模板接口文档", notes = "模板短信接口文档V1.0.docx", tags = "帮助中心",response = R.class)
	@GetMapping("/smstemplatedoc")
	public void smstemplatedoc(HttpServletRequest request,HttpServletResponse response) {
		String path = request.getServletContext().getRealPath("/template/模板短信接口文档V1.0.docx");
		FileUtils.download("模板短信接口文档V1.0.docx",path,response);
	}

	/**
	 * 7. 下载FAQ文档
	 */
	@ApiOperation(value = "FAQ文档", notes = "FAQ文档V1.0.docx", tags = "帮助中心",response = R.class)
	@GetMapping("/faqdoc")
	public void faqdoc(HttpServletRequest request,HttpServletResponse response) {
		String path = request.getServletContext().getRealPath("/template/sms-faq.docx");
		FileUtils.download("FAQ文档V1.0.docx",path,response);
	}


}
