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

/**
 * bugfix :  parse error for '*' by 2016.08.12 Paul S.J.Kim
 */
public class EscapeLiteralSQL {

	static enum STAT {
		NORMAL, COMMENT, ALPABET, NUMBER, QUTATION, COLON
	};

	private String substitute = "@";
	private String substitute_num = "@";
	private boolean substitute_str_mode = true;

	private char[] chars;
	private int pos;
	private int length;
	
	private int count;
	private int comment_su;
	
	final StringBuffer parsedSql;
	final StringBuffer param;
	private STAT status;

	public EscapeLiteralSQL(String sql) {
		this.chars = sql.toCharArray();
		this.length = this.chars.length;
		this.parsedSql = new StringBuffer(this.length + 10);
		this.param = new StringBuffer();
	}

	public EscapeLiteralSQL setSubstitute(String chr) {
		this.substitute = chr;
		if (this.substitute_str_mode) {
			this.substitute_num = "'" + chr + "'";
		} else {
			this.substitute_num = this.substitute;
		}
		return this;
	}

	public EscapeLiteralSQL setSubstituteStringMode(boolean b) {
		if (this.substitute_str_mode == b)
			return this;
		this.substitute_str_mode = b;
		if (this.substitute_str_mode) {
			this.substitute_num = "'" + this.substitute + "'";
		} else {
			this.substitute_num = this.substitute;
		}
		return this;
	}

	public EscapeLiteralSQL process() {
		status = STAT.NORMAL;
		for (pos = 0; pos < chars.length; pos++) {
			switch (chars[pos]) {
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
				_number();
				break;
			case ':':
				_colon();
				break;
			case '.':
				_dot();
				break;
			case '-':
				_minus();
				break;
			case '/':
				_slash();
				break;
			case '*':
				_astar();
				break;
			case '\'':
				_qutation();
				break;
			default:
				_others();
			}
		}
		return this;
	}

	private void _others() {
		// System.out.println("other=>'"+chars[pos]+"'  " +status);
		switch (status) {
		case COMMENT:
			parsedSql.append(chars[pos]);
			break;
		case ALPABET:
			parsedSql.append(chars[pos]);
			if (isProgLetter(chars[pos]) == false) {
				status = STAT.NORMAL;
			}
			break;
		case NUMBER:
			parsedSql.append(chars[pos]);
			status = STAT.NORMAL;
			break;
		case QUTATION:
			param.append(chars[pos]);
			break;
		default:
			if (isProgLetter(chars[pos])) {
				status = STAT.ALPABET;
			} else {
				status = STAT.NORMAL;
			}
			parsedSql.append(chars[pos]);
			break;
		}
	}

	private boolean isProgLetter(char c) {
		return Character.isLetter(c) || c == '_';
	}

	private void _colon() {
		switch (status) {
		case COMMENT:
			parsedSql.append(chars[pos]);
			break;
		case QUTATION:
			param.append(chars[pos]);
			break;
		default:
			parsedSql.append(chars[pos]);
			status = STAT.COLON;
			break;
		}
	}
	private void _qutation() {
		switch (status) {
		case NORMAL:
			if (param.length() > 0) {
				param.append(",");
			}
			param.append(chars[pos]);
			status = STAT.QUTATION;
			break;
		case COMMENT:
			parsedSql.append(chars[pos]);
			break;
		case ALPABET:
			parsedSql.append(chars[pos]);
			status = STAT.QUTATION;
			break;
		case NUMBER:
			parsedSql.append(chars[pos]);
			status = STAT.QUTATION;
			break;
		case QUTATION:
			param.append("'");
			parsedSql.append('\'').append(substitute).append("{").append(++count).append("}").append('\'');
			status = STAT.NORMAL;
			break;
		}
	}

	private void _astar() {
		switch (status) {
		case COMMENT:
			parsedSql.append(chars[pos]);
			if (getNext(pos) == '/') {
				parsedSql.append('/');
				pos++;
				if(--comment_su == 0){
					status = STAT.NORMAL;
				}
			}
			break;
		case QUTATION:
			param.append(chars[pos]);
			break;
		default:
			parsedSql.append(chars[pos]);
			status = STAT.NORMAL;
		}
	}

