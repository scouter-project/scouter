/*
 *  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package scouter.xtra.jdbc;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class Generator {

	public static void main(String[] args) throws Exception {

//		 String className = "ScouterConnection";
//		 String superName =null;
//		 String innerName = "java.sql.Connection";

//		 String className = "ScouterStatement";
//		 String superName = null;
//		 String innerName = "java.sql.Statement";

//		 String className = "ScouterPreparedStatement";
//		 String superName = "ScouterStatement";
//		 String innerName = "java.sql.PreparedStatement";

//		 String className = "ScouterCallableStatement";
//		 String superName = "ScouterPreparedStatement";
//		 String innerName = "java.sql.CallableStatement";

		 String className = "ScouterResultSet";
		 String superName = null;
		 String innerName = "java.sql.ResultSet";

		Class target = Class.forName(innerName);

		if (superName == null) {
			System.out.println("public class " + className + " implements " + innerName + " {");
		} else {
			System.out.println("public class " + className + " extends " + superName + " implements " + innerName
					+ " {");
		}
		System.out.println("    " + innerName + " inner;");
		System.out.println("    public " + className + "(" + innerName + " inner){");
		if (superName != null) {
			System.out.println("           super(inner);");
		}
		System.out.println("           this.inner=inner;");
		System.out.println("    }");

		Method[] m = target.getMethods();
		for (int i = 0; i < m.length; i++) {
			boolean isVoid = m[i].getReturnType().toString().endsWith("void");

			System.out.println("    " + getMethodString(m[i]) + "{");
			System.out.println("        " + getInnerString(m[i]));
			System.out.println("    }");
		}
		System.out.println("}");

	}

	public static String getMethodString(Method m) {
		try {
			StringBuilder sb = new StringBuilder();
			int mod = m.getModifiers();
			if (mod != 0) {
				if (Modifier.isAbstract(mod)) {
					mod ^= Modifier.ABSTRACT;
				}
				sb.append("final " + Modifier.toString(mod)).append(" ");
			}
			sb.append(getTypeName(m.getReturnType())).append(' ');
			sb.append(m.getName()).append('(');
			Class<?>[] params = m.getParameterTypes(); // avoid clone
			for (int j = 0; j < params.length; j++) {
				sb.append(getTypeName(params[j]) + " a" + j);
				if (j < (params.length - 1))
					sb.append(',');
			}
			sb.append(')');
			Class<?>[] exceptions = m.getExceptionTypes(); // avoid clone
			if (exceptions.length > 0) {
				sb.append(" throws ");
				for (int k = 0; k < exceptions.length; k++) {
					sb.append(exceptions[k].getName());
					if (k < (exceptions.length - 1))
						sb.append(',');
				}
			}
			return sb.toString();
		} catch (Exception e) {
			return "<" + e + ">";
		}
	}

	public static String getInnerString(Method m) {
		try {
			boolean isVoid = getTypeName(m.getReturnType()).equals("void");
			StringBuilder sb = new StringBuilder();
			if (isVoid == false)
				sb.append("return ");
			sb.append("this.inner.");
			sb.append(m.getName()).append('(');
			Class<?>[] params = m.getParameterTypes(); // avoid clone
			for (int j = 0; j < params.length; j++) {
				sb.append("a" + j);
				if (j < (params.length - 1))
					sb.append(", ");
			}
			sb.append(");");

			return sb.toString();
		} catch (Exception e) {
			return "<" + e + ">";
		}
	}

	static String getTypeName(Class<?> type) {
		if (type.isArray()) {
			try {
				Class<?> cl = type;
				int dimensions = 0;
				while (cl.isArray()) {
					dimensions++;
					cl = cl.getComponentType();
				}
				StringBuffer sb = new StringBuffer();
				sb.append(cl.getName());
				for (int i = 0; i < dimensions; i++) {
					sb.append("[]");
				}
				return sb.toString();
			} catch (Throwable e) { /* FALLTHRU */
			}
		}
		return type.getName();
	}
}