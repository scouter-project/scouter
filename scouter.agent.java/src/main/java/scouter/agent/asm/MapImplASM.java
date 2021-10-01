package scouter.agent.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;
import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.trace.TraceCollection;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2016. 9. 20.
 */
public class MapImplASM implements IASM, Opcodes {

    public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
        if (Configure.getInstance()._hook_map_impl_enabled == false) {
            return cv;
        }

        if(classDesc.isMapImpl) {
            return new MapImplCV(cv, className);
        }

        return cv;
    }
}

class MapImplCV extends ClassVisitor implements Opcodes {
    private static String TARGET_METHOD = "put";

    private String className;
    public MapImplCV(ClassVisitor cv, String className) {
        super(ASM9, cv);
        this.className = className;
    }
    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (mv == null) {
            return mv;
        }
        if (TARGET_METHOD.equals(name)) {
            Logger.println("A201", "HOOK MAP IMPL - " + className);
            return new MapImplMV(access, desc, mv, className);
        }
        return mv;
    }
}

class MapImplMV extends LocalVariablesSorter implements Opcodes {
    private static final String TRACECOLLECTION = TraceCollection.class.getName().replace('.', '/');
    private final static String END_PUT = "endPut";
    private static final String END_PUT_SIGNATURE = "(Ljava/util/Map;)V";

    private String className;

    public MapImplMV(int access, String desc, MethodVisitor mv, String className) {
        super(ASM9, access, desc, mv);
        this.className = className;
    }

    @Override
    public void visitInsn(int opcode) {
        if ((opcode >= IRETURN && opcode <= RETURN)) {
            int pos = newLocal(Type.INT_TYPE);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            //mv.visitMethodInsn(INVOKEVIRTUAL, className, "size", "()I", false);
            //mv.visitVarInsn(Opcodes.ISTORE, pos);
            //mv.visitVarInsn(Opcodes.ILOAD, pos);
            mv.visitMethodInsn(INVOKESTATIC, TRACECOLLECTION, END_PUT, END_PUT_SIGNATURE, false);
        }
        mv.visitInsn(opcode);
    }

}
