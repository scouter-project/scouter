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

import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;
import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.asm.util.AsmUtil;
import scouter.util.StringUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * extract spring REST annotation as the service name
 * @author Gun Lee (gunlee01@gmail.com)
 */
public class SpringReqMapASM implements IASM, Opcodes {

    static String springControllerNames[] = {"Lorg/springframework/stereotype/Controller;",
                                             "Lorg/springframework/web/bind/annotation/RestController;"};

    static Set<String> springRequestMappingAnnotations = new HashSet<String>();
    static {
        springRequestMappingAnnotations.add("Lorg/springframework/web/bind/annotation/RequestMapping;");
        springRequestMappingAnnotations.add("Lorg/springframework/web/bind/annotation/GetMapping;");
        springRequestMappingAnnotations.add("Lorg/springframework/web/bind/annotation/PostMapping;");
        springRequestMappingAnnotations.add("Lorg/springframework/web/bind/annotation/PutMapping;");
        springRequestMappingAnnotations.add("Lorg/springframework/web/bind/annotation/DeleteMapping;");
        springRequestMappingAnnotations.add("Lorg/springframework/web/bind/annotation/PatchMapping;");

    }

    Configure conf = Configure.getInstance();

    public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
        if (conf._hook_spring_rest_enabled == false)
            return cv;
        if (classDesc.annotation != null) {
            for (int i = 0; i < SpringReqMapASM.springControllerNames.length; i++) {
                if (classDesc.annotation.contains(SpringReqMapASM.springControllerNames[i])) {
                    return new SpringReqMapCV(cv, className);
                }
            }
        }
        return cv;
    }
}

class SpringReqMapCV extends ClassVisitor implements Opcodes {
    public String className;
    public String classRequestMappingUrl;

    public SpringReqMapCV(ClassVisitor cv, String className) {
        super(ASM9, cv);
        this.className = className;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        AnnotationVisitor av = super.visitAnnotation(desc, visible);
        if (av == null)
            return av;
        if (SpringReqMapASM.springRequestMappingAnnotations.contains(desc)) {
            return new SpringReqMapCVAV(av);
        }
        return av;
    }

