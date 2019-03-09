package com.accenture.microservice.app.base;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.ExceptionHandler;

import com.accenture.microservice.core.vo.ResponseResult;

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
