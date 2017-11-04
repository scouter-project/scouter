package scouter.server.core.app

import scouter.lang.TimeTypeEnum
import scouter.lang.constants.ScouterConstants
import scouter.lang.counters.{CounterConstants, CounterEngine}
import scouter.lang.pack.{MapPack, ObjectPack, PerfCounterPack}
import scouter.net.RequestCmd
import scouter.server.CounterManager
import scouter.server.core.{AgentManager, CoreRun}
import scouter.server.netio.AgentCall
import scouter.server.util.ThreadScala
import scouter.util.RequestQueue

import scala.collection.mutable


/**
  * @author Gun Lee (gunlee01@gmail.com) on 2016. 11. 18.
  */
object ObjectCpuChecker {
    val queue: RequestQueue[ObjectPack] = new RequestQueue(100)

    ThreadScala.startDaemon("scouter.server.app.ObjectCpuChecker.invokeDumpOnAgent", { CoreRun.running }) {
        val objectPack = queue.get()
        val mapPack = new MapPack()

        mapPack.put("objHash", objectPack.objHash)
        mapPack.put(RequestCmd.TRIGGER_DUMP_REASON, RequestCmd.TRIGGER_DUMP_REASON_TYPE_CPU_EXCEEDED)
        AgentCall.call(objectPack, RequestCmd.TRIGGER_THREAD_DUMPS_FROM_CONDITIONS, mapPack)
    }

    class ObjectCpuStatus() {
        var overCount = 0
        var firstOccurenceTime = -1L

        def clear() {
            overCount = 0
            firstOccurenceTime = 0L
        }
    }

    val counterManager = CounterManager.getInstance()
    val objectCpuMap = new mutable.HashMap[String, ObjectCpuStatus]

    /**
      * Check if java process cpu threshold exceeded
      * then ask the java agent for threaddump
      * @param pack
      */
    def checkCpu(pack: PerfCounterPack) {
        //only for realtime data
        if(pack.timetype != TimeTypeEnum.REALTIME) return

        val agent = AgentManager.getAgent(pack.objName)
        if(agent == null) return

        val objType = agent.objType
        if(objType == null) return

        val _objectType = counterManager.getCounterEngine.getObjectType(objType);
        if(_objectType == null) {
            return
        }
        val _objFamily = _objectType.getFamily()
        if(_objFamily == null) {
            return
        }

        val objFamily = _objFamily.getName()

        //javaee family type
        if(CounterConstants.FAMILY_JAVAEE.equals(objFamily)) {
            val cpuStatus = objectCpuMap.getOrElse(pack.objName, new ObjectCpuStatus())
            if(cpuStatus.firstOccurenceTime < 0L) { //initail value : -1L
                objectCpuMap.put(pack.objName, cpuStatus)
            }

            //return if autodump-cpu option is disabled
            if(agent.tags.getBoolean(ScouterConstants.TAG_AUTODUMP_CPU_ENABLED) != true) {
                cpuStatus.clear()
                return
            }

            val duration = agent.tags.getInt(ScouterConstants.TAG_AUTODUMP_CPU_DURATION)
            val threshold = agent.tags.getInt(ScouterConstants.TAG_AUTODUMP_CPU_THRESHOLD)

            //return if the cpu initial measure time is bigger than duration*2
            if(cpuStatus.firstOccurenceTime > 0 && cpuStatus.firstOccurenceTime < pack.time - duration*2) {
                cpuStatus.clear()
                return
            }

            if(!pack.data.containsKey(CounterConstants.PROC_CPU)) return

            //check if CPU threshold is exceeded
            if(threshold < pack.data.getFloat(CounterConstants.PROC_CPU)) {
                cpuStatus.overCount += 1
                if(cpuStatus.firstOccurenceTime <= 0) {
                    cpuStatus.firstOccurenceTime = pack.time
                }
                if(cpuStatus.firstOccurenceTime < pack.time - duration) {
                    cpuStatus.clear()
                    queue.put(agent)
                }
            } else {
                cpuStatus.clear()
            }
        }


    }
}
