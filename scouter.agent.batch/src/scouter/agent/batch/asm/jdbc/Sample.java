package scouter.agent.batch.asm.jdbc;

import scouter.agent.batch.trace.TraceSQL;

public abstract class Sample {
	private TraceSQL traceSql;
	public int executeUpdate(){
		int arrays = 0;
		try {
			arrays = execute();
		}finally{
			traceSql.end();
		}
		return arrays;
	}
	
	abstract public int execute();
}
