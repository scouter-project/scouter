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
import scouter.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//TODO test case
public class CounterProtocol extends Counter {
	private List<String> nameTags = new ArrayList<String>();
	private List<String> displayNameTags = new ArrayList<String>();

	public CounterProtocol() {}

	public CounterProtocol(String name) {
		super(name);
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

	public String getTaggingName(Map<String, String> tagMap) {
		return makeValueWithTag(this.getName(), this.nameTags, tagMap);
	}

	public String getTaggingDisplayName(Map<String, String> tagMap) {
		return makeValueWithTag(this.getDisplayName(), this.displayNameTags, tagMap);
	}

	private String makeValueWithTag(String name, List<String> tags, Map<String, String> tagMap) {
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
	public Counter toNormalCounter(Map<String, String> tagMap) {
		Counter normalCounter = this.clone();
		normalCounter.setName(makeValueWithTag(this.getName(), this.nameTags, tagMap));
		normalCounter.setDisplayName(makeValueWithTag(this.getDisplayName(), this.displayNameTags, tagMap));

		return normalCounter;
	}
}