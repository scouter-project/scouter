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

package scouter.agent.asm.modification;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.asm.IASM;

public class MongoModifyASM implements IASM, Opcodes {

	private Configure conf = Configure.getInstance();

	public MongoModifyASM() {
	}

	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
		if (!conf.hook_mongodb_enabled) {
			return cv;
		}

		if ("com/mongodb/connection/InternalConnection".equals(className)) {
			return new InternalConnectionCV(cv, className);
		}
		return cv;
	}

	static class InternalConnectionCV extends ClassVisitor implements Opcodes {
		public String className;

		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			int newAccess = (access | ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED) - ACC_PRIVATE - ACC_PROTECTED;
			super.visit(version, newAccess, name, signature, superName, interfaces);
		}

		@Override
		public MethodVisitor visitMethod(int access, String methodName, String desc, String signature, String[] exceptions) {
			int newAccess = (access | ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED) - ACC_PRIVATE - ACC_PROTECTED;
			return super.visitMethod(newAccess, methodName, desc, signature, exceptions);
		}

		public InternalConnectionCV(ClassVisitor cv, String className) {
			super(ASM9, cv);
			this.className = className;
		}
	}
}
