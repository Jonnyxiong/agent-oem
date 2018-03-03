package com.ucpaas.sms.dto;

import java.io.Serializable;

/**
 * 账户列表查询参数接收实体类
 *
 * @outhor tanjiangqiang
 * @create 2017-12-08 10:50
 */
public class AccountListVO implements Serializable {
    private String start_time_day;
    private String end_time_day;
    private String customerInfo;
    private String currentPage;
    private String pageRowCount;

    public String getStart_time_day() {
        return start_time_day;
    }

    public void setStart_time_day(String start_time_day) {
        this.start_time_day = start_time_day;
    }

    public String getEnd_time_day() {
        return end_time_day;
    }

    public void setEnd_time_day(String end_time_day) {
        this.end_time_day = end_time_day;
    }

    public String getCustomerInfo() {
        return customerInfo;
    }

    public void setCustomerInfo(String customerInfo) {
        this.customerInfo = customerInfo;
    }

    public String getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(String currentPage) {
        this.currentPage = currentPage;
    }

    public String getPageRowCount() {
        return pageRowCount;
    }

    public void setPageRowCount(String pageRowCount) {
        this.pageRowCount = pageRowCount;
    }
}