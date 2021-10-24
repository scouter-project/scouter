/*
*  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 *
 */
package scouter.server.core;
import scouter.lang.constants.ScouterConstants.{POD_NAME, USE_KUBE_SEQ}

import java.util.{ArrayList, Enumeration, HashSet, List}
import scouter.lang.{AlertLevel, TextTypes}
import scouter.lang.pack.{AlertPack, MapPack, ObjectPack, TextPack}
import scouter.server.{Configure, CounterManager, Logger}
import scouter.server.core.cache.{AlertCache, CommonCache}
import scouter.server.db.{AlertWR, ObjectRD, ObjectWR}
import scouter.server.plugin.PlugInManager
import scouter.server.util.{EnumerScala, ThreadScala}
import scouter.util.{CompareUtil, DateUtil, HashUtil, IntKeyMap, StringUtil}
import scouter.lang.counters.{CounterConstants, CounterEngine}
import scouter.net.RequestCmd
import scouter.server.kube.PodSeqManager
import scouter.server.netio.AgentCall

import java.util.concurrent.ConcurrentHashMap
import scala.collection.JavaConversions._

object AgentManager {
    private val counterEngine = scouter.server.CounterManager.getInstance().getCounterEngine();
    private var primaryObjCount = 0;
    private val objMap = new ObjectMap();

    private def read() {
        val list = ObjectRD.getObjectList(DateUtil.yyyymmdd());
        objMap.putAll(list);
    }

    ThreadScala.startDaemon("scouter.server.core.AgentManager", { CoreRun.running }, 1000) {
        val now = System.currentTimeMillis();
        val deadtime = Configure.getInstance().object_deadtime_ms;
        val zipkinDeadTime = Configure.getInstance().object_zipkin_deadtime_ms;
        val en = objMap.objects();
        var primaryObjCount = 0;

        while (en.hasMoreElements()) {
            val objPack = en.nextElement();

            if(!CounterConstants.BATCH.equals(objPack.objType)){
                var adjustDeadTime = 30000;
                if (CounterConstants.ZIPKIN.equals(objPack.objType) || objPack.objType.startsWith(CounterConstants.ZIPKIN_TYPE_PREFIX)) {
                    adjustDeadTime = if (objPack.getDeadTime() == 0) zipkinDeadTime else objPack.getDeadTime();
                } else {
                    adjustDeadTime = if (objPack.getDeadTime() == 0) deadtime else objPack.getDeadTime();
                }

                if (now > objPack.wakeup + adjustDeadTime) {
                    inactive(objPack.objHash);
                } else if (counterEngine.isPrimaryObject(objPack.objType)) {
                    primaryObjCount += 1;
                }
            }
        }

        this.primaryObjCount = primaryObjCount;
    }

    def isActive(agentKey: Int): Boolean = {
        val objPack = objMap.getObject(agentKey);
        if (objPack == null) false else objPack.alive;
    }

    def active(p: ObjectPack) {
        p.allowProceed = true;
        if (p.objHash == 0) {
            p.objHash = HashUtil.hash(p.objName);
        }
        CounterManager.getInstance().addObjectTypeIfNotExist(p);
        PlugInManager.active(p);

        val useKubeSeq = p.tags.getBoolean(USE_KUBE_SEQ)
        val podName = p.tags.getText(POD_NAME)
        if (useKubeSeq && StringUtil.isNotEmpty(podName)) {
            val podSeqManager = PodSeqManager.getInstance(podName)
            podSeqManager.applyPodSeq(p)
            PodSeqManager.pushSeqToAgent(p)
        }
        if (!p.allowProceed) {
            return;
        }

        var objPack = objMap.getObject(p.objHash);
        if (objPack == null) {
            objPack = p;
            objPack.wakeup();
            //
            objMap.put(objPack);
            procObjName(objPack);
            ObjectWR.add(objPack);
            Logger.println("S104", "New " + objPack);

        } else {
            if (!objPack.alive && counterEngine.isPrimaryObject(objPack.objType)) {
                alertReactiveObject(objPack);
            }

            var save = false;
            if (DateUtil.getDateUnit(objPack.wakeup) != DateUtil.getDateUnit(System.currentTimeMillis())) {
                objPack.updated = 0;
                save = true;
            }
            objPack.wakeup();
            objPack.tags = p.tags;

            if (!CompareUtil.equals(p.address, objPack.address)) {
                save = true;
            }
            if (!CompareUtil.equals(p.objType, objPack.objType)) {
                save = true;
            }
            if (!CompareUtil.equals(p.version, objPack.version)) {
                save = true;
            }
            if (save) {
                objPack.updated += 1
                if (objPack.updated % 20 == 0) {
                    alertTooManyChange(objPack);
                }
                p.updated = objPack.updated;
                p.wakeup();
                objMap.put(p);
                procObjName(p);
                ObjectWR.add(p);
                Logger.println("S105", "Update " + p);
            }
        }
    }

