#!/usr/bin/env python

#
# original code from 
#    https://github.com/giampaolo/psutil/blob/master/examples/
#
# Copyright (c) 2009, Giampaolo Rodola'. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

"""
A clone of 'netstat'.
"""

import socket
from socket import AF_INET, SOCK_STREAM, SOCK_DGRAM

import psutil

from scouter.lang.pack import *
from scouter.lang.value import *


AD = "-"
AF_INET6 = getattr(socket, 'AF_INET6', object())
proto_map = {
    (AF_INET, SOCK_STREAM): 'tcp',
    (AF_INET6, SOCK_STREAM): 'tcp6',
    (AF_INET, SOCK_DGRAM): 'udp',
    (AF_INET6, SOCK_DGRAM): 'udp6',
}


def main():
    templ = "%-5s %-22s %-22s %-13s %-6s %s"
    print(templ % (
        "Proto", "Local addr", "Remote addr", "Status", "PID", "Program name"))
    for p in psutil.process_iter():
        name = '?'
        try:
            name = p.name
            cons = p.connections(kind='inet')
        except psutil.AccessDenied:
            print(templ % (AD, AD, AD, AD, p.pid, name))
        except psutil.NoSuchProcess:
            continue
        else:
            for c in cons:
                raddr = ""
                laddr = "%s:%s" % (c.laddr)
                if c.raddr:
                    raddr = "%s:%s" % (c.raddr)
                print(templ % (
                    proto_map[(c.family, c.type)],
                    laddr,
                    raddr,
                    c.status,
                    p.pid,
                    name[:15]))
                
def process(param):
    pack = MapPack()
    protoList = pack.newList("Proto")
    localList = pack.newList("LocalAddr")
    remoteList = pack.newList("RemoteAddr")
    statusList = pack.newList("Status")
    pidList = pack.newList("Pid")
    nameList = pack.newList("ProgramName")
    for p in psutil.process_iter():
        name = '?'
        try:
            name = p.name()
            cons = p.connections(kind='inet')
        except psutil.AccessDenied:
            protoList.addStr(AD)
            localList.addStr(AD)
            remoteList.addStr(AD)
            statusList.addStr(AD)
            pidList.addStr(str(p.pid))
            nameList.addStr(name)
        except psutil.NoSuchProcess:
            continue
        else:
            for c in cons:
                raddr = ""
                laddr = "%s:%s" % (c.laddr)
                if c.raddr:
                    raddr = "%s:%s" % (c.raddr)
                protoList.addStr(proto_map[(c.family, c.type)])
                localList.addStr(laddr)
                remoteList.addStr(raddr)
                statusList.addStr(c.status)
                pidList.addStr(str(p.pid))
                nameList.addStr(name[:15])
    return pack
                
if __name__ == '__main__':
#    main()
    pack = process(None)
    print pack
