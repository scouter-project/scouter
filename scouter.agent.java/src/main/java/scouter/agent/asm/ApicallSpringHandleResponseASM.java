/*
 *  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package scouter.agent.asm;

import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.asm.util.AsmUtil;
import scouter.agent.asm.util.HookingSet;
import scouter.agent.trace.TraceApiCall;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;

import java.util.HashMap;
import java.util.Map;

public class ApicallSpringHandleResponseASM implements IASM, Opcodes {

	private Map<String, HookingSet> reserved = new HashMap<String, HookingSet>();

	public ApicallSpringHandleResponseASM() {
		AsmUtil.add(reserved, "org/springframework/web/client/RestTemplate", "handleResponse(Ljava/net/URI;Lorg/springframework/http/HttpMethod;Lorg/springframework/http/client/ClientHttpResponse;)V");
		AsmUtil.add(reserved, "org/springframework/web/client/AsyncRestTemplate", "handleResponseError(Lorg/springframework/http/HttpMethod;Ljava/net/URI;Lorg/springframework/http/client/ClientHttpResponse;)V");
		AsmUtil.add(reserved, "org/springframework/web/client/AsyncRestTemplate$ResponseExtractorFuture", "convertResponse(Lorg/springframework/http/client/ClientHttpResponse;)Ljava/lang/Object;");
	}

	@Override
	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
		if (Configure.getInstance()._hook_apicall_enabled == false) {
			return cv;
		}

		HookingSet mset = reserved.get(className);
		if (mset != null)
			return new RestTemplateResponseHandlerCV(cv, mset, className);
		return cv;
	}
}

class RestTemplateResponseHandlerCV extends ClassVisitor implements Opcodes {
	public String className;
	private HookingSet mset;
	public RestTemplateResponseHandlerCV(ClassVisitor cv, HookingSet mset, String className) {
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
		return new RestTemplateResponseHandlerMV(access, methodName, desc, mv);
	}
}

class RestTemplateResponseHandlerMV extends LocalVariablesSorter implements Opcodes {
	private static final String TARGET = TraceApiCall.class.getName().replace('.', '/');
	private static final String START_METHOD = "setCalleeToCtxInSpringClientHttpResponse";
	private static final String START_SIGNATURE = "(Ljava/lang/Object;Ljava/lang/Object;)V";

	String name;
	String desc;
	int respIdx;

	public RestTemplateResponseHandlerMV(int access, String name, String desc, MethodVisitor mv) {
		super(ASM9, access, desc, mv);
		this.name = name;
		this.desc = desc;
		this.respIdx = AsmUtil.getIdxByType(access, desc, Type.getType("Lorg/springframework/http/client/ClientHttpResponse;"));
	}

	@Override
	public void visitCode() {
		mv.visitVarInsn(ALOAD, 0);
		mv.visitVarInsn(ALOAD, respIdx);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TARGET, START_METHOD, START_SIGNATURE, false);

		mv.visitCode();
	}
}
