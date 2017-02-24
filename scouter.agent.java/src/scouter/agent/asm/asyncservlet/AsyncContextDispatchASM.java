package scouter.agent.asm.asyncservlet;

import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.asm.IASM;
import scouter.agent.asm.util.AsmUtil;
import scouter.agent.asm.util.HookingSet;
import scouter.agent.trace.TraceMain;
import scouter.org.objectweb.asm.ClassVisitor;
import scouter.org.objectweb.asm.MethodVisitor;
import scouter.org.objectweb.asm.Opcodes;
import scouter.org.objectweb.asm.commons.LocalVariablesSorter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 2. 24.
 */
public class AsyncContextDispatchASM implements IASM, Opcodes {
	private static List<String> preservedAsyncContextDispatchPatterns = new ArrayList<String>();
	static {
		preservedAsyncContextDispatchPatterns.add("org.apache.catalina.core.AsyncContextImpl.dispatch(Ljavax/servlet/ServletContext;Ljava/lang/String;)V");
	}

	private Configure conf = Configure.getInstance();
	private List<HookingSet> dispatchTarget;

	public AsyncContextDispatchASM() {
		//TODO add pattern configureation
		dispatchTarget = HookingSet.getHookingMethodSet(HookingSet.buildPatterns("zzz.zzz", preservedAsyncContextDispatchPatterns));
	}

	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
		if (conf._hook_async_servlet_enabled == false) {
			return cv;
		}

		for (int i = 0; i < dispatchTarget.size(); i++) {
			HookingSet mset = dispatchTarget.get(i);
			if (mset.classMatch.include(className)) {
				return new AsyncContextCV(cv, mset, className);
			}
		}

		return cv;
	}
}

class AsyncContextCV extends ClassVisitor implements Opcodes {
	String className;
	HookingSet mset;

	public AsyncContextCV(ClassVisitor cv, HookingSet mset, String className) {
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

		return new DispatchMV(access, desc, mv);
	}
}


class DispatchMV extends LocalVariablesSorter implements Opcodes {
	private static final String TRACEMAIN = TraceMain.class.getName().replace('.', '/');
	private static final String START_METHOD = "dispatchAsyncServlet";
	private static final String START_SIGNATURE = "(Ljava/lang/Object;Ljava/lang/String;)V";

	public DispatchMV(int access, String desc, MethodVisitor mv) {
		super(ASM4, access, desc, mv);
	}

	@Override
	public void visitCode() {
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitVarInsn(Opcodes.ALOAD, 2);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, START_METHOD, START_SIGNATURE, false);
	}
}
