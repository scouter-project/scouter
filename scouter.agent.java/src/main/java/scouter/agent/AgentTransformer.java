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

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import scouter.agent.asm.AddFieldASM;
import scouter.agent.asm.ApiCallResponseObjectASM;
import scouter.agent.asm.ApicallASM;
import scouter.agent.asm.ApicallInfoASM;
import scouter.agent.asm.ApicallJavaHttpRequestASM;
import scouter.agent.asm.ApicallSpringHandleResponseASM;
import scouter.agent.asm.ApicallSpringHttpAccessorASM;
import scouter.agent.asm.ApicallWebClientInfoASM;
import scouter.agent.asm.ApicallWebClientResponseASM;
import scouter.agent.asm.CapArgsASM;
import scouter.agent.asm.CapReturnASM;
import scouter.agent.asm.CapThisASM;
import scouter.agent.asm.HttpReactiveServiceASM;
import scouter.agent.asm.HttpServiceASM;
import scouter.agent.asm.IASM;
import scouter.agent.asm.InitialContextASM;
import scouter.agent.asm.JDBCConnectionOpenASM;
import scouter.agent.asm.JDBCDriverASM;
import scouter.agent.asm.JDBCGetConnectionASM;
import scouter.agent.asm.JDBCPreparedStatementASM;
import scouter.agent.asm.JDBCResultSetASM;
import scouter.agent.asm.JDBCStatementASM;
import scouter.agent.asm.JspServletASM;
import scouter.agent.asm.MapImplASM;
import scouter.agent.asm.MethodASM;
import scouter.agent.asm.ScouterClassWriter;
import scouter.agent.asm.ServiceASM;
import scouter.agent.asm.SocketASM;
import scouter.agent.asm.SpringReqMapASM;
import scouter.agent.asm.SqlMapASM;
import scouter.agent.asm.UserExceptionASM;
import scouter.agent.asm.UserExceptionHandlerASM;
import scouter.agent.asm.UserTxASM;
import scouter.agent.asm.asyncsupport.AsyncContextDispatchASM;
import scouter.agent.asm.asyncsupport.CallRunnableASM;
import scouter.agent.asm.asyncsupport.CoroutineThreadNameASM;
import scouter.agent.asm.asyncsupport.HystrixCommandASM;
import scouter.agent.asm.asyncsupport.MonoKtASM;
import scouter.agent.asm.asyncsupport.RequestStartAsyncASM;
import scouter.agent.asm.asyncsupport.ThreadASM;
import scouter.agent.asm.asyncsupport.executor.ExecutorServiceASM;
import scouter.agent.asm.asyncsupport.spring.SpringAsyncExecutionASM;
import scouter.agent.asm.asyncsupport.spring.SpringAsyncExecutionAspectSupportDoSubmitASM;
import scouter.agent.asm.elasticsearch.HttpNioEntityASM;
import scouter.agent.asm.elasticsearch.RestClientASM;
import scouter.agent.asm.kafka.KafkaProducerASM;
import scouter.agent.asm.mongodb.MongoCommandProtocolASM;
import scouter.agent.asm.rabbit.RabbitPublisherASM;
import scouter.agent.asm.redis.JedisCommandASM;
import scouter.agent.asm.redis.JedisProtocolASM;
import scouter.agent.asm.redis.LettuceASM;
import scouter.agent.asm.redis.RedisCacheKeyASM;
import scouter.agent.asm.redis.RedisKeyASM;
import scouter.agent.asm.redis.RedissonReadWriteASM;
import scouter.agent.asm.modification.MongoModifyASM;
import scouter.agent.asm.modification.ReactorModifyASM;
import scouter.agent.asm.util.AsmUtil;
import scouter.agent.asm.weaver.WeaverClassASM;
import scouter.agent.util.AsyncRunner;
import scouter.lang.conf.ConfObserver;
import scouter.util.FileUtil;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AgentTransformer implements ClassFileTransformer {
    public static final String JAVA_UTIL_MAP = "java/util/Map";
    public static ThreadLocal<ClassLoader> hookingCtx = new ThreadLocal<ClassLoader>();
    protected static List<IASM> asms = new ArrayList<IASM>();
    // hook 관련 설정이 변경되면 자동으로 변경된다.
    private static int hook_signature;

    static {
        final Configure conf = Configure.getInstance();
        reload();
        hook_signature = conf.getHookSignature();
        ConfObserver.add("AgentTransformer", new Runnable() {
            public void run() {
                if (conf.getHookSignature() != hook_signature) {
                    reload();
                }
                hook_signature = conf.getHookSignature();
            }
        });
    }

    public static void reload() {
        Configure conf = Configure.getInstance();
        List<IASM> temp = new ArrayList<IASM>();
        temp.add(new ReactorModifyASM());
        temp.add(new MongoModifyASM());
        temp.add(new MongoCommandProtocolASM());

        temp.add(new ThreadASM());
        temp.add(new HttpServiceASM());
        temp.add(new ServiceASM());
        temp.add(new HttpReactiveServiceASM());
        temp.add(new CoroutineThreadNameASM());
        temp.add(new MonoKtASM());
        temp.add(new ApicallWebClientInfoASM());
        temp.add(new ApicallWebClientResponseASM());
        temp.add(new HttpNioEntityASM());
        temp.add(new RestClientASM());

        temp.add(new RequestStartAsyncASM());
        temp.add(new AsyncContextDispatchASM());

        temp.add(new JDBCPreparedStatementASM());
        temp.add(new JDBCResultSetASM());
        temp.add(new JDBCStatementASM());
        temp.add(new SqlMapASM());
        temp.add(new UserTxASM());

        temp.add(new JDBCGetConnectionASM());
        temp.add(new JDBCConnectionOpenASM());
        temp.add(new JDBCDriverASM());
        temp.add(new InitialContextASM());

        temp.add(new CapArgsASM());
        temp.add(new CapReturnASM());
        temp.add(new CapThisASM());

        temp.add(new MethodASM());
        temp.add(new ApicallASM());
        temp.add(new ApicallInfoASM());
        temp.add(new ApiCallResponseObjectASM());
        temp.add(new ApicallSpringHandleResponseASM());
        temp.add(new ApicallSpringHttpAccessorASM());
        temp.add(new ApicallJavaHttpRequestASM());

        temp.add(new SpringAsyncExecutionASM());
        temp.add(new SpringAsyncExecutionAspectSupportDoSubmitASM());
        temp.add(new CallRunnableASM());
        temp.add(new ExecutorServiceASM());

        temp.add(new JedisCommandASM());
        temp.add(new RedisKeyASM());
        temp.add(new RedisCacheKeyASM());
        temp.add(new JedisProtocolASM());
        temp.add(new LettuceASM());
        temp.add(new RedissonReadWriteASM());
        temp.add(new KafkaProducerASM());
        temp.add(new RabbitPublisherASM());

        temp.add(new SpringReqMapASM());
        temp.add(new HystrixCommandASM());

        temp.add(new SocketASM());
        temp.add(new JspServletASM());
        temp.add(new MapImplASM());
        temp.add(new UserExceptionASM());
        temp.add(new UserExceptionHandlerASM());

        temp.add(new AddFieldASM());
        temp.add(new WeaverClassASM());

        asms = temp;
    }

    // //////////////////////////////////////////////////////////////
    // boot class이지만 Hooking되어야하는 클래스를 등록한다.
    private static final HashMap<Integer, String> asynchook = new HashMap<Integer, String>();

    static {
        asynchook.put("sun/net/www/protocol/http/HttpURLConnection".hashCode(), "sun/net/www/protocol/http/HttpURLConnection");
        asynchook.put("sun/net/www/http/HttpClient".hashCode(), "sun/net/www/http/HttpClient");
        asynchook.put("java/net/Socket".hashCode(), "java/net/Socket");
        asynchook.put("java/nio/channels/SocketChannel".hashCode(), "java/nio/channels/SocketChannel");
        asynchook.put("sun/nio/ch/SocketChannelImpl".hashCode(), "sun/nio/ch/SocketChannelImpl");
        asynchook.put("javax/naming/InitialContext".hashCode(), "javax/naming/InitialContext");
    }

    private final Configure conf = Configure.getInstance();
    private Logger.FileLog bciOut;

    public byte[] transform(final ClassLoader loader, String className, final Class classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        try {
            hookingCtx.set(loader);

//            if(className != null && (className.indexOf("http") >= 0 || className.indexOf("Http") >= 0)) {
//                System.out.println("[!!!!!!!!] loading ...http className = " + className);
//            }

            if (className == null)
                return null;
            if (classBeingRedefined == null) {
                if (asynchook.containsKey(className.hashCode())) {
                    Logger.trace("[SCTRACE] Async hook class : " + asynchook.get(className.hashCode()));
                    AsyncRunner.getInstance().add(loader, className, classfileBuffer);
                    return null;
                }
                if (loader == null) {
                    if (conf._hook_boot_prefix == null || conf._hook_boot_prefix.length() == 0 || !className.startsWith(conf._hook_boot_prefix)) {
                        return null;
                    }
                }
            }
            if (className.startsWith("scouter/")) {
                return null;
            }
            //
            classfileBuffer = DirectPatch.patch(className, classfileBuffer);
            ObjTypeDetector.check(className);
            final ClassDesc classDesc = new ClassDesc();
            ClassReader cr = new ClassReader(classfileBuffer);
            cr.accept(new ClassVisitor(Opcodes.ASM9) {
                public void visit(int version, int access, String name, String signature, String superName,
                                  String[] interfaces) {
                    classDesc.set(version, access, name, signature, superName, interfaces);
                    if (conf._hook_map_impl_enabled) {
                        classDesc.isMapImpl = isMapImpl(superName, interfaces, loader);
                    }
                    super.visit(version, access, name, signature, superName, interfaces);
                }

                @Override
                public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                    classDesc.annotation += desc;
                    return super.visitAnnotation(desc, visible);
                }
            }, 0);
            if (AsmUtil.isInterface(classDesc.access)
                    && !"reactor/core/publisher/OptimizableOperator".equals(className)
                    && !"com/mongodb/connection/InternalConnection".equals(className)
            ) {
                return null;
            }

            classDesc.classBeingRedefined = classBeingRedefined;
            ClassWriter cw = getClassWriter(classDesc);
            ClassVisitor cv = cw;
            List<IASM> workAsms = asms;
            for (int i = workAsms.size() - 1; i >= 0; i--) {
                cv = workAsms.get(i).transform(cv, className, classDesc);
                if (cv != cw) {
                    cr = new ClassReader(classfileBuffer);
                    cr.accept(cv, ClassReader.EXPAND_FRAMES);
                    classfileBuffer = cw.toByteArray();
                    cv = cw = getClassWriter(classDesc);
                    if (conf._log_asm_enabled) {
                        if (this.bciOut == null) {
                            this.bciOut = new Logger.FileLog("./scouter.bci");
                        }
                        this.bciOut.println(className + "\t\t[" + loader + "]");
                    }
                }
            }
            return classfileBuffer;
        } catch (Throwable t) {
            Logger.println("A101", "Transformer Error : " + className, t);
            t.printStackTrace();
        } finally {
            hookingCtx.set(null);
        }
        return null;
    }

    private boolean isMapImpl(String superName, String[] interfaces, ClassLoader loader) {
        String[] classes = new String[interfaces.length + 1];
        System.arraycopy(interfaces, 0, classes, 0, interfaces.length);
        classes[classes.length-1] = superName;

        for (String aClass : classes) {
            if (isMapImpl(aClass, loader)) {
                return true;
            }
        }

        return false;
    }

    private boolean isMapImpl(String clazz, ClassLoader loader) {
        if("java/lang/Object".equals(clazz)) {
            return false;
        }

        if (JAVA_UTIL_MAP.equals(clazz)) {
            return true;
        }

        if(loader == null) {
            loader = ClassLoader.getSystemClassLoader();
        }

        InputStream in = loader.getResourceAsStream(clazz + ".class");

        try {
            ClassReader classReader = new ClassReader(in);
            String[] interfaces = classReader.getInterfaces();
            if (interfaces != null && interfaces.length > 0) {
                for (int i = 0; i < interfaces.length; i++) {
                    if(JAVA_UTIL_MAP.equals(interfaces[i])) {
                        return true;
                    }
                    isMapImpl(interfaces[i], loader);
                }
            }

            String superClassName = classReader.getSuperName();
            if(superClassName == null) {
                return false;
            }

            if(JAVA_UTIL_MAP.equals(superClassName)) {
                return true;
            }
            isMapImpl(superClassName, loader);

        } catch (IOException e) {
            System.out.println("[A189]-isMapImpl-check super class : " + clazz);
        }
        return false;
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

    private void dump(String className, byte[] bytes) {
        String fname = "/tmp/" + className.replace('/', '_');
        FileUtil.save(fname, bytes);
    }
}
