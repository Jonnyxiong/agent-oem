package com.ucpaas.sms.mapper.message;

import org.springframework.stereotype.Repository;

import com.ucpaas.sms.model.OauthPic;
import com.ucpaas.sms.model.User;

import java.util.Map;

@Repository
public interface UserMapper {

	User get(String id);

	Integer getCountByMobileOrEmail(User user);

	Integer getCountByEmail(String email);

	int updateBaseInfo(User user);

	Integer checkOldPassword(User user);

	int updateLoginTimes(String id);

	int updatePassword(User user);

	int confirmResetPwd(User user);

	int updateCerInfo(OauthPic oauthPic);

	int saveCerInfo(OauthPic oauthPic);

	Map<String, Object> getCerInfo(String adminId);

	Map<String, Object> getCerInfoSource(String agentId);

	Map<String, Object> getAdminById(String adminId);

	Map<String, Object> getUserForLogin(User user);

	Map<String, Object> getUserJoinAgent(User user);

}