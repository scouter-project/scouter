package scouterx.webapp.layer.consumer;

import lombok.extern.slf4j.Slf4j;
import scouter.lang.constants.ParamConstant;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouterx.webapp.framework.client.net.TcpProxy;
import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.model.ProcessObject;

import java.util.*;

/**
 * @author leekyoungil (leekyoungil@gmail.com) on 2017. 10. 14.
 */
@Slf4j
public class HostObjectRequestConsumer {

    /**
     * get top(command) of process by objHash
     *
     * @param objHash
     * @param server
     * @return List<ProcessObject>
     */
    public List<ProcessObject> retrieveRealTimeTopByObjType(final int objHash, final Server server) {
        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
            MapPack mapPack = new MapPack();
            mapPack.put(ParamConstant.OBJ_HASH, objHash);
            final MapPack outMapPack = (MapPack) tcpProxy.getSingle(RequestCmd.HOST_TOP, mapPack);
            if (outMapPack == null) {
                return null;
            }

            final List<String> hostTopKeys = Arrays.asList("PID", "USER", "CPU", "MEM", "TIME", "NAME");
            final Map<String, ListValue> hostTopMap = new HashMap<>();
            hostTopKeys.forEach(key -> {
                hostTopMap.put(key, outMapPack.getList(key));
            });

            final List<ProcessObject> procList = new ArrayList<>();
            int pidLoopCount = 0;

            if (hostTopMap.containsKey("PID")) {
                pidLoopCount = hostTopMap.get("PID").size();
            }

            for (int i = 0; i < pidLoopCount; i++) {
                procList.add(new ProcessObject(hostTopMap, i));
            }

            return procList;
        }
    }
}
