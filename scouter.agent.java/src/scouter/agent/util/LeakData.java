package scouter.agent.util;

public class LeakData {
	public Error error;
	public String inner;
	public int service;
	public long txid;
	public boolean fullstack;
	public LeakData(Error error, String inner, int service, long txid, boolean fullstack) {
		super();
		this.error = error;
		this.inner = inner;
		this.service = service;
		this.txid = txid;
		this.fullstack = fullstack;
	}
	@Override
	public String toString() {
		return "LeakData [error=" + error + ", inner=" + inner + ", service=" + service + ", txid=" + txid
				+ ", fullstack=" + fullstack + "]";
	}

	
}
