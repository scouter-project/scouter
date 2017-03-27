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
package scouter.bytebuddy.jar.asm.optimizer;

import java.util.HashMap;

import scouter.bytebuddy.jar.asm.AnnotationVisitor;
import scouter.bytebuddy.jar.asm.Attribute;
import scouter.bytebuddy.jar.asm.FieldVisitor;
import scouter.bytebuddy.jar.asm.Label;
import scouter.bytebuddy.jar.asm.MethodVisitor;
import scouter.bytebuddy.jar.asm.Opcodes;
import scouter.bytebuddy.jar.asm.Type;
import scouter.bytebuddy.jar.asm.TypePath;
import scouter.bytebuddy.jar.asm.commons.Remapper;
import scouter.bytebuddy.jar.asm.commons.MethodRemapper;

/**
 * A {@link MethodVisitor} that renames fields and methods, and removes debug
 * info.
 * 
 * @author Eugene Kuleshov
 */
public class MethodOptimizer extends MethodRemapper implements Opcodes {

    private final ClassOptimizer classOptimizer;

    public MethodOptimizer(ClassOptimizer classOptimizer, MethodVisitor mv,
            Remapper remapper) {
        super(Opcodes.ASM5, mv, remapper);
        this.classOptimizer = classOptimizer;
    }

    // ------------------------------------------------------------------------
    // Overridden methods
    // ------------------------------------------------------------------------

    @Override
    public void visitParameter(String name, int access) {
        // remove parameter info
    }

    @Override
    public AnnotationVisitor visitAnnotationDefault() {
        // remove annotations
        return null;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        // remove annotations
        return null;
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef,
            TypePath typePath, String desc, boolean visible) {
        return null;
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(final int parameter,
            final String desc, final boolean visible) {
        // remove annotations
        return null;
    }

    @Override
    public void visitLocalVariable(final String name, final String desc,
            final String signature, final Label start, final Label end,
            final int index) {
        // remove debug info
    }

    @Override
    public void visitLineNumber(final int line, final Label start) {
        // remove debug info
    }

    @Override
    public void visitFrame(int type, int local, Object[] local2, int stack,
            Object[] stack2) {
        // remove frame info
    }

    @Override
    public void visitAttribute(Attribute attr) {
        // remove non standard attributes
    }

    @Override
    public void visitLdcInsn(Object cst) {
        if (!(cst instanceof Type)) {
            super.visitLdcInsn(cst);
            return;
        }

        // transform Foo.class for 1.2 compatibility
        String ldcName = ((Type) cst).getInternalName();
        String fieldName = "class$" + ldcName.replace('/', '$');
        if (!classOptimizer.syntheticClassFields.contains(ldcName)) {
            classOptimizer.syntheticClassFields.add(ldcName);
            FieldVisitor fv = classOptimizer.syntheticFieldVisitor(ACC_STATIC
                    | ACC_SYNTHETIC, fieldName, "Ljava/lang/Class;");
            fv.visitEnd();
        }

        String clsName = classOptimizer.clsName;
        mv.visitFieldInsn(GETSTATIC, clsName, fieldName, "Ljava/lang/Class;");
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name,
            String desc, boolean itf) {
        // rewrite boxing method call to use constructor to keep 1.3/1.4
        // compatibility
        String[] constructorParams;
        if (opcode == INVOKESTATIC && name.equals("valueOf")
                && (constructorParams = BOXING_MAP.get(owner + desc)) != null) {
            String type = constructorParams[0];
            String initDesc = constructorParams[1];
            super.visitTypeInsn(NEW, type);
            super.visitInsn(DUP);
            super.visitInsn((initDesc == "(J)V" || initDesc == "(D)V") ? DUP2_X2
                    : DUP2_X1);
            super.visitInsn(POP2);
            super.visitMethodInsn(INVOKESPECIAL, type, "<init>", initDesc,
                    false);
            return;
        }
        super.visitMethodInsn(opcode, owner, name, desc, itf);
    }

    private static final HashMap<String, String[]> BOXING_MAP;
    static {
        String[][] boxingNames = {
                // Boolean.valueOf is 1.4 and is used by the xml package, so no
                // rewrite
                { "java/lang/Byte", "(B)V" }, { "java/lang/Short", "(S)V" },
                { "java/lang/Character", "(C)V" },
                { "java/lang/Integer", "(I)V" }, { "java/lang/Long", "(J)V" },
                { "java/lang/Float", "(F)V" }, { "java/lang/Double", "(D)V" }, };
        HashMap<String, String[]> map = new HashMap<String, String[]>();
        for (String[] boxingName : boxingNames) {
            String wrapper = boxingName[0];
            String desc = boxingName[1];
            String boxingMethod = wrapper + '(' + desc.charAt(1) + ")L"
                    + wrapper + ';';
            map.put(boxingMethod, boxingName);
        }
        BOXING_MAP = map;
    }
}
