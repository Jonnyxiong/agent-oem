package com.ucpaas.sms.dto;

import com.jsmsframework.common.enums.AreaCodeEnum;
import com.jsmsframework.common.enums.OperatorType;
import com.jsmsframework.common.enums.ProductType;
import com.jsmsframework.product.entity.JsmsOemProductInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Created by dylan on 2017/8/18.
 */
public class OemProductInfoVO extends JsmsOemProductInfo implements Serializable{
    private int rowNum;
    private String productTypeStr;
    private String areaCodeStr;
    private String operatorCodeStr;
    private String dueTimeStr;
    private String unitPriceStr;
    private String OEM_GJ_SMS_DISCOUNT;
    private BigDecimal discountPrice;
    private BigDecimal gjSmsDiscount;
    private String flag;
    /**
     * 折后价, 普通短信从discountPrice取值, 国际短信从gjSmsDiscount取值
     */
    private String discountPriceStr;

    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }

    public int getRowNum() {
        return rowNum;
    }

    public void setRowNum(int rowNum) {
        this.rowNum = rowNum;
    }

    public String getProductTypeStr() {
        if (getProductType() == null)
            productTypeStr = "-";
        else
            productTypeStr = ProductType.getDescByValue(getProductType());
        return productTypeStr;
    }

    public void setProductTypeStr(String productTypeStr) {
        this.productTypeStr = productTypeStr;
    }

    public String getAreaCodeStr() {
        if (getAreaCode() == null)
            areaCodeStr = "-";
        else
            areaCodeStr = AreaCodeEnum.getDescByValue(getAreaCode());
        return areaCodeStr;
    }

    public void setAreaCodeStr(String areaCodeStr) {
        this.areaCodeStr = areaCodeStr;
    }

    public String getOperatorCodeStr() {
        if (getOperatorCode() == null)
            operatorCodeStr = "-";
        else
            operatorCodeStr = OperatorType.getDescByValue(getOperatorCode());
        return operatorCodeStr;
    }

    public void setOperatorCodeStr(String operatorCodeStr) {
        this.operatorCodeStr = operatorCodeStr;
    }

    public String getDueTimeStr() {
        if (getDueTime() == null)
            dueTimeStr = "-";
        else
            dueTimeStr = DateFormatUtils.format(getDueTime(),"yyyy/MM/dd");
        return dueTimeStr;
    }

    public void setDueTimeStr(String dueTimeStr) {
        this.dueTimeStr = dueTimeStr;
    }

    public String getUnitPriceStr() {
        if (getProductType()!= null && getProductType().equals(2)){
            unitPriceStr = "请参考国际短信价格表";
        }else if(getUnitPrice() != null){
            unitPriceStr = getUnitPrice().setScale(4,BigDecimal.ROUND_DOWN).toString();
        }else{
            unitPriceStr = "-";
        }
        return unitPriceStr;
    }

    public void setUnitPriceStr(String unitPriceStr) {
        this.unitPriceStr = unitPriceStr;
    }

    public String getOEM_GJ_SMS_DISCOUNT() {
        if(StringUtils.isBlank(OEM_GJ_SMS_DISCOUNT)){
            OEM_GJ_SMS_DISCOUNT = "1";
        }
        return OEM_GJ_SMS_DISCOUNT;
    }

    public void setOEM_GJ_SMS_DISCOUNT(String OEM_GJ_SMS_DISCOUNT) {
        this.OEM_GJ_SMS_DISCOUNT = OEM_GJ_SMS_DISCOUNT;
    }

    public BigDecimal getDiscountPrice() {
        return discountPrice;
    }

    public void setDiscountPrice(BigDecimal discountPrice) {
        this.discountPrice = discountPrice;
    }

    public BigDecimal getGjSmsDiscount() {
        if(gjSmsDiscount == null){
            gjSmsDiscount = BigDecimal.ONE;
        }
        return gjSmsDiscount;
    }

    public void setGjSmsDiscount(BigDecimal gjSmsDiscount) {
        this.gjSmsDiscount = gjSmsDiscount;
    }

    public String getDiscountPriceStr() {

        if (ProductType.国际.getValue().equals(getProductType())){
            if(gjSmsDiscount != null){
                discountPriceStr = new StringBuilder("折扣率:")
                        .append(new BigDecimal(OEM_GJ_SMS_DISCOUNT).multiply(gjSmsDiscount).setScale(4,BigDecimal.ROUND_DOWN))
                        .toString();
            }else{
                discountPriceStr = new StringBuilder("折扣率:")
                        .append(new BigDecimal(OEM_GJ_SMS_DISCOUNT).setScale(4,BigDecimal.ROUND_DOWN))
                        .toString();
            }
        }else{
            if(discountPrice != null){
                discountPriceStr = discountPrice.setScale(4, BigDecimal.ROUND_DOWN).toString();
            }else{
                discountPriceStr = getUnitPriceStr();
            }
        }
        return discountPriceStr;
    }

    public void setDiscountPriceStr(String discountPriceStr) {
        this.discountPriceStr = discountPriceStr;
    }
}
