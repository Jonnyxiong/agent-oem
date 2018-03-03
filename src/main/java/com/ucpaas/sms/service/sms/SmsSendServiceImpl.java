package com.ucpaas.sms.service.sms;

import cn.afterturn.easypoi.excel.ExcelImportUtil;
import cn.afterturn.easypoi.excel.entity.ImportParams;
import com.jsmsframework.access.access.entity.JsmsAccess;
import com.jsmsframework.access.access.entity.JsmsAccessMolog;
import com.jsmsframework.access.service.JsmsAccessMologService;
import com.jsmsframework.access.service.JsmsAccessService;
import com.jsmsframework.common.dto.JsmsPage;
import com.jsmsframework.common.dto.ResultVO;
import com.jsmsframework.common.entity.JsmsSystemErrorDesc;
import com.jsmsframework.common.enums.WebId;
import com.jsmsframework.common.enums.smsSend.ServiceTypeEnum;
import com.jsmsframework.common.enums.smsSend.SmsSendFileType;
import com.jsmsframework.common.service.JsmsSystemErrorDescService;
import com.jsmsframework.common.util.GZIPUtil;
import com.jsmsframework.sms.send.dto.JsmsAccessSmsDTO;
import com.jsmsframework.sms.send.dto.JsmsAccessTimerSmsDTO;
import com.jsmsframework.sms.send.po.JsmsAccessSms;
import com.jsmsframework.sms.send.po.JsmsAccessTimerSms;
import com.jsmsframework.sms.send.service.JsmsSendService;
import com.jsmsframework.sms.send.service.JsmsSubmitProgressService;
import com.jsmsframework.sms.send.service.JsmsTimerSendTaskService;
import com.jsmsframework.user.entity.JsmsAccount;
import com.jsmsframework.user.service.JsmsAccountService;
import com.ucpaas.sms.dto.AccessDTO;
import com.ucpaas.sms.dto.AccessMologDTO;
import com.ucpaas.sms.service.util.ConfigUtils;
import com.ucpaas.sms.util.RegexUtils;
import com.ucpaas.sms.util.SecurityUtils;
import com.ucpaas.sms.util.file.FileUtils;
import com.ucpaas.sms.util.security.Des3Utils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.math.BigDecimal;
import java.util.*;

@Service
public class SmsSendServiceImpl implements SmsSendService {
    private Logger logger = LoggerFactory.getLogger(SmsSendServiceImpl.class);

    public static final int M5_SIZE = 5 * 1024 * 1024;
    public static final String CHECK_AVAILABLE_CREDIT_FLAG = "1";
    public static final String AGENT_SUBMIT_TYPE = "1";
    @Autowired
    private JsmsSendService jsmsSendService;
    @Autowired
    private JsmsAccountService jsmsAccountService;
    @Autowired
    private JsmsAccessMologService jsmsAccessMologService;
    @Autowired
    private JsmsAccessService jsmsAccessService;
    @Autowired
    private JsmsSystemErrorDescService jsmsSystemErrorDescService;
    @Autowired
    private JsmsTimerSendTaskService jsmsTimerSendTaskService;
    @Autowired
    private JsmsSubmitProgressService jsmsSubmitProgressService;

