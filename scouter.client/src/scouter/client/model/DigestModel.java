/*
 *  Copyright 2015 LG CNS.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 *
 */
package scouter.client.model;

import java.util.ArrayList;
import java.util.List;

public class DigestModel {
	
	public int objHash;
	public int digestHash;
	public String name;
	public String database;
	public int execution;
	public int errorCnt;
	public int warnCnt;
	public long sumResponseTime;
	public double avgResponseTime;
	public long minResponseTime = Long.MAX_VALUE;
	public long maxResponseTime = Long.MIN_VALUE;
	public long lockTime;
	public long rowsAffected;
	public long rowsSent;
	public long rowsExamined;
	public long createdTmpDiskTables;
	public long createdTmpTables;
	public long selectFullJoin;
	public long selectFullRangeJoin;
	public long selectRange;
	public long selectRangeCheck;
	public long selectScan;
	public long sortMergePasses;
	public long sortRange;
	public long sortRows;
	public long sortScan;
	public long noIndexUsed;
	public long noGoodIndexUsed;
	public long firstSeen = Long.MAX_VALUE;
	public long lastSeen = Long.MIN_VALUE;
	
	public DigestModel parent;
	private List<DigestModel> childList;
	
	public synchronized void addChild(DigestModel child) {
		if (this.database == null) {
			this.database = child.database;
		}
		this.execution += child.execution;
		this.errorCnt += child.errorCnt;
		this.warnCnt += child.warnCnt;
		this.sumResponseTime += child.sumResponseTime;
		if (this.maxResponseTime < child.maxResponseTime) {
			this.maxResponseTime = child.maxResponseTime;
		}
		if (this.minResponseTime > child.minResponseTime) {
			this.minResponseTime = child.minResponseTime;
		}
		this.lockTime += child.lockTime;
		this.rowsAffected += child.rowsAffected;
		this.rowsSent += child.rowsSent;
		this.rowsExamined += child.rowsExamined;
		this.createdTmpDiskTables += child.createdTmpDiskTables;
		this.createdTmpTables += child.createdTmpTables;
		this.selectFullJoin += child.selectFullJoin;
		this.selectFullRangeJoin += child.selectFullRangeJoin;
		this.selectRange += child.selectRange;
		this.selectRangeCheck += child.selectRangeCheck;
		this.selectScan += child.selectScan;
		this.sortMergePasses += child.sortMergePasses;
		this.sortRange += child.sortRange;
		this.sortRows += child.sortRows;
		this.sortScan += child.sortScan;
		this.noIndexUsed += child.noIndexUsed;
		this.noGoodIndexUsed += child.noGoodIndexUsed;
		if (this.firstSeen > child.firstSeen) {
			this.firstSeen = child.firstSeen;
		}
		if (this.lastSeen < child.lastSeen) {
			this.lastSeen = child.lastSeen;
		}
		if (this.childList == null) {
			this.childList = new ArrayList<DigestModel>();
		}
		this.childList.add(child);
	}
	
	public DigestModel[] getChildArray() {
		if (childList == null) return null;
		DigestModel[] array = new DigestModel[childList.size()];
		for (int i = 0; i < childList.size(); i ++) {
			array[i] = childList.get(i);
		}
		return array;
	}
}
