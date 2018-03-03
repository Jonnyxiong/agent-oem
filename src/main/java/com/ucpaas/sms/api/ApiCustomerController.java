package com.ucpaas.sms.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jsmsframework.access.access.entity.JsmsAccess;
import com.jsmsframework.access.service.JsmsAccessService;
import com.jsmsframework.common.dto.JsmsPage;
import com.jsmsframework.common.dto.ResultVO;
import com.jsmsframework.common.enums.*;
import com.jsmsframework.order.dto.OemClientRechargeRollBackDTO;
import com.jsmsframework.order.exception.JsmsOemAgentPoolException;
import com.jsmsframework.user.entity.JsmsAccount;
import com.jsmsframework.user.entity.JsmsOauthPic;
import com.jsmsframework.user.service.JsmsAccountService;
import com.ucpaas.sms.common.entity.PageContainer;
import com.ucpaas.sms.common.entity.R;
import com.ucpaas.sms.dto.AccountListVO;
import com.ucpaas.sms.dto.JsmsSelfHelpAccountVo;
import com.ucpaas.sms.model.AgentInfo;
import com.ucpaas.sms.model.Excel;
import com.ucpaas.sms.model.OauthPic;
import com.ucpaas.sms.model.User;
import com.ucpaas.sms.service.admin.AdminService;
import com.ucpaas.sms.service.common.AgentIndexService;
import com.ucpaas.sms.service.customer.AccountManageService;
import com.ucpaas.sms.service.customer.CustomerManageService;
import com.ucpaas.sms.service.finance.FinanceManageService;
import com.ucpaas.sms.service.util.AgentUtils;
import com.ucpaas.sms.service.util.ConfigUtils;
import com.ucpaas.sms.util.DateUtil;
import com.ucpaas.sms.util.DateUtils;
import com.ucpaas.sms.util.file.ExcelUtils;
import com.ucpaas.sms.util.file.FileUtils;
import com.ucpaas.sms.util.security.Des3Utils;
import com.ucpaas.sms.util.web.AuthorityUtils;
import com.ucpaas.sms.util.web.ControllerUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by lpjLiu on 2017/5/31.
 */
@RestController
@RequestMapping("/api/client")
@Api(value = "/api/client", description = "客户管理API")
public class ApiCustomerController {

    /**
     * 日志对象
     */
    protected Logger logger = LoggerFactory.getLogger(ApiCustomerController.class);

    @Autowired
    private CustomerManageService customerManageService;

    @Autowired
    private AgentIndexService agentIndexService;

    @Autowired
    private FinanceManageService financeManageService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private AccountManageService accountManageService;

    @Autowired
    private JsmsAccountService jsmsAccountService;

    @Autowired
    private JsmsAccessService jsmsAccessService;

    /**
     * 获取代理商的鉴权信息
     */
    @GetMapping("/oauth")
    public R getAgentData(HttpServletRequest request) {
        Map<String, Object> data = customerManageService.getAgentId(AuthorityUtils.getLoginUserId(request));
        return R.ok("成功", data);
    }