    @Override
    public ResultVO importMobile(CommonsMultipartFile file) {
        String fileName = file.getOriginalFilename();
        logger.debug("access importMobile parmas[fileName={}]", fileName);

        if (file.getSize() > M5_SIZE) {
            return ResultVO.failure("您选择的文件大于5M,请将Excel拆分后重新导入");
        }
        logger.debug("导入号码的文件类型 ----> {}", file.getContentType());
        String path = new StringBuilder(ConfigUtils.temp_file_dir).append(UUID.randomUUID()).append(fileName).toString();
        FileUtils.upload2(path, fileName, file);
        List<Map> tempList = null;
        try {
            ImportParams importParams = new ImportParams();
            importParams.setHeadRows(0);
            tempList = ExcelImportUtil.importExcel(new File(path), Map.class, importParams);
            logger.debug("号码Excel 读取完成 , 删除文件 ----------> {}", path);
            FileUtils.delete(path);
        } catch (Exception e) {
            logger.debug("号码Excel 读取失败 , 删除文件 ----------> {}", path);
            FileUtils.delete(path);
            logger.error("解析excel失败：filePath=" + path, e);
            return ResultVO.failure("导入文件格式错误，目前只支持Excel导入，请使用模板");

        }
        logger.debug("号码Excel 读取完成  ----------> 开始解析");
        if (tempList == null) {
            logger.error("解析excel失败：filePath=" + path);
            return ResultVO.failure("导入文件格式错误，目前只支持Excel导入，请使用模板");

        }
        if (tempList.size() > Integer.parseInt(ConfigUtils.excel_max_import_num)) {
            logger.error("excel 数量限制超过限制：excel_max_import_num = {}, 实际导入数 = {}", ConfigUtils.excel_max_import_num,
                    tempList.size());
            return ResultVO.failure("Excel文件导入数据超过限制<br/>不能超过" + ConfigUtils.excel_max_import_num);

        }
        int errorMobileCount = 0;

        Set<String> phoneSet = new TreeSet();
        String phone = null;
        for (Map map : tempList) {
            for (Object object : map.values()) {
                if (object == null) {
                    continue;
                }
                if (object instanceof Double) {
                    BigDecimal bigDecimal = new BigDecimal((double) object);
                    phone = bigDecimal.toString();
                    if (RegexUtils.isMobile(phone) || RegexUtils.isOverSeaMobile(phone)) {
                        phoneSet.add(phone);
                    } else {
                        if (StringUtils.isNoneBlank(phone)) {
                            ++errorMobileCount;
                        }
                    }
                } else if (object instanceof String) {
                    if (RegexUtils.isMobile((String) object) || RegexUtils.isOverSeaMobile((String) object)) {
                        phoneSet.add((String) object);
                    } else {
                        if (StringUtils.isNoneBlank((String) object)) {
                            ++errorMobileCount;
                        }
                    }
                } else {
                    try {
                        phone = String.valueOf(object);
                        if (RegexUtils.isMobile(phone) || RegexUtils.isOverSeaMobile(phone)) {
                            phoneSet.add(phone);
                        } else {
                            if (StringUtils.isNoneBlank(phone)) {
                                ++errorMobileCount;
                            }
                        }
                    } catch (Exception e) {
                        logger.error("导入号码: {} 无法装换", object);
                        ++errorMobileCount;
                    }
                }
            }
        }

        Map result = new HashMap();
        result.put("mobileList", phoneSet);
        result.put("errorMobileCount", errorMobileCount);
        result.put("duplicateMobileCount", tempList.size() - phoneSet.size() - errorMobileCount);
        return ResultVO.successDefault(result);

    }

    @Override
    public ResultVO oemSmsSend(JsmsAccessSms jsmsAccessSms, Integer agentId) {
        JsmsAccount jsmsAccount = jsmsAccountService.getByClientId(jsmsAccessSms.getClientid());
        if (jsmsAccount == null) {
            return ResultVO.failure("账号信息不存在");
        } else if (jsmsAccount.getAgentId() == null || !jsmsAccount.getAgentId().equals(agentId)) {
            return ResultVO.failure("当前账号归属异常，请刷新后再试...");
        }
        String password = SecurityUtils.encryptMD5(jsmsAccount.getPassword());

        ResultVO resultVO = jsmsSendService.oemSmsSend(jsmsAccessSms.getClientid(), password, jsmsAccessSms.getMobile(),
                jsmsAccessSms.getSmstype(), jsmsAccessSms.getContent(), ConfigUtils.smsp_access_url_json);
        return resultVO;
    }

    @Override
    @Qualifier(value = "accessSlave")
    public JsmsPage queryMoList(JsmsPage jsmsPage) {
        String dateSuffix = (String) jsmsPage.getParams().get("dateSuffix");
        String clientid = (String) jsmsPage.getParams().get("clientid");
        if (StringUtils.isBlank(clientid)) {
            return jsmsPage;
        }
        // 查询
        jsmsAccessMologService.queryList(jsmsPage, dateSuffix);
        if (jsmsPage.getData() != null) {
            supplyRowNum(jsmsPage);
        }
        return jsmsPage;
    }

    @Override
    public JsmsPage querySubmitProgressList(JsmsPage jsmsPage) {

        Map params = jsmsPage.getParams();
        String agentId = (String) params.get("agentId");

        Set<String> clientIds = new HashSet<>();
        if (StringUtils.isBlank(String.valueOf(params.get("clientId")))) {
            if (String.valueOf(agentId) != null) {
                List<JsmsAccount> jsmsAcco = jsmsAccountService.findListForReturnQuantity(Integer.parseInt(agentId));
                if (jsmsAcco.size() > 0) {
                    for (int i = 0; i < jsmsAcco.size(); i++) {
                        clientIds.add(jsmsAcco.get(i).getClientid());
                    }
                } else {
                    clientIds.add("-1");
                }
            }
        }
        params.put("clientIds", clientIds);
        params.put("serviceType", ServiceTypeEnum.立即发送.getValue());
        jsmsPage.setOrderByClause(" create_time DESC,id DESC ");

        // 查询
        jsmsPage = jsmsSubmitProgressService.queryPageList(jsmsPage, WebId.OEM代理商平台, agentId);

        return jsmsPage;
    }

