package scouter.agent.trace;

import scouter.agent.Configure;
import scouter.lang.conf.ConfObserver;
import scouter.lang.pack.XLogDiscardTypes.XLogDiscard;
import scouter.util.KeyGen;
import scouter.util.StringUtil;
import scouter.util.matcher.CommaSeparatedChainedStrMatcher;

/**
 * Created by gunlee01@gmail.com on 2017. 7. 7.
 */
public class XLogSampler {
    private static XLogSampler instance = new XLogSampler();

    private Configure conf;
    private String currentExcludeSamplingPattern;
    private String currentConsequentSamplingIgnorePattern;
    private String currentDiscardServicePatterns;
    private String currentSamplingServicePatterns;
    private String currentSampling2ServicePatterns;
    private String currentSampling3ServicePatterns;
    private String currentSampling4ServicePatterns;
    private String currentSampling5ServicePatterns;
    private String currentFullyDiscardServicePatterns;
    private CommaSeparatedChainedStrMatcher excludeSamplingPatternMatcher;
    private CommaSeparatedChainedStrMatcher consequentSamplingIgnorePatternMatcher;
    private CommaSeparatedChainedStrMatcher discardPatternMatcher;
    private CommaSeparatedChainedStrMatcher samplingPatternMatcher;
    private CommaSeparatedChainedStrMatcher sampling2PatternMatcher;
    private CommaSeparatedChainedStrMatcher sampling3PatternMatcher;
    private CommaSeparatedChainedStrMatcher sampling4PatternMatcher;
    private CommaSeparatedChainedStrMatcher sampling5PatternMatcher;
    private CommaSeparatedChainedStrMatcher fullyDiscardPatternMatcher;

