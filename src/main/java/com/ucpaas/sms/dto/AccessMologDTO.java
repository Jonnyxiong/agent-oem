package com.ucpaas.sms.dto;

import com.jsmsframework.access.access.entity.JsmsAccessMolog;
import org.apache.commons.lang3.time.DateFormatUtils;

public class AccessMologDTO extends JsmsAccessMolog {

    private int rowNum;

    private String receivedateStr;

    public int getRowNum() {
        return rowNum;
    }

    public void setRowNum(int rowNum) {
        this.rowNum = rowNum;
    }

    public String getReceivedateStr() {
        if(getReceivedate() != null){
            receivedateStr = DateFormatUtils.format(getReceivedate(), "yyyy-MM-dd HH:mm:ss");
        }
        return receivedateStr;
    }

    public void setReceivedateStr(String receivedateStr) {
        this.receivedateStr = receivedateStr;
    }
}
