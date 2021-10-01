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
package scouter.agent.asm;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;
import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.asm.util.AsmUtil;
import scouter.agent.asm.util.HookingSet;
import scouter.agent.trace.TraceSQL;

import java.util.Map;
public class JDBCDriverASM implements IASM, Opcodes {
	//user can define driver.connect()
	private Map<String, HookingSet> reserved =HookingSet.getHookingSet(Configure.getInstance().hook_jdbc_wrapping_driver_patterns);
	public JDBCDriverASM() {
		AsmUtil.add(reserved, "com/ibm/db2/jcc/DB2Driver",	"connect(Ljava/lang/String;Ljava/util/Properties;)Ljava/sql/Connection;");
	}

	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
		if (Configure.getInstance()._hook_dbconn_enabled == false) {
			return cv;
		}
		HookingSet mset = reserved.get(className);
		if (mset != null){
			return new JDBCDriverCV(cv, mset, className);
		}

		return cv;
	}
}
class JDBCDriverCV extends ClassVisitor implements Opcodes {
	public String className;
	private HookingSet mset;
	public JDBCDriverCV(ClassVisitor cv, HookingSet mset, String className) {
		super(ASM9, cv);
		this.mset = mset;
		this.className = className;
	}
	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		if (mv == null || mset.isA(name, desc) == false) {
			return mv;
		}
		if (AsmUtil.isSpecial(name)) {
			return mv;
		}
		String fullname = AsmUtil.makeMethodFullName(className, name, desc);
       
		Logger.println("A105", "jdbc db2 driver  loaded: " + fullname);
		return new JDBCDriverMV(access, desc, mv, fullname);
	}
}
// ///////////////////////////////////////////////////////////////////////////
class JDBCDriverMV extends LocalVariablesSorter implements Opcodes {
	private static final String TRACE_SQL = TraceSQL.class.getName().replace('.', '/');
	private final static String CONNECT_METHOD = "driverConnect";
	private static final String CONNECT_SIGNATURE = "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;";

	private static final String ERR_SIGNATURE = "(Ljava/lang/String;Ljava/lang/Throwable;)V";
	
	private Label startFinally = new Label();
	public JDBCDriverMV(int access, String desc, MethodVisitor mv, String fullname) {
		super(ASM9,access, desc, mv);
		this.strArgIdx = AsmUtil.getStringIdx(access, desc);
	}
	private int strArgIdx;
	@Override
	public void visitCode() {
		mv.visitLabel(startFinally);
		mv.visitCode();
	}
	@Override
	public void visitInsn(int opcode) {
		if ((opcode >= IRETURN && opcode <= RETURN)) {
			mv.visitVarInsn(Opcodes.ALOAD, strArgIdx);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE_SQL, CONNECT_METHOD, CONNECT_SIGNATURE,false);
		}
		mv.visitInsn(opcode);
	}
	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		Label endFinally = new Label();
		mv.visitTryCatchBlock(startFinally, endFinally, endFinally, null);
		mv.visitLabel(endFinally);
		mv.visitInsn(DUP);
		
		int errIdx = newLocal(Type.getType(Throwable.class));
		mv.visitVarInsn(Opcodes.ASTORE, errIdx);
		mv.visitVarInsn(Opcodes.ALOAD, strArgIdx);
		mv.visitVarInsn(Opcodes.ALOAD, errIdx);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE_SQL, CONNECT_METHOD, ERR_SIGNATURE,false);
		mv.visitInsn(ATHROW);
		mv.visitMaxs(maxStack + 8, maxLocals + 2);
	}
}
