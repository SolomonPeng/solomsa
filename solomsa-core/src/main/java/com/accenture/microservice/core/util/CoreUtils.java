/**
 * 
 */
package com.accenture.microservice.core.util;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.accenture.microservice.core.GlobalConst;
import com.accenture.microservice.core.vo.ResponseResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

/**
 * @author song.peng
 *
 */
@Slf4j
public class CoreUtils {

	private static ObjectMapper objectMapper = new ObjectMapper(); 
	
	public static final String EMPTY_STRING = "";
	private static final String ARRAY_ELEMENT_SEPARATOR = ", ";
	
	public static <T> T castGeneric(Object o,Class<T> clazz){
        if(clazz!=null){
            if(clazz.isInstance(o))
                  return clazz.cast(o);
            else
            	log.error(o +" is not a "+clazz.getName());
        }
        return null;
    }
	
	/**用于输入参数的无效判断:判断是空或者空白或者null undefined
	 * @param obj
	 * @return
	 */
	public static boolean notValid(Object obj) {
		if(ObjectUtils.isEmpty(obj)) {
			return true;
		}
		if(obj instanceof String) {
			String str = (String)obj;
			return !StringUtils.hasText(str) || str.equalsIgnoreCase("null") || str.equalsIgnoreCase("undefined");
		}
		return false;
	}
	
	public static boolean isValid(Object obj) {
		return !notValid(obj);
	}
	
