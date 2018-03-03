package com.ucpaas.sms.api;

import com.alibaba.fastjson.JSONObject;
import com.jsmsframework.common.dto.R;
import com.jsmsframework.user.entity.JsmsUserFinance;
import com.ucpaas.sms.common.annotation.IgnoreAuth;
import com.ucpaas.sms.common.util.StringUtils;
import com.ucpaas.sms.service.userCenter.UserCenterService;
import com.ucpaas.sms.service.util.ConfigUtils;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * Created by xiongfenglin on 2017/11/25.
 *
 * @author: xiongfenglin
 */
@Controller
@RequestMapping(value="/api/userCenter")
public class ApiUserCenterController {
    @Autowired
    private UserCenterService userCenterService;
    @RequestMapping(path="/agentRegister",method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "/agentRegister", notes = "用户注册")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "email", value = "邮件", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "mobile", value = "手机", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "realName", value = "个人/公司名称", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "inviteCode", value = "邀请码", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "province", value = "省", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "city", value = "市", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "area", value = "区", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "confirmPassword", value = "确认密码", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "remark", value = "备注", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "address", value = "详细地址", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "password", value = "密码", dataType = "String", paramType = "query")})
    @IgnoreAuth
    public R agentRegister(@RequestBody String param) throws UnsupportedEncodingException {
        R r = null;
        JSONObject jo=new JSONObject();
        boolean isCheckInviteCode = false;
        //如果页面传的是json字符串，用下列方式解析
        Map<String, String> params=(Map<String, String> )jo.parse(param); //string转map
        JsmsUserFinance jsmsUserFinance = new JsmsUserFinance();
        jsmsUserFinance.setEmail(params.get("email"));
        jsmsUserFinance.setMobile(params.get("mobile"));
        jsmsUserFinance.setCompany(params.get("realName"));
        jsmsUserFinance.setInviteCode(params.get("inviteCode"));
        jsmsUserFinance.setCity(params.get("city"));
        jsmsUserFinance.setProvince(params.get("province"));
        jsmsUserFinance.setArea(params.get("area"));
        jsmsUserFinance.setConfirmPassword(params.get("confirmPassword"));
        jsmsUserFinance.setRemark(params.get("remark"));
        jsmsUserFinance.setAddress(params.get("address"));
        jsmsUserFinance.setPassword(params.get("password"));
       // jsmsUserFinance.setFlag(params.get("flag"));
        jsmsUserFinance.setDomainName(ConfigUtils.oem_domain_name);
        jsmsUserFinance.setNavTextColor(ConfigUtils.nav_text_color);
        jsmsUserFinance.setCopyright( new String(ConfigUtils.default_copyright.getBytes("ISO-8859-1"),"UTF-8"));
        jsmsUserFinance.setOemAgentDomainName(ConfigUtils.oem_agent_domain_name);
        jsmsUserFinance.setNavBackcolor(ConfigUtils.nav_backcolor);
        if(StringUtils.isBlank(params.get("flag"))){
            isCheckInviteCode = true;//为true时要校验邀请码   为false时不校验（页面点击继续提交后不需要校验）
        }
        try {
            r = userCenterService.oemAgentRegiste(jsmsUserFinance,isCheckInviteCode);
        } catch (RuntimeException e) {
            e.printStackTrace();
            r = R.error(e.getMessage());
        }catch (Exception e) {
            e.printStackTrace();
            r = R.error("请求超时，请稍后再试...");
        }
        return r;
    }
}
