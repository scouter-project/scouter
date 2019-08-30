package scouterx.webapp.layer.consumer;

import lombok.extern.slf4j.Slf4j;
import scouter.lang.constants.ParamConstant;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouterx.webapp.framework.client.net.TcpProxy;
import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.model.HostDiskData;
import scouterx.webapp.model.ProcessObject;

import java.util.*;

/**
 * @author leekyoungil (leekyoungil@gmail.com) on 2017. 10. 14.
 *
 * Modified by David Kim (david100gom@gmail.com) on 2019. 5. 12.
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

    /**
     * get disk usage information
     *
     * @param objHash object id
     * @param server server id
     * @return List<HostDiskData>
     */
    public List<HostDiskData> retrieveRealTimeDiskByObjType(int objHash, Server server) {

        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {

            MapPack mapPack = new MapPack();
            mapPack.put(ParamConstant.OBJ_HASH, objHash);

            MapPack outMapPack = (MapPack) tcpProxy.getSingle(RequestCmd.HOST_DISK_USAGE, mapPack);
            if (outMapPack == null) {
                return null;
            }

            List<String> hostDiskKeys = Arrays.asList("Device", "Total", "Used", "Free", "Pct", "Type", "Mount");

            Map<String, ListValue> hostDiskMap = new HashMap<>();
            hostDiskKeys.forEach(key -> {
                hostDiskMap.put(key, outMapPack.getList(key));
            });

            List<HostDiskData> dataList = new ArrayList<>();
            int diskLoopCount = 0;

            if (hostDiskMap.containsKey("Device")) {
                diskLoopCount = hostDiskMap.get("Device").size();
            }

            for (int i = 0; i < diskLoopCount; i++) {
                dataList.add(new HostDiskData(hostDiskMap, i));
            }

            return dataList;
        }
    }
}
