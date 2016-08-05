/*
 *  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
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
 */

package scouter.lang.pack;

import java.io.IOException;

import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.util.DateUtil;
import scouter.util.Hexa32;

/**
 * Object that contains a stand-alone batch execution information
 */
public class BatchPack implements Pack {
	public int objHash;

	public String objName;
	public String objType;
	
	public String batchJobId;
	public String args;
	public Integer pID;
	
	public long startTime;
	public long elapsedTime = 0L;
	
	public int threadCnt = 0;
	public long cpuTime = 0L;

	public long sqlTotalCnt = 0L;
	public long sqlTotalTime = 0L;
	public long sqlTotalRows = 0L;
	public long sqlTotalRuns = 0L;

	public boolean isResult = false;
	public boolean isLog = false;
		
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Batch ");
		sb.append(DateUtil.timestamp(startTime));
		sb.append(" objHash=").append(Hexa32.toString32(objHash));
		sb.append(" JobId=").append(batchJobId);
		sb.append(" PID=").append(pID);
		sb.append(" elapsed=").append(elapsedTime);
		return sb.toString();
	}

	public byte getPackType() {
		return PackEnum.BATCH;
	}

	public void write(DataOutputX out) throws IOException {
		DataOutputX o = new DataOutputX();
		
		o.writeDecimal(startTime);		
		o.writeDecimal(objHash);
		o.writeText(batchJobId);
		o.writeText(args);
		o.writeInt(pID.intValue());
		
		o.writeLong(elapsedTime);
		o.writeInt(threadCnt);
		o.writeLong(cpuTime);
				
		o.writeLong(sqlTotalCnt);
		o.writeLong(sqlTotalTime);
		o.writeLong(sqlTotalRows);
		o.writeLong(sqlTotalRuns);
		
		o.writeBoolean(isResult);
		o.writeBoolean(isLog);
		
		o.writeText(objName);
		o.writeText(objType);
		
		out.writeBlob(o.toByteArray());
	}

	public Pack read(DataInputX din) throws IOException {

		DataInputX d = new DataInputX(din.readBlob());

		this.startTime = d.readDecimal();		
		this.objHash = (int)d.readDecimal();
		this.batchJobId = d.readText();
		this.args = d.readText();
		this.pID = d.readInt();
		
		this.elapsedTime = d.readLong();
		this.threadCnt = d.readInt();
		this.cpuTime = d.readLong();

		this.sqlTotalCnt = d.readLong();
		this.sqlTotalTime = d.readLong();
		this.sqlTotalRows = d.readLong();
		this.sqlTotalRuns = d.readLong();
		
		this.isResult = d.readBoolean();
		this.isLog = d.readBoolean();
		
		this.objName = d.readText();
		this.objType = d.readText();

		return this;
	}

}