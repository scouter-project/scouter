/***
 * Extracted and modified from ASM 4 test suite. Original copyright notice preserved below
 * 
 * ASM tests
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 * Original Source from : org.eclipse.persistence.internal.jpa.weaving.ComputeClassWriter
 *  
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package scouter.agent.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import scouter.agent.AgentTransformer;

import java.io.IOException;
import java.io.InputStream;
/**
 * A ClassWriter that computes the common super class of two classes without
 * actually loading them with a ClassLoader.
 *
 * @author Eric Bruneton
 */
public class ScouterClassWriter extends ClassWriter {

	public ScouterClassWriter(final int flags) {
		super(flags);
	}

	protected String getCommonSuperClass(final String type1, final String type2) {
		try {
			ClassReader info1 = typeInfo(type1);
			ClassReader info2 = typeInfo(type2);

			if (info1 == null || info2 == null)
				return "java/lang/Object";

			if ((info1.getAccess() & Opcodes.ACC_INTERFACE) != 0) {
				if (typeImplements(type2, info2, type1)) {
					return type1;
				}
				if ((info2.getAccess() & Opcodes.ACC_INTERFACE) != 0) {
					if (typeImplements(type1, info1, type2)) {
						return type2;
					}
				}
				return "java/lang/Object";
			}
			if ((info2.getAccess() & Opcodes.ACC_INTERFACE) != 0) {
				if (typeImplements(type1, info1, type2)) {
					return type2;
				} else {
					return "java/lang/Object";
				}
			}
			StringBuffer b1 = typeAncestors(type1, info1);
			StringBuffer b2 = typeAncestors(type2, info2);
			String result = "java/lang/Object";
			int end1 = b1.length();
			int end2 = b2.length();
			while (true) {
				int start1 = b1.lastIndexOf(";", end1 - 1);
				int start2 = b2.lastIndexOf(";", end2 - 1);
				if (start1 != -1 && start2 != -1
						&& end1 - start1 == end2 - start2) {
					String p1 = b1.substring(start1 + 1, end1);
					String p2 = b2.substring(start2 + 1, end2);
					if (p1.equals(p2)) {
						result = p1;
						end1 = start1;
						end2 = start2;
					} else {
						return result;
					}
				} else {
					return result;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "java/lang/Object";
	}
	/**
     * Returns the internal names of the ancestor classes of the given type.
     *
     * @param type the internal name of a class or interface.
     * @param info the ClassReader corresponding to 'type'.
     * @return a StringBuilder containing the ancestor classes of 'type',
     *         separated by ';'. The returned string has the following format:
     *         ";type1;type2 ... ;typeN", where type1 is 'type', and typeN is a
     *         direct subclass of Object. If 'type' is Object, the returned
     *         string is empty.
     * @throws IOException if the bytecode of 'type' or of some of its ancestor
     *         class cannot be loaded.
     */
	private StringBuffer typeAncestors(String type, ClassReader info)
			throws IOException {
		StringBuffer b = new StringBuffer();
		while (!"java/lang/Object".equals(type)) {
			b.append(';').append(type);
			type = info.getSuperName();
			info = typeInfo(type);
		}
		return b;
	}

	 /**
     * Returns true if the given type implements the given interface.
     *
     * @param type the internal name of a class or interface.
     * @param info the ClassReader corresponding to 'type'.
     * @param itf the internal name of a interface.
     * @return true if 'type' implements directly or indirectly 'itf'
     * @throws IOException if the bytecode of 'type' or of some of its ancestor
     *         class cannot be loaded.
     */
	private boolean typeImplements(String type, ClassReader info, String itf)
			throws IOException {
		while (!"java/lang/Object".equals(type)) {
			String[] itfs = info.getInterfaces();
			for (int i = 0; i < itfs.length; ++i) {
				if (itfs[i].equals(itf)) {
					return true;
				}
			}
			for (int i = 0; i < itfs.length; ++i) {
				if (typeImplements(itfs[i], typeInfo(itfs[i]), itf)) {
					return true;
				}
			}
			type = info.getSuperName();
			info = typeInfo(type);
		}
		return false;
	}
	 /**
     * Returns a ClassReader corresponding to the given class or interface.
     * 
     * Scouter: 원래 소스에서 참조 클래스 로더를 context 에서 가져 오도록 수정함..
     * 
     * @param type the internal name of a class or interface.
     * @return the ClassReader corresponding to 'type'.
     * @throws IOException if the bytecode of 'type' cannot be loaded.
     */
	private ClassReader typeInfo(final String type) throws IOException {
		ClassLoader loader = AgentTransformer.hookingCtx.get();
		InputStream is = null;
		try {
			if (loader == null) {
				is = ClassLoader.getSystemResourceAsStream(type + ".class");
			} else {
				is = loader.getResourceAsStream(type + ".class");
				if(is == null){
					is = ClassLoader.getSystemResourceAsStream(type + ".class");
				}
			}
			if (is == null) {
				return null;
			} else {
				return new ClassReader(is);
			}
		} finally {
			if (is != null)
				is.close();
		}
	}

}
