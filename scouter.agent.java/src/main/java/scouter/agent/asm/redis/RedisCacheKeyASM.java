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
import scouter.agent.Logger;
import scouter.agent.asm.IASM;
import scouter.agent.asm.util.AsmUtil;
import scouter.agent.asm.util.HookingSet;
import scouter.agent.trace.TraceMain;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;

import java.util.ArrayList;
import java.util.List;

import static scouter.agent.asm.redis.RedisCacheKeyASM.KEY_ELEMENT_FIELD;
import static scouter.agent.asm.redis.RedisCacheKeyASM.KEY_ELEMENT_FIELD_DESC;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2018. 3. 20.
 */
public class RedisCacheKeyASM implements IASM, Opcodes {
    public static final String KEY_ELEMENT_FIELD = "keyElement";
    public static final String KEY_ELEMENT_FIELD_DESC = "Ljava/lang/Object;";

    private Configure conf = Configure.getInstance();

    private static List<String> pattern = new ArrayList<String>();
    static {
        pattern.add("org.springframework.data.redis.cache.RedisCacheKey.getKeyBytes()[B");
    }
    private List<HookingSet> targetList;

    public RedisCacheKeyASM() {
        targetList = HookingSet.getHookingMethodSet(HookingSet.buildPatterns("", pattern));
    }

    public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
        if (conf._hook_redis_enabled == false)
            return cv;

        for (int i = 0; i < targetList.size(); i++) {
            HookingSet mset = targetList.get(i);
            if (mset.classMatch.include(className)) {
                return new RedisCacheKeyCV(cv, mset, className);
            }
        }

        return cv;
    }
}

class RedisCacheKeyCV extends ClassVisitor implements Opcodes {
    String className;
    HookingSet mset;
    boolean existKeyElementField;

    public RedisCacheKeyCV(ClassVisitor cv, HookingSet mset, String className) {
        super(ASM9, cv);
        this.mset = mset;
        this.className = className;
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if (name.equals(KEY_ELEMENT_FIELD) && desc.equals(KEY_ELEMENT_FIELD_DESC)) {
            existKeyElementField = true;
        }
        return super.visitField(access, name, desc, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (!existKeyElementField) {
            Logger.println("A902", "Ignore hooking - No Field " + KEY_ELEMENT_FIELD + " on " + className);
            return mv;
        }

        if (mv == null || mset.isA(name, desc) == false) {
            return mv;
        }

        if (AsmUtil.isSpecial(name)) {
            return mv;
        }

        return new GetKeyBytesMV(access, this.className, name, desc, mv);
    }
}

class GetKeyBytesMV extends LocalVariablesSorter implements Opcodes {

    private static final String TRACEMAIN = TraceMain.class.getName().replace('.', '/');
    private static final String START_METHOD = "setRedisKey";
    private static final String START_SIGNATURE = "([BLjava/lang/Object;)V";

    private String className;
    private String name;
    private String desc;
    private Type returnType;

    public GetKeyBytesMV(int access, String className, String name, String desc, MethodVisitor mv) {
        super(ASM9, access, desc, mv);
        this.className = className;
        this.name = name;
        this.desc = desc;
        this.returnType = Type.getReturnType(desc);
    }

    public void visitInsn(int opcode) {
        if ((opcode >= IRETURN && opcode <= RETURN)) {
            Type tp = returnType;
            if (tp.getSort() == Type.ARRAY && tp.getElementType().getSort() == Type.BYTE) {
                mv.visitInsn(Opcodes.DUP);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, this.className, KEY_ELEMENT_FIELD, "Ljava/lang/Object;");
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, START_METHOD, START_SIGNATURE, false);
            }
        }
        mv.visitInsn(opcode);
    }
}
