package scouterx.webapp.model;

import java.util.Map;

import lombok.Getter;
import lombok.ToString;
import scouter.lang.value.ListValue;

/**
 * @author Created by David Kim (david100gom@gmail.com) on 2019. 5. 12.
 *
 */
@Getter
@ToString
public class HostDiskData {

    private String device;
    private long total;
    private long used;
    private long free;
    private double pct;
    private String type;
    private String mount;

    public HostDiskData (Map<String,ListValue> hostDiskMap, int index) {
        this.device = hostDiskMap.get("Device").getString(index);
        this.total = hostDiskMap.get("Total").getLong(index);
        this.used = hostDiskMap.get("Used").getLong(index);
        this.free = hostDiskMap.get("Free").getLong(index);
        this.pct = hostDiskMap.get("Pct").getLong(index);
        this.type = hostDiskMap.get("Type").getString(index);
        this.mount = hostDiskMap.get("Mount").getString(index);
    }
}
