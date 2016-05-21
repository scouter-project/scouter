package scouter.agent.batch.trace;

public class TraceSQL extends java.util.HashMap<Integer, SQL>{
	public static final String CURRENT_SQL_FIELD = "_current_sql_";
	private static final long serialVersionUID = 1L;
	
	public TraceSQL() {
		super(100);
	}
	
	private SQL get(String sqlText){
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
