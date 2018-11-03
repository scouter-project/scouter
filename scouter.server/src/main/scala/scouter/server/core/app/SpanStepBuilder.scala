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

package scouter.server.core.app

import scouter.lang.TextTypes
import scouter.lang.pack.{SpanPack, SpanTypes}
import scouter.lang.step._
import scouter.server.db.TextRD

import scala.annotation.tailrec
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object SpanStepBuilder {

    def toSteps(gxid: Long, txid: Long, packList: List[SpanPack]): (List[Step], SpanPack) = {
        var txidSpanPack: SpanPack = null
        val parentMap = mutable.Map[Long, Long]()
        val levelMap = mutable.Map[Long, Int]()

        packList.foreach(pack => {
            parentMap.put(pack.txid, pack.caller)
            if(pack.txid == txid) {
                txidSpanPack = pack
            }
        })

        val stepList = packList.sortBy(_.timestamp).zipWithIndex.map {
            case (span, index) => spanToStep(span, index)
        }

        val (filteredList, mapByTxid) = filter4Txid(stepList, txid)
        filteredList.foreach(step => {
            step.parent = getParentIndex(step, mapByTxid)
            //step.caller = getValidCaller(step, mapByTxid)
        })

        (filteredList, txidSpanPack)
    }

    private def getParentIndex(step: StepSingle, mapByTxid: Map[Long, StepSingle]): Int = {
        if(mapByTxid.contains(step.spanPack.caller)) {
            val parentStep = mapByTxid(step.spanPack.caller)
            parentStep.index
        } else {
            -1
        }
    }

    @tailrec
    private def getValidCaller(step: StepSingle, mapByTxid: Map[Long, StepSingle]): Long = {
        if(mapByTxid.contains(step.spanPack.caller)) {
            val parentStep = mapByTxid(step.spanPack.caller)
            if(SpanTypes.isXLoggable(parentStep.spanPack.spanType)) {
                step.spanPack.caller
            } else {
                getValidCaller(parentStep, mapByTxid)
            }
        } else {
            0
        }
    }

    private def filter4Txid(stepList: List[StepSingle], txid: Long): (List[StepSingle], Map[Long, StepSingle]) = {
        val adjustList = ListBuffer[StepSingle]()
        val filteredMapByTxid = mutable.Map[Long, StepSingle]()
        val ignoreIds = mutable.Set[Long]()
        val validParentIds = mutable.Set[Long]()

        var txStartTime = 0L
        var txIndex = 0

        stepList.foreach(step => {
            if (txStartTime == 0) {
                if (step.spanPack.txid == txid && SpanTypes.isXLoggable(step.spanPack.spanType)) {
                    txStartTime = step.spanPack.timestamp
                    txIndex = step.index
                    step.start_time = 0
                    step.index = 0
                    adjustList += step
                    filteredMapByTxid.put(step.spanPack.txid, step)
                    validParentIds.add(step.spanPack.txid)
                }
            } else {
                if (step.spanPack.txid != txid &&
                        (SpanTypes.isXLoggable(step.spanPack.spanType) || ignoreIds.contains(step.spanPack.caller))) {

                    ignoreIds.add(step.spanPack.txid)

                } else if (validParentIds.contains(step.spanPack.caller)) {
                    validParentIds.add(step.spanPack.txid)
                    step.start_time = (step.spanPack.timestamp - txStartTime).toInt
                    step.index = step.index - txIndex
                    adjustList += step
                    filteredMapByTxid.put(step.spanPack.txid, step)
                }
            }
        })

        (adjustList.toList, filteredMapByTxid.toMap)
    }

    private def spanToStep(span: SpanPack, index: Int): StepSingle = {
        span match {
            case s if SpanTypes.isXLoggable(s.spanType) => {
                val step = SpanStep.fromPack(span, index)
                step.nameDebug = TextRD.getString("20181103", TextTypes.SERVICE, s.name)
                step
            }
            case s if SpanTypes.isApiable(s.spanType) => {
                val step = SpanCallStep.fromPack(span, index)
                step.nameDebug = TextRD.getString("20181103", TextTypes.SERVICE, s.name)
                step
            }
            case s => {
                val step = SpanStep.fromPack(span, index)
                step.nameDebug = TextRD.getString("20181103", TextTypes.SERVICE, s.name)
                step
            }
        }
    }
}

