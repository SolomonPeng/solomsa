package org.solomsa.app.base;

import javax.servlet.http.HttpServletRequest;

import org.solomsa.core.vo.ResponseResult;
import org.springframework.web.bind.annotation.ExceptionHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseApi {

	@ExceptionHandler(Exception.class)
	public ResponseResult<Object> handlerCoreException(Exception ex, HttpServletRequest request) {
		ResponseResult<Object> result = new ResponseResult<Object>();
		result.setSuccess(false);
		//result.setErrorCode(500);
		result.setMessage(ex.getMessage());
		log.error("Handle api exception:", ex);
		return result;
	}
}
