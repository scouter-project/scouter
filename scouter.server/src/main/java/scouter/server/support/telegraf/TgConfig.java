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
import java.util.List;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 05/01/2019
 */
@XmlRootElement(name = "TelegrafConfig")
public class TgConfig {

    public boolean enabled;
    public boolean debugEnabled;
    public boolean deltaCounterNormalizeDefault;
    public int deltaCounterNormalizeDefaultSeconds;
    public int objectDeadtimeMs;

    @XmlElementWrapper
    @XmlElement(name = "measurement")
    public List<TgmConfig> measurements = new ArrayList<TgmConfig>();

    public TgConfig() {
        asDefault();
    }

    public void asDefault() {
        enabled = true;
        debugEnabled = false;
        deltaCounterNormalizeDefault = true;
        deltaCounterNormalizeDefaultSeconds = 30;
        objectDeadtimeMs = 35000;
    }

    public static void main(String[] args) throws JAXBException {
        TgmConfig tgmConfig = new TgmConfig("M1");
        tgmConfig.objFamilyAppendTags.add("tag1");
        tgmConfig.objFamilyAppendTags.add("tag2");

        tgmConfig.objTypeAppendTags.add("tg1");
        tgmConfig.objTypeAppendTags.add("tg2");

        tgmConfig.hostMappings.add(new TgmConfig.HostMapping("host1", "sc-host1"));
        tgmConfig.hostMappings.add(new TgmConfig.HostMapping("host2", "sc-host2"));

        TgmConfig tgmConfig2 = new TgmConfig("M2");

        TgConfig tgConfig = new TgConfig();
        tgConfig.measurements.add(tgmConfig);
        tgConfig.measurements.add(tgmConfig2);

        JAXBContext jc = JAXBContext.newInstance(TgConfig.class);
        Marshaller m = jc.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        m.marshal(tgConfig, System.out);

        m.marshal(tgmConfig, new File("./temp.xml"));

        Unmarshaller um = jc.createUnmarshaller();
        Object o = um.unmarshal(new File("./temp.xml"));
        System.out.println(o);

    }
}
