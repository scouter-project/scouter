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
 *
 */
package scouter.client.cubrid;

public enum CubridTypeShotPeriod {
	FIVE_MIN("5 min", 5 * 60 * 1000),
    TEN_MIN("10 min", 10 * 60 * 1000),
    TWT_MIN("20 min", 20 * 60 * 1000),
    THIRTY_MIN("30 min", 30 * 60 * 1000),
    ONE_HOUR("1 hour", 60 * 60 * 1000),
    FOUR_HOURS("4 hours", 4 * 60 * 60 * 1000);

    private String label;
    private long time;

    private CubridTypeShotPeriod(String label, long time) {
        this.label = label;
        this.time = time;
    }

    public String getLabel() {
        return this.label;
    }

    public long getTime() {
        return this.time;
    }

    public static CubridTypeShotPeriod fromString(String text) {
        if (text != null) {
            for (CubridTypeShotPeriod b : CubridTypeShotPeriod.values()) {
                if (text.equalsIgnoreCase(b.label)) {
                    return b;
                }
            }
        }
        return null;
    }
    
    public static CubridTypeShotPeriod fromTime(Long time) {
        for (CubridTypeShotPeriod b : CubridTypeShotPeriod.values()) {
            if (time == b.time) {
                return b;
            }
        }
        return null;
    }
}
