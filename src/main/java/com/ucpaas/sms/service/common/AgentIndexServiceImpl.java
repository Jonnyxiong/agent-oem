package com.ucpaas.sms.service.common;

import com.google.common.collect.Lists;
import com.jsmsframework.stats.entity.JsmsClientSuccessRateRealtime;
import com.jsmsframework.stats.service.JsmsClientSuccessRateRealtimeService;
import com.jsmsframework.user.entity.JsmsAccount;
import com.jsmsframework.user.service.JsmsAccountService;
import com.ucpaas.sms.common.entity.PageContainer;
import com.ucpaas.sms.dao.AccessMasterDao;
import com.ucpaas.sms.dto.ClientSuccessRateRealtimeDTO;
import com.ucpaas.sms.mapper.message.AccountMapper;
import com.ucpaas.sms.service.util.AgentUtils;
import com.ucpaas.sms.util.DateUtils;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.DecimalFormat;
import java.util.*;

/**
 * 代理商平台首页
 *
 */
@SuppressWarnings("SingleStatementInBlock")
@Service
@Transactional
public class AgentIndexServiceImpl implements AgentIndexService {

	@Autowired
	private AccountMapper accountMapper;
	@Autowired
	private JsmsAccountService jsmsAccountService;
	@Autowired
	private JsmsClientSuccessRateRealtimeService jsmsClientSuccessRateRealtimeService;

	@Autowired
	private AccessMasterDao accessMasterDao;

	@Override
	public PageContainer queryCustomerForBalanceLack(Map<String, String> params) {

		Integer totalCount = accountMapper.queryCustomerForBalanceLackCount(params);
		if (totalCount == null)
			totalCount = 0;

		PageContainer p = new PageContainer();
		p.setTotalCount(totalCount);

		AgentUtils.buildPageLimitParams(params, totalCount, p);

		List<Map<String, Object>> list = accountMapper.queryCustomerForBalanceLack(params);
		if (list == null) {
			list = Lists.newArrayList();
		} else {
			int count = 0;
			for (Map map : list){
				count++;
				map.put("rownum", count);
			}
		}
		p.setList(list);
		return p;
	}

	@Override
	public Integer queryAgentClientNum(Map<String, String> params) {

		Integer count = accountMapper.queryAgentClientNum(params);
		if (count == null)
			count = 0;
		return count;
	}

	@Override
	public List<Map<String, Object>> querySixMonthsAgentClientNum(Map<String, String> params) {
		//增加判断为0数据
		List<Map<String, Object>> countData= accountMapper.querySixMonthsAgentClientNum(params);
		String  nowmonth= DateUtils.getMonth();
		List<Integer> months= new ArrayList<Integer>();

		for (Map<String, Object> countDatum : countData) {
			Integer month=Integer.parseInt(countDatum.get("month").toString());
			months.add(month);
			Collections.sort(months);
		}
		Map<String ,Object> adddata = new HashedMap();
		DecimalFormat df=new DecimalFormat("00");
		for (int j=months.size()-1; j>0; j--) {
			/*if(nowmonth.equals(months.get(j).toString())){
				adddata.put("month",  nowmonth);
				adddata.put("client_num", "0");
				countData.add(adddata);
				//j++;
			}*/

				if(months.get(j)-months.get(j-1)>1){
					months.add(months.get(j)-1);
					adddata.put("month",  (df.format(months.get(j) - 1)));
					adddata.put("client_num", "0");
					countData.add(adddata);
					Collections.sort(months);
				}
		}

		Collections.sort(countData, new Comparator<Map<String, Object>>() {
			@Override
			public int compare(Map<String, Object> o1, Map<String, Object> o2) {
				return (o1.get("month").toString()).compareTo(o2.get("month").toString());
			}
		});


		return countData;
	}

	@Override
	public String queryClientBlanceNum(Map<String, String> params) {
		return accountMapper.queryCustomerForBalanceByTypeCount(params);
	}

	/**
	 * @param params
	 * @return
	 * @Title: queryCustomerForBalanceByType
	 * @Description: 查询子客户详情剩余短信量
	 * @return: List<Map<String,Object>>
	 */
	@Override
	public List<Map<String, Object>> queryCustomerForBalanceByType(Map<String, String> params) {
		return accountMapper.queryCustomerForBalanceByType(params);
	}

	@Override
	public ClientSuccessRateRealtimeDTO dataOfToday(Integer agentId, String clientId) {

		List<String> clientIdList = new ArrayList<>();;
		if (StringUtils.isNotBlank(clientId)){
			clientIdList.add(clientId);
		}else {
			Map<String,Object> params = new HashMap();
			params.put("agentId",agentId);
			List<JsmsAccount> clientList= jsmsAccountService.queryAll(params);
			for (JsmsAccount jsmsAccount : clientList) {
				clientIdList.add(jsmsAccount.getClientid());
			}
		}
		ClientSuccessRateRealtimeDTO resultData = new ClientSuccessRateRealtimeDTO(0,0,0,0,0,0);
		if(clientIdList.isEmpty()){
			return resultData;
		}

		List<JsmsClientSuccessRateRealtime> dataList = jsmsClientSuccessRateRealtimeService.getLastOneStats(clientIdList, DateTime.now().withMillisOfDay(0).toDate());
		// todo临时时间
//		List<JsmsClientSuccessRateRealtime> dataList = jsmsClientSuccessRateRealtimeService.getLastOneStats(clientIdList, DateTime.now().withMillisOfDay(0).toDate());
		for (JsmsClientSuccessRateRealtime item : dataList) {
			resultData.setSendTotal(resultData.getSendTotal() + item.getSendTotal());
			resultData.setNosend(resultData.getNosend() + item.getNosend());
			resultData.setReallySuccessTotal(resultData.getReallySuccessTotal() + item.getReallySuccessTotal());
			resultData.setFakeSuccessFail(resultData.getFakeSuccessFail() + item.getFakeSuccessFail());
			resultData.setSendFailToatl(resultData.getSendFailToatl() + item.getSendFailToatl());
			resultData.setReallyFailTotal(resultData.getReallyFailTotal() + item.getReallyFailTotal());
		}

		return resultData;
	}

	@Override
	public Map<String, Integer> queryWeekSubmitNumber(String agentId, String clientid) {
		Map<String, Object> params = new HashMap<>();
		Map<String,Integer> resultData = new LinkedHashMap<>();
		if (StringUtils.isNotBlank(agentId)) {
			params.put("agentId", agentId);
		}
		if (StringUtils.isNotBlank(clientid)) {
			params.put("clientid", clientid);
		}
		//获取7天前的时间(yyyyMMdd)
		DateTime minusDay =  DateTime.now().minusDays(7);
		Integer startDate = Integer.valueOf(minusDay.toString("yyyyMMdd"));
//		Integer startDate = 20161205;
		for (int i = 0; i < 7; i++) {
			Integer date = startDate + i;
			params.put("date" , date );
			Integer sum =  accessMasterDao.selectOne("customer.queryWeekSubmitNumber", params);
			String key = new StringBuilder(date.toString()).insert(4,"-").insert(7,"-").toString();
			if (null == sum){
				resultData.put(key, 0);
			}else {
				resultData.put(key, sum);
			}
		}
		return resultData;
	}

}
