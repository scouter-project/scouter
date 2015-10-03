/*
 *  Copyright 2015 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.asm.concurrent.FutureTaskCallMV;
import scouter.agent.asm.concurrent.FutureTaskInitMV;
import scouter.agent.asm.util.AsmUtil;
import scouter.agent.trace.TraceContext;
import scouter.agent.trace.TraceFutureTask;
import scouter.org.objectweb.asm.ClassVisitor;
import scouter.org.objectweb.asm.MethodVisitor;
import scouter.org.objectweb.asm.Opcodes;
import scouter.org.objectweb.asm.Type;
import scouter.util.StrMatch;

public class FutureTaskASM implements IASM, Opcodes {
	public final List<StrMatch> target = toSet(Configure.getInstance().hook_future_task);

	
	public FutureTaskASM() {
		target.add(new StrMatch("shardframe/parallel/Stream"));
	}

	private List<StrMatch> toSet(String hook_future_task) {
		List<StrMatch> set = new ArrayList<StrMatch>();
		StringTokenizer nizer = new StringTokenizer(hook_future_task, ",;");
		while (nizer.hasMoreTokens()) {
			String word = nizer.nextToken();
			if (word.length() > 0) {
				set.add(new StrMatch(word.replace('.', '/')));
			}
		}
		return set;
	}

	public boolean isTarget(String className) {
		return target.contains(className);
	}

	Configure conf = Configure.getInstance();
	
	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
		for(int i=0;i<target.size() ; i++){
			if(target.get(i).include(className)){
				return new FutureCallableCV(cv, className);
			}
		}
		if(conf.hook_future_task_prefix!=null && conf.hook_future_task_prefix.length()>0){
			if(className.startsWith(conf.hook_future_task_prefix)){
				int len = classDesc.interfaces==null?0:classDesc.interfaces.length;
				for(int i = 0 ; i < len;i++){
					if(classDesc.interfaces[i].equals("java/util/concurrent/Callable")){
						return new FutureCallableCV(cv, className);
					}
				}
			}
		}
		return cv;
	}
}

class FutureCallableCV extends ClassVisitor implements Opcodes {

	public FutureCallableCV(ClassVisitor cv,String className) {
		super(ASM4, cv);
	}

	private String owner;

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);
		super.visitField(ACC_PUBLIC, TraceFutureTask.CTX_FIELD, Type.getDescriptor(TraceContext.class), null, null)
				.visitEnd();
		this.owner = name;
		
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

		if (name.equals("<init>")) {
			return new FutureTaskInitMV(access, desc, mv, owner);
		} else if ("call".equals(name) && desc.equals("()Ljava/lang/Object;") && AsmUtil.isPublic(access)) {
			return new FutureTaskCallMV(access, desc, mv, owner);
		}
		return mv;
	}

}