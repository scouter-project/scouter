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

import scouter.server.support.telegraf.TgmConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 06/01/2019
 */
public class TagFilterMapping {
    public String tag;
    public String mappingValue;

    public TagFilterMapping() {
    }

    public TagFilterMapping(String tag, String mappingValue) {
        this.tag = tag;
        this.mappingValue = mappingValue;
    }

    public static List<TagFilterMapping> of(List<TgmConfig.TagFilter> tfList) {
        List<TagFilterMapping> list = new ArrayList<>();
        for (TgmConfig.TagFilter filter : tfList) {
            for (String match : filter.match) {
                TagFilterMapping mapping = new TagFilterMapping(filter.tag, match);
                list.add(mapping);
            }
        }
        return list;
    }

    public static List<TgmConfig.TagFilter> toOriginal(List<TagFilterMapping> mappings) {
        return mappings.stream()
                .collect(groupingBy(m -> m.tag))
                .entrySet().stream()
                .map(
                        e -> new TgmConfig.TagFilter(
                                e.getKey(),
                                e.getValue().stream()
                                        .map(m -> m.mappingValue)
                                        .collect(Collectors.toList())
                                        .toArray(new String[0])
                        )
                )
                .collect(toList());
    }
}
