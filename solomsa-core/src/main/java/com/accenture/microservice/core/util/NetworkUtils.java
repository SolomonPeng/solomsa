/**
 * 
 */
package com.accenture.microservice.core.util;

import java.net.InetAddress;

/**
 * @author song.peng
 *
 */
public class NetworkUtils {

	public static String hostAddress() {
		try {
			return InetAddress.getLocalHost().toString();
		}catch(Exception ex) {
			return "Unknow HostAndAddress";
		}		
	}
}
