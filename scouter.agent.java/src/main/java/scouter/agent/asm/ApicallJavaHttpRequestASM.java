package scouter.agent.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.LocalVariablesSorter;
import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.asm.util.AsmUtil;
import scouter.agent.asm.util.HookingSet;
import scouter.agent.trace.TraceApiCall;

import java.util.HashMap;
import java.util.Map;

public class ApicallJavaHttpRequestASM implements IASM, Opcodes {
    private Map<String, HookingSet> reserved = new HashMap<String, HookingSet>();
    public ApicallJavaHttpRequestASM() {
        AsmUtil.add(reserved, "jdk.internal.net.http.ImmutableHttpRequest", "<init>(Ljdk/internal/net/http/HttpRequestBuilderImpl;)V");
    }

    @Override
    public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
        if (Configure.getInstance()._hook_apicall_enabled == false) {
            return cv;
        }
        HookingSet mset = reserved.get(className);
        if (mset != null)
            return new ApicallJavaHttpRequestCV(cv, mset, className);
        return cv;
    }
}

class ApicallJavaHttpRequestCV extends ClassVisitor implements Opcodes {
    public String className;
    private HookingSet mset;
    public ApicallJavaHttpRequestCV(ClassVisitor cv, HookingSet mset, String className) {
        super(ASM9, cv);
        this.mset = mset;
        this.className = className;
    }
    @Override
    public MethodVisitor visitMethod(int access, String methodName, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, methodName, desc, signature, exceptions);
        if (mv == null || mset.isA(methodName, desc) == false) {
            return mv;
        }
        return new ApicallJavaHttpRequestMV(access, methodName, desc, mv);
    }
}

class ApicallJavaHttpRequestMV extends LocalVariablesSorter implements Opcodes {
    private static final String TARGET = TraceApiCall.class.getName().replace('.', '/');
    private static final String METHOD = "initImmutableJavaHttpRequest";
    private static final String METHOD_DESC = "(Ljava/lang/Object;)V";

    String name;
    String desc;

    public ApicallJavaHttpRequestMV(int access, String name, String desc, MethodVisitor mv) {
        super(ASM9, access, desc, mv);
        this.name = name;
        this.desc = desc;
    }

    @Override
    public void visitCode() {
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, TARGET, METHOD, METHOD_DESC, false);
        mv.visitCode();
    }
}
