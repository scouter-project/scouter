#!/usr/bin/env python

#
# original code from 
#    https://github.com/giampaolo/psutil/blob/master/examples/
#
# Copyright (c) 2009, Giampaolo Rodola'. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

"""
A clone of top / htop.

Author: Giampaolo Rodola' <g.rodola@gmail.com>
"""

import os
import sys
import time

import psutil

from scouter.lang.pack import *
from scouter.lang.value import *


def bytes2human(n):
    """
    >>> bytes2human(10000)
    '9K'
    >>> bytes2human(100001221)
    '95M'
    """
    symbols = ('K', 'M', 'G', 'T', 'P', 'E', 'Z', 'Y')
    prefix = {}
    for i, s in enumerate(symbols):
        prefix[s] = 1 << (i + 1) * 10
    for s in reversed(symbols):
        if n >= prefix[s]:
            value = int(float(n) / prefix[s])
            return '%s%s' % (value, s)
    return "%sB" % n


def poll():
    # sleep some time
    procs = []
    procs_status = {}
    for p in psutil.process_iter():
        try:
            p.dict = p.as_dict(['username', 'nice', 'memory_info',
                                'memory_percent', 'cpu_percent',
                                'cpu_times', 'name', 'status'])
            try:
                procs_status[p.dict['status']] += 1
            except KeyError:
                procs_status[p.dict['status']] = 1
        except psutil.NoSuchProcess:
            pass
        else:
            procs.append(p)

    # return processes sorted by CPU percent usage
    processes = sorted(procs, key=lambda p: p.dict['cpu_percent'], reverse=True)
    return (processes, procs_status)

def extractValues(procs, procs_status):
    """Print results on screen by using curses."""
    pack = MapPack()
    pidLv = pack.newList("PID")
    userLv = pack.newList("USER")
    niLv = pack.newList("NI")
    virtLv = pack.newList("VIRT")
    resLv = pack.newList("RES")
    cpuLv = pack.newList("CPU")
    memLv = pack.newList("MEM")
    timeLv = pack.newList("TIME")
    nameLv = pack.newList("NAME")
    
    for p in procs:
        # TIME+ column shows process CPU cumulative time and it
        # is expressed as: "mm:ss.ms"
        if p.dict['cpu_times'] is not None:
            ctime = int(round(sum(p.dict['cpu_times']) * 1000))
            #ctime = timedelta(seconds=sum(p.dict['cpu_times']))
            #ctime = "%s:%s.%s" % (ctime.seconds // 60 % 60,
            #                     str((ctime.seconds % 60)).zfill(2),
            #                      str(ctime.microseconds)[:2])
        else:
            ctime = 0
        if p.dict['memory_percent'] is not None:
            p.dict['memory_percent'] = round(p.dict['memory_percent'], 1)
        else:
            p.dict['memory_percent'] = 0
        if p.dict['cpu_percent'] is None:
            p.dict['cpu_percent'] = 0
        if p.dict['username']:
            username = p.dict['username'][:8]
        else:
            username = ""
        pidLv.putInt(p.pid)
        userLv.addStr(username)
        niLv.putInt(p.dict['nice'])
        virtLv.putInt(getattr(p.dict['memory_info'], 'vms', 0))
        resLv.putInt(getattr(p.dict['memory_info'], 'rss', 0))
        #virtLv.addStr(bytes2human(getattr(p.dict['memory_info'], 'vms', 0)))
        #resLv.addStr(bytes2human(getattr(p.dict['memory_info'], 'rss', 0)))
        cpuLv.putFloat(p.dict['cpu_percent'])
        memLv.putFloat(p.dict['memory_percent'])
        timeLv.putInt(ctime)
        nameLv.addStr(p.dict['name'] or '')
    return pack

current_milli_time = lambda: int(round(time.time() * 1000))
last_call_time = 0
pack = None

def process(param):
    global last_call_time
    global pack
    try:
        if os.name != 'posix':
            pack = MapPack()
            pack.putStr("error", "platform not supported")
            return pack
        if current_milli_time() > (last_call_time + 5000) :
            args = poll()
            pack = extractValues(*args)
            last_call_time = current_milli_time()
        return pack
    except (KeyboardInterrupt, SystemExit):
        pass

if __name__ == '__main__':
    print current_milli_time()
    time.sleep(3)
    print current_milli_time()
    #process()
