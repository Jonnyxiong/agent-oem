package com.ucpaas.sms.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Created by dylan on 2017/8/20.
 */
public class PurchaseOrderVO implements Serializable {

    /**
     * 产品id,唯一标识	规则:0-999’999’999
     */
    private Integer productId;
    /**
     * 购买数量
     */
    private BigDecimal purchaseNum;

    public Integer getProductId() {
        if(productId == null){
            productId = 0;
        }
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public BigDecimal getPurchaseNum() {
        if (purchaseNum == null){
            purchaseNum = BigDecimal.ZERO;
        }
        return purchaseNum;
    }

    public void setPurchaseNum(BigDecimal purchaseNum) {
        this.purchaseNum = purchaseNum;
    }

}
