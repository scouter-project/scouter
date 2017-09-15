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

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import scouter.lang.pack.XLogPack;
import scouter.lang.pack.XLogTypes;
import scouterx.webapp.framework.client.model.TextLoader;
import scouterx.webapp.framework.client.model.TextProxy;
import scouterx.webapp.framework.client.model.TextTypeEnum;
import scouterx.webapp.framework.util.ZZ;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 29.
 */
@Getter
@Setter
@Builder
public class XLogData {
    /**
     * Transaction endtime
     */
    private long endTime;
    /**
     * Object ID
     */
    private int objHash;
    /**
     * Transaction name
     */
    private String service;
    /**
     * Transaction ID
     */
    private long txid;
    /**
     * thread name
     */
    private String threadName;
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
     * Error
     */
    private String error;
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
    private String ipAddr;
    /**
     * Allocated memory(kilo byte)
     */
    private int allocatedMemory;

    /**
     * xlog generated random string to indicate unique user
     */
    private long internalId;
    /**
     * User-agent hash
     */
    private String userAgent;
    /**
     * Referer
     */
    private String referrer;
    /**
     * Group
     */
    private String group;
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
    private String city;
    /**
     * XLog type. WebService:0, AppService:1, BackgroundThread:2
     */
    private String xLogType; // see XLogTypes
    /**
     * Login value set from java agent plugin scripting
     */
    private String login;
    /**
     * Description value set from java agent plugin scripting
     */
    private String desc;
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
    private String queuingHost;
    private int queuingTime;
    private String queuing2ndHost;
    private int queuing2ndTime;

    public static XLogData of(XLogPack p, int serverId) {
        preLoadDictionary(p, serverId);

        return XLogData.builder()
                .endTime(p.endTime)
                .objHash(p.objHash)
                .service(TextProxy.service.getCachedTextIfNullDefault(p.service))
                .txid(p.txid)
                .threadName(TextProxy.hashMessage.getCachedTextIfNullDefault(p.threadNameHash))
                .caller(p.caller)
                .gxid(p.gxid)
                .elapsed(p.elapsed)
                .error(TextProxy.error.getCachedTextIfNullDefault(p.error))
                .cpu(p.cpu)
                .sqlCount(p.sqlCount)
                .sqlTime(p.sqlTime)
                .ipAddr(ZZ.ipByteToString(p.ipaddr))
                .allocatedMemory(p.kbytes)
                .internalId(p.userid)
                .userAgent(TextProxy.userAgent.getCachedTextIfNullDefault(p.userAgent))
                .referrer(TextProxy.referrer.getCachedTextIfNullDefault(p.referer))
                .group(TextProxy.group.getCachedTextIfNullDefault(p.group))
                .apicallCount(p.apicallCount)
                .apicallTime(p.apicallTime)
                .countryCode(p.countryCode)
                .city(TextProxy.city.getCachedTextIfNullDefault(p.city))
                .xLogType(XLogTypes.Type.of(p.xType).name())
                .login(TextProxy.login.getCachedTextIfNullDefault(p.login))
                .desc(TextProxy.desc.getCachedTextIfNullDefault(p.desc))
                .hasDump(p.hasDump)
                .text1(p.text1)
                .text2(p.text2)
                .text3(p.text3)
                .text4(p.text4)
                .text5(p.text5)
                .queuingHost(TextProxy.hashMessage.getCachedTextIfNullDefault(p.queuingHostHash))
                .queuingTime(p.queuingTime)
                .queuing2ndHost(TextProxy.hashMessage.getCachedTextIfNullDefault(p.queuing2ndHostHash))
                .queuing2ndTime(p.queuing2ndTime)
                .build();
    }

    private static void preLoadDictionary(XLogPack pack, int serverId) {
        TextLoader loader = new TextLoader(serverId);

        loader.addTextHash(TextTypeEnum.SERVICE, pack.service);
        loader.addTextHash(TextTypeEnum.HASH_MSG, pack.threadNameHash);
        loader.addTextHash(TextTypeEnum.ERROR, pack.error);
        loader.addTextHash(TextTypeEnum.USER_AGENT, pack.userAgent);
        loader.addTextHash(TextTypeEnum.REFERRER, pack.referer);
        loader.addTextHash(TextTypeEnum.GROUP, pack.group);
        loader.addTextHash(TextTypeEnum.CITY, pack.city);
        loader.addTextHash(TextTypeEnum.LOGIN, pack.login);
        loader.addTextHash(TextTypeEnum.DESC, pack.desc);
        loader.addTextHash(TextTypeEnum.HASH_MSG, pack.queuingHostHash);
        loader.addTextHash(TextTypeEnum.HASH_MSG, pack.queuing2ndHostHash);

        loader.loadAll();
    }
}
