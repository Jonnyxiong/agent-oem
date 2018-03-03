package com.ucpaas.sms.api;

import com.jsmsframework.common.dto.JsmsPage;
import com.jsmsframework.common.dto.ResultVO;
import com.jsmsframework.common.enums.NeedauditEnum;
import com.jsmsframework.common.enums.NeedmoEnum;
import com.jsmsframework.common.enums.OauthStatusEnum;
import com.jsmsframework.common.util.CheckEmail;
import com.jsmsframework.common.util.JsonUtil;
import com.jsmsframework.common.util.RegexUtils;
import com.jsmsframework.user.entity.JsmsAccount;
import com.jsmsframework.user.entity.JsmsAgentInfo;
import com.jsmsframework.user.entity.JsmsClientInfoExt;
import com.jsmsframework.user.service.JsmsAccountService;
import com.jsmsframework.user.service.JsmsAgentInfoService;
import com.jsmsframework.user.service.JsmsClientInfoExtService;
import com.ucpaas.sms.common.entity.R;
import com.ucpaas.sms.constant.SysConstant;
import com.ucpaas.sms.dto.SmsCodeVO;
import com.ucpaas.sms.model.AgentInfo;
import com.ucpaas.sms.service.common.SendVerifyCodeService;
import com.ucpaas.sms.service.util.AgentUtils;
import com.ucpaas.sms.util.StringUtils;
import com.ucpaas.sms.util.web.AuthorityUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 账户管理api
 *
 * @outhor tanjiangqiang
 * @create 2017-11-29 11:02
 */
@RestController
@RequestMapping("api/account")
@Api(value = "/account", description = "账户管理api")
public class ApiAccountController {

    /**
     * 日志对象
     */
    protected Logger logger = LoggerFactory.getLogger(ApiAccountController.class);


    @Autowired
    private SendVerifyCodeService sendVerifyCodeService;

    @Autowired
    private JsmsAgentInfoService jsmsAgentInfoService;

    @Autowired
    private JsmsAccountService jsmsAccountService;

    @Autowired
    private JsmsClientInfoExtService jsmsClientInfoExtService;

