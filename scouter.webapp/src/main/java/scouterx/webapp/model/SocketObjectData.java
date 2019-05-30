package scouterx.webapp.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Created by David Kim (david100gom@gmail.com) on 2019. 5. 26.
 *
 * Github : https://github.com/david100gom
 */
@Getter
@Setter
@ToString
public class SocketObjectData {

    public long key;
    public String host;
    public int port;
    public long count;
    public String service;
    public long txid;
    public boolean standby;
    public String stack;

}
