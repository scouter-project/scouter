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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.lang.value.BooleanValue;
import scouter.lang.value.MapValue;
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
	public long gcTime = 0L;
	public long gcCount = 0L;

	public int sqlTotalCnt = 0;
	public long sqlTotalTime = 0L;
	public long sqlTotalRows = 0L;
	public long sqlTotalRuns = 0L;
	
	public boolean isStack = false;
	
	public long position  = 0L;
	
	public List<MapValue> sqlStats = null;
	public Map<Integer, String> uniqueSqls = null;
	
	
	// not variable 
	public int index = 0;
	
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
	
	public void writeSimple(DataOutputX out) throws IOException {
		DataOutputX o = new DataOutputX();
		writeInternal(o);
		out.writeBlob(o.toByteArray());
	}
	
	private void writeInternal(DataOutputX o) throws IOException {
		o.writeLong(startTime);		
		o.writeInt(objHash);
		o.writeText(batchJobId);
		o.writeText(args);
		o.writeInt(pID.intValue());
		
		o.writeLong(elapsedTime);
		o.writeInt(threadCnt);
		o.writeLong(cpuTime);
		o.writeLong(gcTime);
		o.writeLong(gcCount);
				
		o.writeInt(sqlTotalCnt);
		o.writeLong(sqlTotalTime);
		o.writeLong(sqlTotalRows);
		o.writeLong(sqlTotalRuns);
		
		o.writeBoolean(isStack);
		
		o.writeText(objName);
		o.writeText(objType);
		
		o.writeLong(position);
	}

	public void write(DataOutputX out) throws IOException {
		DataOutputX o = new DataOutputX();		
		writeInternal(o);
		
		if(sqlTotalCnt > 0){
			for(MapValue value: sqlStats){
				o.writeInt((int)value.getLong("hashValue"));
				o.writeInt((int)value.getLong("runs"));
				o.writeLong(value.getLong("startTime"));
				o.writeLong(value.getLong("endTime"));
				o.writeLong(value.getLong("totalTime"));
				o.writeLong(value.getLong("minTime"));
				o.writeLong(value.getLong("maxTime"));
				o.writeLong(value.getLong("processedRows"));
				o.writeBoolean(value.getBoolean("rowed"));		
			}
			
			for(Integer key : this.uniqueSqls.keySet()){
				o.writeInt(key.intValue());
				o.writeText(this.uniqueSqls.get(key));
			}
		}
		
		out.writeBlob(o.toByteArray());
	}


	public Pack readSimplePack(byte [] data) throws IOException {
		DataInputX d = new DataInputX(data);
		d.readByte(); // Type
		byte [] internalData = d.readBlob(); // Body
		d = new DataInputX(internalData);
		readInternal(d);
		return this;
	}
	
	public Pack readSimple(DataInputX din) throws IOException {
		DataInputX d = new DataInputX(din.readBlob());
		readInternal(d);
		return this;
	}

	private void readInternal(DataInputX d) throws IOException {
		this.startTime = d.readLong();		
		this.objHash = d.readInt();
		this.batchJobId = d.readText();
		this.args = d.readText();
		this.pID = d.readInt();
		
		this.elapsedTime = d.readLong();
		this.threadCnt = d.readInt();
		this.cpuTime = d.readLong();
		this.gcTime = d.readLong();
		this.gcCount = d.readLong();

		this.sqlTotalCnt = d.readInt();
		this.sqlTotalTime = d.readLong();
		this.sqlTotalRows = d.readLong();
		this.sqlTotalRuns = d.readLong();
		
		this.isStack = d.readBoolean();
		
		this.objName = d.readText();
		this.objType = d.readText();
		
		this.position = d.readLong();
	}
	
	public Pack read(DataInputX din) throws IOException {
		DataInputX d = new DataInputX(din.readBlob());
		readInternal(d);

		if(this.sqlTotalCnt > 0){
			this.sqlStats = new ArrayList<MapValue>((int)this.sqlTotalCnt);
			MapValue value;
			for(int i=0; i<this.sqlTotalCnt; i++){
				value = new MapValue();
				this.sqlStats.add(value);
				value.put("hashValue",(long)d.readInt());
				value.put("runs", (long)d.readInt());
				value.put("startTime", d.readLong());
				value.put("endTime", d.readLong());
				value.put("totalTime", d.readLong());
				value.put("minTime", d.readLong());
				value.put("maxTime", d.readLong());
				value.put("processedRows", d.readLong());
				value.put("rowed", new BooleanValue(d.readBoolean()));
			}
			
			this.uniqueSqls = new HashMap<Integer, String>(this.sqlTotalCnt);
			for(int i=0; i<this.sqlTotalCnt; i++){
				this.uniqueSqls.put(d.readInt(), d.readText());
			}
		}
		
		return this;
	}
}