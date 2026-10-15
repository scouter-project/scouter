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
 */
package scouter.server.core

import java.io.File
import java.util.LinkedHashMap

import scouter.server.{Configure, Logger}
import scouter.util.{FileUtil, StringUtil}

import scala.collection.mutable.ArrayBuffer

/**
 * Central store of the reject(block) control.
 *
 *  - policy      : the globally managed rule set, pushed down to every agent
 *  - blocks      : entries currently blocked. released only by an operator (no TTL)
 *  - detections  : violations reported by agents, kept as candidates for review
 *
 * Version numbers let the broadcaster send the full payload only when something
 * actually changed, so the steady-state sync traffic is a few dozen bytes per agent.
 */
object RejectControlStore {

  case class BlockEntry(ip: String, url: String, reason: String,
                        blockedTime: Long, blockedBy: String, detectCount: Long)

  case class DetectEntry(ip: String, url: String, reason: String,
                         firstTime: Long, lastTime: Long, count: Long, objNames: String)

  private val conf = Configure.getInstance()

  private val blocks = new LinkedHashMap[String, BlockEntry]()
  private val detections = new LinkedHashMap[String, DetectEntry]()
  private var policy = new LinkedHashMap[String, String]()

  @volatile private var _policyVersion: Long = 0
  @volatile private var _blockVersion: Long = 0

  private val storeFile = new File(conf.db_dir, "reject_control.dat")

  load()

  def policyVersion: Long = _policyVersion

  def blockVersion: Long = _blockVersion

  private def keyOf(ip: String, url: String): String =
    StringUtil.trimToEmpty(ip) + "\t" + StringUtil.trimToEmpty(url)

  // ------------------------------------------------------------------ policy

  def getPolicy(): LinkedHashMap[String, String] = blocks.synchronized {
    new LinkedHashMap[String, String](policy)
  }

  def setPolicy(newPolicy: java.util.Map[String, String]) {
    blocks.synchronized {
      policy = new LinkedHashMap[String, String]()
      val itr = newPolicy.entrySet().iterator()
      while (itr.hasNext) {
        val e = itr.next()
        if (StringUtil.isNotEmpty(e.getKey)) {
          policy.put(e.getKey, StringUtil.trimToEmpty(e.getValue))
        }
      }
      _policyVersion += 1
      save()
    }
  }

  // ------------------------------------------------------------------ blocks

  def listBlocks(): java.util.List[BlockEntry] = blocks.synchronized {
    val out = new java.util.ArrayList[BlockEntry]()
    val itr = blocks.values().iterator()
    while (itr.hasNext) out.add(itr.next())
    out
  }

  def blockCount(): Int = blocks.synchronized { blocks.size() }

  /**
   * @return true if newly added. an already blocked entry is left untouched so that
   *         the original block time and operator are preserved.
   */
  def addBlock(ip: String, url: String, reason: String, by: String): Boolean = blocks.synchronized {
    val k = keyOf(ip, url)
    if (StringUtil.isEmpty(ip) || blocks.containsKey(k)) {
      false
    } else if (blocks.size() >= conf.reject_control_max_block_count) {
      Logger.println("S920", "reject control block list is full - " + blocks.size())
      false
    } else {
      val d = detections.get(k)
      blocks.put(k, BlockEntry(StringUtil.trimToEmpty(ip), StringUtil.trimToEmpty(url),
        StringUtil.trimToEmpty(reason), System.currentTimeMillis(), StringUtil.trimToEmpty(by),
        if (d == null) 0L else d.count))
      detections.remove(k)
      _blockVersion += 1
      save()
      true
    }
  }

  /** Manual release. There is no TTL - this is the only way an entry leaves the list. */
  def removeBlock(ip: String, url: String): Boolean = blocks.synchronized {
    val removed = blocks.remove(keyOf(ip, url))
    if (removed != null) {
      _blockVersion += 1
      save()
      true
    } else {
      false
    }
  }

  def clearBlocks(): Int = blocks.synchronized {
    val n = blocks.size()
    if (n > 0) {
      blocks.clear()
      _blockVersion += 1
      save()
    }
    n
  }

  def isBlocked(ip: String, url: String): Boolean = blocks.synchronized {
    blocks.containsKey(keyOf(ip, "")) || blocks.containsKey(keyOf(ip, url))
  }

  /** ip / url arrays for the agent payload */
  def blockArrays(): (Array[String], Array[String]) = blocks.synchronized {
    val ips = new ArrayBuffer[String]()
    val urls = new ArrayBuffer[String]()
    val itr = blocks.values().iterator()
    while (itr.hasNext) {
      val b = itr.next()
      ips += b.ip
      urls += b.url
    }
    (ips.toArray, urls.toArray)
  }

