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

	/** 对实体对象非空或字段长度限制做校验
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
			
			if(obj.toString().length()>column.length()) {
				success = false;
				message += "Length of "+field.getName()+" is too large. ";
				CoreUtils.forceSetField(response, field.getName(), obj.toString().substring(0, column.length()));
			}
			/*int length = column.length();
			String str;
			try {
				str = new String(obj.toString().getBytes("UTF-8"),"ISO-8859-1");
				if(str.length()>length) {
					success = false;
					message += "Length of "+field.getName()+" is too large. ";
					CoreUtils.forceSetField(response, field.getName(), new String(str.substring(0, length).getBytes("ISO-8859-1"),"UTF-8"));
				}
			} catch (UnsupportedEncodingException e) {
				success = false;
				message += field.getName()+" UnsupportedEncodingException " + e.getMessage();
			}*/
			
		}
		result.setSuccess(success);
		result.setMessage(message);
		result.setResponse(response);
		return result;
	}
}
