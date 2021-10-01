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

import java.io.File;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import scouter.util.FileUtil;
import scouter.util.StringKeyLinkedMap;
import scouter.util.StringUtil;

public class DirectPatch {

	static StringKeyLinkedMap<byte[]> classPatchMap = new StringKeyLinkedMap<byte[]>();

	static {
		try {
			String patch = Configure.getInstance()._hook_direct_patch_classes;
			String[] files = StringUtil.tokenizer(patch, ",;");
			for (int i = 0; files!=null && i < files.length; i++) {
				byte[] bytes = FileUtil.readAll(new File(files[i]));
				if (bytes != null) {
					String classname = getClassName(bytes);
					if (classname != null) {
						classPatchMap.put(classname, bytes);
					}
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	private static String getClassName(byte[] bytes) {
		try {
			final ClassDesc classDesc = new ClassDesc();
			ClassReader cr = new ClassReader(bytes);
			cr.accept(new ClassVisitor(Opcodes.ASM9) {
				public void visit(int version, int access, String name, String signature, String superName,
						String[] interfaces) {
					classDesc.set(version, access, name, signature, superName, interfaces);
				}
			}, 0);
			return classDesc.name.replace('.', '/');
		} catch (Throwable t) {
			return null;
		}
	}

	public static byte[] patch(String name, byte[] org) {
		byte[] patchClass = classPatchMap.get(name);
		return patchClass != null ? patchClass : org;
	}
}
