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

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;
import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.asm.util.AsmUtil;
import scouter.agent.trace.TraceSQL;

import java.util.HashSet;
import java.util.Set;

public class SqlMapASM implements IASM, Opcodes {
	public final HashSet<String> target = new HashSet<String>();
	public static Set<String> targetMethod = new HashSet<String>();
	public static String[] targetInf;

	static {
		targetMethod.add("update");
		targetMethod.add("delete");
		targetMethod.add("insert");
		targetMethod.add("queryForList");
		targetMethod.add("queryForMap");
		targetMethod.add("queryForObject");
		targetMethod.add("queryWithRowHandler");
	}

	public SqlMapASM() {
		target.add("org/springframework/orm/ibatis/SqlMapClientTemplate");
		targetInf = new String[] { "com/ibatis/sqlmap/client/SqlMapClient" };
	}

	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
		if (Configure.getInstance()._hook_dbsql_enabled == false) {
			return cv;
		}
		for (int i = 0; i < classDesc.interfaces.length; i++) {
			for (int j = 0; j < targetInf.length; j++) {
				if (targetInf[j].equals(classDesc.interfaces[i])) {
					return new SqlMapCV(cv, className);
				}
			}
		}
		if (target.contains(className)) {
			return new SqlMapCV(cv, className);
		}
		return cv;
	}
}

// ///////////////////////////////////////////////////////////////////////////
class SqlMapCV extends ClassVisitor implements Opcodes {

	public String className;

	public SqlMapCV(ClassVisitor cv, String className) {
		super(ASM9, cv);
		this.className = className;
	}

	@Override
	public MethodVisitor visitMethod(int access, String methodName, String desc, String signature,
			String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, methodName, desc, signature, exceptions);
		if (mv == null) {
			return mv;
		}
		if (SqlMapASM.targetMethod.contains(methodName) == false) {
			return mv;
		}

		return new SqlMapMV(access, desc, mv, Type.getArgumentTypes(desc), AsmUtil.isStatic(access), className,
				methodName, desc);
	}
}

// ///////////////////////////////////////////////////////////////////////////
class SqlMapMV extends LocalVariablesSorter implements Opcodes {
	private static final String CLASS = TraceSQL.class.getName().replace('.', '/');
	private static final String METHOD = "sqlMap";
	private static final String SIGNATURE = "(Ljava/lang/String;Ljava/lang/String;)V";

	private Type[] paramTypes;
	private boolean isStatic;
	private String methodName;

	public SqlMapMV(int access, String desc, MethodVisitor mv, Type[] paramTypes, boolean isStatic, String classname,
			String methodname, String methoddesc) {
		super(ASM9, access, desc, mv);
		this.paramTypes = paramTypes;
		this.isStatic = isStatic;
		this.methodName = methodname;
	}

	@Override
	public void visitCode() {

		AsmUtil.PUSH(mv, methodName);

		boolean flag = false;
		int sidx = isStatic ? 0 : 1;
		for (int i = 0; i < paramTypes.length; i++) {
			Type tp = paramTypes[i];
			if ("java/lang/String".equals(tp.getInternalName())) {
				mv.visitVarInsn(Opcodes.ALOAD, sidx);
				flag = true;
				break;
			}
			sidx += tp.getSize();
		}

		if (flag == false) {
			AsmUtil.PUSH(mv, "");
		}
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, CLASS, METHOD, SIGNATURE, false);
		super.visitCode();
	}
}
