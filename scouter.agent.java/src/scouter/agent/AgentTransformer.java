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
package scouter.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import scouter.agent.asm.ApicallASM;
import scouter.agent.asm.ApicallInfoASM;
import scouter.agent.asm.CapArgsASM;
import scouter.agent.asm.CapReturnASM;
import scouter.agent.asm.CapThisASM;
import scouter.agent.asm.JDBCConnectionOpenASM;
import scouter.agent.asm.FutureTaskASM;
import scouter.agent.asm.HttpServiceASM;
import scouter.agent.asm.IASM;
import scouter.agent.asm.JDBCDriverASM;
import scouter.agent.asm.JDBCPreparedStatementASM;
import scouter.agent.asm.JDBCResultSetASM;
import scouter.agent.asm.JDBCStatementASM;
import scouter.agent.asm.JspServletASM;
import scouter.agent.asm.MethodASM;
import scouter.agent.asm.ScouterClassWriter;
import scouter.agent.asm.ServiceASM;
import scouter.agent.asm.SocketASM;
import scouter.agent.asm.SpringReqMapASM;
import scouter.agent.asm.SqlMapASM;
import scouter.agent.asm.UserTxASM;
import scouter.agent.asm.util.AsmUtil;
import scouter.agent.util.AsyncRunner;
import scouter.lang.conf.ConfObserver;
import scouter.org.objectweb.asm.AnnotationVisitor;
import scouter.org.objectweb.asm.Attribute;
import scouter.org.objectweb.asm.ClassReader;
import scouter.org.objectweb.asm.ClassVisitor;
import scouter.org.objectweb.asm.ClassWriter;
import scouter.org.objectweb.asm.Opcodes;
import scouter.org.objectweb.asm.TypePath;
import scouter.util.FileUtil;
import scouter.util.IntSet;

public class AgentTransformer implements ClassFileTransformer {
	public static ThreadLocal<ClassLoader> hookingCtx = new ThreadLocal<ClassLoader>();
	private static List<IASM> asms = new ArrayList<IASM>();
	
	//hook 관련 설정이 변경되면 자동으로 변경된다. 
	private static int hook_signature;
	static {
		final Configure conf = Configure.getInstance();
		reload();
		hook_signature=conf.getHookSignature();
		ConfObserver.add("AgentTransformer", new Runnable(){
			 public void run() {
				if(conf.getHookSignature() !=hook_signature){
					reload();
				}
				hook_signature=conf.getHookSignature();
			}
		 });
		 
	}
	
	public static void reload() {
		Configure conf = Configure.getInstance();
		
		List<IASM> temp = new ArrayList<IASM>();
		
		if (conf.enable_hook_service) {
			temp.add(new HttpServiceASM());
			temp.add(new ServiceASM());
		}
		
		if (conf.enable_hook_dbsql) {
			temp.add(new JDBCPreparedStatementASM());
			temp.add(new JDBCResultSetASM());
			temp.add(new JDBCStatementASM());
			temp.add(new SqlMapASM());			
			temp.add(new UserTxASM());			
		}
	
		if (conf.enable_hook_dbconn) {
			temp.add(new JDBCConnectionOpenASM());
			temp.add(new JDBCDriverASM());
		}
		
		if (conf.enable_hook_cap) {
			temp.add(new CapArgsASM());
			temp.add(new CapReturnASM());
			temp.add(new CapThisASM());
		}
		
		if (conf.enable_hook_methods) {
			temp.add(new MethodASM());
			temp.add(new ApicallASM());
			temp.add(new ApicallInfoASM());
			temp.add(new SpringReqMapASM());
		}
		
		if (conf.enable_hook_socket) {
			temp.add(new SocketASM());
		}
	
		if (conf.enable_hook_jsp) {
			temp.add(new JspServletASM());
		}

		if (conf.enable_hook_future) {
			temp.add(new FutureTaskASM());
		}
		
		asms = temp;
	}
	
	// //////////////////////////////////////////////////////////////
	// boot class이지만 Hooking되어야하는 클래스를 등록한다.
	private static IntSet asynchook = new IntSet();
	static {
		asynchook.add("sun/net/www/protocol/http/HttpURLConnection".hashCode());
		asynchook.add("sun/net/www/http/HttpClient".hashCode());
		asynchook.add("java/net/Socket".hashCode());
	}

	private Configure conf = Configure.getInstance();
	private Logger.FileLog bciOut;
	public byte[] transform(ClassLoader loader, String className, Class classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

		try {
			hookingCtx.set(loader);
            if(className==null)
            	return null;
			if (classBeingRedefined == null) {
				if (asynchook.contains(className.hashCode())) {
					AsyncRunner.getInstance().add(loader, className, classfileBuffer);
					return null;
				}
				if (loader == null) {
					return null;
				}
			}
			if (className.startsWith("scouter/")) {
				return null;
			}
            //
			classfileBuffer=DirectPatch.patch(className, classfileBuffer);
			
			ObjTypeDetector.check(className);

			final ClassDesc classDesc = new ClassDesc();
			ClassReader cr = new ClassReader(classfileBuffer);
			cr.accept(new ClassVisitor(Opcodes.ASM4) {
				public void visit(int version, int access, String name, String signature, String superName,
						String[] interfaces) {
					classDesc.set(version, access, name, signature, superName, interfaces);
				}
		
				@Override
				public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
					classDesc.anotation += desc;
					return super.visitAnnotation(desc, visible);
				}
				
			}, 0);

			if(AsmUtil.isInterface(classDesc.access)){
				return null;
			}
			classDesc.classBeingRedefined=classBeingRedefined;
			ClassWriter cw = getClassWriter(classDesc);

			ClassVisitor cv = cw;
			List<IASM> workAsms = asms;
			for (int i = 0, max = workAsms.size(); i < max; i++) {
				cv = workAsms.get(i).transform(cv, className, classDesc);
				if (cv != cw) {
					cr = new ClassReader(classfileBuffer);
					cr.accept(cv, ClassReader.EXPAND_FRAMES);
					classfileBuffer = cw.toByteArray();
					cv = cw = getClassWriter(classDesc);
					
					if(conf.debug_asm){
						if(this.bciOut==null){
							this.bciOut=new Logger.FileLog("./scouter.bci");
						}
						this.bciOut.println(className + "\t\t["+loader+"]");
					}
				}
			}
			return classfileBuffer;
		} catch (Throwable t) {
			Logger.println("A101","Transformer Error",t);
			t.printStackTrace();
		} finally {
			hookingCtx.set(null);
		}

		return null;
	}

	

	private ClassWriter getClassWriter(final ClassDesc classDesc) {
		ClassWriter cw;
		switch (classDesc.version) {
		case Opcodes.V1_1:
		case Opcodes.V1_2:
		case Opcodes.V1_3:
		case Opcodes.V1_4:
		case Opcodes.V1_5:
		case Opcodes.V1_6:
			cw = new ScouterClassWriter(ClassWriter.COMPUTE_MAXS);
			break;
		default:
			cw = new ScouterClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		}
		return cw;
	}

	private void dump(String className, byte[] bytes) {
		String fname = "/tmp/" + className.replace('/', '_');
		FileUtil.save(fname, bytes);

	}

}
