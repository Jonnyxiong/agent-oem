package com.ucpaas.sms.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ucpaas.sms.model.KeywordMatchTuple;

import java.util.*;

/**
 * 关键字匹配工具类（基于DFA实现）
 */

public class KeyWrodSearchUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeyWrodSearchUtils.class);

    /**
     * 构建用于实现关键字检索的Map
     * @param keywordList
     * @return
     */
    public static Map<String, Object> builWorldMap(List<String> keywordList){
        long start = System.currentTimeMillis();
        Map<String, Object> sesitiveWrodRootMap = new HashMap<>(keywordList.size());
        Map<String, Object> fatherMap;
        Map<String, Object> childMap;

        Iterator<String> iterator = keywordList.iterator();
        fatherMap = sesitiveWrodRootMap;
        while (iterator.hasNext()) {
            String keyword = iterator.next();
            for (int pos = 0; pos < keyword.length(); pos ++) {
                char kwChar = keyword.charAt(pos);
                Object obj = fatherMap.get(String.valueOf(kwChar));
                if(obj != null) {
                    fatherMap = (Map) obj;
                } else {
                    childMap = new HashMap<>();
                    fatherMap.put(String.valueOf(kwChar), childMap) ;
                    fatherMap = childMap;
                }
            }
            fatherMap.put("end", true);
            fatherMap = sesitiveWrodRootMap;
        }

        LOGGER.debug("KeyWrodSearchUtils.builWorldMap 耗时{}ms", System.currentTimeMillis() - start);
        return sesitiveWrodRootMap;
    }
    
    /**
     * 匹配keywordList中所有关键字content这个文本中的开始和结束位置
     * @param content 文本
     * @param sesitiveWrodRootMap 由KeyWrodSearchUtils.builWorldMap生成
     * @return 返回被匹配到的关键字及它在文本中的起始位置
     */
    public static List<KeywordMatchTuple<String, Integer, Integer>> searchKeywordPosByWorldMap(String content, Map<String, Object> sesitiveWrodRootMap) {
        long start = System.currentTimeMillis();

        List<KeywordMatchTuple<String, Integer, Integer>> resultList = new ArrayList<>();
        Map<String, Object> treeMap = null;
        boolean match = false; //
        StringBuilder matchSb = null;
        String key = null;
        int pEnd = 0;

        for (int pStar = 0; pStar < content.length(); pStar++) {
            treeMap = sesitiveWrodRootMap; // 回溯到根部
            pEnd = pStar;
            matchSb = new StringBuilder();
            match = false;
            while(true) {
                if(pEnd == content.length()){
                    break; //已经检查到文本的最后
                }
                key = String.valueOf(content.charAt(pEnd));
                Object obj = treeMap.get(key);
                if(obj == null){
                    break; // 本次匹配结束
                }else{
                    treeMap = (Map<String, Object>) obj;
                    matchSb.append(key);
                }
                pEnd++;
                obj = treeMap.get("end"); // 关键字最长匹配原则
                if(obj != null) {
                    match = true;
                }
            }

            // 是否匹配到关键字
            if(match) {
                resultList.add(new KeywordMatchTuple<>(matchSb.toString(), pStar, pEnd));
                pStar = pEnd;
            }
        }

        LOGGER.debug("KeyWrodSearchUtils.searchKeywordPosByWorldMap 耗时{}ms", System.currentTimeMillis() - start);
        return resultList;
    }
}
