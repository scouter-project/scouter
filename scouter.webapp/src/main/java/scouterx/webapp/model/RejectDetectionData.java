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

package scouterx.webapp.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * A violation reported by agents but not blocked yet - a candidate awaiting an
 * operator decision.
 *
 * Reviewing this list before blocking is the intended workflow : a block has no
 * TTL, so promoting a false positive would keep a legitimate user out for good.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class RejectDetectionData {
    private String ip;
    private String url;
    /** CONCURRENT / INTERVAL / RATE */
    private String reason;
    private long firstTime;
    private long lastTime;
    /** how many times agents reported this (ip,url) */
    private long count;
    /** comma separated names of the agents that reported it */
    private String objNames;
}
