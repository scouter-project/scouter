package scouter.agent.batch.trace;

public class SQL {
	public Integer hashValue;
	public int count = 0;
	
	public int startTime = -1;
	public int endTime = 0;
	public int totalTime = 0;
	public int minTime = 0;
	public int maxTime = 0;
	
	public long processedRows = 0L;
}
