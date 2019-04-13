package org.solomsa.core.util;

import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author song.peng
 *
 */
@Slf4j
public class NumberUtils {

	/**字符串转数字
	 * @param str
	 * @param targetClass
	 * @return
	 */
	public static <T extends Number> T fromString(String str,Class<T> targetClass) {
		if(!StringUtils.hasText(str)) {
			return null;
		}
		try {
			return org.springframework.util.NumberUtils.parseNumber(str.trim(), targetClass);
		}catch(Exception ex) {
			log.error("fromString error for "+str.trim(),ex);
			return null;
		}
		
	}
}

