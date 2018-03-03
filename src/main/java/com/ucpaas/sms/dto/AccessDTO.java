package com.ucpaas.sms.dto;

import com.jsmsframework.access.access.entity.JsmsAccess;
import org.apache.commons.lang3.time.DateFormatUtils;

public class AccessDTO extends JsmsAccess {

    private int rowNum;

    private String dateStr;
    private String stateStr;
    private String errorcodeStr;

    public int getRowNum() {
        return rowNum;
    }

    public void setRowNum(int rowNum) {
        this.rowNum = rowNum;
    }

    public String getDateStr() {
        if (getDate() != null) {
            dateStr = DateFormatUtils.format(getDate(), "yyyy-MM-dd HH:mm:ss");
        }
        return dateStr;
    }

    public void setDateStr(String dateStr) {
        this.dateStr = dateStr;
    }

    public String getStateStr() {
        stateStr = StateDesc.getDescByValue(getState());
        return stateStr;
    }

    public void setStateStr(String stateStr) {
        this.stateStr = stateStr;
    }

    public String getErrorcodeStr() {
        return errorcodeStr;
    }

    public void setErrorcodeStr(String errorcodeStr) {
        this.errorcodeStr = errorcodeStr;
    }

    private enum StateDesc{
        state_0(0,"发送中"),
        state_1(1,"未知"),
        state_2(2,"发送中"),
        state_3(3,"发送成功"),
        state_4(4,"发送失败"),
        state_5(5,"拦截"),
        state_6(6,"发送失败"),
        state_7(7,"拦截"),
        state_8(8,"拦截"),
        state_9(9,"拦截"),
        state_10(10,"拦截");
        private Integer value;
        private String desc;

        StateDesc(Integer value, String desc) {
            this.value = value;
            this.desc = desc;
        }
        public Integer getValue() {
            return value;
        }

        public void setValue(Integer value) {
            this.value = value;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }

        public static String getDescByValue(Integer value) {
            if(value == null){ return null;}
            String result = null;
            for (StateDesc s : StateDesc.values()) {
                if (value == s.getValue()) {
                    result = s.getDesc();
                    break;
                }
            }
            return result;
        }
    }
}
