/*
 *  Copyright 2015 the original author or authors.
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package scouter.agent.netio.request.handle;

import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.netio.request.anotation.RequestHandler;
import scouter.agent.trace.CentralRejectControl;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.ListValue;
import scouter.lang.value.MapValue;
import scouter.net.RequestCmd;

import java.util.List;

/**
 * Handles the periodic reject-control sync initiated by the collector.
 *
 * Request  (collector -&gt; agent) : policyVersion, blockVersion, full, [policy], [blockIp, blockUrl]
 * Response (agent -&gt; collector) : the versions this agent now holds, plus every
 *                                 violation detected locally since the previous sync.
 *
 * Piggybacking the report on the response means there is no separate agent-to-server
 * channel to build and nothing to lose.
 */
public class AgentRejectControl {

    private static final Configure conf = Configure.getInstance();

    @RequestHandler(RequestCmd.REJECT_CONTROL_SYNC)
    public Pack sync(Pack param) {
        MapPack req = (MapPack) param;
        MapPack res = new MapPack();

        CentralRejectControl central = CentralRejectControl.getInstance();

        if (!conf.control_reject_central_enabled) {
            res.put("centralEnabled", "false");
            res.put("policyVersion", 0);
            res.put("blockVersion", 0);
            return res;
        }

        try {
            boolean full = "true".equals(req.getText("full"));
            long policyVersion = req.getLong("policyVersion");
            long blockVersion = req.getLong("blockVersion");

            if (full) {
                MapValue policy = (MapValue) req.get("policy");
                if (policy != null) {
                    central.applyPolicy(policy, policyVersion);
                }

                ListValue ips = req.getList("blockIp");
                if (ips != null) {
                    ListValue urls = req.getList("blockUrl");
                    int n = ips.size();
                    String[] ipArr = new String[n];
                    String[] urlArr = new String[n];
                    for (int i = 0; i < n; i++) {
                        ipArr[i] = ips.getString(i);
                        urlArr[i] = urls == null || i >= urls.size() ? null : urls.getString(i);
                    }
                    central.applyBlockList(ipArr, urlArr, blockVersion);
                } else {
                    central.applyBlockList(new String[0], new String[0], blockVersion);
                }
            }
        } catch (Throwable t) {
            Logger.println("A912", "reject control sync failed", t);
        }

        res.put("centralEnabled", "true");
        res.put("policyVersion", central.getPolicyVersion());
        res.put("blockVersion", central.getBlockVersion());
        res.put("blockCount", central.getBlockCount());

        // hand over locally detected violations
        List<CentralRejectControl.Detection> detections = central.drainDetections();
        if (detections.size() > 0) {
            ListValue dIp = res.newList("detectIp");
            ListValue dUrl = res.newList("detectUrl");
            ListValue dReason = res.newList("detectReason");
            ListValue dTime = res.newList("detectTime");
            for (int i = 0; i < detections.size(); i++) {
                CentralRejectControl.Detection d = detections.get(i);
                dIp.add(d.ip);
                dUrl.add(d.url == null ? "" : d.url);
                dReason.add(d.reason == null ? "" : d.reason);
                dTime.add(d.time);
            }
        }
        return res;
    }
}
