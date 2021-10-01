package scouter.agent.asm.asyncsupport.spring;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.LocalVariablesSorter;
import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.asm.IASM;
import scouter.agent.asm.util.AsmUtil;
import scouter.agent.asm.util.HookingSet;
import scouter.agent.trace.TraceMain;

import java.util.ArrayList;
import java.util.List;

import static scouter.agent.asm.asyncsupport.spring.SpringAsyncExecutionASM.METHOD_DETERMINE_EXECUTOR;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 3. 9.
 */
public class
SpringAsyncExecutionASM implements IASM, Opcodes {
	private static List<String> asyncSubmitPatterns = new ArrayList<String>();

	private List<HookingSet> asyncExecutionTarget;

	private Configure conf = Configure.getInstance();
	public final static String METHOD_DETERMINE_EXECUTOR = "determineAsyncExecutor(Ljava/lang/reflect/Method;)Lorg/springframework/core/task/AsyncTaskExecutor;";

	static {
		asyncSubmitPatterns.add("org.springframework.aop.interceptor.AsyncExecutionAspectSupport.doSubmit(Ljava/util/concurrent/Callable;Lorg/springframework/core/task/AsyncTaskExecutor;Ljava/lang/Class;)Ljava/lang/Object;");
	}

	public SpringAsyncExecutionASM() {
		asyncExecutionTarget = HookingSet.getHookingMethodSet(HookingSet.buildPatterns(conf.hook_spring_async_submit_patterns, asyncSubmitPatterns));
	}

	@Override
	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
		if (conf.hook_spring_async_enabled == false) {
			return cv;
		}

		for (int i = 0; i < asyncExecutionTarget.size(); i++) {
			HookingSet mset = asyncExecutionTarget.get(i);
			if (mset.classMatch.include(className)) {
				return new SpringAsyncExecutionCV(cv, mset, className);
			}
		}

		return cv;
	}
}

class SpringAsyncExecutionCV extends ClassVisitor implements Opcodes {
	String className;
	HookingSet mset;

	public SpringAsyncExecutionCV(ClassVisitor cv, HookingSet mset, String className) {
		super(ASM9, cv);
		this.mset = mset;
		this.className = className;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		if (mv == null || mset.isA(name, desc) == false) {
			if(METHOD_DETERMINE_EXECUTOR.equals(name + desc)) {
				return new DetermineMV(access, name, desc, mv);
			}
			return mv;
		}
		if (AsmUtil.isSpecial(name)) {
			return mv;
		}

		return new SubmitMV(access, name, desc, mv);
	}
}

class SubmitMV extends LocalVariablesSorter implements Opcodes {
	private static final String TRACEMAIN = TraceMain.class.getName().replace('.', '/');
	private static final String START_METHOD = "springAsyncExecutionSubmit";
	private static final String START_SIGNATURE = "(Ljava/lang/Object;Ljava/util/concurrent/Callable;)V";

	String name;
	String desc;

	public SubmitMV(int access, String name, String desc, MethodVisitor mv) {
		super(ASM9, access, desc, mv);
		this.name = name;
		this.desc = desc;
	}

	@Override
	public void visitCode() {
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitVarInsn(Opcodes.ALOAD, 1);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, START_METHOD, START_SIGNATURE, false);
		mv.visitCode();
	}
}

class DetermineMV extends LocalVariablesSorter implements Opcodes {
	private static final String TRACEMAIN = TraceMain.class.getName().replace('.', '/');
	private static final String START_METHOD = "springAsyncDetermineExecutor";
	private static final String START_SIGNATURE = "(Ljava/lang/reflect/Method;)V";

	String name;
	String desc;

	public DetermineMV(int access, String name, String desc, MethodVisitor mv) {
		super(ASM9, access, desc, mv);
		this.name = name;
		this.desc = desc;
	}

	@Override
	public void visitCode() {
		mv.visitVarInsn(Opcodes.ALOAD, 1);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, START_METHOD, START_SIGNATURE, false);
		mv.visitCode();
	}
}
