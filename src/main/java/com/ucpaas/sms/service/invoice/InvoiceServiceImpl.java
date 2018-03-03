package com.ucpaas.sms.service.invoice;

import com.jsmsframework.finance.entity.JsmsAgentInvoiceList;
import com.jsmsframework.user.finance.service.JsmsUserFinanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.jsmsframework.common.dto.R;
import com.ucpaas.sms.common.util.DateUtils;
import com.ucpaas.sms.common.util.StringUtils;
import com.ucpaas.sms.model.initstatic.StaticInitVariable;
import com.jsmsframework.common.enums.WebIdNew;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * Created by xiongfenglin on 2018/1/24.
 *
 * @author: xiongfenglin
 */
@Service
public class InvoiceServiceImpl implements InvoiceService{
    private final static Logger logger = LoggerFactory.getLogger(InvoiceServiceImpl.class);
    @Autowired
    private JsmsUserFinanceService jsmsUserFinanceService;
    @Override
    @Transactional("message_master")
    public R inintInvoice(JsmsAgentInvoiceList jsmsAgentInvoiceList) {
        jsmsAgentInvoiceList.setInvoiceId(getInvoiceId());
        return jsmsUserFinanceService.applicationInvoice(jsmsAgentInvoiceList);
    }
    private String getInvoiceId(){
        Date date = new Date();
        int num = 0;
        String idPre = "I"+ DateUtils.formatDate(date, "yyyyMMdd") + "000"+ WebIdNew.OEM代理商平台.getValue();
        if (idPre.equals(StaticInitVariable.AGENT_INVOICE_ID_PRE)) {
            num = StaticInitVariable.AGENT_INVOICE_ID_NUM;
            StaticInitVariable.AGENT_INVOICE_ID_NUM = num + 1;
        } else {
            StaticInitVariable.AGENT_INVOICE_ID_PRE = idPre;
            num = 1;
            StaticInitVariable.AGENT_INVOICE_ID_NUM = num + 1;
        }

        // 拼成订单号
        String idStr = idPre + StringUtils.addZeroForNum(num, 4, "0");
        logger.debug("生成发票申请id:==========" + idStr);
        return idStr;
    }
}
