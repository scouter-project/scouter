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
import scouter.lang.value.DecimalValue;
import scouter.lang.value.FloatValue;
import scouter.lang.value.NumberValue;
import scouter.server.Configure;
import scouter.server.TelegrafMasurementConfig;
import scouter.util.HashUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2018. 7. 22.
 */
public class InfluxSingleLine {
    private String measuement;
    private String host;
    private String objType;
    private String objName;
    private int objHash;
    long receivedTime;
    long timestampOrigin;

    Map<String, String> tags;
    Map<Counter, NumberValue> numberFields = new HashMap<Counter, NumberValue>();

    private InfluxSingleLine(TelegrafMasurementConfig tConfig,
                             String measuement,
                             Map<String, String> tags,
                             Map<String, String> fields,
                             long receivedTime,
                             long timestampOrigin) {

        this.measuement = measuement;
        this.tags = tags;
        this.receivedTime = receivedTime;
        this.timestampOrigin = timestampOrigin;

        this.host = tags.get(tConfig.getHostTag());
        if (this.host == null) {
            this.host = "unknown";
        } else {
            String mappedHost = tConfig.getHostMapping().get(this.host);
            if (mappedHost != null) {
                this.host = mappedHost;
            }
        }

        StringBuilder objTypeSb = new StringBuilder(tConfig.getObjTypeBase());
        for (String tagKey : tConfig.getObjTypeAppendTags()) {
            objTypeSb.append('_').append(tags.get(tagKey));
        }
        this.objType = objTypeSb.toString();

        StringBuilder objNamSb = new StringBuilder(40).append('/').append(host).append('/').append(tConfig.getObjNameBase());
        for (String tagKey : tConfig.getObjNameAppendTags()) {
            objNamSb.append('_').append(tags.get(tagKey));
        }
        this.objName = objNamSb.toString();
        this.objHash = HashUtil.hash(objName);

        Map<String, Counter> counterMapping = tConfig.getCounterMapping();
        for (Map.Entry<String, String> field : fields.entrySet()) {
            Counter counter = counterMapping.get(field.getKey());
            if (counter == null) {
                continue;
            }
            String valueStr = field.getValue();
            int valueLen = valueStr.length();
            char lastChar = valueStr.charAt(valueLen - 1);

            if (lastChar == 'i') { //long
                long value = Long.parseLong(valueStr.substring(0, valueLen - 1));
                numberFields.put(counter, new DecimalValue(value));
            } else if (lastChar >= '0' && lastChar <= '9') { //float
                numberFields.put(counter, new FloatValue(Float.parseFloat(valueStr)));
            } else {
                //skip - boolean or string
                continue;
            }
        }
    }

    /**
     * line string key is measurement + tag values
     */
    public static String toLineStringKey(String lineString) {
        return lineString.substring(0, lineString.indexOf(' '));
    }

    public static InfluxSingleLine of(String lineStr, Configure configure, long receivedTime) {
        char[] chars = lineStr.toCharArray();
        char sink = '\0';
        int mode = 0; //0: measurement, 1: tags, 2: fields, 3: timestamp

        StringBuilder measurementSb = new StringBuilder();

        StringBuilder tagKeySb = new StringBuilder();
        StringBuilder tagValueSb = new StringBuilder();
        boolean tagKeyMode = true; //if false then tag value mode
        Map<String, String> tags = new HashMap<String, String>();

        StringBuilder fieldKeySb = new StringBuilder();
        StringBuilder fieldValueSb = new StringBuilder();
        boolean fieldKeyMode = true; //if false then field value mode
        Map<String, String> fields = new HashMap<String, String>();

        StringBuilder timestampSb = new StringBuilder();

        for (int pos = 0; pos < lineStr.length(); pos++) {
            char c = chars[pos];
            if (mode == 0) { //measurement
                if (sink == '\\') {
                    measurementSb.append(c);
                    sink = '\0';

                } else {
                    switch (c) {
                        case '\\':
                            sink = '\\';
                            break;
                        case ',':
                            mode++;
                            break;
                        default:
                            measurementSb.append(c);
                            break;
                    }
                }

            } else if (mode == 1) { //tags
                if (sink == '\\') {
                    if (tagKeyMode) {
                        tagKeySb.append(c);
                    } else {
                        tagValueSb.append(c);
                    }
                    sink = '\0';

                } else {
                    switch (c) {
                        case '\\':
                            sink = '\\';
                            break;
                        case ' ':
                            mode++;
                            if (tagKeySb.length() > 0) {
                                tags.put(tagKeySb.toString(), tagValueSb.toString());
                            }
                            break;
                        case '=':
                            tagKeyMode = false;
                            break;
                        case ',':
                            tagKeyMode = true;
                            tags.put(tagKeySb.toString(), tagValueSb.toString());
                            tagKeySb = new StringBuilder();
                            tagValueSb = new StringBuilder();
                            break;
                        default:
                            if (tagKeyMode) {
                                tagKeySb.append(c);
                            } else {
                                tagValueSb.append(c);
                            }
                            break;
                    }
                }

            } else if (mode == 2) { //fields
                if (sink == '\\') {
                    if (fieldKeyMode) {
                        fieldKeySb.append(c);
                    } else {
                        fieldValueSb.append(c);
                    }
                    sink = '\0';

                } else {
                    switch (c) {
                        case '\\':
                            sink = '\\';
                            break;
                        case ' ':
                            mode++;
                            if (fieldKeySb.length() > 0) {
                                fields.put(fieldKeySb.toString(), fieldValueSb.toString());
                            }
                            break;
                        case '=':
                            tagKeyMode = false;
                            break;
                        case ',':
                            tagKeyMode = true;
                            fields.put(fieldKeySb.toString(), fieldValueSb.toString());
                            fieldKeySb = new StringBuilder();
                            fieldValueSb = new StringBuilder();
                            break;
                        default:
                            if (tagKeyMode) {
                                fieldKeySb.append(c);
                            } else {
                                fieldValueSb.append(c);
                            }
                            break;
                    }
                }

            } else if (mode == 3) { // timestamp
                timestampSb.append(c);
            }
        }

        String measurement = measurementSb.toString();
        TelegrafMasurementConfig tConfig = configure.telegrafInputConfigMap.get(measurement);
        if (tConfig == null) {
            return null;
        }
        if (!tConfig.isEnabled()) {
            return null;
        }


        try {
            return new InfluxSingleLine(tConfig, measurement, tags, fields, receivedTime, Long.parseLong(timestampSb.toString()));
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    public String getMeasuement() {
        return measuement;
    }

    public String getHost() {
        return host;
    }

    public String getObjType() {
        return objType;
    }

    public String getObjName() {
        return objName;
    }

    public int getObjHash() {
        return objHash;
    }

    public long getReceivedTime() {
        return receivedTime;
    }

    public long getTimestampOrigin() {
        return timestampOrigin;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public Map<Counter, NumberValue> getNumberFields() {
        return numberFields;
    }
}
