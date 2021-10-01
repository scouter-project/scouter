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
package scouter.agent.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;
import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.asm.util.AsmUtil;
import scouter.agent.asm.util.HookingSet;
import scouter.agent.trace.TraceApiCall;
import scouter.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ApicallASM implements IASM, Opcodes {
    private List<HookingSet> target = HookingSet.getHookingMethodSet(Configure.getInstance().hook_apicall_patterns);
    private Map<String, HookingSet> reserved = new HashMap<String, HookingSet>();
    protected static Set<String> onlyStartClass = new HashSet<String>();

    public static class ApiCallTargetRegister {
        public static final List<Pair<String,String>> klassMethod = new ArrayList<Pair<String,String>>();
        public static void regist(String klass, String method) {
            klassMethod.add(new Pair<String, String>(klass, method));
        }
    }

    public ApicallASM() {
        AsmUtil.add(reserved, "sun/net/www/protocol/http/HttpURLConnection", "getInputStream()Ljava/io/InputStream;");
        AsmUtil.add(reserved, "sun/net/www/protocol/http/HttpURLConnection", "connect()V");
        AsmUtil.add(reserved, "org/apache/commons/httpclient/HttpClient", "executeMethod("
                + "Lorg/apache/commons/httpclient/HostConfiguration;"
                + "Lorg/apache/commons/httpclient/HttpMethod;"
                + "Lorg/apache/commons/httpclient/HttpState;" + ")I");
        AsmUtil.add(reserved, "org/apache/http/impl/client/InternalHttpClient", "doExecute");
        AsmUtil.add(reserved, "sun/net/www/http/HttpClient", "parseHTTP");
        AsmUtil.add(reserved, "org/apache/http/impl/client/AbstractHttpClient",//
                "execute(Lorg/apache/http/HttpHost;"
                        + "Lorg/apache/http/HttpRequest;"
                        + "Lorg/apache/http/protocol/HttpContext;)Lorg/apache/http/HttpResponse;");
        // JCO CLIENT 추가..
        AsmUtil.add(reserved, "com/sap/mw/jco/JCO$Client", "execute(Ljava/lang/String;" + //
                "Lcom/sap/mw/jco/JCO$ParameterList;" + //
                "Lcom/sap/mw/jco/JCO$ParameterList;" + //
                "Lcom/sap/mw/jco/JCO$ParameterList;" + //
                "Lcom/sap/mw/jco/JCO$ParameterList;" + //
                "Ljava/lang/String;Ljava/lang/String;I)V");
        AsmUtil.add(reserved, "io/reactivex/netty/protocol/http/client/HttpClientImpl", "submit");

        AsmUtil.add(reserved, "org/springframework/web/client/RestTemplate",
                "doExecute(" +
                        "Ljava/net/URI;" +
                        "Lorg/springframework/http/HttpMethod;" +
                        "Lorg/springframework/web/client/RequestCallback;" +
                        "Lorg/springframework/web/client/ResponseExtractor;" +
                        ")Ljava/lang/Object;");
        AsmUtil.add(reserved, "org/springframework/web/client/AsyncRestTemplate",
                "doExecute(" +
                        "Ljava/net/URI;" +
                        "Lorg/springframework/http/HttpMethod;" +
                        "Lorg/springframework/web/client/AsyncRequestCallback;" +
                        "Lorg/springframework/web/client/ResponseExtractor;" +
                        ")Lorg/springframework/util/concurrent/ListenableFuture;");

        AsmUtil.add(reserved, "jdk/internal/net/http/HttpClientImpl", "send(" +
                "Ljava/net/http/HttpRequest;" +
                "Ljava/net/http/HttpResponse$BodyHandler;" +
                ")Ljava/net/http/HttpResponse;");

        AsmUtil.add(reserved, "org/springframework/web/reactive/function/client/ExchangeFunctions$DefaultExchangeFunction", "exchange(" +
                "Lorg/springframework/web/reactive/function/client/ClientRequest;" +
                ")Lreactor/core/publisher/Mono;");
        for(int i = ApiCallTargetRegister.klassMethod.size() - 1; i >= 0; i--) {
            AsmUtil.add(reserved, ApiCallTargetRegister.klassMethod.get(i).getLeft(), ApiCallTargetRegister.klassMethod.get(i).getRight());
        }
        onlyStartClass.add("org/springframework/web/reactive/function/client/ExchangeFunctions$DefaultExchangeFunction");
    }

    public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
        if (Configure.getInstance()._hook_apicall_enabled == false) {
            return cv;
        }
        HookingSet mset = reserved.get(className);
        if (mset != null)
            return new ApicallExtCV(cv, mset, className);
        for (int i = 0; i < target.size(); i++) {
            mset = target.get(i);
            if (mset.classMatch.include(className)) {
                return new ApicallExtCV(cv, mset, className);
            }
        }
        return cv;
    }
}

