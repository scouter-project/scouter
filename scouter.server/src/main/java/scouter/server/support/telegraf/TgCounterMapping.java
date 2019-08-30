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

import scouter.lang.DeltaType;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 03/01/2019
 */
@XmlRootElement(name = "counterMapping")
public class TgCounterMapping {
    public String tgFieldName;
    public String counterName;
    public String displayName;
    public String unit = "";
    public boolean totalizable;
    public int normalizeSeconds = 30;
    public DeltaType deltaType = DeltaType.NONE;

    public TgCounterMapping() {
    }

    public TgCounterMapping(String tgFieldName, String counterName) {
        this.tgFieldName = tgFieldName;
        this.counterName = counterName;
    }

    public TgCounterMapping(String tgFieldName, String counterName, String displayName) {
        this.tgFieldName = tgFieldName;
        this.counterName = counterName;
        this.displayName = displayName;
    }

    public static void main(String[] args) throws JAXBException {
        TgCounterMapping tgmConfig = new TgCounterMapping("cpu", "cpu", "CPU");

        JAXBContext jc = JAXBContext.newInstance(TgCounterMapping.class);
        Marshaller m = jc.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        m.marshal(tgmConfig, System.out);
        m.marshal(tgmConfig, new File("./temp.xml"));

        Unmarshaller um = jc.createUnmarshaller();
        Object o = um.unmarshal(new File("./temp.xml"));
        System.out.println(o);
    }
}
