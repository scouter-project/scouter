package scouter.agent.asm.kafka;

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

public class KafkaProducerASM implements IASM, Opcodes {

    private Configure conf = Configure.getInstance();

    private Map<String, HookingSet> reserved = new HashMap<String, HookingSet>();

    public KafkaProducerASM() {
        AsmUtil.add(reserved, "org.apache.kafka.clients.producer.KafkaProducer", "doSend");
    }

    public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
        if (conf._hook_kafka_enabled == false) {
            return cv;
        }
        HookingSet mset = reserved.get(className);
        if (mset != null) {
            return new KafkaProducerCV(cv, mset, className);
        }
        return cv;
    }
}

class KafkaProducerCV extends ClassVisitor implements Opcodes {

    public String className;
    private HookingSet mset;

    KafkaProducerCV(ClassVisitor cv, HookingSet mset, String className) {
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
        Logger.println("kafkaproducer : " + className + "#" + methodName + desc);
        return new KafkaProducerMV(access, desc, className, mv);
    }

}


class KafkaProducerMV extends LocalVariablesSorter implements Opcodes {

    private static final String TRACE_KAFKA = TraceMain.class.getName().replace('.', '/');
    private final static String START_METHOD = "startKafkaProducer";
    private static final String START_SIGNATURE = "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;";
    private final static String END_METHOD = "endKafkaProducer";
    private static final String END_SIGNATURE = "(Ljava/lang/Object;Ljava/lang/Throwable;)V";

    private String ownerClass;
    private Label startFinally = new Label();
    private int statIdx;

    KafkaProducerMV(int access, String desc, String ownerClass, MethodVisitor mv) {
        super(ASM6, access, desc, mv);
        this.ownerClass = ownerClass;
    }

    @Override
    public void visitCode() {
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, ownerClass, "producerConfig", "Lorg/apache/kafka/clients/producer/ProducerConfig;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/kafka/clients/producer/ProducerRecord", "topic", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE_KAFKA, START_METHOD, START_SIGNATURE, false);
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
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE_KAFKA, END_METHOD, END_SIGNATURE, false);
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
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE_KAFKA, END_METHOD, END_SIGNATURE, false);
        mv.visitInsn(ATHROW);
        mv.visitMaxs(maxStack + 8, maxLocals + 2);
    }
}
