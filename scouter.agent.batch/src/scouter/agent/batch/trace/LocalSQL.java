package scouter.agent.batch.trace;

public class LocalSQL extends java.util.HashMap<Integer, TraceSQL>{
	private static final long serialVersionUID = 1L;
	
	public LocalSQL() {
		super(100);
	}
	
	public TraceSQL get(String sqlText){
		int hashValue = sqlText.hashCode();
		TraceSQL traceSql = super.get(hashValue);
		if(traceSql == null){
			hashValue = TraceContext.getInstance().getSQLHash(sqlText, hashValue);
			
			traceSql = new TraceSQL();
			traceSql.hashValue = hashValue;
			super.put(hashValue, traceSql);
		}
		return traceSql;
	}	
}
