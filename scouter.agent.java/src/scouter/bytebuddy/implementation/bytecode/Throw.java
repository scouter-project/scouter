package scouter.bytebuddy.implementation.bytecode;

import scouter.bytebuddy.implementation.Implementation;
import scouter.bytebuddy.jar.asm.MethodVisitor;
import scouter.bytebuddy.jar.asm.Opcodes;

/**
 * Throws a {@link java.lang.Throwable} which must lie on top of the stack when this stack manipulation is called.
 */
public enum Throw implements StackManipulation {

    /**
     * The singleton instance.
     */
    INSTANCE;

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
        methodVisitor.visitInsn(Opcodes.ATHROW);
        return StackSize.SINGLE.toDecreasingSize();
    }
}
