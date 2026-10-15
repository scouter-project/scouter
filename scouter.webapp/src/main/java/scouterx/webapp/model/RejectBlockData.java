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
 * An entry on the central block list.
 *
 * There is no TTL - the entry stays until an operator removes it, so the
 * context needed to make that call (why, when, by whom, how many hits) is
 * carried along with it.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class RejectBlockData {
    private String ip;
    /** empty means the whole ip is blocked regardless of url */
    private String url;
    /** CONCURRENT / INTERVAL / RATE / MANUAL / CENTRAL */
    private String reason;
    private long blockedTime;
    /** 'operator', 'auto', or whatever the caller supplied */
    private String blockedBy;
    /** how many times it had been detected before being blocked */
    private long detectCount;
}
