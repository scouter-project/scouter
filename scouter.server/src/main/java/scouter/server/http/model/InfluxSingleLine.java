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

import scouter.lang.value.DecimalValue;
import scouter.lang.value.FloatValue;
import scouter.server.Configure;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2018. 7. 22.
 */
public class InfluxSingleLine {
    public static final String SPACE_RE = "|!!|space";
    public static final String BACKSLASH_RE = "|!!|backslash";
    public static final String EQUAL_RE = "|!!|equal";
    private Configure configure;
    private String measuement;

    private String counterName;
    private String objType;
    private int objHash;
    long receviedTime;
    long timestamp;

    Map<String, String> tags;
    Map<String, DecimalValue> decimalFields;
    Map<String, FloatValue> floatFields;

    private InfluxSingleLine() {}

    /**
     * line string key is measurement + tag values
     */
    public static String toLineStringKey(String lineString) {
        return lineString.substring(0, lineString.indexOf(' '));
    }

    public static InfluxSingleLine of(String lineStr) {
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

        InfluxSingleLine line = new InfluxSingleLine();
        return null;
    }
}
