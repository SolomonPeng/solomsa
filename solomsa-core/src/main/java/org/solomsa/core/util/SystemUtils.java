/**
 * 
 */
package org.solomsa.core.util;

import org.springframework.util.StringUtils;

/**
 * @author song.peng
 *
 */
public class SystemUtils {
	
	public static boolean isOS(String os) {
		String osName = System.getProperties().getProperty("os.name");//org.apache.commons.lang3.SystemUtils.OS_NAME;
		return StringUtils.hasText(osName)&&osName.toLowerCase().indexOf(os.toLowerCase())>-1;
	}
	
	public static boolean isLinux() {
		return isOS("Linux");
	}
	
	public static boolean isWindows() {
		return isOS("Windows");
	}
	
	public static boolean isUnix() {
		return isLinux()||isOS("Mac")||isOS("AIX")||isOS("HP-UX")||isOS("Irix")||isOS("Solaris")||isOS("SunOS");
	}
}
