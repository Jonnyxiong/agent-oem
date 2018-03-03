package com.ucpaas.sms.api;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jsmsframework.common.enums.ClientAlarmType;
import com.jsmsframework.common.enums.OauthStatusEnum;
import com.jsmsframework.common.util.JsonUtil;
import com.jsmsframework.finance.entity.JsmsClientBalanceAlarm;
import com.jsmsframework.finance.service.JsmsClientBalanceAlarmService;
import com.jsmsframework.order.dto.OemClientRechargeRollBackDTO;
import com.jsmsframework.order.entity.JsmsOemClientPool;
import com.jsmsframework.order.exception.JsmsOemAgentPoolException;
import com.jsmsframework.user.service.JsmsAccountService;
import com.ucpaas.sms.common.entity.PageContainer;
import com.ucpaas.sms.common.entity.R;
import com.ucpaas.sms.common.util.Collections3;
import com.ucpaas.sms.service.util.ConfigUtils;
import com.ucpaas.sms.util.DateUtils;
import com.ucpaas.sms.util.file.ExcelUtils;
import com.ucpaas.sms.util.file.FileUtils;
import com.ucpaas.sms.dto.AccountListVO;
import com.ucpaas.sms.dto.ClientBalanceAlarmPo;
import com.ucpaas.sms.model.AgentInfo;
import com.ucpaas.sms.model.Excel;
import com.ucpaas.sms.service.customer.CustomerManageService;
import com.ucpaas.sms.service.util.AgentUtils;
import com.ucpaas.sms.util.StringUtils;
import com.ucpaas.sms.util.web.AuthorityUtils;
import com.ucpaas.sms.util.web.ControllerUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.*;

/**
 * 充值回退页面api
 *
 * @outhor tanjiangqiang
 * @create 2017-11-27 14:14
 */
@RestController
@RequestMapping("/api/recharge/rollback")
@Api(value = "充值回退", description = "充值回退API")
public class ApiRechargeRollbackController {


    /**
     * 日志对象
     */
    protected Logger logger = LoggerFactory.getLogger(ApiRechargeRollbackController.class);

    @Autowired
    private CustomerManageService customerManageService;

    @Autowired
    private JsmsClientBalanceAlarmService jsmsClientBalanceAlarmService;

    @Autowired
    private JsmsAccountService jsmsAccountService;