class ApicallExtCV extends ClassVisitor implements Opcodes {
    public String className;
    private HookingSet mset;

    public ApicallExtCV(ClassVisitor cv, HookingSet mset, String className) {
        super(ASM9, cv);
        this.mset = mset;
        this.className = className;
    }

    @Override
    public MethodVisitor visitMethod(int access, String methodName, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, methodName, desc, signature, exceptions);
        if (mv == null || mset.isA(methodName, desc) == false) {
            return mv;
        }
        if (AsmUtil.isSpecial(methodName)) {
            return mv;
        }
        Logger.println("apicall: " + className + "." + methodName + desc);
        return new ApicallExtMV(access, desc, mv, Type.getArgumentTypes(desc), (access & ACC_STATIC) != 0, className,
                methodName, desc);
    }
}

class ApicallExtMV extends LocalVariablesSorter implements Opcodes {
    private static final String TRACESUBCALL = TraceApiCall.class.getName().replace('.', '/');
    private final static String START_METHOD = "startApicall";
    private static final String START_SIGNATURE = "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;";
    private final static String END_METHOD = "endApicall";
    private static final String END_SIGNATURE = "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Throwable;)V";
    private Label startFinally = new Label();

    public ApicallExtMV(int access, String desc, MethodVisitor mv, Type[] paramTypes, boolean isStatic,
                        String classname, String methodname, String methoddesc) {
        super(ASM9, access, desc, mv);
        this.paramTypes = paramTypes;
        this.returnType = Type.getReturnType(desc);
        this.isStatic = isStatic;
        this.className = classname;
        this.methodName = methodname;
        this.methodDesc = methoddesc;
    }

    private Type[] paramTypes;
    private Type returnType;
    private boolean isStatic;
    private String className;
    private String methodName;
    private String methodDesc;
    private int statIdx;

