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

package scouter.server.http.handler;

import org.junit.BeforeClass;
import org.junit.Test;
import scouter.server.Configure;
import scouter.server.http.model.InfluxSingleLine;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2018. 7. 24.
 */
public class TelegrafInputHandlerTest {

    @BeforeClass
    public static void beforeClass() {
        System.setProperty("scouter.config", "./conf/testcase-scouter.conf");
        System.setProperty("input_telegraf_$mem$_enabled", "true");
        System.setProperty("input_telegraf_$mem$_debug_enabled", "true");
        System.setProperty("input_telegraf_$mem$_counter_mappings", "used:tg-used,free:tg-free:memory-free::false,available_percent:tg-mem-pct:memory percent:%:false");
        System.setProperty("input_telegraf_$mem$_objType_base", "MEM");
        //System.setProperty("input_telegraf_$mem$_objType_append_tags", "");
        System.setProperty("input_telegraf_$mem$_objName_base", "tg");
        System.setProperty("input_telegraf_$mem$_objName_append_tags", "region");
        System.setProperty("input_telegraf_$mem$_host_tag", "host");
        //System.setProperty("input_telegraf_$mem$_host_mappings", "h1:Sh1,h2:Sh2");
    }

    /**
     * remove counter.site.xml before test
     * @throws InterruptedException
     */
    @Test
    public void count_new_family_and_objType_is_added() throws InterruptedException {
        String metric = "mem,host=GunMac.skbroadband,region=seoul used=11656097792i,free=1467994112i,cached=0i,buffered=0i,wired=2481405952i,slab=0i,available_percent=32.152581214904785,total=17179869184i,available=5523771392i,active=8165543936i,inactive=4055777280i,used_percent=67.84741878509521 1532269780000000000";
        InfluxSingleLine line = InfluxSingleLine.of(metric, Configure.getInstance(), System.currentTimeMillis());
        TelegrafInputHandler.getInstance().count(line);
        Thread.sleep(300000);
    }

    /**
     * test after count_new_family_and_objType_is_added()
     * @throws InterruptedException
     */
    @Test
    public void count_new_counter_is_added() throws InterruptedException {
        System.setProperty("input_telegraf_$mem$_counter_mappings", "used:tg-used,free:tg-free:memory-free::false,available_percent:tg-mem-pct:memory percent:%:false,inactive:tg-mem-inactive:mem inactive:ea:false");

        String metric = "mem,host=GunMac.skbroadband,region=seoul used=11656097792i,free=1467994112i,cached=0i,buffered=0i,wired=2481405952i,slab=0i,available_percent=32.152581214904785,total=17179869184i,available=5523771392i,active=8165543936i,inactive=4055777280i,used_percent=67.84741878509521 1532269780000000000";
        InfluxSingleLine line = InfluxSingleLine.of(metric, Configure.getInstance(), System.currentTimeMillis());
        TelegrafInputHandler.getInstance().count(line);
        Thread.sleep(300000);
    }
}