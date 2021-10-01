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
import scouter.agent.asm.util.AsmUtil;
import scouter.agent.asm.util.HookingSet;
import scouter.agent.netio.data.DataProxy;
import scouter.agent.trace.TraceMain;
import scouter.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class MethodASM implements IASM, Opcodes {
	private static List<String> defaultPatterns = new ArrayList<String>();
	private static List<String> defaultExcludePatterns = new ArrayList<String>();

	private Configure conf = Configure.getInstance();
	private List<HookingSet> target;
	private List<HookingSet> excludeTarget;

	public MethodASM() {
		String patterns = buildPatterns(conf.hook_method_patterns, defaultPatterns);
		String excludPatterns = buildPatterns(conf.hook_method_exclude_patterns, defaultExcludePatterns);
		target = HookingSet.getHookingMethodSet(patterns);
		excludeTarget = HookingSet.getHookingMethodSet(excludPatterns);
	}

	public static void addPatterns(String methodPattern) {
		defaultPatterns.add(methodPattern);
	}

	public static void addExcludePatterns(String methodPattern) {
		defaultExcludePatterns.add(methodPattern);
	}

	private String buildPatterns(String patterns, List<String> patternsList) {
		for(int i=0; i<patternsList.size(); i++) {
			if(StringUtil.isNotEmpty(StringUtil.trim(patterns))) {
				patterns = patterns + "," + patternsList.get(i);
			} else {
				patterns = patternsList.get(i);
			}
		}
		return patterns;
	}

	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
		if (conf._hook_methods_enabled == false) {
			return cv;
		}
		if (target.size() == 0)
			return cv;

		if (conf.isIgnoreMethodClass(className))
			return cv;

		if (!conf.hook_method_anonymous_enable && classIsAnnon(className)) {
			return cv;
		}

		for (int i = 0; i < target.size(); i++) {
			HookingSet mset = target.get(i);
			if (mset.classMatch.include(className)) {
				return new MethodCV(cv, mset, excludeTarget, className);
			}
		}
		return cv;
	}

	private boolean classIsAnnon(String className) {
		int spIndex = className.lastIndexOf('$');
		if (spIndex > 0 && spIndex < className.length() - 1) {
			char dig = className.charAt(spIndex + 1);
			return dig >= 48 && dig <= 57;
		}
		return false;
	}
}

class MethodCV extends ClassVisitor implements Opcodes {

	public String className;
	private HookingSet mset;
	private List<HookingSet> excludeTarget;

	public MethodCV(ClassVisitor cv, HookingSet mset, List<HookingSet> excludeTarget, String className) {
		super(ASM9, cv);
		this.mset = mset;
		this.excludeTarget = excludeTarget;
		this.className = className;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		if (mv == null || mset.isA(name, desc) == false) {
			return mv;
		}
		if (AsmUtil.isSpecial(name)) {
			return mv;
		}
		
		// check exclude method set
		for (int i = 0; i < excludeTarget.size(); i++) {
			HookingSet excludeSet = excludeTarget.get(i);
			if (excludeSet.classMatch.include(className)) {
				if (excludeSet.isA(name, desc)) {
					return mv;
				}
			}
		}

		Configure conf = Configure.getInstance();
		boolean isPublic = conf.hook_method_access_public_enabled;
		boolean isProtected = conf.hook_method_access_protected_enabled;
		boolean isPrivate = conf.hook_method_access_private_enabled;
		boolean isNone = conf.hook_method_access_none_enabled;

		//lambda method
		if(conf.hook_method_lambda_enable && (access & Opcodes.ACC_SYNTHETIC) == Opcodes.ACC_SYNTHETIC && name.indexOf("lambda$") == 0) {
			//if lambda method with hook_method_lambda_enabled then go on without method accessor check
		} else { // non-lambda method
			switch (access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE)) {
				case Opcodes.ACC_PUBLIC:
					if (isPublic == false)
						return mv;
					break;
				case Opcodes.ACC_PROTECTED:
					if (isProtected == false)
						return mv;
					break;
				case Opcodes.ACC_PRIVATE:
					if (isPrivate == false)
						return mv;
					break;
				default:
					if (isNone == false)
						return mv;
					break;
			}
		}

		// check prefix, to ignore simple method such as getter,setter
		if (conf.isIgnoreMethodPrefix(name))
			return mv;

		String fullname = AsmUtil.makeMethodFullName(className, name, desc);
		int fullname_hash = DataProxy.sendMethodName(fullname);

		return new MethodMV(access, desc, mv, fullname, fullname_hash);
	}
}

class MethodMV extends LocalVariablesSorter implements Opcodes {
	private static final String TRACEMAIN = TraceMain.class.getName().replace('.', '/');
	private final static String START_METHOD = "startMethod";
	private static final String START_SIGNATURE = "(ILjava/lang/String;)Ljava/lang/Object;";
	private final static String END_METHOD = "endMethod";
	private static final String END_SIGNATURE = "(Ljava/lang/Object;Ljava/lang/Throwable;)V";

	private Label startFinally = new Label();

	public MethodMV(int access, String desc, MethodVisitor mv, String fullname, int fullname_hash) {
		super(ASM9, access, desc, mv);
		this.fullname = fullname;
		this.fullname_hash = fullname_hash;
	}

	private int fullname_hash;
	private String fullname;
	private int statIdx;

	@Override
	public void visitCode() {
		AsmUtil.PUSH(mv, fullname_hash);
		mv.visitLdcInsn(fullname);

		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, START_METHOD, START_SIGNATURE, false);

		statIdx = newLocal(Type.getType(Object.class));

		mv.visitVarInsn(Opcodes.ASTORE, statIdx);
		mv.visitLabel(startFinally);
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