    public void visitCode() {
        int sidx = isStatic ? 0 : 1;
        int arrVarIdx = newLocal(Type.getType("[Ljava/lang/Object;"));
        AsmUtil.PUSH(mv, paramTypes.length);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
        mv.visitVarInsn(Opcodes.ASTORE, arrVarIdx);
        for (int i = 0; i < paramTypes.length; i++) {
            Type tp = paramTypes[i];
            mv.visitVarInsn(Opcodes.ALOAD, arrVarIdx);
            AsmUtil.PUSH(mv, i);
            switch (tp.getSort()) {
                case Type.BOOLEAN:
                    mv.visitVarInsn(Opcodes.ILOAD, sidx);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;",
                            false);
                    break;
                case Type.BYTE:
                    mv.visitVarInsn(Opcodes.ILOAD, sidx);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
                    break;
                case Type.CHAR:
                    mv.visitVarInsn(Opcodes.ILOAD, sidx);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;",
                            false);
                    break;
                case Type.SHORT:
                    mv.visitVarInsn(Opcodes.ILOAD, sidx);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
                    break;
                case Type.INT:
                    mv.visitVarInsn(Opcodes.ILOAD, sidx);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;",
                            false);
                    break;
                case Type.LONG:
                    mv.visitVarInsn(Opcodes.LLOAD, sidx);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
                    break;
                case Type.FLOAT:
                    mv.visitVarInsn(Opcodes.FLOAD, sidx);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
                    break;
                case Type.DOUBLE:
                    mv.visitVarInsn(Opcodes.DLOAD, sidx);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                    break;
                default:
                    mv.visitVarInsn(Opcodes.ALOAD, sidx);
            }
            mv.visitInsn(Opcodes.AASTORE);
            sidx += tp.getSize();
        }
        AsmUtil.PUSH(mv, className);
        AsmUtil.PUSH(mv, methodName);
        AsmUtil.PUSH(mv, methodDesc);
        if (isStatic) {
            AsmUtil.PUSHNULL(mv);
        } else {
            mv.visitVarInsn(Opcodes.ALOAD, 0);
        }
        mv.visitVarInsn(Opcodes.ALOAD, arrVarIdx);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACESUBCALL, START_METHOD, START_SIGNATURE, false);
        statIdx = newLocal(Type.getType(Object.class));
        mv.visitVarInsn(Opcodes.ASTORE, statIdx);
        mv.visitLabel(startFinally);
        mv.visitCode();
    }

    @Override
    public void visitInsn(int opcode) {
        if ((opcode >= IRETURN && opcode <= RETURN)) {
            capReturn();
        }
        mv.visitInsn(opcode);
    }

    private void capReturn() {
        if (ApicallASM.onlyStartClass.contains(className)) {
            return;
        }
        Type tp = returnType;
        if (tp == null || tp.equals(Type.VOID_TYPE)) {
            mv.visitVarInsn(Opcodes.ALOAD, statIdx);
            mv.visitInsn(Opcodes.ACONST_NULL);
            mv.visitInsn(Opcodes.ACONST_NULL);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACESUBCALL, END_METHOD, END_SIGNATURE, false);
            return;
        }
        int i = newLocal(tp);
        switch (tp.getSort()) {
            case Type.BOOLEAN:
                mv.visitVarInsn(Opcodes.ISTORE, i);
                mv.visitVarInsn(Opcodes.ILOAD, i);
                mv.visitVarInsn(Opcodes.ALOAD, statIdx);// stat
                mv.visitVarInsn(Opcodes.ILOAD, i);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
                mv.visitInsn(Opcodes.ACONST_NULL);// throwable
                break;
            case Type.BYTE:
                mv.visitVarInsn(Opcodes.ISTORE, i);
                mv.visitVarInsn(Opcodes.ILOAD, i);
                mv.visitVarInsn(Opcodes.ALOAD, statIdx);// stat
                mv.visitVarInsn(Opcodes.ILOAD, i);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
                mv.visitInsn(Opcodes.ACONST_NULL);// throwable
                break;
            case Type.CHAR:
                mv.visitVarInsn(Opcodes.ISTORE, i);
                mv.visitVarInsn(Opcodes.ILOAD, i);
                mv.visitVarInsn(Opcodes.ALOAD, statIdx);// stat
                mv.visitVarInsn(Opcodes.ILOAD, i);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;",
                        false);
                mv.visitInsn(Opcodes.ACONST_NULL);// throwable
                break;
            case Type.SHORT:
                mv.visitVarInsn(Opcodes.ISTORE, i);
                mv.visitVarInsn(Opcodes.ILOAD, i);
                mv.visitVarInsn(Opcodes.ALOAD, statIdx);// stat
                mv.visitVarInsn(Opcodes.ILOAD, i);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
                mv.visitInsn(Opcodes.ACONST_NULL);// throwable
                break;
            case Type.INT:
                mv.visitVarInsn(Opcodes.ISTORE, i);
                mv.visitVarInsn(Opcodes.ILOAD, i);
                mv.visitVarInsn(Opcodes.ALOAD, statIdx);// stat
                mv.visitVarInsn(Opcodes.ILOAD, i);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                mv.visitInsn(Opcodes.ACONST_NULL);// throwable
                break;
            case Type.LONG:
                mv.visitVarInsn(Opcodes.LSTORE, i);
                mv.visitVarInsn(Opcodes.LLOAD, i);
                mv.visitVarInsn(Opcodes.ALOAD, statIdx);// stat
                mv.visitVarInsn(Opcodes.LLOAD, i);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
                mv.visitInsn(Opcodes.ACONST_NULL);// throwable
                break;
            case Type.FLOAT:
                mv.visitVarInsn(Opcodes.FSTORE, i);
                mv.visitVarInsn(Opcodes.FLOAD, i);
                mv.visitVarInsn(Opcodes.ALOAD, statIdx);// stat
                mv.visitVarInsn(Opcodes.FLOAD, i);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
                mv.visitInsn(Opcodes.ACONST_NULL);// throwable
                break;
            case Type.DOUBLE:
                mv.visitVarInsn(Opcodes.DSTORE, i);
                mv.visitVarInsn(Opcodes.DLOAD, i);
                mv.visitVarInsn(Opcodes.ALOAD, statIdx);// stat
                mv.visitVarInsn(Opcodes.DLOAD, i);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                mv.visitInsn(Opcodes.ACONST_NULL);// throwable
                break;
            default:
                mv.visitVarInsn(Opcodes.ASTORE, i);
                mv.visitVarInsn(Opcodes.ALOAD, i);
                mv.visitVarInsn(Opcodes.ALOAD, statIdx);// stat
                mv.visitVarInsn(Opcodes.ALOAD, i);// return
                mv.visitInsn(Opcodes.ACONST_NULL);// throwable
        }
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACESUBCALL, END_METHOD, END_SIGNATURE, false);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        Label endFinally = new Label();
        mv.visitTryCatchBlock(startFinally, endFinally, endFinally, null);
        mv.visitLabel(endFinally);
        mv.visitInsn(DUP);
        int errIdx = newLocal(Type.getType(Throwable.class));
        mv.visitVarInsn(Opcodes.ASTORE, errIdx);
        mv.visitVarInsn(Opcodes.ALOAD, statIdx);// stat
        mv.visitInsn(Opcodes.ACONST_NULL);// return
        mv.visitVarInsn(Opcodes.ALOAD, errIdx);// throwable
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACESUBCALL, END_METHOD, END_SIGNATURE, false);
        mv.visitInsn(ATHROW);
        mv.visitMaxs(maxStack + 8, maxLocals + 2);
    }
}
