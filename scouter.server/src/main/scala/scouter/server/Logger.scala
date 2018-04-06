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

package scouter.server;

import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import scala.util.control.Breaks._
import scouter.server.util.ThreadScala
import scouter.util.DateUtil
import scouter.util.StringLongLinkedMap
import scouter.util.ThreadUtil
import scouter.util.FileUtil
import scouter.util.CompareUtil

object Logger {
    private val serverLogPrefix = "server";
    private val serverLogPrefixWithHyphen = serverLogPrefix + "-";
    private val requestLogPrefixWithHyphen = "request-";

    private val lastLog = new StringLongLinkedMap().setMax(1000);

    def trace(message: Any) {
        if(conf._trace) {
            printlnInternal(DateUtil.datetime(System.currentTimeMillis()) + " [TRACE] " + message);
        }
    }

    def println(message: Any) {
        Logger.printlnInternal(DateUtil.datetime(System.currentTimeMillis()) + " " + message);
    }

    def println(id: String, message: Any) {
        if (checkOk(id, 0) == false) {
            return ;
        }
        printlnInternal(DateUtil.datetime(System.currentTimeMillis()) + " [" + id + "] " + message);
    }

    def println(id: String, sec: Int, message: Any) {
        if (checkOk(id, sec) == false) {
            return ;
        }
        printlnInternal(DateUtil.datetime(System.currentTimeMillis()) + " [" + id + "] " + message);
    }

    def println(id: String, sec: Int, message: Any, t: Throwable) {
        if (checkOk(id, sec) == false) {
            return ;
        }
        printlnInternal(DateUtil.datetime(System.currentTimeMillis()) + " [" + id + "] " + message);
        printlnInternal(ThreadUtil.getStackTrace(t));
    }

    def printStackTrace(t: Throwable): Unit = {
        println(getCallStack(t));
    }

    def printStackTrace(id: String, t: Throwable): Unit = {
        println(id, getCallStack(t));
    }

    def getCallStack(t: Throwable): String = {
        val sw = new StringWriter();
        val pw = new PrintWriter(sw);
        try {
            t.printStackTrace(pw);
            return sw.toString();
        } finally {
            pw.close();
        }
    }

    private def checkOk(id: String, sec: Int): Boolean = {
        if (Configure.getInstance().mgr_log_ignore_ids.hasKey(id))
            return false;
        if (sec > 0) {
            val last = lastLog.get(id);
            val now = System.currentTimeMillis();
            if (now < last + sec * 1000)
                return false;
            lastLog.put(id, now);
        }
        return true;
    }

    var pw: PrintWriter = null;
    private var lastDataUnit = 0L;
    private var lastDir = ".";
    private var lastFileRotation = true;

    private def printlnInternal(msg: String) {
        try {
            if (pw != null) {
                if(conf._trace) {
                    System.out.println(msg);
                }
                pw.println(msg);
                pw.flush();
                return ;
            }
            openFile();
            if (pw == null) {
                System.out.println(msg);
            } else {
                if(conf._trace) {
                    System.out.println(msg);
                }
                pw.println(msg);
                pw.flush();
            }
        } catch {
            case e: Exception =>
                FileUtil.close(pw);
                pw = null
                System.out.println(msg);
        }
    }

    ThreadScala.startDaemon("scouter.server.Logger") {
        var last = System.currentTimeMillis();
        while (true) {
            val now = System.currentTimeMillis();
            if (now > last + DateUtil.MILLIS_PER_HOUR) {
                last = now;
                clearOldLog();
            }

            if (lastDataUnit != DateUtil.getDateUnit()) {
                FileUtil.close(pw);
                pw = null
                lastDataUnit = DateUtil.getDateUnit();
            }
            ThreadUtil.sleep(5000);
        }
    }
    val conf = Configure.getInstance()
    ConfObserver.put("Logger", new Runnable() {
        override def run() {

            if (CompareUtil.equals(lastDir, conf.log_dir) == false || lastFileRotation != conf.log_rotation_enabled) {
                FileUtil.close(pw)
                pw = null
                lastDir = conf.log_dir;
                lastFileRotation = conf.log_rotation_enabled;
            }
        }
    });

    private def openFile() {
        this.synchronized {
            if (pw == null) {
                lastDataUnit = DateUtil.getDateUnit();
                lastDir = conf.log_dir;
                lastFileRotation = conf.log_rotation_enabled;

                new File(lastDir).mkdirs();
                if (conf.log_rotation_enabled) {
                    val fw = new FileWriter(new File(conf.log_dir, serverLogPrefixWithHyphen + DateUtil.yyyymmdd() + ".log"), true);
                    pw = new PrintWriter(fw);
                } else {
                    pw = new PrintWriter(new File(conf.log_dir, serverLogPrefix + ".log"));
                }
                lastDataUnit = DateUtil.getDateUnit();
            }
        }
    }

    protected def clearOldLog() {
        if (conf.log_rotation_enabled == false)
            return
        if (conf.log_keep_days <= 0)
            return
        val nowUnit = DateUtil.getDateUnit()
        val dir = new File(conf.log_dir)
        val files = dir.listFiles()

        for (i <- 0 to files.length - 1) {
            breakable {
                if (files(i).isDirectory()) {
                    break
                }

                val name = files(i).getName()
                var prefix : String = null

                if(name.startsWith(serverLogPrefixWithHyphen)) {
                    prefix = serverLogPrefixWithHyphen;
                } else if(name.startsWith(requestLogPrefixWithHyphen)) {
                    prefix = requestLogPrefixWithHyphen;
                } else {
                    break
                }

                val x = name.lastIndexOf('.')
                if (x < 0) {
                    break
                }
                val date = name.substring(prefix.length(), x)
                if (date.length() != 8) {
                    break
                }
                try {
                    val d = DateUtil.yyyymmdd(date)
                    val fileUnit = DateUtil.getDateUnit(d)
                    if (nowUnit - fileUnit > conf.log_keep_days) {
                        files(i).delete()
                        Logger.println("[scouter] delete log file : " + files(i).getAbsolutePath)
                    }
                } catch {
                    case e: Exception =>
                }
            }
        }

    }

}
