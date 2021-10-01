package scouter.agent.util;
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Gun Lee(gunlee01@gmail.com) on 2021/06/09
 */
public class SimpleLru<K, V> {
	private final Map<K, V> accessCache;
	private final Map<K, V> creationCache;


	final int maxSize;
	public SimpleLru(final int maxSize) {
		this.maxSize = maxSize;
		this.accessCache = new ConcurrentHashMap<K, V>(maxSize);
		this.creationCache =
				new LinkedHashMap<K, V>(maxSize, 0.75f) {
					@Override
					protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
						if (size() > maxSize) {
							accessCache.remove(eldest.getKey());
							return true;
						}
						else {
							return false;
						}
					}
				};
	}

	public void put(K key, V value) {
		this.accessCache.put(key, value);
		synchronized (this.creationCache) {
			this.creationCache.put(key, value);
		}
	}

	public void remove(K key) {
		this.accessCache.remove(key);
		synchronized (this.creationCache) {
			this.creationCache.remove(key);
		}
	}

	public V get(K key) {
		return this.accessCache.get(key);
	}

	public int size() {
		return this.accessCache.size();
	}

	public Set<Map.Entry<K, V>> entrySet() {
		return this.accessCache.entrySet();
	}
}
