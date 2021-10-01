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
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.asm.util.HookingSet;

import java.util.Map;

import static scouter.agent.AgentCommonConstant.SCOUTER_ADDED_FIELD;

public class AddFieldASM implements IASM, Opcodes {
	public final Map<String, String> target = HookingSet.getClassFieldSet(Configure.getInstance().hook_add_fields);
	public AddFieldASM() {
		target.put("org/springframework/web/reactive/function/client/DefaultClientRequestBuilder$BodyInserterRequest",
				SCOUTER_ADDED_FIELD);
	}
	Configure conf = Configure.getInstance();
	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
		if (Configure.getInstance()._hook_async_enabled == false) {
			return cv;
		}
		String field = target.get(className);
		if (field != null) {
			return new AddFieldCV(cv, className, field);
		}
		return cv;
	}
}
class AddFieldCV extends ClassVisitor implements Opcodes {
	private String field;
	private String className;
	public AddFieldCV(ClassVisitor cv, String className, String field) {
		super(ASM9, cv);
		this.field = field;
		this.className = className;
	}
	boolean exist = false;
	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);
		if (exist == false) {
			super.visitField(ACC_PUBLIC, field, Type.getDescriptor(Object.class), null, null).visitEnd();
		}
	}
	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		if (name.equals(field)) {
			exist = true;
			Logger.println("A901", "fail to add the field " + name + " on " + className);
		}
		return super.visitField(access, name, desc, signature, value);
	}
}