    def getNewPodSeq(podName: String): String = {
        return ""
    }

    private def alertTooManyChange(objPack: ObjectPack) {
        val p = new AlertPack();
        p.level = AlertLevel.INFO;
        p.title = "MAYBE_OBJ_DUP";
        p.message = "Maybe duplicated objNames!! Please check " + objPack;
        p.time = System.currentTimeMillis();
        p.objType = "scouter";
        AlertCache.put(p);
    }

    private def alertInactiveObject(objPack: ObjectPack) {
        val p = new AlertPack();
        p.level = Configure.getInstance().object_inactive_alert_level.asInstanceOf[Byte];
        p.objHash = objPack.objHash;
        p.title = "INACTIVE_OBJECT";
        p.message = objPack.objName + " is not running. " + objPack;
        p.time = System.currentTimeMillis();
        p.objType = "scouter";
        AlertCore.add(p);
    }

    private def alertReactiveObject(objPack: ObjectPack) {
        val p = new AlertPack();
        p.level = Configure.getInstance().object_inactive_alert_level.asInstanceOf[Byte];
        p.objHash = objPack.objHash;
        p.title = "ACTIVATED_OBJECT";
        p.message = objPack.objName + " is running now. " + objPack;
        p.time = System.currentTimeMillis();
        p.objType = "scouter";
        AlertCore.add(p);
    }

    private def procObjName(objPack: ObjectPack) {
        val tp = new TextPack();
        tp.xtype = TextTypes.OBJECT;
        tp.hash = objPack.objHash;
        tp.text = objPack.objName;
        TextCore.add(tp);
    }

    def getAgentName(objHash: Int): String = {
        val objPack = objMap.getObject(objHash);
        if (objPack == null) null else objPack.objName
    }

    def getAgent(objHash: Int): ObjectPack = {
        return objMap.getObject(objHash);
    }

    def getAgent(objName: String): ObjectPack = {
        return objMap.getObject(objName)
    }

    def inactive(objHash: Int) {
        val objPack = objMap.getObject(objHash);
        if (objPack != null && objPack.alive) {
            objPack.alive = false;
            val obj = counterEngine.getObjectType(objPack.objType);
            if (obj != null && obj.isSubObject() == false) {
                alertInactiveObject(objPack);
            }
            if (objPack.podName != null) {
                PodSeqManager.getInstance(objPack.podName).objectInactivated(objPack.hostName)
            }
        }
    }

    def clearInactive() {
        val death = new ArrayList[ObjectPack]();
        val itr = objMap.objects();
        while (itr.hasMoreElements()) {
            val objPack = itr.nextElement();
            if (objPack.alive == false)
                death.add(objPack);
        }
        EnumerScala.foreach(death.iterator(), (o: ObjectPack) => objMap.remove(o.objHash))
    }

    def getLiveObjHashList(): List[Int] = {
        val agents = new ArrayList[Int]();
        try {
            val itr = objMap.objects();
            while (itr.hasMoreElements()) {
                val a = itr.nextElement();
                if (a.alive) {
                    agents.add(a.objHash);
                }
            }
        } catch {
            case e: Exception =>
        }
        return agents;
    }

    def getObjHashList(): List[Int] = {
        val agents = new ArrayList[Int]();
        val itr = objMap.objects();
        while (itr.hasMoreElements()) {
            val a = itr.nextElement();
            agents.add(a.objHash);
        }
        return agents;
    }

