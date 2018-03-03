package com.ucpaas.sms.service.userCenter;

import com.jsmsframework.common.dto.R;
import com.jsmsframework.user.entity.JsmsUserFinance;

/**
 * Created by xiongfenglin on 2017/12/1.
 *
 * @author: xiongfenglin
 */
public interface UserCenterService {
    R oemAgentRegiste(JsmsUserFinance jsmsUserFinance,boolean isCheckInviteCode) throws RuntimeException;
}
