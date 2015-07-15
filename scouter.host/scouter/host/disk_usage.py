#!/usr/bin/env python

#
# original code from 
#    https://github.com/giampaolo/psutil/blob/master/examples/
#
# Copyright (c) 2009, Giampaolo Rodola'. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

"""
List all mounted disk partitions a-la "df -h" command.
"""

import sys
import os
import psutil

from scouter.lang.pack import *
from scouter.lang.value import *


def bytes2human(n):
    # http://code.activestate.com/recipes/578019
    # >>> bytes2human(10000)
    # '9.8K'
    # >>> bytes2human(100001221)
    # '95.4M'
    symbols = ('K', 'M', 'G', 'T', 'P', 'E', 'Z', 'Y')
    prefix = {}
    for i, s in enumerate(symbols):
        prefix[s] = 1 << (i + 1) * 10
    for s in reversed(symbols):
        if n >= prefix[s]:
            value = float(n) / prefix[s]
            return '%.1f%s' % (value, s)
    return "%sB" % n


def main():
    templ = "%-17s %8s %8s %8s %5s%% %9s  %s"
    print(templ % ("Device", "Total", "Used", "Free", "Use ", "Type", "Mount"))
    for part in psutil.disk_partitions(all=False):
        if os.name == 'nt':
            if 'cdrom' in part.opts or part.fstype == '':
                # skip cd-rom drives with no disk in it; they may raise
                # ENOENT, pop-up a Windows GUI error for a non-ready
                # partition or just hang.
                continue
        usage = psutil.disk_usage(part.mountpoint)
        print(templ % (
            part.device,
            bytes2human(usage.total),
            bytes2human(usage.used),
            bytes2human(usage.free),
            int(usage.percent),
            part.fstype,
            part.mountpoint))
        
def process(param):
    pack = MapPack()
    pack.putStr("_name_", "Disk Usage")
    deviceLv = pack.newList("Device")
    totalLv = pack.newList("Total")
    usedLv = pack.newList("Used")
    freeLv = pack.newList("Free")
    useLv = pack.newList("Use")
    typeLv = pack.newList("Type")
    mountLv = pack.newList("Mount")
    for part in psutil.disk_partitions(all=False):
        if os.name == 'nt':
            if 'cdrom' in part.opts or part.fstype == '':
                # skip cd-rom drives with no disk in it; they may raise
                # ENOENT, pop-up a Windows GUI error for a non-ready
                # partition or just hang.
                continue
        usage = psutil.disk_usage(part.mountpoint)
        deviceLv.addStr(part.device)
        totalLv.addStr(bytes2human(usage.total))
        usedLv.addStr(bytes2human(usage.used))
        freeLv.addStr(bytes2human(usage.free))
        useLv.addStr(str(usage.percent) + "%")
        typeLv.addStr(part.fstype)
        mountLv.addStr(part.mountpoint)
    return pack

if __name__ == '__main__':
    pack = process(None)
    print pack
