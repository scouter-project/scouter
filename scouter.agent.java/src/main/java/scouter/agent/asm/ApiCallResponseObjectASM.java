/*
 *  Copyright 2015 the original author or authors.
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package scouter.agent.asm;

import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.trace.TraceApiCall;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2018. 3. 20.
 */
public class ApiCallResponseObjectASM implements IASM, Opcodes {
    private Configure conf = Configure.getInstance();

    private static Set<String> hookingClasses = new HashSet<String>();
    static {
        hookingClasses.add("org/apache/http/impl/execchain/HttpResponseProxy");
        hookingClasses.add("org/apache/http/impl/client/CloseableHttpResponseProxy");
    }
    public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
        if (conf._hook_apicall_enabled == false)
            return cv;

        if (hookingClasses.contains(className)) {
            return new ApiCallResponseObjectCV(cv, className);
        }

        return cv;
    }
}

class ApiCallResponseObjectCV extends ClassVisitor implements Opcodes {
    String className;

    public ApiCallResponseObjectCV(ClassVisitor cv, String className) {
        super(ASM9, cv);
        this.className = className;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (mv == null) {
            return null;
        }
        if ("<init>".equals(name) && desc.startsWith("(Lorg/apache/http/HttpResponse;")) {
            return new ApiCallResponseObjectInitMV(className, access, name, desc, mv);
        }

        return mv;
    }
}

class ApiCallResponseObjectInitMV extends LocalVariablesSorter implements Opcodes {
    private static final String TRACE = TraceApiCall.class.getName().replace('.', '/');
    private static final String END_METHOD = "setCalleeToCtxInHttpClientResponse";
    private static final String END_SIGNATURE = "(Ljava/lang/Object;Ljava/lang/Object;)V";

    private String className;
    private String name;
    private String desc;
    private int statIdx;
    private Type returnType;
    private Label startFinally = new Label();

    public ApiCallResponseObjectInitMV(String className, int access, String name, String desc, MethodVisitor mv) {
        super(ASM9, access, desc, mv);
        this.className = className;
        this.name = name;
        this.desc = desc;
        this.returnType = Type.getReturnType(desc);
    }

    @Override
    public void visitInsn(int opcode) {
        if ((opcode >= IRETURN && opcode <= RETURN)) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE, END_METHOD, END_SIGNATURE, false);
        }
        mv.visitInsn(opcode);
    }
}
