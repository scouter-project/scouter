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
import scouter.lang.ObjectType;
import scouter.server.Logger;
import scouter.server.support.telegraf.TgCounterMapping;
import scouter.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CounterProtocol extends Counter {
    private static final long KEEP_FRESH_MILLIS = 15 * 1000L;
    private boolean fresh = true;
    private long initTimestamp = System.currentTimeMillis();

    private List<String> nameTags = new ArrayList<String>();
    private List<String> displayNameTags = new ArrayList<String>();
    private int normalizeSec = 0;

    private DeltaType deltaType = DeltaType.NONE;


    public CounterProtocol() {
    }

    public CounterProtocol(String name) {
        super(name);
    }

    public static CounterProtocol of(TgCounterMapping mapping) {
        CounterProtocol counter = new CounterProtocol();
        counter.setName(mapping.counterName);
        counter.setDeltaType(mapping.deltaType);
        if (StringUtil.isNotEmpty(mapping.displayName)) {
            counter.setDisplayName(mapping.displayName);
        } else {
            counter.setDisplayName(mapping.counterName);
        }
        counter.setUnit(mapping.unit);
        counter.setTotal(mapping.totalizable);
        counter.setNormalizeSec(mapping.normalizeSeconds);

        return counter;
    }

    private void setFreshFalseIfIntervalExpired() {
        if (System.currentTimeMillis() - initTimestamp > KEEP_FRESH_MILLIS) {
            this.fresh = false;
        }
    }

    public void setDeltaType(DeltaType deltaType) {
        this.deltaType = deltaType
        ;
    }

    public DeltaType getDeltaType() {
        return deltaType;
    }

    public void setNormalizeSec(int normalizeSec) {
        if (normalizeSec < MIN_NORMALIZE_SEC) {
            this.normalizeSec = 0;
        } else if (normalizeSec > MAX_NORMALIZE_SEC) {
            this.normalizeSec = MAX_NORMALIZE_SEC;
        } else {
            this.normalizeSec = normalizeSec;
        }
    }

    public int getNormalizeSec() {
        return normalizeSec;
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
                default:
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
                default:
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

    public String getTaggedDeltaDisplayName(Map<String, String> tagMap) {
        return generateTaggedDeltaName(this.getDisplayName(), this.displayNameTags, tagMap);
    }

    public String getDelateUnit() {
        return generateDeltaUnit(this.getUnit());
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
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    private String generateDeltaUnit(String unit) {
        return unit + "/s";
    }

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
        deltaCounter.setUnit(generateDeltaUnit(deltaCounter.getUnit()));

        return deltaCounter;
    }

    public boolean hasDeltaCounter() {
        return (this.getDeltaType() == DeltaType.DELTA || this.getDeltaType() == DeltaType.BOTH);
    }

    public boolean hasNormalCounter() {
        return (this.getDeltaType() != DeltaType.DELTA);
    }

    public boolean isNewOrChangedCounter(ObjectType objectType, InfluxSingleLine line) {
        boolean isNew;
        isNew = checkNewOrChanged4NormalCounter(objectType, line);

        if (!isNew) {
            isNew = checkNewOrChanged4DeltaCounter(objectType, line);
            if (isNew) {
                Logger.println("New Telegraf Delta Counter : " + this.getTaggedDelataName(line.getTags()));
            }
        } else {
            Logger.println("New Telegraf Counter : " + this.getTaggedName(line.getTags()));
        }

        setFreshFalseIfIntervalExpired();
        return isNew;
    }

    private boolean checkNewOrChanged4NormalCounter(ObjectType objectType, InfluxSingleLine line) {
        if (!hasNormalCounter()) {
            return false;
        }
        Counter counter = objectType.getCounter(getTaggedName(line.getTags()));
        if (counter == null) {
            return true;
        }
        if (checkChangedDeeper(false, counter, line, getUnit(), getIcon())) {
            return true;
        }

        return false;
    }

    private boolean checkNewOrChanged4DeltaCounter(ObjectType objectType, InfluxSingleLine line) {
        if (!hasDeltaCounter()) {
            return false;
        }

        Counter counter = objectType.getCounter(getTaggedDelataName(line.getTags()));
        if (counter == null) {
            return true;
        }

        if (checkChangedDeeper(true, counter, line, generateDeltaUnit(getUnit()), getIcon())) {
            return true;
        }

        return false;
    }

    private boolean checkChangedDeeper(boolean isDeltaCounter, Counter counter, InfluxSingleLine line, String unit, String icon) {
        if (this.fresh) { //deep search
            String taggedDisplayName = isDeltaCounter ? getTaggedDeltaDisplayName(line.getTags()) : getTaggedDisplayName(line.getTags());
            if (!taggedDisplayName.equals(counter.getDisplayName())
                    || !unit.equals(counter.getUnit())
                    || !icon.equals(counter.getIcon())
                    || isTotal() != counter.isTotal()) {
                return true;
            }
        }
        return false;
    }
}