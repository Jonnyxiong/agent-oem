package com.ucpaas.sms.service.admin;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.ucpaas.sms.mapper.message.CommonMapper;
import com.ucpaas.sms.model.OauthPic;
import com.ucpaas.sms.service.util.EmailUtils;
import com.ucpaas.sms.util.DateUtils;
import com.ucpaas.sms.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ucpaas.sms.dao.MessageMasterDao;
import com.ucpaas.sms.mapper.message.AgentInfoMapper;
import com.ucpaas.sms.mapper.message.UserMapper;
import com.ucpaas.sms.model.AgentInfo;
import com.ucpaas.sms.model.User;
import com.ucpaas.sms.service.log.LogService;
import com.ucpaas.sms.service.util.ConfigUtils;
import com.ucpaas.sms.util.MD5;
import com.ucpaas.sms.util.SecurityUtils;

/**
 * 管理员中心
 * 
 * @author xiejiaan
 */
@Service
@Transactional
public class AdminServiceImpl implements AdminService {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(AdminServiceImpl.class);
	@Autowired
	private MessageMasterDao masterDao;
	@Autowired
	private LogService logService;

	@Autowired
	private UserMapper userMapper;

	@Autowired
	private AgentInfoMapper agentInfoMapper;

	@Autowired
	private CommonMapper commonMapper;

	@Override
	public Map<String, Object> getAdmin(Long id) {
		return userMapper.getAdminById(id.toString());
	}

	@Override
	public Boolean isExistByMobileOrEmail(User user) {
		Integer count = userMapper.getCountByMobileOrEmail(user);
		return count != null && count > 0;
	}

	@Override
	public int updateAdminInfo(User user) {
		// 更新管理原信息
		int userCount = userMapper.updateBaseInfo(user);

		// 更新代理商信息的 t_sms_agent_info 的mobile/agent_name
		AgentInfo agentInfo = new AgentInfo();
		agentInfo.setAdminId(user.getId());
		agentInfo.setAgentName(user.getRealName());
		agentInfo.setMobile(user.getMobile());
		int agentInfoCount = agentInfoMapper.updateBaseInfo(agentInfo);

		return userCount >= 1 && agentInfoCount >= 1 ? 1 : 0;
	}

	@Override
	public User getUserInfo(String adminId) {
		return userMapper.get(adminId);
	}

	@Override
	public Map<String, Object> getAgentInfoAndPackage(String adminId) {
		return agentInfoMapper.getAgentInfoAndPackage(adminId);
	}

	@Override
	public Map<String, Object> getCerInfo(String adminId) {
		Map<String, Object> data = userMapper.getCerInfo(adminId);
		int oauthStatus = (int) data.get("oauth_status");
		String imgUrl = (String) data.get("img_url");
		if (imgUrl != null && imgUrl != "") {
			data.put("img_url", SecurityUtils.encodeDes3(imgUrl));// 给图片路径加密
		}
		if (oauthStatus == 4) { // 如果代理商认证未通过需要查询认证未通过的原因
			String cause = agentInfoMapper.getAuthFailCauseByUser(adminId);
			if (StringUtils.isNotBlank(cause))
				data.put("remark", cause);
		}
		return data;
	}

	@Override
	public Map<String, Object> saveCerInfo(OauthPic oauthPic, String company) throws Exception {

		Map<String, Object> data = new HashMap<String, Object>();

		//buildCerInfoImgUrl(oauthPic, data);

		oauthPic.setOauthType("1");

		Date update_time = Calendar.getInstance().getTime();

		// 添加资质认证信息到数据库
		oauthPic.setUpdateDate(update_time);
		int saveNum = userMapper.saveCerInfo(oauthPic);

		// 更新代理商的信息(company字段)
		AgentInfo agentInfo = new AgentInfo();
		agentInfo.setAgentId(oauthPic.getAgentId());
		agentInfo.setCompany(company);
		agentInfo.setUpdateTime(update_time);
		int updateNum = agentInfoMapper.afterAuthToUpdateCompany(agentInfo);

		if (saveNum > 0 && updateNum > 0) {
			data.put("msg", "操作成功");
			data.put("result", "success");
		} else if (saveNum == 0 && updateNum == 0) {
			data.put("msg", "操作失败");
			data.put("result", "fail");
		} else {
			throw new RuntimeException("添加代理商资质信息:同步更新数据异常");
		}
		return data;
	}

