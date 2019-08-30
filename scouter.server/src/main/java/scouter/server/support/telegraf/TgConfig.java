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

    public static String getSampleContents() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<TelegrafConfig>\n" +
                "    <enabled>true</enabled>\n" +
                "    <debugEnabled>true</debugEnabled>\n" +
                "    <deltaCounterNormalizeDefault>true</deltaCounterNormalizeDefault>\n" +
                "    <deltaCounterNormalizeDefaultSeconds>30</deltaCounterNormalizeDefaultSeconds>\n" +
                "    <objectDeadtimeMs>35000</objectDeadtimeMs>\n" +
                "    <measurements>\n" +
                "        <measurement>\n" +
                "            <measurementName>cpu</measurementName>\n" +
                "            <enabled>true</enabled>\n" +
                "            <debugEnabled>false</debugEnabled>\n" +
                "            <objFamilyBase>HOST_METRIC</objFamilyBase>\n" +
                "            <objFamilyAppendTags/>\n" +
                "            <objTypeBase>HOST_NEW</objTypeBase>\n" +
                "            <objTypePrependTags>\n" +
                "                <tag>scouter_obj_type_prefix</tag>\n" +
                "            </objTypePrependTags>\n" +
                "            <objTypeAppendTags/>\n" +
                "            <objTypeIcon>linux</objTypeIcon>\n" +
                "            <objNameBase>LINUX_NEW</objNameBase>\n" +
                "            <objNameAppendTags/>\n" +
                "            <hostTag>host</hostTag>\n" +
                "            <hostMappings/>\n" +
                "            <tagFilters>\n" +
                "                <tagFilter>\n" +
                "                    <tag>cpu</tag>\n" +
                "                    <match>cpu-total</match>\n" +
                "                </tagFilter>\n" +
                "            </tagFilters>\n" +
                "            <counterMappings>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>usage_user</tgFieldName>\n" +
                "                    <counterName>cpu_user_$cpu$</counterName>\n" +
                "                    <displayName>cpu_user_$cpu$</displayName>\n" +
                "                    <unit>%</unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>NONE</deltaType>\n" +
                "                </counterMapping>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>usage_system</tgFieldName>\n" +
                "                    <counterName>cpu_system_$cpu$</counterName>\n" +
                "                    <displayName>cpu_system_$cpu$</displayName>\n" +
                "                    <unit>%</unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>NONE</deltaType>\n" +
                "                </counterMapping>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>usage_steal</tgFieldName>\n" +
                "                    <counterName>cpu_steal_$cpu$</counterName>\n" +
                "                    <displayName>cpu_steal_$cpu$</displayName>\n" +
                "                    <unit>%</unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>NONE</deltaType>\n" +
                "                </counterMapping>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>usage_iowait</tgFieldName>\n" +
                "                    <counterName>cpu_iowait_$cpu$</counterName>\n" +
                "                    <displayName>cpu_iowait_$cpu$</displayName>\n" +
                "                    <unit>%</unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>NONE</deltaType>\n" +
                "                </counterMapping>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>usage_nice</tgFieldName>\n" +
                "                    <counterName>cpu_nice_$cpu$</counterName>\n" +
                "                    <displayName>cpu_nice_$cpu$</displayName>\n" +
                "                    <unit>%</unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>NONE</deltaType>\n" +
                "                </counterMapping>\n" +
                "            </counterMappings>\n" +
                "        </measurement>\n" +
                "        <measurement>\n" +
                "            <measurementName>mem</measurementName>\n" +
                "            <enabled>true</enabled>\n" +
                "            <debugEnabled>false</debugEnabled>\n" +
                "            <objFamilyBase>HOST_METRIC</objFamilyBase>\n" +
                "            <objFamilyAppendTags/>\n" +
                "            <objTypeBase>HOST_NEW</objTypeBase>\n" +
                "            <objTypePrependTags>\n" +
                "                <tag>scouter_obj_type_prefix</tag>\n" +
                "            </objTypePrependTags>\n" +
                "            <objTypeAppendTags/>\n" +
                "            <objTypeIcon>linux</objTypeIcon>\n" +
                "            <objNameBase>LINUX_NEW</objNameBase>\n" +
                "            <objNameAppendTags/>\n" +
                "            <hostTag>host</hostTag>\n" +
                "            <hostMappings/>\n" +
                "            <tagFilters/>\n" +
                "            <counterMappings>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>used_percent</tgFieldName>\n" +
                "                    <counterName>mem-used_percent</counterName>\n" +
                "                    <displayName>mem_used_percent</displayName>\n" +
                "                    <unit>%</unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>NONE</deltaType>\n" +
                "                </counterMapping>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>free</tgFieldName>\n" +
                "                    <counterName>mem-free</counterName>\n" +
                "                    <displayName>mem_free</displayName>\n" +
                "                    <unit>byte</unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>NONE</deltaType>\n" +
                "                </counterMapping>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>total</tgFieldName>\n" +
                "                    <counterName>mem-total</counterName>\n" +
                "                    <displayName>mem_total</displayName>\n" +
                "                    <unit>byte</unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>NONE</deltaType>\n" +
                "                </counterMapping>\n" +
                "            </counterMappings>\n" +
                "        </measurement>\n" +
                "        <measurement>\n" +
                "            <measurementName>redis</measurementName>\n" +
                "            <enabled>true</enabled>\n" +
                "            <debugEnabled>false</debugEnabled>\n" +
                "            <objFamilyBase>redis</objFamilyBase>\n" +
                "            <objFamilyAppendTags/>\n" +
                "            <objTypeBase>redis</objTypeBase>\n" +
                "            <objTypePrependTags>\n" +
                "                <tag>scouter_obj_type_prefix</tag>\n" +
                "            </objTypePrependTags>\n" +
                "            <objTypeAppendTags/>\n" +
                "            <objTypeIcon>redis</objTypeIcon>\n" +
                "            <objNameBase>redis</objNameBase>\n" +
                "            <objNameAppendTags>\n" +
                "                <tag>port</tag>\n" +
                "            </objNameAppendTags>\n" +
                "            <hostTag>host</hostTag>\n" +
                "            <hostMappings/>\n" +
                "            <tagFilters/>\n" +
                "            <counterMappings>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>keyspace_hits</tgFieldName>\n" +
                "                    <counterName>keyspace_hits</counterName>\n" +
                "                    <displayName>keyspace_hits</displayName>\n" +
                "                    <unit>ea</unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>NONE</deltaType>\n" +
                "                </counterMapping>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>used_memory</tgFieldName>\n" +
                "                    <counterName>used_memory</counterName>\n" +
                "                    <displayName>used_memory</displayName>\n" +
                "                    <unit>bytes</unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>NONE</deltaType>\n" +
                "                </counterMapping>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>expired_keys</tgFieldName>\n" +
                "                    <counterName>expired_keys</counterName>\n" +
                "                    <displayName>expired_keys</displayName>\n" +
                "                    <unit>ea</unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>BOTH</deltaType>\n" +
                "                </counterMapping>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>evicted_keys</tgFieldName>\n" +
                "                    <counterName>evicted_keys</counterName>\n" +
                "                    <displayName>expired_keys</displayName>\n" +
                "                    <unit>ea</unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>BOTH</deltaType>\n" +
                "                </counterMapping>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>total_connections_received</tgFieldName>\n" +
                "                    <counterName>total_connections_received</counterName>\n" +
                "                    <displayName>total_connections_received</displayName>\n" +
                "                    <unit>ea</unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>BOTH</deltaType>\n" +
                "                </counterMapping>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>total_commands_processed</tgFieldName>\n" +
                "                    <counterName>total_commands_processed</counterName>\n" +
                "                    <displayName>total_commands_processed</displayName>\n" +
                "                    <unit>ea</unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>BOTH</deltaType>\n" +
                "                </counterMapping>\n" +
                "            </counterMappings>\n" +
                "        </measurement>\n" +
                "        <measurement>\n" +
                "            <measurementName>redis_keyspace</measurementName>\n" +
                "            <enabled>true</enabled>\n" +
                "            <debugEnabled>false</debugEnabled>\n" +
                "            <objFamilyBase>redis</objFamilyBase>\n" +
                "            <objFamilyAppendTags/>\n" +
                "            <objTypeBase>redis</objTypeBase>\n" +
                "            <objTypePrependTags>\n" +
                "                <tag>scouter_obj_type_prefix</tag>\n" +
                "            </objTypePrependTags>\n" +
                "            <objTypeAppendTags/>\n" +
                "            <objTypeIcon>redis</objTypeIcon>\n" +
                "            <objNameBase>redis</objNameBase>\n" +
                "            <objNameAppendTags>\n" +
                "                <tag>port</tag>\n" +
                "            </objNameAppendTags>\n" +
                "            <hostTag>host</hostTag>\n" +
                "            <hostMappings/>\n" +
                "            <tagFilters/>\n" +
                "            <counterMappings>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>keys</tgFieldName>\n" +
                "                    <counterName>keys-$database$</counterName>\n" +
                "                    <displayName>keys-$database$</displayName>\n" +
                "                    <unit>ea</unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>BOTH</deltaType>\n" +
                "                </counterMapping>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>expires</tgFieldName>\n" +
                "                    <counterName>expires-$database$</counterName>\n" +
                "                    <displayName>expires-$database$</displayName>\n" +
                "                    <unit>ea</unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>BOTH</deltaType>\n" +
                "                </counterMapping>\n" +
                "            </counterMappings>\n" +
                "        </measurement>\n" +
                "        <measurement>\n" +
                "            <measurementName>nginx</measurementName>\n" +
                "            <enabled>true</enabled>\n" +
                "            <debugEnabled>false</debugEnabled>\n" +
                "            <objFamilyBase>nginx</objFamilyBase>\n" +
                "            <objFamilyAppendTags/>\n" +
                "            <objTypeBase>nginx</objTypeBase>\n" +
                "            <objTypePrependTags>\n" +
                "                <tag>scouter_obj_type_prefix</tag>\n" +
                "            </objTypePrependTags>\n" +
                "            <objTypeAppendTags/>\n" +
                "            <objTypeIcon>nginx</objTypeIcon>\n" +
                "            <objNameBase>nginx</objNameBase>\n" +
                "            <objNameAppendTags>\n" +
                "                <tag>port</tag>\n" +
                "            </objNameAppendTags>\n" +
                "            <hostTag>host</hostTag>\n" +
                "            <hostMappings/>\n" +
                "            <tagFilters/>\n" +
                "            <counterMappings>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>active</tgFieldName>\n" +
                "                    <counterName>active</counterName>\n" +
                "                    <displayName>active-connections</displayName>\n" +
                "                    <unit></unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>NONE</deltaType>\n" +
                "                </counterMapping>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>reading</tgFieldName>\n" +
                "                    <counterName>reading</counterName>\n" +
                "                    <displayName>active-conn-header-reading</displayName>\n" +
                "                    <unit></unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>NONE</deltaType>\n" +
                "                </counterMapping>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>writing</tgFieldName>\n" +
                "                    <counterName>writing</counterName>\n" +
                "                    <displayName>active-conn-working</displayName>\n" +
                "                    <unit></unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>NONE</deltaType>\n" +
                "                </counterMapping>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>waiting</tgFieldName>\n" +
                "                    <counterName>waiting</counterName>\n" +
                "                    <displayName>keepalived-connections</displayName>\n" +
                "                    <unit></unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>NONE</deltaType>\n" +
                "                </counterMapping>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>accepts</tgFieldName>\n" +
                "                    <counterName>accepts</counterName>\n" +
                "                    <displayName>accepted-connections</displayName>\n" +
                "                    <unit></unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>DELTA</deltaType>\n" +
                "                </counterMapping>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>handled</tgFieldName>\n" +
                "                    <counterName>handled</counterName>\n" +
                "                    <displayName>handled-connections</displayName>\n" +
                "                    <unit></unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>DELTA</deltaType>\n" +
                "                </counterMapping>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>requests</tgFieldName>\n" +
                "                    <counterName>requests</counterName>\n" +
                "                    <displayName>request-count</displayName>\n" +
                "                    <unit></unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>DELTA</deltaType>\n" +
                "                </counterMapping>\n" +
                "            </counterMappings>\n" +
                "        </measurement>\n" +
                "        <measurement>\n" +
                "            <measurementName>mysql</measurementName>\n" +
                "            <enabled>true</enabled>\n" +
                "            <debugEnabled>false</debugEnabled>\n" +
                "            <objFamilyBase>mysql</objFamilyBase>\n" +
                "            <objFamilyAppendTags/>\n" +
                "            <objTypeBase>mysql</objTypeBase>\n" +
                "            <objTypePrependTags>\n" +
                "                <tag>scouter_obj_type_prefix</tag>\n" +
                "            </objTypePrependTags>\n" +
                "            <objTypeAppendTags/>\n" +
                "            <objTypeIcon>mysql</objTypeIcon>\n" +
                "            <objNameBase></objNameBase>\n" +
                "            <objNameAppendTags>\n" +
                "                <tag>server</tag>\n" +
                "            </objNameAppendTags>\n" +
                "            <hostTag>host</hostTag>\n" +
                "            <hostMappings/>\n" +
                "            <tagFilters/>\n" +
                "            <counterMappings>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>connections</tgFieldName>\n" +
                "                    <counterName>connections</counterName>\n" +
                "                    <displayName>connections</displayName>\n" +
                "                    <unit></unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>NONE</deltaType>\n" +
                "                </counterMapping>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>bytes_received</tgFieldName>\n" +
                "                    <counterName>bytes_received</counterName>\n" +
                "                    <displayName>bytes_received</displayName>\n" +
                "                    <unit></unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>DELTA</deltaType>\n" +
                "                </counterMapping>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>bytes_sent</tgFieldName>\n" +
                "                    <counterName>bytes_sent</counterName>\n" +
                "                    <displayName>bytes_sent</displayName>\n" +
                "                    <unit></unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>DELTA</deltaType>\n" +
                "                </counterMapping>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>com_commit</tgFieldName>\n" +
                "                    <counterName>com_commit</counterName>\n" +
                "                    <displayName>com_commit</displayName>\n" +
                "                    <unit></unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>DELTA</deltaType>\n" +
                "                </counterMapping>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>com_insert</tgFieldName>\n" +
                "                    <counterName>com_insert</counterName>\n" +
                "                    <displayName>com_insert</displayName>\n" +
                "                    <unit></unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>DELTA</deltaType>\n" +
                "                </counterMapping>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>com_update</tgFieldName>\n" +
                "                    <counterName>com_update</counterName>\n" +
                "                    <displayName>com_update</displayName>\n" +
                "                    <unit></unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>DELTA</deltaType>\n" +
                "                </counterMapping>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>com_select</tgFieldName>\n" +
                "                    <counterName>com_select</counterName>\n" +
                "                    <displayName>com_select</displayName>\n" +
                "                    <unit></unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>DELTA</deltaType>\n" +
                "                </counterMapping>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>innodb_data_reads</tgFieldName>\n" +
                "                    <counterName>innodb_data_reads</counterName>\n" +
                "                    <displayName>innodb_data_reads</displayName>\n" +
                "                    <unit></unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>DELTA</deltaType>\n" +
                "                </counterMapping>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>innodb_data_writes</tgFieldName>\n" +
                "                    <counterName>innodb_data_writes</counterName>\n" +
                "                    <displayName>innodb_data_writes</displayName>\n" +
                "                    <unit></unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>DELTA</deltaType>\n" +
                "                </counterMapping>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>innodb_row_lock_waits</tgFieldName>\n" +
                "                    <counterName>innodb_row_lock_waits</counterName>\n" +
                "                    <displayName>innodb_row_lock_waits</displayName>\n" +
                "                    <unit></unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>DELTA</deltaType>\n" +
                "                </counterMapping>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>innodb_buffer_pool_pages_total</tgFieldName>\n" +
                "                    <counterName>innodb_buffer_pool_pages_dirty</counterName>\n" +
                "                    <displayName>innodb_buffer_pool_pages_dirty</displayName>\n" +
                "                    <unit></unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>NONE</deltaType>\n" +
                "                </counterMapping>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>innodb_buffer_pool_pages_dirty</tgFieldName>\n" +
                "                    <counterName>innodb_buffer_pool_pages_dirty</counterName>\n" +
                "                    <displayName>innodb_buffer_pool_pages_dirty</displayName>\n" +
                "                    <unit></unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>NONE</deltaType>\n" +
                "                </counterMapping>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>innodb_buffer_pool_pages_free</tgFieldName>\n" +
                "                    <counterName>innodb_buffer_pool_pages_free</counterName>\n" +
                "                    <displayName>innodb_buffer_pool_pages_free</displayName>\n" +
                "                    <unit></unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>NONE</deltaType>\n" +
                "                </counterMapping>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>innodb_data_pending_fsyncs</tgFieldName>\n" +
                "                    <counterName>innodb_data_pending_fsyncs</counterName>\n" +
                "                    <displayName>innodb_data_pending_fsyncs</displayName>\n" +
                "                    <unit></unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>NONE</deltaType>\n" +
                "                </counterMapping>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>innodb_data_pending_reads</tgFieldName>\n" +
                "                    <counterName>innodb_data_pending_reads</counterName>\n" +
                "                    <displayName>innodb_data_pending_reads</displayName>\n" +
                "                    <unit></unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>NONE</deltaType>\n" +
                "                </counterMapping>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>innodb_data_pending_writes</tgFieldName>\n" +
                "                    <counterName>innodb_data_pending_writes</counterName>\n" +
                "                    <displayName>innodb_data_pending_writes</displayName>\n" +
                "                    <unit></unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>NONE</deltaType>\n" +
                "                </counterMapping>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>innodb_row_lock_current_waits</tgFieldName>\n" +
                "                    <counterName>innodb_row_lock_current_waits</counterName>\n" +
                "                    <displayName>innodb_row_lock_current_waits</displayName>\n" +
                "                    <unit></unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>NONE</deltaType>\n" +
                "                </counterMapping>\n" +
                "            </counterMappings>\n" +
                "        </measurement>\n" +
                "        <measurement>\n" +
                "            <measurementName>mysql_innodb</measurementName>\n" +
                "            <enabled>true</enabled>\n" +
                "            <debugEnabled>false</debugEnabled>\n" +
                "            <objFamilyBase>mysql</objFamilyBase>\n" +
                "            <objFamilyAppendTags/>\n" +
                "            <objTypeBase>mysql</objTypeBase>\n" +
                "            <objTypePrependTags>\n" +
                "                <tag>scouter_obj_type_prefix</tag>\n" +
                "            </objTypePrependTags>\n" +
                "            <objTypeAppendTags/>\n" +
                "            <objTypeIcon>mysql</objTypeIcon>\n" +
                "            <objNameBase>mysql</objNameBase>\n" +
                "            <objNameAppendTags>\n" +
                "                <tag>server</tag>\n" +
                "            </objNameAppendTags>\n" +
                "            <hostTag>host</hostTag>\n" +
                "            <hostMappings/>\n" +
                "            <tagFilters/>\n" +
                "            <counterMappings>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>dml_deletes</tgFieldName>\n" +
                "                    <counterName>dml_deletes</counterName>\n" +
                "                    <displayName>dml_deletes</displayName>\n" +
                "                    <unit></unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>DELTA</deltaType>\n" +
                "                </counterMapping>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>dml_updates</tgFieldName>\n" +
                "                    <counterName>dml_updates</counterName>\n" +
                "                    <displayName>dml_updates</displayName>\n" +
                "                    <unit></unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>DELTA</deltaType>\n" +
                "                </counterMapping>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>dml_inserts</tgFieldName>\n" +
                "                    <counterName>dml_inserts</counterName>\n" +
                "                    <displayName>dml_inserts</displayName>\n" +
                "                    <unit></unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>DELTA</deltaType>\n" +
                "                </counterMapping>\n" +
                "                <counterMapping>\n" +
                "                    <tgFieldName>dml_reads</tgFieldName>\n" +
                "                    <counterName>dml_reads</counterName>\n" +
                "                    <displayName>dml_reads</displayName>\n" +
                "                    <unit></unit>\n" +
                "                    <totalizable>false</totalizable>\n" +
                "                    <normalizeSeconds>30</normalizeSeconds>\n" +
                "                    <deltaType>DELTA</deltaType>\n" +
                "                </counterMapping>\n" +
                "            </counterMappings>\n" +
                "        </measurement>\n" +
                "    </measurements>\n" +
                "</TelegrafConfig>\n";
    }
}
