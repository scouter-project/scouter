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

package scouter.agent;

import scouter.lang.Counter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2018. 7. 22.
 */
public class TelegrafMasurementConfig {
    private String measurement;

    private boolean enabled = true;
    private boolean debugEnabled = false;

    private Map<String, Counter> counterMapping;
    private String objTypeBase;
    private List<String> objTypeAppendTags = new ArrayList<String>();
    private String objNameBase;
    private List<String> objNameAppendTags = new ArrayList<String>();
    private String hostTag;
    private Map<String, String> hostMapping = new HashMap<String, String>();

    public TelegrafMasurementConfig(String measurement) {
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

    public Map<String, Counter> getCounterMapping() {
        return counterMapping;
    }

    public void setCounterMapping(Map<String, Counter> counterMapping) {
        this.counterMapping = counterMapping;
    }

    public String getObjTypeBase() {
        return objTypeBase;
    }

    public void setObjTypeBase(String objTypeBase) {
        this.objTypeBase = objTypeBase;
    }

    public List<String> getObjTypeAppendTags() {
        return objTypeAppendTags;
    }

    public void setObjTypeAppendTags(List<String> objTypeAppendTags) {
        this.objTypeAppendTags = objTypeAppendTags;
    }

    public String getObjNameBase() {
        return objNameBase;
    }

    public void setObjNameBase(String objNameBase) {
        this.objNameBase = objNameBase;
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
}
