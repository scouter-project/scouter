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

import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.asm.util.AsmUtil;
import scouter.org.objectweb.asm.AnnotationVisitor;
import scouter.org.objectweb.asm.ClassVisitor;
import scouter.org.objectweb.asm.MethodVisitor;
import scouter.org.objectweb.asm.Opcodes;
import scouter.org.objectweb.asm.commons.LocalVariablesSorter;
import scouter.util.StringUtil;

public class SpringReqMapASM implements IASM, Opcodes {

	static String springControllerNames[] = { "Lorg/springframework/stereotype/Controller;",
			"Lorg/springframework/web/bind/annotation/RestController;" };

	static String springRequestMappingAnnotation = "Lorg/springframework/web/bind/annotation/RequestMapping;";

	public boolean isTarget(String className) {
		return false;
	}

	Configure conf = Configure.getInstance();

	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
		if (conf.hook_spring_request_mapping == false)
			return cv;

		if (classDesc.anotation != null) {
			for (int i = 0; i < SpringReqMapASM.springControllerNames.length; i++) {
				if (classDesc.anotation.indexOf(SpringReqMapASM.springControllerNames[i]) > 0) {
					return new SpringReqMapCV(cv, className);
				}
			}
		}

		return cv;
	}
}

// ///////////////////////////////////////////////////////////////////////////
class SpringReqMapCV extends ClassVisitor implements Opcodes {

	public String className;
	public String classRequestMappingUrl;

	public SpringReqMapCV(ClassVisitor cv, String className) {
		super(ASM4, cv);
		this.className = className;
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		AnnotationVisitor av = super.visitAnnotation(desc, visible);
		if (av == null)
			return av;

		if (SpringReqMapASM.springRequestMappingAnnotation.equals(desc)) {
			return new SpringReqMapCVAV(av);
		}
		return av;
	};

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
			super(ASM4, av);
		}

		@Override
		public AnnotationVisitor visitArray(String name) {
			AnnotationVisitor av = super.visitArray(name);

			if (av == null)
				return av;

			if ("value".equals(name)) {
				return new SpringReqMapCVAVAV(av);
			}
			return av;
		}
	}

	class SpringReqMapCVAVAV extends AnnotationVisitor implements Opcodes {

		public SpringReqMapCVAVAV(AnnotationVisitor av) {
			super(ASM4, av);
		}

		@Override
		public void visit(String name, Object value) {
			super.visit(name, value);
			classRequestMappingUrl = (String) value;
		}
	}

	class SpringReqMapMV extends LocalVariablesSorter implements Opcodes {
		private static final String TRACEMAIN = "scouter/agent/trace/TraceMain";
		private final static String START_METHOD = "setServiceName";
		private static final String START_SIGNATURE = "(Ljava/lang/String;)V";

		private String methodRequestMappingUrl;
		private String methodType;
		private boolean isHandler = false;

		public SpringReqMapMV(String className, int access, String methodName, String desc, MethodVisitor mv) {
			super(ASM4, access, desc, mv);
		}

		@Override
		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			AnnotationVisitor av = super.visitAnnotation(desc, visible);
			if (SpringReqMapASM.springRequestMappingAnnotation.equals(desc)) {

				return new SpringReqMapMVAV(av);
			}
			return av;
		}

		@Override
		public void visitCode() {
			if (isHandler) {
				StringBuilder sb = new StringBuilder(60);
				sb.append(StringUtil.trimEmpty(classRequestMappingUrl))
				.append(StringUtil.trimEmpty(methodRequestMappingUrl));
				
				if(!StringUtil.isEmpty(methodType)) {
					sb.append("<").append(methodType).append(">"); 
				}
				
				String serviceUrl = sb.toString();

				Logger.println("[Apply Spring F/W REST URL] " + serviceUrl);

				AsmUtil.PUSH(mv, serviceUrl);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, START_METHOD, START_SIGNATURE, false);
			}
			mv.visitCode();
		}

		class SpringReqMapMVAV extends AnnotationVisitor implements Opcodes {

			public SpringReqMapMVAV(AnnotationVisitor av) {
				super(ASM4, av);
			}

			@Override
			public AnnotationVisitor visitArray(String name) {
				AnnotationVisitor av = super.visitArray(name);

				if (av == null)
					return av;

				if ("value".equals(name) || "method".equals(name)) {
					return new SpringReqMapMVAVAV(av, name);
				}
				return av;
			}
		}

		class SpringReqMapMVAVAV extends AnnotationVisitor implements Opcodes {
			String paramName;
			
			public SpringReqMapMVAVAV(AnnotationVisitor av, String paramName) {
				super(ASM4, av);
				this.paramName = paramName;
			}

			@Override
			public void visit(String name, Object value) {
				super.visit(name, value);
				
				if(!"value".equals(paramName)) {
					return;
				}
				
				if (value instanceof String) {
					String sValue = (String) value;
					methodRequestMappingUrl = sValue;
					isHandler = true;
				}
			}
			
			@Override
			public void visitEnum(String name, String desc, String value) {
				super.visitEnum(name, desc, value);
				
				if(!"method".equals(paramName)) {
					return;
				}
				
				methodType = value;
				isHandler = true;
			};
		}

	}
}