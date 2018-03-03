/**    
 * @Title: CommonSeqService.java  
 * @Package: com.ucpaas.sms.service  
 * @Description: TODO
 * @author: Niu.T    
 * @date: 2016年9月6日 下午4:19:49  
 * @version: V1.0    
 */
package com.ucpaas.sms.service.common;

/**  
 * @ClassName: CommonSeqService  
 * @Description: 提供公共的clientid生成序列
 * @author: Niu.T 
 * @date: 2016年9月6日 下午4:19:49  
 */
public interface CommonSeqService {
	
	/**
	 * @Description: 获取公用的clientid序列(如果序列表中有未使用的数据则直接取,否则添加10,000条数据)
	 * @author: Niu.T 
	 * @date: 2016年9月6日 下午4:21:00  
	 * @return: String
	 */
	public String getOrAddId();
	
	/**
	 * @Description: 修改clientid状态为1，表示已经使用
	 * @param clientId
	 */
	public boolean updateClientIdStatus(String clientId);
	
}
