/**
 * Copyright 2010-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.accenture.microservice.data.component;


import java.util.Date;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.accenture.microservice.data.domain.AuditLog;
import com.accenture.microservice.data.event.AuditChanged;
import com.accenture.microservice.data.event.AuditChangedEvent;


/**
 * @author song.peng
 * 修改记录的审计监听器
 */
@Component
public class AuditChangedListener implements ApplicationListener<AuditChangedEvent>,DisposableBean {

	@Autowired
	private AuditLogPersistence persistence;

	@Override
	public void onApplicationEvent(AuditChangedEvent auditChangedEvent) {
		onAuditChanged(auditChangedEvent.getSource());
	}
	
	/**
	 * 实现该方法，获取所需的数据变化实体，以便做进一步操作
	 * @param auditChanged
	 */
	protected void onAuditChanged(AuditChanged auditChanged) {
		AuditLog log = new AuditLog();
		log.setBatchNo(auditChanged.getBatchNo());
		log.setClassName(auditChanged.getClassName());
		log.setDataId(auditChanged.getDataId());
		log.setModifiedBy(auditChanged.getModifiedBy());
		log.setNewValue(getNullable(auditChanged.getNewValue()));
		log.setOldValue(getNullable(auditChanged.getOldValue()));
		log.setTableName(auditChanged.getTableName());
		log.setAuditDate(new Date());
		persistence.append(log);
	}

	protected String getNullable(Object obj) {
		return obj == null ? null : obj.toString();
	}

	@Override
	public void destroy() throws Exception {
		persistence.stop();
	}
}
