package com.ucpaas.sms.api;

import com.jsmsframework.common.dto.JsmsPage;
import com.jsmsframework.common.dto.R;
import com.jsmsframework.common.dto.ResultVO;
import com.jsmsframework.common.enums.Code;
import com.jsmsframework.common.enums.WebId;
import com.jsmsframework.common.enums.invoice.InvoiceBodyEnum;
import com.jsmsframework.common.enums.invoice.InvoiceTypeEnum;
import com.jsmsframework.common.util.RegexUtils;
import com.jsmsframework.finance.dto.JsmsAgentInvoiceListDTO;
import com.jsmsframework.finance.entity.JsmsAgentInvoiceConfig;
import com.jsmsframework.finance.entity.JsmsAgentInvoiceList;
import com.jsmsframework.finance.service.JsmsAgentInvoiceListService;
import com.jsmsframework.user.finance.service.JsmsUserFinanceService;
import com.ucpaas.sms.service.invoice.InvoiceService;
import com.ucpaas.sms.util.StringUtils;
import com.ucpaas.sms.util.web.AuthorityUtils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.*;

/**
 * Created by xiongfenglin on 2018/1/24.
 *
 * @author: xiongfenglin
 */
@RestController
@RequestMapping("/api/invoice")
@Api(value = "发票管理", description = "发票管理API")
public class ApiInvoiceController {
    private final static Logger logger = LoggerFactory.getLogger(ApiInvoiceController.class);
    @Autowired
    private InvoiceService invoiceService;
    @Autowired
    private JsmsUserFinanceService jsmsUserFinanceService;
    @Autowired
    private JsmsAgentInvoiceListService jsmsAgentInvoiceListService;

    @PostMapping("/initInvoice")
    @ResponseBody
    @ApiOperation(value = "初始化发票", notes = "初始化发票",tags = "发票管理")
    public R initInvoice(@RequestBody JsmsAgentInvoiceList jsmsAgentInvoiceList,HttpServletRequest request) {
        ResultVO resultVO = new ResultVO();
        jsmsAgentInvoiceList.setAgentId(Integer.parseInt(AuthorityUtils.getLoginAgentId(request)));
        jsmsAgentInvoiceList.setApplicant(AuthorityUtils.getLoginUserId(request));
        jsmsAgentInvoiceList.setOperator(AuthorityUtils.getLoginUserId(request));
        jsmsAgentInvoiceList.setWebId(WebId.OEM代理商平台.getValue());
        if(jsmsAgentInvoiceList.getInvoiceType().equals(InvoiceTypeEnum.普通发票.getValue())&&jsmsAgentInvoiceList.getInvoiceBody().equals(InvoiceBodyEnum.个人.getValue())){
            jsmsAgentInvoiceList.setCreditCode(null);
        }
        resultVO = checkObject(jsmsAgentInvoiceList);
        if(resultVO == null || resultVO.isSuccess()){
            resultVO = checkJsmsAgentInvoiceList(jsmsAgentInvoiceList);
            if(resultVO == null || resultVO.isSuccess()) {
                return invoiceService.inintInvoice(jsmsAgentInvoiceList);
            }else{
                R r = new R();
                r.setCode(resultVO.getCode());
                r.setMsg(resultVO.getMsg());
                return  r;
            }
        }else{
            R r = new R();
            r.setCode(resultVO.getCode());
            r.setMsg(resultVO.getMsg());
            return  r;
        }
    }

    @PostMapping("/getCanInvoiceAmount")
    @ResponseBody
    @ApiOperation(value = "获取可开票金额", notes = "获取可开票金额",tags = "发票管理")
    public BigDecimal getCanInvoiceAmount(HttpServletRequest request) {
        String agentId = AuthorityUtils.getLoginAgentId(request);
        return jsmsUserFinanceService.getCanInvoiceAmount(Integer.parseInt(agentId));
    }

    @PostMapping("/getOrdinaryInvoiceConfig")
    @ResponseBody
    @ApiOperation(value = "获取普通发票配置信息", notes = "获取普通发票配置信息",tags = "发票管理")
    public JsmsAgentInvoiceConfig getOrdinaryInvoiceConfig(HttpServletRequest request) {
        String agentId = AuthorityUtils.getLoginAgentId(request);
        return jsmsUserFinanceService.findListNomal(Integer.parseInt(agentId));
    }

