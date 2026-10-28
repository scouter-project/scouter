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

import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.lang.value.MapValue;
import scouter.net.RequestCmd;
import scouter.util.StringUtil;
import scouterx.webapp.framework.client.net.TcpProxy;
import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.model.RejectBlockData;
import scouterx.webapp.model.RejectControlStatusData;
import scouterx.webapp.model.RejectDetectionData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Talks to the collector's central reject(block) control.
 *
 * Note that this never reaches an agent directly - the collector owns the
 * policy and the block list, and pushes them out on its own sync cycle. A change
 * made here therefore takes effect on the agents within one sync interval
 * (reject_control_sync_interval_ms, 2s by default) rather than instantly.
 */
public class RejectControlConsumer {

    public RejectControlStatusData getStatus(final Server server) {
        MapPack resultPack;
        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
            resultPack = (MapPack) tcpProxy.getSingle(RequestCmd.REJECT_CONTROL_LIST_BLOCK, new MapPack());
        }

        RejectControlStatusData status = new RejectControlStatusData();
        if (resultPack == null) {
            return status;
        }

        status.setBlockVersion(resultPack.getLong("blockVersion"));
        status.setBlocks(toBlocks(resultPack));
        status.setDetections(toDetections(resultPack));
        return status;
    }

    private List<RejectBlockData> toBlocks(MapPack pack) {
        List<RejectBlockData> list = new ArrayList<>();
        ListValue ip = pack.getList("ip");
        if (ip == null) {
            return list;
        }
        ListValue url = pack.getList("url");
        ListValue reason = pack.getList("reason");
        ListValue time = pack.getList("blockedTime");
        ListValue by = pack.getList("blockedBy");
        ListValue cnt = pack.getList("detectCount");

        for (int i = 0; i < ip.size(); i++) {
            list.add(new RejectBlockData(
                    ip.getString(i),
                    str(url, i),
                    str(reason, i),
                    num(time, i),
                    str(by, i),
                    num(cnt, i)));
        }
        return list;
    }

    private List<RejectDetectionData> toDetections(MapPack pack) {
        List<RejectDetectionData> list = new ArrayList<>();
        ListValue ip = pack.getList("detectIp");
        if (ip == null) {
            return list;
        }
        ListValue url = pack.getList("detectUrl");
        ListValue reason = pack.getList("detectReason");
        ListValue first = pack.getList("detectFirstTime");
        ListValue last = pack.getList("detectLastTime");
        ListValue cnt = pack.getList("detectCount2");
        ListValue objNames = pack.getList("detectObjNames");

        for (int i = 0; i < ip.size(); i++) {
            list.add(new RejectDetectionData(
                    ip.getString(i),
                    str(url, i),
                    str(reason, i),
                    num(first, i),
                    num(last, i),
                    num(cnt, i),
                    str(objNames, i)));
        }
        return list;
    }

    private static String str(ListValue lv, int i) {
        return lv == null || i >= lv.size() ? "" : StringUtil.trimToEmpty(lv.getString(i));
    }

    private static long num(ListValue lv, int i) {
        return lv == null || i >= lv.size() ? 0L : lv.getLong(i);
    }

    public boolean addBlock(String ip, String url, String reason, String by, final Server server) {
        MapPack param = new MapPack();
        param.put("ip", StringUtil.trimToEmpty(ip));
        param.put("url", StringUtil.trimToEmpty(url));
        param.put("reason", StringUtil.trimToEmpty(reason));
        param.put("by", StringUtil.trimToEmpty(by));

        MapPack result;
        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
            result = (MapPack) tcpProxy.getSingle(RequestCmd.REJECT_CONTROL_ADD_BLOCK, param);
        }
        return result != null && "true".equals(result.getText("result"));
    }

    public boolean removeBlock(String ip, String url, final Server server) {
        MapPack param = new MapPack();
        param.put("ip", StringUtil.trimToEmpty(ip));
        param.put("url", StringUtil.trimToEmpty(url));

        MapPack result;
        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
            result = (MapPack) tcpProxy.getSingle(RequestCmd.REJECT_CONTROL_REMOVE_BLOCK, param);
        }
        return result != null && "true".equals(result.getText("result"));
    }

    public int clear(String target, final Server server) {
        MapPack param = new MapPack();
        param.put("target", StringUtil.trimToEmpty(target));

        MapPack result;
        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
            result = (MapPack) tcpProxy.getSingle(RequestCmd.REJECT_CONTROL_CLEAR_BLOCK, param);
        }
        return result == null ? 0 : (int) result.getLong("removed");
    }

    public Map<String, String> getPolicy(final Server server) {
        MapPack result;
        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
            result = (MapPack) tcpProxy.getSingle(RequestCmd.REJECT_CONTROL_GET_POLICY, new MapPack());
        }

        Map<String, String> policy = new LinkedHashMap<>();
        if (result == null) {
            return policy;
        }
        MapValue mv = (MapValue) result.get("policy");
        if (mv != null) {
            for (String key : mv.keySet()) {
                policy.put(key, mv.getText(key));
            }
        }
        return policy;
    }

    public boolean setPolicy(Map<String, String> policy, final Server server) {
        MapValue mv = new MapValue();
        if (policy != null) {
            for (Map.Entry<String, String> e : policy.entrySet()) {
                if (StringUtil.isNotEmpty(e.getKey())) {
                    mv.put(e.getKey(), StringUtil.trimToEmpty(e.getValue()));
                }
            }
        }
        MapPack param = new MapPack();
        param.put("policy", mv);

        MapPack result;
        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
            result = (MapPack) tcpProxy.getSingle(RequestCmd.REJECT_CONTROL_SET_POLICY, param);
        }
        return result != null && "true".equals(result.getText("result"));
    }
}
