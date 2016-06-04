package scouter.agent.batch.trace;

public class LocalSQL extends java.util.HashMap<Integer, TraceSQL>{
	private static final long serialVersionUID = 1L;
	private String currentThreadName;
	private TraceSQL currentTraceSql;
	private Thread currentThread;
	
	public LocalSQL() {
		super(100);
		this.currentThread = Thread.currentThread();
		this.currentThreadName = this.currentThread.getName();
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
		currentTraceSql = traceSql;
		return traceSql;
	}
	
	public TraceSQL getCurrentTraceSQL(){
		return currentTraceSql;
	}
	
	public Thread getThread(){
		return currentThread;
	}
	
	public String toString(){
		return currentThreadName + ": " + super.toString();
	}
}
