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
import scouter.agent.asm.util.HookingSet;
import scouter.agent.trace.TraceApiCall;

import java.util.HashMap;
import java.util.Map;

public class ApicallSpringHttpAccessorASM implements IASM, Opcodes {
	private Map<String, HookingSet> reserved = new HashMap<String, HookingSet>();
	public ApicallSpringHttpAccessorASM() {
		AsmUtil.add(reserved, "org/springframework/http/client/support/HttpAccessor", "createRequest(Ljava/net/URI;Lorg/springframework/http/HttpMethod;)Lorg/springframework/http/client/ClientHttpRequest;");
		AsmUtil.add(reserved, "org/springframework/http/client/support/AsyncHttpAccessor", "createAsyncRequest(Ljava/net/URI;Lorg/springframework/http/HttpMethod;)Lorg/springframework/http/client/AsyncClientHttpRequest;");
	}

	@Override
	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
		if (Configure.getInstance()._hook_apicall_enabled == false) {
			return cv;
		}

		HookingSet mset = reserved.get(className);
		if (mset != null)
			return new HttpAccessorCV(cv, mset, className);
		return cv;
	}
}

class HttpAccessorCV extends ClassVisitor implements Opcodes {
	public String className;
	private HookingSet mset;
	public HttpAccessorCV(ClassVisitor cv, HookingSet mset, String className) {
		super(ASM9, cv);
		this.mset = mset;
		this.className = className;
	}
	@Override
	public MethodVisitor visitMethod(int access, String methodName, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, methodName, desc, signature, exceptions);
		if (mv == null || mset.isA(methodName, desc) == false) {
			return mv;
		}
		if (AsmUtil.isSpecial(methodName)) {
			return mv;
		}
		return new CreateRequestMV(access, methodName, desc, mv);
	}
}

class CreateRequestMV extends LocalVariablesSorter implements Opcodes {
	private static final String TARGET = TraceApiCall.class.getName().replace('.', '/');
	private static final String END_METHOD = "endCreateSpringRestTemplateRequest";
	private static final String END_METHOD_DESC = "(" +
			"Ljava/lang/Object;" +
			"Ljava/lang/Object;" +
			")V";

	String name;
	String desc;
	private Type returnType;

	public CreateRequestMV(int access, String name, String desc, MethodVisitor mv) {
		super(ASM9, access, desc, mv);
		this.name = name;
		this.desc = desc;
		this.returnType = Type.getReturnType(desc);
	}

	@Override
	public void visitInsn(int opcode) {
		if ((opcode >= IRETURN && opcode <= RETURN)) {
			Type tp = returnType;
			int i = newLocal(tp);
			mv.visitVarInsn(Opcodes.ASTORE, i);
			mv.visitVarInsn(Opcodes.ALOAD, i);
			mv.visitVarInsn(Opcodes.ALOAD, 0);
			mv.visitVarInsn(Opcodes.ALOAD, i);

			mv.visitMethodInsn(Opcodes.INVOKESTATIC, TARGET, END_METHOD, END_METHOD_DESC, false);
		}
		mv.visitInsn(opcode);
	}
}
