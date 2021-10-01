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
package scouter.agent;

import scouter.agent.asm.ILASM;
import scouter.agent.asm.ScouterClassWriter;
import scouter.agent.asm.asyncsupport.LambdaFormASM;
import scouter.agent.asm.util.AsmUtil;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import scouter.util.StringUtil;

import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

public class LambdaFormTransformer {
    protected static List<ILASM> asms = new ArrayList<ILASM>();
    private static List<String> scanScopePrefix = new ArrayList<String>();

    static {
        Configure conf = Configure.getInstance();
        if(conf.hook_async_callrunnable_enabled) {
            String[] prefixes = StringUtil.split(conf.hook_async_callrunnable_scan_package_prefixes, ',');
            for(int i=0; i<prefixes.length; i++) {
                scanScopePrefix.add(prefixes[i].replace('.', '/'));
            }
        }

        asms.add(new LambdaFormASM());
    }

    public byte[] transform(final ClassLoader loader, String className, final Class classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer,
                            String lambdaMethodName,
                            String lambdaMethodDesc,
                            String factoryMethodName,
                            String factoryMethodDesc) throws IllegalClassFormatException {
        try {
            if (className == null) return null;

            boolean scoped = false;

            for(int i=0; i<scanScopePrefix.size(); i++) {
                if(className.indexOf(scanScopePrefix.get(i)) == 0) {
                    scoped = true;
                    break;
                }
            }

            if (!scoped) {
                return null;
            }

            final ClassDesc classDesc = new ClassDesc();
            ClassReader cr = new ClassReader(classfileBuffer);
            cr.accept(new ClassVisitor(Opcodes.ASM9) {
                public void visit(int version, int access, String name, String signature, String superName,
                                  String[] interfaces) {
                    classDesc.set(version, access, name, signature, superName, interfaces);
                    super.visit(version, access, name, signature, superName, interfaces);
                }
            }, 0);
            if (AsmUtil.isInterface(classDesc.access)) {
                return null;
            }
            classDesc.classBeingRedefined = classBeingRedefined;
            ClassWriter cw = getClassWriter(classDesc);
            ClassVisitor cv = cw;
            List<ILASM> workAsms = asms;
            for (int i = workAsms.size() - 1; i >= 0; i--) {
                cv = workAsms.get(i).transform(cv, className, classDesc, lambdaMethodName, lambdaMethodDesc, factoryMethodName, factoryMethodDesc);
                if (cv != cw) {
                    cr = new ClassReader(classfileBuffer);
                    cr.accept(cv, ClassReader.EXPAND_FRAMES);
                    classfileBuffer = cw.toByteArray();
                    cv = cw = getClassWriter(classDesc);
                }
            }
            return classfileBuffer;
        } catch (Throwable t) {
            Logger.println("B101", "LambdaFormTransformer Error", t);
            t.printStackTrace();
        } finally {
        }
        return null;
    }

    private ClassWriter getClassWriter(final ClassDesc classDesc) {
        ClassWriter cw;
        switch (classDesc.version) {
            case Opcodes.V1_1:
            case Opcodes.V1_2:
            case Opcodes.V1_3:
            case Opcodes.V1_4:
            case Opcodes.V1_5:
            case Opcodes.V1_6:
                cw = new ScouterClassWriter(ClassWriter.COMPUTE_MAXS);
                break;
            default:
                cw = new ScouterClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        }
        return cw;
    }

}
