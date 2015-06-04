/*
 *  Copyright 2015 LG CNS.
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

import java.util.HashSet;
import java.util.Set;

import scouter.agent.ClassDesc;
import scouter.agent.Logger;
import scouter.agent.trace.TraceMain;
import scouter.org.objectweb.asm.ClassVisitor;
import scouter.org.objectweb.asm.MethodVisitor;
import scouter.org.objectweb.asm.Opcodes;

public class CapContentLengthASM implements IASM, Opcodes {
	private Set<String> target = new HashSet<String>();

	public CapContentLengthASM() {
		target.add("org/apache/catalina/connector/Response");
	}

	public boolean isTarget(String className) {
	       return target.contains(className);
	}
	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {

		if (target.contains(className) == false)
			return cv;

		Logger.println("SA01",className + " capture ContentLength");
		return new CapContentsLengthCV(cv, className);
	}

}

// ///////////////////////////////////////////////////////////////////////////
class CapContentsLengthCV extends ClassVisitor implements Opcodes {
	private static Set<String> methods = new HashSet<String>();
	static {
		methods.add("setContentLength");
		methods.add("setStatus");
	}
	public String className;

	public CapContentsLengthCV(ClassVisitor cv, String className) {
		super(ASM4, cv);
		this.className = className;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		if (mv != null && methods.contains(name) && desc.equals("(I)V")) {
			return new CapContentsLengthMV(mv, name, desc);
		}
		return mv;

	}
}

// ///////////////////////////////////////////////////////////////////////////
class CapContentsLengthMV extends MethodVisitor implements Opcodes {
	private static final String CLASS = TraceMain.class.getName().replace('.', '/');
	// private static final String METHOD = "setContentLength";
	// private static final String SIGNATURE = "(I)V";

	private String METHOD_NAME;
	private String DESC;

	public CapContentsLengthMV(MethodVisitor mv, String name, String desc) {
		super(ASM4, mv);
		METHOD_NAME = name;
		this.DESC = desc;
	}

	@Override
	public void visitCode() {
		mv.visitVarInsn(Opcodes.ILOAD, 1);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, CLASS, METHOD_NAME, "(I)V");

		super.visitCode();
	}
}