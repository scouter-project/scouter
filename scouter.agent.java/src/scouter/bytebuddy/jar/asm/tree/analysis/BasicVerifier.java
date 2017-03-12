/***
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package scouter.bytebuddy.jar.asm.tree.analysis;

import java.util.List;

import scouter.bytebuddy.jar.asm.Type;
import scouter.bytebuddy.jar.asm.tree.AbstractInsnNode;
import scouter.bytebuddy.jar.asm.tree.FieldInsnNode;
import scouter.bytebuddy.jar.asm.tree.InvokeDynamicInsnNode;
import scouter.bytebuddy.jar.asm.tree.MethodInsnNode;
import scouter.bytebuddy.jar.asm.Opcodes;

/**
 * An extended {@link BasicInterpreter} that checks that bytecode instructions
 * are correctly used.
 * 
 * @author Eric Bruneton
 * @author Bing Ran
 */
public class BasicVerifier extends BasicInterpreter {

    public BasicVerifier() {
        super(Opcodes.ASM5);
    }

    protected BasicVerifier(final int api) {
        super(api);
    }

    @Override
    public BasicValue copyOperation(final AbstractInsnNode insn,
            final BasicValue value) throws AnalyzerException {
        Value expected;
        switch (insn.getOpcode()) {
        case Opcodes.ILOAD:
        case Opcodes.ISTORE:
            expected = BasicValue.INT_VALUE;
            break;
        case Opcodes.FLOAD:
        case Opcodes.FSTORE:
            expected = BasicValue.FLOAT_VALUE;
            break;
        case Opcodes.LLOAD:
        case Opcodes.LSTORE:
            expected = BasicValue.LONG_VALUE;
            break;
        case Opcodes.DLOAD:
        case Opcodes.DSTORE:
            expected = BasicValue.DOUBLE_VALUE;
            break;
        case Opcodes.ALOAD:
            if (!value.isReference()) {
                throw new AnalyzerException(insn, null, "an object reference",
                        value);
            }
            return value;
        case Opcodes.ASTORE:
            if (!value.isReference()
                    && !BasicValue.RETURNADDRESS_VALUE.equals(value)) {
                throw new AnalyzerException(insn, null,
                        "an object reference or a return address", value);
            }
            return value;
        default:
            return value;
        }
        if (!expected.equals(value)) {
            throw new AnalyzerException(insn, null, expected, value);
        }
        return value;
    }

    @Override
    public BasicValue unaryOperation(final AbstractInsnNode insn,
            final BasicValue value) throws AnalyzerException {
        BasicValue expected;
        switch (insn.getOpcode()) {
        case Opcodes.INEG:
        case Opcodes.IINC:
        case Opcodes.I2F:
        case Opcodes.I2L:
        case Opcodes.I2D:
        case Opcodes.I2B:
        case Opcodes.I2C:
        case Opcodes.I2S:
        case Opcodes.IFEQ:
        case Opcodes.IFNE:
        case Opcodes.IFLT:
        case Opcodes.IFGE:
        case Opcodes.IFGT:
        case Opcodes.IFLE:
        case Opcodes.TABLESWITCH:
        case Opcodes.LOOKUPSWITCH:
        case Opcodes.IRETURN:
        case Opcodes.NEWARRAY:
        case Opcodes.ANEWARRAY:
            expected = BasicValue.INT_VALUE;
            break;
        case Opcodes.FNEG:
        case Opcodes.F2I:
        case Opcodes.F2L:
        case Opcodes.F2D:
        case Opcodes.FRETURN:
            expected = BasicValue.FLOAT_VALUE;
            break;
        case Opcodes.LNEG:
        case Opcodes.L2I:
        case Opcodes.L2F:
        case Opcodes.L2D:
        case Opcodes.LRETURN:
            expected = BasicValue.LONG_VALUE;
            break;
        case Opcodes.DNEG:
        case Opcodes.D2I:
        case Opcodes.D2F:
        case Opcodes.D2L:
        case Opcodes.DRETURN:
            expected = BasicValue.DOUBLE_VALUE;
            break;
        case Opcodes.GETFIELD:
            expected = newValue(Type
                    .getObjectType(((FieldInsnNode) insn).owner));
            break;
        case Opcodes.CHECKCAST:
            if (!value.isReference()) {
                throw new AnalyzerException(insn, null, "an object reference",
                        value);
            }
            return super.unaryOperation(insn, value);
        case Opcodes.ARRAYLENGTH:
            if (!isArrayValue(value)) {
                throw new AnalyzerException(insn, null, "an array reference",
                        value);
            }
            return super.unaryOperation(insn, value);
        case Opcodes.ARETURN:
        case Opcodes.ATHROW:
        case Opcodes.INSTANCEOF:
        case Opcodes.MONITORENTER:
        case Opcodes.MONITOREXIT:
        case Opcodes.IFNULL:
        case Opcodes.IFNONNULL:
            if (!value.isReference()) {
                throw new AnalyzerException(insn, null, "an object reference",
                        value);
            }
            return super.unaryOperation(insn, value);
        case Opcodes.PUTSTATIC:
            expected = newValue(Type.getType(((FieldInsnNode) insn).desc));
            break;
        default:
            throw new Error("Internal error.");
        }
        if (!isSubTypeOf(value, expected)) {
            throw new AnalyzerException(insn, null, expected, value);
        }
        return super.unaryOperation(insn, value);
    }