    /**
      * @Description: 充值回退账户列表
      * @Author: tanjiangqiang
      * @Date: 2017/12/7 - 20:37
      * @param
      *
      */
    @PostMapping("/getAccounts")
    @ApiOperation(value = "充值回退账户列表", notes = "充值回退账户列表", response = R.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "customerInfo", value = "客户ID,名称", dataType = "String", paramType = "query")
    })
    public R getAccounts(@RequestBody AccountListVO accountListVO, HttpServletRequest request) {
        try {
            Map<String, String> params = ControllerUtils.buildQueryMap(accountListVO.getPageRowCount(), accountListVO.getCurrentPage(), request);
            String customerInfo = accountListVO.getCustomerInfo();
            if (StringUtils.isNotBlank(customerInfo)) {
                params.put("customerInfo", customerInfo);
            }
            params.put("isRechargeRollback","1");//充值回退业务
            PageContainer page = customerManageService.queryCustomerInfo(params);
            return R.ok("获取客户信息成功", page);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("获取账户信息失败" + e.getMessage());
            return R.error("服务器正在检修...");
        }
    }

    /**
     * 充值回退----获取客户列表-导出
     */
    @PostMapping("/recharge/export")
    @ApiOperation(value = "/account/export", notes = "获取子账户列表-导出", response = R.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "customerInfo", value = "子账户ID,子账户名称,手机号", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "start_time_day", value = "开始时间", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "end_time_day", value = "结束时间", dataType = "String", paramType = "query")})
    public void rechargeExport(String customerInfo, String start_time_day, String end_time_day,
                               HttpServletRequest request, HttpServletResponse response) {
        String filePath = ConfigUtils.temp_file_dir + "/账户列表" + DateUtils.getDate("yyyyMMddHHmmss") + ".xls";
        Excel excel = new Excel();
        excel.setFilePath(filePath);
        excel.setTitle("子账户列表");

        Map<String, String> params = ControllerUtils.buildQueryMap(request);
        params.put("isRechargeRollback","1");//充值回退列表业务
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
        excel.addHeader(50, "行业短信", "hy_remain_num");
        excel.addHeader(50, "验证码短信", "co_remain_num");
        excel.addHeader(50, "通知短信", "no_remain_num");
        excel.addHeader(50, "会员营销短信", "yx_remain_num");
        excel.addHeader(50, "国际短信", "gj_remain_num");
        excel.addHeader(100, "备注", "remarks");
        List<Map<String, Object>> list=customerManageService.queryCustomerInfoForAll(params);
        excel.setDataList(list);

        if (ExcelUtils.exportExcel(excel)) {
            FileUtils.download(filePath, response);
            FileUtils.delete(filePath);
        } else {
            ControllerUtils.renderText(response, "导出Excel文件失败，请联系管理员");
        }
    }


    /**
     * 回退产品列表
     */
    @PostMapping("rollbackData")
    @ApiOperation(value = "回退产品列表", notes = "点击回退时，显示的回退产品列表", response = R.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "productType", value = "产品类型", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "operatorCode", value = "运营商类型", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "clientId", value = "客户ID", dataType = "String", paramType = "query")})
    public R rollbackData(@RequestBody JsmsOemClientPool jsmsOemClientPool, HttpServletRequest request) {
        try {
            if (StringUtils.isBlank(jsmsOemClientPool.getClientId())) {
                return R.error("参数验证失败");
            }
            Map<String, Object> params = Maps.newHashMap();
            params.put("client_id", jsmsOemClientPool.getClientId());
            if (null != jsmsOemClientPool.getProductType()) {
                params.put("product_type", jsmsOemClientPool.getProductType().toString());
            }
            if (null != jsmsOemClientPool.getOperatorCode()) {
                params.put("operator_code", jsmsOemClientPool.getOperatorCode().toString());
            }
            List list =  customerManageService.queryCommonSmsInfoForClient(params);
            return R.ok("客户管理-回退-查询子客户短信池成功", list);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("获取回退产品列表失败,"+ e.getMessage());
            return R.error("服务器正在检修...");
        }
    }


    /**
     * 充值产品列表
     */
    @PostMapping("/rechargeData")
    @ApiOperation(value = "充值产品列表", notes = "点击充值时，显示的产品列表", response = R.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "product_type", value = "产品类型", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "operator_code", value = "运营商类型", dataType = "String", paramType = "query")})
    public R rechargeData(String product_type, String operator_code, HttpServletRequest request) {
        try {
            Map<String, Object> params = Maps.newHashMap();
            params.put("agent_id", AuthorityUtils.getLoginAgentId(request));
            params.put("product_type", product_type);
            params.put("operator_code", operator_code);
            List<Map<String, Object>> data = customerManageService.querySmsInfo(params);
            return R.ok("客户管理-充值-查询代理商短信池成功", data);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("获取充值产品列表失败," + e.getMessage());
            return R.error("服务器正在检修...");
        }
    }


    /**
     * 给客户充值
     */
    @PostMapping("/recharge")
    @ApiOperation(value = "充值", notes = "给客户充值", response = R.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "batchdata", value = "json字符串批量数据包括list（client_id,product_type,updateNum,due_time,unit_price,operator_code,area_code）", dataType = "String", paramType = "query")})
    public R rechargeSave(String batchdata, HttpServletRequest request) {
        try {
            if (StringUtils.isBlank(batchdata)) {
                return R.error("请选择充值的客户id");
            }
            String adminId = AuthorityUtils.getLoginUserId(request).toString();
            if (StringUtils.isBlank(adminId)) {
                return R.error("请先登录");
            }
            AgentInfo agentInfo = AgentUtils.queryAgentInfoByAdminId(adminId.toString());
            if (!OauthStatusEnum.证件已认证.getValue().toString().equals(agentInfo.getOauthStatus())) {
                return R.error("该代理商还未认证！");
            }
            Gson gson = new GsonBuilder().setDateFormat("yyyyMMddHHmmss").create();
            Type listType = new TypeToken<List<OemClientRechargeRollBackDTO>>() {
            }.getType();
            List<OemClientRechargeRollBackDTO> list = gson.fromJson(batchdata, listType);
            if (list.isEmpty()) {
                logger.error("充值参数为空"+ JsonUtil.toJson(list));
                return R.error("请选择充值客户id");
            }
            Iterator<OemClientRechargeRollBackDTO> oemClientRechargeRollBackDTOIterator = list.iterator();
            // 迭代充值参数
            while (oemClientRechargeRollBackDTOIterator.hasNext()) {
                OemClientRechargeRollBackDTO dto = oemClientRechargeRollBackDTOIterator.next();
                BigDecimal updateNum = dto.getUpdateNum();
                // 充值数量小于等于0 删除该记录
                if (updateNum.compareTo(BigDecimal.ZERO) < 1) {
                    oemClientRechargeRollBackDTOIterator.remove();
                    continue;
                }
                dto.setAgentId(Integer.valueOf(AuthorityUtils.getLoginAgentId(request)));
            }
            if (list.isEmpty()) {
                logger.error("充值参数为空{}", list);
                return R.error("请选择充值数量");
            }
            return customerManageService.oemClientRecharge(list);
        } catch (JsmsOemAgentPoolException e) {
            logger.error("客户充值失败", e);
            return R.error(e.getMessage());
        } catch (Exception e) {
            logger.error("客户充值失败", e);
            return R.error("服务器异常,正在检修中...");
        }
    }

    /**
     * 充值回退-代理商客户回退
     */
    @PostMapping("/fallback")
    @ApiOperation(value = "充值回退页面的客户回退", notes = "充值回退页面的客户回退", response = R.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "batchback", value = "json字符串批量数据包括list（client_id,product_type,updateNum,due_time,unit_price,operator_code,area_code）", dataType = "String", paramType = "query")})
    public R fallback(String batchback, HttpServletRequest request) {
        try {
            String adminId = AuthorityUtils.getLoginUserId(request).toString();
            AgentInfo agentInfo = AgentUtils.queryAgentInfoByAdminId(adminId.toString());
            if (!OauthStatusEnum.证件已认证.getValue().toString().equals(agentInfo.getOauthStatus().toString())) {
                return R.error("代理商还未认证！");
            }
            Gson gson = new GsonBuilder().setDateFormat("yyyyMMddHHmmss").create();
            Type listType = new TypeToken<List<OemClientRechargeRollBackDTO>>() {
            }.getType();
            List<OemClientRechargeRollBackDTO> list = gson.fromJson(batchback, listType);
            if (list.isEmpty()){
                logger.error("json转换集合为空--------------{集合，json字符串}", list ,batchback);
                return R.error("服务器异常,正在检修中...");
            }
            Iterator<OemClientRechargeRollBackDTO> oemClientRechargeRollBackDTOIterator = list.iterator();
            // 迭代充值参数
            while (oemClientRechargeRollBackDTOIterator.hasNext()) {
                OemClientRechargeRollBackDTO dto = oemClientRechargeRollBackDTOIterator.next();
                BigDecimal updateNum = dto.getUpdateNum();
                // 充值数量小于等于0 删除该记录
                if (updateNum.compareTo(BigDecimal.ZERO) < 1) {
                    oemClientRechargeRollBackDTOIterator.remove();
                    continue;
                }
                dto.setAgentId(Integer.valueOf(AuthorityUtils.getLoginAgentId(request)));
            }
           return customerManageService.oemClientRollback(list);
        } catch (Exception e) {
            logger.error("客户回退失败, clientId{}, 信息{}", batchback,e.getMessage());
            return R.error("服务器异常,正在检修中...");
        }
    }


    /**
     * @param
     * @Description: 提醒设置
     * @Author: tanjiangqiang
     * @Date: 2017/11/27 - 14:30
     */
    @PostMapping("/remindSet")
    @ApiOperation(value = "/提醒设置", notes = "短信余额提醒设置", response = R.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clientid", value = "客户id", dataType = "String", paramType = "update"),
            @ApiImplicitParam(name = "yzmAlarmNumber", value = "验证码短信", dataType = "String", paramType = "update"),
            @ApiImplicitParam(name = "tzAlarmNumber", value = "通知短信", dataType = "String", paramType = "update"),
            @ApiImplicitParam(name = "yxAlarmNumber", value = "营销短信", dataType = "String", paramType = "update"),
            @ApiImplicitParam(name = "gjAlarmAmount", value = "国际短信", dataType = "String", paramType = "update"),
            @ApiImplicitParam(name = "alarmPhone", value = "短信提醒,多个手机号以\",\"分隔", dataType = "String", paramType = "update"),
            @ApiImplicitParam(name = "alarmEmail", value = "邮件提醒,多个邮箱以\",\"分隔", dataType = "String", paramType = "update")
    })
    public R remindSet(@RequestBody ClientBalanceAlarmPo clientBalanceAlarm) {
        try {
            if (null == clientBalanceAlarm.getYzmAlarmNumber()) {
                R.error("请设置验证码短信");
            }
            if (null == clientBalanceAlarm.getTzAlarmNumber()) {
                R.error("请设置通知短信");
            }
            if (null == clientBalanceAlarm.getYxAlarmNumber()) {
                R.error("请设置营销短信");
            }
            if (null == clientBalanceAlarm.getGjAlarmAmount()) {
                R.error("请设置国际短信");
            }
            if (null == clientBalanceAlarm.getAlarmPhone()) {
                R.error("请填写至少一个提醒短信号码");
            }
            if (null == clientBalanceAlarm.getAlarmEmail()) {
                R.error("请填写至少一个提醒邮箱");
            }
            if (null == clientBalanceAlarm.getClientid()) {
                R.error("客户id为空");
            }
            logger.debug("客户余额提醒设置：{}", JSON.toJSONString(clientBalanceAlarm));
            return customerManageService.saveClientBalanceAlarm(clientBalanceAlarm);
        } catch (Exception e) {
            e.printStackTrace();
            logger.debug("客户余额提醒设置失败, 参数：{};  错误 {}", JSON.toJSONString(clientBalanceAlarm), e);
            return R.error("客户余额提醒设置失败");
        }
    }

