package scouter.agent.asm;

import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.asm.util.AsmUtil;
import scouter.agent.asm.util.HookingSet;
import scouter.agent.trace.TraceMain;
import scouter.org.objectweb.asm.ClassVisitor;
import scouter.org.objectweb.asm.MethodVisitor;
import scouter.org.objectweb.asm.Opcodes;
import scouter.org.objectweb.asm.Type;
import scouter.org.objectweb.asm.commons.LocalVariablesSorter;
import scouter.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 2. 21.
 */
public class AsyncServletASM implements IASM, Opcodes {
	private static List<String> preservedAsyncServletStartPatterns = new ArrayList<String>();
	static {
		preservedAsyncServletStartPatterns.add("org.apache.catalina.connector.Request.startAsync(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;)Ljavax/servlet/AsyncContext;");
	}

	private Configure conf = Configure.getInstance();
	private List<HookingSet> target;

	public AsyncServletASM() {
		//TODO add pattern configureation
		String patterns = buildPatterns("zzz.zzz", preservedAsyncServletStartPatterns);
		target = HookingSet.getHookingMethodSet(patterns);
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
		if (conf._hook_async_servlet_enabled == false) {
			return cv;
		}
		if (target.size() == 0)
			return cv;

		for (int i = 0; i < target.size(); i++) {
			HookingSet mset = target.get(i);
			if (mset.classMatch.include(className)) {
				return new RequestStartAsyncCV(cv, mset, className);
			}
		}
		return cv;
	}
}

class RequestStartAsyncCV extends ClassVisitor implements Opcodes {
	public String className;
	private HookingSet mset;

	public RequestStartAsyncCV(ClassVisitor cv, HookingSet mset, String className) {
		super(ASM4, cv);
		this.mset = mset;
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

		return new StartAsyncMV(access, desc, mv);
	}
}

class StartAsyncMV extends LocalVariablesSorter implements Opcodes {
	private static final String TRACEMAIN = TraceMain.class.getName().replace('.', '/');
	private static final String END_METHOD = "endRequestAsyncStart";
	private static final String END_SIGNATURE = "(Ljava/lang/Object;)V";

	private Type returnType;

	public StartAsyncMV(int access, String desc, MethodVisitor mv) {
		super(ASM4, access, desc, mv);
		returnType = Type.getReturnType(desc);
	}

	@Override
	public void visitInsn(int opcode) {
		if ((opcode >= IRETURN && opcode <= RETURN)) {
			capReturn();
		}
		mv.visitInsn(opcode);
	}

	private void capReturn() {
		Type tp = returnType;
		if(!"javax/servlet/AsyncContext".equals(tp.getInternalName())) {
			return;
		}
		int i = newLocal(tp);
		mv.visitVarInsn(Opcodes.ASTORE, i);
		mv.visitVarInsn(Opcodes.ALOAD, i);
		mv.visitVarInsn(Opcodes.ALOAD, i);

		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, END_METHOD, END_SIGNATURE, false);
	}
}

