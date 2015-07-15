#!/usr/bin/env python

#
# original code from 
#    https://github.com/giampaolo/psutil/blob/master/examples/
#
# Copyright (c) 2009, Giampaolo Rodola'. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

"""
Print system memory information.
"""

import psutil

from cStringIO import StringIO

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


def pprint_ntuple(nt, result):
    for name in nt._fields:
        value = getattr(nt, name)
        if name != 'percent':
            value = bytes2human(value)
        result.write('%-10s : %7s' % (name.capitalize(), value))
        result.write('\n')

def process(param):
    result=StringIO()
    result.write('MEMORY\n------\n')
    pprint_ntuple(psutil.virtual_memory(), result)
    result.write('\nSWAP\n----\n')
    pprint_ntuple(psutil.swap_memory(), result)
    pack = MapPack()
    pack.putStr("result", result.getvalue())
    result.close()
    return pack

def main():
    print_('MEMORY\n------')
    pprint_ntuple(psutil.virtual_memory())
    print_('\nSWAP\n----')
    pprint_ntuple(psutil.swap_memory())

if __name__ == '__main__':
#    main()
    pack = process(None)
    print pack
