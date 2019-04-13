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
package org.solomsa.data.domain;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.GenericGenerator;
import org.solomsa.data.IdGenerator;
import org.springframework.data.domain.Persistable;

import lombok.Data;


/**
 * <p> <b>AuditLog</b> 是变化日志保存表. </p>
 * 
 * @author song.peng
 */

@Entity
@Table(name = "AUDIT_LOG")
@Data
public class AuditLog implements Persistable<String> {

	private static final long serialVersionUID = 1L;

	@Id
	@GenericGenerator(name = IdGenerator.NAME, strategy = IdGenerator.CLASS)
	@GeneratedValue(generator = IdGenerator.NAME)
	@Column(name = "ID",unique=true, nullable=false, length=64)
	private String id;

	/** 变化的表名 */
	@Column(name = "TABLE_NAME", length = 150)
	private String tableName;

	/** 变化的字段名 */
	@Column(name = "CLASS_NAME", length = 150)
	private String className;

	/** 变化的数据ID */
	@Column(name = "DATA_ID", length = 100)
	private String dataId;

	/** 变化前的数据 */
	@Lob
	@Column(name = "OLD_VALUE")
	private String oldValue;

	/** 变化后的数据 */
	@Lob
	@Column(name = "NEW_VALUE")
	private String newValue;

	/** 变化批次 */
	@Column(name = "BATCH_NO", length = 100)
	private String batchNo;

	/** 变更人 */
	@Column(name = "MODIFIED_BY", length = 50)
	private String modifiedBy;
	
	@Column(name = "AUDIT_DATE")
	@Temporal(TemporalType.TIMESTAMP)
	private Date auditDate;	

	@Override
	public boolean isNew() {
		return id == null;
	}

}
