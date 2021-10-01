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

package scouter.agent.asm.asyncsupport.spring;

import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.asm.IASM;
import scouter.agent.asm.util.AsmUtil;
import scouter.agent.asm.util.HookingSet;
import scouter.agent.trace.TraceMain;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.LocalVariablesSorter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2018. 7. 7.
 */
public class SpringAsyncExecutionAspectSupportDoSubmitASM implements IASM, Opcodes {
    private static List<String> doSumitPatterns = new ArrayList<String>();
    static {
        doSumitPatterns.add("org.springframework.aop.interceptor.AsyncExecutionAspectSupport.doSubmit(Ljava/util/concurrent/Callable;Lorg/springframework/core/task/AsyncTaskExecutor;Ljava/lang/Class;)Ljava/lang/Object;");
    }

    private Configure conf = Configure.getInstance();
    private List<HookingSet> target;

    public SpringAsyncExecutionAspectSupportDoSubmitASM() {
        target = HookingSet.getHookingMethodSet(HookingSet.buildPatterns("", doSumitPatterns));
    }

    public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
        if (conf.hook_spring_async_enabled == false) {
            return cv;
        }

        for (int i = 0; i < target.size(); i++) {
            HookingSet mset = target.get(i);
            if (mset.classMatch.include(className)) {
                return new SpringAsyncExecutionAspectSupportCV(cv, mset, className);
            }
        }

        return cv;
    }
}

class SpringAsyncExecutionAspectSupportCV extends ClassVisitor implements Opcodes {
    String className;
    HookingSet mset;

    public SpringAsyncExecutionAspectSupportCV(ClassVisitor cv, HookingSet mset, String className) {
        super(ASM9, cv);
        this.mset = mset;
        this.className = className;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (mv == null || mset.isA(name, desc) == false) {
            return mv;
        }
        if (AsmUtil.isSpecial(name)) {
            return mv;
        }

        return new DoSubmitMV(access, desc, mv);
    }
}


class DoSubmitMV extends LocalVariablesSorter implements Opcodes {
    private static final String TRACEMAIN = TraceMain.class.getName().replace('.', '/');
    private static final String CALL_METHOD = "wrap1stParamAsWrTaskCallable";
    private static final String CALL_SIGNATURE = "(Ljava/util/concurrent/Callable;)Ljava/util/concurrent/Callable;";

    public DoSubmitMV(int access, String desc, MethodVisitor mv) {
        super(ASM9, access, desc, mv);
    }

    @Override
    public void visitCode() {
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, CALL_METHOD, CALL_SIGNATURE, false);
        mv.visitVarInsn(Opcodes.ASTORE, 1);
        mv.visitCode();
    }
}
