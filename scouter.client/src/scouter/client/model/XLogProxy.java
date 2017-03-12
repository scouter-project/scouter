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
 *
 */
package scouter.client.model;

import scouter.client.net.INetReader;
import scouter.client.net.TcpProxy;
import scouter.client.preferences.PManager;
import scouter.client.preferences.PreferenceConstants;
import scouter.client.util.ConsoleProxy;
import scouter.client.xlog.XLogUtil;
import scouter.io.DataInputX;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.pack.XLogPack;
import scouter.lang.pack.XLogProfilePack;
import scouter.lang.step.Step;
import scouter.lang.value.DecimalValue;
import scouter.net.RequestCmd;
import scouter.util.FileUtil;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class XLogProxy {

	public static Step[] getProfile(String date, long txid, int serverId) {
		MapPack param = new MapPack();
		if (date != null) {
			param.put("date", date);
		}
		param.put("txid", new DecimalValue(txid));
		int max = PManager.getInstance().getInt(PreferenceConstants.P_MASS_PROFILE_BLOCK);
		param.put("max", new DecimalValue(max));
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {	
			Pack re = tcp.getSingle(RequestCmd.TRANX_PROFILE, param);		
			if (re == null){
				return null;
			}
			return Step.toObjects(((XLogProfilePack)re).profile);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		return null;
	}
	
	public static Step[] getFullProfile(String date, long txid, int max, int serverId) {
		
		System.err.println("TRANX_PROFILE_FULL: date="+date);
		final ArrayList<Step> arr = new ArrayList<Step>();
		
		MapPack param = new MapPack();
		if (date != null) {
			param.put("date", date);
		}
		param.put("txid", new DecimalValue(txid));
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			
			tcp.process(RequestCmd.TRANX_PROFILE_FULL, param, new INetReader() {
				public void process(DataInputX in) throws IOException {
					byte[] buff = in.readBlob();
					arr.addAll(Arrays.asList(Step.toObjects(buff)));
				}
			});
			
			return (Step[]) arr.toArray(new Step[0]);
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		return null;
	}
	public static byte[] getFullProfileByteArray(String date, long txid, int max, int serverId) {
		
		final ArrayList<Byte> arr = new ArrayList<Byte>();
		MapPack param = new MapPack();
		if (date != null) {
			param.put("date", date);
		}
		param.put("txid", new DecimalValue(txid));
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			
			tcp.process(RequestCmd.TRANX_PROFILE_FULL, param, new INetReader() {
				public void process(DataInputX in) throws IOException {
					byte[] buff = in.readBlob();
					for(byte b : buff){
						arr.add(b);
					}
				}
			});
			
			byte[] profile = new byte[arr.size()];
			for(int inx = 0 ; inx < arr.size() ; inx++){
				profile[inx] = arr.get(inx);
			}
			return profile;
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		return null;
	}
	
	public static boolean getHeapdumpByteArray(int objHash, String fileName, String localName, int serverId) {
		
		MapPack param = new MapPack();
		param.put("objHash", objHash);
		param.put("fileName", fileName);
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			final FileOutputStream fileOuputStream = new FileOutputStream(localName); 
			tcp.process(RequestCmd.OBJECT_DOWNLOAD_HEAP_DUMP, param, new INetReader() {
				public void process(DataInputX in) throws IOException {
					byte[] buff = in.readBlob();
						fileOuputStream.write(buff);
				}
			});
			FileUtil.close(fileOuputStream);
		} catch (Exception e) {
			ConsoleProxy.errorSafe(e.toString());
			return false;
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		return true;
	}

	public static XLogData getXLogData(int serverId, String date, long txid) {
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			MapPack param = new MapPack();
			param.put("date", date);
			param.put("txid", txid);
			Pack p = tcp.getSingle(RequestCmd.XLOG_READ_BY_TXID, param);
			if (p != null) {
				XLogPack xp = XLogUtil.toXLogPack(p);
				XLogData d = new XLogData(xp, serverId);
				d.objName = TextProxy.object.getLoadText(date, xp.objHash, serverId);
				d.serviceName = TextProxy.service.getLoadText(date, xp.service, serverId);
				return d;
			}
		} catch (Throwable th) {
			ConsoleProxy.errorSafe(th.toString());
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		return null;
	}
}
