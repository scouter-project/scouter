/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 2004 Bill Burke. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License.  Alternatively, the contents of this file may be used under
 * the terms of the GNU Lesser General Public License Version 2.1 or later,
 * or the Apache License Version 2.0.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 */

package scouter.javassist.bytecode.annotation;

import scouter.javassist.ClassPool;
import scouter.javassist.bytecode.BadBytecode;
import scouter.javassist.bytecode.ConstPool;
import scouter.javassist.bytecode.Descriptor;
import scouter.javassist.bytecode.SignatureAttribute;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * Class value.
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @author Shigeru Chiba
 */
public class ClassMemberValue extends MemberValue {
    int valueIndex;

    /**
     * Constructs a class value.  The initial value is specified
     * by the constant pool entry at the given index.
     *
     * @param index the index of a CONSTANT_Utf8_info structure.
     */
    public ClassMemberValue(int index, ConstPool cp) {
        super('c', cp);
        this.valueIndex = index;
    }

    /**
     * Constructs a class value.
     *
     * @param className         the initial value.
     */
    public ClassMemberValue(String className, ConstPool cp) {
        super('c', cp);
        setValue(className);
    }

    /**
     * Constructs a class value.
     * The initial value is java.lang.Class.
     */
    public ClassMemberValue(ConstPool cp) {
        super('c', cp);
        setValue("java.lang.Class");
    }

    Object getValue(ClassLoader cl, ClassPool cp, Method m)
            throws ClassNotFoundException {
        final String classname = getValue();
        switch (classname) {
            case "void":
                return void.class;
            case "int":
                return int.class;
            case "byte":
                return byte.class;
            case "long":
                return long.class;
            case "double":
                return double.class;
            case "float":
                return float.class;
            case "char":
                return char.class;
            case "short":
                return short.class;
            case "boolean":
                return boolean.class;
            default:
                return loadClass(cl, classname);
        }

    }

    Class getType(ClassLoader cl) throws ClassNotFoundException {
        return loadClass(cl, "java.lang.Class");
    }

    /**
     * Obtains the value of the member.
     *
     * @return fully-qualified class name.
     */
    public String getValue() {
        String v = cp.getUtf8Info(valueIndex);
        try {
            return SignatureAttribute.toTypeSignature(v).jvmTypeName();
        } catch (BadBytecode e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets the value of the member.
     *
     * @param newClassName      fully-qualified class name.
     */
    public void setValue(String newClassName) {
        String setTo = Descriptor.of(newClassName);
        valueIndex = cp.addUtf8Info(setTo);
    }

    /**
     * Obtains the string representation of this object.
     */
    public String toString() {
        return getValue().replace('$', '.') + ".class";
    }

    /**
     * Writes the value.
     */
    public void write(AnnotationsWriter writer) throws IOException {
        writer.classInfoIndex(cp.getUtf8Info(valueIndex));
    }

    /**
     * Accepts a visitor.
     */
    public void accept(MemberValueVisitor visitor) {
        visitor.visitClassMemberValue(this);
    }
}