    @PostMapping("/getAddedTaxInvoiceConfig")
    @ResponseBody
    @ApiOperation(value = "获取增值发票配置信息", notes = "获取增值发票配置信息",tags = "发票管理")
    public JsmsAgentInvoiceConfig getAddedTaxInvoiceConfig(HttpServletRequest request) {
        String agentId = AuthorityUtils.getLoginAgentId(request);
        return jsmsUserFinanceService.findListAdd(Integer.parseInt(agentId));
    }

    @PostMapping("/invoiceList")
    @ResponseBody
    @ApiOperation(value = "查询发票申请列表", notes = "查询发票申请列表",tags = "发票管理",response = JsmsPage.class)
    @ApiImplicitParams({ @ApiImplicitParam(name = "page", value = "当前页码", dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "rows", value = "每页行数", dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "createTimeStart", value = "申请时间开始", dataType = "string",  paramType = "query"),
            @ApiImplicitParam(name = "createTimeEnd", value = "申请时间结束", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "status", value = "申请状态", dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "applicationOEM", value = "申请ID/发票金额/发票抬头", dataType = "string", paramType = "query") })
    public JsmsPage<JsmsAgentInvoiceList> invoiceList(JsmsPage jsmsPage,String createTimeStart,String createTimeEnd,String applicationOEM,Integer status,HttpServletRequest request) {
        Map<String, Object> params = new HashMap<>();
        params.put("createTimeStart",createTimeStart);
        params.put("createTimeEnd",createTimeEnd);
        params.put("applicationOEM",applicationOEM);
        params.put("status",status);
        Set<Integer> agentIdPermission = new HashSet<>();
        agentIdPermission.add(Integer.parseInt(AuthorityUtils.getLoginAgentId(request)));
        params.put("agentIdPermission",agentIdPermission);
        jsmsPage.setParams(params);
        jsmsPage.setOrderByClause(" update_time DESC");
        jsmsPage = jsmsUserFinanceService.queryPageList(jsmsPage, WebId.OEM代理商平台);
        return jsmsPage;
    }

