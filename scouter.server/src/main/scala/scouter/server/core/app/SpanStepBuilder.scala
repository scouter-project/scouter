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
import scouter.server.{Configure, Logger}
import scouter.util.{Hexa32, IPUtil}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object SpanStepBuilder {
    val conf = Configure.getInstance()

    case class SpanRelation(pack: SpanPack) {
        object SpanRelationOrdering extends Ordering[SpanRelation] {
            def compare(element1: SpanRelation, element2: SpanRelation): Int = {
                if (element1.pack.timestamp < element2.pack.timestamp) -1
                else if (element1.pack.timestamp == element2.pack.timestamp) 0
                else 1
            }
        }
        val children: mutable.TreeSet[SpanRelation] = mutable.TreeSet()(SpanRelationOrdering)
    }

    def toSteps(gxid: Long, txid: Long, packList: ListBuffer[SpanPack]): (ListBuffer[StepSingle], SpanPack) = {
        var txidSpanPack: SpanPack = null
        val spanRelationMap = mutable.Map[Long, SpanRelation]()
        val xLoggableSpanRelationMap = mutable.Map[Long, SpanRelation]()
        packList.foreach(pack => {
            if (pack.txid == txid) {
                txidSpanPack = pack
            }
            if (SpanTypes.isXLoggable(pack.spanType)) {
                xLoggableSpanRelationMap.put(pack.txid, SpanRelation(pack))
            } else {
                spanRelationMap.put(pack.txid, SpanRelation(pack))
            }
        })

        packList.foreach(pack => {
            if (pack.caller != 0 && spanRelationMap.contains(pack.caller) && spanRelationMap.contains(pack.txid)) {
                spanRelationMap(pack.caller).children += spanRelationMap(pack.txid)
            }

            if (pack.caller != 0 && xLoggableSpanRelationMap.contains(pack.caller) && spanRelationMap.contains(pack.txid)) {
                xLoggableSpanRelationMap(pack.caller).children += spanRelationMap(pack.txid)
            }
        })

        val arrangedPackList = establishSpanPackHierarchy(txid, xLoggableSpanRelationMap(txid))

        if (arrangedPackList.isEmpty) {
            return (null, null)
        }

        val initialTime = arrangedPackList(0).timestamp
        val stepList = arrangedPackList.zipWithIndex.map {
            case (span, index) => spanToStep(span, index, initialTime)
        }

        if (conf._trace) {
            Logger.println(s"=========== all span pack [$txid] =============")
            packList.foreach(pack => Logger.println(s"[all span pack][$txid] : $pack"))

            Logger.println(s"=========== span steps to profile [$txid] =============")
            stepList.foreach(s => Logger.println(s"[span steps to profile][$txid] : " +
                    spanStepDebugString(s.asInstanceOf[CommonSpanStep])))
        }

        val txidStepMap = stepList.map(step => (step.spanPack.txid, step)).toMap
        stepList.foreach(step => {
            step.parent = getParentIndex(step, txidStepMap)
        })

        (stepList, xLoggableSpanRelationMap(txid).pack)
    }


    private def establishSpanPackHierarchy(startTxid: Long, spanRelation: SpanRelation): ListBuffer[SpanPack] = {
        val spanPacks = mutable.ListBuffer[SpanPack]()
        val isInitial = startTxid == spanRelation.pack.txid
        var profileEnd = !isInitial && SpanTypes.isBoundary(spanRelation.pack.spanType)
        spanPacks += spanRelation.pack
        if (!profileEnd) {
            for (childSpanRelation <- spanRelation.children) {
                spanPacks ++= establishSpanPackHierarchy(startTxid, childSpanRelation)
            }
        }
        spanPacks
    }

    private def getParentIndex(step: StepSingle, mapByTxid: Map[Long, StepSingle]): Int = {
        if (mapByTxid.contains(step.spanPack.caller)) {
            val parentStep = mapByTxid(step.spanPack.caller)
            parentStep.index
        } else {
            -1
        }
    }

    private def spanToStep(span: SpanPack, index: Int, initialTime: Long): StepSingle = {
        span match {
            case s if SpanTypes.isXLoggable(s.spanType) => {
                val step = SpanStep.fromPack(span, index, initialTime)
                step.nameDebug = TextRD.getString("00000000", TextTypes.SERVICE, s.name)
                step
            }
            case s if SpanTypes.isApiable(s.spanType) => {
                val step = SpanCallStep.fromPack(span, index, initialTime)
                step.nameDebug = TextRD.getString("00000000", TextTypes.SERVICE, s.name)
                step
            }
            case s => {
                val step = SpanStep.fromPack(span, index, initialTime)
                step.nameDebug = TextRD.getString("00000000", TextTypes.SERVICE, s.name)
                step
            }
        }
    }

    def spanStepDebugString(step: CommonSpanStep): String = {
        val stepString =
            s"""SpanStep{nameDebug='${step.nameDebug}'
            , hash=${TextRD.getString("00000000", TextTypes.SERVICE, step.hash)}
            , hash=${step.hash}
            , parent=${step.parent}
            , index=${step.index}
            , start_time=${step.start_time}
            , elapsed=${step.elapsed}
            , error=${step.error}
            , timestamp=${step.timestamp}
            , spanType=${step.spanType}
            , localEndpointServiceName=${TextRD.getString("00000000", TextTypes.OBJECT, step.localEndpointServiceName)}
            , localEndpointIp=${IPUtil.toString(step.localEndpointIp)}
            , localEndpointPort=${step.localEndpointPort}
            , remoteEndpointServiceName=${TextRD.getString("00000000", TextTypes.OBJECT, step.remoteEndpointServiceName)}
            , remoteEndpointIp=${IPUtil.toString(step.remoteEndpointIp)}
            , remoteEndpointPort=${step.remoteEndpointPort}
            , debug=${step.debug}
            , shared=${step.shared}
            , annotationTimestamps=${step.annotationTimestamps}
            , annotationValues=${step.annotationValues}
            , tags=${step.tags}
           }""".split("\n")
                    .filter(_.nonEmpty)
                    .map(_.trim)
                    .mkString("", "", "")

        val span = step.spanPack
        val spanString = if (span == null) "" else
            s"""
            SpanPack{gxid=${span.gxid}-${Hexa32.toString32(span.gxid)},
            , txid=${span.txid}-${Hexa32.toString32(span.txid)},
            , caller=${span.caller}-${Hexa32.toString32(span.caller)},
            , name=${TextRD.getString("00000000", TextTypes.SERVICE, span.name)}
            , objHash=${span.objHash}
            }'""".split("\n")
                    .filter(_.nonEmpty)
                    .map(_.trim)
                    .mkString("", "", "")

        s"$stepString, span=$spanString"
    }
}

