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
import org.objectweb.asm.commons.LocalVariablesSorter;
import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.trace.TraceMain;

import java.util.HashSet;

public class HttpReactiveServiceASM implements IASM, Opcodes {
	public HashSet<String> handlers = new HashSet<String>();
//	public HashSet<String> handlersRes = new HashSet<String>();
	public HttpReactiveServiceASM() {
		handlers.add("org/springframework/web/reactive/DispatcherHandler");
		handlers.add("org/springframework/web/server/handler/FilteringWebHandler");
//		handlersRes.add("org/springframework/http/server/reactive/ReactorServerHttpResponse");
	}

	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
		if (Configure.getInstance()._hook_serivce_enabled == false) {
			return cv;
		}
		if (Configure.getInstance()._hook_reactive_enabled == false) {
			return cv;
		}
		if (handlers.contains(className)) {
			return new HttpReactiveServiceCV(cv, className);
		}
//		if (handlersRes.contains(className)) {
//			return new HttpReactiveServiceResCV(cv, className);
//		}
		return cv;
	}
}

class HttpReactiveServiceCV extends ClassVisitor implements Opcodes {
	private static String handler = "invokeHandler";
	private static String handler_sig = "(Lorg/springframework/web/server/ServerWebExchange;Ljava/lang/Object;)Lreactor/core/publisher/Mono;";

	private static String handler2 = "handle";
	private static String handler_sig2 = "(Lorg/springframework/web/server/ServerWebExchange;)Lreactor/core/publisher/Mono;";

	private static String loading = "<init>";
	private static String loading_class = "org/springframework/web/reactive/DispatcherHandler";

	private String className;

	public HttpReactiveServiceCV(ClassVisitor cv, String className) {
		super(ASM9, cv);
		this.className = className;
	}
	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		if (mv == null) {
			return mv;
		}

		if (desc.startsWith(handler_sig2) && handler2.equals(name) || desc.startsWith(handler_sig) && handler.equals(name)) {
			Logger.println("A103", "HTTP-REACTIVE " + className);
			return new HttpReactiveServiceMV(access, desc, mv);

		}
//		else if (loading.equals(name) && loading_class.equals(className)) {
//			Logger.println("A103", "HTTP-REACTIVE INIT" + className);
//			return new HttpReactiveInitMV(access, desc, mv);
//		}
		return mv;
	}
}

//class HttpReactiveInitMV extends LocalVariablesSorter implements Opcodes {
//	private static final String TRACEMAIN = TraceMain.class.getName().replace('.', '/');
//	private final static String START = "startReactiveInit";
//	private static final String START_SIGNATURE = "(Ljava/lang/Object;)V";
//
//	public HttpReactiveInitMV(int access, String desc, MethodVisitor mv) {
//		super(ASM9, access, desc, mv);
//	}
//
//	@Override
//	public void visitCode() {
//		mv.visitVarInsn(ALOAD, 0);
//		mv.visitTypeInsn(CHECKCAST, "java/lang/Object");
//		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, START, START_SIGNATURE, false);
//		mv.visitCode();
//	}
//}

class HttpReactiveServiceMV extends LocalVariablesSorter implements Opcodes {
	private static final String TRACEMAIN = TraceMain.class.getName().replace('.', '/');
	private final static String START = "startReactiveHttpService";
	private static final String START_SIGNATURE = "(Ljava/lang/Object;)V";
	private final static String START_RETURN = "startReactiveHttpServiceReturn";
	private final static String START_RETURN_SIGNATUER = "(Ljava/lang/Object;)Ljava/lang/Object;";

	//TODO private final static String REJECT = "reject";

	public HttpReactiveServiceMV(int access, String desc, MethodVisitor mv) {
		super(ASM9, access, desc, mv);
	}

	@Override
	public void visitCode() {
		mv.visitVarInsn(Opcodes.ALOAD, 1);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, START, START_SIGNATURE, false);
		mv.visitCode();
	}

	@Override
	public void visitInsn(int opcode) {
		if ((opcode >= IRETURN && opcode <= RETURN)) {
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, START_RETURN, START_RETURN_SIGNATUER, false);
			mv.visitTypeInsn(CHECKCAST, "reactor/core/publisher/Mono");
		}
		mv.visitInsn(opcode);
	}
}


//class HttpReactiveServiceResCV extends ClassVisitor implements Opcodes {
//	private static String method1 = "writeWithInternal";
//	private static String desc1 = "(Lorg/reactivestreams/Publisher;)Lreactor/core/publisher/Mono;";
//	private static String method2 = "writeAndFlushWithInternal";
//	private static String desc2 = "(Lorg/reactivestreams/Publisher;)Lreactor/core/publisher/Mono;";
//
//	private String className;
//
//	public HttpReactiveServiceResCV(ClassVisitor cv, String className) {
//		super(ASM9, cv);
//		this.className = className;
//	}
//	@Override
//	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
//		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
//		if (mv == null) {
//			return mv;
//		}
//
//		if (method1.equals(name) && desc.startsWith(desc1) || method2.equals(name) && desc.startsWith(desc2) ) {
//			Logger.println("A103", "HTTP-REACTIVE-RES " + className);
//			return new HttpReactiveServiceResMV(access, desc, mv);
//		}
//		return mv;
//	}
//}
//
//class HttpReactiveServiceResMV extends LocalVariablesSorter implements Opcodes {
//	private static final String TRACEMAIN = TraceMain.class.getName().replace('.', '/');
//	private final static String METHOD = "endReactiveHttpService";
//	private static final String METHOD_SIGNATURE = "()V";
//
//
//	public HttpReactiveServiceResMV(int access, String desc, MethodVisitor mv) {
//		super(ASM9, access, desc, mv);
//	}
//
//	@Override
//	public void visitCode() {
//		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, METHOD, METHOD_SIGNATURE, false);
//		mv.visitCode();
//	}
//}
