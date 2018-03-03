package com.ucpaas.sms.service.customer;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ucpaas.sms.dao.MessageMasterDao;
import com.ucpaas.sms.mapper.message.AccountMapper;
import com.ucpaas.sms.model.OauthPic;
import com.ucpaas.sms.util.SecurityUtils;
import com.ucpaas.sms.util.StringUtils;

@Service
@Transactional
public class AccountManageServiceImpl implements AccountManageService {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(AccountManageServiceImpl.class);

	@Autowired
	private MessageMasterDao masterDao;

	@Autowired
	private AccountMapper accountMapper;

	@Override
	public boolean canOpenTestCount(String agentId) {
		Integer count = accountMapper.getTestCount(agentId);
		return count == null || count <= 0;
	}

	@Override
	public OauthPic getClientCerInfo(String clientId) {
		OauthPic oauthPic = accountMapper.getCerInfo(clientId);

		// 加密图片路径
		String imgUrl = oauthPic.getImgUrl();
		if (StringUtils.isNotBlank(imgUrl)) {
			oauthPic.setImgUrl(SecurityUtils.encodeDes3(imgUrl));
		}

		// 3. 判断用户资质认证状态 (若认证未通过则需要查询未通过的原因)
		if ("4".equals(oauthPic.getOauthStatus())) {// 认证状态，2：待认证
													// ，3：证件已认证(正常)，4：认证不通过
			String notPassRemark = accountMapper.getNotPassRemark(clientId);
			oauthPic.setReason(notPassRemark);
		}

		return oauthPic;
	}

	@Override
	public Map<String, Object> addClientCerInfo(OauthPic oauthPic) {
		Map<String, Object> data = new HashMap<String, Object>();

		oauthPic.setUpdateDate(new Date());// 添加资质更新时间
		oauthPic.setOauthType("2");// 认证类型： 1、代理商资质认证 2、客户资质认证
		int addCer = accountMapper.addCerInfo(oauthPic); // 添加用户信息
		int updateAcc = accountMapper.updateAccWithCer(oauthPic);// 更新客户状态
		if (addCer > 0 && updateAcc > 0) {
			data.put("result", "success");
			data.put("msg", "添加成功");
			logger.debug("添加客户资质: 成功");
		} else if (addCer == 0 && updateAcc == 0) {
			data.put("result", "fail");
			data.put("msg", "添加失败");
			logger.debug("添加客户资质: 失败");
		} else {
			throw new RuntimeException("添加客户资质信息:同步更新数据异常");
		}
		return data;
	}

	@Override
	public Map<String, Object> modClientCerInfo(OauthPic oauthPic) {
		Map<String, Object> data = new HashMap<String, Object>();
		oauthPic.setUpdateDate(new Date());// 添加资质更新时间

		int updateCer = accountMapper.updateCerInfo(oauthPic); // 添加用户信息
		int updateAcc = accountMapper.updateAccWithCer(oauthPic);// 更新客户状态
		if (updateCer > 0 && updateAcc > 0) {
			data.put("result", "success");
			data.put("msg", "更新成功");
			logger.debug("更新客户资质: 成功! 修改账户信息 -->{}条, 修改图片信息 -->{}条", updateAcc, updateCer);
		} else if (updateCer == 0 && updateAcc == 0) {
			data.put("result", "fail");
			data.put("msg", "更新失败");
			logger.debug("更新客户资质: 失败");
		} else {
			throw new RuntimeException("更改客户资质信息(重新提交):同步更新数据异常");
		}
		return data;
	}
}