    @Override
    public JsmsPage querySendRecordList(JsmsPage jsmsPage) {

        Map params = jsmsPage.getParams();
        String beginDate = (String) params.get("beginDate");
        String clientid = (String) params.get("clientid");
        String states = (String) params.get("stateList");
        if (StringUtils.isNotBlank(states)) {
            String[] stateArr = StringUtils.split(states, ",");
            List<String> stateList = Arrays.asList(stateArr);
            params.put("stateList", stateList);
        } else {
            params.remove("stateList");
        }
        JsmsAccount jsmsAccount = jsmsAccountService.getByClientId(clientid);
        if (jsmsAccount == null) {
            logger.error("根据clientid查询客户信息错误----------", clientid);
            return jsmsPage;
        }

        String suffixDate = StringUtils.replace(beginDate, "-", "").substring(0, 8);
        jsmsAccessService.queryOneDayList(jsmsAccount.getIdentify().toString(), suffixDate, jsmsPage);
        supplyAccessRowNum(jsmsPage);
        return jsmsPage;
    }

    @Override
//	@Transactional("message_master")
    public JsmsPage smsTimerSendQuery(JsmsPage jsmsPage, WebId webId, String agengId) {
        return jsmsTimerSendTaskService.queryPageList(jsmsPage, webId, agengId);
    }

    @Override
    public ResultVO oemSmsTimSend4BigFile(JsmsAccessTimerSmsDTO jsmsAccessTimerSmsDTO, Integer agentId) {
        JsmsAccount jsmsAccount = jsmsAccountService.getByClientId(jsmsAccessTimerSmsDTO.getClientId());
        if (jsmsAccount == null) {
            return ResultVO.failure("请选择子账户");
        } else if (jsmsAccount.getAgentId() == null || !jsmsAccount.getAgentId().equals(agentId)) {
            return ResultVO.failure("当前账号归属异常，请刷新后再试...");
        }
        if (!SmsSendFileType.号码池.getValue().equals(jsmsAccessTimerSmsDTO.getFileType())) {
            jsmsAccessTimerSmsDTO.setImportFilePath(Des3Utils.decodeDes3(jsmsAccessTimerSmsDTO.getImportFilePath()));
        }

        String password = SecurityUtils.encryptMD5(jsmsAccount.getPassword());
        ResultVO resultVO = jsmsSendService.oemSmsTimSend(jsmsAccessTimerSmsDTO,password, getCurrentTomcatDataDir(), ConfigUtils.smsp_access_tim_url ,WebId.OEM代理商平台,agentId.toString());
        return resultVO;
    }

    @Override
    @Transactional("message_master")
    public ResultVO oemSmsTimSend(JsmsAccessTimerSms jsmsAccessTimerSms, String taskId, Integer agentId, Integer chargeNumTotal, String submitFlag) {
        boolean bool = false;
        JsmsAccount jsmsAccount = jsmsAccountService.getByClientId(jsmsAccessTimerSms.getClientid());
        if (jsmsAccount == null) {
            return ResultVO.failure("账号信息不存在");
        } else if (jsmsAccount.getAgentId() == null || !jsmsAccount.getAgentId().equals(agentId)) {
            return ResultVO.failure("当前账号归属异常，请刷新后再试...");
        }
        if (com.jsmsframework.common.util.StringUtils.isNotBlank(submitFlag)) {
            if ("1".equals(submitFlag)) {
                bool = false;
            } else if ("0".equals(submitFlag)) {
                bool = true;
            }
        } else {
            bool = true;
        }
        String password = SecurityUtils.encryptMD5(jsmsAccount.getPassword());
        jsmsAccessTimerSms.setPassword(password);
        jsmsAccessTimerSms.setSubmittype("1");
        jsmsAccessTimerSms.setMobilelist(DatatypeConverter.printBase64Binary(GZIPUtil.gzip(jsmsAccessTimerSms.getMobilelist(), GZIPUtil.UTF_8)));
        ResultVO resultVO = jsmsSendService.oemSmsTimSend(jsmsAccessTimerSms, taskId, ConfigUtils.smsp_access_tim_url, chargeNumTotal, bool);
        return resultVO;
    }