    @PostMapping("/invoiceDetailed")
    @ResponseBody
    @ApiOperation(value = "查看发票信息", notes = "查看发票信息",tags = "发票管理")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "id", dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "invoiceType", value = "发票类型", dataType = "int", paramType = "query") })
    public ResultVO invoiceDetailed(Integer id, Integer invoiceType) {
        ResultVO resultVO = new ResultVO();
        JsmsAgentInvoiceListDTO jsmsAgentInvoiceListDTO = jsmsUserFinanceService.checkDetailedInformation(id, invoiceType,WebId.OEM代理商平台);
        if(jsmsAgentInvoiceListDTO!=null){
            resultVO.setCode(Code.SUCCESS);
            resultVO.setData(jsmsAgentInvoiceListDTO);
        }else{
            resultVO.setCode(Code.OPT_ERR);
            resultVO.setMsg("查看发票信息失败,请联系客服！");
        }
        return resultVO;
    }
    @PostMapping("/invoiceCancelApply")
    @ResponseBody
    @ApiOperation(value = "取消发票申请", notes = "取消发票申请",tags = "发票管理")
    public ResultVO invoiceCancelApply(@RequestBody JsmsAgentInvoiceList jsmsAgentInvoiceList,HttpServletRequest request) {
        ResultVO resultVO = new ResultVO();
        int flag = 0;
        jsmsAgentInvoiceList.setOperator(AuthorityUtils.getLoginUserId(request));
        logger.debug("取消之前的数据jsmsAgentInvoiceList-->{}",jsmsAgentInvoiceList);
        flag = jsmsAgentInvoiceListService.cancelApply(jsmsAgentInvoiceList);
        if(flag > 0){
            resultVO.setCode(Code.SUCCESS);
            resultVO.setMsg("取消成功！");
        }else{
            resultVO.setCode(Code.OPT_ERR);
            resultVO.setMsg("取消失败,请联系客服！");
        }
        return resultVO;
    }
    public ResultVO checkJsmsAgentInvoiceList(JsmsAgentInvoiceList jsmsAgentInvoiceList) {
        ResultVO resultVO = new ResultVO();
        resultVO = checkLength(jsmsAgentInvoiceList.getRemark(), 50, "备注", true);
        if (resultVO.isFailure()) {
            return resultVO;
        }
        resultVO = checkLength(jsmsAgentInvoiceList.getInvoiceHead(), 50, "发票抬头", false);
        if (resultVO.isFailure()) {
            return resultVO;
        }
        return null;
    }
    @PostMapping("/checkObject")
    @ResponseBody
    public ResultVO checkObject(@RequestBody JsmsAgentInvoiceList jsmsAgentInvoiceList){
        ResultVO resultVO = new ResultVO();
        boolean newBool= false;
        if(jsmsAgentInvoiceList.getInvoiceBody().equals(InvoiceBodyEnum.企业.getValue())){
            resultVO = checkLength(jsmsAgentInvoiceList.getCreditCode(),18,"社会统一信用代码",false);
        }else if (jsmsAgentInvoiceList.getInvoiceBody().equals(InvoiceBodyEnum.个人.getValue())){
            resultVO = checkLength(jsmsAgentInvoiceList.getCreditCode(),18,"社会统一信用代码",true);
        }
        if(resultVO.isFailure()){
            return resultVO;
        }
        if(jsmsAgentInvoiceList.getInvoiceType().equals(InvoiceTypeEnum.普通发票.getValue())){
            newBool = true;
        }
        resultVO = checkLength(jsmsAgentInvoiceList.getCompanyRegAddr(),50,"公司注册地址",newBool);
        if(resultVO.isFailure()){
            return resultVO;
        }
        resultVO = checkLength(jsmsAgentInvoiceList.getBank(),20,"基本户开户银行",newBool);
        if(resultVO.isFailure()){
            return resultVO;
        }
        resultVO = checkLength(jsmsAgentInvoiceList.getBankAccount(),20,"基本户开户账号",newBool);
        if(resultVO.isFailure()){
            return resultVO;
        }
        resultVO = checkLength(jsmsAgentInvoiceList.getTelphone(),20,"公司固定电话",newBool);
        if(resultVO.isFailure()){
            return resultVO;
        }
        resultVO = checkLength(jsmsAgentInvoiceList.getToName(),20,"收件人",newBool);
        if(resultVO.isFailure()){
            return resultVO;
        }
        resultVO = checkLength(jsmsAgentInvoiceList.getToAddr(),50,"收件人地址",newBool);
        if(resultVO.isFailure()){
            return resultVO;
        }
        resultVO = checkLength(jsmsAgentInvoiceList.getToAddrDetail(),50,"详细地址",newBool);
        if(resultVO.isFailure()){
            return resultVO;
        }
        resultVO = checkLength(jsmsAgentInvoiceList.getToQq(),20,"收件人qq",true);
        if(resultVO.isFailure()){
            return resultVO;
        }
        if(jsmsAgentInvoiceList.getInvoiceType().equals(InvoiceTypeEnum.普通发票.getValue())) {
            if (StringUtils.isNotBlank(jsmsAgentInvoiceList.getEmail()) && !RegexUtils.checkEmail(jsmsAgentInvoiceList.getEmail())) {
                return ResultVO.failure(Code.OPT_ERR_FORBIDDEN, "电子邮箱格式错误!");
            } else {
                resultVO = checkLength(jsmsAgentInvoiceList.getEmail(), 50, "电子邮箱", false);
            }
            if (resultVO.isFailure()) {
                return resultVO;
            }
        }
        if(jsmsAgentInvoiceList.getInvoiceType().equals(InvoiceTypeEnum.增值税专票.getValue())) {
            if (jsmsAgentInvoiceList.getToPhone() != null && StringUtils.isNotBlank(jsmsAgentInvoiceList.getToPhone())) {
                if (!RegexUtils.isMobile(jsmsAgentInvoiceList.getToPhone())) {
                    return ResultVO.failure(Code.OPT_ERR_FORBIDDEN, "收件人手机格式错误!");
                }
            }else{
                return ResultVO.failure(Code.OPT_ERR_FORBIDDEN, "请输入收件人手机号!");
            }
        }
        return null;
    }

    /**
     * 校验长度
     *
     * @param str     检验对象
     * @param length  长度
     * @param strName 检验对象名称
     * @param bool    是否可空  true可空
     * @return
     */
    public ResultVO checkLength(String str, Integer length, String strName, boolean bool) {
        if (str != null && StringUtils.isNotBlank(str)) {
            if (str.length() > length) {
                return ResultVO.failure(Code.OPT_ERR_FORBIDDEN, strName + "长度过长,请重新输入！");
            } else {
                return ResultVO.successDefault();
            }
        } else {
            if (bool) {
                return ResultVO.successDefault();
            } else {
                return ResultVO.failure(Code.OPT_ERR_FORBIDDEN, strName + "不能为空！");
            }
        }
    }
}
