package com.ucpaas.sms.service.content;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ucpaas.sms.common.entity.R;
import com.ucpaas.sms.dao.MessageMasterDao;
import com.ucpaas.sms.model.KeywordMatchTuple;
import com.ucpaas.sms.util.KeyWrodSearchUtils;

@Service
@Transactional
public class ContentTestServiceImpl implements ContentTestService {
	@Autowired
	private MessageMasterDao messageMasterDao;

	@Override
	public boolean contentQuery(Map<String, String> params) {
		String clientid = params.get("clientid");
		List<String> auditKeywordsList = messageMasterDao.selectList("content.getAuditKeywordsByClientId", clientid);
		List<String> KeyWordList = messageMasterDao.selectList("content.getKeyWordlist", params);

		Map<String, Object> worldMapkeyWordList = KeyWrodSearchUtils.builWorldMap(KeyWordList);
		Map<String, Object> worldMap = KeyWrodSearchUtils.builWorldMap(auditKeywordsList);
		// 转移HTML防止注入，JSP页面输出是不能再用c:cout 因为下面需要显示标红的关键字
		String content = StringEscapeUtils.escapeHtml4(params.get("content").toString());

		// 匹配当前内容中的审核关键字
		List<KeywordMatchTuple<String, Integer, Integer>> keywordMatchTuples = KeyWrodSearchUtils
				.searchKeywordPosByWorldMap(content, worldMap);
		List<KeywordMatchTuple<String, Integer, Integer>> keywordMatchTuple = KeyWrodSearchUtils
				.searchKeywordPosByWorldMap(content, worldMapkeyWordList);

		// 通道关键字匹配
//		List<Integer> cids = messageMasterDao.selectList("content.getChannelId", clientid);
//		int i = 0;
//		int n = 0;
//		if (cids.size() == 0) {
//			i = 1;
//		}
//		int j = cids.size();
//		for (Integer cid : cids) {
//			String c = cid + "";
//			params.put("cid", c);
//			List<String> KeyWordChannelList = messageMasterDao.selectList("content.getKeyWordChannelList", params);
//			String s = KeyWordChannelList.get(0);
//			String[] split = s.split("\\|");
//			
//			for (String string : split) {
//				List<String> list = new ArrayList<>();
//				list.add(string);
//				// KeyWordChannelList.add(string);
//				Map<String, Object> keyWordChannelList = KeyWrodSearchUtils.builWorldMap(list);
//				List<KeywordMatchTuple<String, Integer, Integer>> keywordMatchTupless = KeyWrodSearchUtils
//						.searchKeywordPosByWorldMap(content, keyWordChannelList);
//				if(keywordMatchTupless.size()>0){
//					n++;
//					continue;
//				}
//			}
//
//		}
//		if (keywordMatchTuple.size() == 0) {
//			if (keywordMatchTuples.size() == 0) {
//				if (i > 0||n!=j) {
//					return true;
//				}
//			}
//		}
		// 查询用户绑定的通道组
		Map<String, Object> userChannelGroup = messageMasterDao.getOneInfo("content.queryUserChannelGroup", clientid);
		Boolean  b = false;
		int n =0 ;
		if(userChannelGroup != null){
			List<String> channelGroupStrList = new ArrayList<>();
			// 以下是通道组并不是通道
			String channelid = Objects.toString(userChannelGroup.get("channelid"), "");
			String ydchannelid = Objects.toString(userChannelGroup.get("ydchannelid"), "");
			String ltchannelid = Objects.toString(userChannelGroup.get("ltchannelid"), "");
			String dxchannelid = Objects.toString(userChannelGroup.get("dxchannelid"), "");
			String gjchannelid = Objects.toString(userChannelGroup.get("gjchannelid"), "");
			
			if(StringUtils.isNotBlank(channelid)){
				channelGroupStrList.add(channelid);
			}
			if(StringUtils.isNotBlank(ydchannelid)){
				channelGroupStrList.add(ydchannelid);
			}
			if(StringUtils.isNotBlank(ltchannelid)){
				channelGroupStrList.add(ltchannelid);
			}
			if(StringUtils.isNotBlank(dxchannelid)){
				channelGroupStrList.add(dxchannelid);
			}
			if(StringUtils.isNotBlank(gjchannelid)){
				channelGroupStrList.add(gjchannelid);
			}
			
			
			String channelGroupStr = StringUtils.join(channelGroupStrList, ",");
			List<String> channelGroupList = Arrays.asList(channelGroupStr.split(","));
			
			List<String> channelIdList = new ArrayList<>();
			for (String channelGroupId : channelGroupList) {
				List<String> list = messageMasterDao.selectList("content.queryChannelIdByGroupId", channelGroupId);
				channelIdList.addAll(list);
			}
			
			Set<String> channelIdSet = new HashSet<>();
			
			for (String channelId : channelIdList) {
					n=0;
				// 上面获得的通道组中的通道号可能会重复，这里重复直接跳过
				if(channelIdSet.contains(channelId)){
					continue;
				}else{
					channelIdSet.add(channelId);
					// 一条通道的关键字用“|”分隔， 超过4000个字会存在多条记录
					List<String> channelKeywordInDB = messageMasterDao.selectList("content.queryChannelKeywordById", channelId);
					String channelKeywordStr = StringUtils.join(channelKeywordInDB, "");
					// 将通道关键字中的“|”剔除掉然后保存到一个list中
//					channelKeywordList.addAll(Arrays.asList(channelKeywordStr.split("\\|")));
					String[] split = channelKeywordStr.split("\\|");
					
					for (String string : split) {
						List<String> list = new ArrayList<>();
						list.add(string);
						// KeyWordChannelList.add(string);
						Map<String, Object> keyWordChannelList = KeyWrodSearchUtils.builWorldMap(list);
						List<KeywordMatchTuple<String, Integer, Integer>> keywordMatchTupless = KeyWrodSearchUtils
								.searchKeywordPosByWorldMap(content, keyWordChannelList);
						if(keywordMatchTupless.size()>0){
							n++;
						}
					}
					if(n==0){
						b=true;
						break;
					}
					
				}
			}
			
		}



		if (keywordMatchTuple.size() == 0) {
			if (keywordMatchTuples.size() == 0) {
				if (b==true) {
					return true;
				}
			}
		}
		return false;
		
		}
		

}
