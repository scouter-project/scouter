/*
 *  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package scouter.server.http.model;

import scouter.lang.Counter;
import scouter.lang.DeltaType;
import scouter.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//TODO test case
public class CounterProtocol extends Counter {
	private List<String> nameTags = new ArrayList<String>();
	private List<String> displayNameTags = new ArrayList<String>();
	private DeltaType deltaType = DeltaType.NONE;

	public CounterProtocol() {}

	public CounterProtocol(String name) {
		super(name);
	}

	public void setDeltaType(DeltaType deltaType) {
		this.deltaType = deltaType
		;
	}

	public DeltaType getDeltaType() {
		return deltaType;
	}

	//TODO test case
	public void setName(String nameWithTag) {
		char[] chars = nameWithTag.toCharArray();
		StringBuilder nameBuilder = new StringBuilder(chars.length);
		StringBuilder tagBuilder = new StringBuilder(chars.length);
		boolean sink = false;
		for (char c : chars) {
			switch (c) {
				case '$':
					sink = !sink;
					if (!sink) {
						nameBuilder.append('*');
						tagBuilder.append('*');
					}
					break;
				default :
					if (!sink) {
						nameBuilder.append(c);
					} else {
						tagBuilder.append(c);
					}
			}
		}
		String name = nameBuilder.toString();
		super.setName(name);
		if (tagBuilder.length() > 0) {
			this.nameTags = StringUtil.splitAsList(tagBuilder.toString(), '*');
		}
	}

	//TODO test case
	public void setDisplayName(String displayNameWithTag) {
		char[] chars = displayNameWithTag.toCharArray();
		StringBuilder nameBuilder = new StringBuilder(chars.length);
		StringBuilder tagBuilder = new StringBuilder(chars.length);
		boolean sink = false;
		for (char c : chars) {
			switch (c) {
				case '$':
					sink = !sink;
					if (!sink) {
						nameBuilder.append('*');
						tagBuilder.append('*');
					}
					break;
				default :
					if (!sink) {
						nameBuilder.append(c);
					} else {
						tagBuilder.append(c);
					}
			}
		}
		String displayName = nameBuilder.toString();
		super.setDisplayName(displayName);
		if (tagBuilder.length() > 0) {
			this.displayNameTags = StringUtil.splitAsList(tagBuilder.toString(), '*');
		}
	}

	public String getTaggedName(Map<String, String> tagMap) {
		return generateTaggedName(this.getName(), this.nameTags, tagMap);
	}

	public String getTaggedDelataName(Map<String, String> tagMap) {
		return generateTaggedDeltaName(this.getName(), this.nameTags, tagMap);
	}

	public String getTaggedDisplayName(Map<String, String> tagMap) {
		return generateTaggedName(this.getDisplayName(), this.displayNameTags, tagMap);
	}

	private String generateTaggedDeltaName(String name, List<String> tags, Map<String, String> tagMap) {
		return generateTaggedName(name, tags, tagMap) + "_$delta";
	}

	private String generateTaggedName(String name, List<String> tags, Map<String, String> tagMap) {
		if (tags == null || tags.size() == 0) {
			return name;
		}
		if (name.indexOf('*') < 0) {
			return name;
		}
		StringBuilder sb = new StringBuilder(name.length());
		int tagCounter = 0;
		for (char c : name.toCharArray()) {
			switch (c) {
				case '*':
					sb.append(tagMap.get(tags.get(tagCounter++)));
					break;
				default :
					sb.append(c);
			}
		}
		return sb.toString();
	}

	//TODO test case
	public List<Counter> toCounters(Map<String, String> tagMap) {
		List<Counter> counters = new ArrayList<Counter>();

		if (hasNormalCounter()) {
			counters.add(toNormalCounter(tagMap));
		}

		if (hasDeltaCounter()) {
			counters.add(toDeltaCounter(tagMap));
		}
		return counters;
	}

	public Counter toNormalCounter(Map<String, String> tagMap) {
		if (!hasNormalCounter()) {
			return null;
		}
		Counter normalCounter = this.clone();
		normalCounter.setName(generateTaggedName(this.getName(), this.nameTags, tagMap));
		normalCounter.setDisplayName(generateTaggedName(this.getDisplayName(), this.displayNameTags, tagMap));

		return normalCounter;
	}

	public Counter toDeltaCounter(Map<String, String> tagMap) {
		if (!hasDeltaCounter()) {
			return null;
		}
		Counter deltaCounter = this.clone();
		deltaCounter.setName(generateTaggedDeltaName(this.getName(), this.nameTags, tagMap));
		deltaCounter.setDisplayName(generateTaggedDeltaName(this.getDisplayName(), this.displayNameTags, tagMap));
		deltaCounter.setUnit(deltaCounter.getUnit() + "/s");

		return deltaCounter;
	}

	public boolean hasDeltaCounter() {
		return (this.getDeltaType() == DeltaType.DELTA || this.getDeltaType() == DeltaType.BOTH);
	}

	public boolean hasNormalCounter() {
		return (this.getDeltaType() != DeltaType.DELTA);
	}
}