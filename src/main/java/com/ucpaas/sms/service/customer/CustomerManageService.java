package com.ucpaas.sms.service.customer;

import java.util.List;
import java.util.Map;

import com.jsmsframework.order.dto.OemClientRechargeRollBackDTO;
import com.jsmsframework.user.entity.JsmsAccount;
import com.jsmsframework.user.entity.JsmsOauthPic;
import com.ucpaas.sms.common.entity.PageContainer;
import com.ucpaas.sms.common.entity.R;
import com.ucpaas.sms.dto.ClientBalanceAlarmPo;
import com.ucpaas.sms.model.RechargeInfo;

public interface CustomerManageService {

	/**
	 * 根据admin_id查询对应的agentId
	 * 
	 * @param admin_id
	 * @return
	 */
	Map<String, Object> getAgentId(Long admin_id);

	int updateClientOauthDate(String clientId);

	/**
	 * @Title: saveAcc
	 * @Description: 用户开户 -> 添加用户
	 * @param params
	 * @return
	 * @return: Map<String,Object>
	 */
	@Deprecated
	Map<String, Object> saveAcc(Map<String, String> params);

	/**
	 * @param jsmsAccount 用户信息
	 * @param oauthPic    子客户认证信息(代理商自有用户可空)
	 * @param adminId     操作者
	 * @Description: 子客户开户
	 * @Author: tanjiangqiang
	 * @Date: 2017/11/25 - 10:05
	 */
	JsmsAccount addUser(JsmsAccount jsmsAccount, JsmsOauthPic oauthPic, Long adminId);

	/**
	 * @Title: validateAcc
	 * @Description: 用户管理:新增用户(开户)数据验证
	 * @param params
	 * @return
	 * @return: boolean
	 */
	boolean validateAcc(Map<String, String> params);

	String queryClientSiteOemUrl(String agentId);

	/**
	 * @Title: getDetailInfo
	 * @Description: 获取单个客户的详细信息
	 * @param params
	 * @return
	 * @return: Map<String,Object>
	 */
	Map<String, Object> getDetailInfo(Map<String, String> params);

	/**
	 * @Title: resetPsd
	 * @Description: 用户管理:重置用户账户密码
	 * @param params
	 * @return
	 * @return: Map<String,Object>
	 */
	Map<String, Object> resetPsd(Map<String, String> params);

	/**
	 * @Title: queryCustomerForBalanceLack
	 * @Description:
	 * @param params
	 * @return
	 * @return: PageContainer
	 */
	PageContainer queryCustomerInfo(Map<String, String> params);

	/**
	 * @Title: queryCustomerConsumeReport
	 * @Description: 查询客户短信报表
	 * @param params
	 * @return
	 * @return: PageContainer
	 */
	PageContainer queryCustomerConsumeReport(Map<String, String> params);



	/**
	 * @Title: querycustomerConsumeEveryReport
	 * @Description: 查询客户每日报表
	 * @param params
	 * @return
	 * @return: PageContainer
	 */
	PageContainer querycustomerConsumeEveryReport(Map<String, String> params);

	/**
	 *@Title: queryCustomerConsumeReport4Export
	 * @Description: 查询客户短信报表下载
	 * @param params
	 * @return:List
	 */
	List<Map<String,Object>> queryCustomerConsumeReport4Export(Map<String,String> params);


	/**
	 *@Title: queryCustomerConsumeReport4Export
	 * @Description: 查询客户短信报表总数
	 * @param params
	 * @return:List
	 */
	Map queryCustomerConsumeReportTotal(Map<String, Object> params) ;

	/**
	 *@Title: queryCustomerConsumeReport4Export
	 * @Description: 查询客户每日报表下载
	 * @param params
	 * @return:List
	 */
	List<Map<String,Object>> querycustomerConsumeEveryReport4Export(Map<String,String> params);


	/**
	 *@Title: queryCustomerConsumeReport4Export
	 * @Description: 查询客户每日短信报表总数
	 * @param params
	 * @return:List
	 */
	Map querycustomerConsumeEveryReportTotal(Map<String, Object> params) ;


	/**
	 * @Title: queryCustomerInfoForAll
	 * @Description: 导出客户列表
	 * @param params
	 * @return
	 * @return: List<Map<String,Object>>
	 */
	List<Map<String, Object>> queryCustomerInfoForAll(Map<String, String> params);

	/**
	 * @Title: editStatus
	 * @Description: 修改订单的状态
	 * @param params
	 * @return
	 * @return: Map<String,Object>
	 */
	Map<String, Object> editStatus(Map<String, String> params);

