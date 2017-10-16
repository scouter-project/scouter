package scouterx.webapp.model;

import lombok.Getter;
import lombok.ToString;
import scouter.lang.value.ListValue;
import scouter.util.DateUtil;

import java.util.Map;

/**
 * @author leekyoungil (leekyoungil@gmail.com) on 2017. 10. 14.
 */
@Getter
@ToString
public class ProcessObject {

    private int pid;
    private String user;
    private float cpu;
    private long mem;
    private long time;
    private String name;

    public ProcessObject (final Map<String,ListValue> hostTopMap, final int index) {
        this.pid = hostTopMap.get("PID").getInt(index);
        this.user = hostTopMap.get("USER").getString(index);
        this.cpu = hostTopMap.get("CPU").getFloat(index);
        this.mem = hostTopMap.get("MEM").getLong(index);
        this.time = hostTopMap.get("TIME").getLong(index);
        this.name = hostTopMap.get("NAME").getString(index);
    }
}
