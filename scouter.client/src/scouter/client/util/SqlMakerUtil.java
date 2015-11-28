package scouter.client.util;

import java.util.ArrayList;

public class SqlMakerUtil {
	static public String bindSQL(String sqlText, String params){
		if(params == null || "".equals(params)){
			return sqlText;
		}
		
		ArrayList<String> binds = divideParams(params);
		if(binds == null || binds.size() == 0){
			return sqlText;
		}
		
		int index = 0;
		int pos = 0;
		
		int sqlLength = sqlText.length();
		int bindLength =  binds.size();
		String bind;
		int search;
		boolean isChar;

		StringBuilder sb = new StringBuilder(100);
		while(pos < sqlLength){
			search = sqlText.indexOf('@', pos);
			
			if(search < 0 || index >= bindLength){
				sb.append(sqlText.substring(pos));
				break;
			}
			
			bind = binds.get(index);
			if(bind.charAt(0) == '\''){
				isChar = true;
			}else{
				isChar = false;
			}
			
			if(isChar){
				if(search == 0 || (search + 1) == sqlLength){
					return errorMessage(sb, sqlText, bind, "SQL Character Position Check", pos, search, isChar);
				}
				
				if(sqlText.charAt(search - 1) != '\'' || sqlText.charAt(search + 1) != '\''){
					return errorMessage(sb, sqlText, bind, "SQL Character Quata Check", pos, search, isChar);
				}
				
				sb.append(sqlText.subSequence(pos, search - 1));
				sb.append(bind);
				pos = search + 2;
			}else{
				if(search > 0){
					if(" \t=<>,+-*/|^&(".indexOf(sqlText.charAt(search-1))<0){
						return errorMessage(sb, sqlText, bind, "Number Check", pos, search, isChar);						
					}
				}
				sb.append(sqlText.subSequence(pos, search));
				sb.append(bind);
				pos = search + 1;
			}
			
			index++;
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
}