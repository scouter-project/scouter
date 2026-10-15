package scouterx.webapp.layer.websock.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientParams {
    
    private int serverId;
    private Set<Integer> objHashes;
    private Set<String> counterNames; // 예: ["TPS", "HeapUsed"]
    private Set<String> objTypes;     // 예: ["java", "tomcat"]
    
    // XLog 실시간 조회를 위한 Cursor
    private int xlogLoop;
    private int xlogIndex;
    
    private int xlogMaxCount = 10000;

    // Getters and Setters
    public int getServerId() { return serverId; }
    public void setServerId(int serverId) { this.serverId = serverId; }

    public Set<Integer> getObjHashes() { return objHashes; }
    public void setObjHashes(Set<Integer> objHashes) { this.objHashes = objHashes; }

    public Set<String> getCounterNames() { return counterNames; }
    public void setCounterNames(Set<String> counterNames) { this.counterNames = counterNames; }

    public Set<String> getObjTypes() { return objTypes; }
    public void setObjTypes(Set<String> objTypes) { this.objTypes = objTypes; }

    public int getXlogLoop() { return xlogLoop; }
    public void setXlogLoop(int xlogLoop) { this.xlogLoop = xlogLoop; }

    public int getXlogIndex() { return xlogIndex; }
    public void setXlogIndex(int xlogIndex) { this.xlogIndex = xlogIndex; }

    public int getXlogMaxCount() { return xlogMaxCount; }
    public void setXlogMaxCount(int xlogMaxCount) { this.xlogMaxCount = xlogMaxCount; }
}