  // -------------------------------------------------------------- detections

  def listDetections(): java.util.List[DetectEntry] = blocks.synchronized {
    val out = new java.util.ArrayList[DetectEntry]()
    val itr = detections.values().iterator()
    while (itr.hasNext) out.add(itr.next())
    out
  }

  /**
   * Record a violation reported by an agent and, when configured, promote it
   * to the block list once it has been seen often enough.
   */
  def addDetection(ip: String, url: String, reason: String, time: Long, objName: String) {
    if (!StringUtil.isEmpty(ip)) {
    var promote = false
    blocks.synchronized {
      val k = keyOf(ip, url)
      // skip entries that are already blocked
      if (!blocks.containsKey(k) && !blocks.containsKey(keyOf(ip, ""))) {
        val prev = detections.get(k)
        val merged = if (prev == null) {
          if (detections.size() >= conf.reject_control_max_detection_count) {
            // drop the oldest candidate to keep the list bounded
            val oldest = detections.keySet().iterator()
            if (oldest.hasNext) detections.remove(oldest.next())
          }
          DetectEntry(StringUtil.trimToEmpty(ip), StringUtil.trimToEmpty(url),
            StringUtil.trimToEmpty(reason), time, time, 1L, StringUtil.trimToEmpty(objName))
        } else {
          DetectEntry(prev.ip, prev.url, prev.reason, prev.firstTime, time, prev.count + 1,
            if (prev.objNames.contains(objName)) prev.objNames else prev.objNames + "," + objName)
        }
        detections.put(k, merged)

        if (conf.reject_control_auto_block_enabled
          && merged.count >= conf.reject_control_auto_block_threshold
          && isAutoBlockReason(merged.reason)) {
          promote = true
        }
      }
    }
    if (promote) {
      addBlock(ip, url, reason, "auto")
      Logger.println("S921", "auto blocked by reject control : " + ip + " " + url + " (" + reason + ")")
    }
    }
  }

  private def isAutoBlockReason(reason: String): Boolean = {
    val allowed = StringUtil.split(conf.reject_control_auto_block_reasons, ',')
    var found = false
    if (allowed != null) {
      val target = StringUtil.trimToEmpty(reason)
      var i = 0
      while (i < allowed.length) {
        if (StringUtil.trimToEmpty(allowed(i)).equalsIgnoreCase(target)) found = true
        i += 1
      }
    }
    found
  }

  def clearDetections(): Int = blocks.synchronized {
    val n = detections.size()
    detections.clear()
    n
  }

  // ------------------------------------------------------------- persistence

  private def save() {
    try {
      val sb = new StringBuilder()
      sb.append("#version\t").append(_policyVersion).append('\t').append(_blockVersion).append('\n')
      val pItr = policy.entrySet().iterator()
      while (pItr.hasNext) {
        val e = pItr.next()
        sb.append("P\t").append(e.getKey).append('\t').append(e.getValue).append('\n')
      }
      val bItr = blocks.values().iterator()
      while (bItr.hasNext) {
        val b = bItr.next()
        sb.append("B\t").append(b.ip).append('\t').append(b.url).append('\t')
          .append(b.reason).append('\t').append(b.blockedTime).append('\t')
          .append(b.blockedBy).append('\t').append(b.detectCount).append('\n')
      }
      val dir = storeFile.getParentFile
      if (dir != null && !dir.exists()) dir.mkdirs()
      FileUtil.save(storeFile, sb.toString().getBytes("UTF-8"))
    } catch {
      case t: Throwable => Logger.println("S922", 10, "cannot save reject control store", t)
    }
  }

  private def load() {
    try {
      if (storeFile.canRead) {
      val body = new String(FileUtil.readAll(storeFile), "UTF-8")
      val lines = body.split("\n")
      var i = 0
      while (i < lines.length) {
        val line = lines(i).trim()
        if (line.length > 0) {
          val f = line.split("\t")
          if (line.startsWith("#version") && f.length >= 3) {
            _policyVersion = f(1).toLong
            _blockVersion = f(2).toLong
          } else if ("P".equals(f(0)) && f.length >= 3) {
            policy.put(f(1), f(2))
          } else if ("B".equals(f(0)) && f.length >= 7) {
            blocks.put(keyOf(f(1), f(2)),
              BlockEntry(f(1), f(2), f(3), f(4).toLong, f(5), f(6).toLong))
          }
        }
        i += 1
      }
      Logger.println("S923", "reject control loaded. blocks=" + blocks.size()
        + ", policyVersion=" + _policyVersion + ", blockVersion=" + _blockVersion)
      }
    } catch {
      case t: Throwable => Logger.println("S924", 10, "cannot load reject control store", t)
    }
  }
}
