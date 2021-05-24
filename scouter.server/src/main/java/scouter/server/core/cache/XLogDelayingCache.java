package scouter.server.core.cache;
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

import scouter.lang.pack.XLogDiscardTypes;
import scouter.lang.pack.XLogPack;
import scouter.server.Configure;
import scouter.server.core.XLogDelayingsRecoverCore;
import scouter.util.LongIntLinkedMap;
import scouter.util.LongKeyLinkedMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Keep xlogs on memory for consequent xlog sampling.
 * all child xlogs follow commencement service's discarding option.
 *
 * Created by Gun Lee(gunlee01@gmail.com) on 02/05/2020
 */
public class XLogDelayingCache {
	public static XLogDelayingCache instance = new XLogDelayingCache();

	private Configure conf = Configure.getInstance();
	private int lastMaxSize = 0;
	private long lastTime = System.currentTimeMillis() / conf.xlog_sampling_matcher_xlog_keep_memory_millis;

	private LongIntLinkedMap processedGxidMap;
	private LongKeyLinkedMap<List<XLogPack>> gxidXLogs;
	private LongKeyLinkedMap<List<XLogPack>> gxidXLogsOld;

	public static XLogDelayingCache getInstance() {
		return instance;
	}

	private XLogDelayingCache() {
		this.processedGxidMap = new LongIntLinkedMap();
		this.processedGxidMap.setMax(conf.xlog_sampling_matcher_gxid_keep_memory_count);

		this.gxidXLogs = newMap();
		this.gxidXLogsOld = newMap();
	}

	public void addProcessed(XLogPack xLogPack) {
		if (xLogPack.isDriving()) {
			processedGxidMap.add(xLogPack.txid, xLogPack.discardType);
		}
	}

	public boolean isProcessedGxid(long gxid) {
		return processedGxidMap.containsKey(gxid);
	}

	public boolean isProcessedGxidWithoutProfile(long gxid) {
		if (XLogDiscardTypes.DISCARD_PROFILE == processedGxidMap.get(gxid)) {
			return true;
		}
		return false;
	}

	public boolean isProcessedGxidWithProfile(long gxid) {
		byte discardType = (byte) processedGxidMap.get(gxid);
		if (discardType != 0 && XLogDiscardTypes.isAliveProfile(discardType)) {
			return true;
		}
		return false;
	}

	public void addDelaying(XLogPack xLogPack) {
		if (lastTime != System.currentTimeMillis() / conf.xlog_sampling_matcher_xlog_keep_memory_millis) {
			lastTime = System.currentTimeMillis() / conf.xlog_sampling_matcher_xlog_keep_memory_millis;
			lastMaxSize = Math.max(lastMaxSize, gxidXLogs.size());
			XLogDelayingsRecoverCore.add(gxidXLogsOld);
			gxidXLogsOld = gxidXLogs;
			gxidXLogs = newMap();
		}
		List<XLogPack> xLogPacks = gxidXLogs.get(xLogPack.gxid);
		if (xLogPacks == null) {
			xLogPacks = new ArrayList<>();
			gxidXLogs.put(xLogPack.gxid, xLogPacks);
		}
		xLogPacks.add(xLogPack);
	}

	public List<XLogPack> popDelayingChildren(XLogPack drivingXLogPack) {
		if (!drivingXLogPack.isDriving()) {
			return Collections.emptyList();
		}
		List<XLogPack> children = gxidXLogs.get(drivingXLogPack.txid); //txid is gxid (in a drivingXLogPack)
		if (children != null) {
			gxidXLogs.remove(drivingXLogPack.txid);
		}

		List<XLogPack> childrenInOld = gxidXLogsOld.get(drivingXLogPack.txid);
		if (childrenInOld != null) {
			gxidXLogsOld.remove(drivingXLogPack.txid);
		}

		if (children == null && childrenInOld == null) {
			return Collections.emptyList();
		}

		if (children != null && childrenInOld != null) {
			children.addAll(childrenInOld);
			return children;
		}

		if (children != null) {
			return children;
		} else {
			return childrenInOld;
		}
	}

	public void removeDelayingChildren(XLogPack drivingXLogPack) {
		if (drivingXLogPack.isDriving()) {
			gxidXLogs.remove(drivingXLogPack.txid); //txid is gxid (in a drivingXLogPack)
			gxidXLogsOld.remove(drivingXLogPack.txid);
		}
	}

	private LongKeyLinkedMap<List<XLogPack>> newMap() {
		int initSize = 0;
		if (lastMaxSize == 0) {
			initSize = 5 * 1000;
		} else {
			initSize = (int) (lastMaxSize * 1.5f);
		}
		LongKeyLinkedMap<List<XLogPack>> map = new LongKeyLinkedMap<>(initSize, 0.75f);
		map.setMax(conf.xlog_sampling_matcher_xlog_keep_memory_count);
		return map;
	}
}
