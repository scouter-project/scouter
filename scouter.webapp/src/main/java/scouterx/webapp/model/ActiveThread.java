/*
 *  Copyright 2015 the original author or authors.
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package scouterx.webapp.model;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import scouter.lang.pack.MapPack;
import scouterx.webapp.model.enums.ActiveServiceMode;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 9. 9.
 */
@Data
public class ActiveThread {
	String txidName;
	long elapsed;
	String serviceName;
	String note;

	String mode;

	long threadId;
	String threadName;
	String threadStatus;

	long threadCpuTime;
	long threadUserTime;
	long blockCount;
	long blockTime;
	long waitedCount;
	long waitedTime;

	long lockOwnerId;
	String lockName;
	String lockOwnerName;

	String stackTrace;

	String sqlActiveBindVar;

	public static ActiveThread of(MapPack pack) {
		ActiveThread activeThread = new ActiveThread();
		activeThread.txidName = pack.getText("Service Txid");
		activeThread.elapsed = pack.getLong("Service Elapsed");
		activeThread.serviceName = pack.getText("Service Name");

		String sql = pack.getText("SQL");
		String sqlBindVar = pack.getText("SQLActiveBindVar");

		String subcall = pack.getText("Subcall");

		if (StringUtils.isNotEmpty(sql)) {
			activeThread.note = sql;
			activeThread.mode = ActiveServiceMode.SQL.name();
		} else if (StringUtils.isNotEmpty(subcall)) {
			activeThread.note = subcall;
			activeThread.mode = ActiveServiceMode.SUBCALL.name();
		} else {
			activeThread.mode = ActiveServiceMode.NONE.name();
		}
		if(StringUtils.isNotEmpty(sqlBindVar)){
			activeThread.sqlActiveBindVar= sqlBindVar;
		}
		activeThread.threadId = pack.getLong("Thread Id");
		activeThread.threadName = pack.getText("Thread Name");
		activeThread.threadStatus = pack.getText("State");

		activeThread.threadCpuTime = pack.getLong("Thread Cpu Time");
		activeThread.threadUserTime = pack.getLong("Thread User Time");
		activeThread.blockCount = pack.getLong("Blocked Count");
		activeThread.blockTime = pack.getLong("Blocked Time");
		activeThread.waitedCount = pack.getLong("Waited Count");
		activeThread.waitedTime = pack.getLong("Waited Time");

		activeThread.lockOwnerId = pack.getLong("Lock Owner Id");
		activeThread.lockName = pack.getText("Lock Name");
		activeThread.lockOwnerName = pack.getText("Lock Owner Name");

		activeThread.stackTrace = pack.getText("Stack Trace");

		return activeThread;
	}
}
