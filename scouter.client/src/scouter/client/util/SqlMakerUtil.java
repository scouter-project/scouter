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
package scouter.client.util;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import scouter.util.StringUtil;

public class SqlMakerUtil {
	public static String SQLDIVIDE = "\r\n\r\n[Bind Variables]\r\n";
	static Pattern pattern = Pattern.compile("\\@\\{\\d+\\}");
	
	static public String bindSQL(String sqlText, String params){
		if(params == null || "".equals(params)){
			return sqlText;
		}
		
		ArrayList<String> binds = divideParams(params);
		if(binds == null || binds.size() == 0){
			return sqlText;
		}
		
		int bindLength =  binds.size();
		
		if(sqlText == null || sqlText.length() == 0){
			return "No SQL Text";
		}
		
		String newSqlText = convertBindVariable(sqlText);
		
		StringBuilder sqlBuilder = new StringBuilder();
		
		int index = 0;
		int pos = 0;
		Matcher m = pattern.matcher(newSqlText);
		while (m.find()) {
			sqlBuilder.append(newSqlText.substring(pos, m.start())).append(StringUtil.stripSideChar(binds.get(index), '\''));
			pos = m.end();
			index++;
		}
		sqlBuilder.append(newSqlText.substring(pos));
		
//		int sqlLength = newSqlText.length();
//		int bindLength =  binds.size();
//		String bind;
//		int search;
//		boolean isChar;
//
//		StringBuilder sb = new StringBuilder(100);
//		while(pos < sqlLength){
//			search = newSqlText.indexOf('@', pos);
//			
//			if(search < 0 || index >= bindLength){
//				sb.append(newSqlText.substring(pos));
//				break;
//			}
//			
//			bind = binds.get(index);
//			if(bind.charAt(0) == '\''){
//				isChar = true;
//			}else{
//				isChar = false;
//			}
//			
//			if(isChar){
//				if(search == 0 || (search + 1) == sqlLength){
//					return errorMessage(sb, newSqlText, bind, "SQL Character Position Check", pos, search, isChar);
//				}
//				
//				if(newSqlText.charAt(search - 1) != '\'' || newSqlText.charAt(search + 1) != '\''){
//					return errorMessage(sb, newSqlText, bind, "SQL Character Quata Check", pos, search, isChar);
//				}
//				
//				sb.append(newSqlText.subSequence(pos, search - 1));
//				sb.append(bind);
//				pos = search + 2;
//			}else{
//				if(search > 0){
//					if(" \t=<>,+-*/|^&(".indexOf(newSqlText.charAt(search-1))<0){
//						return errorMessage(sb, newSqlText, bind, "Number Check", pos, search, isChar);						
//					}
//				}
//				sb.append(newSqlText.subSequence(pos, search));
//				sb.append(bind);
//				pos = search + 1;
//			}
//			
//			index++;
//		}
		
		if(index < bindLength){
			sqlBuilder.append(SQLDIVIDE);
			
			int inx = 1;
			for(int i = index; i < bindLength; i++){
				sqlBuilder.append(':').append(inx).append(" - ").append(binds.get(i)).append("\r\n");
				inx++;
			}
		}
		
		return sqlBuilder.toString();
	}
	
	static private String convertBindVariable(String sqlText){ // convert to ':1'  from '?'
		StringBuilder sb = new StringBuilder(sqlText.length() + 40);
		
		int sqlLength = sqlText.length();
		int search;
		int index = 1;
		int pos = 0;
		
		while(pos < sqlLength){
			search = sqlText.indexOf('?', pos);
			
			if(search < 0 ){
				sb.append(sqlText.substring(pos));
				break;
			}
			sb.append(sqlText.substring(pos, search)).append(':').append(index);
			index++;
			pos = search + 1;
		}		
		return sb.toString();
	}
	
