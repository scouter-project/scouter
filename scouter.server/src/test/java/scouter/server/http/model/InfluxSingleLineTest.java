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

package scouter.server.http.model;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2018. 7. 22.
 */
public class InfluxSingleLineTest {

//    mem,host=GunMac.skbroadband used=11656097792i,free=1467994112i,cached=0i,buffered=0i,wired=2481405952i,slab=0i,available_percent=32.152581214904785,total=17179869184i,available=5523771392i,active=8165543936i,inactive=4055777280i,used_percent=67.84741878509521 1532269780000000000
//    cpu,cpu=cpu0,host=GunMac.skbroadband usage_user=23.923923923923923,usage_idle=64.46446446446447,usage_iowait=0,usage_irq=0,usage_softirq=0,usage_guest=0,usage_system=11.611611611611611,usage_nice=0,usage_steal=0,usage_guest_nice=0 1532269780000000000
//    cpu,cpu=cpu1,host=GunMac.skbroadband usage_nice=0,usage_irq=0,usage_user=12.574850299401197,usage_system=5.289421157684631,usage_idle=82.13572854291417,usage_guest=0,usage_guest_nice=0,usage_iowait=0,usage_softirq=0,usage_steal=0 1532269780000000000
//    cpu,cpu=cpu2,host=GunMac.skbroadband usage_system=9.3,usage_iowait=0,usage_softirq=0,usage_guest=0,usage_guest_nice=0,usage_user=24.4,usage_idle=66.3,usage_nice=0,usage_irq=0,usage_steal=0 1532269780000000000
//    cpu,cpu=cpu3,host=GunMac.skbroadband usage_guest_nice=0,usage_user=12.387612387612387,usage_idle=82.51748251748252,usage_iowait=0,usage_guest=0,usage_steal=0,usage_system=5.094905094905095,usage_nice=0,usage_irq=0,usage_softirq=0 1532269780000000000
//    cpu,cpu=cpu-total,host=GunMac.skbroadband usage_user=18.315842078960518,usage_iowait=0,usage_irq=0,usage_softirq=0,usage_guest=0,usage_system=7.8210894552723635,usage_idle=73.86306846576711,usage_nice=0,usage_steal=0,usage_guest_nice=0 1532269780000000000

    @Test
    public void of_test() {
        InfluxSingleLine.of("mem,host=GunMac.skbroadband,region=seoul used=11656097792i,free=1467994112i,cached=0i,buffered=0i,wired=2481405952i,slab=0i,available_percent=32.152581214904785,total=17179869184i,available=5523771392i,active=8165543936i,inactive=4055777280i,used_percent=67.84741878509521 1532269780000000000");
    }

    @Test
    public void perf_test() {
        String metric = "mem,host=GunMac.skbroadband,region=seoul used=11656097792i,free=1467994112i,cached=0i,buffered=0i,wired=2481405952i,slab=0i,available_percent=32.152581214904785,total=17179869184i,available=5523771392i,active=8165543936i,inactive=4055777280i,used_percent=67.84741878509521 1532269780000000000";

        long start = System.currentTimeMillis();
        for (int i = 0; i < 300000; i++) {
            InfluxSingleLine.of(metric);
        }
        System.out.println("[of millis]" + (System.currentTimeMillis() - start));

        start = System.currentTimeMillis();
        for (int i = 0; i < 300000; i++) {
            InfluxSingleLine.of(metric);
        }
        System.out.println("[of millis]" + (System.currentTimeMillis() - start));

        start = System.currentTimeMillis();
        for (int i = 0; i < 300000; i++) {
            InfluxSingleLine.of(metric);
        }
        System.out.println("[of millis]" + (System.currentTimeMillis() - start));
    }
}