    /**
     * @param smsCode 接收参数实体
     * @param session
     * @Description: 获取子客户密码
     * @Author: tanjiangqiang
     * @Date: 2017/11/29 - 10:38
     */
    @PostMapping("/getPwd")
    @ApiOperation(value = "接口获取", notes = "获取子客户密码", response = R.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clientId", value = "子客户Id", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "verifyCode", value = "验证码", dataType = "String", paramType = "query")
    })
    public R getPassword(@RequestBody SmsCodeVO smsCode, HttpSession session) {
        JsmsAccount jsmsAccount = null;
        try {
            if (StringUtils.isBlank(smsCode.getClientId())) {
                return R.error("请选择客户id");
            }
            if (StringUtils.isBlank(smsCode.getVerifyCode())) {
                return R.error("请输入验证码");
            }
            if (!smsCode.getVerifyCode().equals(session.getAttribute(SysConstant.VERIFY_CODE))) {
                return R.error("输入验证码错误");
            }
            jsmsAccount = jsmsAccountService.getByClientId(smsCode.getClientId());
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("获取密码失败"+e.getMessage());
            return R.error("获取密码失败，服务器正在检修...");
        }
        //commons.codec base64 加密
        return R.ok("获取密码成功", Base64.encodeBase64String(jsmsAccount.getPassword().getBytes()));
    }


    /**
     * @param jsmsAccount
     * @Description: 子账户更改信息
     * @Author: wujianghui
     */
    @PostMapping("/updateClient")
    @ApiOperation(value = "更改子账户信息", notes = "用户中心-自助开户", response = R.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "name", value = "子账户名称", dataType = "String", paramType = "update"),
            @ApiImplicitParam(name = "clientid", value = "子账户id", dataType = "String", paramType = "update"),
            @ApiImplicitParam(name = "email", value = "提醒邮箱", dataType = "String", paramType = "update"),
            @ApiImplicitParam(name = "address", value = "地址", dataType = "String", paramType = "update"),
            @ApiImplicitParam(name = "mobile", value = "提醒手机", dataType = "String", paramType = "update"),
            @ApiImplicitParam(name = "realname", value = "姓名", dataType = "String", paramType = "update"),
            @ApiImplicitParam(name = "needmo", value = "是否需要上行，0：不需要，1：需要(系统推送)", dataType = "String", paramType = "update"),
            @ApiImplicitParam(name = "needreport", value = "状态报告，0：不需要，1：系统推送(简单状态报告)", dataType = "String", paramType = "update"),
            @ApiImplicitParam(name = "deliveryurl", value = "状态报告回调地址", dataType = "String", paramType = "update"),
            @ApiImplicitParam(name = "mourl", value = "上行回调地址", dataType = "String", paramType = "update")
    })
    public R updateClient(HttpServletRequest request, @RequestBody JsmsAccount jsmsAccount) {
        Long adminId = AuthorityUtils.getLoginUserId(request);
        AgentInfo agentInfo = AgentUtils.queryAgentInfoByAdminId(adminId.toString());
        if (!OauthStatusEnum.证件已认证.getValue().toString().equals(agentInfo.getOauthStatus().toString())) {
            logger.error("代理商还未认证，方法结束======================================");
            return R.error("代理商还未认证");
        }

        JsmsAccount account=jsmsAccountService.getByClientId(jsmsAccount.getClientid());
        if(account==null){
            logger.error("子账户为空，方法结束======================================");
            return R.error("无子账户信息");
        }

        if (StringUtils.isEmpty(jsmsAccount.getName())) {
            logger.error("子账户为空，方法结束======================================");
            return R.error("请填写子账户名称");
        }


        if (NeedauditEnum.NEEDREPORT_SIMPLE.getValue().equals(jsmsAccount.getNeedreport())) {
            if (StringUtils.isEmpty(jsmsAccount.getDeliveryurl())){
                logger.error("状态报告地址为空，方法结束======================================");
                return R.error("请填写状态报告地址");
            }
            account.setDeliveryurl(jsmsAccount.getDeliveryurl());
            account.setNeedreport(NeedauditEnum.NEEDREPORT_SIMPLE.getValue());
        }else{
            account.setDeliveryurl(null);
            account.setNeedreport(jsmsAccount.getNeedreport());
        }

        if (NeedmoEnum.NEEDMO_WANT.getValue().equals(jsmsAccount.getNeedmo())) {
            if (StringUtils.isEmpty(jsmsAccount.getMourl())){
                logger.error("上行回调地址为空，方法结束======================================");
                return R.error("请填写上行回调地址");
            }
            account.setMourl(jsmsAccount.getMourl());
            account.setNeedmo(NeedmoEnum.NEEDMO_WANT.getValue());
        }else{
            account.setMourl(null);
            account.setNeedmo(jsmsAccount.getNeedmo());
        }

        JsmsPage accountPage;
        String email= jsmsAccount.getEmail();
        if (StringUtils.isNotEmpty(email)) {
            if (!CheckEmail.checkEmail(email)) {
                logger.error("邮箱格式错误" + JsonUtil.toJson(email));
                return R.error("请输入正确邮箱");
            }
            if(!email.equals(account.getEmail())){
                accountPage = new JsmsPage();
                accountPage.getParams().put("email", email);
                accountPage = this.jsmsAccountService.queryList(accountPage);
                if (!accountPage.getData().isEmpty()) {
                    logger.debug("邮箱已经被注册，方法结束======================================");
                    return R.error("邮箱已经被注册");
                }
            }
        }

        String mobile= jsmsAccount.getMobile();
        if (StringUtils.isNotEmpty(mobile)) {
            if (!RegexUtils.isMobile(mobile)) {
                logger.error("手机号码格式错误" + JsonUtil.toJson(mobile));
                return R.error("请输入正确手机号码");
            }
            if(!mobile.equals(account.getMobile())){
                accountPage = new JsmsPage();
                accountPage.getParams().put("mobile", mobile);
                accountPage = this.jsmsAccountService.queryList(accountPage);
                if (!accountPage.getData().isEmpty()) {
                    logger.debug("手机已经被注册，方法结束======================================");
                    return R.error("手机已经被注册");
                }
            }

        }

        account.setName(jsmsAccount.getName());
        account.setAddress(jsmsAccount.getAddress());
        account.setRealname(jsmsAccount.getRealname());
        account.setMobile(mobile);
        account.setEmail(email);
        account.setUpdatetime(new Date());
        int n = jsmsAccountService.update(account);
        if (n != 1) {
            logger.error("修改子账户信息更新失败,更新数据为：" + JsonUtil.toJson(jsmsAccount));
            return R.error("修改子账户信息失败，服务器正在检修...");
        }
        return R.ok("恭喜您，子账户信息更改成功!");
    }


    /**
     * @param smsCode 接收参数实体
     * @param session
     * @Description: 修改子客户接口密码(用户信息表)
     * @Author: wujianghui
     */
    @PostMapping("/updateClientPwd")
    @ApiOperation(value = "修改接口密码密码", notes = "修改子客户接口密码", response = R.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clientId", value = "子客户Id", dataType = "String", paramType = "update"),
            @ApiImplicitParam(name = "password", value = "密码", dataType = "String", paramType = "update"),
            @ApiImplicitParam(name = "verifyCode", value = "验证码", dataType = "String", paramType = "update")
    })
    public R updateClientPassword(@RequestBody SmsCodeVO smsCode, HttpSession session) {
        try {
            if (StringUtils.isBlank(smsCode.getClientId())) {
                return R.error("请选择修改的客户id");
            }
            if (StringUtils.isBlank(smsCode.getVerifyCode())) {
                return R.error("请输入验证码");
            }
            if (!smsCode.getVerifyCode().equals(session.getAttribute(SysConstant.VERIFY_CODE))) {
                return R.error("输入验证码错误");
            }

            // base解密得到密码
            String pwd = new String(Base64.decodeBase64(smsCode.getPassword()));
            // 密码长度8-12位
            if (StringUtils.isBlank(pwd)) {
                return R.error("密码格式为8-12为数字字母组合");
            }
            String regex = "^\\w{8,12}$";
            if (!pwd.matches(regex)) {
                return R.error("密码是数字和字母组成,长度为8-12位");
            }
            JsmsAccount jsmsAccount=jsmsAccountService.getByClientId(smsCode.getClientId());
            if(jsmsAccount==null){
                logger.error("修改子客户表接口密码失败,更新数据为：" + JsonUtil.toJson(jsmsAccount));
                return R.error("修改接口密码失败，服务器正在检修...");
            }
            logger.debug("用户信息修改前的数据-------" + JsonUtil.toJson(jsmsAccount));
            // 解密后，修改数据库
            jsmsAccount.setPassword(pwd);
            jsmsAccount.setUpdatetime(new Date());
            int n = jsmsAccountService.updateSelective(jsmsAccount);
            if (n != 1) {
                logger.error("修改子客户表接口密码失败,更新数据为：" + JsonUtil.toJson(jsmsAccount));
                return R.error("修改接口密码失败，服务器正在检修...");
            }
            logger.debug("修改用户表接口密码成功，修改后的数据-------" + JsonUtil.toJson(jsmsAccount));
            return R.ok("修改接口密码成功");
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("修改接口密码失败" + e.getMessage());
            return R.error("修改接口密码失败，服务器正在检修...");
        }
    }




    /**
     * @param smsCode 接收参数实体
     * @param session
     * @Description: 修改子客户密码(用户信息扩展表)
     * @Author: tanjiangqiang
     * @Date: 2017/11/29 - 10:38
     */
    @PostMapping("/updatePwd")
    @ApiOperation(value = "修改密码", notes = "修改子客户密码", response = R.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clientId", value = "子客户Id", dataType = "String", paramType = "update"),
            @ApiImplicitParam(name = "password", value = "密码", dataType = "String", paramType = "update"),
            @ApiImplicitParam(name = "verifyCode", value = "验证码", dataType = "String", paramType = "update")
    })
    public R updatePassword(@RequestBody SmsCodeVO smsCode, HttpSession session) {
        try {
            if (StringUtils.isBlank(smsCode.getClientId())) {
                return R.error("请选择修改的客户id");
            }
            if (StringUtils.isBlank(smsCode.getVerifyCode())) {
                return R.error("请输入验证码");
            }
            if (!smsCode.getVerifyCode().equals(session.getAttribute(SysConstant.VERIFY_CODE))) {
                return R.error("输入验证码错误");
            }
            // base解密得到密码
            String pwd = new String(Base64.decodeBase64(smsCode.getPassword()));
            // 密码长度8-12位
            if (StringUtils.isBlank(pwd)) {
                return R.error("请输入密码");
            }
            String regex = "^\\w{8,12}$";
            if (!pwd.matches(regex)) {
                return R.error("密码是数字和字母组成,长度为8-12位");
            }
            JsmsClientInfoExt clientInfoExt = jsmsClientInfoExtService.getByClientId(smsCode.getClientId());
            logger.debug("用户信息扩展修改前的数据-------" + JsonUtil.toJson(clientInfoExt));
            // 解密后，修改数据库
            clientInfoExt.setWebPassword(pwd);
            clientInfoExt.setUpdateDate(new Date());
            int n = jsmsClientInfoExtService.updateSelective(clientInfoExt);
            if (n != 1) {
                logger.error("修改子客户扩展表密码失败,更新数据为：" + JsonUtil.toJson(clientInfoExt));
                return R.error("修改密码失败，服务器正在检修...");
            }
            logger.debug("修改用户扩展表密码成功，修改后的数据-------" + JsonUtil.toJson(clientInfoExt));
            return R.ok("修改密码成功");
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("修改密码失败" + e.getMessage());
            return R.error("修改密码失败，服务器正在检修...");
        }
    }

    /**
     * 发送短信验证码
     */
    @RequestMapping(value = "/sendCode", method = RequestMethod.POST)
    @ApiOperation(value = "发送短信验证码", notes = "发送短信验证码(获取密码和修改)", response = ResultVO.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "mobile", value = "手机号码", dataType = "String", paramType = "update"),
            @ApiImplicitParam(name = "clientId", value = "客户id", dataType = "String", paramType = "update"),
            @ApiImplicitParam(name = "sendType", value = "发送类型(0为修改短信验证码,其他默认为获取密码验证码)", dataType = "String", paramType = "update")
    })
    public ResultVO smsCode(@RequestBody SmsCodeVO smsCode, HttpSession session) {
        ResultVO resultVO = null;
        String verifyCode = null;
        try {
            if (StringUtils.isBlank(smsCode.getMobile())) {
                logger.error("手机号码为空");
                return ResultVO.failure("请填写手机号码");
            }
            String template;
            List<String> params = new ArrayList<>();
            verifyCode = RandomStringUtils.randomNumeric(4); // 验证码

            JsmsAccount jsmsAccount = jsmsAccountService.getByClientId(smsCode.getClientId());
            //0为修改短信验证码,其他默认为获取密码验证码
            if ("0".equals(smsCode.getSendType())) {
                template = SysConstant.SMS_UPDATE_TEMPLATE;
            } else {
                template = SysConstant.SMS_CODE_TEMPLATE;
            }
            StringBuilder clientIdAndName = new StringBuilder("-");
            clientIdAndName.insert(0, jsmsAccount.getClientid()).append(jsmsAccount.getName());
            params.add(clientIdAndName.toString());
            params.add(verifyCode);

            resultVO = sendVerifyCodeService.smsCode(smsCode.getMobile(), template, params);
            session.setAttribute(SysConstant.VERIFY_CODE, verifyCode);
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
            return ResultVO.failure("发送短信验证码失败，服务器正在检修...");
        }
        return resultVO.setMsg("发送成功");
    }

    /**
     * 获取代理商验证码接收手机
     */
    @RequestMapping(value = "/getAgentInfo", method = RequestMethod.POST)
    @ApiOperation(value = "获取代理商验证码接收手机", notes = "获取代理商信息", response = ResultVO.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "agentId", value = "代理商id", dataType = "String", paramType = "query")
    })
    public ResultVO getAgentInfo(@RequestParam String agentId) {
        try {
            if (StringUtils.isBlank(agentId)) {
                logger.error("代理商id为空"+ JsonUtil.toJson(agentId));
                return ResultVO.failure("代理商id错误");
            }
            JsmsAgentInfo jsmsAgentInfo = jsmsAgentInfoService.getByAgentId(Integer.valueOf(agentId));
            return ResultVO.successDefault(jsmsAgentInfo);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("获取代理商验证码接收手机失败-----------{}"+ e.getMessage());
            return ResultVO.failure("服务器正在检修.....");
        }
    }

}