    def getLiveObjHashList(objType: String): List[Int] = {
        val agents = new ArrayList[Int]();
        val itr = objMap.enumTypeObject(objType);
        while (itr.hasMoreElements()) {
            val a = itr.nextElement();
            if (a.alive) {
                agents.add(a.objHash);
            }
        }
        return agents;
    }

    def getObjHashList(objType: String): List[Int] = {
        val agents = new ArrayList[Int]();
        val itr = objMap.enumTypeObject(objType);
        while (itr.hasMoreElements()) {
            val a = itr.nextElement();
            agents.add(a.objHash);
        }
        return agents;
    }

    def getObjHashListAsString(objType: String): String = {
        return getObjHashList(objType).mkString(",")
    }

    def getObjList(objType: String): List[ObjectPack] = {
        val agents = new ArrayList[ObjectPack]();
        val itr = objMap.enumTypeObject(objType);
        while (itr.hasMoreElements()) {
            val a = itr.nextElement();
            agents.add(a);
        }
        return agents;
    }

    def filter(word: String): List[Int] = {
        if ("*".equals(word)) return getLiveObjHashList()
        if (isObjType(word) == true) {
            return getObjHashList(word)
        } else {
            val agents = new ArrayList[Int]();
            val itr = objMap.objects();
            while (itr.hasMoreElements()) {
                val a = itr.nextElement();
                if (a.objName.indexOf(word) >= 0) {
                    agents.add(a.objHash);
                }
            }
            return agents;
        }
    }

    def isObjType(word: String) = objMap.getTypeObjects(word) != null

    def getObjPacks(): Enumeration[ObjectPack] = {
        return objMap.objects();
    }

    def getCurrentObjects(objType: String): MapPack = {
        val m = new MapPack();
        val objTypeLv = m.newList("objType");
        val objHashLv = m.newList("objHash");
        val en = objMap.enumTypeObject(objType);
        while (en.hasMoreElements()) {
            val obj = en.nextElement();
            objTypeLv.add(obj.objType);
            objHashLv.add(obj.objHash);
        }
        return m;
    }

    def getDailyObjects(date: String, objType: String): MapPack = {
        val key = "DailyObjects:" + date + ":" + objType;
        var m = CommonCache.get(key).asInstanceOf[MapPack]
        if (m != null)
            return m;
        val list = ObjectRD.getObjectList(date);
        m = new MapPack();
        val objTypeLv = m.newList("objType");
        val objHashLv = m.newList("objHash");
        val en = list.iterator();
        while (en.hasNext()) {
            val obj = en.next();
            if (obj.objType.equals(objType)) {
                objTypeLv.add(obj.objType);
                objHashLv.add(obj.objHash);
            }
        }
        CommonCache.put(key, m, 2000);
        return m;
    }

    def getPeriodicObjects(sDate: String, eDate: String, objType: String): MapPack = {
        val key = "PeriodicObjects:" + sDate + eDate + ":" + objType;
        var m = CommonCache.get(key).asInstanceOf[MapPack];
        if (m != null)
            return m;
        val stime = DateUtil.yyyymmdd(sDate);
        val etime = DateUtil.yyyymmdd(eDate);
        val objSet = new HashSet[ObjectPack]();
        var date = stime;
        while (date <= etime) {
            val d = DateUtil.yyyymmdd(date);
            val list = ObjectRD.getObjectList(d);
            objSet.addAll(list);
            date += DateUtil.MILLIS_PER_DAY
        }
        m = new MapPack();
        val objTypeLv = m.newList("objType");
        val objHashLv = m.newList("objHash");
        val itr = objSet.iterator();
        while (itr.hasNext()) {
            val obj = itr.next();
            if (objType.equals(obj.objType)) {
                objTypeLv.add(obj.objType);
                objHashLv.add(obj.objHash);
            }
        }
        CommonCache.put(key, m, 2000);
        return m;
    }

    def removeAgents(objHashList: List[Int], permanent: Boolean) {
        EnumerScala.foreach(objHashList.iterator(), (objHash: Int) => {
            objMap.remove(objHash);
            if (permanent) {
                ObjectWR.remove(objHash);
            }
        })
    }

    def getPrimaryObjCount() = this.primaryObjCount
}
