package scouter.agent.util;

public class LeakInfo {
    public Error error;
    public String inner;
    public int serviceHash;
    public long txid;
    public boolean fullstack;
    public int fullstackSkip;

    public LeakInfo(Error error, String inner, int serviceHash, long txid, boolean fullstack, int fullstackSkip) {
        super();
        this.error = error;
        this.inner = inner;
        this.serviceHash = serviceHash;
        this.txid = txid;
        this.fullstack = fullstack;
        this.fullstackSkip = fullstackSkip;
    }

    @Override
    public String toString() {
        return "LeakInfo [error=" + error + ", inner=" + inner + ", serviceHash=" + serviceHash + ", txid=" + txid
                + ", fullstack=" + fullstack + "]";
    }
}
