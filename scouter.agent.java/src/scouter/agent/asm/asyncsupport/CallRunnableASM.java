package scouter.agent.asm.asyncsupport;

import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.asm.IASM;
import scouter.agent.trace.TraceMain;
import scouter.org.objectweb.asm.*;
import scouter.org.objectweb.asm.commons.LocalVariablesSorter;
import scouter.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 2. 21.
 */
public class CallRunnableASM implements IASM, Opcodes {

	private Configure conf = Configure.getInstance();
	private static final String CALLABLE = "java/util/concurrent/Callable";
	private static final String RUNNABLE = "java/lang/Runnable";
	private static List<String> scanScopePrefix = new ArrayList<String>();


	public CallRunnableASM() {
		if(conf.hook_spring_async_enabled) {
			scanScopePrefix.add("org/springframework/aop/interceptor/AsyncExecutionInterceptor");
		}
		if(conf.hook_async_callrunnable_enable) {
			String[] prefixes = StringUtil.split(conf.hook_async_callrunnable_scan_package_prefixes, ',');
			for(int i=0; i<prefixes.length; i++) {
				Logger.println("Callable Runnable scan scope : " + prefixes[i]);
				scanScopePrefix.add(prefixes[i].replace('.', '/'));
			}
		}
	}

	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
//		if (conf.hook_async_servlet_enabled == false) {
//			return cv;
//		}

		String[] interfaces = classDesc.interfaces;

		for (int inx = 0; inx < interfaces.length; inx++) {
			if (CALLABLE.equals(interfaces[inx]) || RUNNABLE.equals(interfaces[inx])) {
				for (int jnx = 0; jnx < scanScopePrefix.size(); jnx++) {
					if(className.indexOf(scanScopePrefix.get(jnx)) == 0) {
						return new CallRunnableCV(cv, className);
					}
				}
			}
		}

		return cv;
	}
}

class CallRunnableCV extends ClassVisitor implements Opcodes {
	String className;

	public CallRunnableCV(ClassVisitor cv, String className) {
		super(ASM4, cv);
		this.className = className;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		if (mv == null) {
			return mv;
		}

		if (name.equals("call") || name.equals("run")) {
			return new CallOrRunMV(access, name, desc, mv);
		}

		if (name.equals("<init>")) {
			return new InitMV(access, name, desc, mv);
		}

		return mv;
	}
}

class CallOrRunMV extends LocalVariablesSorter implements Opcodes {
	private static final String TARGET = TraceMain.class.getName().replace('.', '/');
	private static final String START_METHOD = "callRunnableCallInvoked";
	private static final String START_SIGNATURE = "(Ljava/lang/Object;)Ljava/lang/Object;";

	private static final String END_METHOD = "callRunnableCallEnd";
	private static final String END_METHOD_DESC = "(" +
			"Ljava/lang/Object;" +
			"Ljava/lang/Object;" +
			"Ljava/lang/Throwable;" +
			")V";

	private Label startFinally = new Label();
	private Type returnType;
	String name;
	String desc;
	private int statIdx;

	public CallOrRunMV(int access, String name, String desc, MethodVisitor mv) {
		super(ASM4, access, desc, mv);
		this.name = name;
		this.desc = desc;
		this.returnType = Type.getReturnType(desc);
	}

	@Override
	public void visitCode() {
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TARGET, START_METHOD, START_SIGNATURE, false);

		statIdx = newLocal(Type.getType(Object.class));
		mv.visitVarInsn(Opcodes.ASTORE, statIdx);
		mv.visitLabel(startFinally);
		mv.visitCode();
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

		if (tp == null || tp.equals(Type.VOID_TYPE)) {
			mv.visitInsn(Opcodes.ACONST_NULL);
			mv.visitVarInsn(Opcodes.ALOAD, statIdx);
			mv.visitInsn(Opcodes.ACONST_NULL);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, TARGET, END_METHOD, END_METHOD_DESC, false);
			return;
		}

		switch (tp.getSort()) {
			case Type.DOUBLE:
			case Type.LONG:
				mv.visitInsn(Opcodes.DUP2);
				break;
			default:
				mv.visitInsn(Opcodes.DUP);
		}
		// TODO method return test dup and store
//		int rtnIdx = newLocal(tp);
//		mv.visitVarInsn(Opcodes.ASTORE, rtnIdx);
//		mv.visitVarInsn(Opcodes.ALOAD, rtnIdx);

		mv.visitVarInsn(Opcodes.ALOAD, statIdx);// stat
		mv.visitInsn(Opcodes.ACONST_NULL);// throwable
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TARGET, END_METHOD, END_METHOD_DESC, false);
	}

	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		Label endFinally = new Label();
		mv.visitTryCatchBlock(startFinally, endFinally, endFinally, null);
		mv.visitLabel(endFinally);
		mv.visitInsn(DUP);
		int errIdx = newLocal(Type.getType(Throwable.class));
		mv.visitVarInsn(Opcodes.ASTORE, errIdx);

		mv.visitInsn(Opcodes.ACONST_NULL);// return
		mv.visitVarInsn(Opcodes.ALOAD, statIdx);
		mv.visitVarInsn(Opcodes.ALOAD, errIdx);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TARGET, END_METHOD, END_METHOD_DESC,false);
		mv.visitInsn(ATHROW);
		mv.visitMaxs(maxStack + 8, maxLocals + 2);
	}
}

class InitMV extends LocalVariablesSorter implements Opcodes {
	private static final String TARGET = TraceMain.class.getName().replace('.', '/');
	private static final String START_METHOD = "callRunnableInitInvoked";
	private static final String START_SIGNATURE = "(Ljava/lang/Object;)V";

	String name;
	String desc;

	public InitMV(int access, String name, String desc, MethodVisitor mv) {
		super(ASM4, access, desc, mv);
		this.name = name;
		this.desc = desc;
	}

	@Override
	public void visitInsn(int opcode) {
		if ((opcode >= IRETURN && opcode <= RETURN)) {
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, TARGET, START_METHOD, START_SIGNATURE, false);
		}
		mv.visitInsn(opcode);
	}
}
