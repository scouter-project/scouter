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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.asm.util.AsmUtil;
import scouter.agent.asm.util.HookingSet;
import scouter.agent.trace.TraceSQL;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;

public class JDBCGetConnectionASM implements IASM, Opcodes {
	private List<HookingSet> target = HookingSet.getHookingMethodSet(Configure.getInstance().hook_get_connection_patterns);
	private Map<String, HookingSet> reserved = new HashMap<String, HookingSet>();

	public JDBCGetConnectionASM() {
		//AsmUtil.add(reserved, "weblogic/jdbc/common/internal/RmiDataSource", "getConnection()Ljava/sql/Connection;");
	   
	}

	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
		if (Configure.getInstance()._hook_dbsql_enabled == false) {
			return cv;
		}
		
		HookingSet mset = reserved.get(className);
		if (mset != null)
			return new DataSourceCV(cv, mset, className);
		
		for (int i = 0; i < target.size(); i++) {
			mset = target.get(i);
			if (mset.classMatch.include(className)) {
				return new DataSourceCV(cv, mset, className);
			}
		}
		return cv;
	}
}

class DataSourceCV extends ClassVisitor implements Opcodes {

	public String className;
	private HookingSet mset;

	public DataSourceCV(ClassVisitor cv, HookingSet mset, String className) {
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
		return new DataSourceMV(access, desc, mv, className,name);
	}
}

// ///////////////////////////////////////////////////////////////////////////
class DataSourceMV extends LocalVariablesSorter implements Opcodes {
	private static final String TRACE_SQL = TraceSQL.class.getName().replace('.', '/');
	private final static String METHOD = "getConnection";
	private static final String SIGNATURE = "(Ljava/sql/Connection;)Ljava/sql/Connection;";
	
	private Type returnType;
	private String className;
	private String methodName;
	private String methodDesc; 

	public DataSourceMV(int access, String desc, MethodVisitor mv, String className, String methodName) {
		super(ASM9,access, desc, mv);
		this.returnType = Type.getReturnType(desc);
		this.className = className;
		this.methodName = methodName;
		this.methodDesc = desc;
	}
	

	@Override
	public void visitInsn(int opcode) {
		if ((opcode >= IRETURN && opcode <= RETURN)) {
			int i = newLocal(this.returnType);
			mv.visitVarInsn(ASTORE, i);
			mv.visitVarInsn(Opcodes.ALOAD, i);
			AsmUtil.PUSH(mv, className);
			AsmUtil.PUSH(mv, methodName);
			AsmUtil.PUSH(mv, methodDesc);
			mv.visitVarInsn(Opcodes.ALOAD, 0);
			
			mv.visitVarInsn(Opcodes.ALOAD, i);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE_SQL, METHOD, SIGNATURE,false);
			
		}
		mv.visitInsn(opcode);
	}
	
	
}
