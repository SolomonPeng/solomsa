
package com.accenture.microservice.app.context;

import java.security.Principal;
import java.util.Locale;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Component;
import org.springframework.ui.context.Theme;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.support.RequestContextUtils;

import com.accenture.microservice.core.GlobalConst;



/**
 * <p> <b>ApplicationContext.java</b>是使用spring的bean context </p>
 * 
 * @since 2010-1-5
 * @author song.peng
 */
@Component
public final class AppContext implements ApplicationContextAware {

	private AppContext() {
		//Noops
	}

	/** container */
	private static ApplicationContext container;

	/** web容器 */
	private static ServletContext servletContext;

	/**
	 * 获取bean
	 * 
	 * @param name
	 * @return Object
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getBean(String name) {
		try {
			return (T) getContainer().getBean(name);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 获取bean
	 * 
	 * @param name
	 * @param clz
	 * @return Object
	 */
	public static <T> T getBean(String name, Class<T> clz) {
		return getContainer().getBean(name, clz);
	}

	/**
	 * 获取bean
	 * 
	 * @param clz
	 * @return Object
	 */
	public static <T> T getBean(Class<T> clz) {
		return getContainer().getBean(clz);
	}

	/**
	 * 获取WEB应用上下文
	 * 
	 * @return ServletContext
	 */
	public static ServletContext getServletContext() {
		return servletContext;
	}

	/**
	 * 设置WEB应用上下文
	 * 
	 * @param servletContext
	 */
	public static void setServletContext(ServletContext servletContext) {
		AppContext.servletContext = servletContext;
	}

	/**
	 * 获取当前HTTP请求的Request，只有在HTTP请求线程中调用生效
	 * 
	 * @return
	 */
	public static HttpServletRequest getRequest() {
		ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if (null != attrs) {
			return attrs.getRequest();
		}
		return null;
	}

	/**
	 * 获取当前HTTP请求的SESSION，只在HTTP请求线程中生效
	 * 
	 * @return
	 */
	public static HttpSession getSession() {
		HttpServletRequest req = getRequest();
		if (null != req) {
			return req.getSession(false);
		}
		return null;
	}

	/**
	 * 获取i18n字符串，如果不存在则原样返回，Locale是采用用户信息中的， 如果不存在，则使用系统默认
	 * 
	 * @param code
	 *            i18n的编码
	 * @return i18n字符串
	 */
	public static String getMessage(String code) {
		return getMessage(code, (Object[]) null);
	}

	/**
	 * 获取i18n字符串，如果不存在则原样返回，Locale是由参数指定.
	 * 
	 * @param code
	 *            i18n的编码
	 * @param locale
	 *            指定的地区信息
	 * @return i18n字符串
	 */
	public static String getMessage(String code, Locale locale) {
		return getMessage(code, null, locale);
	}

	/**
	 * 获取i18n字符串，如果不存在则原样返回，Locale是采用用户信息中的， 如果不存在，则使用系统默认
	 * 
	 * @param code
	 *            i18n的编码
	 * @param args
	 *            参数值
	 * @return i18n字符串
	 */
	public static String getMessage(String code, Object[] args) {
		Locale locale = getCurrentUserLocale();
		if (null == locale) {
			// 获取操作系统默认的地区
			locale = Locale.getDefault();
		}
		return getMessage(code, args, code, locale);
	}

	/**
	 * 获取i18n字符串，如果不存在则原样返回，Locale是由参数指定.
	 * 
	 * @param code
	 *            i18n的编码
	 * @param locale
	 *            指定的地区信息
	 * @param args
	 *            参数值
	 * @param locale
	 * @return i18n字符串
	 */
	public static String getMessage(String code, Object[] args, Locale locale) {
		return getMessage(code, args, code, locale);
	}

	/**
	 * 获取i18n字符串
	 * 
	 * @param code
	 *            i18n的编码
	 * @param args
	 *            参数值
	 * @param defaultMessage
	 *            如果找不到code对应的i18n信息，则使用该默认信息
	 * @param locale
	 *            地区编码
	 * @return i18n字符串
	 */
	public static String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
		if (getContainer() == null) {
			return code;
		}
		try {
			return getContainer().getMessage(code, args, defaultMessage, locale);
		} catch (NoSuchMessageException ex) {
			return code;
		}
	}

	/**
	 * 获取i18n字符串
	 * 
	 * @param messagesourceresolvable
	 * @param locale
	 *            地区编码
	 * @return i18n字符串
	 */
	public static String getMessage(MessageSourceResolvable messagesourceresolvable, Locale locale) {
		if (getContainer() == null) {
			return messagesourceresolvable.getDefaultMessage();
		}
		return getContainer().getMessage(messagesourceresolvable, locale);
	}

	public static String getMessage(MessageSourceResolvable messagesourceresolvable) {
		Locale locale = getCurrentUserLocale();
		if (null == locale) {
			// 获取操作系统默认的地区
			locale = Locale.getDefault();
		}
		return getMessage(messagesourceresolvable, locale);
	}

	/**
	 * 获取当前用户的Locale，需要在Servlet环境下使用
	 * 
	 * @return 返回当前用户的Locale,否则返回NULL
	 */
	public static Locale getCurrentUserLocale() {
		Locale locale = null;
		// 尝试获取用户的地区
		HttpServletRequest request = getRequest();
		if (null != request) {
			locale = RequestContextUtils.getLocale(request);
		}
		return locale;
	}

	/**
	 * 获取当前用户的主题，需要在Servlet环境下使用
	 * 
	 * @return 返回当前用户的主题，否则返回NULL
	 */
	public static Theme getCurrentUserTheme() {
		Theme theme = null;
		// 尝试获取用户的主题
		HttpServletRequest request = getRequest();
		if (null != request) {
			theme = RequestContextUtils.getTheme(request);
		}
		return theme;
	}

	/**
	 * 设置当前系统使用的container
	 * 
	 * @param c
	 */
	public static void setContainer(org.springframework.context.ApplicationContext c) {
		container = c;
	}

	public static synchronized org.springframework.context.ApplicationContext getContainer() {
		if (container == null) {
			ServletContext ctx = AppContext.getServletContext();
			if(ctx == null && getRequest()!=null) {
				ctx = getRequest().getServletContext();
			}
			if (ctx != null) {
				container = WebApplicationContextUtils.getRequiredWebApplicationContext(ctx);
			}
		}
		return container;
	}

	@Override
	public void setApplicationContext(org.springframework.context.ApplicationContext applicationContext)
			throws BeansException { //NOSONAR
		container = applicationContext; //NOSONAR
	}

	public static void publishApplicationEvent(final ApplicationEvent event) {
		if (event != null && getContainer() != null) {
			container.publishEvent(event);
		}
	}
	
	public static String getCurrentUser() {
		HttpServletRequest request = getRequest();
        if (null != request) {
			Principal principal = request.getUserPrincipal();
			if (null != principal) {
				return principal.getName();
			}
		}        
		return GlobalConst.ANONYMOUS_USER;
	}
}
