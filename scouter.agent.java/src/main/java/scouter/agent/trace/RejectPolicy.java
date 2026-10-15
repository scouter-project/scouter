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
import scouter.util.StringUtil;

/**
 * Immutable snapshot of the effective duplicated-ip reject policy.
 *
 * The values are resolved once (when local config reloads, or when the collector
 * pushes a central policy) and then read as plain fields on the request path,
 * so per-request cost stays at zero map lookups.
 *
 * Precedence : central policy pushed by the collector &gt; local agent config.
 * A central key that is absent falls back to the local value, so operators can
 * centralize only the keys they care about.
 */
public class RejectPolicy {

    /** policy keys used over the wire (without the 'control_reject_duplicated_ip_' prefix) */
    public static final String KEY_ENABLED = "enabled";
    public static final String KEY_URL_ENABLED = "url_enabled";
    public static final String KEY_INTERVAL_MS = "interval_ms";
    public static final String KEY_CONCURRENT_ENABLED = "concurrent_enabled";
    public static final String KEY_MAX_CONCURRENT_COUNT = "max_concurrent_count";
    public static final String KEY_RATE_ENABLED = "rate_enabled";
    public static final String KEY_RATE_WINDOW_MS = "rate_window_ms";
    public static final String KEY_RATE_MAX_COUNT = "rate_max_count";
    public static final String KEY_RATE_BLOCK_MS = "rate_block_ms";
    public static final String KEY_INFLIGHT_TIMEOUT_MS = "inflight_timeout_ms";
    public static final String KEY_MAX_KEEP_COUNT = "max_keep_count";
    public static final String KEY_EXCLUDE_URL_PATTERNS = "exclude_url_patterns";
    public static final String KEY_EXCLUDE_IPS = "exclude_ips";
    public static final String KEY_TEXT = "text";
    public static final String KEY_REDIRECT_URL_ENABLED = "redirect_url_enabled";
    public static final String KEY_REDIRECT_URL = "redirect_url";
    public static final String KEY_ALERT_ENABLED = "alert_enabled";

    public final boolean enabled;
    public final boolean urlEnabled;
    public final int intervalMs;
    public final boolean concurrentEnabled;
    public final int maxConcurrentCount;
    public final boolean rateEnabled;
    public final int rateWindowMs;
    public final int rateMaxCount;
    public final int rateBlockMs;
    public final int inflightTimeoutMs;
    public final int maxKeepCount;
    public final String excludeUrlPatterns;
    public final String excludeIps;
    public final String text;
    public final boolean redirectUrlEnabled;
    public final String redirectUrl;
    public final boolean alertEnabled;

    /** true when at least one value came from the collector */
    public final boolean central;

    /**
     * @param central overrides pushed by the collector. may be null.
     */
    public RejectPolicy(Configure conf, MapValue central) {
        boolean fromCentral = central != null && central.size() > 0;
        this.central = fromCentral;

        this.enabled = bool(central, KEY_ENABLED, conf.control_reject_duplicated_ip_enabled);
        this.urlEnabled = bool(central, KEY_URL_ENABLED, conf.control_reject_duplicated_ip_url_enabled);
        this.intervalMs = num(central, KEY_INTERVAL_MS, conf.control_reject_duplicated_ip_interval_ms);
        this.concurrentEnabled = bool(central, KEY_CONCURRENT_ENABLED,
                conf.control_reject_duplicated_ip_concurrent_enabled);
        this.maxConcurrentCount = atLeast(1,
                num(central, KEY_MAX_CONCURRENT_COUNT, conf.control_reject_duplicated_ip_max_concurrent_count));
        this.rateEnabled = bool(central, KEY_RATE_ENABLED, conf.control_reject_duplicated_ip_rate_enabled);
        this.rateWindowMs = atLeast(1,
                num(central, KEY_RATE_WINDOW_MS, conf.control_reject_duplicated_ip_rate_window_ms));
        this.rateMaxCount = atLeast(1,
                num(central, KEY_RATE_MAX_COUNT, conf.control_reject_duplicated_ip_rate_max_count));
        this.rateBlockMs = num(central, KEY_RATE_BLOCK_MS, conf.control_reject_duplicated_ip_rate_block_ms);
        this.inflightTimeoutMs = num(central, KEY_INFLIGHT_TIMEOUT_MS,
                conf.control_reject_duplicated_ip_inflight_timeout_ms);
        this.maxKeepCount = atLeast(100,
                num(central, KEY_MAX_KEEP_COUNT, conf.control_reject_duplicated_ip_max_keep_count));
        this.excludeUrlPatterns = StringUtil.trimToEmpty(
                str(central, KEY_EXCLUDE_URL_PATTERNS, conf.control_reject_duplicated_ip_exclude_url_patterns));
        this.excludeIps = StringUtil.trimToEmpty(
                str(central, KEY_EXCLUDE_IPS, conf.control_reject_duplicated_ip_exclude_ips));
        this.text = str(central, KEY_TEXT, conf.control_reject_duplicated_ip_text);
        this.redirectUrlEnabled = bool(central, KEY_REDIRECT_URL_ENABLED,
                conf.control_reject_duplicated_ip_redirect_url_enabled);
        this.redirectUrl = str(central, KEY_REDIRECT_URL, conf.control_reject_duplicated_ip_redirect_url);
        this.alertEnabled = bool(central, KEY_ALERT_ENABLED, conf.control_reject_duplicated_ip_alert_enabled);
    }

    private static int atLeast(int min, int v) {
        return v < min ? min : v;
    }

    private static String raw(MapValue m, String key) {
        if (m == null) {
            return null;
        }
        if (!m.containsKey(key)) {
            return null;
        }
        String s = StringUtil.trimToEmpty(m.getText(key));
        return s.length() == 0 ? null : s;
    }

    private static boolean bool(MapValue m, String key, boolean def) {
        String s = raw(m, key);
        if (s == null) {
            return def;
        }
        return "true".equalsIgnoreCase(s) || "1".equals(s) || "on".equalsIgnoreCase(s);
    }

    private static int num(MapValue m, String key, int def) {
        String s = raw(m, key);
        if (s == null) {
            return def;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static String str(MapValue m, String key, String def) {
        String s = raw(m, key);
        return s == null ? def : s;
    }
}
