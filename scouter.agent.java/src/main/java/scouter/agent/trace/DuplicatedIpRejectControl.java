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
import scouter.agent.Logger;
import scouter.lang.AlertLevel;
import scouter.lang.conf.ConfObserver;
import scouter.util.LongKeyLinkedMap;
import scouter.util.StringSet;
import scouter.util.StringUtil;
import scouter.util.matcher.CommaSeparatedChainedStrMatcher;

/**
 * Blocks duplicated requests coming from the same client ip.
 *
 * Three independent rules are applied (any combination can be turned on) :
 *
 * 1. CONCURRENT rule - the same (ip, service) pair is already being processed.
 *                      useful for the classic 'double click / F5 hammering' case.
 * 2. INTERVAL rule   - the same (ip, service) pair was requested within
 *                      {@code control_reject_duplicated_ip_interval_ms}.
 * 3. RATE rule       - the same (ip, service) pair was called
 *                      {@code control_reject_duplicated_ip_rate_max_count} times or more
 *                      within {@code control_reject_duplicated_ip_rate_window_ms}.
 *                      the window is a fixed window starting at the first call.
 *
 * The state table is an LRU map so that memory usage is bounded regardless of
 * how many client ip's hit the server.
 *
 * @see scouter.agent.trace.TraceMain#reject(Object, Object, Object)
 */
public class DuplicatedIpRejectControl {

    private static final DuplicatedIpRejectControl instance = new DuplicatedIpRejectControl();

    public static DuplicatedIpRejectControl getInstance() {
        return instance;
    }

    static class Entry {
        long lastAccessTime;
        int inflight;
        long inflightStartTime;
        /** fixed counting window for the call-rate rule */
        long windowStartTime;
        int windowCount;
        /** sticky block deadline set when the call-rate is exceeded */
        long blockedUntil;
    }

    private final Configure conf = Configure.getInstance();

    /** key(ip+service) -> state. LRU bounded. */
    private final LongKeyLinkedMap<Entry> table = new LongKeyLinkedMap<Entry>();

    private volatile CommaSeparatedChainedStrMatcher excludeUrlMatcher;
    private volatile StringSet excludeIpSet = new StringSet();

    private String currentExcludeUrlPatterns = null;
    private String currentExcludeIps = null;

    private final CentralRejectControl central = CentralRejectControl.getInstance();

    private long blockedCount = 0;

    private DuplicatedIpRejectControl() {
        onPolicyChanged();

        ConfObserver.add("DuplicatedIpRejectControl", new Runnable() {
            public void run() {
                // local config changed - only takes effect for keys the collector has not overridden
                CentralRejectControl.getInstance().rebuildLocalPolicy();
                onPolicyChanged();
            }
        });
    }

    /**
     * Rebuild the derived structures. Called on local config reload and whenever the
     * collector pushes a new central policy.
     */
    void onPolicyChanged() {
        RejectPolicy p = central.getPolicy();
        reconfigure(p);
        table.setMax(p.maxKeepCount);
    }

    private synchronized void reconfigure(RejectPolicy p) {
        String urlPatterns = p.excludeUrlPatterns;
        if (currentExcludeUrlPatterns == null || !currentExcludeUrlPatterns.equals(urlPatterns)) {
            currentExcludeUrlPatterns = urlPatterns;
            excludeUrlMatcher = urlPatterns.length() == 0 ? null : new CommaSeparatedChainedStrMatcher(urlPatterns);
        }

        String ips = p.excludeIps;
        if (currentExcludeIps == null || !currentExcludeIps.equals(ips)) {
            currentExcludeIps = ips;
            StringSet set = new StringSet();
            String[] arr = StringUtil.split(ips, ',');
            if (arr != null) {
                for (int i = 0; i < arr.length; i++) {
                    String x = StringUtil.trimToEmpty(arr[i]);
                    if (x.length() > 0) {
                        set.put(x);
                    }
                }
            }
            excludeIpSet = set;
        }
    }

    private long makeKey(RejectPolicy p, String ip, String serviceName) {
        if (p.urlEnabled && serviceName != null) {
            return CentralRejectControl.keyOfIpUrl(ip, serviceName);
        }
        return CentralRejectControl.keyOfIp(ip);
    }

    private boolean isExcluded(String ip, String serviceName) {
        if (excludeIpSet.hasKey(ip)) {
            return true;
        }
        if (excludeUrlMatcher != null && serviceName != null && excludeUrlMatcher.isMatch(serviceName)) {
            return true;
        }
        return false;
    }

