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

package scouter.server;

import scouter.server.http.model.CounterProtocol;
import scouter.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2018. 7. 22.
 */
public class ScouterTgMtConfig {
    public static final String X = "X$";

    private String measurement;

    private boolean enabled = true;
    private boolean debugEnabled = false;

    private Map<String, CounterProtocol> counterMapping = new HashMap<String, CounterProtocol>();

    private String objFamilyBase = X;
    private List<String> objFamilyAppendTags = new ArrayList<String>();

    private String objTypeBase = X;
    private List<String> objTypePrependTags = new ArrayList<String>();
    private List<String> objTypeAppendTags = new ArrayList<String>();
    private String objTypeIcon = "";

    private String objNameBase = X;
    private List<String> objNameAppendTags = new ArrayList<String>();

    private String hostTag = "host";
    private Map<String, String> hostMapping = new HashMap<String, String>();
    private Map<String, List<String>> tagFilter = new HashMap<String, List<String>>();

    public ScouterTgMtConfig(String measurement) {
        this.measurement = measurement;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public void setDebugEnabled(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
    }

    public Map<String, CounterProtocol> getCounterMapping() {
        return counterMapping;
    }

    public void setCounterMapping(Map<String, CounterProtocol> counterMapping) {
        this.counterMapping = counterMapping;
    }

    public String getObjFamilyBase() {
        return objFamilyBase;
    }

    public void setObjFamilyBase(String objFamilyBase) {
        this.objFamilyBase = X + objFamilyBase;
    }

    public List<String> getObjFamilyAppendTags() {
        return objFamilyAppendTags;
    }

    public void setObjFamilyAppendTags(List<String> objFamilyAppendTags) {
        this.objFamilyAppendTags = objFamilyAppendTags;
    }

    public String getObjTypeBase() {
        return objTypeBase;
    }

    public void setObjTypeBase(String objTypeBase) {
        this.objTypeBase = objTypeBase;
    }

    public List<String> getObjTypePrependTags() {
        return objTypePrependTags;
    }

    public void setObjTypePrependTags(List<String> objTypePrependTags) {
        this.objTypePrependTags = objTypePrependTags;
    }

    public List<String> getObjTypeAppendTags() {
        return objTypeAppendTags;
    }

    public void setObjTypeAppendTags(List<String> objTypeAppendTags) {
        this.objTypeAppendTags = objTypeAppendTags;
    }

    public String getObjTypeIcon() {
        return objTypeIcon;
    }

    public void setObjTypeIcon(String objTypeIcon) {
        this.objTypeIcon = objTypeIcon;
    }

    public String getObjNameBase() {
        return objNameBase;
    }

    public void setObjNameBase(String objNameBase) {
        this.objNameBase = X + objNameBase;
    }

    public List<String> getObjNameAppendTags() {
        return objNameAppendTags;
    }

    public void setObjNameAppendTags(List<String> objNameAppendTags) {
        this.objNameAppendTags = objNameAppendTags;
    }

    public String getHostTag() {
        return hostTag;
    }

    public void setHostTag(String hostTag) {
        this.hostTag = hostTag;
    }

    public Map<String, String> getHostMapping() {
        return hostMapping;
    }

    public void setHostMapping(Map<String, String> hostMapping) {
        this.hostMapping = hostMapping;
    }

    public static String getPrefix() {
        return X;
    }

    public Map<String, List<String>> getTagFilter() {
        return tagFilter;
    }

    public void setTagFilter(Map<String, List<String>> tagFilter) {
        this.tagFilter = tagFilter;
    }

    public String toFamily(Map<String, String> tags) {
        StringBuilder objFamilySb = new StringBuilder(objFamilyBase);
        for (String tagKey : objFamilyAppendTags) {
            objFamilySb.append('_').append(tags.get(tagKey));
        }
        return objFamilySb.toString();
    }

    public String toObjType(Map<String, String> tags) {
        StringBuilder objTypeSb = new StringBuilder();

        for (String tagKey : objTypePrependTags) {
            String prependValue = tags.get(tagKey);
            if (StringUtil.isNotEmpty(prependValue)) {
                objTypeSb.append(prependValue).append('_');
            }
        }

        objTypeSb.append(objTypeBase);

        for (String tagKey : objTypeAppendTags) {
            objTypeSb.append('_').append(tags.get(tagKey));
        }
        return objTypeSb.toString();
    }

    public String toHost(Map<String, String> tags) {
        String host = tags.get(hostTag);
        if (host == null) {
            host = "unknown";
        } else {
            String mappedHost = hostMapping.get(host);
            if (mappedHost != null) {
                host = mappedHost;
            }
        }
        return host;
    }

    public String toObjName(String host, Map<String, String> tags) {
        StringBuilder objNameSb = new StringBuilder(40).append('/').append(host).append('/').append(objNameBase);
        for (String tagKey : objNameAppendTags) {
            objNameSb.append('_').append(tags.get(tagKey));
        }
        return objNameSb.toString();
    }

    public CounterProtocol getCounterProtocol(String counterName) {
        return counterMapping.get(counterName);
    }
    public boolean isTagFilterMatching(Map<String, String> tags) {
        if (tagFilter == null || tagFilter.size() == 0) {
            return true;
        }

        boolean matching = false;
        for (Map.Entry<String, List<String>> e : tagFilter.entrySet()) {
            List<String> matchStrList = e.getValue();
            for (String matchStr : matchStrList) {
                boolean not = false;
                if (matchStr.charAt(0) == '!') {
                    not = true;
                    matchStr = matchStr.substring(1);
                }
                String value = tags.get(e.getKey());
                if (not) {
                    if (!matchStr.equals(value)) {
                        matching = true;
                        break;
                    }
                } else {
                    if (matchStr.equals(value)) {
                        matching = true;
                        break;
                    }
                }
            }
            if (matching) {
                break;
            }
        }
        return matching;
    }

    public boolean isValidConfig() {
        if (StringUtil.isEmpty(objFamilyBase) || objFamilyBase.equals(X)) {
            return false;
        }
        return true;
    }
}
