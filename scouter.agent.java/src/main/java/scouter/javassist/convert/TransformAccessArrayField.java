/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2007 Shigeru Chiba. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License.  Alternatively, the contents of this file may be used under
 * the terms of the GNU Lesser General Public License Version 2.1 or later.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 */
package scouter.javassist.convert;

import scouter.javassist.bytecode.*;
import scouter.javassist.bytecode.analysis.Frame;
import scouter.javassist.CannotCompileException;
import scouter.javassist.CtClass;
import scouter.javassist.NotFoundException;
import scouter.javassist.bytecode.analysis.Analyzer;
import scouter.javassist.CodeConverter;

/**
 * A transformer which replaces array access with static method invocations.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Jason T. Greene
 * @version $Revision: 1.8 $
 */
public final class TransformAccessArrayField extends Transformer {
    private final String methodClassname;
    private final CodeConverter.ArrayAccessReplacementMethodNames names;
    private Frame[] frames;
    private int offset;

    public TransformAccessArrayField(Transformer next, String methodClassname,
                                     CodeConverter.ArrayAccessReplacementMethodNames names) throws NotFoundException {
        super(next);
        this.methodClassname = methodClassname;
        this.names = names;

    }

    public void initialize(ConstPool cp, CtClass clazz, MethodInfo minfo) throws CannotCompileException {
        /*
         * This transformer must be isolated from other transformers, since some
         * of them affect the local variable and stack maximums without updating
         * the code attribute to reflect the changes. This screws up the
         * data-flow analyzer, since it relies on consistent code state. Even
         * if the attribute values were updated correctly, we would have to
         * detect it, and redo analysis, which is not cheap. Instead, we are
         * better off doing all changes in initialize() before everyone else has
         * a chance to muck things up.
         */
        CodeIterator iterator = minfo.getCodeAttribute().iterator();
        while (iterator.hasNext()) {
            try {
                int pos = iterator.next();
                int c = iterator.byteAt(pos);

                if (c == Opcode.AALOAD)
                    initFrames(clazz, minfo);

                if (c == Opcode.AALOAD || c == Opcode.BALOAD || c == Opcode.CALOAD || c == Opcode.DALOAD
                        || c == Opcode.FALOAD || c == Opcode.IALOAD || c == Opcode.LALOAD
                        || c == Opcode.SALOAD) {
                    pos = replace(cp, iterator, pos, c, getLoadReplacementSignature(c));
                } else if (c == Opcode.AASTORE || c == Opcode.BASTORE || c == Opcode.CASTORE
                        || c == Opcode.DASTORE || c == Opcode.FASTORE || c == Opcode.IASTORE
                        || c == Opcode.LASTORE || c == Opcode.SASTORE) {
                    pos = replace(cp, iterator, pos, c, getStoreReplacementSignature(c));
                }

            } catch (Exception e) {
                throw new CannotCompileException(e);
            }
        }
    }

    public void clean() {
        frames = null;
        offset = -1;
    }

    public int transform(CtClass tclazz, int pos, CodeIterator iterator,
            ConstPool cp) throws BadBytecode {
        // Do nothing, see above comment
        return pos;
    }

    private Frame getFrame(int pos) throws BadBytecode {
        return frames[pos - offset]; // Adjust pos
    }

    private void initFrames(CtClass clazz, MethodInfo minfo) throws BadBytecode {
        if (frames == null) {
            frames = ((new Analyzer())).analyze(clazz, minfo);
            offset = 0; // start tracking changes
        }
    }

    private int updatePos(int pos, int increment) {
        if (offset > -1)
            offset += increment;

        return pos + increment;
    }

    private String getTopType(int pos) throws BadBytecode {
        Frame frame = getFrame(pos);
        if (frame == null)
            return null;

        CtClass clazz = frame.peek().getCtClass();
        return clazz != null ? Descriptor.toJvmName(clazz) : null;
    }