    /**
     * Decide whether this request must be rejected, and mark it as in-flight when accepted.
     * Must be paired with {@link #release(TraceContext)}.
     *
     * @return true if the request should be rejected.
     */
    public boolean isDuplicated(TraceContext ctx) {
        if (ctx == null) {
            return false;
        }
        RejectPolicy p = central.getPolicy();
        if (!p.enabled) {
            return false;
        }
        String ip = ctx.remoteIp;
        if (StringUtil.isEmpty(ip)) {
            return false;
        }
        String serviceName = ctx.serviceName;
        if (isExcluded(ip, serviceName)) {
            return false;
        }

        // rule 0 : centrally managed block list. released only by an operator.
        if (central.isBlocked(ip, serviceName)) {
            blockedCount++;
            alert(p, ip, serviceName, "CENTRAL");
            return true;
        }

        long key = makeKey(p, ip, serviceName);
        long now = System.currentTimeMillis();
        boolean rejected = false;
        String reason = null;

        synchronized (table) {
            Entry e = table.get(key);
            if (e == null) {
                e = new Entry();
                e.windowStartTime = now;
                table.putLast(key, e);
            }

            // roll the counting window. counted on every arrival, including rejected ones,
            // so that the window reflects the real inbound call rate.
            if (p.rateEnabled) {
                if (e.windowStartTime == 0 || now - e.windowStartTime >= p.rateWindowMs) {
                    e.windowStartTime = now;
                    e.windowCount = 0;
                }
                if (e.windowCount < Integer.MAX_VALUE) {
                    e.windowCount++;
                }
            }

            // recover leaked in-flight counts (ex. a request that never reached endHttpServiceFinal)
            if (e.inflight > 0 && now - e.inflightStartTime > p.inflightTimeoutMs) {
                e.inflight = 0;
            }

            // rule C-1 : still inside the sticky block period of a previous rate violation
            if (e.blockedUntil > now) {
                rejected = true;
                reason = "RATE";
            }

            // rule A : the same request is still being processed
            if (!rejected && p.concurrentEnabled && e.inflight >= p.maxConcurrentCount) {
                rejected = true;
                reason = "CONCURRENT";
            }

            // rule B : the same request was re-called too soon
            if (!rejected && p.intervalMs > 0 && e.lastAccessTime > 0
                    && now - e.lastAccessTime < p.intervalMs) {
                rejected = true;
                reason = "INTERVAL";
            }

            // rule C-2 : too many calls within the counting window
            if (!rejected && p.rateEnabled && e.windowCount >= p.rateMaxCount) {
                rejected = true;
                reason = "RATE";
                if (p.rateBlockMs > 0) {
                    e.blockedUntil = now + p.rateBlockMs;
                }
            }

            if (!rejected) {
                e.lastAccessTime = now;
                if (e.inflight == 0) {
                    e.inflightStartTime = now;
                }
                e.inflight++;
                // refresh LRU position so that busy clients are not evicted first
                table.putLast(key, e);
                ctx.duplicatedIpCheckKey = key;
                ctx.duplicatedIpCheckMarked = true;
            } else {
                blockedCount++;
            }
        }

        if (rejected) {
            // hand the violation to the collector so it can be promoted to the
            // central block list. deduplicated inside, so hammering costs nothing.
            if (conf.control_reject_central_enabled) {
                central.reportDetection(ip, p.urlEnabled ? serviceName : null, reason, now);
            }
            alert(p, ip, serviceName, reason);
        }
        return rejected;
    }

    /**
     * Release the in-flight mark. Safe to call for every context - it is a no-op
     * unless the context was actually marked by {@link #isDuplicated(TraceContext)}.
     */
    public void release(TraceContext ctx) {
        if (ctx == null || !ctx.duplicatedIpCheckMarked) {
            return;
        }
        ctx.duplicatedIpCheckMarked = false;
        synchronized (table) {
            Entry e = table.get(ctx.duplicatedIpCheckKey);
            if (e != null && e.inflight > 0) {
                e.inflight--;
            }
        }
    }

    private void alert(RejectPolicy p, String ip, String serviceName, String reason) {
        if (!p.alertEnabled) {
            return;
        }
        try {
            // AlertProxy itself suppresses duplicated titles within alert_send_interval_ms
            AlertProxy.sendAlert(AlertLevel.WARN, "REJECT_DUPLICATED_IP",
                    "rejected by " + reason + ". ip=" + ip + ", service=" + serviceName);
        } catch (Throwable t) {
            Logger.println("A911", "fail to send duplicated-ip alert", t);
        }
    }

    public long getBlockedCount() {
        return blockedCount;
    }

    public int getTableSize() {
        return table.size();
    }

    public void clear() {
        synchronized (table) {
            table.clear();
        }
    }
}
