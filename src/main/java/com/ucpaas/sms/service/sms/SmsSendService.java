package com.ucpaas.sms.service.sms;

import com.jsmsframework.common.dto.JsmsPage;
import com.jsmsframework.common.dto.ResultVO;
import com.jsmsframework.common.enums.WebId;
import com.jsmsframework.sms.send.dto.JsmsAccessSmsDTO;
import com.jsmsframework.sms.send.dto.JsmsAccessTimerSmsDTO;
import com.jsmsframework.sms.send.po.JsmsAccessSms;
import com.jsmsframework.sms.send.po.JsmsAccessTimerSms;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

public interface SmsSendService {


	ResultVO importMobile(CommonsMultipartFile excel);

	ResultVO oemSmsSend(JsmsAccessSms jsmsAccessSms,Integer agentId);

    JsmsPage queryMoList(JsmsPage jsmsPage);

    JsmsPage querySubmitProgressList(JsmsPage jsmsPage);

    JsmsPage querySendRecordList(JsmsPage jsmsPage);

    JsmsPage smsTimerSendQuery(JsmsPage jsmsPage, WebId webId,String agengId);

    /**
     * 推荐使用 oemSmsTimSend4BigFile
     */
    @Deprecated
    ResultVO oemSmsTimSend(JsmsAccessTimerSms jsmsAccessTimerSms, String taskId,Integer agentId,Integer chargeNumTotal,String submitFlag);

    ResultVO oemSmsTimSend4BigFile(JsmsAccessTimerSmsDTO jsmsAccessTimerSmsDTO, Integer agentId);

    ResultVO oemSmsSend(JsmsAccessSmsDTO jsmsAccessSmsDTO, Integer agentId);
}

