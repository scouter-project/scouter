package scouter.agent.counter.task;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

import oshi.SystemInfo;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.OperatingSystem;
import scouter.agent.Logger;
import scouter.agent.counter.CounterBasket;
import scouter.agent.counter.anotation.Counter;
import scouter.lang.counters.CounterConstants;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Net Usage, Disk Usage
 * author: gunlee01@gmail.com
 */


public class HostNetDiskPerf {
	SystemInfo si = new SystemInfo();
	OperatingSystem os = si.getOperatingSystem();
	HardwareAbstractionLayer hal = si.getHardware();

	static char ch_l = 'l';
	static char ch_o = 'o';
	private Set<String> fsExceptionOccured = new HashSet<String>();

	private static HashMap<String, Map<String, Long>> previousNetworkStats = new HashMap<String, Map<String, Long>>();
	private static HashMap<String, Map<String, Long>> previousDiskStats = new HashMap<String, Map<String, Long>>();
	private static final String RX_DELTA = "rxD";
	private static final String TX_DELTA = "txD";
	private static final String READ_DELTA = "rdD";
	private static final String WRITE_DELTA = "wrD";
	private static volatile long rxTotalBytesPerSec = 0L;
	private static volatile long txTotalBytesPerSec = 0L;
	private static volatile long readTotalBytesPerSec = 0L;
	private static volatile long writeTotalBytesPerSec = 0L;

	public static long getRxTotalBytesPerSec() {
		return HostNetDiskPerf.rxTotalBytesPerSec;
	}

	public static long getTxTotalBytesPerSec() {
		return HostNetDiskPerf.txTotalBytesPerSec;
	}

	public static long getReadTotalBytesPerSec() {
		return HostNetDiskPerf.readTotalBytesPerSec;
	}

	public static long getWriteTotalBytesPerSec() {
		return HostNetDiskPerf.writeTotalBytesPerSec;
	}

	@Counter(interval = 10000)
	public void process(CounterBasket pw) {
		try {
			netUsage(10);
			diskIO(10);
		} catch (Exception e) {
			Logger.println("A141", 10, "HostPerfProcess10s", e);
		}
	}

	private void netUsage(int checkIntervalSec) {
		try {
			InternetProtocolStats istat = os.getInternetProtocolStats();
			List<NetworkIF> nifs = hal.getNetworkIFs();

			long tmpRxTotal = 0L;
			long tmpTxTotal = 0L;

			for (int i = 0; i < nifs.size(); i++) {
				String name = nifs.get(i).getName();
				if(name.length() >= 2) {
					if(name.charAt(0) == ch_l && name.charAt(1) == ch_o) {
						continue;
					}
				}

				long bytesRecv = 0;
				long bytesSent = 0;
				try {
					bytesRecv = nifs.get(i).getBytesRecv();
					bytesSent = nifs.get(i).getBytesSent();
				} catch (Exception e) {
					// Ignore the exception when trying to stat network interface
					Logger.println("A143", 300, "Exception trying to stat network device " + name, e);
					continue;
				}

				Map<String, Long> netMap = new HashMap<String, Long>();
				netMap.put(CounterConstants.HOST_NET_RX_BYTES, bytesRecv);
				netMap.put(CounterConstants.HOST_NET_TX_BYTES, bytesSent);

				Map<String, Long> preMap = previousNetworkStats.get(name);

				if (preMap != null) {
					long rxDelta = (bytesRecv - preMap.get(CounterConstants.HOST_NET_RX_BYTES)) / checkIntervalSec; // per sec delta
					long txDelta = (bytesSent - preMap.get(CounterConstants.HOST_NET_TX_BYTES)) / checkIntervalSec; // per sec delta

					netMap.put(RX_DELTA, rxDelta);
					netMap.put(TX_DELTA, txDelta);

					tmpRxTotal += rxDelta;
					tmpTxTotal += txDelta;
				}
				previousNetworkStats.put(name, netMap);
			}
			rxTotalBytesPerSec = tmpRxTotal;
			txTotalBytesPerSec = tmpTxTotal;

		} catch (Exception se) {
			Logger.println("A144", 60, "Exception on net usage", se);
			rxTotalBytesPerSec = 0;
			txTotalBytesPerSec = 0;
		}
	}

	private void diskIO(int checkIntervalSec) {
		try {
			long tmpReadTotal = 0L;
			long tmpWriteTotal = 0L;

			List<HWDiskStore> ds = hal.getDiskStores();
			for (int i = 0; i < ds.size(); i++) {
				long readBytes = 0;
				long writeBytes = 0;
				try {
					readBytes = ds.get(i).getReadBytes();
					writeBytes = ds.get(i).getWriteBytes();
				} catch (Exception e) {
					if(!fsExceptionOccured.contains(ds.get(i).getName())) {
						// Ignore the exception when trying to stat file interface
						Logger.println("A145", 300, "Exception trying to stat file system device " + ds.get(i), e);
						fsExceptionOccured.add(ds.get(i).getName());
					}
					continue;
				}
				fsExceptionOccured.remove(ds.get(i).getName());
				Map<String, Long> fsMap = new HashMap<String, Long>();
				fsMap.put(CounterConstants.HOST_DISK_READ_BYTES, readBytes);
				fsMap.put(CounterConstants.HOST_DISK_WRITE_BYTES, writeBytes);

				Map<String, Long> preMap = previousDiskStats.get(ds.get(i).getName());

				if (preMap != null) {
					long readDelta = (readBytes - preMap.get(CounterConstants.HOST_DISK_READ_BYTES)) / checkIntervalSec; // per sec delta
					long writeDelta = (writeBytes - preMap.get(CounterConstants.HOST_DISK_WRITE_BYTES)) / checkIntervalSec; // per sec delta

					fsMap.put(READ_DELTA, readDelta);
					fsMap.put(WRITE_DELTA, writeDelta);

					tmpReadTotal += readDelta;
					tmpWriteTotal += writeDelta;
				}
				previousDiskStats.put(ds.get(i).getName(), fsMap);
			}

			readTotalBytesPerSec = tmpReadTotal;
			writeTotalBytesPerSec = tmpWriteTotal;

		} catch (Exception e) {
			Logger.println("A144", 60, "Exception on net usage", e);
			rxTotalBytesPerSec = 0;
			txTotalBytesPerSec = 0;
		}
	}

}
