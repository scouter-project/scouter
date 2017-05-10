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


public class StrMatch {

	public enum COMP {
		EQU, STR, STR_MID, STR_END, MID, MID_MID, MID_END, END, ANY
	}

	protected COMP comp = COMP.EQU;
	protected String start, end, mid, mid2;
	protected String pattern;

	public boolean equals(Object obj) {
		if (obj instanceof StrMatch) {
			return this.pattern.equals(((StrMatch) obj).pattern);
		}
		return false;
	}

	public int hashCode() {
		return pattern.hashCode();
	}

	public StrMatch(String pattern) {
		this('*', pattern);
	}

	private final String any1, any2;

	public StrMatch(char CHAR, String pattern) {
		this.pattern = pattern;
		this.any1 = new String(new char[] { CHAR });
		this.any2 = new String(new char[] { CHAR, CHAR });
		if (any1.equals(pattern) || any2.equals(pattern)) {
			comp = COMP.ANY;
			return;
		}

		int length = pattern.length();
		if (length < 2) {
			comp = COMP.EQU;
			mid = pattern;
			return;
		}

		boolean anyStart = pattern.charAt(0) == CHAR;
		boolean anyEnd = pattern.charAt(length - 1) == CHAR;
		int x = pattern.indexOf(CHAR, 1);
		boolean anyMid = x > 0 && x < (length - 1);

		if (anyMid) {
			if (anyStart && anyEnd) {
				comp = COMP.MID_MID;
				mid = pattern.substring(1, x);
				mid2 = pattern.substring(x + 1, length - 1);
			} else if (anyStart) {
				comp = COMP.MID_END;
				mid = pattern.substring(1, x);
				end = pattern.substring(x + 1);
			} else if (anyEnd) {
				comp = COMP.STR_MID;
				start = pattern.substring(0, x);
				mid = pattern.substring(x + 1, length - 1);
			} else {
				comp = COMP.STR_END;
				start = pattern.substring(0, x);
				end = pattern.substring(x + 1);
			}
		} else {
			if (anyStart && anyEnd) {
				comp = COMP.MID;
				mid = pattern.substring(1, length - 1);
			} else if (anyStart) {
				comp = COMP.END;
				end = pattern.substring(1);
			} else if (anyEnd) {
				comp = COMP.STR;
				start = pattern.substring(0, length - 1);
			} else {
				comp = COMP.EQU;
				mid = pattern;
			}
		}
	}

	public boolean include(String target) {
		if (target == null || target.length() == 0)
			return false;
		switch (comp) {
		case ANY:
			return true;
		case EQU:
			return target.equals(mid);
		case STR:
			return target.startsWith(start);
		case STR_MID:
			return target.startsWith(start) && target.indexOf(mid) >= 0;
		case STR_END:
			return target.startsWith(start) && target.endsWith(end);
		case MID:
			return target.indexOf(mid) >= 0;
		case MID_MID:
			int x = target.indexOf(mid);
			if (x < 0)
				return false;
			return target.indexOf(mid2, x + mid.length()) >= 0;
		case MID_END:
			return target.indexOf(mid) >= 0 && target.endsWith(end);
		case END:
			return target.endsWith(end);
		default:
			return false;
		}
	}

	public String toString() {
		return pattern;
	}

	public String getPattern() {
		return pattern;
	}

	public COMP getComp() {
		return comp;
	}

	public static void main(String[] args) {
		StrMatch sc = new StrMatch("**");
		System.out.println(sc.pattern);
	}
}
