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

package scouter.util;

import java.text.DecimalFormat;

import scouter.lang.value.BooleanValue;

public class CastUtil {
	public static int cint(Object value) {
		if (value == null) {
			return 0;
		} else if (value instanceof Number) {
			return ((Number) value).intValue();
		} else {
			try {
				return Integer.parseInt(toString(value));
			} catch (Exception e) {
				return 0;
			}
		}
	}

	public static Integer cInteger(Object value) {
		return cint(value);
	}

	public static long clong(Object value) {
		if (value == null) {
			return 0;
		} else if (value instanceof Number) {
			return ((Number) value).longValue();
		} else {
			try {
				return Long.parseLong(toString(value));
			} catch (Exception e) {
				return 0L;
			}
		}

	}

	public static Long cLong(Object value) {
		return clong(value);
	}

	public static float cfloat(Object value) {
		if (value == null) {
			return 0f;
		} else if (value instanceof Number) {
			return ((Number) value).floatValue();
		} else {
			try {
				return Float.parseFloat(toString(value));
			} catch (Exception e) {
				return 0f;
			}
		}

	}

	public static Float cFloat(Object value) {
		return cfloat(value);
	}

	public static double cdouble(Object value) {
		if (value == null) {
			return 0;
		} else if (value instanceof Number) {
			return ((Number) value).doubleValue();
		} else {
			try {
				return Double.parseDouble(toString(value));
			} catch (Exception e) {
				return 0;
			}
		}
	}

	public static Double cDouble(Object value) {
		return cdouble(value);
	}

	private static String toString(Object value) {
		if (value instanceof String)
			return (String) value;
		return value.toString();
	}

	public static String cString(Object value) {
		if (value == null) {
			return "";
		} else if (value instanceof Number) {
			if (value instanceof Double || value instanceof Float) {
				return new DecimalFormat("#0.0#######").format(value);
			} else {
				return new DecimalFormat("#0").format(value);
			}
		} else {
			return toString(value);
		}
	}

	public static boolean cboolean(Object value) {
		if (value == null) {
			return false;
		} else if (value instanceof Boolean) {
			return ((Boolean) value).booleanValue();
		} else if (value instanceof BooleanValue) {
			return ((BooleanValue) value).value;
		} else {
			return "true".equalsIgnoreCase(toString(value));
		}
	}

	public static Boolean cBoolean(Object value) {
		return cboolean(value);
	}

	public static void main(String[] args) {
		System.out.println((long) CastUtil.cdouble("9876544321.0"));
	}

}