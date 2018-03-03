package com.ucpaas.sms.model;

import com.ucpaas.sms.common.base.BaseEntity;

import java.util.List;

/**
 * Created by Don on 2017/8/18.
 */
public class RechargeInfo extends BaseEntity {

    private String client_id;

    private String product_type;
    private String updateNum;
    private String due_time;
    private String unit_price;
    private String operator_code;
    private String area_code;

    public String getClient_id() {
        return client_id;
    }

    public void setClient_id(String client_id) {
        this.client_id = client_id;
    }

    public String getProduct_type() {
        return product_type;
    }

    public void setProduct_type(String product_type) {
        this.product_type = product_type;
    }

    public String getUpdateNum() {
        return updateNum;
    }

    public void setUpdateNum(String updateNum) {
        this.updateNum = updateNum;
    }

    public String getDue_time() {
        return due_time;
    }

    public void setDue_time(String due_time) {
        this.due_time = due_time;
    }

    public String getUnit_price() {
        return unit_price;
    }

    public void setUnit_price(String unit_price) {
        this.unit_price = unit_price;
    }

    public String getOperator_code() {
        return operator_code;
    }

    public void setOperator_code(String operator_code) {
        this.operator_code = operator_code;
    }

    public String getArea_code() {
        return area_code;
    }

    public void setArea_code(String area_code) {
        this.area_code = area_code;
    }
}
