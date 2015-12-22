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
 *  
 */
package scouter.util;
/**
 *  @author Paul S.J. Kim(sjkim@whatap.io)
 */
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class BytesClassLoader extends ClassLoader {
	private BytesMap bytes;

	public BytesClassLoader(BytesMap bytes) {
		this(bytes, null);
	}

	public BytesClassLoader(BytesMap bytes, ClassLoader parent) {
		super(parent);
		this.bytes = bytes;
	}

	public BytesClassLoader(byte[] jarBytes) {
		this(jarBytes, null);
	}

	public BytesClassLoader(byte[] jarBytes, ClassLoader parent) {
		super(parent);
		this.bytes = load(jarBytes);
	}

	protected BytesMap load(byte[] bytes) {
		BytesMap bmap = new BytesMap();

		JarInputStream jar = null;
		try {
			jar = new JarInputStream(new ByteArrayInputStream(bytes));
			for (JarEntry ent = jar.getNextJarEntry(); ent != null; ent = jar.getNextJarEntry()) {
				byte[] buff = FileUtil.readAll(jar);
				if (buff != null) {
					bmap.put(ent.getName(), buff);
				}
			}
		} catch (Exception e) {
		} finally {
			FileUtil.close(jar);
		}
		return bmap;
	}

	protected Class<?> findClass(String classname) throws ClassNotFoundException {
		String name = classname.replace('.', '/') + ".class";
		byte[] b = bytes.getBytes(name);
		if (b == null)
			throw new ClassNotFoundException("not found class " + classname);
		return defineClass(classname, b, 0, b.length, null);
	}

	protected URL findResource(String name) {
		return bytes.getResource(name);
	}
}