    @Override
    public MethodVisitor visitMethod(int access, String methodName, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, methodName, desc, signature, exceptions);
        if (mv == null) {
            return mv;
        }
        if (AsmUtil.isSpecial(methodName)) {
            return mv;
        }
        return new SpringReqMapMV(className, access, methodName, desc, mv);
    }

    class SpringReqMapCVAV extends AnnotationVisitor implements Opcodes {
        public SpringReqMapCVAV(AnnotationVisitor av) {
            super(ASM9, av);
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            AnnotationVisitor av = super.visitArray(name);
            if (av == null)
                return av;
            if ("value".equals(name) || "path".equals(name)) {
                return new SpringReqMapCVAVAV(av);
            }
            return av;
        }
    }

    class SpringReqMapCVAVAV extends AnnotationVisitor implements Opcodes {
        public SpringReqMapCVAVAV(AnnotationVisitor av) {
            super(ASM9, av);
        }

        @Override
        public void visit(String name, Object value) {
            super.visit(name, value);
            String v = value.toString();
            if (StringUtil.isNotEmpty(v)) {
                classRequestMappingUrl = v;
            }
        }
    }

    class SpringReqMapMV extends LocalVariablesSorter implements Opcodes {
        private static final String TRACEMAIN = "scouter/agent/trace/TraceMain";
        private final static String SET_METHOD = "setSpringControllerName";
        private static final String SET_METHOD_SIGNATURE = "(Ljava/lang/String;)V";

        private final static String CONTROLLER_START_METHOD = "startSpringControllerMethod";
        private static final String CONTROLLER_START_METHOD_SIGNATURE = "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;)V";

        private String methodRequestMappingUrl;
        private String methodType;
        private boolean isRequestHandler = false;

        private String className;
        private int access;
        private String methodName;
        private String desc;

        public SpringReqMapMV(String className, int access, String methodName, String desc, MethodVisitor mv) {
            super(ASM9, access, desc, mv);
            this.className = className;
            this.access = access;
            this.methodName = methodName;
            this.desc = desc;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            AnnotationVisitor av = super.visitAnnotation(desc, visible);
            if (SpringReqMapASM.springRequestMappingAnnotations.contains(desc)) {
                if (!desc.endsWith("RequestMapping;")) {
                    String pkg = "web/bind/annotation/";
                    String postfix = "Mapping;";
                    int index = desc.indexOf(pkg);
                    if (index > -1) {
                        int startIndex = index + pkg.length();
                        index = desc.lastIndexOf(postfix);
                        if (index > startIndex) {
                            int lastIndex = index;
                            this.methodType = desc.substring(startIndex, lastIndex).toUpperCase();
                        }
                    }
                }
                return new SpringReqMapMVAV(av);
            }
            return av;
        }

        @Override
        public void visitCode() {
            if (isRequestHandler) {
                StringBuilder sb = new StringBuilder(60);
                sb.append(StringUtil.trimEmpty(classRequestMappingUrl))
                        .append(StringUtil.trimEmpty(methodRequestMappingUrl));

                if (!StringUtil.isEmpty(methodType)) {
                    sb.append("<").append(methodType).append(">");
                }

                String serviceUrl = sb.toString();
                Logger.println("[Apply Spring F/W REST URL] " + serviceUrl);
                AsmUtil.PUSH(mv, serviceUrl);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, SET_METHOD, SET_METHOD_SIGNATURE, false);

                //=========== call for spring request mapping method capture plugin ============
                Type[] types = Type.getArgumentTypes(desc);
                boolean isStatic = (access & ACC_STATIC) != 0;

                int sidx = isStatic ? 0 : 1;

                int arrVarIdx = newLocal(Type.getType("[Ljava/lang/Object;"));
                AsmUtil.PUSH(mv, types.length);
                mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
                mv.visitVarInsn(Opcodes.ASTORE, arrVarIdx);

                for (int i = 0; i < types.length; i++) {
                    Type type = types[i];
                    mv.visitVarInsn(Opcodes.ALOAD, arrVarIdx);
                    AsmUtil.PUSH(mv, i);

                    switch (type.getSort()) {
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
                    sidx += type.getSize();
                }
                AsmUtil.PUSH(mv, className);
                AsmUtil.PUSH(mv, methodName);
                AsmUtil.PUSH(mv, desc);
                if (isStatic) {
                    AsmUtil.PUSHNULL(mv);
                } else {
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                }
                mv.visitVarInsn(Opcodes.ALOAD, arrVarIdx);

                mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, CONTROLLER_START_METHOD, CONTROLLER_START_METHOD_SIGNATURE, false);

            }
            mv.visitCode();
        }

        class SpringReqMapMVAV extends AnnotationVisitor implements Opcodes {
            public SpringReqMapMVAV(AnnotationVisitor av) {
                super(ASM9, av);
            }

            @Override
            public AnnotationVisitor visitArray(String name) {
                AnnotationVisitor av = super.visitArray(name);
                if (av == null)
                    return av;
                if ("value".equals(name) || "method".equals(name) || "path".equals(name)) {
                    return new SpringReqMapMVAVAV(av, name);
                }
                return av;
            }
        }

        class SpringReqMapMVAVAV extends AnnotationVisitor implements Opcodes {
            String paramName;

            public SpringReqMapMVAVAV(AnnotationVisitor av, String paramName) {
                super(ASM9, av);
                this.paramName = paramName;
            }

            @Override
            public void visit(String name, Object value) {
                super.visit(name, value);

                if (!"value".equals(paramName) && !"path".equals(paramName)) {
                    return;
                }

                if (value instanceof String) {
                    String sValue = (String) value;
                    methodRequestMappingUrl = sValue;
                    isRequestHandler = true;
                }
            }

            @Override
            public void visitEnum(String name, String desc, String value) {
                super.visitEnum(name, desc, value);

                if (!"method".equals(paramName)) {
                    return;
                }

                methodType = value;
                isRequestHandler = true;
            }
        }
    }
}
