package com.ucpaas.sms.service.sms;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jsmsframework.common.enums.ProductType;
import com.jsmsframework.product.entity.JsmsOemAgentProduct;
import com.jsmsframework.product.entity.JsmsOemProductInfo;
import com.jsmsframework.product.service.JsmsOemAgentProductService;
import com.jsmsframework.user.entity.JsmsOemDataConfig;
import com.jsmsframework.user.service.JsmsOemDataConfigService;
import com.ucpaas.sms.common.entity.PageContainer;
import com.ucpaas.sms.common.util.Collections3;
import com.ucpaas.sms.mapper.message.AgentInfoMapper;
import com.ucpaas.sms.mapper.message.ProductMapper;
import com.ucpaas.sms.model.AgentInfo;
import com.ucpaas.sms.service.util.AgentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);

	@Autowired
	private ProductMapper productMapper;

	@Autowired
	private AgentInfoMapper agentInfoMapper;

	@Autowired
	private JsmsOemDataConfigService jsmsOemDataConfigService;

	@Autowired
	private JsmsOemAgentProductService jsmsOemAgentProductService;

	@Override
	public PageContainer query(Map params) {
		Integer totalCount = productMapper.queryCount(params);
		if (totalCount == null)
			totalCount = 0;

		PageContainer p = new PageContainer();
		p.setTotalCount(totalCount);
		List<Map<String, Object>> list = null;

		AgentUtils.buildPageLimitParams(params, totalCount, p);

		list = productMapper.query(params);
		if (list == null) {
			list = Lists.newArrayList();
		}
		p.setList(list);
		return p;
	}

	@Override
	public String getTheMostNumForMinute(String orderIdPre) {
		Map<String, Object> sqlParams = new HashMap<>();
		sqlParams.put("orderIdPre", orderIdPre);
		return productMapper.getMostNum(sqlParams);
	}

	@Transactional
	@Override
	public void fixOemAgentProduct() {

		// 查询所有的代理商
		List<AgentInfo> agentInfos = agentInfoMapper.findList();
		if (Collections3.isEmpty(agentInfos)) {
			return;
		}

		// 查询所有的已上架的OEM产品
		List<JsmsOemProductInfo> productInfos = productMapper.findOemProductInfo();
		if (Collections3.isEmpty(productInfos)) {
			return;
		}

		// 循环所有的代理商
		JsmsOemDataConfig dataConfig;
		for (AgentInfo agentInfo : agentInfos) {
			logger.debug("-------【修复代理商的产品】：开始修复代理商 代理商ID={} 代理商名称={}", agentInfo.getAgentId(), agentInfo.getAgentName());

			// 查询OEM代理商的产品表是否存在该代理商的数据，存在跳过，不存在继续处理
			Map<String, Object> params = Maps.newHashMap();
			params.put("agentId", agentInfo.getAgentId());
			int count = jsmsOemAgentProductService.count(params);
			if (count > 0) {
				logger.debug("-------【修复代理商的产品】：代理商已被修复过");
				continue;
			}


			// 查询代理商的折扣率
			dataConfig = jsmsOemDataConfigService.getByAgentId(Integer.parseInt(agentInfo.getAgentId()));

			if (dataConfig == null){
				dataConfig = new JsmsOemDataConfig();
				dataConfig.setGjSmsDiscount(BigDecimal.ONE);
				dataConfig.setHySmsDiscount(BigDecimal.ONE);
				dataConfig.setYxSmsDiscount(BigDecimal.ONE);
			}

			// 插入数据
			JsmsOemAgentProduct jsmsOemAgentProduct;
			for (JsmsOemProductInfo productInfo : productInfos) {
				if (productInfo.getProductType() != ProductType.营销.getValue()
						&& productInfo.getProductType() != ProductType.国际.getValue()
						&& productInfo.getProductType() != ProductType.行业.getValue()) {
					continue;
				}

				jsmsOemAgentProduct = new JsmsOemAgentProduct();
				jsmsOemAgentProduct.setAgentId(Integer.parseInt(agentInfo.getAgentId()));
				jsmsOemAgentProduct.setProductId(productInfo.getProductId());
				jsmsOemAgentProduct.setAdminId(0L);
				jsmsOemAgentProduct.setCreateTime(Calendar.getInstance().getTime());
				jsmsOemAgentProduct.setUpdateTime(jsmsOemAgentProduct.getCreateTime());

				if (productInfo.getProductType() == ProductType.国际.getValue()) {
					// 取折扣率
					jsmsOemAgentProduct.setGjSmsDiscount(dataConfig.getGjSmsDiscount());
					if (jsmsOemAgentProduct.getGjSmsDiscount() == null
							|| jsmsOemAgentProduct.getGjSmsDiscount().compareTo(BigDecimal.ZERO) <= 0) {
						jsmsOemAgentProduct.setGjSmsDiscount(BigDecimal.ONE);
					}

				} else {
					// 算出折后价
					BigDecimal unitPrice = productInfo.getUnitPrice();
					if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
						unitPrice = BigDecimal.ZERO;
					}

					BigDecimal discount = productInfo.getProductType() == ProductType.行业.getValue()
							? dataConfig.getHySmsDiscount() : dataConfig.getYxSmsDiscount();
					if (discount == null || discount.compareTo(BigDecimal.ZERO) <= 0) {
						discount = BigDecimal.ONE;
					}

					BigDecimal discount_price = unitPrice.multiply(discount);
					jsmsOemAgentProduct.setDiscountPrice(discount_price);
				}

				logger.debug("-------【修复代理商的产品】：开始修复代理商 代理商产品{}", JSON.toJSONString(jsmsOemAgentProduct));

				jsmsOemAgentProductService.insert(jsmsOemAgentProduct);
			}
		}
	}
}
