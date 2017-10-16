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

package scouter.agent.util;

import java.io.File;
import java.net.URISyntaxException;
import java.security.CodeSource;

import scouter.agent.JavaAgent;

public class JarUtil {

	public static File getThisJarFile() {
		String path = "";
		ClassLoader cl = JavaAgent.class.getClassLoader();
		if (cl == null) {
			path = ""
					+ ClassLoader.getSystemClassLoader().getResource(JavaAgent.class.getName().replace('.', '/') + ".class");
		} else {
			path = "" + cl.getResource(JavaAgent.class.getName().replace('.', '/') + ".class");
		}
		try {
			path = path.substring("jar:file:/".length(), path.indexOf("!"));
			if (path.indexOf(':') > 0)
				return new File(path);
			else
				return new File("/" + path);
		} catch (Throwable th) {
			return null;
		}
	}

    public static File getThisJarFile(Class clazz) {
        try {
            CodeSource codeSource = clazz.getProtectionDomain().getCodeSource();
            File agentJarFile = new File(codeSource.getLocation().toURI());
            if (agentJarFile.getName().endsWith(".jar")) {
                return agentJarFile;
            }
        } catch (Throwable e) {
        }
        return null;
    }
}