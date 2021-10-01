/*
 *  Copyright 2015 the original author or authors.
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package scouter.agent.asm.asyncsupport.executor;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.LocalVariablesSorter;
import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.asm.IASM;
import scouter.agent.trace.TraceMain;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 7. 29.
 */
public class ExecutorServiceASM implements IASM, Opcodes {
    private static final String THREAD_POOL_EXECUTOR_CLASS_NAME = "java/util/concurrent/ThreadPoolExecutor";
    private static final String ABSTRACT_EXECUTOR_SERVICE_CLASS_NAME = "java/util/concurrent/AbstractExecutorService";

    private Configure conf = Configure.getInstance();

    @Override
    public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
        if (conf.hook_async_thread_pool_executor_enabled == false) {
            return cv;
        }
        if (THREAD_POOL_EXECUTOR_CLASS_NAME.equals(className)) {
            return new ThreadPoolExecutorCV(cv, className);
        } else if (ABSTRACT_EXECUTOR_SERVICE_CLASS_NAME.equals(className)) {
            return new AbstractExecutorServiceCV(cv, className);
        }

        return cv;
    }
}

class ThreadPoolExecutorCV extends ClassVisitor implements Opcodes {
    String className;

    public ThreadPoolExecutorCV(ClassVisitor cv, String className) {
        super(ASM9, cv);
        this.className = className;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if ("execute".equals(name) && "(Ljava/lang/Runnable;)V".equals(desc)) {
            return new ThreadPoolExecutorExecuteMV(access, name, desc, mv);
        } else if ("getTask".equals(name)) {
            return new ThreadPoolExecutorGetTaskMV(access, name, desc, mv);
        }
//        Ignore first execution !
//        else if("runWorker".equals(name) && "(Ljava/util/concurrent/ThreadPoolExecutor$Worker;)V".equals(desc)) {
//            return new ThreadPoolExecutorRunWorkerMV(access, name, desc, mv);
//        }

        return mv;
    }
}

class ThreadPoolExecutorExecuteMV extends LocalVariablesSorter implements Opcodes {
    private static final String TRACEMAIN = TraceMain.class.getName().replace('.', '/');
    private static final String START_METHOD = "executorServiceExecuted";
    private static final String START_SIGNATURE = "(Ljava/lang/Object;)V";

    String name;
    String desc;

    public ThreadPoolExecutorExecuteMV(int access, String name, String desc, MethodVisitor mv) {
        super(ASM9, access, desc, mv);
        this.name = name;
        this.desc = desc;
    }

    @Override
    public void visitCode() {
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, START_METHOD, START_SIGNATURE, false);
        mv.visitCode();
    }
}


class AbstractExecutorServiceCV extends ClassVisitor implements Opcodes {
    String className;

    public AbstractExecutorServiceCV(ClassVisitor cv, String className) {
        super(ASM9, cv);
        this.className = className;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if ("submit".equals(name)) {
            Logger.trace("[SCTRACE]AbstractExecutorServiceCV.visitMethod:submit(), class:" + className + ", " + name + ", " + signature);
            return new AbstraceExecutorServiceSubmitMV(access, name, desc, mv);
        }
        return mv;
    }
}

class AbstraceExecutorServiceSubmitMV extends LocalVariablesSorter implements Opcodes {
    private static final String TRACEMAIN = TraceMain.class.getName().replace('.', '/');
    private static final String START_METHOD = "executorServiceSubmitted";
    private static final String START_SIGNATURE = "(Ljava/lang/Object;)V";

    String name;
    String desc;

    public AbstraceExecutorServiceSubmitMV(int access, String name, String desc, MethodVisitor mv) {
        super(ASM9, access, desc, mv);
        this.name = name;
        this.desc = desc;
    }

    @Override
    public void visitCode() {
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, START_METHOD, START_SIGNATURE, false);
        mv.visitCode();
    }
}

class ThreadPoolExecutorGetTaskMV extends LocalVariablesSorter implements Opcodes {
    private static final String TARGET = TraceMain.class.getName().replace('.', '/');

    private static final String END_METHOD = "executorServiceGetTaskEnd";
    private static final String END_SIGNATURE = "(Ljava/lang/Runnable;)Ljava/lang/Runnable;";

    String name;
    String desc;

    public ThreadPoolExecutorGetTaskMV(int access, String name, String desc, MethodVisitor mv) {
        super(ASM9, access, desc, mv);
        this.name = name;
        this.desc = desc;
    }

    @Override
    public void visitInsn(int opcode) {
        if ((opcode >= IRETURN && opcode <= RETURN)) {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, TARGET, END_METHOD, END_SIGNATURE, false);
        }
        mv.visitInsn(opcode);
    }
}
