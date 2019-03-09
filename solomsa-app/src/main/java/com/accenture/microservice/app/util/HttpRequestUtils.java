package com.accenture.microservice.app.util;

import javax.servlet.http.HttpServletRequest;

import org.springframework.util.StringUtils;

/**
 * <p> <b>HttpRequestUtils</b> 是HttpRequest的操作助手类. </p>
 * 
 * @author song.peng
 */
public final class HttpRequestUtils {

	private static final String AJAX_MARK_UP = "isAjaxRequest";

	public static final String AJAX_REQUEST_HEADER = "X-Requested-With";

	public static final String AJAX_REQUEST_VALUE = "XMLHttpRequest";

	public static final String API_REQUEST_VALUE = "OpenAPIRequest";

	private HttpRequestUtils() {
		// Noops
	}

	public static boolean isAjaxOrOpenAPIRequest(HttpServletRequest request) {
		String requestType = request.getHeader(AJAX_REQUEST_HEADER);
		return AJAX_REQUEST_VALUE.equals(requestType) || API_REQUEST_VALUE.equals(requestType);
	}

	public static boolean isAjaxRequest(HttpServletRequest request) {
		String requestType = request.getHeader(AJAX_REQUEST_HEADER);
		return AJAX_REQUEST_VALUE.equals(requestType);
	}

	public static boolean isOpenAPIRequest(HttpServletRequest request) {
		String requestType = request.getHeader(AJAX_REQUEST_HEADER);
		return API_REQUEST_VALUE.equals(requestType);
	}

	public static void markAsAjaxRequest(HttpServletRequest request) {
		if (request.getAttribute(AJAX_MARK_UP) == null) {
			request.setAttribute(AJAX_MARK_UP, Boolean.TRUE);
		}
	}

	public static boolean isMarkAsAjaxRequest(HttpServletRequest request) {
		return Boolean.TRUE == request.getAttribute(AJAX_MARK_UP);
	}

	public static String getRequestBrowser(HttpServletRequest request) {
		return request.getHeader("User-Agent");
	}

	public static String getRequestAddress(HttpServletRequest request) {
		String[] headNames = new String[] { "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP" };
		String ip = null;
		for (int i = 0; i < headNames.length; i++) {
			ip = request.getHeader(headNames[i]);
			if (!StringUtils.isEmpty(ip)) {
				break;
			}
		}
		return StringUtils.isEmpty(ip) ? request.getRemoteAddr() : ip;
	}
}
