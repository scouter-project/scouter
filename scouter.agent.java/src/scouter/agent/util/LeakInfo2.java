package scouter.agent.util;

/**
 * Leak Info having an origin object.
 * @author Gun Lee(gunlee01@gmail.com)
 */
public class LeakInfo2 {
    public Error error;
    public String inner;
    public Object innerObject;
    public int serviceHash;
    public long txid;
    public boolean fullstack;
    public int fullstackSkip;
    public ICloseManager closeManager;

    /**
     * constructor
     * @param error error object to describe
     * @param inner original object leackable
     * @param closeManager  close manager for the inner
     * @param serviceHash service hash id
     * @param txid txid
     * @param fullstack the flag for displaying full stack trace or not on a leak detecting
     * @param fullstackSkip skip rows of the full stack trace
     */
    public LeakInfo2(Error error, Object inner, ICloseManager closeManager, int serviceHash, long txid, boolean fullstack, int fullstackSkip) {
        super();
        this.error = error;
        this.inner = inner.getClass().getName();
        this.innerObject = inner;
        this.serviceHash = serviceHash;
        this.txid = txid;
        this.fullstack = fullstack;
        this.fullstackSkip = fullstackSkip;
        this.closeManager = closeManager;
    }

    @Override
    public String toString() {
        return "LeakInfo [error=" + error + ", inner=" + inner + ", serviceHash=" + serviceHash + ", txid=" + txid
                + ", fullstack=" + fullstack + "]";
    }
}
