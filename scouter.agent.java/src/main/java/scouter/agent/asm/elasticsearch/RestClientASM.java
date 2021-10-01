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

package scouter.agent.asm.elasticsearch;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.LocalVariablesSorter;
import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.asm.IASM;
import scouter.agent.trace.TraceElasticSearch;

public class RestClientASM implements IASM, Opcodes {

	private Configure conf = Configure.getInstance();

	public RestClientASM() {
	}

	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
		if (conf._hook_elasticsearch_enabled == false) {
			return cv;
		}

		if ("org/elasticsearch/client/RestClient".equals(className)) {
			return new RestClientCV(cv, className);
		} else if ("org/elasticsearch/client/RequestLogger".equals(className)) {
			return new RequestLoggerCV(cv, className);
		}
		return cv;
	}

	static class RestClientCV extends ClassVisitor implements Opcodes {
		public String className;

		public RestClientCV(ClassVisitor cv, String className) {
			super(ASM9, cv);
			this.className = className;
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
			if (mv == null) {
				return mv;
			}

			if ("performRequestAsync".equals(name) && desc.startsWith("(JLorg/elasticsearch/client/RestClient$NodeTuple;Lorg/apache/http/client/methods/HttpRequestBase;")) {
				return new RestClientStartMV(access, desc, mv, className);
			}
			return mv;
		}
	}

	static class RestClientStartMV extends LocalVariablesSorter implements Opcodes {
		private static final String TRACE = TraceElasticSearch.class.getName().replace('.', '/');
		private final static String METHOD = "startRequest";
		private static final String SIGNATURE = "(Ljava/lang/Object;)V";

		private String className;

		public RestClientStartMV(int access, String desc, MethodVisitor mv, String className) {
			super(ASM9, access, desc, mv);
			this.className = className;
		}

		@Override
		public void visitCode() {
			mv.visitVarInsn(ALOAD, 4);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE, METHOD, SIGNATURE, false);
			mv.visitCode();
		}
	}


	static class RequestLoggerCV extends ClassVisitor implements Opcodes {
		public String className;

		public RequestLoggerCV(ClassVisitor cv, String className) {
			super(ASM9, cv);
			this.className = className;
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
			if (mv == null) {
				return mv;
			}
			if ("logResponse".equals(name)
					&& desc.startsWith("(Lorg/apache/commons/logging/Log;Lorg/apache/http/client/methods/HttpUriRequest;Lorg/apache/http/HttpHost;Lorg/apache/http/HttpResponse;")) {
				return new RequestLoggerMV(access, desc, mv, className);

			} else if ("logFailedRequest".equals(name)
					&& desc.startsWith("(Lorg/apache/commons/logging/Log;Lorg/apache/http/client/methods/HttpUriRequest;Lorg/elasticsearch/client/Node;Ljava/lang/Exception;")) {
				return new RequestFailLoggerMV(access, desc, mv, className);
			}
			return mv;
		}

		static class RequestLoggerMV extends LocalVariablesSorter implements Opcodes {
			private static final String TRACE = TraceElasticSearch.class.getName().replace('.', '/');
			private final static String METHOD = "endRequest";
			private static final String SIGNATURE = "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V";

			private String className;

			public RequestLoggerMV(int access, String desc, MethodVisitor mv, String className) {
				super(ASM9, access, desc, mv);
				this.className = className;
			}

			@Override
			public void visitCode() {
				mv.visitVarInsn(ALOAD, 1); //HttpUriRequest
				mv.visitVarInsn(ALOAD, 2); //HttpHost
				mv.visitVarInsn(ALOAD, 3); //HttpResponse
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE, METHOD, SIGNATURE, false);
				mv.visitCode();
			}
		}

		static class RequestFailLoggerMV extends LocalVariablesSorter implements Opcodes {
			private static final String TRACE = TraceElasticSearch.class.getName().replace('.', '/');
			private final static String METHOD = "endFailRequest";
			private static final String SIGNATURE = "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Exception;)V";

			private String className;

			public RequestFailLoggerMV(int access, String desc, MethodVisitor mv, String className) {
				super(ASM9, access, desc, mv);
				this.className = className;
			}

			@Override
			public void visitCode() {
				mv.visitVarInsn(ALOAD, 1); //HttpUriRequest
				mv.visitVarInsn(ALOAD, 2); //Node
				mv.visitVarInsn(ALOAD, 3); //Exception
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE, METHOD, SIGNATURE, false);
				mv.visitCode();
			}
		}
	}
}
