package com.ucpaas.sms.service.common;

import com.jsmsframework.common.dto.ResultVO;
import com.jsmsframework.common.enums.SmsTypeEnum;
import com.jsmsframework.sms.send.service.JsmsSendService;
import com.ucpaas.sms.service.util.ConfigUtils;
import com.ucpaas.sms.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 发送验证码接口实现
 *
 * @outhor tanjiangqiang
 * @create 2017-11-29 11:10
 */
@Service
public class SendVerifyCodeServiceImpl implements SendVerifyCodeService {


    @Autowired
    private JsmsSendService jsmsSendService;

    @Override
    @Transactional
    public ResultVO smsCode(String mobile, String template, List<String> params) {
        String clientid = ConfigUtils.smsp_access_clientid;
        String url = ConfigUtils.smsp_access_url_json.replaceFirst("\\{[^\\}]*\\}", clientid);
        return jsmsSendService.sendTemplateSms(clientid, SecurityUtils.encryptMD5(ConfigUtils.smsp_access_password),mobile, SmsTypeEnum.验证码, template, params,url);
    }
}