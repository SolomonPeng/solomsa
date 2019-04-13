/**
 * 
 */
package org.solomsa.data.component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.Table;

import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.solomsa.app.context.AppContext;
import org.solomsa.core.util.CoreUtils;
import org.solomsa.data.annotation.AuditChange;
import org.solomsa.data.annotation.AuditDelete;
import org.solomsa.data.base.AbstractAuditable;
import org.solomsa.data.event.AuditChanged;
import org.solomsa.data.event.AuditChangedEvent;
import org.solomsa.data.event.AuditDeleted;
import org.solomsa.data.event.AuditDeletedEvent;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author song.peng
 *
 */
@Slf4j
public class AuditedEventListener implements PostUpdateEventListener, PostDeleteEventListener, PreUpdateEventListener {

	private static final long serialVersionUID = 1L;
 
	private static Map<String, String> tableNameMap = new ConcurrentHashMap<String, String>();

	//private static Map<String, String> columnNameMap = new ConcurrentHashMap<String, String>();

	//private static Map<String, Boolean> entityAuditedMap = new ConcurrentHashMap<String, Boolean>();
	
	private static Map<String, Boolean> entityAuditChangeMap = new ConcurrentHashMap<String, Boolean>();
	
	private static Map<String, Boolean> entityAuditDeleteMap = new ConcurrentHashMap<String, Boolean>();

	//private static Map<String, Boolean> propertyAuditedMap = new ConcurrentHashMap<String, Boolean>();
	
	//@Autowired
	//private org.springframework.context.ApplicationContext context;
	
	//private final Logger log = (Logger) LoggerFactory.getLogger(getClass());

