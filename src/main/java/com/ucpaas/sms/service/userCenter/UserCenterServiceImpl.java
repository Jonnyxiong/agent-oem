package com.ucpaas.sms.service.userCenter;

import com.jsmsframework.common.dto.R;
import com.jsmsframework.user.entity.JsmsUserFinance;
import com.jsmsframework.user.finance.service.JsmsUserFinanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by xiongfenglin on 2017/12/1.
 *
 * @author: xiongfenglin
 */
@Service
public class UserCenterServiceImpl implements UserCenterService{
    @Autowired
    private JsmsUserFinanceService jsmsUserFinanceService;
    @Override
    @Transactional("message_master")
    public R oemAgentRegiste(JsmsUserFinance jsmsUserFinance,boolean isCheckInviteCode) {
        return jsmsUserFinanceService.oemAgentRegister(jsmsUserFinance,isCheckInviteCode);
    }
}