    @Override
    public ResultVO oemSmsSend(JsmsAccessSmsDTO jsmsAccessSmsDTO, Integer agentId) {
        JsmsAccount jsmsAccount = jsmsAccountService.getByClientId(jsmsAccessSmsDTO.getClientId());
        if (jsmsAccount == null) {
            return ResultVO.failure("请选择子账户");
        } else if (jsmsAccount.getAgentId() == null || !jsmsAccount.getAgentId().equals(agentId)) {
            return ResultVO.failure("当前账号归属异常，请刷新后再试...");
        }
        if (!SmsSendFileType.号码池.getValue().equals(jsmsAccessSmsDTO.getFileType())) {
            jsmsAccessSmsDTO.setImportFilePath(Des3Utils.decodeDes3(jsmsAccessSmsDTO.getImportFilePath()));
        }
        ResultVO resultVO = jsmsSendService.oemSmsSend(jsmsAccessSmsDTO, getCurrentTomcatDataDir(), WebId.OEM代理商平台, String.valueOf(agentId));
        return resultVO;
    }

    private String getCurrentTomcatDataDir() {
        String currentTomcatDataDir;
        if (ConfigUtils.current_tomcat_data_dir.endsWith("/")) {
            currentTomcatDataDir = ConfigUtils.current_tomcat_data_dir.substring(0, ConfigUtils.current_tomcat_data_dir.lastIndexOf("/"));
        } else if (ConfigUtils.current_tomcat_data_dir.endsWith("\\")) {
            currentTomcatDataDir = ConfigUtils.current_tomcat_data_dir.substring(0, ConfigUtils.current_tomcat_data_dir.lastIndexOf("\\"));
        } else {
            currentTomcatDataDir = ConfigUtils.current_tomcat_data_dir;
        }
        return currentTomcatDataDir;
    }

    /**
     * 补充rowNum
     *
     * @param jsmsPage
     * @return jsmsPage
     */
    private JsmsPage supplyRowNum(JsmsPage jsmsPage) {

        List<JsmsAccessMolog> origin = jsmsPage.getData();
        List<AccessMologDTO> result = new ArrayList<>(jsmsPage.getRows());
        int rowNum = jsmsPage.getRows() * (jsmsPage.getPage() - 1);
        for (JsmsAccessMolog jsmsAccessMolog : origin) {
            AccessMologDTO temp = new AccessMologDTO();
            BeanUtils.copyProperties(jsmsAccessMolog, temp);
            temp.setRowNum(++rowNum);
            result.add(temp);
        }
        jsmsPage.setData(result);
        return jsmsPage;
    }

    /**
     * 补充rowNum
     *
     * @param jsmsPage
     * @return jsmsPage
     */
    private JsmsPage supplyAccessRowNum(JsmsPage jsmsPage) {
        // 查询所有错误码
        List<JsmsSystemErrorDesc> errorDescList = jsmsSystemErrorDescService.queryAllList(null);
        Map<String, String> errorDescMap = new HashMap<>();
        StringBuilder tempSb = new StringBuilder();
        for (JsmsSystemErrorDesc systemError : errorDescList) {
            // 错误码客户端显示格式 “错误码-错误原因”
            tempSb.append(systemError.getSyscode()).append("-").append(systemError.getClientSideNote());
            errorDescMap.put(systemError.getSyscode(), tempSb.toString());
            tempSb.delete(0, tempSb.length());
        }
        List<JsmsAccess> origin = jsmsPage.getData();
        List<AccessDTO> result = new ArrayList<>(jsmsPage.getRows());
        int rowNum = jsmsPage.getRows() * (jsmsPage.getPage() - 1);
        for (JsmsAccess jsmsAccess : origin) {
            AccessDTO temp = new AccessDTO();
            BeanUtils.copyProperties(jsmsAccess, temp);
            temp.setRowNum(++rowNum);
            if (StringUtils.isNotBlank(temp.getErrorcode())) {
                String tempDesc = errorDescMap.get(StringUtils.substring(temp.getErrorcode(), 0, 7));
                temp.setErrorcodeStr(tempDesc);
            }
            result.add(temp);
        }
        jsmsPage.setData(result);
        return jsmsPage;
    }

}