	/**
	 * @Title: editCustomerRemark
	 * @Description: 修改客户的备注
	 * @param params
	 * @return
	 * @return: Map<String,Object>
	 */
	Map<String, Object> editCustomerRemark(Map<String, String> params);

	/**
	 * @Title: queryCommonSmsInfo
	 * @Description: 查询代理商普通短信种类信息
	 * @param params
	 * @return
	 * @return: List<Map<String,Object>>
	 */
	List<Map<String, Object>> queryCommonSmsInfo(Map<String, Object> params);

	/**
	 * @Title: queryCommonSmsInfoForClient
	 * @Description: 查询客户普通短信种类信息
	 * @param params
	 * @return
	 * @return: List<Map<String,Object>>
	 */
	List<Map<String, Object>> queryCommonSmsInfoForClient(Map<String, Object> params);

	/**
	 * @Title: queryInterSmsInfo
	 * @Description: 查询代理商国际短信信息
	 * @param params
	 * @return
	 * @return: List<Map<String,Object>>
	 */
	List<Map<String, Object>> queryInterSmsInfo(Map<String, Object> params);

	/**
	 * @Title: queryInterSmsInfoForClient
	 * @Description: 查询客户国际短信信息
	 * @param params
	 * @return
	 * @return: List<Map<String,Object>>
	 */
	List<Map<String, Object>> queryInterSmsInfoForClient(Map<String, Object> params);

	/**
	 * @Title: confirmRecharge
	 * @Description: 代理商给客户充值
	 * @param params
	 * @return
	 * @return: Map<String,Object>
	 */
	@Deprecated
	Map<String, Object> confirmRecharge(Map<String, String> params);


	/**
	 * @Title: confirmRecharge
	 * @Description: 代理商给客户充值
	 * @param rechargeInfo
	 * @return
	 * @return: Map<String,Object>
	 */
	@Deprecated
	Map<String, Object> oemClientRecharge(OemClientRechargeRollBackDTO rechargeInfo);


	/**
	  * @Description: 代理商给客户充值
	  * @Author: tanjiangqiang
	  * @Date: 2017/12/7 - 16:45
	  * @param dtoList
	  *
	  */
    R oemClientRecharge(List<OemClientRechargeRollBackDTO> dtoList);

	/**
	 * @Title: confirmRollback
	 * @Description: 代理商回退
	 * @param params
	 * @return
	 * @return: Map<String,Object>
	 */
	@Deprecated
	Map<String, Object> confirmRollback(Map<String, String> params);

	/**
	 * OEM客户回退
	 * @param dto
	 * @return
	 */
	@Deprecated
	Map<String, Object> oemClientRollback(OemClientRechargeRollBackDTO dto);

	/**
	  * @Description: 客户回退
	  * @Author: tanjiangqiang
	  * @Date: 2017/12/7 - 17:19
	  * @param dtoList
	  *
	  */
	R oemClientRollback(List<OemClientRechargeRollBackDTO> dtoList);

	/**
	 * @Title: getClientTheMostNumForMinute
	 * @Description: 获取客户订单表的前缀
	 * @param orderIdPre
	 * @return
	 * @return: String
	 */
	String getClientTheMostNumForMinute(String orderIdPre);

	/**
	 * 自动认证
	 *
	 * @param params
	 * @return
	 */
	Map<String, Object> autoAuthAccount(Map<String, String> params);


	/**
	 * @Title: queryInterSmsInfo
	 * @Description: 查询代理商短信信息
	 * @param params
	 * @return
	 * @return: List<Map<String,Object>>
	 */
	List<Map<String, Object>> querySmsInfo(Map<String, Object> params);

	/**
	 * 
	 * 根据agentId查询用户id
	 * @param agentId
	 * @return
	 */
	List<JsmsAccount> queryCustomerConsumeReport(String agentId);
	

	/**
	 * 根据cliengtId查询表
	 * @param params
	 * @return
	 */
	PageContainer queryAll(Map<String, String> params);

	/**
	 * 报表下载
	 * @param params
	 * @return
	 */
	List<Map<String, Object>> querySmsRecord4Excel(Map<String, String> params);

	/**
	 *
	 * @Description:
	 * @param params
	 * @return
	 * @return:
	 */
	int getCustomerOfAlarm(Map<String, String> params);

	R saveClientBalanceAlarm(ClientBalanceAlarmPo clientBalanceAlarm);

	/**
	 * @Title: total
	 * @Description: 查询客户短信报表合计
	 * @param params
	 * @return
	 * @return: PageContainer
	 */
	List<Map<String, Object>> total(Map<String, String> params);
}
