package scouter.client.xlog;

public enum XLogYAxisEnum {
	
	ELAPSED("Elapsed", "Elapsed (sec)", true, 9),
	CPU("Cpu Time", "Cpu Time (ms)", false, 100),
	SQL_TIME("SQL Time", "SQL Time (sec)", false, 9),
	SQL_COUNT("SQL Count", "SQL Count (cnt)", false, 50),
	APICALL_TIME("ApiCall Time", "ApiCall Time (sec)", false, 9),
	APICALL_COUNT("ApiCall Count", "ApiCall Count (cnt)", false, 50),
	HEAP_USED("Memory Allocation", "Memory Allocation (KB)", false, 5000);
	
	private final String name;
	private final String desc;
	private final boolean isDefault;
	private final double defaultMax;
	
	private XLogYAxisEnum(String name, String desc, boolean isDefault, double defaultMax) {
		this.name = name;
		this.desc = desc;
		this.isDefault = isDefault;
		this.defaultMax = defaultMax;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getDesc() {
		return this.desc;
	}
	
	public boolean isDefault() {
		return this.isDefault;
	}
	
	public double getDefaultMax() {
		return this.defaultMax;
	}
}