	static private String errorMessage(StringBuilder sb, String sqlText, String param, String error, int pos, int search, boolean isChar){
		sb.append(sqlText.substring(pos,search));
		sb.append('[').append(error).append('-').append(param).append(']').append(sqlText.substring(search));
		return "Fail to convert =>\r\n" + sb.toString();
	}
	
	static private ArrayList<String> divideParams(String params){
		if(params == null || "".equals(params.trim())){
			return null;
		}
		ArrayList<String> binds = new ArrayList<String>();
		
		char ch;
		int start = 0;
		boolean isQ = false;
		boolean isDQ = false;
		int size = params.length();
		for(int i = 0; i< size; i++){
			ch = params.charAt(i);
			if(ch == ',' && !isQ && !isDQ){
				binds.add(params.substring(start, i));				
				start = i + 1;
				continue;
			}
			if(ch != '\'' && ch != '\"' ){
				continue;
			}
			if(ch == '\''){
				if(isQ){
					isQ = false;
				}else{
					isQ = true;
				}
			}else if(ch == '\"'){
				if(isDQ){
					isDQ = false;
				}else{
					isDQ = true;
				}				
			}
		}
		binds.add(params.substring(start));
		return binds;
	}
	
	public static UnescapedSQL unescapeLiteralSQL(String sql, String params) {
		if (StringUtil.isEmpty(sql) || StringUtil.isEmpty(params)) {
			return new UnescapedSQL(sql, params);
		}
		ArrayList<String> paramList = divideParams(params);
		
		StringBuilder sqlBuilder = new StringBuilder();
		
		int index = 0;
		int pos = 0;
		Matcher m = pattern.matcher(sql);
		while (m.find()) {
			sqlBuilder.append(sql.substring(pos, m.start())).append(StringUtil.stripSideChar(paramList.get(index), '\''));
			pos = m.end();
			index++;
		}
		sqlBuilder.append(sql.substring(pos));
		
		String sqlParam = null;
		if (index < paramList.size()) {
			StringBuffer sb = new StringBuffer();
			for (; index < paramList.size(); index++) {
				if (sb.length() > 0) {
					sb.append(",");
				}
				sb.append(paramList.get(index));
			}
			sqlParam = sb.toString();
		}
		return new UnescapedSQL(sqlBuilder.toString(), sqlParam);
	}
	
	public static class UnescapedSQL {
		public String sql;
		public String param;
		public UnescapedSQL(String sql, String param) {
			this.sql = sql;
			this.param = param;
		}
		public String toString() {
			return "UnescapedSQL [sql=" + sql + ", param=" + param + "]";
		}
	}
	
	public static String replaceSQLParameter(String sql, String params) {
		UnescapedSQL unescapedSql = unescapeLiteralSQL(sql, params);
		sql = unescapedSql.sql;
		params = unescapedSql.param;
		if (StringUtil.isEmpty(sql) || StringUtil.isEmpty(params)) {
			return sql;
		}
		ArrayList<String> paramList = divideParams(params);
		StringBuilder sqlBuilder = new StringBuilder();
		
		int sqlLength = sql.length();
		int search;
		int index = 0;
		int pos = 0;
		
		try {
			while(pos < sqlLength){
				search = sql.indexOf('?', pos);
				if(search < 0 ){
					sqlBuilder.append(sql.substring(pos));
					break;
				}
				sqlBuilder.append(sql.substring(pos, search)).append(paramList.get(index));
				index++;
				pos = search + 1;
			}
		} catch (Exception e) {
			return ">>>> Failed bind parameter : " + e.getMessage();
		}
		return sqlBuilder.toString();
	}
	
	static public void main(String [] args){
		try {
			System.out.println(convertBindVariable("?sel?ect =? test???-?-?"));
			
			String sql = "select @,@,@ from emp where emp_id=? and sex=?";
			String param = "age,weight,score,1234,'M'";
			System.out.println(SqlMakerUtil.replaceSQLParameter(sql, param));
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
}