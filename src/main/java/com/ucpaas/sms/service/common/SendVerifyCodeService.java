package com.ucpaas.sms.service.common;

import com.jsmsframework.common.dto.ResultVO;

import java.util.List;

/**
 * 发送验证码接口
 *
 * @outhor tanjiangqiang
 * @create 2017-11-29 11:10
 */
public interface SendVerifyCodeService {


    /**
     * @Description: 发送验证码
     * @Author: tanjiangqiang
     * @Date: 2017/11/29 - 9:10
     * @param mobile 手机号码
     * @param template 发送模板, 模板中参数需要和List中参数数量一致
     * @param params 模板参数
     *
     */
    ResultVO smsCode(String mobile, String template, List<String> params);
}