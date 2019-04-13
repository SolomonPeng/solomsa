/**
 * Copyright 2010-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.solomsa.data.component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.solomsa.data.domain.AuditLog;
import org.solomsa.data.repository.AuditLogRepository;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * <p> <b>DataChangedPersistence</b> 是持久化类. </p>
 * 
 * @since 2011-4-11
 * @author song.peng
 */
@Component
public class AuditLogPersistence implements InitializingBean {

	private final BlockingQueue<AuditLog> logQueue = new LinkedBlockingQueue<AuditLog>();

	@Autowired
	private AuditLogRepository auditLogRepository;

	private final AuditLog NULL_LOG = new AuditLog();

	private boolean running = true;

	public void start() {

		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				while (running) {
					AuditLog log = null;
					try {
						log = logQueue.take();
					} catch (InterruptedException e) {
						return;
					}
					if (log == NULL_LOG) {
						return;
					}
					if (log != null) {
						auditLogRepository.save(log);
					}
				}
			}
		}, "AuditLogPersistenceThread");
		t.setDaemon(true);
		t.start();
	}

	public void stop() {
		running = false;
		append(NULL_LOG);
	}

	/**
	 * @param log
	 */
	public void append(AuditLog log) {
		try {
			logQueue.put(log);
		} catch (InterruptedException e) {
			// ignore
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.start();
	}
}
