/**
 * 
 */
package org.solomsa.data.util;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Lob;

import org.solomsa.core.util.CoreUtils;
import org.solomsa.core.vo.ResponseResult;
import org.springframework.beans.BeanUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ObjectUtils;

import lombok.NonNull;

/**
 * @author song.peng
 *
 */
public class DataUtils {

	/** 对实体对象非空或字段长度限制做校验(字段长度目前JPA注解只支持以char做长度单位定义的字段不支持byte做长度单位)
	 * @param entity 要校验的对象
	 * @return ResponseResult 内包含的是可通过校验的对象
	 */
	@SuppressWarnings("unchecked")
	public static <T> ResponseResult<T> validateEntity(@NonNull T entity) {
		ResponseResult<T> result = new ResponseResult<T>();
		T response = null;
		try {
			response = (T) entity.getClass().newInstance();
			BeanUtils.copyProperties(entity, response);
		} catch (Exception e) {
			result.setSuccess(false);
			result.setResponse(entity);
			result.setMessage(e.getMessage());
			return result;
		} 
		List<Field> fields = Arrays.asList(entity.getClass().getDeclaredFields());
		boolean success = true;
		String message = CoreUtils.EMPTY_STRING;
		for (Field field : fields) {			
			if(field.isAnnotationPresent(EmbeddedId.class)) {
				Object obj = CoreUtils.forceGetField(entity, field.getName());
				if(ObjectUtils.isEmpty(obj)) {
					success = false;
					message += field.getName()+" is id field,unable to be null. ";
				}else {
					ResponseResult<Object> idValid = validateEntity(obj);
					success = idValid.isSuccess();
					message += idValid.getMessage();
					CoreUtils.forceSetField(response, field.getName(), idValid.getResponse());
				}
			}
			Column column = AnnotationUtils.findAnnotation(field,Column.class);
			if(ObjectUtils.isEmpty(column) || field.isAnnotationPresent(Lob.class) || !field.getType().equals(String.class)) {
				continue;
			}
			Object obj = CoreUtils.forceGetField(entity, field.getName());
			if(ObjectUtils.isEmpty(obj)) {
				if(!column.nullable()) {
					success = false;
					message += field.getName()+" unable to be null. ";
					CoreUtils.forceSetField(response, field.getName(), "null");
				}
				continue;
			}
			//字段长度校验,只支持数据库定义的长度单位是char,如果是byte则无效
			if(obj.toString().length()>column.length()) {
				success = false;
				message += "Length of "+field.getName()+" is too large. ";
				CoreUtils.forceSetField(response, field.getName(), obj.toString().substring(0, column.length()));
			}
		}
		result.setSuccess(success);
		result.setMessage(message);
		result.setResponse(response);
		return result;
	}
}
