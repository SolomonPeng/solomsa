/**
 * 
 */
package org.solomsa.data.base;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.solomsa.app.context.AppContext;
import org.solomsa.core.GlobalConst;
import org.solomsa.core.util.CoreUtils;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Maps;

import lombok.Data;

/**表基类
 * @author song.peng
 *
 */
@MappedSuperclass
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AbstractAuditable<PK extends Serializable> implements Persistable<PK>{

	private static final long serialVersionUID = 1L;
	
	static final String transactionIdKey = "transactionId";
	static final String appContextClassPath = "com.accenture.microservice.app.context.AppContext";
	
	/** 用于存放后台程序计算需要保留的信息，譬如事务ID等 */
	@Column(name = "EXTEND", length = 500)
	@JsonIgnore
	private String extend;
	
	@Column(name = "CREATE_BY", nullable = false, length = 50, updatable=false)
	@CreatedBy
	private String createBy;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "CREATE_DATE", nullable = false, updatable=false)
	@CreatedDate
	private Date createDate;

	@Column(name = "UPDATE_BY", nullable = false, length = 50)
	@LastModifiedBy
	private String updateBy;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "UPDATE_DATE", nullable = false)
	@LastModifiedDate
	private Date updateDate;
	
	@Transient
	private String extUpdateBy;
	
	/* (non-Javadoc)
	 * @see org.springframework.data.domain.Persistable#getId()
	 */
	@Override
	public abstract PK getId();

	/* (non-Javadoc)
	 * @see org.springframework.data.domain.Persistable#isNew()
	 */
	@Override
	@JsonIgnore
	public boolean isNew() {
		return ObjectUtils.isEmpty(createDate);
	}
	
	public void setTransactionId(String transactionId) {		
		Map<String,Object> map = CoreUtils.toMap(extend);
		if(!StringUtils.hasText(getTransactionId())) {
			if(!StringUtils.hasText(transactionId)) {
				return;
			}
			map.put(transactionIdKey, transactionId);
		}else {
			map.remove(transactionIdKey);
			if(StringUtils.hasText(transactionId)) {
				map.put(transactionIdKey, transactionId);
			}
		}
		setExtend(CoreUtils.toString(map));
	}
	
	@JsonIgnore
	public String getTransactionId() {
		Map<String,Object> map = Maps.newHashMap();
		if(StringUtils.hasText(extend)) {
			map = CoreUtils.toMap(extend);
			if(!ObjectUtils.isEmpty(map)&&!ObjectUtils.isEmpty(map.get(transactionIdKey))) {
				return map.get(transactionIdKey).toString();
			}
		}		
		return null;
	}

	@PrePersist
	void prePersist() {
		if(!StringUtils.hasText(getCreateBy())) {
			setCreateBy(ClassUtils.isPresent(appContextClassPath, getClass().getClassLoader())
					?AppContext.getCurrentUser():GlobalConst.ANONYMOUS_USER);
		}
		setCreateDate(new Date());
		setUpdateBy(getCreateBy());
		setUpdateDate(getCreateDate());
	}
	
	@PreUpdate
	void preUpdate() {
		if(StringUtils.hasText(getExtUpdateBy())) {
			setUpdateBy(getExtUpdateBy());
		}else {
			setUpdateBy(ClassUtils.isPresent(appContextClassPath, getClass().getClassLoader())
					?AppContext.getCurrentUser():GlobalConst.ANONYMOUS_USER);
		}
		setUpdateDate(new Date());
	}
	
}
