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
import scouter.agent.asm.util.HookingSet;
import scouter.agent.trace.TraceMain;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2018. 3. 20.
 */
public class RedisKeyASM implements IASM, Opcodes {
    private Configure conf = Configure.getInstance();

    private static List<String> setKeyPattern = new ArrayList<String>();
    static {
        setKeyPattern.add("org.springframework.data.redis.core.AbstractOperations.rawKey(Ljava/lang/Object;)[B");
    }
    private List<HookingSet> targetList;

    public RedisKeyASM() {
        targetList = HookingSet.getHookingMethodSet(HookingSet.buildPatterns(conf._hook_redis_set_key_patterns, setKeyPattern));
    }

    public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
        if (conf._hook_redis_enabled == false)
            return cv;

        for (int i = 0; i < targetList.size(); i++) {
            HookingSet mset = targetList.get(i);
            if (mset.classMatch.include(className)) {
                return new RedisKeySetCV(cv, mset, className);
            }
        }

        return cv;
    }
}

class RedisKeySetCV extends ClassVisitor implements Opcodes {
    String className;
    HookingSet mset;

    public RedisKeySetCV(ClassVisitor cv, HookingSet mset, String className) {
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

        return new KeySetMV(access, name, desc, mv);
    }
}

class KeySetMV extends LocalVariablesSorter implements Opcodes {
    private static final String TRACEMAIN = TraceMain.class.getName().replace('.', '/');
    private static final String START_METHOD = "setRedisKey";
    private static final String START_SIGNATURE = "([BLjava/lang/Object;)V";

    private String name;
    private String desc;
    private Type returnType;

    public KeySetMV(int access, String name, String desc, MethodVisitor mv) {
        super(ASM9, access, desc, mv);
        this.name = name;
        this.desc = desc;
        this.returnType = Type.getReturnType(desc);
    }

    public void visitInsn(int opcode) {
        if ((opcode >= IRETURN && opcode <= RETURN)) {
            Type tp = returnType;
            if (tp.getSort() == Type.ARRAY && tp.getElementType().getSort() == Type.BYTE) {
                mv.visitInsn(Opcodes.DUP);
                mv.visitVarInsn(Opcodes.ALOAD, 1);// stat
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, START_METHOD, START_SIGNATURE, false);
            }
        }
        mv.visitInsn(opcode);
    }
}
