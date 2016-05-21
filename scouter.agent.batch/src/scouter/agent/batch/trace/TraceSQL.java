package scouter.agent.batch.trace;

import scouter.agent.batch.Configure;

public class TraceSQL extends java.util.HashMap<Integer, SQL>{
	private static final long serialVersionUID = 1L;
		
	public TraceSQL() {
		super(100);
	}
	
	public SQL get(String sqlText){
		int hashValue  = sqlText.hashCode();
		SQL sql = super.get(hashValue);
		if(sql == null){
			hashValue = TraceContext.getInstance().getSQLHash(sqlText, hashValue);
			
			sql = new SQL();
			sql.hashValue = hashValue;
			super.put(hashValue, sql);
		}
		return sql;
	}
}
