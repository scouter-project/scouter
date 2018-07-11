package scouter.agent.trace;

import scouter.agent.Configure;
import scouter.agent.trace.enums.XLogDiscard;
import scouter.lang.conf.ConfObserver;
import scouter.util.KeyGen;
import scouter.util.StringUtil;
import scouter.util.matcher.CommaSeparatedChainedStrMatcher;

/**
 * Created by gunlee01@gmail.com on 2017. 7. 7.
 */
public class XLogSampler {
    private static XLogSampler instance = new XLogSampler();

    private Configure conf;
    private String currentDiscardServicePatterns;
    private String currentSamplingServicePatterns;
    private String currentFullyDiscardServicePatterns;
    private CommaSeparatedChainedStrMatcher discardPatternMatcher;
    private CommaSeparatedChainedStrMatcher samplingPatternMatcher;
    private CommaSeparatedChainedStrMatcher fullyDiscardPatternMatcher;

    private XLogSampler() {
        conf = Configure.getInstance();
        currentDiscardServicePatterns = conf.xlog_discard_service_patterns;
        currentFullyDiscardServicePatterns = conf.xlog_fully_discard_service_patterns;
        currentSamplingServicePatterns = conf.xlog_patterned_sampling_service_patterns;
        discardPatternMatcher = new CommaSeparatedChainedStrMatcher(currentDiscardServicePatterns);
        fullyDiscardPatternMatcher = new CommaSeparatedChainedStrMatcher(currentFullyDiscardServicePatterns);
        samplingPatternMatcher = new CommaSeparatedChainedStrMatcher(currentSamplingServicePatterns);

        ConfObserver.add("XLogSampler.StrMatch", new Runnable() {
            @Override public void run() {
                XLogSampler sampler = XLogSampler.getInstance();
                Configure conf = Configure.getInstance();
                if (sampler.currentDiscardServicePatterns.equals(conf.xlog_discard_service_patterns) == false) {
                    sampler.currentDiscardServicePatterns = conf.xlog_discard_service_patterns;
                    sampler.discardPatternMatcher = new CommaSeparatedChainedStrMatcher(conf.xlog_discard_service_patterns);
                }
                if (sampler.currentFullyDiscardServicePatterns.equals(conf.xlog_fully_discard_service_patterns) == false) {
                    sampler.currentFullyDiscardServicePatterns = conf.xlog_fully_discard_service_patterns;
                    sampler.fullyDiscardPatternMatcher = new CommaSeparatedChainedStrMatcher(conf.xlog_fully_discard_service_patterns);
                }
                if (sampler.currentSamplingServicePatterns.equals(conf.xlog_patterned_sampling_service_patterns) == false) {
                    sampler.currentSamplingServicePatterns = conf.xlog_patterned_sampling_service_patterns;
                    sampler.samplingPatternMatcher = new CommaSeparatedChainedStrMatcher(conf.xlog_patterned_sampling_service_patterns);
                }
            }
        });
    }

    public static XLogSampler getInstance() {
        return instance;
    }

    public XLogDiscard evaluateXLogDiscard(int elapsed, String serviceName) {
        XLogDiscard discardMode = XLogDiscard.NONE;

        if (elapsed < conf.xlog_lower_bound_time_ms) {
            return XLogDiscard.DISCARD_ALL;
        }

        boolean isSamplingServicePattern = false;
        if (conf.xlog_patterned_sampling_enabled && (isSamplingServicePattern = isSamplingServicePattern(serviceName))) {
            if (elapsed < conf.xlog_patterned_sampling_step1_ms) {
                if (Math.abs(KeyGen.next() % 100) >= conf.xlog_patterned_sampling_step1_rate_pct) {
                    discardMode = conf.xlog_patterned_sampling_only_profile ? XLogDiscard.DISCARD_PROFILE : XLogDiscard.DISCARD_ALL;
                }
            } else if (elapsed < conf.xlog_patterned_sampling_step2_ms) {
                if (Math.abs(KeyGen.next() % 100) >= conf.xlog_patterned_sampling_step2_rate_pct) {
                    discardMode = conf.xlog_patterned_sampling_only_profile ? XLogDiscard.DISCARD_PROFILE : XLogDiscard.DISCARD_ALL;
                }
            } else if (elapsed < conf.xlog_patterned_sampling_step3_ms) {
                if (Math.abs(KeyGen.next() % 100) >= conf.xlog_patterned_sampling_step3_rate_pct) {
                    discardMode = conf.xlog_patterned_sampling_only_profile ? XLogDiscard.DISCARD_PROFILE : XLogDiscard.DISCARD_ALL;
                }
            } else {
                if (Math.abs(KeyGen.next() % 100) >= conf.xlog_patterned_sampling_over_rate_pct) {
                    discardMode = conf.xlog_patterned_sampling_only_profile ? XLogDiscard.DISCARD_PROFILE : XLogDiscard.DISCARD_ALL;
                }
            }
        }

        if (!isSamplingServicePattern && conf.xlog_sampling_enabled) {
            if (elapsed < conf.xlog_sampling_step1_ms) {
                if (Math.abs(KeyGen.next() % 100) >= conf.xlog_sampling_step1_rate_pct) {
                    discardMode = conf.xlog_sampling_only_profile ? XLogDiscard.DISCARD_PROFILE : XLogDiscard.DISCARD_ALL;
                }
            } else if (elapsed < conf.xlog_sampling_step2_ms) {
                if (Math.abs(KeyGen.next() % 100) >= conf.xlog_sampling_step2_rate_pct) {
                    discardMode = conf.xlog_sampling_only_profile ? XLogDiscard.DISCARD_PROFILE : XLogDiscard.DISCARD_ALL;
                }
            } else if (elapsed < conf.xlog_sampling_step3_ms) {
                if (Math.abs(KeyGen.next() % 100) >= conf.xlog_sampling_step3_rate_pct) {
                    discardMode = conf.xlog_sampling_only_profile ? XLogDiscard.DISCARD_PROFILE : XLogDiscard.DISCARD_ALL;
                }
            } else {
                if (Math.abs(KeyGen.next() % 100) >= conf.xlog_sampling_over_rate_pct) {
                    discardMode = conf.xlog_sampling_only_profile ? XLogDiscard.DISCARD_PROFILE : XLogDiscard.DISCARD_ALL;
                }
            }
        }
        return discardMode;
    }

    public boolean isDiscardServicePattern(String serviceName) {
        if (StringUtil.isEmpty(conf.xlog_discard_service_patterns)) {
            return false;
        }
        if (discardPatternMatcher.isMatch(serviceName)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isFullyDiscardServicePattern(String serviceName) {
        if (StringUtil.isEmpty(conf.xlog_fully_discard_service_patterns)) {
            return false;
        }
        if (fullyDiscardPatternMatcher.isMatch(serviceName)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isSamplingServicePattern(String serviceName) {
        if (StringUtil.isEmpty(conf.xlog_patterned_sampling_service_patterns)) {
            return false;
        }
        if (samplingPatternMatcher.isMatch(serviceName)) {
            return true;
        } else {
            return false;
        }
    }
}