	private void buildCerInfoImgUrl(OauthPic oauthPic, Map<String, Object> data) {
		if (StringUtils.isNotBlank(oauthPic.getImgUrl())) {
			String uploadPath = ConfigUtils.client_oauth_pic;// 客户资质图片存放文件夹
			if (!uploadPath.endsWith("/"))
				uploadPath += "/";
			String imgUrl = oauthPic.getImgUrl();
			String datePath = DateUtils.formatDate(new Date(), "yyyy/MM/dd/");
			String newImgUrl = uploadPath + datePath + imgUrl.substring(imgUrl.lastIndexOf("$$") + 2);
			oauthPic.setImgUrl(newImgUrl);
			data.put("datePath", datePath);
		}
	}

	@Override
	public Map<String, Object> updateCerInfo(OauthPic oauthPic, String company) throws Exception {
		Map<String, Object> data = new HashMap<String, Object>();

		//buildCerInfoImgUrl(oauthPic, data);

		// 添加更新时间参数(需要为两张表更新时间)
		Date update_time = Calendar.getInstance().getTime();

		// 更新代理商资质信息
		oauthPic.setUpdateDate(update_time);

		int updateCer = userMapper.updateCerInfo(oauthPic);

		// 更新代理商的信息(company字段)
		AgentInfo agentInfo = new AgentInfo();
		agentInfo.setAgentId(oauthPic.getAgentId());
		agentInfo.setCompany(company);
		agentInfo.setUpdateTime(update_time);
		int updateAgent = agentInfoMapper.afterAuthToUpdateCompany(agentInfo);

		if (updateCer > 0 && updateAgent > 0) {
			data.put("msg", "操作成功");
			data.put("result", "success");
		} else if (updateCer == 0 && updateAgent == 0) {
			data.put("msg", "操作失败");
			data.put("result", "fail");
		} else {
			throw new RuntimeException("更新代理商资质信息:同步更新数据异常 ");
		}
		return data;
	}

	@Override
	public boolean checkPassword(User user) {
		user.setOldPassword(MD5.md5(user.getOldPassword()));
		Integer count = userMapper.checkOldPassword(user);
		return count != null && count > 0 ? true : false;
	}

	@Override
	public boolean updateSecurityInfo(User user) {
		user.setPassword(MD5.md5(user.getPassword()));
		int update = userMapper.updatePassword(user);

		// 获取邮箱模板,8为普通用户开户模板
		Map<String, Object> mail = commonMapper.querySmsMailProp("100012");

		// 发送开户邮件到邮箱
		String body = (String) mail.get("text");
		User userInfo = userMapper.get(user.getId());
		EmailUtils.sendHtmlEmail(userInfo.getEmail(), (String) mail.get("subject"), body); // 发送提示邮件
		return update > 0 ? true : false;
	}

	@Override
	public AgentInfo getAgentInfoByUserId(String adminId) {
		return agentInfoMapper.getByUserId(adminId);
	}

	@Override
	public Map<String, Object> confirmResetPwd(User user) {
		user.setPassword(MD5.md5(user.getPassword()));

		int i = userMapper.confirmResetPwd(user);
		Map<String, Object> data = new HashMap<>();
		if (i > 0) {
			data.put("result", "success");
			data.put("msg", "修改成功！");
		} else {
			data.put("result", "fail");
			data.put("msg", "修改失败！");
		}
		return data;
	}
}
