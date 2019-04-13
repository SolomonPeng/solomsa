package org.solomsa.data;

import java.io.Serializable;
import java.util.Properties;
import java.util.UUID;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;


/**
 * 封装各种生成唯一性ID算法的工具类.
 * @author song.peng
 * @version 2017-08-24
 */
public class IdGenerator implements Configurable,IdentifierGenerator {

	
	public static final String CLASS = "org.solomsa.data.IdGenerator";
	public static final String NAME = "idGenerator";
	
	/**
	 * 封装JDK自带的UUID, 通过Random数字生成, 中间无-分割.
	 */
	

	@Override
	public void configure(Type type, Properties params, ServiceRegistry serviceRegistry) throws MappingException {
	}

	@Override
	public Serializable generate(SessionImplementor session, Object object) throws HibernateException {
		return UUID.randomUUID().toString().replaceAll("-", "");
	}

}
