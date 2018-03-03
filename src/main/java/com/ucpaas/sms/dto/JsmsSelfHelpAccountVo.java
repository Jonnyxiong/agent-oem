package com.ucpaas.sms.dto;

import com.jsmsframework.user.entity.JsmsAccount;
import com.jsmsframework.user.entity.JsmsOauthPic;

import java.io.Serializable;

/**
 * 自助服务-自助开户页面参数实体
 *
 * @outhor tanjiangqiang
 * @create 2017-11-25 10:41
 */
public class JsmsSelfHelpAccountVo extends JsmsAccount implements Serializable {

    //证件号码
    private String idNbr;
    //认证类型 ，1：身份证(11：身份证正面,10：身份证背面)，2：护照，3：组织机构证，4：税务登记证，5：营业执照，6：三证合一(企业)，7：四证合一(企业)，8：登记证书号
    private Integer idType;
    //证件图片
    private String imgUrl;

    public String getIdNbr() {
        return idNbr;
    }

    public void setIdNbr(String idNbr) {
        this.idNbr = idNbr;
    }

    public Integer getIdType() {
        return idType;
    }

    public void setIdType(Integer idType) {
        this.idType = idType;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }
}