//    @PostMapping("/remindDate")
//    @ApiOperation(value = "/提醒设置数据", notes = "短信余额提醒设置时，原数据回显", response = R.class)
//    @ApiImplicitParams({
//            @ApiImplicitParam(name = "client_id", value = "客户id", dataType = "String", paramType = "query")
//    })
//    public R remindDate(@RequestBody JsmsClientBalanceAlarm jsmsClientBalanceAlarm) {
//        JsmsPage pageResult = null;
//        try {
//            JsmsPage page = new JsmsPage();
//            page.getParams().put("clientid",jsmsClientBalanceAlarm.getClientid());
//            pageResult = jsmsClientBalanceAlarmService.queryList(page);
//        } catch (Exception e) {
//            e.printStackTrace();
//            logger.error(e.getMessage());
//            return R.error("服务器正在检修...");
//        }
//        return R.ok("获取提醒设置数据成功",pageResult.getData());
//    }


    /**
     * 提醒设置数据
     */
    @PostMapping("/remindDate")
    @ApiOperation(value = "/提醒设置数据", notes = "短信余额提醒设置时，原数据回显", response = R.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clientid", value = "客户id", dataType = "String", paramType = "query")
    })
    public R getClientBalanceAlarm(@RequestBody JsmsClientBalanceAlarm jsmsClientBalanceAlarm) {
        try {
            String clientId = jsmsClientBalanceAlarm.getClientid();
            // 查询客户余额配置
            JsmsClientBalanceAlarm queryCba = new JsmsClientBalanceAlarm();
            queryCba.setClientid(clientId);
            List<JsmsClientBalanceAlarm> clientBalanceAlarms = jsmsClientBalanceAlarmService.findList(queryCba);
            ClientBalanceAlarmPo clientBalanceAlarmPo = null;
            if (!Collections3.isEmpty(clientBalanceAlarms))
            {
                clientBalanceAlarmPo = new ClientBalanceAlarmPo();
                clientBalanceAlarmPo.setId(clientBalanceAlarms.get(0).getId());
                clientBalanceAlarmPo.setClientid(clientBalanceAlarms.get(0).getClientid());
                clientBalanceAlarmPo.setAlarmEmail(clientBalanceAlarms.get(0).getAlarmEmail());
                clientBalanceAlarmPo.setAlarmPhone(clientBalanceAlarms.get(0).getAlarmPhone());
                for (JsmsClientBalanceAlarm clientBalanceAlarm : clientBalanceAlarms) {
                    if (clientBalanceAlarm.getAlarmType().intValue() == ClientAlarmType.验证码.getValue().intValue())
                    {
                        clientBalanceAlarmPo.setYzmAlarmNumber(clientBalanceAlarm.getAlarmNumber());
                    }
                    if (clientBalanceAlarm.getAlarmType().intValue() == ClientAlarmType.通知.getValue().intValue())
                    {
                        clientBalanceAlarmPo.setTzAlarmNumber(clientBalanceAlarm.getAlarmNumber());
                    }
                    if (clientBalanceAlarm.getAlarmType().intValue() == ClientAlarmType.营销.getValue().intValue())
                    {
                        clientBalanceAlarmPo.setYxAlarmNumber(clientBalanceAlarm.getAlarmNumber());
                    }
                    if (clientBalanceAlarm.getAlarmType().intValue() == ClientAlarmType.国际.getValue().intValue())
                    {
                        clientBalanceAlarmPo.setGjAlarmAmount(clientBalanceAlarm.getAlarmAmount());
                    }
                }
            }

            List resultData = new ArrayList();
            resultData.add(clientBalanceAlarmPo);
            return R.ok("获取提醒设置数据成功", resultData);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
            return R.error("服务器正在检修...");
        }
    }

}