    @Override
    public BasicValue binaryOperation(final AbstractInsnNode insn,
            final BasicValue value1, final BasicValue value2)
            throws AnalyzerException {
        BasicValue expected1;
        BasicValue expected2;
        switch (insn.getOpcode()) {
        case Opcodes.IALOAD:
            expected1 = newValue(Type.getType("[I"));
            expected2 = BasicValue.INT_VALUE;
            break;
        case Opcodes.BALOAD:
            if (isSubTypeOf(value1, newValue(Type.getType("[Z")))) {
                expected1 = newValue(Type.getType("[Z"));
            } else {
                expected1 = newValue(Type.getType("[B"));
            }
            expected2 = BasicValue.INT_VALUE;
            break;
        case Opcodes.CALOAD:
            expected1 = newValue(Type.getType("[C"));
            expected2 = BasicValue.INT_VALUE;
            break;
        case Opcodes.SALOAD:
            expected1 = newValue(Type.getType("[S"));
            expected2 = BasicValue.INT_VALUE;
            break;
        case Opcodes.LALOAD:
            expected1 = newValue(Type.getType("[J"));
            expected2 = BasicValue.INT_VALUE;
            break;
        case Opcodes.FALOAD:
            expected1 = newValue(Type.getType("[F"));
            expected2 = BasicValue.INT_VALUE;
            break;
        case Opcodes.DALOAD:
            expected1 = newValue(Type.getType("[D"));
            expected2 = BasicValue.INT_VALUE;
            break;
        case Opcodes.AALOAD:
            expected1 = newValue(Type.getType("[Ljava/lang/Object;"));
            expected2 = BasicValue.INT_VALUE;
            break;
        case Opcodes.IADD:
        case Opcodes.ISUB:
        case Opcodes.IMUL:
        case Opcodes.IDIV:
        case Opcodes.IREM:
        case Opcodes.ISHL:
        case Opcodes.ISHR:
        case Opcodes.IUSHR:
        case Opcodes.IAND:
        case Opcodes.IOR:
        case Opcodes.IXOR:
        case Opcodes.IF_ICMPEQ:
        case Opcodes.IF_ICMPNE:
        case Opcodes.IF_ICMPLT:
        case Opcodes.IF_ICMPGE:
        case Opcodes.IF_ICMPGT:
        case Opcodes.IF_ICMPLE:
            expected1 = BasicValue.INT_VALUE;
            expected2 = BasicValue.INT_VALUE;
            break;
        case Opcodes.FADD:
        case Opcodes.FSUB:
        case Opcodes.FMUL:
        case Opcodes.FDIV:
        case Opcodes.FREM:
        case Opcodes.FCMPL:
        case Opcodes.FCMPG:
            expected1 = BasicValue.FLOAT_VALUE;
            expected2 = BasicValue.FLOAT_VALUE;
            break;
        case Opcodes.LADD:
        case Opcodes.LSUB:
        case Opcodes.LMUL:
        case Opcodes.LDIV:
        case Opcodes.LREM:
        case Opcodes.LAND:
        case Opcodes.LOR:
        case Opcodes.LXOR:
        case Opcodes.LCMP:
            expected1 = BasicValue.LONG_VALUE;
            expected2 = BasicValue.LONG_VALUE;
            break;
        case Opcodes.LSHL:
        case Opcodes.LSHR:
        case Opcodes.LUSHR:
            expected1 = BasicValue.LONG_VALUE;
            expected2 = BasicValue.INT_VALUE;
            break;
        case Opcodes.DADD:
        case Opcodes.DSUB:
        case Opcodes.DMUL:
        case Opcodes.DDIV:
        case Opcodes.DREM:
        case Opcodes.DCMPL:
        case Opcodes.DCMPG:
            expected1 = BasicValue.DOUBLE_VALUE;
            expected2 = BasicValue.DOUBLE_VALUE;
            break;
        case Opcodes.IF_ACMPEQ:
        case Opcodes.IF_ACMPNE:
            expected1 = BasicValue.REFERENCE_VALUE;
            expected2 = BasicValue.REFERENCE_VALUE;
            break;
        case Opcodes.PUTFIELD:
            FieldInsnNode fin = (FieldInsnNode) insn;
            expected1 = newValue(Type.getObjectType(fin.owner));
            expected2 = newValue(Type.getType(fin.desc));
            break;
        default:
            throw new Error("Internal error.");
        }
        if (!isSubTypeOf(value1, expected1)) {
            throw new AnalyzerException(insn, "First argument", expected1,
                    value1);
        } else if (!isSubTypeOf(value2, expected2)) {
            throw new AnalyzerException(insn, "Second argument", expected2,
                    value2);
        }
        if (insn.getOpcode() == Opcodes.AALOAD) {
            return getElementValue(value1);
        } else {
            return super.binaryOperation(insn, value1, value2);
        }
    }

