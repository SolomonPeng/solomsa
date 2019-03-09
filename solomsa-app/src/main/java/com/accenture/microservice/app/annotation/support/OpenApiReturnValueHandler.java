/**
 * 
 */
package com.accenture.microservice.app.annotation.support;

import java.io.IOException;
import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.ui.Model;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;

import com.accenture.microservice.app.annotation.OpenApi;
import com.accenture.microservice.core.vo.ResponseResult;

/**
 * @author song.peng
 *
 */
public class OpenApiReturnValueHandler extends RequestResponseBodyMethodProcessor {

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return (AnnotatedElementUtils.hasAnnotation(returnType.getContainingClass(), OpenApi.class) ||
				returnType.hasMethodAnnotation(OpenApi.class));
	}

	/**
	 * @param messageConverters
	 */
	public OpenApiReturnValueHandler(List<HttpMessageConverter<?>> messageConverters) {
		super(messageConverters);
	}

	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest) throws IOException, HttpMediaTypeNotAcceptableException {
		mavContainer.setRequestHandled(true);
		Object newValue = returnValue;
		Class<?> returnParaType = returnType.getParameterType();
		if (!void.class.isAssignableFrom(returnParaType)) {
			// 不是Response、Map、Model等类型的返回值，需要包裹为ResponseResult类型
			if (!ResponseResult.class.isAssignableFrom(returnParaType) /*&& !Map.class.isAssignableFrom(returnParaType)*/
					&& !Model.class.isAssignableFrom(returnParaType)) {
				
				/*if (Page.class.isAssignableFrom(returnType.getParameterType())) {
					newValue = new PageResponse((Page<?>) returnValue);
				} else {
					newValue = new ExecuteResponse<Object>(returnValue);
				}*/
				ResponseResult<Object> result = new ResponseResult<Object>();
				result.setSuccess(true);
				result.setResponse(returnValue);
				newValue = result;
			}
			
			
			
			/*if (newValue == null) {
				newValue = new ExecuteResponse<Object>(returnValue);
			}*/

			writeWithMessageConverters(newValue, returnType, webRequest);
		}
	}
}
