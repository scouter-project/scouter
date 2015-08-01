# -*- coding: UTF-8 -*-

'''
 Copyright (c) 2015 Scouter Project.

 Licensed under the Apache License, Version 2.0 (the "License"); 
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License. 
'''

from scouter.lang.utility import *
from scouter.lang.request  import *
from scouter.lang.pack  import *
from scouter.lang.inout  import *

import scouter.host.top
import scouter.host.process_detail
import scouter.host.disk_usage
import scouter.host.netstat
import scouter.host.who
import scouter.host.meminfo

import datetime, time
import sys, getopt
import platform, socket
import datetime, time

import threading
import psutil


lastTimeDict = dict()

def getInterval(key, interval):
    etime = datetime.datetime.now()
    if (lastTimeDict.has_key(key)) :
        stime = lastTimeDict.get(key)
        lastTimeDict[key] = etime
        return diffSeconds(stime, etime)
    else :
        lastTimeDict[key] = etime
        return interval

def process(pack, default_interval=2):
   
    pack.putFloat("Cpu" , psutil.cpu_percent(0,False))
    
    mem = psutil.virtual_memory()
    
    used = mem.total - mem.available
    pack.putFloat("Mem", mem.percent)
    pack.putFloat("MemA", mem.available / 1024.0 / 1024.0)
    pack.putInt("MemU", int(used / 1024 / 1024))
    pack.putInt("MemT", int(mem.total / 1024 / 1024))
    
    swap = psutil.swap_memory()
    pack.putFloat("Swap", swap.percent)
    pack.putInt("SwapU", int(swap.used / 1024 / 1024))
    pack.putInt("SwapT", int(swap.total / 1024 / 1024))
    
    interval = getInterval("disk", default_interval)
    if interval > 0:
        disk = psutil.disk_io_counters(perdisk=False)
        calc(pack, "ReadCount", delta("ReadCount", disk.read_count) / interval)
        calc(pack, "WriteCount", delta("WriteCount", disk.write_count) / interval)
        calc(pack, "ReadBytes", delta("ReadBytes", disk.read_bytes) / interval)
        calc(pack, "WriteBytes", delta("WriteBytes", disk.write_bytes) / interval)
        calc(pack, "ReadTime", delta("ReadTime", disk.read_time) / interval)
        calc(pack, "WriteTime", delta("WriteTime", disk.write_time) / interval)
   
    interval = getInterval("net", default_interval)
    if interval > 0:
        net = psutil.net_io_counters(pernic=False)
        calc(pack, "PacketsSent", delta("PacketsSent", net.packets_sent) / interval)
        calc(pack, "PacketsRecv", delta("PacketsRecv", net.packets_recv) / interval)
        calc(pack, "BytesSent", delta("BytesSent", net.bytes_sent) / interval)
        calc(pack, "BytesRecv", delta("BytesRecv", net.bytes_recv) / interval)
        calc(pack, "ErrIn", delta("ErrIn", net.errin) / interval)
        calc(pack, "ErrOut", delta("ErrOut", net.errout) / interval)
        calc(pack, "DropIn", delta("DropIn", net.dropin) / interval)
        calc(pack, "DropOut", delta("DropOut", net.dropout) / interval)


def help():
    helpText = """hostperf.py [--scouter_name] [--host host] [--port port] [--debug] [--help]
    --scouter_name : set the custom object name
    --host    host : hostname or ip (127.0.0.1)
    --port    port : port (6100)
    --debug        : debug
    --help         : help
    """
    print helpText
    sys.exit()
    
server_ip = "127.0.0.1"
server_port = 6100 
debug = False
so_timeout=60000

def init(args):
    global debug
    global server_ip
    global server_port
     
    logo()
    opts, args = getopt.getopt(args, "h", ["scouter_name=","host=","port=","debug","help"])
    for opt in opts:
        if opt[0] == "--help" or opt[0] == "-h":
            help()
        elif opt[0] == "--host": 
            server_ip = opt[1]
        elif opt[0] == "--port":
            server_port = int(opt[1])
        elif opt[0] == "--debug":
            debug = True
        elif opt[0] == "--scouter_name": 
            setObjname(opt[1])

def h_ignore(param):
    m = MapPack()
    m.putValue('msg', TextValue('ignored command'))
    return m

def h_env(param):
    import os
    env = os.environ
    m = MapPack()
    for key, value in env.iteritems():
        m.putStr(key, value)
    return m

def openReqServer():
    handlers = dict()
    handlers["OBJECT_ENV"]=h_env
    handlers["OBJECT_RESET_CACHE"]=h_ignore
    handlers["HOST_TOP"]=scouter.host.top.process
    handlers["HOST_PROCESS_DETAIL"]=scouter.host.process_detail.process
    handlers["HOST_DISK_USAGE"]=scouter.host.disk_usage.process
    handlers["HOST_NET_STAT"]=scouter.host.netstat.process
    handlers["HOST_WHO"]=scouter.host.who.process
    handlers["HOST_MEMINFO"]=scouter.host.meminfo.process
    handlers["KEEP_ALIVE"]=h_ignore
    startReqHandler(server_ip, server_port, handlers)
 

def sendObjectPack(sock):
    objPack = ObjectPack()
    objPack.objName = objname()
    objPack.objHash = binascii.crc32(objPack.objName)
    objPack.objType = objtype()
    objPack.address = getLocalAddr()
    objPack.version = "0.2.0"
    out = DataOutputX()
    out.writePack(objPack)
    sock.sendto("CAFE" + out.toByteArray(), (server_ip, server_port))
    

REALTIME=1
FIVE_MIN=3
    
def sendPerfCounterPack(sock, interval):
     pack = PerfCounterPack()
     pack.objName = objname()
     pack.timeType = REALTIME
     process(pack, interval)
     if debug == True:
         print pack
     out = DataOutputX()
     out.writePack(pack)
     sock.sendto("CAFE" + out.toByteArray(), (server_ip, server_port))
    
def main():
    init(sys.argv[1:])
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    
    
    thread1 = threading.Thread(target=openReqServer)
    thread1.setDaemon(True)
    thread1.start()    
    #thread2 = threading.Thread(target=openReqServer)
    #thread2.setDaemon(True)
    #thread2.start()    

    skip=2
    interval = 2
    objPackSend = bool(1)
    while 1:
        sendObjectPack(sock)
        sendPerfCounterPack(sock, interval)
        time.sleep(interval)

if __name__ == '__main__':
    main()