    @Override
    public BasicValue ternaryOperation(final AbstractInsnNode insn,
            final BasicValue value1, final BasicValue value2,
            final BasicValue value3) throws AnalyzerException {
        BasicValue expected1;
        BasicValue expected3;
        switch (insn.getOpcode()) {
        case Opcodes.IASTORE:
            expected1 = newValue(Type.getType("[I"));
            expected3 = BasicValue.INT_VALUE;
            break;
        case Opcodes.BASTORE:
            if (isSubTypeOf(value1, newValue(Type.getType("[Z")))) {
                expected1 = newValue(Type.getType("[Z"));
            } else {
                expected1 = newValue(Type.getType("[B"));
            }
            expected3 = BasicValue.INT_VALUE;
            break;
        case Opcodes.CASTORE:
            expected1 = newValue(Type.getType("[C"));
            expected3 = BasicValue.INT_VALUE;
            break;
        case Opcodes.SASTORE:
            expected1 = newValue(Type.getType("[S"));
            expected3 = BasicValue.INT_VALUE;
            break;
        case Opcodes.LASTORE:
            expected1 = newValue(Type.getType("[J"));
            expected3 = BasicValue.LONG_VALUE;
            break;
        case Opcodes.FASTORE:
            expected1 = newValue(Type.getType("[F"));
            expected3 = BasicValue.FLOAT_VALUE;
            break;
        case Opcodes.DASTORE:
            expected1 = newValue(Type.getType("[D"));
            expected3 = BasicValue.DOUBLE_VALUE;
            break;
        case Opcodes.AASTORE:
            expected1 = value1;
            expected3 = BasicValue.REFERENCE_VALUE;
            break;
        default:
            throw new Error("Internal error.");
        }
        if (!isSubTypeOf(value1, expected1)) {
            throw new AnalyzerException(insn, "First argument", "a "
                    + expected1 + " array reference", value1);
        } else if (!BasicValue.INT_VALUE.equals(value2)) {
            throw new AnalyzerException(insn, "Second argument",
                    BasicValue.INT_VALUE, value2);
        } else if (!isSubTypeOf(value3, expected3)) {
            throw new AnalyzerException(insn, "Third argument", expected3,
                    value3);
        }
        return null;
    }

    @Override
    public BasicValue naryOperation(final AbstractInsnNode insn,
            final List<? extends BasicValue> values) throws AnalyzerException {
        int opcode = insn.getOpcode();
        if (opcode == Opcodes.MULTIANEWARRAY) {
            for (int i = 0; i < values.size(); ++i) {
                if (!BasicValue.INT_VALUE.equals(values.get(i))) {
                    throw new AnalyzerException(insn, null,
                            BasicValue.INT_VALUE, values.get(i));
                }
            }
        } else {
            int i = 0;
            int j = 0;
            if (opcode != Opcodes.INVOKESTATIC && opcode != Opcodes.INVOKEDYNAMIC) {
                Type owner = Type.getObjectType(((MethodInsnNode) insn).owner);
                if (!isSubTypeOf(values.get(i++), newValue(owner))) {
                    throw new AnalyzerException(insn, "Method owner",
                            newValue(owner), values.get(0));
                }
            }
            String desc = (opcode == Opcodes.INVOKEDYNAMIC) ? ((InvokeDynamicInsnNode) insn).desc
                    : ((MethodInsnNode) insn).desc;
            Type[] args = Type.getArgumentTypes(desc);
            while (i < values.size()) {
                BasicValue expected = newValue(args[j++]);
                BasicValue encountered = values.get(i++);
                if (!isSubTypeOf(encountered, expected)) {
                    throw new AnalyzerException(insn, "Argument " + j,
                            expected, encountered);
                }
            }
        }
        return super.naryOperation(insn, values);
    }

    @Override
    public void returnOperation(final AbstractInsnNode insn,
            final BasicValue value, final BasicValue expected)
            throws AnalyzerException {
        if (!isSubTypeOf(value, expected)) {
            throw new AnalyzerException(insn, "Incompatible return type",
                    expected, value);
        }
    }

    protected boolean isArrayValue(final BasicValue value) {
        return value.isReference();
    }

    protected BasicValue getElementValue(final BasicValue objectArrayValue)
            throws AnalyzerException {
        return BasicValue.REFERENCE_VALUE;
    }

    protected boolean isSubTypeOf(final BasicValue value,
            final BasicValue expected) {
        return value.equals(expected);
    }
}
