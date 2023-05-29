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
import scouter.agent.trace.TraceMain;

import java.util.HashSet;
import java.util.Set;

public class HttpServiceASM implements IASM, Opcodes {
	public Set<String> servlets = new HashSet<String>();
	public HttpServiceASM() {
		servlets.add("javax/servlet/http/HttpServlet");
		servlets.add("jakarta/servlet/http/HttpServlet");
		servlets.add("weblogic/servlet/jsp/JspBase");
	}

	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
		if (!Configure.getInstance()._hook_serivce_enabled) {
			return cv;
		}
		if (servlets.contains(className)) {
			return new HttpServiceCV(cv, className);
		}
		for (int i = 0; classDesc.interfaces != null && i < classDesc.interfaces.length; i++) {
			if ("javax/servlet/Filter".equals(classDesc.interfaces[i])) {
				return new HttpServiceCV(cv, className);
			}

			if ("jakarta/servlet/Filter".equals(classDesc.interfaces[i])) {
				return new HttpServiceCV(cv, className);
			}
		}
		return cv;
	}
}
class HttpServiceCV extends ClassVisitor implements Opcodes {
	private static final String TARGET_SERVICE = "service";
	private static final String TARGET_DOFILTER = "doFilter";
	private static final String TARGET_SIGNATURE = "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;";
	private static final String TARGET_SIGNATURE_JAKARTA = "(Ljakarta/servlet/ServletRequest;Ljakarta/servlet/ServletResponse;";
	private String className;
	public HttpServiceCV(ClassVisitor cv, String className) {
		super(ASM9, cv);
		this.className = className;
	}
	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		if (mv == null) {
			return mv;
		}
		if (desc.startsWith(TARGET_SIGNATURE) || desc.startsWith(TARGET_SIGNATURE_JAKARTA)) {
			if (TARGET_SERVICE.equals(name)) {
				Logger.println("A103", "HTTP " + className);
				return new HttpServiceMV(access, desc, mv, true);
			} else if (TARGET_DOFILTER.equals(name)) {
				Logger.println("A104", "FILTER " + className);
				return new HttpServiceMV(access, desc, mv, false);
			}
		}
		return mv;
	}
}
// ///////////////////////////////////////////////////////////////////////////
class HttpServiceMV extends LocalVariablesSorter implements Opcodes {
	private static final String TRACEMAIN = TraceMain.class.getName().replace('.', '/');
	private final static String START_SERVICE = "startHttpService";
	private final static String START_FILTER = "startHttpFilter";
	private static final String START_SIGNATURE = "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";
	private final static String END_METHOD = "endHttpService";
	private static final String END_SIGNATURE = "(Ljava/lang/Object;Ljava/lang/Throwable;)V";
	private final static String REJECT = "reject";
	private static final String REJECT_SIGNATURE = "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";
	private Label startFinally = new Label();
	private boolean httpservlet;
	public HttpServiceMV(int access, String desc, MethodVisitor mv, boolean httpservlet) {
		super(ASM9, access, desc, mv);
		this.httpservlet = httpservlet;
	}
	private int statIdx;
	@Override
	public void visitCode() {
		mv.visitVarInsn(Opcodes.ALOAD, 1);
		mv.visitVarInsn(Opcodes.ALOAD, 2);
		if (httpservlet) {
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, START_SERVICE, START_SIGNATURE, false);
		} else {
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, START_FILTER, START_SIGNATURE, false);
		}
		statIdx = newLocal(Type.getType(Object.class));
		mv.visitVarInsn(Opcodes.ASTORE, statIdx);
		mv.visitLabel(startFinally);
		mv.visitVarInsn(Opcodes.ALOAD, statIdx);
		mv.visitVarInsn(Opcodes.ALOAD, 1);
		mv.visitVarInsn(Opcodes.ALOAD, 2);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, REJECT, REJECT_SIGNATURE, false);
		Label end = new Label();
		mv.visitJumpInsn(IFNULL, end);
		mv.visitInsn(Opcodes.RETURN);
		mv.visitLabel(end);
		mv.visitCode();
	}
	@Override
	public void visitInsn(int opcode) {
		if ((opcode >= IRETURN && opcode <= RETURN)) {
			mv.visitVarInsn(Opcodes.ALOAD, statIdx);
			mv.visitInsn(Opcodes.ACONST_NULL);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, END_METHOD, END_SIGNATURE, false);
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
		mv.visitVarInsn(Opcodes.ALOAD, statIdx);
		mv.visitVarInsn(Opcodes.ALOAD, errIdx);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, END_METHOD, END_SIGNATURE, false);
		mv.visitInsn(ATHROW);
		mv.visitMaxs(maxStack + 8, maxLocals + 2);
	}
}
