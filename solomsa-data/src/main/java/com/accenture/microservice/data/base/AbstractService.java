/**
 * 
 */
package com.accenture.microservice.data.base;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.hibernate.SQLQuery;
import org.hibernate.transform.Transformers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Persistable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.accenture.microservice.core.util.CoreUtils;
import com.google.common.collect.Lists;
import com.querydsl.jpa.JPQLQueryFactory;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author song.peng
 *
 */
@Slf4j
public abstract class AbstractService {

	@PersistenceContext
	@Getter
	EntityManager entityManager;
	
	@Autowired(required=false)
	@Getter
	JPQLQueryFactory jpaQueryFactory;
	
	public <T,ID extends Serializable> T transObject(Map<String,Object> map,JpaRepository<T, ID> repository){
		return transObject(map,repository,false);
	}
	
	@SuppressWarnings("unchecked")
	public <T,ID extends Serializable> T transObject(@NonNull Map<String,Object> map,@NonNull JpaRepository<T, ID> repository, boolean ignoreCaseMatch){
		Class<T> tClass = (Class<T>)((ParameterizedType)repository.getClass().getInterfaces()[0].getGenericInterfaces()[0]).getActualTypeArguments()[0];
		Class<ID> idClass = (Class<ID>)((ParameterizedType)repository.getClass().getInterfaces()[0].getGenericInterfaces()[0]).getActualTypeArguments()[1];
		ID id = null;
		try {
			id = idClass.newInstance();
		} catch (Exception e1) {
			log.error("TransObject Error: ", e1);
		}
		if(id instanceof String) {
			if(StringUtils.hasText(CoreUtils.optionalMapString(map, "id"))) {
				id = CoreUtils.castGeneric(map.get("id"), idClass);
			}else {
				map.remove("id");
				id = null;
			}
		}else {
			id = CoreUtils.copyProperties(id, map, ignoreCaseMatch);
		}
		T resultObj = null;
		if(!ObjectUtils.isEmpty(id)) {
			resultObj = repository.findOne(id);
		}
		if(ObjectUtils.isEmpty(resultObj)) {
			try {
				resultObj = tClass.newInstance();
			} catch (Exception e) {
				log.error("TransObject Error: ", e);
				return null;
			}
		}
		resultObj = CoreUtils.copyProperties(resultObj, map, ignoreCaseMatch);
		return resultObj;
	}
	
	protected Sort transSort(String ascs,String descs) {
		List<Order> orders = Lists.newArrayList();
    	if(StringUtils.hasText(ascs)) {
    		String[] arrAscs = StringUtils.delimitedListToStringArray(ascs.trim(), "-");
    		for(String asc:arrAscs) {
    			orders.add(new Order(Direction.ASC,asc));
    		}
    	}
    	if(StringUtils.hasText(descs)) {
    		String[] arrDescs = StringUtils.delimitedListToStringArray(descs.trim(), "-");
    		for(String desc:arrDescs) {
    			orders.add(new Order(Direction.DESC,desc));
    		}
    	}
    	if(CollectionUtils.isEmpty(orders)) {
    		orders.add(new Order(Direction.DESC,"createDate"));
    	}
    	return new Sort(orders);
	}
	
	protected Sort transSort(Map<String,Object> paramsMap) {
		return transSort(CoreUtils.optionalMapString(paramsMap, "ascs"),CoreUtils.optionalMapString(paramsMap, "descs"));
	}
	
	@SuppressWarnings("unchecked")	
	protected List<Object> getListBySql(String sql, List<Object> params) {
		List<Object> result = null;
        if (!CollectionUtils.isEmpty(params)) {
            Query query = entityManager.createNativeQuery(sql);
            for (int i = 0; i < params.size(); i++) {
                query.setParameter(i+1, params.get(i));
            }
            query.unwrap(SQLQuery.class).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
            try{
            	result = query.getResultList();
            }catch(Exception exc){
            	result = null;
            	log.error("getListBySql Error: ", exc);
            }
        } else {
            Query query = entityManager.createNativeQuery(sql);
            query.unwrap(SQLQuery.class).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
            try{
            	result = query.getResultList();
            }catch(Exception exc){
            	result = null;
            	log.error("getListBySql Error: ", exc);
            }
        }
        return result;
    }
    
    /**运行select SQL返回单个结果
     * @author song.peng
     * @param sql
     * @param params
     * @return
     */
    protected Object getObjectBySql(String sql,List<Object> params) {
    	Object result = null;
        if (!CollectionUtils.isEmpty(params)) {
            Query query = entityManager.createNativeQuery(sql);
            for (int i = 0; i < params.size(); i++) {
                query.setParameter( i+1 , params.get(i));
            }
            query.unwrap(SQLQuery.class).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
            try{
            	result = query.getSingleResult();
            }catch(Exception exc){
            	result = null;
            	log.error("getObjectBySql Error: ", exc);
            }
        } else {
            Query query = entityManager.createNativeQuery(sql);
            query.unwrap(SQLQuery.class).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
            try{
            	result = query.getSingleResult();
            }catch(Exception exc){
            	result = null;
            	log.error("getObjectBySql Error: ", exc);
            }            
        }
        return result;
    }
    
    
    /**运行update/delete SQL返回影响行数
     * @author song.peng
     * @param sql 原生sql语句
     * @param params 参数
     * @return
     */
    protected int executeUpdate(String sql,List<Object> params) {
    	 if (!CollectionUtils.isEmpty(params)) {
             Query query = entityManager.createNativeQuery(sql);
             for (int i = 0; i < params.size(); i++) {
                 query.setParameter( i , params.get(i));
             }
             query.unwrap(SQLQuery.class).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
             return query.executeUpdate();
         } else {
             Query query = entityManager.createNativeQuery(sql);
             query.unwrap(SQLQuery.class).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
             return query.executeUpdate();
         }
     }
    
    /**把懒加载属性加载
     * @param list
     * @return
     */
    @Transactional
    protected <T> List<T> loadLazy(List<T> list){
    	for(@SuppressWarnings("unused") T item: list) {
    		
    	}
    	return list;
    }
    
    /**在Transactional中放弃对象的改变
     * @param entity
     */
    public void cancelChange(@NonNull Persistable<?> entity) {
    	if(!entity.isNew()) {
    		entityManager.detach(entity);
    	}
    }
}
