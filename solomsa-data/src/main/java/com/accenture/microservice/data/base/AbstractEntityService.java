/**
 * 
 */
package com.accenture.microservice.data.base;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Persistable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.accenture.microservice.core.util.CoreUtils;
import com.accenture.microservice.core.vo.ResponseResult;
import com.accenture.microservice.data.util.DataUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.SimpleExpression;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


/**抽象实体增删改查服务类
 * @author song.peng
 *
 * @param <REPO> 实体Repository类型
 * @param <T> 实体对象类型
 * @param <ID> 实体的ID类型
 */
@Slf4j
public abstract class AbstractEntityService<REPO extends JpaRepository<T, ID> & QueryDslPredicateExecutor<T>,T extends Persistable<ID>,ID extends Serializable & Comparable<?>> extends AbstractService {

	@Autowired(required=false)
	@Getter
	REPO repository;
	@Getter @Setter
	SimpleExpression<ID> idPath;
	@Getter @Setter
	EntityPathBase<T> qEntity;
	
	/**transPredicate方法需要使用该属性
	 * 
	 */
	protected Map<String,Function<Object,BooleanExpression>> paramConfigMap = Maps.newConcurrentMap();
	
	/**设置paramConfigs属性
	 * 
	 */
	protected abstract void initParamConfigs();
	
	@PostConstruct
	private void init() {
		initQuerydslPropertis();
		initParamConfigs();
	}
	
	public void addParamConfig(@NonNull String key,@NonNull Function<Object,BooleanExpression> expressionFun) {
		if(CoreUtils.isValid(key)) {
			CoreUtils.setProperty(paramConfigMap, key, expressionFun);
		}
	}
	
	/**Q对象id表达式
	 * @return
	 */
	protected SimpleExpression<ID> idExpression(){
		return idPath;
	}
	
	@SuppressWarnings("unchecked")
	private void initQuerydslPropertis() {
		try {
			Type genType = this.getClass().getGenericSuperclass();
			Type[] params = ((ParameterizedType) genType).getActualTypeArguments();
			Class<T> clazz = (Class<T>) params[1];
			String qClassName = clazz.getName().substring(0, clazz.getName().lastIndexOf(".")) + ".Q" + clazz.getSimpleName();
			Class<?> qClazz = Class.forName(qClassName);
			String qStaticName = StringUtils.uncapitalize(clazz.getSimpleName());
			Field field = CoreUtils.getDeclaredField(qClazz, qStaticName);
			qEntity = (EntityPathBase<T>) field.get(qClazz);
			idPath = (SimpleExpression<ID>)CoreUtils.getProperty(qEntity,"id");
		}catch(Exception ex) {
			log.error("initQuerydslPropertis Error:", ex);
		}		
	}
	
	/**把Map对象转换成实体对象
	 * @param map
	 * @return
	 */
	public T transObject(@NonNull Map<String,Object> map) {
		return transObject(map,repository);
	}
	
	/**根据paramMap对象的值转换成查询条件,通过遍历自身paramConfigs属性实现
	 * @param paramMap
	 * @return
	 */
	protected Predicate transPredicate(@NonNull Map<String,Object> paramMap) {
		List<Predicate> predicates = Lists.newArrayList();
		for(String key : paramConfigMap.keySet()) {
			if(CoreUtils.isValid(paramMap.get(key))) {
				predicates.add(paramConfigMap.get(key).apply(paramMap.get(key)));
			}
		}
		return ExpressionUtils.allOf(predicates);
	}
	
	/**根据id查询对象
	 * @param id
	 * @return
	 */
	public T findById(@NonNull ID id) {
		return repository.findOne(id);
	}
	
	/**根据一系列id查询对象集合
	 * @param ids
	 * @return
	 */
	public List<T> findByIds(Collection<ID> ids){
		return Lists.newArrayList(repository.findAll(idExpression().in(ids)));
	}
	
	/**根据输入的查询参数查询对象集合
	 * @param paramsMap
	 * @return
	 */
	public List<T> findByParams(@NonNull Map<String,Object> paramsMap){
		Predicate predicate = transPredicate(paramsMap);
		Sort sort = transSort(paramsMap);
		return Lists.newArrayList(repository.findAll(predicate,sort));
	}
	
	/**根据输入的查询参数查询一个对象
	 * @param paramsMap
	 * @return
	 */
	public T findOneByParams(@NonNull Map<String,Object> paramsMap) {
		Predicate predicate = transPredicate(paramsMap);
		Sort sort = transSort(paramsMap);
		if(ObjectUtils.isEmpty(sort)) {
			return this.getJpaQueryFactory().selectFrom(qEntity).where(predicate).fetchFirst();
		}
		OrderSpecifier<?>[] orders = transQdOrder(sort);
		return this.getJpaQueryFactory().selectFrom(qEntity).where(predicate).orderBy(orders).fetchFirst();
		
	}
	
	private OrderSpecifier<?>[] transQdOrder(@NonNull Sort sort){
		List<OrderSpecifier<?>> orders = Lists.newArrayList();
		Iterator<Order> it = sort.iterator();
		while(it.hasNext()) {
			Order od = it.next();
			try {
				Expression<?> mixin = (Expression<?>) CoreUtils.getProperty(qEntity, od.getProperty());
				Method oMethod = null;
				if(od.isAscending()) {
					oMethod = mixin.getClass().getMethod("asc");
				}else if(od.isDescending()) {
					oMethod = mixin.getClass().getMethod("desc");
				}
				if(null!=oMethod) {
					orders.add((OrderSpecifier<?>) oMethod.invoke(mixin));
				}
			} catch (Exception e) {
				log.error("transQdOrder error:", e);;
			} 
			
		}
		OrderSpecifier<?>[] result = new OrderSpecifier<?>[orders.size()];
		return orders.toArray(result);
	}
	
	/**根据输入的查询参数分页查询对象集合
	 * @param paramsMap
	 * @param page
	 * @param size
	 * @return
	 */
	public Page<T> findByParamsPage(@NonNull Map<String,Object> paramsMap,final int page, final int size){
		Predicate predicate = transPredicate(paramsMap);
		Sort sort = transSort(paramsMap);
		PageRequest pageable = new PageRequest(page, size, sort);
		return repository.findAll(predicate,pageable);	
	}
	
	/**保存一个对象
	 * @param entity
	 * @return
	 */
	@Transactional
	public T saveOne(@NonNull T entity) {
		ResponseResult<T> validate = DataUtils.validateEntity(entity);
		Assert.isTrue(validate.isSuccess(),validate.getMessage());
		return repository.save(entity);
	}
	
	/**保存一组对象
	 * @param objects
	 * @return
	 */
	@Transactional
	public List<T> saveList(@NonNull List<T> entitys){
		List<T> list = Lists.newArrayList();
		for(T entity : entitys) {
			ResponseResult<T> validate = DataUtils.validateEntity(entity);
			Assert.isTrue(validate.isSuccess(),validate.getMessage());
			list.add(entity);
		}
		list = repository.save(list);
		return list;
	}
	
	/**根据id删除一个对象
	 * @param id
	 */
	@Transactional
	public void deleteById(@NonNull ID id) {
		repository.delete(id);
	}
	
	/**根据一组id删除一组对象
	 * @param ids
	 */
	@Transactional
	public void deleteList(@NonNull List<ID> ids) {
		List<T> list = findByIds(ids);
		if(!CollectionUtils.isEmpty(list)) {
			repository.delete(list);
		}
	}
	
}
