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
import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.asm.util.AsmUtil;
import scouter.agent.asm.util.HookingSet;
import scouter.agent.trace.TraceMain;

import java.util.List;

public class UserExceptionASM implements IASM, Opcodes {
	String exceptionPatterns = HookingSet.classPattrensToMethodPatterns(Configure.getInstance().hook_exception_class_patterns, "<init>");
	String exceptionExcludePatterns = HookingSet.classPattrensToMethodPatterns(Configure.getInstance().hook_exception_exclude_class_patterns, "<init>");

	private  List<HookingSet> target = HookingSet.getHookingMethodSet(exceptionPatterns);
	private  List<HookingSet> excludeTarget = HookingSet.getHookingMethodSet(exceptionExcludePatterns);

	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
		for (int i = 0; i < target.size(); i++) {
			HookingSet mset = target.get(i);
			if (mset.classMatch.include(className)) {
				for (int j = 0; j < excludeTarget.size(); j++) {
					HookingSet excludeHookinSet = excludeTarget.get(j);
					if (excludeHookinSet.classMatch.include(className)) {
						return cv;
					}
				}
				return new UserExceptionCV(cv, mset, className);
			}
		}
		return cv;
	}
}

class UserExceptionCV extends ClassVisitor implements Opcodes {

	private HookingSet mset;
	private String className;

	public UserExceptionCV(ClassVisitor cv, HookingSet mset, String className) {
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
		return new UserExceptionConsturtorMV(className,  desc, mv);
	}
}

// ///////////////////////////////////////////////////////////////////////////
class UserExceptionConsturtorMV extends MethodVisitor implements Opcodes {
	private static final String CLASS = TraceMain.class.getName().replace('.', '/');
	private static final String METHOD = "endExceptionConstructor";
	private static final String SIGNATURE = "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;)V";

	private String className;
	private String methodDesc;

	public UserExceptionConsturtorMV(String classname, String methoddesc, MethodVisitor mv) {
		super(ASM9, mv);
		this.className = classname;
		this.methodDesc = methoddesc;
	}

	@Override
	public void visitInsn(int opcode) {
		if ((opcode >= IRETURN && opcode <= RETURN) || opcode == ATHROW) {
			AsmUtil.PUSH(mv, className);
			AsmUtil.PUSH(mv, methodDesc);
			mv.visitVarInsn(Opcodes.ALOAD, 0);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, CLASS, METHOD, SIGNATURE,false);
		}
		mv.visitInsn(opcode);
	}
}