    private int replace(ConstPool cp, CodeIterator iterator, int pos,
                        int opcode, String signature) throws BadBytecode {
        String castType = null;
        String methodName = getMethodName(opcode);
        if (methodName != null) {
            // See if the object must be cast
            if (opcode == Opcode.AALOAD) {
                castType = getTopType(iterator.lookAhead());
                // Do not replace an AALOAD instruction that we do not have a type for
                // This happens when the state is guaranteed to be null (Type.UNINIT)
                // So we don't really care about this case.
                if (castType == null)
                    return pos;
                if ("java/lang/Object".equals(castType))
                    castType = null;
            }

            // The gap may include extra padding
            // Write a nop in case the padding pushes the instruction forward
            iterator.writeByte(Opcode.NOP, pos);
            CodeIterator.Gap gap
                = iterator.insertGapAt(pos, castType != null ? 5 : 2, false);
            pos = gap.position;
            int mi = cp.addClassInfo(methodClassname);
            int methodref = cp.addMethodrefInfo(mi, methodName, signature);
            iterator.writeByte(Opcode.INVOKESTATIC, pos);
            iterator.write16bit(methodref, pos + 1);

            if (castType != null) {
                int index = cp.addClassInfo(castType);
                iterator.writeByte(Opcode.CHECKCAST, pos + 3);
                iterator.write16bit(index, pos + 4);
            }

            pos = updatePos(pos, gap.length);
        }

        return pos;
    }

    private String getMethodName(int opcode) {
        String methodName = null;
        switch (opcode) {
        case Opcode.AALOAD:
            methodName = names.objectRead();
            break;
        case Opcode.BALOAD:
            methodName = names.byteOrBooleanRead();
            break;
        case Opcode.CALOAD:
            methodName = names.charRead();
            break;
        case Opcode.DALOAD:
            methodName = names.doubleRead();
            break;
        case Opcode.FALOAD:
            methodName = names.floatRead();
            break;
        case Opcode.IALOAD:
            methodName = names.intRead();
            break;
        case Opcode.SALOAD:
            methodName = names.shortRead();
            break;
        case Opcode.LALOAD:
            methodName = names.longRead();
            break;
        case Opcode.AASTORE:
            methodName = names.objectWrite();
            break;
        case Opcode.BASTORE:
            methodName = names.byteOrBooleanWrite();
            break;
        case Opcode.CASTORE:
            methodName = names.charWrite();
            break;
        case Opcode.DASTORE:
            methodName = names.doubleWrite();
            break;
        case Opcode.FASTORE:
            methodName = names.floatWrite();
            break;
        case Opcode.IASTORE:
            methodName = names.intWrite();
            break;
        case Opcode.SASTORE:
            methodName = names.shortWrite();
            break;
        case Opcode.LASTORE:
            methodName = names.longWrite();
            break;
        }

        if (methodName.equals(""))
            methodName = null;

        return methodName;
    }

    private String getLoadReplacementSignature(int opcode) throws BadBytecode {
        switch (opcode) {
        case Opcode.AALOAD:
            return "(Ljava/lang/Object;I)Ljava/lang/Object;";
        case Opcode.BALOAD:
            return "(Ljava/lang/Object;I)B";
        case Opcode.CALOAD:
            return "(Ljava/lang/Object;I)C";
        case Opcode.DALOAD:
            return "(Ljava/lang/Object;I)D";
        case Opcode.FALOAD:
            return "(Ljava/lang/Object;I)F";
        case Opcode.IALOAD:
            return "(Ljava/lang/Object;I)I";
        case Opcode.SALOAD:
            return "(Ljava/lang/Object;I)S";
        case Opcode.LALOAD:
            return "(Ljava/lang/Object;I)J";
        }

        throw new BadBytecode(opcode);
    }

    private String getStoreReplacementSignature(int opcode) throws BadBytecode {
        switch (opcode) {
        case Opcode.AASTORE:
            return "(Ljava/lang/Object;ILjava/lang/Object;)V";
        case Opcode.BASTORE:
            return "(Ljava/lang/Object;IB)V";
        case Opcode.CASTORE:
            return "(Ljava/lang/Object;IC)V";
        case Opcode.DASTORE:
            return "(Ljava/lang/Object;ID)V";
        case Opcode.FASTORE:
            return "(Ljava/lang/Object;IF)V";
        case Opcode.IASTORE:
            return "(Ljava/lang/Object;II)V";
        case Opcode.SASTORE:
            return "(Ljava/lang/Object;IS)V";
        case Opcode.LASTORE:
            return "(Ljava/lang/Object;IJ)V";
        }

        throw new BadBytecode(opcode);
    }
}