	private void _slash() {
		switch (status) {
		case COMMENT:
			parsedSql.append(chars[pos]);
			break;
		case QUTATION:
			param.append(chars[pos]);
			break;
		default:
			if (getNext(pos) == '*') {
				pos++;
				comment_su++;
				parsedSql.append("/*");
				status = STAT.COMMENT;
			} else {
				parsedSql.append(chars[pos]);
				status = STAT.NORMAL;
			}
		}
	}

	private void _minus() {
		switch (status) {
		case COMMENT:
			parsedSql.append(chars[pos]);
			break;
		case QUTATION:
			param.append(chars[pos]);
			break;
		default:
			if (getNext(pos) == '-') {
				parsedSql.append(chars[pos]);
				while (chars[pos] != '\n') {
					pos++;
					if (pos < length) {
						parsedSql.append(chars[pos]);
					} else {
						break;
					}
				}
			}else{
				parsedSql.append(chars[pos]);
			}
			status = STAT.NORMAL;
		}
	}

	private void _dot() {
		switch (status) {
		case NORMAL:
			parsedSql.append(chars[pos]);
			break;
		case COMMENT:
			parsedSql.append(chars[pos]);
			break;
		case ALPABET:
			parsedSql.append(chars[pos]);
			status = STAT.NORMAL;
			break;
		case NUMBER:
			param.append(chars[pos]);
			break;
		case QUTATION:
			param.append(chars[pos]);
			break;
		}
	}

	private void _number() {
		switch (status) {
		case NORMAL:
			if(param.length() > 0){
				param.append(",");
			}
			param.append(chars[pos]);
			parsedSql.append(substitute_num).append("{").append(++count).append("}");
			status = STAT.NUMBER;
			break;
		case COMMENT:
		case COLON:
		case ALPABET:
			parsedSql.append(chars[pos]);
			break;
		case NUMBER:
		case QUTATION:
			param.append(chars[pos]);
			break;
		}
	}

	private char getNext(int x) {
		return x < length ? chars[x + 1] : 0;
	}

	public static void main(String[] args) throws Exception {

		//String s = new String(FileUtil.readAll(new File("d:/tmp/sample-query2.sql")), "EUC_KR");
	    String s = "select  aa_1 ,( a - b)  as b from tab";//new
		// String(FileUtil.readAll(new
		// File("d:/tmp/sample-query2.sql")),"EUC_KR");
		long time = System.currentTimeMillis();
		EscapeLiteralSQL ec = new EscapeLiteralSQL(s).process();
		long etime = System.currentTimeMillis();
		//FileUtil.save("d:/tmp/sample-query2.out", ec.parsedSql.toString().getBytes());
		System.out.println("SQL Orgin: " + s);
		System.out.println("SQL Parsed: " + ec.getParsedSql());
		System.out.println("PARAM: " + ec.param);

		s = "select 1 / 2 from dual";
		ec = new EscapeLiteralSQL(s).process();
		System.out.println("SQL Orgin: " + s);
		System.out.println("SQL Parsed: " + ec.getParsedSql());
		System.out.println("PARAM: " + ec.param);

		s = "select 1/2 from dual";
		ec = new EscapeLiteralSQL(s).process();
		System.out.println("SQL Orgin: " + s);
		System.out.println("SQL Parsed: " + ec.getParsedSql());
		System.out.println("PARAM: " + ec.param);

		s = "select 1/2 /* 3/4 3 / 4*/ from dual";
		ec = new EscapeLiteralSQL(s).process();
		System.out.println("SQL Orgin: " + s);
		System.out.println("SQL Parsed: " + ec.getParsedSql());
		System.out.println("PARAM: " + ec.param);
	}

	public String getParsedSql() {
		return this.parsedSql.toString();
	}

	public String getParameter() {
		return this.param.toString();
	}
}
