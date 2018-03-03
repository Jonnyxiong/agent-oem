package com.ucpaas.sms.api;
import com.jsmsframework.audit.entity.JsmsAutoTemplate;
import com.jsmsframework.audit.service.JsmsAutoTemplateService;
import com.jsmsframework.common.dto.JsmsPage;
import com.jsmsframework.common.dto.R;
import com.jsmsframework.common.enums.AutoTemplateLevel;
import com.jsmsframework.common.enums.AutoTemplateStatus;
import com.jsmsframework.common.enums.AutoTemplateSubmitType;
import com.jsmsframework.common.enums.WebId;
import com.jsmsframework.common.enums.balckAndWhiteTemplate.TemplateLevel;
import com.jsmsframework.user.audit.service.JsmsUserAutoTemplateService;
import com.jsmsframework.user.entity.JsmsAccount;
import com.jsmsframework.user.entity.JsmsAgentInfo;
import com.jsmsframework.user.service.JsmsAccountService;
import com.jsmsframework.user.service.JsmsAgentInfoService;
import com.jsmsframework.user.service.JsmsUserService;
import com.ucpaas.sms.common.entity.PageContainer;
import com.ucpaas.sms.common.util.StringUtils;
import com.ucpaas.sms.dto.JsmsAutoTemplateDTO;
import com.ucpaas.sms.service.util.ConfigUtils;
import com.ucpaas.sms.util.JsonUtils;
import com.ucpaas.sms.util.PageConvertUtil;
import com.ucpaas.sms.util.file.FileUtils;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by xiongfenglin on 2017/10/21.
 *
 * @author: xiongfenglin
 */
