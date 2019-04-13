/**
 * 
 */
package org.solomsa.data.component;

import java.lang.reflect.Field;
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
import org.solomsa.data.annotation.AuditDelete;
import org.solomsa.data.annotation.AuditUpdate;
import org.solomsa.data.base.AbstractAuditable;
import org.solomsa.data.event.AuditDeletedEvent;
import org.solomsa.data.event.AuditUpdatedEvent;
import org.solomsa.data.event.DeletedRecord;
import org.solomsa.data.event.UpdatedRecord;
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

/**自动审计监听器
 * @author song.peng
 *
 */
@Slf4j
public class AuditedEventListener implements PostUpdateEventListener, PostDeleteEventListener, PreUpdateEventListener {

	private static final long serialVersionUID = 1L;
	private static Map<String, String> tableNameMap = new ConcurrentHashMap<String, String>();
	private static Map<String, Boolean> entityAuditUpdateMap = new ConcurrentHashMap<String, Boolean>();
	private static Map<String, Boolean> entityAuditDeleteMap = new ConcurrentHashMap<String, Boolean>();

	/* (non-Javadoc)
	 * @see org.hibernate.event.spi.PostUpdateEventListener#requiresPostCommitHanding(org.hibernate.persister.entity.EntityPersister)
	 * @see org.hibernate.event.spi.PostDeleteEventListener#requiresPostCommitHanding(org.hibernate.persister.entity.EntityPersister)
	 */
	@Override
	public boolean requiresPostCommitHanding(EntityPersister persister) {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.hibernate.event.spi.PreUpdateEventListener#onPreUpdate(org.hibernate.event.PreUpdateEvent)
	 */
	@Override
	public boolean onPreUpdate(PreUpdateEvent event) {
		if (event.getEntity() instanceof Persistable) {
			Persistable<?> entity = (Persistable<?>) event.getEntity();
			Object id = entity.getId();
			if (null != id) {
				String[] propertyNames = event.getPersister().getPropertyNames();
				boolean isChanged = false;
				for (int i = 0; i < propertyNames.length; i++) {
					String propertyName = propertyNames[i];
					if(isSpringAuditedField(event.getEntity(), propertyName)) {
						continue;
					}
					Object oldValue = event.getOldState()[i];
					Object newValue = event.getState()[i];
					if (isUpdated(oldValue, newValue)) {
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
	 * @see org.hibernate.event.spi.PostUpdateEventListener#onPostUpdate(org.hibernate.event.PostUpdateEvent)
	 */
	@Override
	public void onPostUpdate(PostUpdateEvent event) {
		// 判断实体是否需要记录日志
		if (isEntityAuditUpdate(event.getEntity()) && ClassUtils.isPresent("org.solomsa.app.context.AppContext", getClass().getClassLoader())) {
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
					boolean changed = false;
					String[] propertyNames = event.getPersister().getPropertyNames();
					Map<String, Object> oldEntityMap = new HashMap<String, Object>();
					Map<String, Object> newEntityMap = new HashMap<String, Object>();
					for (int i = 0; i < propertyNames.length; i++) {
						String propertyName = propertyNames[i];
						//自动审计字段忽略
						if(isSpringAuditedField(event.getEntity(), propertyName)) {
							continue;
						}
						Object oldValue = event.getOldState()[i];
						Object newValue = event.getState()[i];
						oldEntityMap.put(propertyName, oldValue);
						newEntityMap.put(propertyName, newValue);
						if (isUpdated(oldValue, newValue)) {							
							changed = true;
							break;
						}
					}
					if(changed) {
						UpdatedRecord record = new UpdatedRecord();
						Object oldEntity = CoreUtils.toString(oldEntityMap);
						Object newEntity = CoreUtils.toString(newEntityMap);						
						record.setBatchNo(batchNo);
						record.setTableName(tableName);
						record.setClassName(event.getEntity().getClass().getName());
						record.setDataId(dataId);
						record.setOldValue(oldEntity);
						record.setNewValue(newEntity);
						record.setEntity(event.getEntity());
						record.setModifiedBy(AppContext.getCurrentUser());						
						AppContext.publishApplicationEvent(new AuditUpdatedEvent(record));
					}
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.hibernate.event.spi.PostDeleteEventListener#onPostDelete(org.hibernate.event.PostDeleteEvent)
	 */
	@Override
	public void onPostDelete(PostDeleteEvent event) {
		// 判断实体是否需要记录日志
		if (isEntityAuditDelete(event.getEntity()) && ClassUtils.isPresent("org.solomsa.app.context.AppContext", getClass().getClassLoader())) {
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
					DeletedRecord record = new DeletedRecord();
					record.setBatchNo(batchNo);
					record.setTableName(tableName);
					record.setClassName(event.getEntity().getClass().getName());
					record.setDataId(dataId);
					record.setEntity(event.getEntity());
					record.setModifiedBy(AppContext.getCurrentUser());
					// 发布事件
					AppContext.publishApplicationEvent(new AuditDeletedEvent(record));
				}
			}
		}
	}

	
	/**获取表名
	 * @param entity
	 * @return
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

		
	/**是否有更新审计的注解标识
	 * @param entity
	 * @return
	 */
	private boolean isEntityAuditUpdate(Object entity) {
		String key = entity.getClass().getName();
		Boolean result = entityAuditUpdateMap.get(key);
		if (result == null) {
			result = Boolean.valueOf(AnnotatedElementUtils.hasAnnotation(entity.getClass(),AuditUpdate.class));
			entityAuditUpdateMap.put(key, result);
		}
		return result.booleanValue();
	}
	
	/**是否有删除审计的注解标识
	 * @param entity
	 * @return
	 */
	private boolean isEntityAuditDelete(Object entity) {
		String key = entity.getClass().getName();
		Boolean result = entityAuditDeleteMap.get(key);
		if (result == null) {
			result = Boolean.valueOf(AnnotatedElementUtils.hasAnnotation(entity.getClass(),AuditDelete.class));
			entityAuditDeleteMap.put(key, result);
		}
		return result.booleanValue();
	}

		
	/**是否自带的审计字段
	 * @param entity
	 * @param propertyName
	 * @return
	 */
	private boolean isSpringAuditedField(Object entity, String propertyName) {
		Field field = ReflectionUtils.findField(entity.getClass(), propertyName);
		if (field != null) {
			return AnnotatedElementUtils.hasAnnotation(field,CreatedBy.class)
					|| AnnotatedElementUtils.hasAnnotation(field,CreatedDate.class)
					|| AnnotatedElementUtils.hasAnnotation(field,LastModifiedBy.class)
					|| AnnotatedElementUtils.hasAnnotation(field,LastModifiedDate.class);
		}
		return false;
	}
	

	/**判断是否真的更新了
	 * @param oldValue
	 * @param newValue
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private boolean isUpdated(Object oldValue, Object newValue) {
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
			return isUpdated(((Persistable)oldValue).getId(),((Persistable)newValue).getId());
		}
		if(oldValue instanceof Comparable && newValue instanceof Comparable) {
			return ((Comparable)oldValue).compareTo((Comparable)newValue)!=0;
		}
		return !oldValue.equals(newValue);
	}

	

	

}