	/** 
     * 判断是否基本类型
     * @param bean 
     * @return 
     */  
	public static boolean isWrapClass(Class<?> clz) { 
        if(clz.equals(String.class)||clz.equals(Boolean.class)||clz.equals(BigDecimal.class)
        		||clz.equals(Integer.class)||clz.equals(Double.class)||clz.equals(Long.class)||clz.equals(Float.class)
        		||clz.equals(Date.class)||clz.equals(Timestamp.class)) {
        	return true;
        }
		return clz.isPrimitive();
    } 
	
	
	/**
     * 循环向上转型,获取对象的DeclaredField.
     *
     * @throws NoSuchFieldException 如果没有该Field时抛出.
     */
    public static Field getDeclaredField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Assert.notNull(clazz,GlobalConst.NULL_ARGUMENT_MSG);
        Assert.hasText(fieldName,GlobalConst.NULL_ARGUMENT_MSG);
        for (Class<?> superClass = clazz; superClass != Object.class; superClass = superClass.getSuperclass()) {
            try {
                return superClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                // Field不在当前类定义,继续向上转型
            }
        }
        throw new NoSuchFieldException("No such field: " + clazz.getName() + '.' + fieldName);
    }
    
    /**获取对象属性名列表
     * @param object
     * @return
     */
    private static <T> List<String> getPropertiesNameList(T object){
		List<String> list = Lists.newArrayList();
		if(object instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String,Object> map = (Map<String, Object>) object;
			for(String key:map.keySet()) {
				list.add(key);
			}
		}else {
			@SuppressWarnings("unchecked")
			Class<T> cls = (Class<T>) object.getClass();
			List<Field> fields = Arrays.asList(cls.getDeclaredFields());
			for (Field field : fields) {
				if (field.getName().contains("serialVersionUID")) {
					continue;
				}
				list.add(field.getName());
			}
			Class<? super T> clazz = cls.getSuperclass();
			for (Class<? super T> superClass = clazz; superClass != Object.class; superClass = superClass.getSuperclass()) {
				if (superClass.getName().contains("Persistable")) {
					break;
				}
				Field[] superFields = superClass.getDeclaredFields();
				for (Field field : superFields) {
					if (field.getName().contains("serialVersionUID")) {
						continue;
					}
					if (!list.contains(field.getName())) {
						list.add(field.getName());
					}
				}
			}
		}
		return list;
	}
		
	
	/**获取对象属性
	 * @param object
	 * @param propertyName
	 * @return
	 */
	public static Object getProperty(Object object,String propertyName) {
		if(object instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String,Object> map = (Map<String,Object>)object;
			return map.get(propertyName);
		}else {
			PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(object.getClass(), propertyName);
	        if (pd != null) {
				Method readMethod = pd.getReadMethod();
				if (readMethod != null ) {
					try {
						if (!Modifier.isPublic(readMethod.getDeclaringClass().getModifiers())) {
							readMethod.setAccessible(true);
						}
						Object value = readMethod.invoke(object);
						return value;
					}
					catch (Throwable ex) {
						log.error("Unable to get property:", ex);
					}
				}
			}else {
				try {
					Field field = getDeclaredField(object.getClass(),propertyName);
					if(ObjectUtils.isEmpty(field)) {
						return null;
					}
					return forceGetField(object,field);
				}catch (Throwable ex) {
					log.error("Unable to get property:", ex);
				}
			}
		}
		return null;
	}
	
	/**设置对象属性
	 * @param object
	 * @param propertyName
	 * @param value
	 */
	public static void setProperty(Object object,String propertyName,Object value) {
		if(object instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String,Object> map = (Map<String,Object>)object;
			if(map.keySet().contains(propertyName)) {
				map.remove(propertyName);
			}
			map.put(propertyName, value);			
		}else {
			PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(object.getClass(), propertyName);        
	        if (pd != null) {
	        	Class<?> type = pd.getPropertyType();
	        	Object o = null;
	        	if(value!=null) {
	        		if(!ClassUtils.matchesTypeName(type, value.getClass().getName())) {
		            	//不能转换的类型直接返回
		            	if(!isWrapClass(type)) {
		            		return;
		            	}
		            	try {
		    				o = TypeCaseUtils.convert(value, type.getName(), null);
		    			} catch (Exception ex) {
		    				log.error("Unable to cast object:", ex);
		    				return;
		    			}
		            }else {
		            	o = value;
		            }
	        	}	            
	        	Method writeMethod = pd.getWriteMethod();
				if (writeMethod != null ) {
					try {
						if (!Modifier.isPublic(writeMethod.getDeclaringClass().getModifiers())) {
							writeMethod.setAccessible(true);
						}
						writeMethod.invoke(object, o);
					}
					catch (Throwable ex) {
						log.error("Unable to set property:", ex);
					}
				}
			}else {
				try {
					Field field = getDeclaredField(object.getClass(),propertyName);
					if(ObjectUtils.isEmpty(field)) {
						log.error("Nope property:"+propertyName);
					}
					Object o = null;
					if(value!=null) {
						Class<?> type = field.getType();			        			        	
			            if(!ClassUtils.matchesTypeName(type, value.getClass().getName())) {
			            	//不能转换的类型直接返回
			            	if(!isWrapClass(type)) {
			            		return;
			            	}
			            	try {
			    				o = TypeCaseUtils.convert(value, type.getName(), null);
			    			} catch (Exception ex) {
			    				log.error("Unable to cast object:", ex);
			    				return;
			    			}
			            }else {
			            	o = value;
			            }
					}					
					forceSetField(object,field,o);
				}catch (Throwable ex) {
					log.error("Unable to set property:", ex);
				}
			}
		}
	}
	
	/**
     * 暴力设置对象变量值,忽略private,protected修饰符的限制.
     *
     * @throws NoSuchFieldException 如果没有该Field时抛出.
     * @throws ParseException 
     */
	private static void forceSetField(Object object,Field field,Object value) throws Exception {
		if(Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
			return;
		}
		boolean accessible = field.isAccessible();
        field.setAccessible(true);
        field.set(object, value);
        field.setAccessible(accessible);
	}
	
	/**
     * 暴力设置对象变量值,忽略private,protected修饰符的限制.
     *
     * @throws NoSuchFieldException 如果没有该Field时抛出.
     * @throws ParseException 
     */
	public static void forceSetField(Object object,String fieldName,Object value) {
		try {
			Field field = getDeclaredField(object.getClass(),fieldName);
			forceSetField(object,field,value);
		}catch(Exception ex) {
			log.error("Unable to set "+fieldName+" :", ex);
		}		
	}
	
	/**
     * 暴力获取对象变量值,忽略private,protected修饰符的限制.
     *
     * @throws NoSuchFieldException 如果没有该Field时抛出.
     */
	private static Object forceGetField(Object object,Field field) throws Exception {
		boolean accessible = field.isAccessible();
        field.setAccessible(true);
        Object result = field.get(object);
        field.setAccessible(accessible);
        return result;
	}
	
	/**
     * 暴力获取对象变量值,忽略private,protected修饰符的限制.
     *
     * @throws NoSuchFieldException 如果没有该Field时抛出.
     */
	public static Object forceGetField(Object object,String fieldName) {
		try {
			Field field = getDeclaredField(object.getClass(),fieldName);
			boolean accessible = field.isAccessible();
	        field.setAccessible(true);
	        Object result = field.get(object);
	        field.setAccessible(accessible);
	        return result;
		}catch(Exception ex) {
			log.error("Unable to get "+fieldName+" :", ex);
			return null;
		}
	}
	
	/**对象copy
	 * @param target
	 * @param source
	 * @param ignorePropertyNameCase 是否忽略大小写
	 * @return
	 */
	public static <T,S> T copyProperties(T target, S source, boolean ignorePropertyNameCase){
		List<String> targetProperties = getPropertiesNameList(target);
		List<String> sourceProperties = getPropertiesNameList(source);
		for(String tpName : targetProperties) {
			boolean match = false;
			String matchSpName = "";
			for(String spName : sourceProperties) {
				if(ignorePropertyNameCase) {
					match = tpName.equalsIgnoreCase(spName);
				}else {
					match = tpName.equals(spName);
				}
				if(match) {
					matchSpName = spName;
					break;
				}
			}
			if(match && StringUtils.hasText(matchSpName)) {
				Object value = getProperty(source,matchSpName);
				if(CoreUtils.isValid(value) && isWrapClass(value.getClass())) {
					setProperty(target,tpName,value);
				}				
			}
		}
		return target;
	}


	/**对象copy
	 * @param target
	 * @param source
	 * @return
	 */
	public static <T,S> T copyProperties(T target, S source) {
		return copyProperties(target,source,false);
	}
	
	/**对象copy属性名匹配忽略大小写
	 * @param target
	 * @param source
	 * @return
	 */
	public static <T,S> T copyPropertiesIgnoreCaseMatch(T target, S source){
		return copyProperties(target,source,true);
	}
	
	/**List对象转换
	 * @param clazz
	 * @param sourceList
	 * @return
	 */
	public static <T,S> List<T> transCollectionToOtherClass(Class<T> clazz, Collection<S> sourceList){
		List<T> result = new ArrayList<T>();
		for(S item:sourceList){
			try {
				T resultObj = clazz.newInstance();
				copyProperties(resultObj, item);
				result.add(resultObj);
			} catch (Exception e) {
				log.error("transCollectionToOtherClass Error",e);
			}
		}
		return result;
	}
	
	
		
	
	/**根据key从list里提取不相同的字段值返回其list
	 * @param <T>
	 * @param list
	 * @param key
	 * @return
	 */
	public static <T,S> List<S> distinctValueList(List<T> list,String key, Class<S> clazz){
		List<S> result = new ArrayList<S>();
		try {
			for(T obj : list) {
				if(!ObjectUtils.isEmpty(obj)) {
					Object itemValue = getProperty(obj,key);
					S item = castGeneric(itemValue,clazz);
					if(!result.contains(itemValue)&&!ObjectUtils.isEmpty(itemValue)) {
						result.add(item);
					}
				}				
			}
		}catch (Exception e) {
			log.error("distinctValueList Error",e);
        }
		return result;
	}
	
	
	/**过滤唯一值的LIST
	 * @param list
	 * @return
	 */
	public static <T> List<T> distinctList(List<T> list){
		List<T> result = new ArrayList<T>();
		for(T obj : list) {
			if(result.contains(obj)) {
				continue;
			}
			result.add(obj);
		}
		return result;
	}
	
	/**可选字符串参数合并返回
	 * @param str
	 * @return
	 */
	public static String optionalStringConbine(String... str) {
		String result="";
		if(!ObjectUtils.isEmpty(str)) {
			for(String s : str) {
				result += s;
			}
		}
		return result;
	}
	
	
	/**可选根据key值获取map的value
	 * @param map
	 * @param key
	 * @return
	 */
	@SafeVarargs
	public static <T> T optionalMap(Map<String,T> map,String key, T... defaultValue ) {
		T defaultObject = null;
		if(!ObjectUtils.isEmpty(defaultValue)) {
			defaultObject = defaultValue[0];
		}
		return (ObjectUtils.isEmpty(map)||ObjectUtils.isEmpty(map.get(key)))?defaultObject:map.get(key);
	}
	
	/**可选根据key值获取map的value字符串,若获取不到返回空字符串
	 * @param map
	 * @param key
	 * @return
	 */
	public static String optionalMapString(Map<String,Object> map,String key) {
		return optionalMap(map,key,"").toString();
	}
	
	/**获取ResponseResult的结果
	 * @param rs
	 * @param extMsg
	 * @return
	 */
	public static <T> T getResponseResult(ResponseResult<T> rs, String... extErrorMsg) {
		Assert.isTrue(rs.isSuccess(),optionalStringConbine(extErrorMsg)+rs.getMessage());
		return rs.getResponse();		
	}
	
	
	/**获取一个带字段空表
	 * @param keys
	 * @return
	 */
	public static List<Map<String,Object>> emptyKeyList(String... keys){
		List<Map<String,Object>> list = Lists.newArrayList();
		Map<String,Object> map = Maps.newHashMap();
		for(String key : keys) {			
			map.put(key, null);
		}
		list.add(map);
		return list;
	}
	
	
	/** 两个LIST根据相同字段做链接
	 * @param leftList     源list做左连接
	 * @param leftKeys     源list对比字段
	 * @param rightList    对比的list
	 * @param rightKeys    对比的字段
	 * @param extendKeys   对源list添加的字段Map<rightKey名,as名>
	 * @return
	 */
	public static List<Map<String,Object>> conbineList(List<Map<String,Object>> leftList,String[] leftKeys,
			List<Map<String,Object>> rightList,String[] rightKeys,Map<String,String> extendKeys){
		List<Map<String,Object>> result = Lists.newArrayList();
		for(Map<String,Object> mapLeft:leftList) {
			Map<String,Object> item = Maps.newHashMap();
			item.putAll(mapLeft);
			Map<String,Object> matchMap = Maps.newHashMap();
			for(String key : leftKeys) {
				matchMap.put(key, mapLeft.get(key));
			}
			Map<String,Object> matchedMap = null;   //匹配上的rightList
			for(Map<String,Object> mapRight : rightList) {
				Map<String,Object> valueMap = Maps.newHashMap();
				for(String key : rightKeys) {
					valueMap.put(key, mapRight.get(key));
				}
				boolean isMatch = false;
				for(int i = 0 ; i<leftKeys.length; ++i) {
					if(!matchMap.get(leftKeys[i]).equals(valueMap.get(rightKeys[i]))) {
		            	isMatch = false;
		            	break;
		            }
		            isMatch = true;
				}		       
		        if(!isMatch) {
		        	continue;
		        }
		        //匹配成功
		        matchedMap = mapRight;
		        break;
			}
			if(ObjectUtils.isEmpty(matchedMap)) {
				for(String extendKey : extendKeys.keySet()) {
					//Object value = matchedMap.get(extendKey);
					item.put(extendKeys.get(extendKey), null);
				}
			}else {
				for(String extendKey : extendKeys.keySet()) {
					item.put(extendKeys.get(extendKey), matchedMap.get(extendKey));
				}
			}
			result.add(item);
		}		
		return result;
	}
	
	
	/** List合成String
	 * @param list
	 * @param separator 分隔符
	 * @return
	 */
	public static String listToString(List<String> list, String... separator) {    
		StringBuilder sb = new StringBuilder();
		String realSeperator = ARRAY_ELEMENT_SEPARATOR;
		if(!ObjectUtils.isEmpty(separator)) {
			realSeperator = separator[0];
		}
		if(CollectionUtils.isEmpty(list)) {
			return sb.toString();
		}
		for (int i = 0; i < list.size(); i++) {        
			if (i == list.size() - 1) {            
				sb.append(list.get(i));        
			} else {
				sb.append(list.get(i));            
				sb.append(realSeperator);        
			}    
		}    
		return sb.toString();
	}
	
	
	/** String转List
	 * @param str
	 * @param separator 分隔符
	 * @return
	 */
	public static List<String> stringToList(String str, String... separator){
		List<String> result = Lists.newArrayList();
		String realSeperator = ARRAY_ELEMENT_SEPARATOR;
		if(!ObjectUtils.isEmpty(separator)) {
			realSeperator = separator[0];
		}
		if(StringUtils.hasText(str)) {
			String[] arrStr = str.split(realSeperator);
			for(int i = 0; i<arrStr.length; ++i) {
				result.add(arrStr[i]);
			}
		}		
		return result;
	}
	
	/** 对象转字符串
	 * @param obj
	 * @return
	 */
	public static String toString(Object obj) {
		String result = EMPTY_STRING;
		if(ObjectUtils.isEmpty(obj)) {
			return result;
		}
		try {
			result = objectMapper.writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			log.error("toString Error",e);
		}
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public static Map<String,Object> toMap(Object obj){
		Map<String,Object> map = Maps.newHashMap();
		if(ObjectUtils.isEmpty(obj)) {
			return map;
		}
		try {
			String str = EMPTY_STRING;
			if(obj instanceof String) {
				str = (String) obj;
			}else {
				str = toString(obj);
			}
			map = objectMapper.readValue(str, Map.class);
		} catch (Exception e) {
			log.error("toMap Error",e);
		} 
		return map;
	}
}
