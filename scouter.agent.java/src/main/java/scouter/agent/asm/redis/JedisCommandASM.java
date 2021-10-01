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

package scouter.agent.asm.redis;

import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.asm.IASM;
import scouter.agent.asm.util.AsmUtil;
import scouter.agent.trace.TraceMain;
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
public class JedisCommandASM implements IASM, Opcodes {
    private Configure conf = Configure.getInstance();

    private static Set<String> hookingClasses = new HashSet<String>();
    static {
        hookingClasses.add("redis/clients/jedis/Jedis");
        hookingClasses.add("redis/clients/jedis/BinaryJedis");
    }
    public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
        if (conf._hook_redis_enabled == false)
            return cv;

        if (hookingClasses.contains(className)) {
            return new JedisCommandCV(cv, className);
        }

        return cv;
    }
}

class JedisCommandCV extends ClassVisitor implements Opcodes {
    String className;

    public JedisCommandCV(ClassVisitor cv, String className) {
        super(ASM9, cv);
        this.className = className;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (mv == null) {
            return null;
        }
        if (AsmUtil.isSpecial(name)) {
            return mv;
        }
        if (AsmUtil.isPublic(access)) {
            return new JedisCommandMV(className, access, name, desc, mv);
        }

        return mv;
    }
}

class JedisCommandMV extends LocalVariablesSorter implements Opcodes {
    private static final String TRACEMAIN = TraceMain.class.getName().replace('.', '/');
    private static final String START_METHOD = "setTraceJedisHostPort";
    private static final String START_SIGNATURE = "(Ljava/lang/String;I)V";

    private String className;
    private String name;
    private String desc;
    private int statIdx;
    private Label startFinally = new Label();

    public JedisCommandMV(String className, int access, String name, String desc, MethodVisitor mv) {
        super(ASM9, access, desc, mv);
        this.className = className;
        this.name = name;
        this.desc = desc;
    }

    @Override
    public void visitCode() {
        int hostIdx = newLocal(Type.getType("Ljava/lang/String;"));
        int portIdx = newLocal(Type.INT_TYPE);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, "client", "Lredis/clients/jedis/Client;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "redis/clients/jedis/Client", "getHost", "()Ljava/lang/String;", false);
        mv.visitVarInsn(ASTORE, hostIdx);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, "client", "Lredis/clients/jedis/Client;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "redis/clients/jedis/Client", "getPort", "()I", false);
        mv.visitVarInsn(ISTORE, portIdx);

        mv.visitVarInsn(ALOAD, hostIdx);
        mv.visitVarInsn(ILOAD, portIdx);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, START_METHOD, START_SIGNATURE, false);

        mv.visitCode();
    }
}
