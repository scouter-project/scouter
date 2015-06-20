#!/usr/bin/env python

#
# original code from 
#    https://github.com/giampaolo/psutil/blob/master/examples/
#
# Copyright (c) 2009, Giampaolo Rodola'. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

"""
Print detailed information about a process.

Author: Giampaolo Rodola' <g.rodola@gmail.com>
"""

import datetime
import os
import socket
import sys

import psutil
from cStringIO import StringIO


from scouter.lang.pack import *
from scouter.lang.value import *


def convert_bytes(n):
    symbols = ('K', 'M', 'G', 'T', 'P', 'E', 'Z', 'Y')
    prefix = {}
    for i, s in enumerate(symbols):
        prefix[s] = 1 << (i + 1) * 10
    for s in reversed(symbols):
        if n >= prefix[s]:
            value = float(n) / prefix[s]
            return '%.1f%s' % (value, s)
    return "%sB" % n

def print_(a, b, result):
    #if sys.stdout.isatty() and os.name == 'posix':
     #   fmt = '\x1b[1;32m%-17s\x1b[0m %s' % (a, b)
    #else:
    fmt = '%-15s %s' % (a, b)
    result.write(fmt + '\n')
    # python 2/3 compatibility layer
    #sys.stdout.write(fmt + '\n')
    #sys.stdout.flush()


def run(pid, result):
    ACCESS_DENIED = ''
    try:
        p = psutil.Process(pid)
        pinfo = p.as_dict(ad_value=ACCESS_DENIED)
    except psutil.NoSuchProcess:
        result.write('No Such Process')
        return

    try:
        if p.parent:
            parent = '(%s)' % p.parent.name
        else:
            parent = ''
    except psutil.Error:
        parent = ''
    started = datetime.datetime.fromtimestamp(
        pinfo['create_time']).strftime('%Y-%M-%d %H:%M')
    io = pinfo.get('io_counters', ACCESS_DENIED)
    mem = '%s%% (resident=%s, total=%s, virtual=%s) ' % (
        round(pinfo['memory_percent'], 1),
        convert_bytes(pinfo['memory_info'].rss),
        convert_bytes(psutil.virtual_memory().total),
        convert_bytes(pinfo['memory_info'].vms))
    children = p.get_children()

    print_('pid', pinfo['pid'], result)
    print_('name', pinfo['name'], result)
    print_('exe', pinfo['exe'], result)
    print_('parent', '%s %s' % (pinfo['ppid'], parent), result)
    print_('cmdline', ' '.join(pinfo['cmdline']), result)
    print_('started', started, result)
    print_('user', pinfo['username'], result)
    if os.name == 'posix' and pinfo['uids'] and pinfo['gids']:
        print_('uids', 'real=%s, effective=%s, saved=%s' % pinfo['uids'], result)
    if os.name == 'posix' and pinfo['gids']:
        print_('gids', 'real=%s, effective=%s, saved=%s' % pinfo['gids'], result)
    if os.name == 'posix':
        print_('terminal', pinfo['terminal'] or '', result)
    if hasattr(p, 'getcwd'):
        print_('cwd', pinfo['cwd'], result)
    print_('memory', mem, result)
    print_('cpu', '%s%% (user=%s, system=%s)' % (
        pinfo['cpu_percent'],
        getattr(pinfo['cpu_times'], 'user', '?'),
        getattr(pinfo['cpu_times'], 'system', '?')), result)
    print_('status', pinfo['status'], result)
    print_('niceness', pinfo['nice'], result)
    print_('num threads', pinfo['num_threads'], result)
    if io != ACCESS_DENIED:
        print_('I/O', 'bytes-read=%s, bytes-written=%s' % (
            convert_bytes(io.read_bytes),
            convert_bytes(io.write_bytes)), result)
    if children:
        print_('children', '', result)
        for child in children:
            print_('', 'pid=%s name=%s' % (child.pid, child.name), result)

    if pinfo['open_files'] != ACCESS_DENIED:
        print_('open files', '', result)
        for file in pinfo['open_files']:
            print_('',  'fd=%s %s ' % (file.fd, file.path), result)

    if pinfo['threads']:
        print_('running threads', '', result)
        for thread in pinfo['threads']:
            print_('',  'id=%s, user-time=%s, sys-time=%s' % (
                thread.id, thread.user_time, thread.system_time), result)
    if pinfo['connections'] != ACCESS_DENIED:
        print_('open connections', '', result)
        for conn in pinfo['connections']:
            if conn.type == socket.SOCK_STREAM:
                type = 'TCP'
            elif conn.type == socket.SOCK_DGRAM:
                type = 'UDP'
            else:
                type = 'UNIX'
            lip, lport = conn.laddr
            if not conn.raddr:
                rip, rport = '*', '*'
            else:
                rip, rport = conn.raddr
            print_('',  '%s:%s -> %s:%s type=%s status=%s' % (
                lip, lport, rip, rport, type, conn.status), result)

def process(param):
    result=StringIO()
    pid = param.getInt("pid")
    run(pid, result)
    pack = MapPack()
    pack.putStr("result", result.getvalue())
    result.close()
    return pack
    


def main(argv=None):
    result=StringIO()
    if argv is None:
        argv = sys.argv
    if len(argv) == 1:
        sys.exit(run(os.getpid(), result))
    elif len(argv) == 2:
        sys.exit(run(int(argv[1]), result))
    else:
        print "invalid argv length"

if __name__ == '__main__':
    sys.exit(main())
