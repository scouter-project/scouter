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

package scouter.agent.asm.mongodb;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;
import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.asm.IASM;
import scouter.agent.trace.TraceMongoDB;

import static scouter.agent.trace.TraceMongoDB.V364;
import static scouter.agent.trace.TraceMongoDB.V382;
import static scouter.agent.trace.TraceMongoDB.V405;

public class MongoCommandProtocolASM implements IASM, Opcodes {

    private Configure conf = Configure.getInstance();

    public MongoCommandProtocolASM() {
    }

    public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
        if (!conf.hook_mongodb_enabled) {
            return cv;
        }

        if ("com/mongodb/internal/connection/CommandProtocolImpl".equals(className) ||
                "com/mongodb/connection/CommandProtocolImpl".equals(className)
        ) {
            return new MongoCommandProtocolCV(cv, className);
        }
        return cv;
    }

    static class MongoCommandProtocolCV extends ClassVisitor implements Opcodes {
        public String className;

        public MongoCommandProtocolCV(ClassVisitor cv, String className) {
            super(ASM9, cv);
            this.className = className;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            int newAccess = (access | ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED) - ACC_PRIVATE - ACC_PROTECTED;
            super.visit(version, newAccess, name, signature, superName, interfaces);
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            int newAccess = (access | ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED) - ACC_PRIVATE - ACC_PROTECTED;
            if (name.equals("namespace") && descriptor.equals("Lcom/mongodb/MongoNamespace;")) {
                namespace = true;
            } else if (name.equals("command") && descriptor.equals("Lorg/bson/BsonDocument;")) {
                command = true;
            } else if (name.equals("readPreference") && descriptor.equals("Lcom/mongodb/ReadPreference;")) {
                readPreference = true;
            } else if (name.equals("payload")
                    && (descriptor.equals("Lcom/mongodb/internal/connection/SplittablePayload;")
                    || descriptor.equals("Lcom/mongodb/connection/SplittablePayload;"))) {
                payload = true;

                if (descriptor.equals("Lcom/mongodb/connection/SplittablePayload;")) {
                    version = V382;
                }
            }
            return super.visitField(newAccess, name, descriptor, signature, value);
        }

        boolean namespace;
        boolean command;
        boolean readPreference;
        boolean payload;
        String version = V405;

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            if (mv == null) {
                return mv;
            }

            if ("executeAsync".equals(name) && desc.startsWith("(Lcom/mongodb/internal/connection/InternalConnection;Lcom/mongodb/internal/async/SingleResultCallback;)V")) {
                return new ExecuteAsyncMV(access, desc, mv, className, namespace, command, readPreference, payload, V405);

            } else if ("executeAsync".equals(name) && desc.startsWith("(Lcom/mongodb/internal/connection/InternalConnection;Lcom/mongodb/async/SingleResultCallback;)V")) {
                return new ExecuteAsyncMV(access, desc, mv, className, namespace, command, readPreference, payload, V382);

            } else if ("execute".equals(name) && desc.startsWith("(Lcom/mongodb/internal/connection/InternalConnection;)")) {
                return new ExecuteMV(access, desc, mv, className, namespace, command, readPreference, payload, version);

            } else if ("execute".equals(name) && desc.startsWith("(Lcom/mongodb/connection/InternalConnection;)")) {
                return new ExecuteMV(access, desc, mv, className, namespace, command, readPreference, payload, V364);
            }
            return mv;
        }
    }

    static class ExecuteAsyncMV extends LocalVariablesSorter implements Opcodes {
        private static final String TRACE = TraceMongoDB.class.getName().replace('.', '/');
        private final static String METHOD = "startExecuteAsync";
        private static final String SIGNATURE = "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;" +
                "Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;";

        private String className;

        boolean namespace;
        boolean command;
        boolean readPreference;
        boolean payload;
        String version;

        public ExecuteAsyncMV(int access, String desc, MethodVisitor mv, String className, boolean namespace,
                              boolean command, boolean readPreference, boolean payload, String version) {
            super(ASM9, access, desc, mv);
            this.className = className;
            this.namespace = namespace;
            this.command = command;
            this.readPreference = readPreference;
            this.payload = payload;
            this.version = version;
        }

        @Override
        public void visitCode() {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            if (namespace) {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "namespace", "Lcom/mongodb/MongoNamespace;");
            } else {
                mv.visitInsn(Opcodes.ACONST_NULL);
            }
            if (command) {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "command", "Lorg/bson/BsonDocument;");
            } else {
                mv.visitInsn(Opcodes.ACONST_NULL);
            }
            if (readPreference) {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "readPreference", "Lcom/mongodb/ReadPreference;");
            } else {
                mv.visitInsn(Opcodes.ACONST_NULL);
            }
            if (payload) {
                mv.visitVarInsn(ALOAD, 0);
                if (version.equals(V405)) {
                    mv.visitFieldInsn(GETFIELD, className, "payload", "Lcom/mongodb/internal/connection/SplittablePayload;");
                } else {
                    mv.visitFieldInsn(GETFIELD, className, "payload", "Lcom/mongodb/connection/SplittablePayload;");
                }
            } else {
                mv.visitInsn(Opcodes.ACONST_NULL);
            }
            mv.visitVarInsn(ALOAD, 2);
            mv.visitLdcInsn(version);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE, METHOD, SIGNATURE, false);
            if (version.equals(V405)) {
                mv.visitTypeInsn(CHECKCAST, "com/mongodb/internal/async/SingleResultCallback");
            } else {
                mv.visitTypeInsn(CHECKCAST, "com/mongodb/async/SingleResultCallback");
            }
            mv.visitVarInsn(ASTORE, 2);
            mv.visitCode();
        }
    }

    static class ExecuteMV extends LocalVariablesSorter implements Opcodes {
        private static final String TRACE = TraceMongoDB.class.getName().replace('.', '/');
        private final static String METHOD = "startExecute";
        private static final String SIGNATURE = "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;" +
                "Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;";

        private final static String END_METHOD = "endExecute";
        private final static String END_SIGNATURE = "(Ljava/lang/Object;Ljava/lang/Throwable;)V";;

        private String className;

        boolean namespace;
        boolean command;
        boolean readPreference;
        boolean payload;
        String version;

        private int statIdx;
        private Label startFinally = new Label();

        public ExecuteMV(int access, String desc, MethodVisitor mv, String className, boolean namespace,
                              boolean command, boolean readPreference, boolean payload, String version) {
            super(ASM9, access, desc, mv);
            this.className = className;
            this.namespace = namespace;
            this.command = command;
            this.readPreference = readPreference;
            this.payload = payload;
            this.version = version;
        }

        @Override
        public void visitCode() {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            if (namespace) {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "namespace", "Lcom/mongodb/MongoNamespace;");
            } else {
                mv.visitInsn(Opcodes.ACONST_NULL);
            }
            if (command) {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "command", "Lorg/bson/BsonDocument;");
            } else {
                mv.visitInsn(Opcodes.ACONST_NULL);
            }
            if (readPreference) {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "readPreference", "Lcom/mongodb/ReadPreference;");
            } else {
                mv.visitInsn(Opcodes.ACONST_NULL);
            }
            if (payload) {
                mv.visitVarInsn(ALOAD, 0);
                if (version.equals(V405)) {
                    mv.visitFieldInsn(GETFIELD, className, "payload", "Lcom/mongodb/internal/connection/SplittablePayload;");
                } else {
                    mv.visitFieldInsn(GETFIELD, className, "payload", "Lcom/mongodb/connection/SplittablePayload;");
                }
            } else {
                mv.visitInsn(Opcodes.ACONST_NULL);
            }
            mv.visitLdcInsn(version);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE, METHOD, SIGNATURE, false);

            statIdx = newLocal(Type.getType(Object.class));
            mv.visitVarInsn(Opcodes.ASTORE, statIdx);
            mv.visitLabel(startFinally);

            mv.visitCode();
        }

        @Override
        public void visitInsn(int opcode) {
            if ((opcode >= IRETURN && opcode <= RETURN)) {
                mv.visitVarInsn(Opcodes.ALOAD, statIdx);
                mv.visitInsn(Opcodes.ACONST_NULL);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE, END_METHOD, END_SIGNATURE, false);
            }
            mv.visitInsn(opcode);
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            Label endFinally = new Label();
            mv.visitTryCatchBlock(startFinally, endFinally, endFinally, null);
            mv.visitLabel(endFinally);
            mv.visitInsn(DUP);
            int errIdx = newLocal(Type.getType(Throwable.class));
            mv.visitVarInsn(Opcodes.ASTORE, errIdx);

            mv.visitVarInsn(Opcodes.ALOAD, statIdx);
            mv.visitVarInsn(Opcodes.ALOAD, errIdx);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE, END_METHOD, END_SIGNATURE, false);
            mv.visitInsn(ATHROW);
            mv.visitMaxs(maxStack + 8, maxLocals + 2);
        }
    }
}
