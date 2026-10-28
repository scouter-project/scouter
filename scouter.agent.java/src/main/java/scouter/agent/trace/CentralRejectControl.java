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
package scouter.agent.trace;

import scouter.agent.Configure;
import scouter.lang.value.MapValue;
import scouter.util.HashUtil;
import scouter.util.LongKeyLinkedMap;
import scouter.util.StringUtil;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Holds the centrally managed reject policy and block list pushed by the collector,
 * and buffers locally detected violations until the collector picks them up.
 *
 * Sync model is collector-driven : the collector periodically calls
 * {@code REJECT_CONTROL_SYNC} on this agent, hands over the policy / block list,
 * and collects the pending detections from the response of the very same call.
 * There is therefore no agent-to-server push path and nothing can be lost to UDP.
 *
 * Enforcement always stays local - the request path only reads volatile fields,
 * so a collector outage never adds latency and never unblocks anything.
 */
public class CentralRejectControl {

    private static final CentralRejectControl instance = new CentralRejectControl();

    public static CentralRejectControl getInstance() {
        return instance;
    }

    /** a locally detected violation waiting to be reported to the collector */
    public static class Detection {
        public final String ip;
        public final String url;
        public final String reason;
        public final long time;

        Detection(String ip, String url, String reason, long time) {
            this.ip = ip;
            this.url = url;
            this.reason = reason;
            this.time = time;
        }
    }

    private final Configure conf = Configure.getInstance();

    /**
     * Block table pushed by the collector. Replaced wholesale on every full sync,
     * so the request path can read it without any lock.
     * key -&gt; marker. built by {@link #keyOfIp(String)} / {@link #keyOfIpUrl(String, String)}.
     */
    private volatile LongKeyLinkedMap<String> blockTable = new LongKeyLinkedMap<String>();

    private volatile RejectPolicy policy = new RejectPolicy(Configure.getInstance(), null);

    private volatile long policyVersion = 0;
    private volatile long blockVersion = 0;

    /** true once the collector has pushed anything - used to tell 'no central control' apart from 'empty list' */
    private volatile boolean synced = false;

    /**
     * Pending detections, keyed so that one hammering client cannot flood the buffer -
     * repeated violations of the same (ip,url) collapse into a single entry.
     */
    private final LongKeyLinkedMap<Detection> pending = new LongKeyLinkedMap<Detection>();
    private long droppedDetectionCount = 0;

    private CentralRejectControl() {
    }

    // ------------------------------------------------------------------ keys

    /** key of an ip-wide block (every url of that ip) */
    public static long keyOfIp(String ip) {
        return ((long) HashUtil.hash(ip) << 32) | 0xFFFFFFFFL;
    }

    /** key of an (ip + url) block */
    public static long keyOfIpUrl(String ip, String url) {
        long h = HashUtil.hash(url) & 0xFFFFFFFFL;
        // 0xFFFFFFFF is reserved as the 'whole ip' marker
        if (h == 0xFFFFFFFFL) {
            h = 0xFFFFFFFEL;
        }
        return ((long) HashUtil.hash(ip) << 32) | h;
    }

    // --------------------------------------------------------------- reading

    public RejectPolicy getPolicy() {
        return policy;
    }

    /**
     * @return true if this (ip) or (ip,url) is on the centrally managed block list.
     */
    public boolean isBlocked(String ip, String url) {
        LongKeyLinkedMap<String> t = blockTable;
        if (t.size() == 0) {
            return false;
        }
        if (t.get(keyOfIp(ip)) != null) {
            return true;
        }
        return url != null && t.get(keyOfIpUrl(ip, url)) != null;
    }

    public boolean isSynced() {
        return synced;
    }

    public long getPolicyVersion() {
        return policyVersion;
    }

    public long getBlockVersion() {
        return blockVersion;
    }

    public int getBlockCount() {
        return blockTable.size();
    }

    // --------------------------------------------------------------- writing

    /**
     * Apply a full payload pushed by the collector.
     *
     * @param ips   parallel array of ip. required.
     * @param urls  parallel array of url. an empty/null entry means 'whole ip'.
     */
    public void applyBlockList(String[] ips, String[] urls, long version) {
        LongKeyLinkedMap<String> t = new LongKeyLinkedMap<String>();
        if (ips != null) {
            for (int i = 0; i < ips.length; i++) {
                String ip = StringUtil.trimToEmpty(ips[i]);
                if (ip.length() == 0) {
                    continue;
                }
                String url = urls == null || i >= urls.length ? null : StringUtil.trimToEmpty(urls[i]);
                if (url == null || url.length() == 0) {
                    t.put(keyOfIp(ip), ip);
                } else {
                    t.put(keyOfIpUrl(ip, url), ip + " " + url);
                }
            }
        }
        this.blockTable = t;
        this.blockVersion = version;
        this.synced = true;
    }

    public void applyPolicy(MapValue centralValues, long version) {
        this.policy = new RejectPolicy(conf, centralValues);
        this.policyVersion = version;
        this.synced = true;
        DuplicatedIpRejectControl.getInstance().onPolicyChanged();
    }

    /** rebuild the effective policy from local config only (used on local config reload) */
    public void rebuildLocalPolicy() {
        if (policyVersion == 0) {
            this.policy = new RejectPolicy(conf, null);
            DuplicatedIpRejectControl.getInstance().onPolicyChanged();
        }
    }

    // ------------------------------------------------------------ detections

    /**
     * Record a locally detected violation so that the collector can promote it
     * to the central block list on the next sync.
     */
    public void reportDetection(String ip, String url, String reason, long now) {
        long key = url == null ? keyOfIp(ip) : keyOfIpUrl(ip, url);
        synchronized (pending) {
            if (pending.get(key) != null) {
                return; // already buffered - do not report the same violation twice
            }
            if (pending.size() >= conf.control_reject_central_max_pending_detection) {
                droppedDetectionCount++;
                return;
            }
            pending.put(key, new Detection(ip, url, reason, now));
        }
    }

    /** Take everything buffered so far. Called from the sync request handler. */
    public List<Detection> drainDetections() {
        List<Detection> out = new ArrayList<Detection>();
        synchronized (pending) {
            if (pending.size() == 0) {
                return out;
            }
            Enumeration<Detection> en = pending.values();
            while (en.hasMoreElements()) {
                out.add(en.nextElement());
            }
            pending.clear();
        }
        return out;
    }

    public long getDroppedDetectionCount() {
        return droppedDetectionCount;
    }
}