    /**
     * 获取客户管理的短信池（短信过期）
     *
     * @param client_id
     */
    @GetMapping("/pools")
    @ApiOperation(value = "/pools", notes = "客户管理获取客户管理对应产品类型的短信池（短信过期）", response = R.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "client_id", value = "子客户Id", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "product_type", value = "产品类型", dataType = "String", paramType = "query")})
    public R clientPools(String client_id, String product_type, HttpServletRequest request) {
        if (StringUtils.isBlank(client_id) || StringUtils.isBlank(product_type)) {
            return R.error("参数为空");
        }
        // 查询参数
        Map<String, String> params = Maps.newHashMap();
        params.put("clientid", client_id);
        params.put("product_type", product_type);
        Map<String, Object> defaultData = Maps.newHashMap();
        List<Map<String, Object>> data;
        String sum;
        data = agentIndexService.queryCustomerForBalanceByType(params);
        sum = agentIndexService.queryClientBlanceNum(params);
        defaultData.put("data", data);
        defaultData.put("sum", sum);

        //删除剩余条数或国际剩余余额小于等于1的数据
        Iterator<Map<String, Object>> dataIterator =  data.iterator();
        while (dataIterator.hasNext()){
            Map<String, Object> map = dataIterator.next();
            if (ProductType.国际.getValue().toString().equals(product_type)) {
                BigDecimal remainAmount = new BigDecimal(map.get("remain_amount").toString());
                if (remainAmount.compareTo(BigDecimal.ZERO) < 1) {
                    dataIterator.remove();
                }
            } else {
                int remainNumber = Integer.valueOf(map.get("remain_number").toString());
                if (remainNumber <= 0) {
                    dataIterator.remove();
                }
            }
        }

        return R.ok("获取子客户对应产品类型列表成功", defaultData);
    }

    /**
     * 验证手机
     */
    @GetMapping("/account/validate/mobile")
    public R validateMobile(String mobile, HttpServletRequest request) {
        if (StringUtils.isBlank(mobile)) {
            return R.error("参数为空");
        }

        Map<String, String> params = Maps.newHashMap();
        params.put("mobile", mobile);
        return R.ok("成功", customerManageService.validateAcc(params));
    }

    /**
     * 验证邮箱
     */
    @GetMapping("/account/validate/email")
    public R validateEmail(String email, HttpServletRequest request) {
        if (StringUtils.isBlank(email)) {
            return R.error("参数为空");
        }

        Map<String, String> params = Maps.newHashMap();
        params.put("email", email);
        return R.ok("成功", customerManageService.validateAcc(params));
    }

    /**
     * @param jsmsSelfHelpAccountVo
     * @Description: 子客户开户
     * @Author: tanjiangqiang
     * @Date: 2017/11/25 - 10:27
     */
    @PostMapping("/account/addUser")
    @ApiOperation(value = "自助开户", notes = "用户中心-自助开户", response = R.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "agentOwned", value = "使用对象", dataType = "String", paramType = "insert"),
            @ApiImplicitParam(name = "email", value = "提醒邮箱", dataType = "String", paramType = "insert"),
            @ApiImplicitParam(name = "mobile", value = "提醒手机", dataType = "String", paramType = "insert"),
            @ApiImplicitParam(name = "name", value = "账户名称", dataType = "String", paramType = "insert"),
            @ApiImplicitParam(name = "needmo", value = "是否需要上行，0：不需要，1：需要(系统推送)，3：用户拉取上行", dataType = "String", paramType = "insert"),
            @ApiImplicitParam(name = "needreport", value = "状态报告，0：不需要，1：系统推送(简单状态报告)", dataType = "String", paramType = "insert"),
            @ApiImplicitParam(name = "deliveryurl", value = "状态报告回调地址", dataType = "String", paramType = "insert"),
            @ApiImplicitParam(name = "remarks", value = "备注", dataType = "String", paramType = "insert"),
            @ApiImplicitParam(name = "idNbr", value = "证件号码", dataType = "String", paramType = "insert"),
            @ApiImplicitParam(name = "idType", value = "认证类型", dataType = "String", paramType = "insert"),
            @ApiImplicitParam(name = "imgUrl", value = "证件图片", dataType = "String", paramType = "insert")
    })
    public R addUser(HttpServletRequest request, @RequestBody JsmsSelfHelpAccountVo jsmsSelfHelpAccountVo) {
        Long adminId = AuthorityUtils.getLoginUserId(request);
        AgentInfo agentInfo = AgentUtils.queryAgentInfoByAdminId(adminId.toString());
        if (!OauthStatusEnum.证件已认证.getValue().toString().equals(agentInfo.getOauthStatus().toString())) {
            logger.error("代理商还未认证，方法结束======================================");
            return R.error("代理商还未认证");
        }
        if (null == jsmsSelfHelpAccountVo) {
            return R.error("请填写相关信息");
        }
        if (StringUtils.isBlank(jsmsSelfHelpAccountVo.getName())) {
            logger.error("账户名称为空，方法结束======================================");
            return R.error("请填写账户名称");
        }

        if (NeedauditEnum.NEEDREPORT_SIMPLE.getValue().equals(jsmsSelfHelpAccountVo.getNeedreport())) {
            if (StringUtils.isBlank(jsmsSelfHelpAccountVo.getDeliveryurl())){
                logger.error("状态报告地址为空，方法结束======================================");
                return R.error("请填写状态报告地址");
            }
        }
        if (NeedmoEnum.NEEDMO_WANT.getValue().equals(jsmsSelfHelpAccountVo.getNeedmo())) {
            if (StringUtils.isBlank(jsmsSelfHelpAccountVo.getMourl())){
                logger.error("上行回调地址为空，方法结束======================================");
                return R.error("请填写上行回调地址");
            }
        }

        if (AgentOwned.代理商子客户使用.getValue().equals(jsmsSelfHelpAccountVo.getAgentOwned())) {
            if (StringUtils.isBlank(jsmsSelfHelpAccountVo.getIdType().toString())) {
                logger.error("认证类型为空======================================");
                return R.error("请选择认证类型");
            }
            if (StringUtils.isBlank(jsmsSelfHelpAccountVo.getIdNbr())) {
                logger.error("证件号码为空======================================");
                return R.error("请填写证件号码");
            }
            if (StringUtils.isBlank(jsmsSelfHelpAccountVo.getImgUrl())) {
                logger.error("证件图片地址为空======================================");
                return R.error("请上传证件图片");
            }
        }

        jsmsSelfHelpAccountVo.setAgentId(Integer.valueOf(agentInfo.getAgentId()));
        JsmsAccount jsmsAccount = null;

        JsmsOauthPic jsmsOauthPic = new JsmsOauthPic();
        jsmsOauthPic.setIdNbr(jsmsSelfHelpAccountVo.getIdNbr());
        jsmsOauthPic.setIdType(jsmsSelfHelpAccountVo.getIdType());
        jsmsOauthPic.setImgUrl(jsmsSelfHelpAccountVo.getImgUrl());
        try {
            jsmsAccount = customerManageService.addUser(jsmsSelfHelpAccountVo, jsmsOauthPic, adminId);
            jsmsAccount.setPassword(Base64.encodeBase64String(jsmsAccount.getPassword().getBytes()));
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
            return R.error(e.getMessage());
        }
        return R.ok("恭喜您，开户成功!", jsmsAccount);
    }


    /**
     * 开户
     */
    @PostMapping("/account/add")
    @Deprecated
    public R addAccount(String email, String mobile, String name, String client_type, String realname, String remarks,
                        String province, String city, String area, String address, HttpServletRequest request) {
        R r;
        if (StringUtils.isBlank(email) || StringUtils.isBlank(mobile) || StringUtils.isBlank(name)
                || StringUtils.isBlank(realname) || StringUtils.isBlank(address) || StringUtils.isBlank(remarks)) {
            r = R.error("数据验证失败");
            return r;
        }

        try {
            Long adminId = AuthorityUtils.getLoginUserId(request);
            AgentInfo agentInfo = AgentUtils.queryAgentInfoByAdminId(adminId.toString());
            if (!"3".equals(agentInfo.getOauthStatus())) {
                r = R.error("代理商还未认证！");
                return r;
            }

            Map<String, String> params = ControllerUtils.buildQueryMap(request);
            params.put("admin_id", adminId.toString()); // 获取用户登录的id
            params.put("email", email);
            params.put("mobile", mobile);
            params.put("name", name);
            params.put("client_type", client_type);
            params.put("realname", realname);
            params.put("remarks", remarks);
            params.put("province", province);
            params.put("city", city);
            params.put("area", area);
            params.put("address", address);

            // OEM代理商开户自动继承代理商的销售归属
            if (StringUtils.isNotBlank(agentInfo.getBelongSale())) {
                params.put("belong_sale", agentInfo.getBelongSale());
            }

            Map data = customerManageService.saveAcc(params);

            if ("success".equals(data.get("result").toString())) {
                Map<String, Object> resultData = Maps.newHashMap();
                resultData.put("clientid", params.get("clientid"));
                resultData.put("sendEmail", params.get("sendEmail"));
                r = R.ok(data.get("msg").toString(), resultData);
            } else {
                r = R.error(data.get("msg").toString());
            }
        } catch (Exception e) {
            logger.error("开户失败, email{}, 信息{}", email, e);
            r = R.error("服务器异常,正在检修中...");
        }

        return r;
    }

    /**
     * 一键创建测试帐号
     */
    @PostMapping("/account/test/add")
    public R addTestAccount(HttpServletRequest request) {
        R r;
        Long adminId = AuthorityUtils.getLoginUserId(request);
        try {

            // 代理商信息
            AgentInfo agentInfo = AgentUtils.queryAgentInfoByAdminId(adminId.toString());
            if (!"3".equals(agentInfo.getOauthStatus())) {
                r = R.error("代理商还未认证！");
                return r;
            }

            // 管理员信息
            User userInfo = adminService.getUserInfo(adminId.toString());

            Map<String, String> params = ControllerUtils.buildQueryMap(request);
            params.put("admin_id", adminId.toString()); // 获取用户登录的id
            params.put("email", userInfo.getEmail());
            params.put("mobile", userInfo.getMobile());
            params.put("client_type", "2");

            if (StringUtils.isNotBlank(agentInfo.getAgentName())) {
                params.put("name", agentInfo.getAgentName());
                params.put("realname", agentInfo.getAgentName());
            }

            if (StringUtils.isNotBlank(agentInfo.getAddress())) {
                params.put("address", agentInfo.getAddress());
            }

            if (StringUtils.isNotBlank(agentInfo.getBelongSale())) {
                params.put("belong_sale", agentInfo.getBelongSale());
            }

            params.put("isTestAccount", "true"); // 是否测试帐号
            params.put("remarks", "一键创建");
            Map data = customerManageService.saveAcc(params);

            if ("success".equals(data.get("result").toString())) {
                // 更新认证时间
                customerManageService.updateClientOauthDate(data.get("clientid").toString());

                Map<String, Object> resultData = Maps.newHashMap();
                // resultData.put("clientid", data.get("clientid"));
                // resultData.put("mobile", userInfo.getMobile());
                resultData.put("sendEmail", data.get("sendEmail"));
                // resultData.put("password", data.get("password"));

                String url = customerManageService.queryClientSiteOemUrl(agentInfo.getAgentId());
                StringBuilder ym = new StringBuilder(url);
                StringBuilder encStrB = new StringBuilder();
                encStrB.append(data.get("clientid")).append("====").append(userInfo.getMobile()).append("====")
                        .append(data.get("password")).append("====").append(Calendar.getInstance().getTimeInMillis());
                String encStr = Des3Utils.encodeDes3(encStrB.toString());
                ym.append("/console").append("?oemAuthToken=").append(encStr);

                resultData.put("oemUrl", ym.toString());

                r = R.ok(data.get("msg").toString(), resultData);
            } else {
                r = R.error(data.get("msg").toString());
            }
        } catch (Exception e) {
            logger.error("一键创建测试帐号失败, adminId{}, 信息{}", adminId.toString(), e);
            r = R.error("服务器异常,正在检修中...");
        }

        return r;
    }

    /**
     * 自动Auth，暂不提供
     *
     * @param client_id
     * @param request
     * @return
     */
    /*
     * @PostMapping("/account/autoauth") public R accountAuth(String client_id,
	 * HttpServletRequest request) { R r; try { Map<String, String> params =
	 * ControllerUtils.buildQueryMap(request); params.put("client_id",
	 * client_id); params.put("agent_id",
	 * AuthorityUtils.getLoginAgentId(request));
	 * 
	 * Map<String, Object> data = customerManageService.autoAuthAccount(params);
	 * if ("success".equals(data.get("result").toString())) { r =
	 * R.ok(data.get("msg").toString()); } else { r =
	 * R.error(data.get("msg").toString()); }
	 * 
	 * } catch (Exception e) { logger.debug(e.getMessage()); r =
	 * R.error("服务器异常,正在检修中..."); } return r; }
	 */

    /**
     * 代理商客户列表
     */
    @PostMapping("/accounts")
    @ApiOperation(value = "自助开户账户列表", notes = "自助开户账户列表", response = R.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "customerInfo", value = "客户ID,名称,手机号", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "start_time_day", value = "开始时间", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "end_time_day", value = "结束时间", dataType = "String", paramType = "query")})
    public R accounts(@RequestBody AccountListVO accountListVO, HttpServletRequest request) {

        Map<String, String> params = ControllerUtils.buildQueryMap(accountListVO.getPageRowCount(), accountListVO.getCurrentPage(), request);
        String customerInfo = accountListVO.getCustomerInfo();
        String start_time_day = accountListVO.getStart_time_day();
        String end_time_day = accountListVO.getEnd_time_day();
        if (StringUtils.isNotBlank(customerInfo)) {
            params.put("customerInfo", customerInfo);
        }
        if (StringUtils.isNotBlank(start_time_day)) {
            params.put("start_time_day", start_time_day);
        }
        if (StringUtils.isNotBlank(end_time_day)) {
            params.put("end_time_day", end_time_day + " 23:59:59");
        }
        params.put("isRechargeRollback","0");//自助开户列表业务
        PageContainer page = customerManageService.queryCustomerInfo(params);

        return R.ok("获取客户信息成功", page);
    }

    @GetMapping("/weburl")
    public R getOemWebUrl(HttpServletRequest request) {
        return R.ok("成功", customerManageService.queryClientSiteOemUrl(AuthorityUtils.getLoginAgentId(request)));
    }

    @GetMapping("/citys")
    public R getCity(HttpServletRequest request) {
        FileInputStream inputStream = null;
        Map<String, Object> obj = null;
        try {
            inputStream = new FileInputStream(new File(FileUtils.getClassPath() + "/package.json"));
            obj = JSON.parseObject(inputStream, Map.class);

            return R.ok("成功", obj);
        } catch (IOException e) {
            logger.error("获取城市级联信息失败{}", e);
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e1) {
                    return R.error("失败");
                }
            }
            return R.error("失败");
        }
    }

    /**
     * 代理商客户汇总信息
     */
    @GetMapping("/stat")
    public R totalData(HttpServletRequest request) {

        AgentInfo agentInfo = AgentUtils.queryAgentInfoByAdminId(AuthorityUtils.getLoginUserId(request).toString());

        Map<String, String> params = Maps.newHashMap();
        params.put("agent_id", agentInfo.getAgentId());

        Map<String, Object> resultData = Maps.newHashMap();

        // 获取代理商的总客户数和当月客户数
        resultData.put("total_client_num", agentIndexService.queryAgentClientNum(params));

        DateTime dateTime = new DateTime();
        String thisMonth = dateTime.toString("yyyy-MM");
        params.put("thisMonth", thisMonth);
        resultData.put("thisMonth_client_num", agentIndexService.queryAgentClientNum(params));
        resultData.put("oauth_status", agentInfo.getOauthStatus());

        return R.ok("获取汇总信息成功", resultData);
    }

    /**
     * 自助开户---获取客户列表-导出
     */
    @PostMapping("/account/export")
    @ApiOperation(value = "/account/export", notes = "获取客户列表-导出", response = R.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "customerInfo", value = "子账户ID,子账户名称,手机号", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "start_time_day", value = "开始时间", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "end_time_day", value = "结束时间", dataType = "String", paramType = "query")})
    public void accountsExport(String customerInfo, String start_time_day, String end_time_day,
                               HttpServletRequest request, HttpServletResponse response) {
        String filePath = ConfigUtils.temp_file_dir + "/账户列表" + DateUtils.getDate("yyyyMMddHHmmss") + ".xls";
        Excel excel = new Excel();
        excel.setFilePath(filePath);
        excel.setTitle("子账户列表");

        Map<String, String> params = ControllerUtils.buildQueryMap(request);
        params.put("isRechargeRollback","0");//自助开户列表业务
        StringBuffer buffer = new StringBuffer();
        buffer.append("查询条件：");
        if (StringUtils.isNotBlank(customerInfo)) {
            params.put("customerInfo", customerInfo);

            buffer.append("  子账户ID/子账户名称;");
            buffer.append(customerInfo);
            buffer.append(";");
        }

        if (StringUtils.isNotBlank(start_time_day)) {
            params.put("start_time_day", start_time_day);

            buffer.append("  开始时间：");
            buffer.append(start_time_day);
            buffer.append(";");
        }

        if (StringUtils.isNotBlank(end_time_day)) {
            params.put("end_time_day", end_time_day + " 23:59:59");

            buffer.append("  结束时间：");
            buffer.append(end_time_day);
            buffer.append(";");
        }

        excel.addRemark(buffer.toString());

        excel.addHeader(20, "子账户ID", "client_id");
        excel.addHeader(20, "子账户名称", "client_name");
        excel.addHeader(20, "账户状态", "status_str");
        excel.addHeader(20, "使用对象", "agent_owned_str");
        excel.addHeader(20, "认证状态", "oauth_status_str");
        excel.addHeader(20, "提醒邮箱", "email");
        excel.addHeader(20, "提醒手机", "mobile");
        excel.addHeader(20, "开户时间", "createtime");
		/*excel.addHeader(20, "创建时间", "create_time");
		excel.addHeader(50, "行业短信", "hy_string");
		excel.addHeader(50, "验证码短信", "co_string");
		excel.addHeader(50, "通知短信", "no_string");
		excel.addHeader(50, "营销短信", "yx_string");
		excel.addHeader(50, "国际短信", "gj_string");
		excel.addHeader(100, "备注", "remarks");*/
        List<Map<String, Object>> list = customerManageService.queryCustomerInfoForAll(params);
        if (list.size() > 0) {
            for (int i = 0; i < list.size(); i++) {
                if ((list.get(i)).get("agent_owned") != null) {
                    if (String.valueOf((list.get(i)).get("agent_owned")).equals("0")) {
                        (list.get(i)).put("agent_owned_str", "下级客户");
                    } else if (String.valueOf((list.get(i)).get("agent_owned")).equals("1")) {
                        (list.get(i)).put("agent_owned_str", "自己使用");
                    }
                }

            }
        }
        excel.setDataList(list);

        if (ExcelUtils.exportExcel(excel)) {
            FileUtils.download(filePath, response);
            FileUtils.delete(filePath);
        } else {
            ControllerUtils.renderText(response, "导出Excel文件失败，请联系管理员");
        }
    }

    /**
     * 客户详细信息
     */
    @GetMapping("/account/detail")
    public R accountDetail(String client_id, HttpServletRequest request) {
        R r;
        if (StringUtils.isBlank(client_id)) {
            r = R.error("客户ID不能为空！");
            return r;
        }

        Map<String, String> params = Maps.newHashMap();
        params.put("clientid", client_id);

        return R.ok("获取详细信息成功", customerManageService.getDetailInfo(params));
    }

    @PostMapping("/account/resetPsd")
    public R accountResetPsd(String clientId, String email, HttpServletRequest request) {
        R r;
        if (StringUtils.isBlank(clientId)) {
            r = R.error("客户ID不能为空！");
            return r;
        }

        if (StringUtils.isBlank(email)) {
            r = R.error("邮箱不能为空！");
            return r;
        }

        try {
            Map<String, String> params = Maps.newHashMap();
            params.put("clientId", clientId);
            params.put("admin_id", AuthorityUtils.getLoginUserId(request).toString());
            params.put("email", email);

            Map data = customerManageService.resetPsd(params);

            r = "success".equals(data.get("result").toString()) ? R.ok(data.get("msg").toString())
                    : R.error(data.get("msg").toString());
        } catch (Exception e) {
            logger.error("客户密码重置失败, clientId{}, 信息{}", clientId, e);
            r = R.error("服务器异常,正在检修中...");
        }

        return r;
    }

    /**
     * 充值产品列表
     */
    @PostMapping("/account/recharge/data")
    @ApiOperation(value = "/account/recharge/data", notes = "充值产品列表", response = R.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "product_type", value = "产品类型", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "operator_code", value = "运营商类型", dataType = "String", paramType = "query")})
    @Deprecated
    public R rechargeData(String product_type, String operator_code, HttpServletRequest request) {
        Map<String, Object> params = Maps.newHashMap();
        params.put("agent_id", AuthorityUtils.getLoginAgentId(request));
        params.put("product_type", product_type);
        params.put("operator_code", operator_code);
        Map<String, Object> resultData = Maps.newHashMap();
        List<Map<String, Object>> data;

        data = customerManageService.querySmsInfo(params);

        resultData.put("data", data);

        return R.ok("客户管理-充值-查询代理商短信池成功", resultData);
    }

    /**
     * 给客户充值
     */
    @PostMapping("/account/recharge/save")
    @ApiOperation(value = "/account/recharge/save", notes = "充值", response = R.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "batchdata", value = "json字符串批量数据包括list（client_id,product_type,updateNum,due_time,unit_price,operator_code,area_code）", dataType = "String", paramType = "query")})
    @Deprecated
    public R rechargeSave(String batchdata, HttpServletRequest request) {
        R r = null;

        try {
            String adminId = AuthorityUtils.getLoginUserId(request).toString();
            AgentInfo agentInfo = AgentUtils.queryAgentInfoByAdminId(adminId.toString());
            if (!"3".equals(agentInfo.getOauthStatus())) {
                r = R.error("代理商还未认证！");
                return r;
            }

            Gson gson = new GsonBuilder().setDateFormat("yyyyMMddHHmmss").create();
            Type listType = new TypeToken<List<OemClientRechargeRollBackDTO>>() {
            }.getType();
            List<OemClientRechargeRollBackDTO> lst = gson.fromJson(batchdata, listType);
            for (OemClientRechargeRollBackDTO rechargeInfo : lst) {

                rechargeInfo.setAgentId(Integer.valueOf(AuthorityUtils.getLoginAgentId(request)));
                Map data = customerManageService.oemClientRecharge(rechargeInfo);

                r = "success".equals(data.get("result").toString()) ? R.ok(data.get("msg").toString())
                        : R.error(data.get("msg").toString());

            }

        } catch (JsmsOemAgentPoolException e) {
            logger.error("客户充值失败,  信息{}", e);
            r = R.error(e.getMessage());
        } catch (Exception e) {
            logger.error("客户充值失败,  信息{}", e);
            r = R.error("服务器异常,正在检修中...");
        }

        return r;
    }

    /**
     * 回退产品列表
     */
    @GetMapping("/account/rollback/data")
    @ApiOperation(value = "/account/rollback/data", notes = "充值产品列表", response = R.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "product_type", value = "产品类型", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "operator_code", value = "运营商类型", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "client_id", value = "客户ID", dataType = "String", paramType = "query")})
    @Deprecated
    public R rollbackData(String product_type, String operator_code, String client_id, HttpServletRequest request) {
        if (StringUtils.isBlank(client_id)) {
            return R.error("参数验证失败");
        }

        Map<String, Object> params = Maps.newHashMap();
        params.put("client_id", client_id);
        params.put("product_type", product_type);
        params.put("operator_code", operator_code);
        Map<String, Object> resultData = Maps.newHashMap();

        resultData.put("data", customerManageService.queryCommonSmsInfoForClient(params));

        return R.ok("客户管理-回退-查询子客户短信池成功", resultData);
    }

    /**
     * 做回滚的处理
     */
    @PostMapping("/account/rollback/save")
    @ApiOperation(value = "/account/rollback/save", notes = "回退", response = R.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "batchback", value = "json字符串批量数据包括list（client_id,product_type,updateNum,due_time,unit_price,operator_code,area_code）", dataType = "String", paramType = "query")})
    @Deprecated
    public R rollbackSave(String batchback, HttpServletRequest request) {

        R r = null;

        List<OemClientRechargeRollBackDTO> lst = null;
        try {
            String adminId = AuthorityUtils.getLoginUserId(request).toString();
            AgentInfo agentInfo = AgentUtils.queryAgentInfoByAdminId(adminId.toString());
            if (!"3".equals(agentInfo.getOauthStatus())) {
                r = R.error("代理商还未认证！");
                return r;
            }

            Gson gson = new GsonBuilder().setDateFormat("yyyyMMddHHmmss").create();
            Type listType = new TypeToken<List<OemClientRechargeRollBackDTO>>() {
            }.getType();
            lst = gson.fromJson(batchback, listType);
            for (OemClientRechargeRollBackDTO rollBackDTO : lst) {
                rollBackDTO.setAgentId(Integer.valueOf(agentInfo.getAgentId()));
                Map data = customerManageService.oemClientRollback(rollBackDTO);

                r = "success".equals(data.get("result").toString()) ? R.ok(data.get("msg").toString())
                        : R.error(data.get("msg").toString());
            }
        } catch (Exception e) {
            logger.error("客户回退失败, clientId{}, 信息{}", lst.get(0).getClientId(), e);
            r = R.error("服务器异常,正在检修中...");
        }

        return r;
    }

    /**
     * 冻结、解冻
     */
    @PostMapping("/account/status/update")
    public R updateAccountStatus(String client_id, String status, HttpServletRequest request) {
        R r;
        try {
            String adminId = AuthorityUtils.getLoginUserId(request).toString();
            AgentInfo agentInfo = AgentUtils.queryAgentInfoByAdminId(adminId.toString());
            if (!"3".equals(agentInfo.getOauthStatus())) {
                r = R.error("代理商还未认证！");
                return r;
            }

            Map<String, String> params = Maps.newHashMap();
            params.put("clientid", client_id);
            params.put("status", status);

            Map data = customerManageService.editStatus(params);

            r = "success".equals(data.get("result").toString()) ? R.ok(data.get("msg").toString())
                    : R.error(data.get("msg").toString());
        } catch (Exception e) {
            logger.error("客户冻结或解冻失败, clientId{}, 信息{}", client_id, e);
            r = R.error("服务器异常,正在检修中...");
        }

        return r;
    }

    /**
     * 更新备注
     */
    @PostMapping("/account/remark/update")
    public R updateAccountRemark(String client_id, String remarks, HttpServletRequest request) {
        R r;
        try {
            String adminId = AuthorityUtils.getLoginUserId(request).toString();
            AgentInfo agentInfo = AgentUtils.queryAgentInfoByAdminId(adminId.toString());
            if (!"3".equals(agentInfo.getOauthStatus())) {
                r = R.error("代理商还未认证！");
                return r;
            }

            Map<String, String> params = Maps.newHashMap();
            params.put("clientid", client_id);
            params.put("remarks", remarks);

            Map data = customerManageService.editCustomerRemark(params);

            r = "success".equals(data.get("result").toString()) ? R.ok(data.get("msg").toString())
                    : R.error(data.get("msg").toString());
        } catch (Exception e) {
            logger.error("客户备注更新失败, clientId{}, 信息{}", client_id, e);
            r = R.error("服务器异常,正在检修中...");
        }

        return r;
    }

    /**
     * 代理商客户消耗汇总报表-预付费
     */
    @PostMapping("/report/consume/pretotal")
    @ApiOperation(value = "/report/consume/pretotal", notes = "代理商客户消耗汇总报表-预付费", response = R.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "customerInfo", value = "客户", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "start_time_day", value = "开始时间", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "end_time_day", value = "结束时间", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "pageRowCount", value = "行数", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "currentPage", value = "页数", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "smstype", value = "短信类型", dataType = "String", paramType = "query")})
    public R consumeReportPreTotal(@RequestBody String param,HttpServletRequest request) {
        JSONObject jo=new JSONObject();
        //如果页面传的是json字符串，用下列方式解析
        Map<String, String> param1=(Map<String, String> )jo.parse(param); //string转map
        Map<String, String> totalParam=new HashMap<>();
        Map<String, String> params = ControllerUtils.buildQueryMap(String.valueOf(param1.get("pageRowCount")), String.valueOf(param1.get("currentPage")), request);
        String start_time_day =param1.get("start_time_day");
        String end_time_day =param1.get("end_time_day");
        List<Map<String, Object>> total = null;
        if (StringUtils.isNotBlank(param1.get("customerInfo"))) {
            params.put("customerInfo", param1.get("customerInfo"));
            totalParam.put("customerInfo", param1.get("customerInfo"));
        }
        if (StringUtils.isNotBlank(param1.get("smstype"))) {
            params.put("smstype", param1.get("smstype"));
            totalParam.put("smstype", param1.get("smstype"));
        }
        params.put("agent_id", AuthorityUtils.getLoginAgentId(request));
        totalParam.put("agent_id", AuthorityUtils.getLoginAgentId(request));
        // 预付费设置
        params.put("paytype", "0");
        totalParam.put("paytype", "0");
        String every_day_flag = "no";
        DateTime dt = new DateTime();

        // 若未传值，默认查询昨天的
        if (StringUtils.isBlank(start_time_day)) {
            DateTime yesterday = dt.minusDays(1);
            start_time_day = yesterday.toString("yyyyMMdd");
            end_time_day = yesterday.toString("yyyyMMdd");
        } else {
            if (StringUtils.isBlank(end_time_day)) {
                end_time_day = dt.toString("yyyy-MM-dd");
            }
            Calendar beginCal = Calendar.getInstance();
            beginCal.setTime(DateUtils.parseDate(start_time_day));
            Calendar endCal = Calendar.getInstance();
            endCal.setTime(DateUtils.parseDate(end_time_day));
            long between_days = (endCal.getTimeInMillis() - endCal.getTimeInMillis()) / (1000 * 3600 * 24);
            if (Integer.parseInt(String.valueOf(between_days)) != 0) {
                every_day_flag = "yes";
            }
            start_time_day = DateUtils.formatDate(beginCal.getTime(), "yyyyMMdd");
            end_time_day = DateUtils.formatDate(endCal.getTime(), "yyyyMMdd");
        }

        params.put("start_time_day", start_time_day);
        totalParam.put("start_time_day", start_time_day);
        params.put("end_time_day", end_time_day);
        totalParam.put("end_time_day", end_time_day);
        logger.info("预付费查询条件为params={}", params);
        logger.info("预付费合计查询条件为totalParam={}", totalParam);
        PageContainer page = customerManageService.queryCustomerConsumeReport(params);
        total = customerManageService.total(totalParam);
        Map<String, Object> map = Maps.newHashMap();
        map.put("page", page);
        map.put("every_day_flag", every_day_flag);
        Map<String,Object> totalMap = new HashMap<>();
        if(!total.isEmpty()){
            if(total.size()==1){
                if(total.get(0)!=null){
                    totalMap.put("rownum", "合计");
                    //提交条数
                    totalMap.put("send_num", total.get(0).get("sendNum"));
                    //计费条数
                    totalMap.put("chargeTotal", total.get(0).get("charge_total"));
                    //成功条数
                    totalMap.put("success_num", total.get(0).get("successNum"));
                    //未知条数
                    totalMap.put("not_known_num", total.get(0).get("notKnownNum"));
                    //失败条数
                    totalMap.put("fail_num", total.get(0).get("failNum"));
                    //待发送条数
                    totalMap.put("wait_send_num", total.get(0).get("waitSendNum"));
                    //拦截条数
                    totalMap.put("intercept_num", total.get(0).get("interceptNum"));
                    page.getList().add(totalMap);
                }
            }else{
                return R.error("获取合计失败");
            }
        }
        return R.ok("获取预付费客户消耗汇总报表成功", page);
    }

    /**
     * 代理商客户消耗汇总报表-每天-预付费
     */
    @GetMapping("/report/consume/preday")
    @ApiOperation(value = "/report/consume/preday", notes = "代理商客户消耗汇总报表-每天-预付费", response = R.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "customerInfo", value = "客户", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "start_time_day", value = "开始时间", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "end_time_day", value = "结束时间", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "pageRowCount", value = "行数", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "currentPage", value = "页数", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "smstype", value = "短信类型", dataType = "String", paramType = "query")})
    public R consumeReportPreDay(String customerInfo, String smstype, String start_time_day, String end_time_day,
                                 String pageRowCount, String currentPage, HttpServletRequest request) {

        Map<String, String> params = ControllerUtils.buildQueryMap(pageRowCount, currentPage, request);
        if (StringUtils.isNotBlank(customerInfo)) {
            params.put("customerInfo", customerInfo);
        }
        // if (StringUtils.isNotBlank(product_type)) {
        // params.put("product_type", product_type);
        // }
        if (StringUtils.isNotBlank(smstype)) {
            params.put("smstype", smstype);
        }
        // 预付费设置
        params.put("paytype", "0");
        if (StringUtils.isNotBlank(start_time_day)) {
            params.put("start_time_day", DateUtils.formatDate(DateUtils.parseDate(start_time_day), "yyyyMMdd"));
        }
        if (StringUtils.isNotBlank(end_time_day)) {
            params.put("end_time_day", DateUtils.formatDate(DateUtils.parseDate(end_time_day), "yyyyMMdd"));
        }

        if (StringUtils.isNotBlank(start_time_day) && StringUtils.isBlank(end_time_day)) {
            params.put("end_time_day", DateUtils.formatDate(new Date(), "yyyyMMdd"));
        }
        params.put("agent_id", AuthorityUtils.getLoginAgentId(request));
        PageContainer page = customerManageService.querycustomerConsumeEveryReport(params);
        return R.ok("获取预付费客户消耗每天报表成功", page);
    }

    /**
     * 代理商客户消耗汇总报表-后付费
     */
    @PostMapping("/report/consume/suftotal")
    @ApiOperation(value = "/report/consume/suftotal", notes = "代理商客户消耗汇总报表-后付费", response = R.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "customerInfo", value = "客户", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "start_time_day", value = "开始时间", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "end_time_day", value = "结束时间", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "pageRowCount", value = "行数", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "currentPage", value = "页数", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "smstype", value = "短信类型", dataType = "String", paramType = "query")})
    public R consumeReportSufTotal(@RequestBody String param,HttpServletRequest request) {
        JSONObject jo=new JSONObject();
        //如果页面传的是json字符串，用下列方式解析
        Map<String, String> param1=(Map<String, String> )jo.parse(param); //string转map
        Map<String, String> totalParam=new HashMap<>();
        Map<String, String> params = ControllerUtils.buildQueryMap(String.valueOf(param1.get("pageRowCount")), String.valueOf(param1.get("currentPage")), request);
        List<Map<String, Object>> total = new ArrayList<>();
        String start_time_day =param1.get("start_time_day");
        String end_time_day =param1.get("end_time_day");
        if (StringUtils.isNotBlank(param1.get("customerInfo"))) {
            params.put("customerInfo", param1.get("customerInfo"));
            totalParam.put("customerInfo", param1.get("customerInfo"));
        }

        if (StringUtils.isNotBlank(param1.get("smstype"))) {
            params.put("smstype", param1.get("smstype"));
            totalParam.put("smstype", param1.get("smstype"));
        }
        params.put("agent_id", AuthorityUtils.getLoginAgentId(request));
        totalParam.put("agent_id", AuthorityUtils.getLoginAgentId(request));
        // 后付费设置
        params.put("paytype", "1");
        totalParam.put("paytype", "1");
        String every_day_flag = "no";
        DateTime dt = new DateTime();

        // 若未传值，默认查询昨天的
        if (StringUtils.isBlank(start_time_day)) {
            DateTime yesterday = dt.minusDays(1);
            start_time_day = yesterday.toString("yyyyMMdd");
            end_time_day = yesterday.toString("yyyyMMdd");
        } else {
            if (StringUtils.isBlank(end_time_day)) {
                end_time_day = dt.toString("yyyy-MM-dd");
            }
            Calendar beginCal = Calendar.getInstance();
            beginCal.setTime(DateUtils.parseDate(start_time_day));
            Calendar endCal = Calendar.getInstance();
            endCal.setTime(DateUtils.parseDate(end_time_day));
            long between_days = (endCal.getTimeInMillis() - endCal.getTimeInMillis()) / (1000 * 3600 * 24);


            if (Integer.parseInt(String.valueOf(between_days)) != 0) {
                every_day_flag = "yes";
            }

            start_time_day = DateUtils.formatDate(beginCal.getTime(), "yyyyMMdd");
            end_time_day = DateUtils.formatDate(endCal.getTime(), "yyyyMMdd");
        }

        params.put("start_time_day", start_time_day);
        totalParam.put("start_time_day", start_time_day);
        params.put("end_time_day", end_time_day);
        totalParam.put("end_time_day", end_time_day);
        PageContainer page = customerManageService.queryCustomerConsumeReport(params);
        total = customerManageService.total(totalParam);
        Map<String, Object> map = Maps.newHashMap();
        map.put("page", page);
        map.put("every_day_flag", every_day_flag);
        Map<String,Object> totalMap = new HashMap<>();
        if(!total.isEmpty()){
            if(total.size()==1){
                if(total.get(0)!=null){
                    totalMap.put("rownum", "合计");
                    //提交条数
                    totalMap.put("send_num", total.get(0).get("sendNum"));
                    //计费条数
                    totalMap.put("chargeTotal", total.get(0).get("charge_total"));
                    //成功条数
                    totalMap.put("success_num", total.get(0).get("successNum"));
                    //未知条数
                    totalMap.put("not_known_num", total.get(0).get("notKnownNum"));
                    //失败条数
                    totalMap.put("fail_num", total.get(0).get("failNum"));
                    //待发送条数
                    totalMap.put("wait_send_num", total.get(0).get("waitSendNum"));
                    //拦截条数
                    totalMap.put("intercept_num", total.get(0).get("interceptNum"));
                    page.getList().add(totalMap);
                }
            }else{
                return R.error("获取合计失败");
            }
        }
        return R.ok("获取后付费客户消耗汇总报表成功", page);
    }

    /**
     * 代理商客户消耗汇总报表-每天-后付费
     */
    @GetMapping("/report/consume/sufday")
    @ApiOperation(value = "/report/consume/sufday", notes = "代理商客户消耗汇总报表-每天-后付费", response = R.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "customerInfo", value = "客户", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "start_time_day", value = "开始时间", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "end_time_day", value = "结束时间", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "pageRowCount", value = "行数", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "currentPage", value = "页数", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "smstype", value = "短信类型", dataType = "String", paramType = "query")})
    public R consumeReportSufDay(String customerInfo, String smstype, String start_time_day, String end_time_day,
                                 String pageRowCount, String currentPage, HttpServletRequest request) {

        Map<String, String> params = ControllerUtils.buildQueryMap(pageRowCount, currentPage, request);
        if (StringUtils.isNotBlank(customerInfo)) {
            params.put("customerInfo", customerInfo);
        }

        if (StringUtils.isNotBlank(smstype)) {
            params.put("smstype", smstype);
        }
        // 后付费设置
        params.put("paytype", "1");
        if (StringUtils.isNotBlank(start_time_day)) {
            params.put("start_time_day", DateUtils.formatDate(DateUtils.parseDate(start_time_day), "yyyyMMdd"));
        }
        if (StringUtils.isNotBlank(end_time_day)) {
            params.put("end_time_day", DateUtils.formatDate(DateUtils.parseDate(end_time_day), "yyyyMMdd"));
        }

        if (StringUtils.isNotBlank(start_time_day) && StringUtils.isBlank(end_time_day)) {
            params.put("end_time_day", DateUtils.formatDate(new Date(), "yyyyMMdd"));
        }
        params.put("agent_id", AuthorityUtils.getLoginAgentId(request));
        PageContainer page = customerManageService.querycustomerConsumeEveryReport(params);
        return R.ok("获取后付费客户消耗每天报表成功", page);
    }

    /**
     * 代理商客户消耗汇总报表-下载报表o
     */
    @PostMapping("/report/consume/consumeReportTotalExport")
    @ApiOperation(value = "/report/consume/consumeReportTotalExport", notes = "代理商客户消耗汇总报表-下载报表", response = R.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "customerInfo", value = "客户", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "start_time_day", value = "开始时间", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "end_time_day", value = "结束时间", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "pageRowCount", value = "行数", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "currentPage", value = "页数", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "paytype", value = "付费类", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "smstype", value = "短信类型", dataType = "String", paramType = "query")})
    public void consumeReportTotalExport(String customerInfo, String paytype, String smstype, String start_time_day,
                                         String end_time_day, HttpServletRequest request, HttpServletResponse response) {
        String fileName = "";
        if ("0".equals(paytype)) {
            fileName = "预付费";
        } else if ("1".equals(paytype)) {
            fileName = "后付费";
        }
        String every_day_flag = "no";
        DateTime dt = new DateTime();

        // 若未传值，默认查询昨天的
        if (StringUtils.isBlank(start_time_day)) {
            DateTime yesterday = dt.minusDays(1);
            start_time_day = yesterday.toString("yyyyMMdd");
            end_time_day = yesterday.toString("yyyyMMdd");
        } else {
            if (StringUtils.isBlank(end_time_day)) {
                end_time_day = dt.toString("yyyy-MM-dd");
            }
            Calendar beginCal = Calendar.getInstance();
            beginCal.setTime(DateUtils.parseDate(start_time_day));
            Calendar endCal = Calendar.getInstance();
            endCal.setTime(DateUtils.parseDate(end_time_day));
            long between_days = (endCal.getTimeInMillis() - endCal.getTimeInMillis()) / (1000 * 3600 * 24);

            System.out.println(between_days);

            if (Integer.parseInt(String.valueOf(between_days)) != 0) {
                every_day_flag = "yes";
            }

            start_time_day = DateUtils.formatDate(beginCal.getTime(), "yyyyMMdd");
            end_time_day = DateUtils.formatDate(endCal.getTime(), "yyyyMMdd");
        }
        String filePath = ConfigUtils.temp_file_dir + "/" + fileName + "子账户消耗汇总报列表" + start_time_day + "至" + end_time_day
                + ".xls";
        Excel excel = new Excel();
        excel.setFilePath(filePath);
        excel.setTitle(fileName);

        Map<String, String> params = ControllerUtils.buildQueryMap(request);

        Map<String, Object> fomdata = new HashedMap();

        params.put("paytype", paytype);
        params.put("agent_id", AuthorityUtils.getLoginAgentId(request));
        StringBuffer buffer = new StringBuffer();
        buffer.append("查询条件：");
        if (StringUtils.isNotBlank(customerInfo)) {
            params.put("customerInfo", customerInfo);

            buffer.append("  子账户ID，子账户名称：");
            buffer.append(customerInfo);
            buffer.append(";");
        }
        // 短信类型,产品类型
        if (StringUtils.isNotBlank(smstype)) {
            params.put("smstype", smstype);
            buffer.append("  短信类型：：");
            buffer.append(smstype);
            buffer.append(";");
        }
        // if("0".equals(paytype)){
        // if(StringUtils.isNotBlank(product_type)){
        // params.put("product_type",product_type);
        // buffer.append(" 产品类型：：");
        // buffer.append(product_type);
        // buffer.append(";");
        // }
        // }

        if (StringUtils.isNotBlank(start_time_day)) {
            params.put("start_time_day", start_time_day);

            buffer.append("  开始时间：");
            buffer.append(start_time_day);
            buffer.append(";");
        }

        if (StringUtils.isNotBlank(end_time_day)) {
            params.put("end_time_day", end_time_day + " 23:59:59");

            buffer.append("  结束时间：");
            buffer.append(end_time_day);
            buffer.append(";");
        }

        excel.addRemark(buffer.toString());

        excel.addHeader(20, "子账户ID", "clientid");
        excel.addHeader(20, "子账户名称", "name");
        excel.addHeader(20, "短信类型", "smstype_str");
        // if("0".equals(paytype)){
        // excel.addHeader(20, "产品类型", "product_type_str");
        // }

        excel.addHeader(20, "提交条数（条）", "send_num");
        excel.addHeader(20, "计费条数（条）", "chargeTotal");
        excel.addHeader(20, "成功条数（条）", "success_num");
        excel.addHeader(20, "未知条数（条）", "not_known_num");
        excel.addHeader(20, "失败条数（条）", "fail_num");
        excel.addHeader(20, "待发送条数（条）", "wait_send_num");
        excel.addHeader(20, "拦截条数（条）", "intercept_num");

        params.put("limit", "LIMIT 0 , 60000");
        // List<Map<String, Object>> data =
        // statDao.getSearchList("bussiness.queryStatistic", formData);

        fomdata.putAll(params);
        List<Map<String, Object>> data = customerManageService.queryCustomerConsumeReport4Export(params);

        // excel.setDataList(customerManageService.queryCustomerConsumeReport4Export(params));
        excel.setShowRownum(false);
        int i = 1;
        for (Map da : data) {
            da.put("rownum", i++);
        }

        int totalCount = data.size();
        if(totalCount>0){
            Map subtotal = customerManageService.queryCustomerConsumeReportTotal(fomdata);
            subtotal.put("smstype_str", "共计");
            subtotal.put("send_num", subtotal.get("num_all_total"));
            subtotal.put("chargeTotal", subtotal.get("chargeTotal_total"));
            subtotal.put("success_num", subtotal.get("num_sucs_total"));
            subtotal.put("not_known_num", subtotal.get("num_known_total"));
            subtotal.put("fail_num", subtotal.get("num_fail_total"));
            subtotal.put("wait_send_num", subtotal.get("num_pending_total"));
            subtotal.put("intercept_num", subtotal.get("num_intercept_total"));

            data.add(subtotal);
            excel.setDataList(data);

            if (ExcelUtils.exportExcel(excel, totalCount)) {
                FileUtils.download(filePath, response);
                FileUtils.delete(filePath);
            } else {
                ControllerUtils.renderText(response, "导出Excel文件失败，请联系管理员");
            }
        }

    }

    /**
     * 代理商客户消耗汇总报表-下载报表
     */
    @PostMapping("/report/consume/consumeReportDayExport")
    @ApiOperation(value = "/report/consume/consumeReportDayExport", notes = "代理商客户消耗汇总报表-下载报表每日", response = R.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "customerInfo", value = "客户", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "start_time_day", value = "开始时间", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "end_time_day", value = "结束时间", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "pageRowCount", value = "行数", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "currentPage", value = "页数", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "paytype", value = "付费类", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "smstype", value = "短信类型", dataType = "String", paramType = "query")})
    public void consumeReportDayExport(String customerInfo, String paytype, String smstype, String product_type,
                                       String start_time_day, String end_time_day, HttpServletRequest request, HttpServletResponse response) {

        String fileName = "";
        if ("0".equals(paytype)) {
            fileName = "预付费";
        } else if ("1".equals(paytype)) {
            fileName = "后付费";
        }
        String every_day_flag = "no";
        DateTime dt = new DateTime();
        // 若未传值，默认查询昨天的
        if (StringUtils.isBlank(start_time_day)) {
            DateTime yesterday = dt.minusDays(1);
            start_time_day = yesterday.toString("yyyyMMdd");
            end_time_day = yesterday.toString("yyyyMMdd");
        } else {
            if (StringUtils.isBlank(end_time_day)) {
                end_time_day = dt.toString("yyyy-MM-dd");
            }
            Calendar beginCal = Calendar.getInstance();
            beginCal.setTime(DateUtils.parseDate(start_time_day));
            Calendar endCal = Calendar.getInstance();
            endCal.setTime(DateUtils.parseDate(end_time_day));
            long between_days = (endCal.getTimeInMillis() - endCal.getTimeInMillis()) / (1000 * 3600 * 24);

            System.out.println(between_days);

            if (Integer.parseInt(String.valueOf(between_days)) != 0) {
                every_day_flag = "yes";
            }

            start_time_day = DateUtils.formatDate(beginCal.getTime(), "yyyyMMdd");
            end_time_day = DateUtils.formatDate(endCal.getTime(), "yyyyMMdd");
        }
        String filePath = ConfigUtils.temp_file_dir + "/" + fileName + "客户日数据列表" + start_time_day + "至" + end_time_day
                + ".xls";
        Excel excel = new Excel();
        excel.setFilePath(filePath);
        excel.setTitle(fileName);

        Map<String, String> params = ControllerUtils.buildQueryMap(request);
        Map<String, Object> fomdata = new HashedMap();

        params.put("paytype", paytype);
        params.put("agent_id", AuthorityUtils.getLoginAgentId(request));
        StringBuffer buffer = new StringBuffer();
        buffer.append("查询条件：");
        if (StringUtils.isNotBlank(customerInfo)) {
            params.put("customerInfo", customerInfo);

            buffer.append("  客户ID，客户名称：");
            buffer.append(customerInfo);
            buffer.append(";");
        }
        // 短信类型,产品类型
        if (StringUtils.isNotBlank(smstype)) {
            params.put("smstype", smstype);
            buffer.append("  短信类型：：");
            buffer.append(smstype);
            buffer.append(";");
        }
        // if(StringUtils.isNotBlank(product_type)){
        // params.put("product_type",product_type);
        // buffer.append(" 产品类型：：");
        // buffer.append(product_type);
        // buffer.append(";");
        // }

        if (StringUtils.isNotBlank(start_time_day)) {
            params.put("start_time_day", start_time_day);

            buffer.append("  开始时间：");
            buffer.append(start_time_day);
            buffer.append(";");
        }

        if (StringUtils.isNotBlank(end_time_day)) {
            params.put("end_time_day", end_time_day + " 23:59:59");

            buffer.append("  结束时间：");
            buffer.append(end_time_day);
            buffer.append(";");
        }

        excel.addRemark(buffer.toString());
        excel.addHeader(20, "日期", "date");
        excel.addHeader(20, "客户ID", "clientid");
        excel.addHeader(20, "客户名称", "name");
        excel.addHeader(20, "短信类型", "smstype_str");
        // if("0".equals(paytype)){
        // excel.addHeader(20, "产品类型", "product_type_str");
        // }
        excel.addHeader(20, "提交条数（条）", "send_num");
        excel.addHeader(20, "计费条数（条）", "chargeTotal");
        excel.addHeader(20, "成功条数（条）", "success_num");
        excel.addHeader(20, "未知条数（条）", "not_known_num");
        excel.addHeader(20, "失败条数（条）", "fail_num");
        excel.addHeader(20, "待发送条数（条）", "wait_send_num");
        excel.addHeader(20, "拦截条数（条）", "intercept_num");

        // excel.setDataList(customerManageService.querycustomerConsumeEveryReport4Export(params));

        params.put("limit", "LIMIT 0 , 60000");
        fomdata.putAll(params);
        // List<Map<String, Object>> data =
        // statDao.getSearchList("bussiness.queryStatistic", formData);
        List<Map<String, Object>> data = customerManageService.querycustomerConsumeEveryReport4Export(params);

        excel.setShowRownum(false);
        int i = 1;
        for (Map da : data) {
            da.put("rownum", i++);
        }

        int totalCount = data.size();
        Map subtotal = customerManageService.querycustomerConsumeEveryReportTotal(fomdata);
        subtotal.put("smstype_str", "共计");
        subtotal.put("send_num", subtotal.get("num_all_total"));
        subtotal.put("chargeTotal", subtotal.get("chargeTotal_total"));
        subtotal.put("success_num", subtotal.get("num_sucs_total"));
        subtotal.put("not_known_num", subtotal.get("num_known_total"));
        subtotal.put("fail_num", subtotal.get("num_fail_total"));
        subtotal.put("wait_send_num", subtotal.get("num_pending_total"));
        subtotal.put("intercept_num", subtotal.get("num_intercept_total"));

        data.add(subtotal);
        excel.setDataList(data);

        if (ExcelUtils.exportExcel(excel, totalCount)) {
            FileUtils.download(filePath, response);
            FileUtils.delete(filePath);
        } else {
            ControllerUtils.renderText(response, "导出Excel文件失败，请联系管理员");
        }
    }

    /**
     * 客户资质信息
     */
    @GetMapping("/cer/info")
    public R clientCerInfo(String clientId, HttpServletRequest request) {

        if (StringUtils.isBlank(clientId)) {
            return R.error("客户ID不能为空！");
        }

        OauthPic info = accountManageService.getClientCerInfo(clientId);

        String smspImgUrl = ConfigUtils.smsp_img_url.endsWith("/")
                ? ConfigUtils.smsp_img_url.substring(0, ConfigUtils.smsp_img_url.lastIndexOf("/"))
                : ConfigUtils.smsp_img_url;
        if (StringUtils.isNotBlank(info.getImgUrl())) {
            String img = smspImgUrl + "/file/scanPic.html?path=" + info.getImgUrl();
            info.setImgUrl(img);
        }

        Map<String, Object> obj = Maps.newHashMap();
        obj.put("cerInfo", info);
        obj.put("smsp_img_url", smspImgUrl);

        return R.ok("获取账户资质信息成功", obj);
    }

    /**
     * 上传帐号资质信息
     */
    @PostMapping("/cer/add")
    public R addAccountCer(OauthPic info, HttpServletRequest request) {
        R r;

        if (info == null || StringUtils.isBlank(info.getClientId()) || StringUtils.isBlank(info.getRealName())
                || StringUtils.isBlank(info.getIdNbr()) || StringUtils.isBlank(info.getImgUrl())
                || StringUtils.isBlank(info.getIdType()) || StringUtils.isBlank(info.getClientType())) {
            r = R.error("参数错误！");
            return r;
        }

        try {
            String adminId = AuthorityUtils.getLoginUserId(request).toString();
            AgentInfo agentInfo = AgentUtils.queryAgentInfoByAdminId(adminId.toString());
            if (!"3".equals(agentInfo.getOauthStatus())) {
                r = R.error("代理商还未认证！");
                return r;
            }

            info.setAgentId(agentInfo.getAgentId());

            Map<String, Object> data = accountManageService.addClientCerInfo(info); // 上传资质信息
            r = "success".equals(data.get("result").toString()) ? R.ok(data.get("msg").toString(), data.get("datePath"))
                    : R.error(data.get("msg").toString());
        } catch (Exception e) {
            logger.error("客户资质添加失败, clientId{}, 信息{}", info.getClientId(), e);
            r = R.error("服务器异常,正在检修中...");
        }
        return r;
    }

    /**
     * 修改帐号资质信息
     */
    @PostMapping("/cer/edit")
    public R editAccountCer(OauthPic info, HttpServletRequest request) {
        R r;

        if (info == null || StringUtils.isBlank(info.getClientId()) || StringUtils.isBlank(info.getRealName())
                || StringUtils.isBlank(info.getIdNbr()) || StringUtils.isBlank(info.getIdType())
                || StringUtils.isBlank(info.getClientType())) {
            r = R.error("参数错误！");
            return r;
        }

        try {
            String adminId = AuthorityUtils.getLoginUserId(request).toString();
            AgentInfo agentInfo = AgentUtils.queryAgentInfoByAdminId(adminId.toString());
            if (!"3".equals(agentInfo.getOauthStatus())) {
                r = R.error("代理商还未认证！");
                return r;
            }

            info.setAgentId(agentInfo.getAgentId());

            Map<String, Object> data = accountManageService.modClientCerInfo(info); // 上传资质信息
            r = "success".equals(data.get("result").toString()) ? R.ok(data.get("msg").toString(), data.get("datePath"))
                    : R.error(data.get("msg").toString());
        } catch (Exception e) {
            logger.error("客户资质修改失败, clientId{}, 信息{}", info.getClientId(), e);
            r = R.error("服务器异常,正在检修中...");
        }
        return r;
    }

    /**
     * 查询表中有多少客户
     */
    @GetMapping("/consume/user")
    public List<JsmsAccount> consumeAll(HttpSession session, HttpServletRequest request) {
        String agentId = AuthorityUtils.getLoginAgentId(request);// 获取代理商id
        List<JsmsAccount> list = customerManageService.queryCustomerConsumeReport(agentId);
        return list;
    }

    /**
     * 客户发送记录
     *
     * @throws ParseException
     */
    @PostMapping("/report/consume/pretotals")
