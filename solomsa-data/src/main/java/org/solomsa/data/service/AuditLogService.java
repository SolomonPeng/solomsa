/**
 * 
 */
package org.solomsa.data.service;


import org.solomsa.data.base.AbstractEntityService;
import org.solomsa.data.domain.AuditLog;
import org.solomsa.data.domain.QAuditLog;
import org.solomsa.data.repository.AuditLogRepository;

/**
 * @author song.peng
 *
 */
public class AuditLogService extends AbstractEntityService<AuditLogRepository, AuditLog, String> {


	@Override
	protected void initParamConfigs() {
		QAuditLog q = (QAuditLog) this.getQEntity();
		this.addParamConfig("tableName",x->q.tableName.eq(x.toString()));
		this.addParamConfig("classNameContains",x->q.className.like("%"+x.toString()+"%"));
	}

}
