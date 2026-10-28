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

package scouter.agent.counter.task;

import scouter.agent.counter.CounterBasket;
import scouter.agent.counter.anotation.Counter;
import scouter.agent.counter.meter.MeterResource;
import scouter.lang.TimeTypeEnum;
import scouter.lang.counters.CounterConstants;
import scouter.lang.pack.PerfCounterPack;
import scouter.lang.value.DecimalValue;
import scouter.util.SysJMX;

public class GCInfo {

	public MeterResource gcCountInfo = new MeterResource();
	public MeterResource gcTimeInfo = new MeterResource();
	public MeterResource sGcCountInfo = new MeterResource();
	public MeterResource sGcTimeInfo = new MeterResource();
	public MeterResource fGcCountInfo = new MeterResource();
	public MeterResource fGcTimeInfo = new MeterResource();
	public MeterResource cpuTimeInfo = new MeterResource();

	private long[] oldGc = null;
    private long oldCpu=0;

	@Counter
	public void getGCInfo(CounterBasket pw) {

		long[] gcInfo = SysJMX.getCurrentProcGcInfo();

		if (oldGc == null) {
			oldGc = gcInfo;
			return;
		}

		long dCount = gcInfo[0] - oldGc[0];
		long dTime = gcInfo[1] - oldGc[1];
		long sCount = gcInfo[2] - oldGc[2];
		long sTime = gcInfo[3] - oldGc[3];
		long fCount = gcInfo[4] - oldGc[4];
		long fTime = gcInfo[5] - oldGc[5];
		oldGc = gcInfo;

		gcCountInfo.add(dCount);
		gcTimeInfo.add(dTime);
		sGcCountInfo.add(sCount);
		sGcTimeInfo.add(sTime);
		fGcCountInfo.add(fCount);
		fGcTimeInfo.add(fTime);

		PerfCounterPack p = pw.getPack(TimeTypeEnum.REALTIME);
		p.put(CounterConstants.JAVA_GC_COUNT, new DecimalValue(dCount));
		p.put(CounterConstants.JAVA_GC_TIME, new DecimalValue(dTime));
		p.put("MinorGcCount", new DecimalValue(sCount));
		p.put("MinorGcTime", new DecimalValue(sTime));
		p.put("MajorGcCount", new DecimalValue(fCount));
		p.put("MajorGcTime", new DecimalValue(fTime));

		p = pw.getPack(TimeTypeEnum.FIVE_MIN);
		p.put(CounterConstants.JAVA_GC_COUNT, new DecimalValue((long) gcCountInfo.getSum(300)));
		p.put(CounterConstants.JAVA_GC_TIME, new DecimalValue((long) gcTimeInfo.getSum(300)));
		p.put("MinorGcCount", new DecimalValue((long) sGcCountInfo.getSum(300)));
		p.put("MinorGcTime", new DecimalValue((long) sGcTimeInfo.getSum(300)));
		p.put("MajorGcCount", new DecimalValue((long) fGcCountInfo.getSum(300)));
		p.put("MajorGcTime", new DecimalValue((long) fGcTimeInfo.getSum(300)));

	}/*
	@Counter
	public void cpuTime(CounterBasket pw) {

		if(SysJMX.isProcessCPU()==false)
			return;
		
		long cpu = SysJMX.getProcessCPU();
        
		if (oldCpu<=0) {
			oldCpu = cpu;
			return;
		}

		long dTime = cpu - oldCpu;
		oldCpu = cpu;

		cpuTimeInfo.add(dTime);
		
		PerfCounterPack p = pw.getPack(TimeTypeEnum.REALTIME);
		p.put(CounterConstants.JAVA_CPU_TIME, new DecimalValue(dTime));

		p = pw.getPack(TimeTypeEnum.FIVE_MIN);
		p.put(CounterConstants.JAVA_CPU_TIME, new DecimalValue((long) cpuTimeInfo.getSum(300)));

	}
	
	*/
}