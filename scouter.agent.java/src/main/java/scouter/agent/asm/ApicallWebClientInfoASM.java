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

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.LocalVariablesSorter;
import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.asm.util.AsmUtil;
import scouter.agent.asm.util.HookingSet;
import scouter.agent.trace.TraceApiCall;

import java.util.HashMap;
import java.util.Map;

public class ApicallWebClientInfoASM implements IASM, Opcodes {

	private Map<String, HookingSet> reserved = new HashMap<String, HookingSet>();

	public ApicallWebClientInfoASM() {
		AsmUtil.add(reserved, "org/springframework/web/reactive/function/client/DefaultClientRequestBuilder$BodyInserterRequest",
				"writeTo(Lorg/springframework/http/client/reactive/ClientHttpRequest;" +
						"Lorg/springframework/web/reactive/function/client/ExchangeStrategies;)" +
						"Lreactor/core/publisher/Mono;");
	}

	@Override
	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
		Configure conf = Configure.getInstance();
		if (!conf._hook_apicall_enabled) {
			return cv;
		}
		if (conf._hook_reactive_enabled == false) {
			return cv;
		}

		HookingSet mset = reserved.get(className);
		if (mset != null)
			return new WebClientRequestBuilderBodyInserterCV(cv, mset, className);
		return cv;
	}
}

class WebClientRequestBuilderBodyInserterCV extends ClassVisitor implements Opcodes {
	public String className;
	private HookingSet mset;
	public WebClientRequestBuilderBodyInserterCV(ClassVisitor cv, HookingSet mset, String className) {
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
		return new RequestBuilderBodyInserterWriteToMV(access, methodName, desc, mv);
	}
}

class RequestBuilderBodyInserterWriteToMV extends LocalVariablesSorter implements Opcodes {
	private static final String TARGET = TraceApiCall.class.getName().replace('.', '/');
	private static final String START_METHOD = "webClientInfo";
	private static final String START_SIGNATURE = "(Ljava/lang/Object;Ljava/lang/Object;)V";

	String name;
	String desc;
	int respIdx;

	public RequestBuilderBodyInserterWriteToMV(int access, String name, String desc, MethodVisitor mv) {
		super(ASM9, access, desc, mv);
		this.name = name;
		this.desc = desc;
	}

	@Override
	public void visitCode() {
		mv.visitVarInsn(ALOAD, 0);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TARGET, START_METHOD, START_SIGNATURE, false);

		mv.visitCode();
	}
}
