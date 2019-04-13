package org.solomsa.core.vo;

import lombok.Data;

@Data
public class ResponseResult<T> {

	T response;
	boolean success;
	String message;
	Integer errorCode;
	
	
}
