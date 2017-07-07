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

	public static XLogDiscard evaluateXLogDiscard(int elapsed, String serviceName) {
		XLogDiscard discardMode = XLogDiscard.DISCARD_NONE;

		if(elapsed < conf.xlog_lower_bound_time_ms) {
			return XLogDiscard.DISCARD_ALL;
		}

		if (isDiscardServicePattern(serviceName)) {
			return XLogDiscard.DISCARD_ALL;
		}

		if(conf.xlog_sampling_enabled) {
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
		String patterns = conf.xlog_discard_service_patterns;
		if(StringUtil.isEmpty(patterns)) {
			return false;
		}
		StrMatch match = new StrMatch(patterns);
		if (match.include(serviceName)) {
			return true;
		} else {
			return false;
		}
	}
}