    private XLogSampler() {
        conf = Configure.getInstance();
        currentExcludeSamplingPattern = conf.xlog_sampling_exclude_patterns;
        currentConsequentSamplingIgnorePattern = conf.xlog_consequent_sampling_ignore_patterns;
        currentDiscardServicePatterns = conf.xlog_discard_service_patterns;
        currentFullyDiscardServicePatterns = conf.xlog_fully_discard_service_patterns;
        currentSamplingServicePatterns = conf.xlog_patterned_sampling_service_patterns;
        currentSampling2ServicePatterns = conf.xlog_patterned2_sampling_service_patterns;
        currentSampling3ServicePatterns = conf.xlog_patterned3_sampling_service_patterns;
        currentSampling4ServicePatterns = conf.xlog_patterned4_sampling_service_patterns;
        currentSampling5ServicePatterns = conf.xlog_patterned5_sampling_service_patterns;

        excludeSamplingPatternMatcher  = new CommaSeparatedChainedStrMatcher(currentExcludeSamplingPattern);
        consequentSamplingIgnorePatternMatcher  = new CommaSeparatedChainedStrMatcher(currentConsequentSamplingIgnorePattern);
        discardPatternMatcher = new CommaSeparatedChainedStrMatcher(currentDiscardServicePatterns);
        fullyDiscardPatternMatcher = new CommaSeparatedChainedStrMatcher(currentFullyDiscardServicePatterns);
        samplingPatternMatcher = new CommaSeparatedChainedStrMatcher(currentSamplingServicePatterns);
        sampling2PatternMatcher = new CommaSeparatedChainedStrMatcher(currentSampling2ServicePatterns);
        sampling3PatternMatcher = new CommaSeparatedChainedStrMatcher(currentSampling3ServicePatterns);
        sampling4PatternMatcher = new CommaSeparatedChainedStrMatcher(currentSampling4ServicePatterns);
        sampling5PatternMatcher = new CommaSeparatedChainedStrMatcher(currentSampling5ServicePatterns);

        ConfObserver.add("XLogSampler.StrMatch", new Runnable() {
            @Override public void run() {
                XLogSampler sampler = XLogSampler.getInstance();
                Configure conf = Configure.getInstance();
                if (sampler.currentExcludeSamplingPattern.equals(conf.xlog_sampling_exclude_patterns) == false) {
                    sampler.currentExcludeSamplingPattern = conf.xlog_sampling_exclude_patterns;
                    sampler.excludeSamplingPatternMatcher = new CommaSeparatedChainedStrMatcher(conf.xlog_sampling_exclude_patterns);
                }
                if (sampler.currentConsequentSamplingIgnorePattern.equals(conf.xlog_consequent_sampling_ignore_patterns) == false) {
                    sampler.currentConsequentSamplingIgnorePattern = conf.xlog_consequent_sampling_ignore_patterns;
                    sampler.consequentSamplingIgnorePatternMatcher = new CommaSeparatedChainedStrMatcher(conf.xlog_consequent_sampling_ignore_patterns);
                }
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
                if (sampler.currentSampling2ServicePatterns.equals(conf.xlog_patterned2_sampling_service_patterns) == false) {
                    sampler.currentSampling2ServicePatterns = conf.xlog_patterned2_sampling_service_patterns;
                    sampler.sampling2PatternMatcher = new CommaSeparatedChainedStrMatcher(conf.xlog_patterned2_sampling_service_patterns);
                }
                if (sampler.currentSampling3ServicePatterns.equals(conf.xlog_patterned3_sampling_service_patterns) == false) {
                    sampler.currentSampling3ServicePatterns = conf.xlog_patterned3_sampling_service_patterns;
                    sampler.sampling3PatternMatcher = new CommaSeparatedChainedStrMatcher(conf.xlog_patterned3_sampling_service_patterns);
                }
                if (sampler.currentSampling4ServicePatterns.equals(conf.xlog_patterned4_sampling_service_patterns) == false) {
                    sampler.currentSampling4ServicePatterns = conf.xlog_patterned4_sampling_service_patterns;
                    sampler.sampling4PatternMatcher = new CommaSeparatedChainedStrMatcher(conf.xlog_patterned4_sampling_service_patterns);
                }
                if (sampler.currentSampling5ServicePatterns.equals(conf.xlog_patterned5_sampling_service_patterns) == false) {
                    sampler.currentSampling5ServicePatterns = conf.xlog_patterned5_sampling_service_patterns;
                    sampler.sampling5PatternMatcher = new CommaSeparatedChainedStrMatcher(conf.xlog_patterned5_sampling_service_patterns);
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
            return XLogDiscard.DISCARD_ALL_FORCE;
        }

        if (conf.xlog_sampling_enabled && isExcludeSamplingServicePattern(serviceName)) {
            return XLogDiscard.NONE;
        }

        boolean isSamplingServicePattern = false;
        if (conf.xlog_patterned_sampling_enabled && (isSamplingServicePattern = isSamplingServicePattern(serviceName))) {
            discardMode = toForce(samplingPatterned1(elapsed, discardMode), serviceName);
        }
        if (!isSamplingServicePattern &&
                conf.xlog_patterned2_sampling_enabled && (isSamplingServicePattern = isSampling2ServicePattern(serviceName))) {

            discardMode = toForce(samplingPatterned2(elapsed, discardMode), serviceName);
        }
        if (!isSamplingServicePattern &&
                conf.xlog_patterned3_sampling_enabled && (isSamplingServicePattern = isSampling3ServicePattern(serviceName))) {

            discardMode = toForce(samplingPatterned3(elapsed, discardMode), serviceName);
        }
        if (!isSamplingServicePattern &&
                conf.xlog_patterned4_sampling_enabled && (isSamplingServicePattern = isSampling4ServicePattern(serviceName))) {

            discardMode = toForce(samplingPatterned4(elapsed, discardMode), serviceName);
        }
        if (!isSamplingServicePattern &&
                conf.xlog_patterned5_sampling_enabled && (isSamplingServicePattern = isSampling5ServicePattern(serviceName))) {

            discardMode = toForce(samplingPatterned5(elapsed, discardMode), serviceName);
        }

        if (!isSamplingServicePattern && conf.xlog_sampling_enabled) {
            discardMode = toForce(sampling4Elapsed(elapsed, discardMode), serviceName);
        }

        return discardMode;
    }

    private XLogDiscard toForce(XLogDiscard discardMode, String serviceName) {
        if (isConsequentSamplingIgnoreServicePattern(serviceName)) {
            return discardMode.toForce();
        } else {
            return discardMode;
        }
    }

    private XLogDiscard sampling4Elapsed(int elapsed, XLogDiscard discardMode) {
        if (elapsed < conf.xlog_sampling_step1_ms) {
            if (Math.abs(KeyGen.next() % (100 * conf.xlog_sampling_rate_precision)) >= conf.xlog_sampling_step1_rate_pct) {
                discardMode = conf.xlog_sampling_only_profile ? XLogDiscard.DISCARD_PROFILE : XLogDiscard.DISCARD_ALL;
            }
        } else if (elapsed < conf.xlog_sampling_step2_ms) {
            if (Math.abs(KeyGen.next() % (100 * conf.xlog_sampling_rate_precision)) >= conf.xlog_sampling_step2_rate_pct) {
                discardMode = conf.xlog_sampling_only_profile ? XLogDiscard.DISCARD_PROFILE : XLogDiscard.DISCARD_ALL;
            }
        } else if (elapsed < conf.xlog_sampling_step3_ms) {
            if (Math.abs(KeyGen.next() % (100 * conf.xlog_sampling_rate_precision)) >= conf.xlog_sampling_step3_rate_pct) {
                discardMode = conf.xlog_sampling_only_profile ? XLogDiscard.DISCARD_PROFILE : XLogDiscard.DISCARD_ALL;
            }
        } else {
            if (Math.abs(KeyGen.next() % (100 * conf.xlog_sampling_rate_precision)) >= conf.xlog_sampling_over_rate_pct) {
                discardMode = conf.xlog_sampling_only_profile ? XLogDiscard.DISCARD_PROFILE : XLogDiscard.DISCARD_ALL;
            }
        }
        return discardMode;
    }

    private XLogDiscard samplingPatterned5(int elapsed, XLogDiscard discardMode) {
        if (elapsed < conf.xlog_patterned5_sampling_step1_ms) {
            if (Math.abs(KeyGen.next() % (100 * conf.xlog_patterned5_sampling_rate_precision)) >= conf.xlog_patterned5_sampling_step1_rate_pct) {
                discardMode = conf.xlog_patterned5_sampling_only_profile ? XLogDiscard.DISCARD_PROFILE : XLogDiscard.DISCARD_ALL;
            }
        } else if (elapsed < conf.xlog_patterned5_sampling_step2_ms) {
            if (Math.abs(KeyGen.next() % (100 * conf.xlog_patterned5_sampling_rate_precision)) >= conf.xlog_patterned5_sampling_step2_rate_pct) {
                discardMode = conf.xlog_patterned5_sampling_only_profile ? XLogDiscard.DISCARD_PROFILE : XLogDiscard.DISCARD_ALL;
            }
        } else if (elapsed < conf.xlog_patterned5_sampling_step3_ms) {
            if (Math.abs(KeyGen.next() % (100 * conf.xlog_patterned5_sampling_rate_precision)) >= conf.xlog_patterned5_sampling_step3_rate_pct) {
                discardMode = conf.xlog_patterned5_sampling_only_profile ? XLogDiscard.DISCARD_PROFILE : XLogDiscard.DISCARD_ALL;
            }
        } else {
            if (Math.abs(KeyGen.next() % (100 * conf.xlog_patterned5_sampling_rate_precision)) >= conf.xlog_patterned5_sampling_over_rate_pct) {
                discardMode = conf.xlog_patterned5_sampling_only_profile ? XLogDiscard.DISCARD_PROFILE : XLogDiscard.DISCARD_ALL;
            }
        }
        return discardMode;
    }

    private XLogDiscard samplingPatterned4(int elapsed, XLogDiscard discardMode) {
        if (elapsed < conf.xlog_patterned4_sampling_step1_ms) {
            if (Math.abs(KeyGen.next() % (100 * conf.xlog_patterned4_sampling_rate_precision)) >= conf.xlog_patterned4_sampling_step1_rate_pct) {
                discardMode = conf.xlog_patterned4_sampling_only_profile ? XLogDiscard.DISCARD_PROFILE : XLogDiscard.DISCARD_ALL;
            }
        } else if (elapsed < conf.xlog_patterned4_sampling_step2_ms) {
            if (Math.abs(KeyGen.next() % (100 * conf.xlog_patterned4_sampling_rate_precision)) >= conf.xlog_patterned4_sampling_step2_rate_pct) {
                discardMode = conf.xlog_patterned4_sampling_only_profile ? XLogDiscard.DISCARD_PROFILE : XLogDiscard.DISCARD_ALL;
            }
        } else if (elapsed < conf.xlog_patterned4_sampling_step3_ms) {
            if (Math.abs(KeyGen.next() % (100 * conf.xlog_patterned4_sampling_rate_precision)) >= conf.xlog_patterned4_sampling_step3_rate_pct) {
                discardMode = conf.xlog_patterned4_sampling_only_profile ? XLogDiscard.DISCARD_PROFILE : XLogDiscard.DISCARD_ALL;
            }
        } else {
            if (Math.abs(KeyGen.next() % (100 * conf.xlog_patterned4_sampling_rate_precision)) >= conf.xlog_patterned4_sampling_over_rate_pct) {
                discardMode = conf.xlog_patterned4_sampling_only_profile ? XLogDiscard.DISCARD_PROFILE : XLogDiscard.DISCARD_ALL;
            }
        }
        return discardMode;
    }

    private XLogDiscard samplingPatterned3(int elapsed, XLogDiscard discardMode) {
        if (elapsed < conf.xlog_patterned3_sampling_step1_ms) {
            if (Math.abs(KeyGen.next() % (100 * conf.xlog_patterned3_sampling_rate_precision)) >= conf.xlog_patterned3_sampling_step1_rate_pct) {
                discardMode = conf.xlog_patterned3_sampling_only_profile ? XLogDiscard.DISCARD_PROFILE : XLogDiscard.DISCARD_ALL;
            }
        } else if (elapsed < conf.xlog_patterned3_sampling_step2_ms) {
            if (Math.abs(KeyGen.next() % (100 * conf.xlog_patterned3_sampling_rate_precision)) >= conf.xlog_patterned3_sampling_step2_rate_pct) {
                discardMode = conf.xlog_patterned3_sampling_only_profile ? XLogDiscard.DISCARD_PROFILE : XLogDiscard.DISCARD_ALL;
            }
        } else if (elapsed < conf.xlog_patterned3_sampling_step3_ms) {
            if (Math.abs(KeyGen.next() % (100 * conf.xlog_patterned3_sampling_rate_precision)) >= conf.xlog_patterned3_sampling_step3_rate_pct) {
                discardMode = conf.xlog_patterned3_sampling_only_profile ? XLogDiscard.DISCARD_PROFILE : XLogDiscard.DISCARD_ALL;
            }
        } else {
            if (Math.abs(KeyGen.next() % (100 * conf.xlog_patterned3_sampling_rate_precision)) >= conf.xlog_patterned3_sampling_over_rate_pct) {
                discardMode = conf.xlog_patterned3_sampling_only_profile ? XLogDiscard.DISCARD_PROFILE : XLogDiscard.DISCARD_ALL;
            }
        }
        return discardMode;
    }

    private XLogDiscard samplingPatterned2(int elapsed, XLogDiscard discardMode) {
        if (elapsed < conf.xlog_patterned2_sampling_step1_ms) {
            if (Math.abs(KeyGen.next() % (100 * conf.xlog_patterned2_sampling_rate_precision)) >= conf.xlog_patterned2_sampling_step1_rate_pct) {
                discardMode = conf.xlog_patterned2_sampling_only_profile ? XLogDiscard.DISCARD_PROFILE : XLogDiscard.DISCARD_ALL;
            }
        } else if (elapsed < conf.xlog_patterned2_sampling_step2_ms) {
            if (Math.abs(KeyGen.next() % (100 * conf.xlog_patterned2_sampling_rate_precision)) >= conf.xlog_patterned2_sampling_step2_rate_pct) {
                discardMode = conf.xlog_patterned2_sampling_only_profile ? XLogDiscard.DISCARD_PROFILE : XLogDiscard.DISCARD_ALL;
            }
        } else if (elapsed < conf.xlog_patterned2_sampling_step3_ms) {
            if (Math.abs(KeyGen.next() % (100 * conf.xlog_patterned2_sampling_rate_precision)) >= conf.xlog_patterned2_sampling_step3_rate_pct) {
                discardMode = conf.xlog_patterned2_sampling_only_profile ? XLogDiscard.DISCARD_PROFILE : XLogDiscard.DISCARD_ALL;
            }
        } else {
            if (Math.abs(KeyGen.next() % (100 * conf.xlog_patterned2_sampling_rate_precision)) >= conf.xlog_patterned2_sampling_over_rate_pct) {
                discardMode = conf.xlog_patterned2_sampling_only_profile ? XLogDiscard.DISCARD_PROFILE : XLogDiscard.DISCARD_ALL;
            }
        }
        return discardMode;
    }

    private XLogDiscard samplingPatterned1(int elapsed, XLogDiscard discardMode) {
        if (elapsed < conf.xlog_patterned_sampling_step1_ms) {
            if (Math.abs(KeyGen.next() % (100 * conf.xlog_patterned_sampling_rate_precision)) >= conf.xlog_patterned_sampling_step1_rate_pct) {
                discardMode = conf.xlog_patterned_sampling_only_profile ? XLogDiscard.DISCARD_PROFILE : XLogDiscard.DISCARD_ALL;
            }
        } else if (elapsed < conf.xlog_patterned_sampling_step2_ms) {
            if (Math.abs(KeyGen.next() % (100 * conf.xlog_patterned_sampling_rate_precision)) >= conf.xlog_patterned_sampling_step2_rate_pct) {
                discardMode = conf.xlog_patterned_sampling_only_profile ? XLogDiscard.DISCARD_PROFILE : XLogDiscard.DISCARD_ALL;
            }
        } else if (elapsed < conf.xlog_patterned_sampling_step3_ms) {
            if (Math.abs(KeyGen.next() % (100 * conf.xlog_patterned_sampling_rate_precision)) >= conf.xlog_patterned_sampling_step3_rate_pct) {
                discardMode = conf.xlog_patterned_sampling_only_profile ? XLogDiscard.DISCARD_PROFILE : XLogDiscard.DISCARD_ALL;
            }
        } else {
            if (Math.abs(KeyGen.next() % (100 * conf.xlog_patterned_sampling_rate_precision)) >= conf.xlog_patterned_sampling_over_rate_pct) {
                discardMode = conf.xlog_patterned_sampling_only_profile ? XLogDiscard.DISCARD_PROFILE : XLogDiscard.DISCARD_ALL;
            }
        }
        return discardMode;
    }

    private boolean isExcludeSamplingServicePattern(String serviceName) {
        if (StringUtil.isEmpty(conf.xlog_sampling_exclude_patterns)) {
            return false;
        }
        if (excludeSamplingPatternMatcher.isMatch(serviceName)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isConsequentSamplingIgnoreServicePattern(String serviceName) {
        if (StringUtil.isEmpty(conf.xlog_consequent_sampling_ignore_patterns)) {
            return false;
        }
        if (consequentSamplingIgnorePatternMatcher.isMatch(serviceName)) {
            return true;
        } else {
            return false;
        }
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

    private boolean isSampling2ServicePattern(String serviceName) {
        if (StringUtil.isEmpty(conf.xlog_patterned2_sampling_service_patterns)) {
            return false;
        }
        if (sampling2PatternMatcher.isMatch(serviceName)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isSampling3ServicePattern(String serviceName) {
        if (StringUtil.isEmpty(conf.xlog_patterned3_sampling_service_patterns)) {
            return false;
        }
        if (sampling3PatternMatcher.isMatch(serviceName)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isSampling4ServicePattern(String serviceName) {
        if (StringUtil.isEmpty(conf.xlog_patterned4_sampling_service_patterns)) {
            return false;
        }
        if (sampling4PatternMatcher.isMatch(serviceName)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isSampling5ServicePattern(String serviceName) {
        if (StringUtil.isEmpty(conf.xlog_patterned5_sampling_service_patterns)) {
            return false;
        }
        if (sampling5PatternMatcher.isMatch(serviceName)) {
            return true;
        } else {
            return false;
        }
    }
}
