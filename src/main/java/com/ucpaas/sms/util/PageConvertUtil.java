package com.ucpaas.sms.util;

import com.jsmsframework.common.dto.JsmsPage;
import com.ucpaas.sms.common.entity.PageContainer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Created by dylan on 2017/8/16.
 */
public class PageConvertUtil {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(PageConvertUtil.class);

    public static JsmsPage paramToPage(Map<String,String> pageParams){
        JsmsPage page = new JsmsPage();
        try {
            if(!StringUtils.isEmpty(pageParams.get("pageRowCount"))){
                page.setRows(Integer.parseInt(pageParams.get("pageRowCount")));
            }else{
                page.setRows( 30);
            }
            if(!StringUtils.isEmpty(pageParams.get("currentPage"))){
                page.setPage(Integer.parseInt(pageParams.get("currentPage")));
            }else{
                page.setPage(1);
            }
            if(!StringUtils.isEmpty(pageParams.get("orderByClause"))){
                page.setOrderByClause(pageParams.get("orderByClause"));
            }

        } catch (ClassCastException e) {
            LOGGER.debug("pageParams 转 Page , 参数异常 ---> {}",e);
        }
        page.setParams(pageParams);
        return page;
    }

    public static PageContainer pageToContainer(JsmsPage page){
        PageContainer pageContainer = new PageContainer();
        pageContainer.setCurrentPage(page.getPage());
        pageContainer.setTotalPage(page.getTotalPage());
        pageContainer.setTotalCount(page.getTotalRecord());
        pageContainer.setPageRowCount(page.getRows());
        List entitylist = pageContainer.getEntityList();
        entitylist.addAll(page.getData());

        return pageContainer;
    }

}
