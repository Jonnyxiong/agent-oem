/**    
 * @Title: CommonSeqServiceImp.java  
 * @Package: com.ucpaas.sms.service  
 * @Description: 公用id序列service
 * @author: Niu.T    
 * @date: 2016年9月6日 下午4:22:51  
 * @version: V1.0    
 */
package com.ucpaas.sms.service.common;

import com.google.common.collect.Lists;
import com.ucpaas.sms.mapper.message.ClientIdSequenceMapper;
import com.ucpaas.sms.model.ClientIdSequence;
import com.ucpaas.sms.util.NumConverUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName: CommonSeqServiceImp
 * @Description: 提供公共的clientid生成序列
 * @author: Niu.T
 * @date: 2016年9月6日 下午4:22:51
 */
@Service
@Transactional
public class CommonSeqServiceImpl implements CommonSeqService {

	@Autowired
	private ClientIdSequenceMapper clientIdSequenceMapper;

	/**
	 * @Description: 从clientid序列表中(按规则生成共6位,36位递增,首位为a-z,末位为0-9),获取当前可用clientid(默认去可用状态中的最小的序列)
	 * @author: Niu.T
	 * @date: 2016年9月3日 下午4:32:21
	 * @return: String
	 */
	@Override
	public synchronized String getOrAddId() {
		Map<String, Object> data = new HashMap<String, Object>();

		// 随机取出未使用的序列
		ClientIdSequence clientIdSequence = clientIdSequenceMapper.getUnusedRandom();

		// 生成帐号
		if (clientIdSequence == null) {
			String max = clientIdSequenceMapper.getMax();

			List<String> numbers = Lists.newLinkedList();

			String clientId = "azzzz9";// 默认 9zzzz是 值a0000小1的36进制数字,最后一个9时占位的
			// ,a0000(三十六进制) = 16796160(十进制)
			if (StringUtils.isNotBlank(max)) {
				clientId = max;
			}
			long id = NumConverUtil.converToDecimal(clientId.substring(0, clientId.length() - 1));
			for (int i = 1; i <= 1000; i++) {
				for (int j = 0; j < 10; j++) {
					numbers.add(NumConverUtil.converTo36HEX(id + i) + j);
				}
			}

			clientIdSequenceMapper.batchAdd(numbers);

			// 重新获取
			clientIdSequence = clientIdSequenceMapper.getUnusedRandom();
		}

		// 更新lock字段为1表示临时占用，更新lock_start_time表示开始占用时间，ucpaas-sms-task会检查占用超过30分钟的记录并修改lock为0
		clientIdSequenceMapper.lock(clientIdSequence.getClientId());
		return clientIdSequence.getClientId();
	}

	/**
	 * @Description: 修改clientid状态为1，表示已经使用
	 * @param clientId
	 */
	@Override
	public boolean updateClientIdStatus(String clientId) {
		boolean result = false;
		if (StringUtils.isNotBlank(clientId)) {
			result = clientIdSequenceMapper.updateStatus(clientId) > 0;
		}
		return result;
	}

}
