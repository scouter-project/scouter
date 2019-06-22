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

package scouter.client.configuration.model;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 06/01/2019
 */
public class SingleString {
    public String value;
    public SingleString() {
    }

    public SingleString(String value) {
        this.value = value;
    }

    public static List<SingleString> of(List<String> list) {
        return list.stream().map(v -> new SingleString(v)).collect(Collectors.toList());
    }

    public static List<String> toOriginal(List<SingleString> singleStrings) {
        return singleStrings.stream().map(s -> s.value).collect(Collectors.toList());
    }
}
