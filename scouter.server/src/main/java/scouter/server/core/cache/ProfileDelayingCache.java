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

import scouter.lang.pack.XLogProfilePack2;
import scouter.server.Configure;
import scouter.server.Logger;
import scouter.server.core.ProfileDelayingsRecoverCore;
import scouter.util.LongKeyLinkedMap;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Keep xlogs on memory for consequent xlog sampling.
 * all child xlogs follow commencement service's discarding option.
 * <p>
 * Created by Gun Lee(gunlee01@gmail.com) on 02/05/2020
 */
public final class ProfileDelayingCache {
	public static ProfileDelayingCache instance = new ProfileDelayingCache();
	private static final int BUCKET_MILLIS = 500;

	private Configure conf = Configure.getInstance();
	private int lastMaxSize = 0;
	private long lastTime = System.currentTimeMillis() / BUCKET_MILLIS;

	private final ReferenceQueue<LongKeyLinkedMap<List<XLogProfilePack2>>> referenceQueue;

	private LongKeyLinkedMap<List<XLogProfilePack2>> gxidProfiles;
	private final LinkedList<SoftReference<LongKeyLinkedMap<List<XLogProfilePack2>>>> gxidProfilesOld;

	private ProfileDelayingCache() {
		this.gxidProfiles = newMap();
		this.gxidProfilesOld = new LinkedList<>();
		this.referenceQueue = new ReferenceQueue<>();

		try {
			ReferenceQueueHandler referenceQueueHandler = new ReferenceQueueHandler(referenceQueue);
			referenceQueueHandler.start();
		} catch (Exception e) {
			Logger.printStackTrace(e);
		}
	}

	static class ReferenceQueueHandler extends Thread {
		private final ReferenceQueue<LongKeyLinkedMap<List<XLogProfilePack2>>> referenceQueue;

		ReferenceQueueHandler(ReferenceQueue<LongKeyLinkedMap<List<XLogProfilePack2>>> referenceQueue) {
			this.referenceQueue = referenceQueue;
			this.setName("SCOUTER-ProfileDelayingCache-ReferenceQueueHandler");
			this.setDaemon(true);
		}

		@Override
		public void run() {
			try {
				while (true) {
					try {
						Reference<? extends LongKeyLinkedMap<List<XLogProfilePack2>>> reference = referenceQueue.remove();
						Logger.println("S0101", "Consider heap increase, ProfileDelayingCache purged by GC, reference=" + reference);

					} catch (InterruptedException e) {
						Logger.println("S0102", "ProfileDelayingCache ReferenceQueueHandler interrupted!");
					}
				}

			} catch (Throwable ex) {
				Logger.printStackTrace(ex);
			}
		}
	}

	public void addDelaying(XLogProfilePack2 profilePack) {
		if (profilePack.discardType == 0) { //in case of old version. (no discard type)
			return;
		}
		if (lastTime != System.currentTimeMillis() / BUCKET_MILLIS) {
			lastTime = System.currentTimeMillis() / BUCKET_MILLIS;
			lastMaxSize = Math.max(lastMaxSize, gxidProfiles.size());

			int maxBucketCount = conf.xlog_sampling_matcher_profile_keep_memory_secs * 1000 / BUCKET_MILLIS;
			if (gxidProfilesOld.size() >= maxBucketCount) {
				for (int inx = gxidProfilesOld.size(); inx >= maxBucketCount; inx--) {
					SoftReference<LongKeyLinkedMap<List<XLogProfilePack2>>> packsRef = gxidProfilesOld.removeFirst();
					if (packsRef == null) {
						continue;
					}
					LongKeyLinkedMap<List<XLogProfilePack2>> profilesInOld = packsRef.get();
					if (profilesInOld == null) {
						continue;
					}
					ProfileDelayingsRecoverCore.add(profilesInOld);
				}
			}

			gxidProfilesOld.addLast(new SoftReference<>(gxidProfiles, referenceQueue));
			gxidProfiles = newMap();
		}

		List<XLogProfilePack2> profilePacks = gxidProfiles.get(profilePack.gxid);
		if (profilePacks == null) {
			profilePacks = new ArrayList<>();
			gxidProfiles.put(profilePack.gxid, profilePacks);
		}
		profilePacks.add(profilePack);
	}

	public void removeDelayingChildren(XLogProfilePack2 drivingPack) {
		if (drivingPack.isDriving()) {
			gxidProfiles.remove(drivingPack.txid);
			for (SoftReference<LongKeyLinkedMap<List<XLogProfilePack2>>> profilesRef : gxidProfilesOld) {
				LongKeyLinkedMap<List<XLogProfilePack2>> profilesInOld = profilesRef.get();
				if (profilesInOld == null) {
					continue;
				}
				profilesInOld.remove(drivingPack.txid);
			}
		}
	}

	public List<XLogProfilePack2> popDelayingChildren(XLogProfilePack2 drivingPack) {
		if (!drivingPack.isDriving()) {
			return Collections.emptyList();
		}
		List<XLogProfilePack2> children = gxidProfiles.get(drivingPack.txid);
		if (children != null) {
			gxidProfiles.remove(drivingPack.txid);
		} else {
			children = new ArrayList<>();
		}

		for (SoftReference<LongKeyLinkedMap<List<XLogProfilePack2>>> profilesRef : gxidProfilesOld) {
			LongKeyLinkedMap<List<XLogProfilePack2>> profilesInOld = profilesRef.get();
			if (profilesInOld == null) {
				continue;
			}
			List<XLogProfilePack2> childrenInOld = profilesInOld.get(drivingPack.txid);
			if (childrenInOld != null) {
				profilesInOld.remove(drivingPack.txid);
				children.addAll(childrenInOld);
			}
		}
		return children;
	}

	private LongKeyLinkedMap<List<XLogProfilePack2>> newMap() {
		int initSize = 0;
		if (lastMaxSize == 0) {
			initSize = 5 * 1000;
		} else {
			initSize = (int) (lastMaxSize * 1.5f);
		}
		LongKeyLinkedMap<List<XLogProfilePack2>> map = new LongKeyLinkedMap<>(initSize, 0.75f);
		map.setMax(conf.xlog_sampling_matcher_profile_keep_memory_count);
		return map;
	}
}
