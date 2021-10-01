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

package scouter.agent.asm.elasticsearch;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;
import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.asm.IASM;

import static scouter.agent.AgentCommonConstant.SCOUTER_ADDED_FIELD;

public class HttpNioEntityASM implements IASM, Opcodes {

	private Configure conf = Configure.getInstance();

	public HttpNioEntityASM() {
	}

	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
		if (conf._hook_elasticsearch_enabled == false) {
			return cv;
		}

		if ("org/apache/http/nio/entity/NByteArrayEntity".equals(className)
				|| "org/apache/http/nio/entity/NStringEntity".equals(className)) {
			return new HttpNioEntityCV(cv, className);
		}
		return cv;
	}
}

class HttpNioEntityCV extends ClassVisitor implements Opcodes {

	public String className;

	public HttpNioEntityCV(ClassVisitor cv, String className) {
		super(ASM9, cv);
		this.className = className;
	}

	boolean exist = false;
	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);
		if (!exist) {
			super.visitField(ACC_PUBLIC, SCOUTER_ADDED_FIELD, Type.getDescriptor(Object.class), null, null).visitEnd();
		}
	}
	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		if (name.equals(SCOUTER_ADDED_FIELD)) {
			exist = true;
			Logger.println("A901e", "fail to add the field " + name + " on " + className);
		}
		return super.visitField(access, name, desc, signature, value);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		if (mv == null) {
			return mv;
		}

		if ("<init>".equals(name) &&
				(desc.startsWith("([B") || desc.startsWith("(Ljava/lang/String;"))) {
			return new HttpNioEntityMV(access, desc, mv, className);
		}
		return mv;
	}
}

class HttpNioEntityMV extends LocalVariablesSorter implements Opcodes {
	private String className;

	public HttpNioEntityMV(int access, String desc, MethodVisitor mv, String className) {
		super(ASM9, access, desc, mv);
		this.className = className;
	}

	@Override
	public void visitInsn(int opcode) {
		if ((opcode >= IRETURN && opcode <= RETURN)) {
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitFieldInsn(PUTFIELD, className, SCOUTER_ADDED_FIELD, "Ljava/lang/Object;");
		}
		mv.visitInsn(opcode);
	}
}
