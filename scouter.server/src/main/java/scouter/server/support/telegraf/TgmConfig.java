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

package scouter.server.support.telegraf;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 03/01/2019
 */
@XmlRootElement(name = "measurement")
public class TgmConfig {
    public static final String DEFAULT_OBJ_TYPE_PREPEND_TAG = "scouter_obj_type_prefix";

    public static class HostMapping {
        public String telegraf;
        public String scouter;

        public HostMapping() {
        }

        public HostMapping(String telegraf, String scouter) {
            this.telegraf = telegraf;
            this.scouter = scouter;
        }
    }

    public static class TagFilter {
        public String tag;
        public List<String> match;

        public TagFilter() {
        }

        public TagFilter(String tag, String... matches) {
            this.tag = tag;
            this.match = new ArrayList<>(Arrays.asList(matches));
        }

        public String getTag() {
            return tag;
        }

        public List<String> getMatch() {
            return match;
        }
    }

    public String measurementName;
    public boolean enabled = true;
    public boolean debugEnabled = false;

    public String objFamilyBase = "";
    @XmlElementWrapper
    @XmlElement(name = "tag")
    public List<String> objFamilyAppendTags = new ArrayList<String>();

    public String objTypeBase = "";
    @XmlElementWrapper
    @XmlElement(name = "tag")
    public List<String> objTypePrependTags = new ArrayList<>(Arrays.asList(DEFAULT_OBJ_TYPE_PREPEND_TAG));
    @XmlElementWrapper
    @XmlElement(name = "tag")
    public List<String> objTypeAppendTags = new ArrayList<String>();
    public String objTypeIcon = "";

    public String objNameBase = "";
    @XmlElementWrapper
    @XmlElement(name = "tag")
    public List<String> objNameAppendTags = new ArrayList<String>();

    public String hostTag = "host";
    @XmlElementWrapper
    @XmlElement(name = "hostMapping")
    public List<HostMapping> hostMappings = new ArrayList<HostMapping>();
    @XmlElementWrapper
    @XmlElement(name = "tagFilter")
    public List<TagFilter> tagFilters = new ArrayList<TagFilter>();

    @XmlElementWrapper
    @XmlElement(name = "counterMapping")
    public List<TgCounterMapping> counterMappings = new ArrayList<TgCounterMapping>();

    public TgmConfig() {
    }

    public TgmConfig(String measurementName) {
        this.measurementName = measurementName;
    }

    public static void main(String[] args) throws JAXBException {
        TgmConfig tgmConfig = new TgmConfig("M1");
        tgmConfig.objFamilyAppendTags.add("tag1");
        tgmConfig.objFamilyAppendTags.add("tag2");

        tgmConfig.objTypeAppendTags.add("tg1");
        tgmConfig.objTypeAppendTags.add("tg2");

        tgmConfig.hostMappings.add(new HostMapping("host1", "sc-host1"));
        tgmConfig.hostMappings.add(new HostMapping("host2", "sc-host2"));

        tgmConfig.tagFilters.add(new TagFilter("cpu", "cpu0", "cpu1"));
        tgmConfig.tagFilters.add(new TagFilter("mem", "mem0", "mem1"));

        TgCounterMapping tgCounterMapping = new TgCounterMapping("cpu", "cpu", "CPU");
        TgCounterMapping tgCounterMapping2 = new TgCounterMapping("mem", "mem", "MEM");

        tgmConfig.counterMappings.add(tgCounterMapping);
        tgmConfig.counterMappings.add(tgCounterMapping2);

        JAXBContext jc = JAXBContext.newInstance(TgmConfig.class);
        Marshaller m = jc.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        m.marshal(tgmConfig, System.out);

        m.marshal(tgmConfig, new File("./temp.xml"));

        Unmarshaller um = jc.createUnmarshaller();
        Object o = um.unmarshal(new File("./temp.xml"));
        System.out.println(o);

    }
}
