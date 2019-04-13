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
package org.solomsa.data.event;

import java.io.Serializable;

import lombok.Data;


/**
 * <p>
 * <b>DeletedRecord</b> 数据删除实体
 * </p>
 *
 * @author song.peng
 */
@Data
public class AuditDeleted implements Serializable {

	private static final long serialVersionUID = 1L;

	/** 变化的表名 */
	private String tableName;
	
	/** 变化的类名 */
	private String className;

	/** 变化的数据ID */
	private String dataId;

	/**变化批次*/
	private String batchNo;
	
	/**实体内容*/
	private String content;

	/** 变更人 */
	private String modifiedBy;

	/** 变化的实体 */
	private Object entity;
	
}
