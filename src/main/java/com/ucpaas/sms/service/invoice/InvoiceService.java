package com.ucpaas.sms.service.invoice;

import com.jsmsframework.finance.entity.JsmsAgentInvoiceList;
import com.jsmsframework.common.dto.R;

/**
 * Created by xiongfenglin on 2018/1/24.
 *
 * @author: xiongfenglin
 */
public interface InvoiceService {
    R inintInvoice(JsmsAgentInvoiceList jsmsAgentInvoiceList);
}