	@Override
	public boolean onPreUpdate(PreUpdateEvent event) {
		if (event.getEntity() instanceof Persistable) {
			Persistable<?> entity = (Persistable<?>) event.getEntity();
			Object id = entity.getId();
			if (null != id) {
				// 表名
				//String tableName = getTableName(entity);
				// 实体ID
				//String dataId = id.toString();
				// 本次变化批号
				//String batchNo = UUID.randomUUID().toString().replaceAll("-", "");
				
				/*if(entity instanceof AbstractAuditable) {
					batchNo = ((AbstractAuditable)entity).getTransactionId();
				}*/

				String[] propertyNames = event.getPersister().getPropertyNames();
				boolean isChanged = false;
				for (int i = 0; i < propertyNames.length; i++) {
					String propertyName = propertyNames[i];
					if(isSpringAuditedField(event.getEntity(), propertyName)) {
						continue;
					}
					
					Object oldValue = event.getOldState()[i];
					Object newValue = event.getState()[i];

					// 判断是否需要记录日志并且数据发生变化
					if (isChanged(oldValue, newValue)) {
						// 获取列名
						//String columnName = getColumnName(entity, propertyName);
						isChanged = true;
						break;
					}				
				}
				if(isChanged) {					
					return false;
				}else {
					log.debug("No change occur this time, break");
					return true;
				}
			}
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.hibernate.event.PostUpdateEventListener#onPostUpdate(org.hibernate.event.PostUpdateEvent)
	 */
	@Override
	public void onPostUpdate(PostUpdateEvent event) {
		// 判断实体是否需要记录日志
		if (isEntityAuditChange(event.getEntity()) && ClassUtils.isPresent("com.accenture.microservice.app.context.AppContext", getClass().getClassLoader())) {
			if (event.getEntity() instanceof Persistable) {
				Persistable<?> entity = (Persistable<?>) event.getEntity();
				Object id = entity.getId();
				if (null != id) {
					// 表名
					String tableName = getTableName(entity);
					// 实体ID
					String dataId = id.toString();
					// 本次变化批号
					String batchNo = null;
					if(entity instanceof AbstractAuditable) {
						batchNo = ((AbstractAuditable<?>)entity).getTransactionId();
					}else {
						batchNo = UUID.randomUUID().toString().replaceAll("-", "");
					}
					
					//String oldEntity = "";
					//String newEntity = "";
					boolean changed = false;
					String[] propertyNames = event.getPersister().getPropertyNames();
					Map<String, Object> oldEntityMap = new HashMap<String, Object>();
					Map<String, Object> newEntityMap = new HashMap<String, Object>();
					for (int i = 0; i < propertyNames.length; i++) {
						String propertyName = propertyNames[i];
						if(isSpringAuditedField(event.getEntity(), propertyName)) {
							continue;
						}
						Object oldValue = event.getOldState()[i];
						Object newValue = event.getState()[i];
						oldEntityMap.put(propertyName, oldValue);
						newEntityMap.put(propertyName, newValue);
						//oldEntity = oldEntity + ","+propertyName+":"+(oldValue==null?"null":oldValue.toString());
						//newEntity = newEntity + ","+propertyName+":"+(newValue==null?"null":newValue.toString());

						// 判断是否需要记录日志并且数据发生变化
						//if (isPropertyAudited(event.getEntity(), propertyName) && isChanged(oldValue, newValue)) {
						if (isChanged(oldValue, newValue)) {							
							changed = true;
							break;
						}
					}
					if(changed) {
						//oldEntity = oldEntity.substring(1);
						//newEntity = newEntity.substring(1);
						AuditChanged chg = new AuditChanged();
						Object oldEntity = CoreUtils.toString(oldEntityMap);
						Object newEntity = CoreUtils.toString(newEntityMap);						
						chg.setBatchNo(batchNo);
						chg.setTableName(tableName);
						chg.setClassName(event.getEntity().getClass().toString().replace("class ", ""));
						chg.setDataId(dataId);
						chg.setOldValue(oldEntity);
						chg.setNewValue(newEntity);
						chg.setEntity(event.getEntity());
						chg.setModifiedBy(AppContext.getCurrentUser());						
						AppContext.publishApplicationEvent(new AuditChangedEvent(chg));
					}
				}
			}
		}
	}

	@Override
	public void onPostDelete(PostDeleteEvent event) {
		// 判断实体是否需要记录日志
		if (isEntityAuditDelete(event.getEntity()) && ClassUtils.isPresent("com.accenture.microservice.app.context.AppContext", getClass().getClassLoader())) {
			if (event.getEntity() instanceof Persistable) {
				Persistable<?> entity = (Persistable<?>) event.getEntity();
				Object id = entity.getId();
				if (null != id) {
					// 表名
					String tableName = getTableName(entity);
					// 实体ID
					String dataId = id.toString();
					// 本次变化批号
					String batchNo = null;
					if(entity instanceof AbstractAuditable) {
						batchNo = ((AbstractAuditable<?>)entity).getTransactionId();
					}else {
						batchNo = UUID.randomUUID().toString().replaceAll("-", "");
					}
					/*String[] propertyNames = event.getPersister().getPropertyNames();
					String content = "";
					for (int i = 0; i < propertyNames.length; i++) {
						String propertyName = propertyNames[i];
					}*/
					AuditDeleted chg = new AuditDeleted();
					chg.setBatchNo(batchNo);
					chg.setTableName(tableName);
					chg.setClassName(event.getEntity().getClass().toString().replace("class ", ""));
					chg.setDataId(dataId);
					chg.setEntity(event.getEntity());
					chg.setModifiedBy(AppContext.getCurrentUser());
					// 发布事件
					/*if (log.isDebugEnabled()) {
						log.debug("Publish AuditDeletedEvent: {}", ToStringBuilder.reflectionToString(chg));
					}*/
					AppContext.publishApplicationEvent(new AuditDeletedEvent(chg));
				}
			}
		}
	}

	/*
	 * 获取表名
	 */
	private String getTableName(Object entity) {
		String key = entity.getClass().getName();

		String tableName = tableNameMap.get(key);
		if (StringUtils.isEmpty(tableName)) {
			Table ann = entity.getClass().getAnnotation(Table.class);
			if (null != ann) {
				tableName = ann.name();
			} else {
				tableName = entity.getClass().getSimpleName();
			}
			tableNameMap.put(key, tableName);
		}

		return tableName;
	}

	/*
	 * 获取列名
	 */
	/*private String getColumnName(Object entity, String propertyName) {
		String key = entity.getClass().getName() + "." + propertyName;

		String columnName = columnNameMap.get(key);
		if (columnName == null) {
			Column ann = null;
			Field f = ReflectionUtils.findField(entity.getClass(), propertyName);
			if (f != null) {
				ann = f.getAnnotation(Column.class);
			}
			if (ann == null) {
				Method m = ReflectionUtils.findMethod(entity.getClass(), "get" + StringUtils.capitalize(propertyName));
				if (m == null) {
					m = ReflectionUtils.findMethod(entity.getClass(), "is" + StringUtils.capitalize(propertyName));
				}
				if (m != null) {
					ann = m.getAnnotation(Column.class);
				}
			}
			if (ann != null) {
				columnName = ann.name();
			} else {
				columnName = propertyName;
			}
			columnNameMap.put(key, columnName);
		}

		return columnName;
	}*/

	/*
	 * 判断实体是否需要记录日志
	 */
	/*private boolean isEntityAudited(Object entity) {
		return isEntityAuditChange(entity) || isEntityAuditDelete(entity);
	}*/
	
	private boolean isEntityAuditChange(Object entity) {
		String key = entity.getClass().getName();
		Boolean auditchange = entityAuditChangeMap.get(key);
		if (auditchange == null) {
			auditchange = AnnotatedElementUtils.hasAnnotation(entity.getClass(),AuditChange.class);
			entityAuditChangeMap.put(key, auditchange);
		}
		return auditchange;
	}
	
	private boolean isEntityAuditDelete(Object entity) {
		String key = entity.getClass().getName();
		Boolean auditdelete = entityAuditDeleteMap.get(key);
		if (auditdelete == null) {
			auditdelete = AnnotatedElementUtils.hasAnnotation(entity.getClass(),AuditDelete.class);
			entityAuditDeleteMap.put(key, auditdelete);
		}
		return auditdelete;
	}

	/*
	 * 判断字段属性是否需要记录日志
	 */
	/*private boolean isPropertyAudited(Object entity, String propertyName) {
		String key = entity.getClass().getName() + "." + propertyName;

		Boolean auditable = propertyAuditedMap.get(key);
		if (auditable == null) {
			Auditable ann = null;
			Field f = ReflectionUtils.findField(entity.getClass(), propertyName);
			if (f != null) {
				ann = f.getAnnotation(Auditable.class);
			}
			if (ann == null) {
				Method m = ReflectionUtils.findMethod(entity.getClass(), "get" + StringUtils.capitalize(propertyName));
				if (m == null) {
					m = ReflectionUtils.findMethod(entity.getClass(), "is" + StringUtils.capitalize(propertyName));
				}
				if (m != null) {
					ann = m.getAnnotation(Auditable.class);
				}
			}
			auditable = (ann != null && ann.value());
			propertyAuditedMap.put(key, auditable);
		}
		return auditable;
	}*/
	
	private boolean isSpringAuditedField(Object entity, String propertyName) {
		Field f = ReflectionUtils.findField(entity.getClass(), propertyName);
		Method m = ReflectionUtils.findMethod(entity.getClass(), "get" + StringUtils.capitalize(propertyName));
		if (f != null) {
			return AnnotatedElementUtils.hasAnnotation(f,CreatedBy.class)
					|| AnnotatedElementUtils.hasAnnotation(f,CreatedDate.class)
					|| AnnotatedElementUtils.hasAnnotation(f,LastModifiedBy.class)
					|| AnnotatedElementUtils.hasAnnotation(f,LastModifiedDate.class);
		}
		if (m != null) {
			return AnnotatedElementUtils.hasAnnotation(m,CreatedBy.class)
					|| AnnotatedElementUtils.hasAnnotation(m,CreatedDate.class)
					|| AnnotatedElementUtils.hasAnnotation(m,LastModifiedBy.class)
					|| AnnotatedElementUtils.hasAnnotation(m,LastModifiedDate.class);
		}
		return false;
	}
	
	/*private boolean isJsonIgnoreField(Object entity, String propertyName) {
		Field f = ReflectionUtils.findField(entity.getClass(), propertyName);
		Method m = ReflectionUtils.findMethod(entity.getClass(), "get" + StringUtils.capitalize(propertyName));
		if (f != null) {
			return AnnotatedElementUtils.hasAnnotation(f,JsonIgnore.class);
		}
		if (m != null) {
			return AnnotatedElementUtils.hasAnnotation(m,JsonIgnore.class);
		}
		return false;
	}*/

	/*
	 * 比较数据前后是否发生变化
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private boolean isChanged(Object oldValue, Object newValue) {
		if(CoreUtils.notValid(oldValue) && CoreUtils.notValid(newValue)) {
			return false;
		}
		if(CoreUtils.notValid(oldValue)!= CoreUtils.notValid(newValue)) {
			return true;
		}		
		if (oldValue instanceof Collection || newValue instanceof Collection) {
			return false;
		}
		if (oldValue instanceof Persistable && newValue instanceof Persistable) {
			return isChanged(((Persistable)oldValue).getId(),((Persistable)newValue).getId());
		}
		if(oldValue instanceof Comparable && newValue instanceof Comparable) {
			return ((Comparable)oldValue).compareTo((Comparable)newValue)!=0;
		}
		return !oldValue.equals(newValue);
	}

	@Override
	public boolean requiresPostCommitHanding(EntityPersister persister) {
		// TODO Auto-generated method stub
		return false;
	}

	

}