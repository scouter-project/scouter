package scouter.agent.trace;

import scouter.agent.Configure;
import scouter.agent.trace.enums.XLogDiscard;
import scouter.util.KeyGen;
import scouter.util.StrMatch;
import scouter.util.StringUtil;

/**
 * Created by gunlee on 2017. 7. 7.
 */
public class XLogSampler {
	private static Configure conf = Configure.getInstance();
	private static StrMatch discardPatternMatch = new StrMatch(conf.xlog_discard_service_patterns);
	private static StrMatch samplingPatternMatch = new StrMatch(conf.xlog_patterned_sampling_service_patterns);

	public static void main(String[] args) {
		String a = "100";
		if((a="200").equals("100")) {
			System.out.println(100);
		} else {
			System.out.println(a);
		}

	}

	public static XLogDiscard evaluateXLogDiscard(int elapsed, String serviceName) {
		XLogDiscard discardMode = XLogDiscard.DISCARD_NONE;

		if(elapsed < conf.xlog_lower_bound_time_ms) {
			return XLogDiscard.DISCARD_ALL;
		}

		if (isDiscardServicePattern(serviceName)) {
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

		if(!isSamplingServicePattern && conf.xlog_sampling_enabled) {
			if(elapsed < conf.xlog_sampling_step1_ms) {
				if(Math.abs(KeyGen.next()%100) >= conf.xlog_sampling_step1_rate_pct) {
					discardMode = conf.xlog_sampling_only_profile ? XLogDiscard.DISCARD_PROFILE : XLogDiscard.DISCARD_ALL;
				}
			} else if(elapsed < conf.xlog_sampling_step2_ms) {
				if(Math.abs(KeyGen.next()%100) >= conf.xlog_sampling_step2_rate_pct) {
					discardMode = conf.xlog_sampling_only_profile ? XLogDiscard.DISCARD_PROFILE : XLogDiscard.DISCARD_ALL;
				}
			} else if(elapsed < conf.xlog_sampling_step3_ms) {
				if(Math.abs(KeyGen.next()%100) >= conf.xlog_sampling_step3_rate_pct) {
					discardMode = conf.xlog_sampling_only_profile ? XLogDiscard.DISCARD_PROFILE : XLogDiscard.DISCARD_ALL;
				}
			} else {
				if(Math.abs(KeyGen.next()%100) >= conf.xlog_sampling_over_rate_pct) {
					discardMode = conf.xlog_sampling_only_profile ? XLogDiscard.DISCARD_PROFILE : XLogDiscard.DISCARD_ALL;
				}
			}
		}
		return discardMode;
	}

	private static boolean isDiscardServicePattern(String serviceName) {
		if(StringUtil.isEmpty(conf.xlog_discard_service_patterns)) {
			return false;
		}
		if (discardPatternMatch.include(serviceName)) {
			return true;
		} else {
			return false;
		}
	}

	private static boolean isSamplingServicePattern(String serviceName) {
		if(StringUtil.isEmpty(conf.xlog_patterned_sampling_service_patterns)) {
			return false;
		}
		if (samplingPatternMatch.include(serviceName)) {
			return true;
		} else {
			return false;
		}
	}
}
