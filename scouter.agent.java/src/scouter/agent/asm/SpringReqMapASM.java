/*
 *  Copyright 2015 the original author or authors.
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

import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.asm.util.AsmUtil;
import scouter.agent.trace.TraceMain;
import scouter.org.objectweb.asm.AnnotationVisitor;
import scouter.org.objectweb.asm.ClassVisitor;
import scouter.org.objectweb.asm.MethodVisitor;
import scouter.org.objectweb.asm.Opcodes;
import scouter.org.objectweb.asm.commons.LocalVariablesSorter;

public class SpringReqMapASM implements IASM, Opcodes {
	
	public boolean isTarget(String className) {
		return false;
	}

	Configure conf = Configure.getInstance();
	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
		if(conf.enable_spring_request ==false)
			return cv;
		if ("Lorg/springframework/stereotype/Controller;".equals(classDesc.anotation)) {
			return new SpringReqMapCV(cv, className);
		}
		return cv;
	}
}

// ///////////////////////////////////////////////////////////////////////////
class SpringReqMapCV extends ClassVisitor implements Opcodes {

	public String className;

	public SpringReqMapCV(ClassVisitor cv, String className) {
		super(ASM4, cv);
		this.className = className;
	}

	@Override
	public MethodVisitor visitMethod(int access, String methodName, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, methodName, desc, signature, exceptions);
		if (mv == null) {
			return mv;
		}
		if (AsmUtil.isSpecial(methodName)) {
			return mv;
		}
		return new SpringReqMapMV(className, access, methodName, desc, mv);
	}
	
}

class SpringReqMapMV extends LocalVariablesSorter implements Opcodes {
	private static final String TRACEMAIN = TraceMain.class.getName().replace('.', '/');
	private final static String START_METHOD = "setServiceName";
	private static final String START_SIGNATURE = "(Ljava/lang/String;)V";
	
	private String methodName;
	private String className;
	private boolean isHandler=false;

	public SpringReqMapMV(String className,int access, String methodName, String desc, MethodVisitor mv) {
		super(ASM4, access, desc, mv);
		this.methodName=methodName;
		this.className=className;
	}
	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
	    if("Lorg/springframework/web/bind/annotation/RequestMapping;".equals(desc)){
	         this.isHandler=true;
	    }
		return super.visitAnnotation(desc, visible);
	}
	@Override
	public void visitCode() {
		if(isHandler){
		    AsmUtil.PUSH(mv, className+"."+methodName);
		    mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, START_METHOD, START_SIGNATURE, false);
		}
		mv.visitCode();
	}
}