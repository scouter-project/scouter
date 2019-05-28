/*
 *  Copyright 2015 the original author or authors.
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package scouterx.webapp.layer.consumer;

import scouter.lang.constants.ParamConstant;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.ObjectPack;
import scouter.lang.value.BlobValue;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.IPUtil;
import scouter.util.StringUtil;
import scouterx.webapp.framework.client.model.TextProxy;
import scouterx.webapp.framework.client.net.TcpProxy;
import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.framework.client.server.ServerManager;
import scouterx.webapp.model.HeapHistogramData;
import scouterx.webapp.model.SocketObjectData;
import scouterx.webapp.model.ThreadObjectData;
import scouterx.webapp.model.VariableData;
import scouterx.webapp.model.scouter.SObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 *
 * Modified by David Kim (david100gom@gmail.com) on 2019. 5. 26.
 *
 */
public class ObjectConsumer {

    /**
     * retrieve object(agent) list from collector server
     */
    public List<SObject> retrieveObjectList(final Server server) {
        List<SObject> objectList = null;
        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
            objectList = tcpProxy
                    .process(RequestCmd.OBJECT_LIST_REAL_TIME, null).stream()
                    .map(p -> SObject.of((ObjectPack) p, server))
                    .collect(Collectors.toList());
        }

        return objectList;
    }

    /**
     * retrieve object(agent) thread list from collector server
     *
     */
    public List<ThreadObjectData> retrieveThreadList(int objHash, Server server) {

        List<ThreadObjectData> dataList = new ArrayList<>();

        try (TcpProxy tcp = TcpProxy.getTcpProxy(server)) {

            MapPack param = new MapPack();
            param.put(ParamConstant.OBJ_HASH, objHash);

            MapPack mapPack = (MapPack) tcp.getSingle(RequestCmd.OBJECT_THREAD_LIST, param);

            List<String> threadKeys = Arrays.asList("id", "name", "stat", "cpu", "txid", "elapsed", "service");

            Map<String, ListValue> threadObjectMap = new HashMap<>();
            threadKeys.forEach(key -> {
                threadObjectMap.put(key, mapPack.getList(key));
            });

            int count = 0;

            if (threadObjectMap.containsKey("id")) {
                count = threadObjectMap.get("id").size();
            }

            for (int i = 0; i < count; i++) {
                dataList.add(new ThreadObjectData(threadObjectMap, i));
            }

        }

        return dataList;

    }

    /**
     * retrieve object(agent) thread dump from collector server
     *
     */
    public String retrieveThreadDump(int objHash, Server server) {

        StringBuilder sb = new StringBuilder();

        try (TcpProxy tcp = TcpProxy.getTcpProxy(server)) {
            MapPack param = new MapPack();
            param.put(ParamConstant.OBJ_HASH, objHash);
            MapPack mpack = (MapPack) tcp.getSingle(RequestCmd.OBJECT_THREAD_DUMP, param);

            if (mpack != null) {
                ListValue lv = mpack.getList("threadDump");
                for (int i = 0; i < lv.size(); i++) {
                    sb.append(lv.getString(i) + "\n");
                }
            }
        }

        return sb.toString();

    }

    /**
     * retrieve object(agent) heap histogram from collector server
     *
     */
    public List<HeapHistogramData> retrieveHeapHistogram(int objHash, Server server) {

        List<HeapHistogramData> list = new ArrayList<HeapHistogramData>();

        try (TcpProxy tcp = TcpProxy.getTcpProxy(server)) {
            MapPack param = new MapPack();
            param.put(ParamConstant.OBJ_HASH, objHash);
            MapPack mpack = (MapPack) tcp.getSingle(RequestCmd.OBJECT_HEAPHISTO, param);

            ListValue lv = mpack.getList("heaphisto");

            for (int i = 0; i < lv.size(); i++) {
                HeapHistogramData data = new HeapHistogramData();
                String[] tokens = StringUtil.tokenizer(lv.getString(i)," ");
                if (tokens == null || tokens.length < 4) {
                    continue;
                }
                String index = removeNotDigit(tokens[0]);
                data.no = CastUtil.cint(index);
                data.count = CastUtil.cint(tokens[1]);
                data.size = CastUtil.clong(tokens[2]);
                data.name = getCanonicalName(tokens[3]);
                list.add(data);
            }

        }

        return list;

    }

    /**
     * retrieve object(agent) environment info from collector server
     *
     */
    public List<VariableData> retrieveEnv(int objHash, Server server) {

        List<VariableData> list = null;

        try (TcpProxy tcp = TcpProxy.getTcpProxy(server)) {

            MapPack param = new MapPack();
            param.put(ParamConstant.OBJ_HASH, objHash);
            param.put("userSession", server.getSession());
            param.put("userIp", tcp.getLocalInetAddress().getHostAddress());

            MapPack mapPack = (MapPack) tcp.getSingle(RequestCmd.OBJECT_ENV, param);
            if (mapPack != null) {
                list = new ArrayList<>();
                Iterator<String> keys = mapPack.keys();
                while (keys.hasNext()) {
                    String name = keys.next();
                    String value = CastUtil.cString(mapPack.get(name));
                    VariableData data = new VariableData();
                    list.add(data);
                    data.name = name;
                    data.value = value;
                }

            }

        }

        return list;
    }

    /**
     * retrieve object(agent) socket info from collector server
     *
     */
    public List<SocketObjectData> retrieveSocket(int objHash, int serverId) {

        List<SocketObjectData> list = new ArrayList<>();

        Server server = ServerManager.getInstance().getServerIfNullDefault(serverId);

        try (TcpProxy tcp = TcpProxy.getTcpProxy(server)) {

            MapPack param = new MapPack();
            param.put(ParamConstant.OBJ_HASH, objHash);
            MapPack m = (MapPack) tcp.getSingle(RequestCmd.OBJECT_SOCKET, param);

            ListValue keyLv = m.getList("key");
            ListValue hostLv = m.getList("host");
            ListValue portLv = m.getList("port");
            ListValue countLv = m.getList("count");
            ListValue serviceLv = m.getList("service");
            ListValue txidLv = m.getList("txid");
            ListValue orderLv = m.getList("order");
            ListValue stackLv = m.getList("stack");

            for (int i = 0; i < keyLv.size(); i++) {
                SocketObjectData socketObj = new SocketObjectData();
                socketObj.key = keyLv.getLong(i);
                socketObj.host = IPUtil.toString(((BlobValue) hostLv.get(i)).value);
                socketObj.port = portLv.getInt(i);
                socketObj.count = countLv.getInt(i);
                socketObj.service = TextProxy.service.getTextIfNullDefault(Long.parseLong(DateUtil.yyyymmdd()), serviceLv.getInt(i), serverId);
                socketObj.txid = txidLv.getLong(i);
                socketObj.standby = orderLv.getBoolean(i);
                socketObj.stack = stackLv.getString(i);
                list.add(socketObj);
            }

        }

        return list;
    }

    /**
     *
     * get only digit
     *
     * @param name String
     * @return
     */
    private static String removeNotDigit(String name) {
        StringBuffer sb = new StringBuffer();
        char[] charArray = name.toCharArray();
        for (int i = 0; i < charArray.length; i++) {
            if (Character.isDigit(charArray[i])) {
                sb.append(charArray[i]);
            }
        }
        return sb.toString();
    }

    /**
     *
     * get canonical name
     *
     * @param className String
     * @return
     */
    private static String getCanonicalName(String className) {
        if (StringUtil.isEmpty(className)) {
            return className;
        }
        int arrayCnt = 0;
        boolean prefix = true;
        char[] arr = className.toCharArray();
        int offset = 0;
        StringBuilder sb = new StringBuilder();
        for (; offset < arr.length && prefix; offset++) {
            if (arr[offset] == '[') {
                arrayCnt++;
                continue;
            } else if(offset == 0 || arr[offset-1] == '[') {
                if ('L' == arr[offset]) {
                    if (className.endsWith(";")) {
                        sb.append(className.substring(offset + 1, className.length() - 1));
                    } else {
                        sb.append(className.substring(offset + 1));
                    }
                } else if ('V' == arr[offset]) {
                    sb.append("void");
                } else if ('Z' == arr[offset]) {
                    sb.append("boolean");
                } else if ('C' == arr[offset]) {
                    sb.append("char");
                } else if ('B' == arr[offset]) {
                    sb.append("byte");
                } else if ('S' == arr[offset]) {
                    sb.append("short");
                } else if ('I' == arr[offset]) {
                    sb.append("int");
                } else if ('F' == arr[offset]) {
                    sb.append("float");
                } else if ('J' == arr[offset]) {
                    sb.append("long");
                } else if ('D' == arr[offset]) {
                    sb.append("double");
                } else {
                    sb.append(className);
                }
                prefix = false;
            }
        }
        while(arrayCnt > 0) {
            sb.append("[]");
            arrayCnt--;
        }
        return sb.toString();
    }

}