//	@GetMapping("/report/consume/pretotals")
    @ApiOperation(value = "/report/consume/pretotals", notes = "代理商客户短信发送记录", response = R.class)
    @ApiImplicitParams({@ApiImplicitParam(name = "clientid", value = "账户名称", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "phone", value = "手机号", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "state", value = "发送状态", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "start_time_day", value = "开始时间", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "end_time_day", value = "结束时间", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "pageRowCount", value = "行数", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "currentPage", value = "页数", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "content", value = "发送内容", dataType = "String", paramType = "query")})
    public R consumeReportPre(String clientid, String phone, String content, String start_time_day, String end_time_day,
                              String pageRowCount, String currentPage, String state, HttpServletRequest request) throws ParseException {

        Map<String, String> params = ControllerUtils.buildQueryMap(pageRowCount, currentPage, request);
        Map<String, Object> objectMap = new HashMap<>();
        StringBuilder parstr = new StringBuilder();
        Object obj = new Object();
        if (StringUtils.isBlank(clientid)) {
            return R.ok("请选择账户名称");
        }
        if (StringUtils.isNotBlank(clientid)) {
            params.put("account_id", clientid);
        }
        if (StringUtils.isNotBlank(state)) {

            if (state.equals("1")) {
                params.put("send_status", "1");
            }
            if (state.equals("0")) {
                params.put("send_status", "0");
            }
            if (state.equals("3")) {
                params.put("send_status", "3");
            }
            if (state.equals("55")) {
                parstr = parstr.append("'").append(5).append("'").append(",");
                parstr = parstr.append("'").append(7).append("'").append(",");
                parstr = parstr.append("'").append(8).append("'").append(",");
                parstr = parstr.append("'").append(9).append("'").append(",");
                parstr = parstr.append("'").append(10).append("'");
                params.put("send_status", parstr.toString());
            }
            if (state.equals("46")) {
                parstr = parstr.append("'").append(4).append("'").append(",");
                parstr = parstr.append("'").append(6).append("'");
                params.put("send_status", parstr.toString());
            }
        }
        if (StringUtils.isNotBlank(phone)) {
            params.put("phone", phone);
        }
        if (StringUtils.isNotBlank(content)) {
            params.put("content", content);
        }

        String date = Objects.toString(request.getParameter("start_time_day"), "");
        String start_time = date + " 00:00:00";
        String end_time = Objects.toString(request.getParameter("start_time_day"), "") + " 23:59:59";
        params.put("data", date);
        params.put("createEndTime", end_time);
        params.put("createStartTime", start_time);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");//小写的mm表示的是分钟
        String parse = sdf.format(new Date());//系统时间
        int i = Integer.parseInt(parse);
        String data = date.replaceAll("-", "");//传入时间
        if (StringUtils.isBlank(data)) {
            data = DateTime.now().toString("yyyyMMdd");
        }
        int j = Integer.parseInt(data);
        if (i < j) {
            return R.ok("获取短信列表成功", "");
        }

        PageContainer pageContainer = customerManageService.queryAll(params);
        Integer totalPage = pageContainer.getTotalPage();
        System.out.println(totalPage);
        return R.ok("获取短信列表成功", pageContainer);

    }

    /**
     * 代理商客户客户发送短信总报表
     */
    @PostMapping("/report/consume/excel")
    @ApiOperation(value = "/report/consume/excel", notes = "代理商客户客户发送短信总报表", response = R.class)
    @ApiImplicitParams({@ApiImplicitParam(name = "clientid", value = "账户名称", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "phone", value = "手机号", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "state", value = "发送状态", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "start_time_day", value = "开始时间", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "end_time_day", value = "结束时间", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "pageRowCount", value = "行数", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "currentPage", value = "页数", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "content", value = "发送内容", dataType = "String", paramType = "query")})
    public void exportRecord(String clientid, String phone, String content, String start_time_day, String end_time_day,
                             String pageRowCount, String currentPage, String state, HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("success", false);
        result.put("msg", "生成报表失败");
        try {

            Map<String, String> params = ControllerUtils.buildQueryMap(pageRowCount, currentPage, request);
            Map<String, Object> objectMap = new HashMap<>();
            StringBuilder parstr = new StringBuilder();
            Object obj = new Object();
            if (StringUtils.isNotBlank(clientid)) {
                params.put("account_id", clientid);
            }
            if (StringUtils.isNotBlank(state)) {

                if (state.equals("1")) {
                    params.put("send_status", "1");
                }
                if (state.equals("0")) {
                    params.put("send_status", "0");
                }
                if (state.equals("3")) {
                    params.put("send_status", "3");
                }
                if (state.equals("55")) {
                    parstr = parstr.append("'").append(5).append("'").append(",");
                    parstr = parstr.append("'").append(7).append("'").append(",");
                    parstr = parstr.append("'").append(8).append("'").append(",");
                    parstr = parstr.append("'").append(9).append("'").append(",");
                    parstr = parstr.append("'").append(10).append("'");
                    params.put("send_status", parstr.toString());
                }
                if (state.equals("46")) {
                    parstr = parstr.append("'").append(4).append("'").append(",");
                    parstr = parstr.append("'").append(6).append("'");
                    params.put("send_status", parstr.toString());
                }
            }
            if (StringUtils.isNotBlank(phone)) {
                params.put("phone", phone);
            }
            if (StringUtils.isNotBlank(content)) {
                params.put("content", content);
            }

            String date = Objects.toString(request.getParameter("start_time_day"), "");
            String start_time = date + " 00:00:00";
            String end_time = Objects.toString(request.getParameter("start_time_day"), "") + " 23:59:59";
            date = date.replaceAll("-", "");
            params.put("data", date);
            params.put("createEndTime", end_time);
            params.put("createStartTime", start_time);


            StringBuffer fileName = new StringBuffer();
            fileName.append("短信记录-");
            String uuid = UUID.randomUUID().toString().replaceAll("-", "");
            fileName.append(".xls");
            String filePath = ConfigUtils.temp_file_dir + "/" + uuid + "/" + fileName.toString();

            Excel excel = new Excel();
            excel.setFilePath(filePath);
            excel.setTitle("短信记录");
            excel.addHeader(20, "手机号", "phone");
            excel.addHeader(20, "发送内容", "content");
            excel.addHeader(20, "发送状态", "status");
            excel.addHeader(20, "状态码", "errorcode_name");
            excel.addHeader(20, "发送时间", "sendTime");
            excel.addHeader(20, "计费条数", "charge_num");

            Map<String, Object> p = new HashMap<String, Object>();
            p.putAll(params);


            List<Map<String, Object>> list = customerManageService.querySmsRecord4Excel(params);
            if (list.isEmpty()) {
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("没有数据！先不导出了  ^_^");
                response.getWriter().flush();
            } else {
                excel.setDataList(list);
                if (ExcelUtils.exportExcel(excel)) {
                    FileUtils.download(filePath, response);
                    FileUtils.delete(filePath);
                }
            }


        } catch (Exception e) {

            logger.error("导出Excel文件失败 -------> ", e);
            response.setContentType("text/plain;charset=UTF-8");
            try {
                response.getWriter().write("导出Excel文件失败，请联系管理员");
                response.getWriter().flush();
            } catch (IOException e1) {
                logger.error("导出Excel文件失败", e1);
            }
        }
    }

    @PostMapping("/sendRecord")
    @ApiOperation(value = "客户发送记录", notes = "代理商客户短信发送记录", response = R.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clientid", value = "账户名称", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "phone", value = "手机号", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "states", value = "发送状态", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "start_time_day", value = "开始时间", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "pageRowCount", value = "行数", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "currentPage", value = "页数", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "content", value = "发送内容", dataType = "String", paramType = "query")
    })
    public R sendRecord(String clientid, String phone, String content, String start_time_day,
                        String pageRowCount, String currentPage, String state) {
        try {
            if (StringUtils.isBlank(clientid) || StringUtils.isBlank(start_time_day)) {
                logger.error("必选条件为空----{客户id=" + clientid +"，开始时间="+ start_time_day +"}");
                return R.error("请选择正确的筛选条件");
            }
            JsmsAccount jsmsAccount =  jsmsAccountService.getByClientId(clientid);
            if (null == jsmsAccount) {
                logger.error("根据clientid查询客户信息错误----------{}" + clientid);
                return R.error("账号名称错误");
            }

            JsmsPage jsmsPage;
//            if (null == page || null == rows){
            if (StringUtils.isBlank(currentPage)|| StringUtils.isBlank(pageRowCount)){
                jsmsPage = new JsmsPage();
            } else {
                jsmsPage = new JsmsPage(Integer.valueOf(currentPage),Integer.valueOf(pageRowCount));
            }
            if (StringUtils.isNotBlank(phone)) {
                jsmsPage.getParams().put("phone",phone);
            }
            if (StringUtils.isNotBlank(content)) {
                jsmsPage.getParams().put("content",content);
            }
            if (StringUtils.isNotBlank(state)) {
                jsmsPage.getParams().put("state",state);
            }


            JsmsPage pageResult = jsmsAccessService.queryOneDayList(String.valueOf(jsmsAccount.getIdentify()), start_time_day, jsmsPage);
            return R.ok("获取成功", pageResult);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("获取客户发送记录失败---------{}"+e.getMessage());
            return R.error("服务器正在检修...");
        }
    }

    @PostMapping("/exportSendRecord")
    @ApiOperation(value = "下载客户发送记录报表", notes = "下载代理商客户短信发送记录报表", response = ResultVO.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clientid", value = "账户名称", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "phone", value = "手机号", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "state", value = "发送状态", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "start_time_day", value = "开始时间", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "end_time_day", value = "结束时间", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "pageRowCount", value = "行数", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "currentPage", value = "页数", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "content", value = "发送内容", dataType = "String", paramType = "query")
    })
    public ResultVO exportSendRecord(String clientid, String phone, String content, String start_time_day, String end_time_day,
                        String pageRowCount, String currentPage, String state, HttpServletResponse response) {
        if (StringUtils.isBlank(clientid) || StringUtils.isBlank(start_time_day)) {
            logger.error("必选条件为空----{客户id=" + clientid +"，开始时间="+ start_time_day +"}");
            return ResultVO.failure("请选择正确的筛选条件");
        }
        JsmsAccount jsmsAccount =  jsmsAccountService.getByClientId(clientid);
        if (null == jsmsAccount) {
            logger.error("根据clientid查询客户信息错误----------{}" + clientid);
            return ResultVO.failure("账号名称错误");
        }
        Integer page = Integer.valueOf(currentPage);
        Integer rows = Integer.valueOf(pageRowCount);
        JsmsPage jsmsPage;
        if (null == page || null == rows){
            jsmsPage = new JsmsPage();
        } else {
            jsmsPage = new JsmsPage(page,rows);
        }

        StringBuilder condition = new StringBuilder("查询条件 -> ")
                .append("账户：").append(clientid)
                .append("；开始时间：").append(start_time_day)
                .append("；结束时间：").append(end_time_day);
        if (StringUtils.isNotBlank(phone)) {
            jsmsPage.getParams().put("phone",phone);
            condition.append("；手机号码");
        }
        if (StringUtils.isNotBlank(content)) {
            jsmsPage.getParams().put("content",content);
            condition.append("；发送内容：").append(content);
        }
        if (StringUtils.isNotBlank(state)) {
            jsmsPage.getParams().put("state",state);
            condition.append("；发送状态：").append(state);
        }
        String date = new SimpleDateFormat("yyyyMMdd").format(DateUtil.stringToDate(start_time_day,"yyyy-MM-dd"));

        JsmsPage pageResult = jsmsAccessService.queryOneDayList(String.valueOf(jsmsAccount.getIdentify()), date, jsmsPage);
        List<JsmsAccess> accessList = pageResult.getData();
        if (accessList.isEmpty()) {
            return ResultVO.failure("未查询出相关数据，请选择正确筛选条件");
        }
        List<Map<String,String>> dataList = new ArrayList<>();
        for (JsmsAccess access : accessList) {
            Map<String,String> data = new HashMap<>();
            data.put("phone",access.getPhone());
            data.put("content",access.getContent());
//            data.put("state",access.getState());
            data.put("errorcode",access.getErrorcode());
            data.put("sendTime",access.getDate().toString());
            data.put("chargeNum", access.getDate().toString());
        }

        StringBuilder filePath = new StringBuilder(ConfigUtils.temp_file_dir);
        if(!ConfigUtils.temp_file_dir.endsWith("/")){
            filePath.append("/");
        }
        filePath.append(clientid)
                .append("客户发送记录").append(".xls").append("$$$").append(UUID.randomUUID());
        String path = filePath.toString();
        Excel excel = new Excel();
        excel.addRemark(condition.toString());
        excel.setFilePath(path);
        excel.setTitle("客户发送记录");
        excel.addHeader(20, "手机号", "phone");
        excel.addHeader(20, "发送内容", "content");
        excel.addHeader(20, "发送状态", "state");
        excel.addHeader(20, "状态码", "errorcode");
        excel.addHeader(20, "发送时间", "sendTime");
        excel.addHeader(20, "计费条数", "chargeNum");

        ResultVO resultVO = null;
//        resultVO = PageExportUtil.instance().exportPage(jsmsAccessService,jsmsPage,excel,"queryOneDayList");
//        if(resultVO.isSuccess()){
//            FileUtils.download("客户发送记录.xls", (String) resultVO.getData(),response);
//            FileUtils.delete(path);
//            return ResultVO.successDefault("下载成功");
//        }
        pageResult = jsmsAccessService.queryOneDayList(String.valueOf(jsmsAccount.getIdentify()), start_time_day, jsmsPage);
        List data = pageResult.getData();
        if (data.isEmpty()) {
            return ResultVO.failure("未查询出相关数据，请选择正确筛选条件");
        }
        excel.setDataList(data);
        if (ExcelUtils.exportExcel(excel)) {
            FileUtils.download(filePath.toString(), response);
            FileUtils.delete(filePath.toString());
        } else {
            ControllerUtils.renderText(response, "导出Excel文件失败，请联系管理员");
        }
        return ResultVO.successDefault("导出成功");
    }
}
