/**
 * 
 */
package com.accenture.microservice.app.base;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.ExceptionHandler;

import com.accenture.microservice.core.util.CoreUtils;
import com.accenture.microservice.core.vo.ResponseResult;

/** Controller基类
 * @author song.peng
 *
 */
public abstract class AbstractController {
	
	@ExceptionHandler(Exception.class)
	public String exceptionHandler(Exception ex, HttpServletRequest request,HttpServletResponse response) {
		ResponseResult<Object> result = new ResponseResult<Object>();		
		result.setSuccess(false);
		result.setMessage(ex.getMessage());
		try {
			response.getWriter().write(CoreUtils.toString(result));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}
	
	public <T> ResponseResult<T> processResponse(T obj){
		return processResponse(obj,true);
	}
	
	public <T> ResponseResult<T> processResponse(T obj,boolean isSucess){
		ResponseResult<T> result = new ResponseResult<T>();
		result.setResponse(obj);
        result.setSuccess(isSucess);
		return result;
	}
	
	public String toJson(Object value) {
		try {
			return CoreUtils.toString(value);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}	
	
}