@Controller
@RequestMapping(value="/api/autoTemplate")
public class ApiAutoTemplateController {
    @Autowired
    private JsmsAutoTemplateService jsmsAutoTemplateService;
    @Autowired
    private JsmsUserService jsmsUserService;
    @Autowired
    private JsmsAccountService jsmsAccountService;
    @Autowired
    private JsmsAgentInfoService jsmsAgentInfoService;
    @Autowired
    private JsmsUserAutoTemplateService jsmsUserAutoTemplateService;
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiAutoTemplateController.class);
    @RequestMapping(path="/list",method = RequestMethod.GET )
    public ModelAndView autoTemplate(HttpSession session, HttpServletRequest request, ModelAndView mv){
        mv.setViewName("autoTemplate/list");
        return mv;
    }
    @RequestMapping(path="/list",method = RequestMethod.POST)
    @ResponseBody
    public String autoTemplateQuery(HttpSession session, HttpServletRequest request,@RequestParam Map<String,String> params){
        JsmsAccount jsmsAccount = null;
        JsmsAgentInfo jsmsAgentInfo = null;
        Set<String> clientIds = new HashSet<>();
        int index =0;
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd  HH:mm:ss");
        // 创建时间：开始时间
        String createStartTime = request.getParameter("createStartTime");
        String createEndTime = request.getParameter("createEndTime");
        if (StringUtils.isNotBlank(createStartTime)) {
            params.put("createStartTime", createStartTime+" 00:00:00");
        }
        // 创建时间：结束时间
        if (StringUtils.isNotBlank(createEndTime)) {
            params.put("createEndTime", createEndTime+" 23:59:59");
        }
        Integer agentId =Integer.parseInt(session.getAttribute("agentId").toString());
        if (StringUtils.isBlank(String.valueOf(params.get("clientId")))) {
            if(String.valueOf(agentId) != null){
                List<JsmsAccount> jsmsAcco = jsmsAccountService.findListForReturnQuantity(agentId);
                if(jsmsAcco.size()>0){
                    for(int i =0;i<jsmsAcco.size();i++) {
                        clientIds.add(jsmsAcco.get(i).getClientid());
                    }
                }else{
                    clientIds.add("-1");
                }
            }
        }else{
            clientIds.add(params.get("clientId"));
        }
        JsmsPage jsmsPage = PageConvertUtil.paramToPage(params);
        jsmsPage.setParams(params);
        jsmsPage.getParams().put("clientIds",clientIds);
        jsmsPage.setOrderByClause("a.create_time DESC");
        JsmsPage queryPage = jsmsUserAutoTemplateService.findListOfAutoTemplate(jsmsPage,WebId.OEM代理商平台.getValue(),AutoTemplateLevel.用户级别);
        List<JsmsAutoTemplateDTO> list = new ArrayList<>();
        for (Object temp : queryPage.getData()) {
            index =index+1;
            JsmsAutoTemplateDTO jsmsAutoTemplateDTO = new JsmsAutoTemplateDTO();
            BeanUtils.copyProperties(temp , jsmsAutoTemplateDTO);
            com.jsmsframework.user.entity.JsmsUser jsmsUser = jsmsUserService.getById(String.valueOf(jsmsAutoTemplateDTO.getAdminId()));
            if(jsmsUser != null){
                jsmsAutoTemplateDTO.setAdminName(jsmsUser.getRealname());
            }
            if(jsmsAutoTemplateDTO.getSubmitType().equals(AutoTemplateSubmitType.客户提交.getValue())){
                jsmsAccount = jsmsAccountService.getByClientId(String.valueOf(jsmsAutoTemplateDTO.getUserId()));
                if(jsmsAccount!=null){
                    jsmsAutoTemplateDTO.setUserName(jsmsAccount.getName());
                }
            }else if(jsmsAutoTemplateDTO.getSubmitType().equals(AutoTemplateSubmitType.代理商提交.getValue())){
                jsmsAgentInfo = jsmsAgentInfoService.getByAgentId(Integer.parseInt(String.valueOf(jsmsAutoTemplateDTO.getUserId())));
                if(jsmsAgentInfo!=null){
                    jsmsAutoTemplateDTO.setUserName(jsmsAgentInfo.getAgentName());
                }
            }else{
                jsmsAutoTemplateDTO.setUserName("系统");
            }
            if(jsmsAutoTemplateDTO.getRemark()==null){
                jsmsAutoTemplateDTO.setRemark(" ");
            }
            if(StringUtils.isNotBlank(String.valueOf(jsmsAutoTemplateDTO.getAgentId()))&&jsmsAutoTemplateDTO.getAgentId()!=null){
                if(jsmsAutoTemplateDTO.getAgentId().equals(agentId)&&jsmsAutoTemplateDTO.getWebId().equals(WebId.OEM代理商平台.getValue())&&jsmsAutoTemplateDTO.getSubmitType().equals(1)){
                    if(!jsmsAutoTemplateDTO.getState().equals(0)){
                        jsmsAutoTemplateDTO.setIsCreateor(1);
                    }
                }else{
                    jsmsAutoTemplateDTO.setIsCreateor(0);
                }
            }else{
                jsmsAutoTemplateDTO.setIsCreateor(0);
            }
            jsmsAutoTemplateDTO.setOrderNo(index);
            jsmsAutoTemplateDTO.setCreateTimeStr(format.format(jsmsAutoTemplateDTO.getCreateTime()));
            if(jsmsAutoTemplateDTO.getUpdateTime()!=null){
                jsmsAutoTemplateDTO.setUpdateTimeStr(format.format(jsmsAutoTemplateDTO.getUpdateTime()));
            }
            list.add(jsmsAutoTemplateDTO);
        }
        queryPage.setData(list);
        PageContainer page  = PageConvertUtil.pageToContainer(queryPage);
        return JsonUtils.toJson(page);
    }

    /**
     * 查询所有的客户
     */
    @RequestMapping("/autoTemplateAccounts")
    @ResponseBody
    public List<JsmsAccount> getAccounts(HttpSession session, HttpServletRequest request) {
        List<JsmsAccount> data = jsmsAccountService.findListForReturnQuantity(Integer.parseInt(session.getAttribute("agentId").toString()));
        return data;
    }

    @RequestMapping(path="/add",method = RequestMethod.GET )
    public ModelAndView autoTemplateAddView(HttpSession session, HttpServletRequest request, ModelAndView mv){
        mv.setViewName("autoTemplate/add");
        return mv;
    }

    @RequestMapping(path="/autoTemplateModify",method = RequestMethod.GET)
    @ResponseBody
    public R autoTemplateAdd(HttpSession session, HttpServletRequest request, ModelAndView mv){
        JsmsAutoTemplate jsmsAutoTemplate = null;
        if(StringUtils.isNotBlank(request.getParameter("templateId"))){
            jsmsAutoTemplate= jsmsAutoTemplateService.getByTemplateId(Integer.parseInt(request.getParameter("templateId")));
        }
        if(jsmsAutoTemplate!=null){
            return R.ok("获取成功", jsmsAutoTemplate);
        }else{
            return R.error("获取数据失败");
        }
    }

    /**
     * 删除客户模板
     */
    @RequestMapping(value = "/del",method = RequestMethod.POST)
    @ResponseBody
    public R autoTemplateDel(HttpSession session, HttpServletRequest request) {
        R r = new R();
        String templateIdStr = request.getParameter("templateId");
        Integer templateId = null;
        // 模板ID
        if (StringUtils.isNotBlank(templateIdStr)) {
            templateId  = Integer.parseInt(templateIdStr);
        }

        if (templateId == null) {
            r.error("模板ID不能为空");
            return r;
        }else{
            r = jsmsAutoTemplateService.delAutoTemplate(templateId);
            return r;
        }
    }

    /**
     * 新增客户模板
     */
    @RequestMapping("/autoTemplateSave")
    @ResponseBody
    public R autoTemplateSave(HttpSession session, HttpServletRequest request,@RequestParam Map<String,String> params) {
        boolean isMod = false;
        JsmsAutoTemplate template = new JsmsAutoTemplate();
        R r = null;
        // 模板ID
        Object obj = request.getParameter("templateId");
        if (obj != null && obj != "") {
            isMod = true;
            template.setTemplateId(Integer.parseInt(String.valueOf(obj)));
        }
        // 模板类型
        obj = request.getParameter("templateType");
        if (obj != null && obj != "") {
            template.setTemplateType(Integer.parseInt(String.valueOf(obj)));
        }

        // 用户帐号
        obj = request.getParameter("clientId");
        if (obj != null && obj != "") {
            template.setClientId(String.valueOf(obj));
        }

        // 短信类型
        obj = request.getParameter("smsType");
        if (obj != null && obj != "") {
            template.setSmsType(Integer.parseInt(String.valueOf(obj)));
        }
        // 模板内容
        obj = request.getParameter("content");
        if (obj != null && obj != "") {
            template.setContent(String.valueOf(obj));
        }
        // 短信签名
        obj = request.getParameter("sign");
        if (obj != null && obj != "") {
            template.setSign(String.valueOf(obj));
        }
        if (StringUtils.isNotBlank(request.getParameter("state"))){
            if (request.getParameter("state").equals("1")){
                template.setState(AutoTemplateStatus.待审核.getValue());
            }else if(request.getParameter("state").equals("3")){
                template.setState(AutoTemplateStatus.审核不通过.getValue());
            }
        }else{
            template.setState(AutoTemplateStatus.待审核.getValue());
        }
        template.setSubmitType(AutoTemplateSubmitType.代理商提交.getValue());
        template.setWebId(4);
        template.setAgentId(Integer.parseInt(String.valueOf(session.getAttribute("agentId"))));
        template.setTemplateLevel(TemplateLevel.用户级别.getValue());
        if (isMod) {
            if (template == null){
                r.error("智能模板不能为空");
                return r;
            }else{
                if (template.getTemplateId() == null){
                    r.error("智能模板的模板ID不能为空");
                    return r;
                }else{
                    r = jsmsAutoTemplateService.modifyTemplate(template);
                }
            }
        } else {
            template.setUserId(String.valueOf(session.getAttribute("agentId")));
            r = jsmsAutoTemplateService.addAutoTemplate(template);
        }
        return r;
    }

    @RequestMapping("/downloadExcelTemplate")
    public void downloadExcelTemplate(HttpSession session, HttpServletRequest request,HttpServletResponse response) {
        String path = request.getServletContext().getRealPath("/template/批量添加智能模板.xls");
        FileUtils.download(path,response);
    }

    /**
     * 批量添加智能模板
     */
    @RequestMapping("/addAutoTemplateBatch")
    @ResponseBody
    public R importOperationExcel(HttpSession session,HttpServletRequest request,@RequestParam("excel") CommonsMultipartFile file) {
        R r = new R();
        String fileName = file.getOriginalFilename();
        LOGGER.debug("importOperationExcel[fileName={}]", fileName);
        if (StringUtils.isBlank(file.getContentType())) {
            r.setCode(500);
            r.setMsg("请先选择导入Excel");
            return r;
        }
        if (file.getSize() > 2097152L) {
            r.setCode(500);
            r.setMsg("您选择的文件大于2M,请将excel拆分后重新导入");
            return r;
        }
        LOGGER.debug("导入文件的类型 ----> {}", file.getContentType());
        String path = new StringBuilder(ConfigUtils.temp_file_dir).append("/").toString();
        FileUtils.delete2(path);
        FileUtils.upload2(path, fileName, file);
        // 获得Excel文件中的数据
        LOGGER.debug("智能模板Excel 读取完成  ----------> 开始解析");
        r = jsmsUserAutoTemplateService.addAutoTemplateBatch(null,null,String.valueOf(session.getAttribute("agentId").toString()), path, WebId.OEM代理商平台.getValue());
        return r;
    }
    @RequestMapping("/exportImportResult")
    @ResponseBody
    public String exportError(HttpSession session,HttpServletResponse response){
        String msg = "";
        String filePath = ConfigUtils.temp_file_dir +"/import"+ "/批量添加智能模板结果-userid-" + session.getAttribute("agentId")+".xls";
        File file = new File(filePath);
        if(file.exists()){
            FileUtils.download(filePath,response);
            msg = "下载成功";
        }else{
            msg = "文件过期、不存在或者已经被管理员删除";
        }
        return msg;
    }
    private File multipartToFile(MultipartFile multfile) throws IOException {
        CommonsMultipartFile cf = (CommonsMultipartFile)multfile;
        //这个myfile是MultipartFile的
        DiskFileItem fi = (DiskFileItem) cf.getFileItem();
        File file = fi.getStoreLocation();
        return file;
    }

    @RequestMapping(path="/commonlist",method = RequestMethod.GET )
    public ModelAndView overAllList(HttpSession session, HttpServletRequest request, ModelAndView mv){
        mv.setViewName("autoTemplate/commonlist");
        return mv;
    }
    @RequestMapping(path="/commonlist",method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "获取通用模板", notes = "通用模板",tags = "测试/报备", response = JsmsPage.class)
    @ApiImplicitParams({ @ApiImplicitParam(name = "page", value = "当前页码", dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "rows", value = "每页行数", dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "params", value = "参数", dataType = "int", paramType = "query") })
    public String overAllListQuery(@RequestParam Map<String,String> params){
        int rowNum = 0;
        //params.put("clientId","*");
        JsmsPage jsmsPage = PageConvertUtil.paramToPage(params);
        jsmsPage.setParams(params);
        jsmsPage.setOrderByClause("a.create_time DESC");
        JsmsPage queryPage = jsmsUserAutoTemplateService.findListOfAutoTemplate(jsmsPage,WebId.OEM代理商平台.getValue(), AutoTemplateLevel.全局级别);
        List<JsmsAutoTemplateDTO> list = new ArrayList<>();
        for (Object temp : queryPage.getData()) {
            rowNum =rowNum+1;
            JsmsAutoTemplateDTO jsmsAutoTemplateDTO = new JsmsAutoTemplateDTO();
            BeanUtils.copyProperties(temp , jsmsAutoTemplateDTO);
            jsmsAutoTemplateDTO.setOrderNo(rowNum);
            list.add(jsmsAutoTemplateDTO);
        }
        queryPage.setData(list);
        PageContainer page  = PageConvertUtil.pageToContainer(queryPage);
        return JsonUtils.toJson(page);
    }
}

