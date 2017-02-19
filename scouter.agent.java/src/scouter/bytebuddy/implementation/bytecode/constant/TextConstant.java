package scouter.bytebuddy.implementation.bytecode.constant;

import scouter.bytebuddy.implementation.Implementation;
import scouter.bytebuddy.implementation.bytecode.StackManipulation;
import scouter.bytebuddy.implementation.bytecode.StackSize;
import scouter.bytebuddy.jar.asm.MethodVisitor;

/**
 * Represents a {@link java.lang.String} value that is stored in a type's constant pool.
 */
public class TextConstant implements StackManipulation {

    /**
     * The text value to load onto the operand stack.
     */
    private final String text;

    /**
     * Creates a new stack manipulation to load a {@code String} constant onto the operand stack.
     *
     * @param text The value of the {@code String} to be loaded.
     */
    public TextConstant(String text) {
        this.text = text;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
        methodVisitor.visitLdcInsn(text);
        return StackSize.SINGLE.toIncreasingSize();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && text.equals(((TextConstant) other).text);
    }

    @Override
    public int hashCode() {
        return text.hashCode();
    }

    @Override
    public String toString() {
        return "TextConstant{text='" + text + '\'' + '}';
    }
}
