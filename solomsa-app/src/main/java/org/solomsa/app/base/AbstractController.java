/**
 * 
 */
package org.solomsa.app.base;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.solomsa.core.util.CoreUtils;
import org.solomsa.core.vo.ResponseResult;
import org.springframework.web.bind.annotation.ExceptionHandler;

import lombok.extern.slf4j.Slf4j;

/** Controller基类
 * @author song.peng
 *
 */
@Slf4j
public abstract class AbstractController {
	
	@ExceptionHandler(Exception.class)
	public void exceptionHandler(Exception ex, HttpServletRequest request,HttpServletResponse response) {
		ResponseResult<Object> result = new ResponseResult<Object>();		
		result.setSuccess(false);
		result.setMessage(ex.getMessage());
		try {
			response.getWriter().write(CoreUtils.toString(result));
		} catch (Exception e) {
			log.error("exceptionHandler:", e);
		}
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
		
}
