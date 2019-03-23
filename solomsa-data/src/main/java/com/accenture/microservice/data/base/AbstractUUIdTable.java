/**
 * 
 */
package com.accenture.microservice.data.base;


import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import org.hibernate.annotations.GenericGenerator;

import com.accenture.microservice.data.IdGenerator;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author song.peng
 *
 */
@MappedSuperclass
@Data
@EqualsAndHashCode(callSuper=false)
public abstract class AbstractUUIdTable extends AbstractAuditable<String> {

	private static final long serialVersionUID = 1L;
	
	@Id
	@GenericGenerator(name = IdGenerator.NAME, strategy = IdGenerator.CLASS)
	@GeneratedValue(generator = IdGenerator.NAME)
	@Column(unique=true, nullable=false, length=64)
	private String id;
	
	/** 是否已删除标志 */
	@Transient
	private boolean deleted = false;
	
}
