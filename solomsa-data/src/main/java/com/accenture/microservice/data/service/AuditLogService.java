/**
 * 
 */
package com.accenture.microservice.data.service;


import com.accenture.microservice.data.base.AbstractEntityService;
import com.accenture.microservice.data.domain.AuditLog;
import com.accenture.microservice.data.domain.QAuditLog;
import com.accenture.microservice.data.repository.AuditLogRepository;

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
