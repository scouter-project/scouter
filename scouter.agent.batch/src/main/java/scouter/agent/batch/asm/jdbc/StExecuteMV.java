/*
 *  Copyright 2016 Scouter Project.
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

package scouter.agent.batch.asm.jdbc;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;
import scouter.agent.batch.trace.TraceContextManager;
import scouter.agent.batch.trace.TraceSQL;
import scouter.lang.step.SqlXType;

import java.util.HashSet;
import java.util.Set;

public class StExecuteMV extends LocalVariablesSorter implements Opcodes {
	private static Set<String> target = new HashSet<String>();
	static {
		target.add("execute");
		target.add("executeQuery");
		target.add("executeUpdate");
		target.add("executeBatch");
	}

	private final Type returnType;

	public static boolean isTarget(String name) {
		return target.contains(name);
	}

	private final static String TRACE_CONTEXT_MANAGER = TraceContextManager.class.getName().replace('.', '/');
	private final static String START_METHOD = "startTraceSQL";
	private final static String START_SIGNATURE = "(Ljava/lang/String;)Lscouter/agent/batch/trace/TraceSQL;";
	
	private final static String TRACESQL = TraceSQL.class.getName().replace('.', '/');
	private final static String END_METHOD = "end";
	private static final String END_SIGNATURE = "()V";
	private final static String ADD_METHOD = "addRow";
	private static final String ADD_SIGNATURE = "(I)V";
	private final static String ADDS_METHOD = "addRows";
	private static final String ADDS_SIGNATURE = "([I)V";

	public StExecuteMV(int access, String desc, MethodVisitor mv, String owner, String name) {
		super(ASM5, access, desc, mv);
		this.returnType = Type.getReturnType(desc);
        this.owner = owner;
	}

	public static byte methodType(String name) {
		if("execute".equals(name)){
			return SqlXType.METHOD_EXECUTE;
		}
		if("executeQuery".equals(name)){
			return SqlXType.METHOD_QUERY;
		}
		if("executeUpdate".equals(name)){
			return SqlXType.METHOD_UPDATE;
		}
		return SqlXType.METHOD_KNOWN;
	}

	private Label startFinally = new Label();
	private String owner;
    
	@Override
	public void visitCode() {
		mv.visitVarInsn(ALOAD, 0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitVarInsn(ALOAD, 1);

		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE_CONTEXT_MANAGER, START_METHOD, START_SIGNATURE, false);
		mv.visitFieldInsn(PUTFIELD, owner, TraceSQL.CURRENT_TRACESQL_FIELD, "Lscouter/agent/batch/trace/TraceSQL;");
		mv.visitLabel(startFinally);
		mv.visitCode();
	}

	@Override
	public void visitInsn(int opcode) {
		if ((opcode >= IRETURN && opcode <= RETURN)) {		
			int lvPosReturn;
			switch (returnType.getSort()) {
            case Type.ARRAY:
                if(returnType.getElementType().getSort() == Type.INT) {
                    lvPosReturn = newLocal(returnType);
                    mv.visitVarInsn(Opcodes.ASTORE, lvPosReturn);
                    mv.visitVarInsn(Opcodes.ALOAD, lvPosReturn);
        			mv.visitVarInsn(ALOAD, 0);
        		    mv.visitFieldInsn(GETFIELD, owner, TraceSQL.CURRENT_TRACESQL_FIELD, "Lscouter/agent/batch/trace/TraceSQL;");
                    mv.visitVarInsn(Opcodes.ALOAD, lvPosReturn);
        			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, TRACESQL, ADDS_METHOD, ADDS_SIGNATURE,false);
                }
                break;
            case Type.INT:
            	lvPosReturn = newLocal(returnType);
                mv.visitVarInsn(Opcodes.ISTORE, lvPosReturn);
                mv.visitVarInsn(Opcodes.ILOAD, lvPosReturn);
    			mv.visitVarInsn(ALOAD, 0);
    		    mv.visitFieldInsn(GETFIELD, owner, TraceSQL.CURRENT_TRACESQL_FIELD, "Lscouter/agent/batch/trace/TraceSQL;");
                mv.visitVarInsn(Opcodes.ILOAD, lvPosReturn);
    			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, TRACESQL, ADD_METHOD, ADD_SIGNATURE,false);
                break;
			}

			mv.visitVarInsn(ALOAD, 0);
		    mv.visitFieldInsn(GETFIELD, owner, TraceSQL.CURRENT_TRACESQL_FIELD, "Lscouter/agent/batch/trace/TraceSQL;");
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, TRACESQL, END_METHOD, END_SIGNATURE,false);
		}
		mv.visitInsn(opcode);
	}

	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		Label endFinally = new Label();
		mv.visitTryCatchBlock(startFinally, endFinally, endFinally, null);
		mv.visitLabel(endFinally);
		
		mv.visitVarInsn(ALOAD, 0);
		mv.visitVarInsn(ALOAD, 0);
	    mv.visitFieldInsn(GETFIELD, owner, TraceSQL.CURRENT_TRACESQL_FIELD, "Lscouter/agent/batch/trace/TraceSQL;");
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, TRACESQL, END_METHOD, END_SIGNATURE,false);
		
		int errIdx = newLocal(Type.getType(Throwable.class));
		mv.visitVarInsn(Opcodes.ASTORE, errIdx);
		mv.visitInsn(ATHROW);
		
		mv.visitMaxs(maxStack + 8, maxLocals + 2);
	}
}