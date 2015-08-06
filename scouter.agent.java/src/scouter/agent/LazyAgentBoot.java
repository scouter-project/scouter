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
 */

package scouter.agent;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;

import scouter.agent.asm.DbcOpenASM;
import scouter.agent.asm.HttpServiceASM;
import scouter.agent.asm.IASM;
import scouter.agent.asm.JDBCDriverASM;
import scouter.agent.asm.JDBCPreparedStatementASM;
import scouter.agent.asm.JDBCResultSetASM;
import scouter.agent.asm.JDBCStatementASM;
import scouter.agent.asm.JspServletASM;
import scouter.agent.asm.SocketASM;
import scouter.agent.counter.CounterExecutingManager;
import scouter.agent.netio.data.net.TcpRequestMgr;
import scouter.agent.netio.request.ReqestHandlingProxy;
import scouter.util.FileUtil;


public class LazyAgentBoot implements Runnable {
	public void run() {
		boot();
	}

	private static boolean booted = false;

	private static List<IASM> classes = new ArrayList<IASM>();
	static {
		classes.add( new HttpServiceASM());
		classes.add( new JDBCPreparedStatementASM());
		classes.add( new JDBCResultSetASM());
		classes.add( new JDBCStatementASM());
		classes.add(new DbcOpenASM());
		classes.add(new JspServletASM());
		classes.add(new JDBCDriverASM());
		classes.add(new SocketASM());
	}

	public synchronized static void boot() {
		if (booted)
			return;
		booted = true;
		
			
		/*Attach로 한 경우에만 사용
		 * JDBC설치 모드는 REDEFINED모드로 설정한다. 중첩 PSTMT에 대해서 SQL이 다르게 수집되는 현상이 발생할 수있다.
		 */
		Configure.JDBC_REDEFINED=true;
		
		CounterExecutingManager.load();
		ReqestHandlingProxy.load();
		
		Configure.getInstance().printConfig();
		TcpRequestMgr.getInstance();
		
		try {
			Instrumentation instr =JavaAgent.getInstrumentation();
			if(instr==null)
				return;
			
			List<Class> clsList = new ArrayList<Class>();
			List<byte[]> bytList = new ArrayList<byte[]>();

			Class[] loadedClass = instr.getAllLoadedClasses();
			for (int i = 0; i < loadedClass.length; i++) {
				String name = loadedClass[i].getName().replace('.', '/');
				for(IASM asm : classes){
					if(asm.isTarget(name)){
						Class cls = loadedClass[i];	
						byte[] buff = getByteCode(cls);
						clsList.add(cls);
						bytList.add(buff);
						if(clsList.size() >10){
							redefine(clsList, bytList);
						}
						break;
					}
				}
			}
			if (clsList.size() >= 0){
			     redefine(clsList, bytList);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}



	private static void redefine(List<Class> clsList, List<byte[]> bytList) throws Exception {
		ClassDefinition[] cd = new ClassDefinition[clsList.size()];
		for (int i = 0; i < clsList.size(); i++) {
			cd[i] = new ClassDefinition(clsList.get(i), bytList.get(i));
		}
		JavaAgent.getInstrumentation().redefineClasses(cd);
		clsList.clear();
		bytList.clear();
	}



	public static byte[] getByteCode(Class c) {

		String clsAsResource = "/" + c.getName().replace('.', '/').concat(".class");
		InputStream in = null;
		try {
			in = c.getResourceAsStream(clsAsResource);
			if (in == null) {
				return null;
			}
			ByteArrayOutputStream out = new ByteArrayOutputStream();

			byte[] buff = new byte[1024];
			int n = 0;
			while ((n = in.read(buff, 0, 1024)) >= 0) {
				out.write(buff, 0, n);
			}
			return out.toByteArray();
		} catch (Exception e) {
		} finally {
			FileUtil.close(in);
		}
		return null;
	}
}