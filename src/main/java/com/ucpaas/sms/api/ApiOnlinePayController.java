package com.ucpaas.sms.api;


import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.jsmsframework.common.dto.JsmsPage;
import com.jsmsframework.common.dto.R;
import com.jsmsframework.common.enums.Code;
import com.jsmsframework.common.util.StringUtils;
import com.jsmsframework.finance.dto.JsmsOnlinePaymentDTO;
import com.jsmsframework.finance.exception.JsmsOnlinePaymentException;
import com.jsmsframework.finance.service.JsmsOnlinePaymentService;
import com.ucpaas.sms.service.payment.OnlinePaymentService;
import com.ucpaas.sms.util.web.AuthorityUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 在线支付管理
 * @author yeshiyuan
 * @create 2017/12/29
 */
@Api(value = "/onlinePay", description = "在线支付api")
@RestController
@RequestMapping("/api/onlinePay")
public class ApiOnlinePayController {

    private final static Logger logger = LoggerFactory.getLogger(ApiOnlinePayController.class);
    @Autowired
    private JsmsOnlinePaymentService jsmsOnlinePaymentService;

    @Autowired
    private OnlinePaymentService onlinePaymentService;

    private static final String IMAGETYPE = "JPEG";

    /**
     * @Description   创建一个待支付的在线支付订单
     * @author yeshiyuan
     * @date 2018/1/2 17:10
     * @param [request, paymentAmount, paymentMode]
     * @return com.jsmsframework.common.dto.R
     */
    @RequestMapping(value = "/createOrder",method= RequestMethod.POST)
    @ApiOperation(value = "创建一个待支付的在线支付订单",notes = "创建一个待支付的在线支付订单", response = R.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="paymentAmount",value = "充值金额",dataType = "bigDecimal",paramType = "query"),
        @ApiImplicitParam(name="paymentMode",value = "充值方式（0：支付宝，1：微信支付）",dataType = "int",paramType = "query"),
    })
    public R createOrder(HttpServletRequest request, @RequestParam(required = true) BigDecimal paymentAmount,
        @RequestParam(required = true) Integer paymentMode){
        try {
            String agentId = AuthorityUtils.getLoginAgentId(request);
            Long adminId = AuthorityUtils.getLoginUserId(request);
            //创建支付订单
            R order = onlinePaymentService.createOrder(paymentAmount, paymentMode, agentId, adminId);
            return order;
        }catch (JsmsOnlinePaymentException e) {
            logger.error("生成订单id出错:",e);
            return R.error(Code.OPT_ERR,e.getMessage());
        } catch (Exception e) {
            logger.error("生成订单id出错:",e);
            return R.error(Code.SYS_ERR,"生成订单id出错");
        }
    }

    /**
     * @Description  获取订单信息（epay地址）
     * @author yeshiyuan
     * @date 2018/1/3 10:31
     * @param [request, paymentId]
     * @return com.jsmsframework.common.dto.R
     */
    @PostMapping(value = "/getPaymentInfo")
    @ApiOperation(value = "获取订单信息（epay地址等）",notes = "获取订单信息（epay地址）",response = R.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "paymentId",value = "在线支付订单id",paramType = "query",dataType = "String")
    })
    public R getPaymentInfo(HttpServletRequest request,String paymentId){
        try {
            //获取订单信息
            R r = onlinePaymentService.getPaymentInfo(paymentId);
            return r;
        }catch (JsmsOnlinePaymentException e) {
            logger.error("获取订单信息出错:",e);
            return R.error(Code.OPT_ERR,e.getMessage());
        } catch (Exception e) {
            logger.error("获取订单信息出错:",e);
            return R.error(Code.SYS_ERR,"获取订单信息出错");
        }
    }


    /**
     * @Description 立即支付
     * @author yeshiyuan
     * @date 2018/1/2 17:10
     * @param [request, paymentId]
     * @return com.jsmsframework.common.dto.R
     */
    @PostMapping(value = "/paySubmit")
    @ApiOperation(value = "立即支付（适用于支付宝）",notes = "立即支付（订单状态变为支付已提交）",response = R.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "paymentId",value = "在线支付订单id",paramType = "query",dataType = "String")
    })
    public R paySubmit(HttpServletRequest request,String paymentId){
        try {
            Long adminId = AuthorityUtils.getLoginUserId(request);
            //立即支付
            R r = onlinePaymentService.paySubmit(paymentId, adminId);
            return r;
        }catch (JsmsOnlinePaymentException e) {
            logger.error("支付出错:",e);
            return R.error(Code.OPT_ERR,e.getMessage());
        } catch (Exception e) {
            logger.error("生成订单id出错:",e);
            return R.error(Code.SYS_ERR,"支付出错");
        }
    }

    /**
     * @Description  取消支付
     * @author yeshiyuan
     * @date 2018/1/3 10:10
     * @param [request, paymentId]
     * @return com.jsmsframework.common.dto.R
     */
    @PostMapping(value = "/cancelPay")
    @ApiOperation(value = "取消支付",notes = "取消支付",response = R.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "paymentId",value = "支付订单id",dataType = "String",paramType = "query")
    })
    public R cancelPay(HttpServletRequest request,String paymentId){
        try {
            Long adminId = AuthorityUtils.getLoginUserId(request);
            R r = onlinePaymentService.cancelPay(paymentId, adminId);
            return r;
        }catch (JsmsOnlinePaymentException e) {
            logger.error("取消支付异常:",e);
            return R.error(Code.OPT_ERR,e.getMessage());
        } catch (Exception e) {
            logger.error("取消支付异常:",e);
            return R.error(Code.SYS_ERR,"取消支付异常");
        }
    }

    /**
     * @Description   查询在线支付订单状态
     * @author yeshiyuan
     * @date 2018/1/3 14:10
     * @param [paymentId]
     * @return com.jsmsframework.common.dto.R
     */
    @GetMapping(value = "/getOnlinePaymentState")
    @ApiOperation(value = "查询在线支付订单状态",notes = "查询在线支付订单状态",httpMethod = "GET",response = R.class)
    @ApiImplicitParams({
            @ApiImplicitParam(value = "支付订单id",name = "paymentId",dataType = "String",paramType = "query")
    })
    public R getOnlinePaymentState(String paymentId) {
        try {
            R r = onlinePaymentService.getOnlinePaymentState(paymentId);
            return r;
        } catch (JsmsOnlinePaymentException e) {
            logger.error("查询支付订单状态异常:", e);
            return R.error(Code.OPT_ERR, e.getMessage());
        } catch (Exception e) {
            logger.error("查询支付订单状态异常:", e);
            return R.error(Code.SYS_ERR, "查询支付订单状态异常");
        }
    }
    /**
     * @Description  获取微信支付地址
     * @author yeshiyuan
     * @date 2018/1/3 15:12
     * @param [request, paymentId]
     * @return com.jsmsframework.common.dto.R
     */
    @PostMapping(value = "/getPaymentAddrForWeChat")
    @ApiOperation(value = "获取微信支付地址",notes = "获取微信支付地址",response = R.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "paymentId",value = "支付订单id",dataType = "String",paramType = "query")
    })
    public R getPaymentAddrForWeChat(HttpServletRequest request,String paymentId) {
        try {
            Long adminId = AuthorityUtils.getLoginUserId(request);
            R r = onlinePaymentService.getPaymentAddrForWeChat(paymentId, adminId);
            return r;
        } catch (JsmsOnlinePaymentException e) {
            logger.error("获取微信支付地址异常:", e);
            return R.error(Code.OPT_ERR, e.getMessage());
        } catch (Exception e) {
            logger.error("获取微信支付地址异常:", e);
            return R.error(Code.SYS_ERR, "获取微信支付地址异常");
        }
    }

    /**
     * @Title: getQRCodeImgForWeChat
     * @Description: 获取二维码图片
     * @param response
     * @param paymentAddr
     * @return: void
     */
    @GetMapping(value = "/getQRCodeImgForWeChat")
    @ApiOperation(value = "生成微信支付二维码",notes = "生成微信支付二维码",httpMethod = "GET")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "paymentAddr",value = "微信支付地址",dataType = "String",paramType = "query")
    })
    public void getQRCodeImgForWeChat(HttpServletResponse response, String paymentAddr) {
        if (paymentAddr == null || "".equals(paymentAddr)) {
            logger.error("支付地址不能为空");
            return;
        }
        ServletOutputStream stream = null;
        try {
            int size = 300;
            stream = response.getOutputStream();
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix m = writer.encode(paymentAddr, BarcodeFormat.QR_CODE, size, size);
            MatrixToImageWriter.writeToStream(m, IMAGETYPE, stream);
        } catch (IOException | WriterException e) {
            logger.error("二维码生成失败,value=" + paymentAddr, e);
        } finally {
            if (stream != null) {
                try {
                    stream.flush();
                    stream.close();
                } catch (IOException e) {
                    logger.error("二维码输出流关闭失败", e);
                }
            }
        }
    }

    /**
     * @Description  支付订单列表数据加载
     * @author yeshiyuan
     * @date 2018/1/3 17:53
     * @param [request, params]
     * @return com.jsmsframework.common.dto.R
     */
    @RequestMapping(value = "/queryPayOrder")
    @ApiOperation(value = "支付订单列表数据加载",notes = "支付订单列表数据加载",response = R.class,httpMethod = "POST")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "searchText",value = "输入框搜索内容",dataType = "String",paramType = "query"),
            @ApiImplicitParam(name = "startPayTime",value = "支付起始时间",dataType = "String",paramType = "query"),
            @ApiImplicitParam(name = "endPayTime",value = "支付截止时间",dataType = "String",paramType = "query"),
            @ApiImplicitParam(name = "paymentMode",value = "支付方式（0：支付宝，1：微信）",dataType = "String",paramType = "query"),
            @ApiImplicitParam(name = "paymentState",value = "支付状态（0：未支付；1：支付已提交；2：支付成功；3：支付失败；4：支付已取消）",dataType = "String",paramType = "query"),
    })
    public R queryPayOrder(HttpServletRequest request, JsmsPage<JsmsOnlinePaymentDTO> jsmsPage, String searchText,
                           String startPayTime, String endPayTime, Integer paymentMode, Integer paymentState) {
        try {
            String agentId = AuthorityUtils.getLoginAgentId(request);
            if (jsmsPage == null) {
                jsmsPage = new JsmsPage<JsmsOnlinePaymentDTO>();
            }
            Map<String, Object> map = new HashMap<>();
            map.put("agentId", agentId);
            if (StringUtils.isNotBlank(startPayTime)) {
                map.put("startPayTime", startPayTime);
            }
            if (StringUtils.isNotBlank(startPayTime)) {
                map.put("endPayTime", endPayTime);
            }
            map.put("paymentMode", paymentMode);
            map.put("searchText", searchText);
            //查询待支付和已取消的订单状态时做转化
            map.put("paymentState", paymentState);
            jsmsPage.getParams().putAll(map);
            if (StringUtils.isEmpty(jsmsPage.getOrderByClause())) {
                jsmsPage.setOrderByClause(" create_time desc ");
            }
            jsmsPage = onlinePaymentService.queryPayOrder(jsmsPage,paymentState);
            return R.ok("加载成功", jsmsPage);
        } catch (JsmsOnlinePaymentException e) {
            logger.error("支付订单列表数据加载异常:", e);
            return R.error(Code.OPT_ERR, e.getMessage());
        } catch (Exception e) {
            logger.error("支付订单列表数据加载:", e);
            return R.error(Code.SYS_ERR, "支付订单列表数据加载");
        }
    }


}
