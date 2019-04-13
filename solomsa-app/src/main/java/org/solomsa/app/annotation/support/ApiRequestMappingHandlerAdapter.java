/**
 * 
 */
package org.solomsa.app.annotation.support;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;

/**
 * @author song.peng
 *
 */
public class ApiRequestMappingHandlerAdapter extends RequestMappingHandlerAdapter {

	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();

		// 将OpenApiReturnValueHandler插入到RequestResponseBodyMethodProcessor前面
		List<HandlerMethodReturnValueHandler> returnValueHandlers = new ArrayList<HandlerMethodReturnValueHandler>();
		returnValueHandlers.addAll(this.getReturnValueHandlers());

		for (int i = 0; i < returnValueHandlers.size(); i++) {
			HandlerMethodReturnValueHandler handler = returnValueHandlers.get(i);
			if (handler instanceof RequestResponseBodyMethodProcessor) {
				returnValueHandlers.add(i, new OpenApiReturnValueHandler(getMessageConverters()));
				break;
			}
		}
		this.setReturnValueHandlers(returnValueHandlers);

	}
}

