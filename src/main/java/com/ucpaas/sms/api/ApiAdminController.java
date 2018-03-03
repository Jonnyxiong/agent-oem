package com.ucpaas.sms.api;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.ucpaas.sms.common.entity.PageContainer;
import com.ucpaas.sms.dao.MessageMasterDao;
import com.ucpaas.sms.service.customer.CustomerManageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Maps;
import com.ucpaas.sms.common.entity.R;
import com.ucpaas.sms.model.OauthPic;
import com.ucpaas.sms.model.User;
import com.ucpaas.sms.service.admin.AdminService;
import com.ucpaas.sms.service.util.ConfigUtils;
import com.ucpaas.sms.util.StringUtils;
import com.ucpaas.sms.util.web.AuthorityUtils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * Created by lpjLiu on 2017/5/31.
 */
@Api(value = "管理员API", description = "管理员API")
@RestController
@RequestMapping("/api/admin")
public class ApiAdminController {

	/**
	 * 日志对象
	 */
	protected Logger logger = LoggerFactory.getLogger(ApiAdminController.class);

	@Autowired
	private AdminService adminService;
	@Autowired
	private CustomerManageService customerManageService;
	/**
	 * 个人资料信息
	 *
	 * @param request
	 * @return
	 */
	@ApiOperation(value = "获取管理员信息", notes = "根据Session管理员查询信息", response = R.class)
	@GetMapping("/info")
	public R adminInfo(HttpServletRequest request) {
		Map<String, Object> info = adminService.getAdmin(AuthorityUtils.getLoginUserId(request));
		return R.ok("获取个人信息成功", info);
	}

	/**
	 * 保存管理员信息
	 * 
	 *
	 * @param email
	 * @param realName
	 * @param mobile
	 * @param request
	 * @return
	 */
	@ApiOperation(value = "保存管理员信息", notes = "保存管理员信息", response = R.class, consumes = "application/json", produces = "application/json")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "email", value = "邮箱", required = true, dataType = "String", paramType = "form"),
			@ApiImplicitParam(name = "realName", value = "真实姓名", required = true, dataType = "String", paramType = "form"),
			@ApiImplicitParam(name = "mobile", value = "手机号码", required = true, dataType = "String", paramType = "form") })
	@PostMapping("/info/save")
	// public R saveAdminInfo(@ApiParam(value = "邮箱", required = true) String
	// email, @ApiParam(value = "真实姓名", required = true) String realName,
	// @ApiParam(value = "手机号码", required = true) String mobile,
	// HttpServletRequest request) {
	public R saveAdminInfo(String email, String realName, String mobile, HttpServletRequest request) {
		R r;

		if (StringUtils.isBlank(email) || StringUtils.isBlank(mobile) || StringUtils.isBlank(realName)) {
			r = R.error("数据验证失败");
			return r;
		}

		try {
			User user = new User(AuthorityUtils.getLoginUserId(request).toString());
			user.setEmail(email);
			user.setMobile(mobile);
			user.setRealName(realName);

			if (adminService.isExistByMobileOrEmail(user)) {
				r = R.error("邮箱或联系方式已存在！");

			} else {
				int result = adminService.updateAdminInfo(user);

				r = R.ok("个人资料修改成功！");
			}

		} catch (Exception e) {
			logger.debug(e.getMessage());
			r = R.error("服务器异常,正在检修中...");
		}
		return r;
	}

	/**
	 * 账户安全信息、弃用
	 *
	 */
	/*
	 * @GetMapping("/agent/info") public R adminAgentInfo(HttpServletRequest
	 * request) { User user =
	 * adminService.getUserInfo(AuthorityUtils.getLoginUserId(request).toString(
	 * )); return R.ok("获取账户安全信息成功", user); }
	 */

	/**
	 * 帐号基本信息
	 */
	@GetMapping("/baseinfo")
	public R accountBaseInfo(HttpServletRequest request) {
		// 获取用户登录的id
		Map<String, Object> info = adminService
				.getAgentInfoAndPackage(AuthorityUtils.getLoginUserId(request).toString());
		//获取余额不足的子账户个数
		Map<String, String> params = new HashMap<>();
		if (StringUtils.isNotBlank(String.valueOf(info.get("agent_id")))) {
			params.put("agent_id", String.valueOf(info.get("agent_id")));
		}
		int count = customerManageService.getCustomerOfAlarm(params);
		info.put("alarmCount",count);
		return R.ok("获取帐号基本信息成功", info);
	}

	/**
	 * 帐号资质信息
	 */
	@GetMapping("/cer/info")
	public R accountCerInfo(HttpServletRequest request) {
		Map<String, String> params = Maps.newHashMap();

		Map<String, Object> info = adminService.getCerInfo(AuthorityUtils.getLoginUserId(request).toString());
		String smspImgUrl = ConfigUtils.smsp_img_url.endsWith("/")
				? ConfigUtils.smsp_img_url.substring(0, ConfigUtils.smsp_img_url.lastIndexOf("/"))
				: ConfigUtils.smsp_img_url;
		info.put("smsp_img_url", smspImgUrl);

		return R.ok("获取账户资质信息成功", info);
	}

	/**
	 * 上传帐号资质信息
	 */
	@PostMapping("/cer/add")
	public R addAccountCer(String id_type, String company, String id_nbr, String imgUrl, HttpServletRequest request) {
		R r;
		OauthPic oauthPic = new OauthPic();
		oauthPic.setAgentId(AuthorityUtils.getLoginAgentId(request).toString());
		try {
			oauthPic.setIdType(id_type);
			oauthPic.setIdNbr(id_nbr);
			oauthPic.setImgUrl(imgUrl);

			Map<String, Object> data = adminService.saveCerInfo(oauthPic, company); // 上传资质信息
			r = "success".equals(data.get("result").toString()) ? R.ok(data.get("msg").toString(), data.get("datePath"))
					: R.error(data.get("msg").toString());
		} catch (Exception e) {
			logger.error("代理商资质添加失败\n agentId{} 消息{}", oauthPic.getAgentId(), e);
			r = R.error("服务器异常,正在检修中...");
		}
		return r;
	}

	/**
	 * 修改帐号资质信息
	 */
	@PostMapping("/cer/edit")
	public R editAccountCer(String id_type, String company, String id_nbr, String imgUrl, HttpServletRequest request) {
		R r;
		OauthPic oauthPic = new OauthPic();
		oauthPic.setAgentId(AuthorityUtils.getLoginAgentId(request).toString());
		try {
			oauthPic.setIdType(id_type);
			oauthPic.setIdNbr(id_nbr);
			if (StringUtils.isNotBlank(imgUrl)) {
				oauthPic.setImgUrl(imgUrl);
			}

			Map<String, Object> data = adminService.updateCerInfo(oauthPic, company); // 上传资质信息
			r = "success".equals(data.get("result").toString()) ? R.ok(data.get("msg").toString())
					: R.error(data.get("msg").toString());
		} catch (Exception e) {
			logger.error("代理商资质修改失败\n agentId{} 消息{}", oauthPic.getAgentId(), e);
			r = R.error("服务器异常,正在检修中...");
		}
		return r;
	}
}
