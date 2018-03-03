package com.ucpaas.sms.service.admin;

import com.ucpaas.sms.model.AgentInfo;
import com.ucpaas.sms.model.OauthPic;
import com.ucpaas.sms.model.User;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 * 管理员中心
 * 
 * @author xiejiaan
 */
public interface AdminService {

	/**
	 * 获取管理员资料
	 * 
	 * @return
	 */
	Map<String, Object> getAdmin(Long id);

	/**
	 * 已经存在邮箱或帐号
	 * 
	 * @param user
	 * @return
	 */
	Boolean isExistByMobileOrEmail(User user);

	/**
	 * 更新用户的基本信息
	 * 
	 * @param user
	 * @return
	 */
	int updateAdminInfo(User user);

	/**
	 * 根据登录的user id获取代理商的基本信息
	 *
	 * @param adminId
	 * @return
	 */
	public User getUserInfo(String adminId);

	/**
	 * 查询账户信息
	 *
	 * @param adminId
	 * @return
	 */
	public Map<String, Object> getAgentInfoAndPackage(String adminId);

	/**
	 * 获取代理商资质信息
	 *
	 * @param adminId
	 * @return
	 */
	public Map<String, Object> getCerInfo(String adminId);

	/**
	 * @Title: saveCerInfo
	 * @Description: 保存认证信息
	 * @param oauthPic
	 * @return
	 * @throws Exception
	 * @return: Map<String,Object>
	 */
	public Map<String, Object> saveCerInfo(OauthPic oauthPic, String company) throws Exception;

	/**
	 * @Title: updateCerInfo
	 * @Description: 修改资质认证信息
	 * @param oauthPic
	 * @return
	 * @throws Exception
	 * @return: Map<String,Object>
	 */
	public Map<String, Object> updateCerInfo(OauthPic oauthPic, String company) throws Exception;

	/**
	 * 验证用户密码是否存在
	 *
	 * @param user
	 * @return
	 */
	public boolean checkPassword(User user);

	/**
	 * 更新用户的安全信息
	 *
	 * @param user
	 * @return
	 */
	public boolean updateSecurityInfo(User user);

	/**
	 * @Title: getAgentInfoByUserId
	 * @Description: 返回账户的基本信息
	 * @param adminId
	 * @return
	 * @return: Map<String,Object>
	 */
	public AgentInfo getAgentInfoByUserId(String adminId);

	public Map<String,Object> confirmResetPwd(User user);
}
