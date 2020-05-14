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

package scouterx.webapp.model.scouter;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import scouter.lang.pack.XLogPack;
import scouterx.webapp.framework.util.ZZ;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 29.
 */
@Getter
@Setter
@Builder
public class SXLog {
    /**
     * Transaction endtime
     */
    private long endTime;
    /**
     * Object ID
     */
    private int objHash;
    /**
     * Transaction name Hash
     */
    private int service;
    /**
     * Transaction ID
     */
    private long txid;
    /**
     * thread name hash
     */
    private int threadNameHash;
    /**
     * Caller ID
     */
    private long caller;
    /**
     * Global transaction ID
     */
    private long gxid;
    /**
     * Elapsed time(ms)
     */
    private int elapsed;
    /**
     * Error hash
     */
    private int error;
    /**
     * Cpu time(ms)
     */
    private int cpu;
    /**
     * SQL count
     */
    private int sqlCount;
    /**
     * SQL time(ms)
     */
    private int sqlTime;
    /**
     * Remote ip address
     */
    private String ipaddr;
    /**
     * Allocated memory(kilo byte)
     */
    private int kbytes;

    /**
     * xlog generated random string to indicate unique user
     */
    private long userid;
    /**
     * User-agent hash
     */
    private int userAgent;
    /**
     * Referrer hash
     */
    private int referrer;
    /**
     * Group hash
     */
    private int group;
    /**
     * ApiCall count
     */
    private int apicallCount;
    /**
     * ApiCall time(ms)
     */
    private int apicallTime;
    /**
     * Country code
     */
    private String countryCode; // CountryCode.getCountryName(countryCode);
    /**
     * City hash
     */
    private int city;
    /**
     * XLog type. WebService:0, AppService:1, BackgroundThread:2
     */
    private int xType; // see XLogTypes
    /**
     * Login hash
     */
    private int login;
    /**
     * Description hash
     */
    private int desc;
    /**
     * has Thread Dump ? No:0, Yes:1
     */
    private byte hasDump;

    /**
     * any text (not use dic)
     */
    private String text1;
    private String text2;
    private String text3;
    private String text4;
    private String text5;

    /**
     * queuing host and time
     */
    private int queuingHostHash;
    private int queuingTime;
    private int queuing2ndHostHash;
    private int queuing2ndTime;

    private int profileCount;
    private int profileSize;

    public static SXLog of(XLogPack p) {
        return SXLog.builder()
                .endTime(p.endTime)
                .objHash(p.objHash)
                .service(p.service)
                .txid(p.txid)
                .threadNameHash(p.threadNameHash)
                .caller(p.caller)
                .gxid(p.gxid)
                .elapsed(p.elapsed)
                .error(p.error)
                .cpu(p.cpu)
                .sqlCount(p.sqlCount)
                .sqlTime(p.sqlTime)
                .ipaddr(ZZ.ipByteToString(p.ipaddr))
                .kbytes(p.kbytes)
                .userid(p.userid)
                .userAgent(p.userAgent)
                .referrer(p.referer)
                .group(p.group)
                .apicallCount(p.apicallCount)
                .apicallTime(p.apicallTime)
                .countryCode(p.countryCode)
                .city(p.city)
                .xType(p.xType)
                .login(p.login)
                .desc(p.desc)
                .hasDump(p.hasDump)
                .text1(p.text1)
                .text2(p.text2)
                .text3(p.text3)
                .text4(p.text4)
                .text5(p.text5)
                .queuingHostHash(p.queuingHostHash)
                .queuingTime(p.queuingTime)
                .queuing2ndHostHash(p.queuing2ndHostHash)
                .queuing2ndTime(p.queuing2ndTime)
                .profileCount(p.profileCount)
                .profileSize(p.profileSize)
                .build();
    }
}
