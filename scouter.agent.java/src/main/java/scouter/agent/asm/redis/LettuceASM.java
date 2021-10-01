package scouter.agent.asm.redis;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;
import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.asm.IASM;
import scouter.agent.asm.util.AsmUtil;
import scouter.agent.trace.TraceMain;

import java.util.HashSet;
import java.util.Set;

public class LettuceASM implements IASM, Opcodes {

    private Configure conf = Configure.getInstance();

    private Set<String> classSet = new HashSet<String>();

    public LettuceASM() {
        classSet.add("io/lettuce/core/protocol/DefaultEndpoint");
    }

    @Override
    public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
        if (conf._hook_redis_enabled == false) {
            return cv;
        }

        if(classSet.contains(className)) {
            return new LettuceCV(cv, className);
        }
        return cv;
    }
}

class LettuceCV extends ClassVisitor implements Opcodes {

    public String className;
    private boolean hasChannelDescriptor = false;

    LettuceCV(ClassVisitor cv, String className) {
        super(ASM9, cv);
        this.className = className;
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        if("Lio/netty/channel/Channel;".equals(descriptor)) {
            hasChannelDescriptor = true;
        }
        return super.visitField(access, name, descriptor, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String methodName, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, methodName, desc, signature, exceptions);

        if (!hasChannelDescriptor || mv == null) {
            return mv;
        }

        if ("write".equals(methodName) && AsmUtil.isPublic(access)) {
            return new LettuceMV(access, desc, className, mv);
        }

        return mv;
    }
}

class LettuceMV extends LocalVariablesSorter implements Opcodes {
    private static final String TRACE_MAIN = TraceMain.class.getName().replace('.', '/');
    private final static String START_METHOD = "startLettuceCommand";
    private static final String START_SIGNATURE = "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";
    private final static String END_METHOD = "endLettuceCommand";
    private static final String END_SIGNATURE = "(Ljava/lang/Object;Ljava/lang/Throwable;)V";

    private Label startFinally = new Label();
    private int statIdx;
    private String ownerClass;

    LettuceMV(int access, String desc, String ownerClass, MethodVisitor mv) {
        super(ASM9, access, desc, mv);
        this.ownerClass = ownerClass;
    }

    @Override
    public void visitCode() {
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, ownerClass, "channel", "Lio/netty/channel/Channel;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE_MAIN, START_METHOD, START_SIGNATURE, false);
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
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE_MAIN, END_METHOD, END_SIGNATURE, false);
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
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE_MAIN, END_METHOD, END_SIGNATURE, false);
        mv.visitInsn(ATHROW);
        mv.visitMaxs(maxStack + 8, maxLocals + 2);
    }

}

