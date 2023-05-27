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
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.asm.IASM;

public class ReactorModifyASM implements IASM, Opcodes {

	private Configure conf = Configure.getInstance();

	public ReactorModifyASM() {
	}

	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
		if (conf._hook_reactive_enabled == false) {
			return cv;
		}

		if ("reactor/core/publisher/OptimizableOperator".equals(className)) {
			return new OptimizableOperatorCV(cv, className);
		}
		if ("reactor/core/publisher/MonoOnAssembly".equals(className)) {
			return new MonoOnAssemblyCV(cv, className);
		}
		if ("reactor/core/publisher/FluxOnAssembly".equals(className)) {
			return new FluxOnAssemblyCV(cv, className);
		}
		if ("reactor/core/publisher/FluxOnAssembly$AssemblySnapshot".equals(className)) {
			return new AssemblySnapshotCV(cv, className);
		}
		return cv;
	}

	static class OptimizableOperatorCV extends ClassVisitor implements Opcodes {
		public String className;

		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			int newAccess = (access | ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED) - ACC_PRIVATE - ACC_PROTECTED;
			super.visit(version, newAccess, name, signature, superName, interfaces);
		}

		public OptimizableOperatorCV(ClassVisitor cv, String className) {
			super(ASM9, cv);
			this.className = className;
		}
	}

	static class MonoOnAssemblyCV extends ClassVisitor implements Opcodes {
		public String className;

		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			int newAccess = (access | ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED) - ACC_PRIVATE - ACC_PROTECTED;
			super.visit(version, newAccess, name, signature, superName, interfaces);
		}

		@Override
		public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
			int newAccess = (access | ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED) - ACC_PRIVATE - ACC_PROTECTED;
			return super.visitField(newAccess, name, descriptor, signature, value);
		}

		public MonoOnAssemblyCV(ClassVisitor cv, String className) {
			super(ASM9, cv);
			this.className = className;
		}
	}

	static class FluxOnAssemblyCV extends ClassVisitor implements Opcodes {
		public String className;

		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			int newAccess = (access | ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED) - ACC_PRIVATE - ACC_PROTECTED;
			super.visit(version, newAccess, name, signature, superName, interfaces);
		}

		@Override
		public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
			int newAccess = (access | ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED) - ACC_PRIVATE - ACC_PROTECTED;
			return super.visitField(newAccess, name, descriptor, signature, value);
		}

		public FluxOnAssemblyCV(ClassVisitor cv, String className) {
			super(ASM9, cv);
			this.className = className;
		}
	}

	static class AssemblySnapshotCV extends ClassVisitor implements Opcodes {
		public String className;

		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			int newAccess = (access | ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED) - ACC_PRIVATE - ACC_PROTECTED;
			super.visit(version, newAccess, name, signature, superName, interfaces);
		}

		@Override
		public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
			int newAccess = (access | ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED) - ACC_PRIVATE - ACC_PROTECTED;
			return super.visitField(newAccess, name, descriptor, signature, value);
		}

		public AssemblySnapshotCV(ClassVisitor cv, String className) {
			super(ASM9, cv);
			this.className = className;
		}
	}
}
