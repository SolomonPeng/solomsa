/**
 * 
 */
package org.solomsa.core.log;

import org.solomsa.core.util.NetworkUtils;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * @author song.peng
 *
 */
public class HostAndAddressConverter extends ClassicConverter {

	/* (non-Javadoc)
	 * @see ch.qos.logback.core.pattern.Converter#convert(java.lang.Object)
	 */
	@Override
	public String convert(ILoggingEvent event) {
		return NetworkUtils.hostAddress();
	}

}
