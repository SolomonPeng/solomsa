/**
 * 
 */
package org.solomsa.data.component;

import java.util.Date;
import java.util.Map;

import org.solomsa.core.util.CoreUtils;
import org.solomsa.data.domain.AuditLog;
import org.solomsa.data.event.DeletedRecord;
import org.solomsa.data.event.AuditDeletedEvent;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;


/**
 * @author song.peng
 * 删除记录的审计监听器
 */
@Component
public class AuditDeletedListener implements ApplicationListener<AuditDeletedEvent>,DisposableBean{

	@Autowired
	private AuditLogPersistence persistence;
	
	@Override
	public void destroy() throws Exception {
		persistence.stop();
	}

	@Override
	public void onApplicationEvent(AuditDeletedEvent event) {
		onAuditDeleted(event.getSource());		
	}
	
	/**
	 * 实现该方法，获取所需的数据变化实体，以便做进一步操作
	 * @param auditChanged
	 */
	protected void onAuditDeleted(DeletedRecord auditDeleted) {
		AuditLog log = new AuditLog();
		log.setBatchNo(auditDeleted.getBatchNo());
		log.setClassName(auditDeleted.getClassName());
		log.setDataId(auditDeleted.getDataId());
		log.setModifiedBy(auditDeleted.getModifiedBy());
		log.setNewValue("DELETED");
		log.setOldValue(getNullableJson(auditDeleted.getEntity()));
		log.setTableName(auditDeleted.getTableName());
		log.setAuditDate(new Date());
		persistence.append(log);
	}
	
	protected String getNullableJson(Object obj) {
		if(ObjectUtils.isEmpty(obj)) {
			return null;
		}
		Map<String,Object> map = CoreUtils.toMap(obj);
		map.remove("deleted");
		map.remove("new");
		return CoreUtils.toString(map);		
	}

}
