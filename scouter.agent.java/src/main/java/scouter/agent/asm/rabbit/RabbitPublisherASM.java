package scouter.agent.asm.rabbit;

import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.asm.IASM;
import scouter.agent.asm.util.AsmUtil;
import scouter.agent.asm.util.HookingSet;
import scouter.agent.trace.TraceMain;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;

import java.util.HashMap;
import java.util.Map;

public class RabbitPublisherASM implements IASM, Opcodes {

    private Configure conf = Configure.getInstance();

    private Map<String, HookingSet> reserved = new HashMap<String, HookingSet>();

    public RabbitPublisherASM() {
        AsmUtil.add(reserved, "com.rabbitmq.client.impl.ChannelN", "basicPublish(Ljava/lang/String;Ljava/lang/String;ZZLcom/rabbitmq/client/AMQP$BasicProperties;[B)V");
    }

    public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
        if (conf._hook_rabbit_enabled == false) {
            return cv;
        }
        HookingSet mset = reserved.get(className);
        if (mset != null) {
            return new RabbitPublisherCV(cv, mset, className);
        }
        return cv;
    }
}

class RabbitPublisherCV extends ClassVisitor implements Opcodes {

    public String className;
    private HookingSet mset;

    RabbitPublisherCV(ClassVisitor cv, HookingSet mset, String className) {
        super(ASM6, cv);
        this.className = className;
        this.mset = mset;
    }

    @Override
    public MethodVisitor visitMethod(int access, String methodName, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, methodName, desc, signature, exceptions);
        if (mv == null || mset.isA(methodName, desc) == false) {
            return mv;
        }
        if (AsmUtil.isSpecial(methodName)) {
            return mv;
        }
        Logger.println("rabbitmq publisher : " + className + "#" + methodName + desc);
        return new RabbitPublisherMV(access, desc, className, mv);
    }

}


class RabbitPublisherMV extends LocalVariablesSorter implements Opcodes {

    private static final String TRACE_RABBIT = TraceMain.class.getName().replace('.', '/');
    private final static String START_METHOD = "startRabbitPublish";
    private static final String START_SIGNATURE = "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;";
    private final static String END_METHOD = "endRabbitPublish";
    private static final String END_SIGNATURE = "(Ljava/lang/Object;Ljava/lang/Throwable;)V";

    private Label startFinally = new Label();
    private String ownerClass;
    private int statIdx;

    RabbitPublisherMV(int access, String desc, String ownerClass, MethodVisitor mv) {
        super(ASM6, access, desc, mv);
        this.ownerClass = ownerClass;
    }

    @Override
    public void visitCode() {
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE_RABBIT, START_METHOD, START_SIGNATURE, false);
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
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE_RABBIT, END_METHOD, END_SIGNATURE, false);
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
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE_RABBIT, END_METHOD, END_SIGNATURE, false);
        mv.visitInsn(ATHROW);
        mv.visitMaxs(maxStack + 8, maxLocals + 2);
    }
}
