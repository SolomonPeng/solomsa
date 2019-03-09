/**
 * 
 */
package com.accenture.microservice.data.service;


import com.accenture.microservice.data.base.AbstractEntityService;
import com.accenture.microservice.data.domain.AuditLog;
import com.accenture.microservice.data.domain.QAuditLog;
import com.accenture.microservice.data.repository.AuditLogRepository;
import com.querydsl.core.types.dsl.SimpleExpression;

/**
 * @author song.peng
 *
 */
public class AuditLogService extends AbstractEntityService<AuditLogRepository, AuditLog, String> {

	
	@Override
	protected SimpleExpression<String> idExpression() {
		return QAuditLog.auditLog.id;
	}

	@Override
	protected void initParamConfigs() {
		QAuditLog q = QAuditLog.auditLog;
		this.addParamConfig("tableName",x->q.tableName.eq(x.toString()));
		this.addParamConfig("className",x->q.className.like("%"+x.toString()+"%"));
	}

}
