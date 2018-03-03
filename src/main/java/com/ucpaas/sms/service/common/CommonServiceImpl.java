package com.ucpaas.sms.service.common;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ucpaas.sms.constant.AgentStatusEnum;
import com.ucpaas.sms.constant.UserConstant;
import com.ucpaas.sms.mapper.message.CommonMapper;
import com.ucpaas.sms.mapper.message.MenuMapper;
import com.ucpaas.sms.mapper.message.UserMapper;
import com.ucpaas.sms.model.Menu;
import com.ucpaas.sms.model.User;
import com.ucpaas.sms.service.util.ConfigUtils;
import com.ucpaas.sms.service.util.EmailUtils;
import com.ucpaas.sms.util.CommonUtils;
import com.ucpaas.sms.util.Encodes;
import com.ucpaas.sms.util.MD5;
import com.ucpaas.sms.util.SecurityUtils;

/**
 * 公共业务
 * 
 * @author xiejiaan
 */
@Service
@Transactional
public class CommonServiceImpl implements CommonService {
	private static final Logger logger = LoggerFactory.getLogger(CommonServiceImpl.class);

	@Autowired
	private MenuMapper menuMapper;

	@Autowired
	private UserMapper userMapper;

	@Autowired
	private CommonMapper commonMapper;

	@Override
	public String getMostAgentNumForMonth(String agentIdPre) {
		return commonMapper.getMostAgentNumForMonth(agentIdPre);
	}

	@Override
	public Map<String, Object> submitEmail(Map<String, String> params) {
		Map<String, Object> data = new HashMap<>();

		{
			// 校验验证码
			String validCode = params.get("captcha");
			String randCheckCode = params.get("randCheckCode");
			if (!validCode.equalsIgnoreCase(randCheckCode)) {
				data.put("result", "fail");
				data.put("msg", "验证码不正确，请重新输入！");
				return data;
			}
		}

		{

			// 校验邮箱是否存在
			int num = userMapper.getCountByEmail(params.get("email"));
			if (num == 0) {
				data.put("result", "fail");
				data.put("msg", "邮箱不存在！");
				return data;
			}
		}

		// 发送邮件
		String to = params.get("email");
		// String subject = "重置密码连接";
		// String body =
		// "http://127.0.0.1:8080/ucpaas-sms/agent/common/resetPwd?email=huangzaizheng@163.com";
		// String body = ConfigUtils.smap_resetpwd_url+"?email="+to;
		// String body = ConfigUtils.smap_resetpwd_url;

		String now = String.valueOf(new Date().getTime());
		String vUrl = ConfigUtils.agent_site_url;// 代理商公用服务器站点地址

		Map<String, Object> smsMailpropMap = commonMapper.querySmsMailProp("100021");
		String body = smsMailpropMap.get("text").toString();
		String subject = smsMailpropMap.get("subject").toString();
		// body = body.replaceAll("remail", to);
		body = body.replaceAll("remail",
				Encodes.encodeBase64(to + "&" + Encodes.encodeBase64(now) + "&" + SecurityUtils.encryptMD5(to + now)));
		body = body.replace("vUrl", vUrl);

		boolean flag = EmailUtils.sendHtmlEmail(to, subject, body);
		if (flag == false) {
			data.put("result", "fail");
			data.put("msg", "发送失败！");
			return data;
		}

		data.put("result", "success");
		data.put("msg", "提交成功，请查看邮件！");
		return data;
	}

	@Override
	public Map<String, Object> login(String username, String password) {
		// 默认成功
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("result", "success");
		data.put("msg", "登录成功");

		logger.debug("登录开始：" + username);

		if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
			data.put("result", "fail");
			data.put("msg", "用户名或密码不能为空");
			return data;
		}

		User userForQuery = new User();
		userForQuery.setUserName(username);
		userForQuery.setPassword(MD5.md5(password));

		Map<String, Object> user = userMapper.getUserForLogin(userForQuery);
		if (user == null) {
			data.put("result", "fail");
			data.put("msg", "用户名或密码错误，请重新输入");
			return data;
		}

		// 判断代理商是否已经被注销和冻结
		Map<String, Object> agentUser = userMapper.getUserJoinAgent(userForQuery);
		if (agentUser == null) {
			data.put("result", "fail");
			data.put("msg", "用户名或密码错误，请重新输入");
			return data;
		}

		String status = (String) agentUser.get("status");
		if (AgentStatusEnum.注销.getValue().equals(status) || AgentStatusEnum.冻结.getValue().equals(status)) {
			data.put("result", "fail");
			data.put("msg", "用户已被冻结或者注销");
			return data;
		}

		if (Integer.parseInt(user.get("roleStatus").toString()) == UserConstant.ROLE_STATUS_0) {
			data.put("result", "fail");
			data.put("msg", "管理身份（" + user.get("roleName") + "）已删除，登录失败");
			return data;
		}

		switch (Integer.parseInt(user.get("status").toString())) {
		case UserConstant.USER_STATUS_2:
			data.put("result", "fail");
			data.put("msg", "用户已锁定，登录失败");
			return data;
		case UserConstant.USER_STATUS_3:
			data.put("result", "fail");
			data.put("msg", "用户已删除，登录失败");
			return data;
		}

		// 更新登录次数
		userMapper.updateLoginTimes(user.get("id").toString());

		logger.debug("登录成功：" + username);

		// Add by lpjLiu 2017-04-25 增加菜单查询 begin
		Integer roleId = Integer.parseInt(user.get("roleId").toString());
		Map<String, Object> queryMenuParams = new HashMap<String, Object>();
		queryMenuParams.put("roleId", roleId);

		List<Menu> menus = menuMapper.getRoleMenu(queryMenuParams);
		Menu menuRoot = CommonUtils.toTree(menus);

		Map<String, Object> resultData = new HashedMap();
		resultData.put("userId", user.get("id"));
		resultData.put("userName", user.get("realname"));
		resultData.put("roleId", roleId);
		resultData.put("webId", user.get("web_id"));
		resultData.put("menu", menuRoot);
		data.put("resultData", resultData);
		return data;
	}

	/**
	 * 获取系统参数
	 */
	@Override
	public Map<String, Object> getSysParams(String paramKey) {
		return commonMapper.getSysParams(paramKey);
	}

}
