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

import atexit
import datetime
import platform

class MeterResource():
    BUCKET_SIZE = 300
    stime = datetime.datetime.now() 
    cur_pos = 0
    last_time = 0
    def __init__(self):
        self.last_time = self.getTime()
        self.cur_pos = int(self.last_time % self.BUCKET_SIZE)
        self.counts = [0] * self.BUCKET_SIZE
        self.value = [0] * self.BUCKET_SIZE

    def  getPosition(self):
        curTime = self.getTime()
        if curTime != self.last_time:
            i = 0
            period = curTime - self.last_time;
            while  i < period and i < self.BUCKET_SIZE:
                if self.cur_pos + 1 >= self.BUCKET_SIZE :
                    self.cur_pos = 0 
                else:
                    self.cur_pos += 1
                self.clear(self.cur_pos);
                i += 1           
            self.last_time = curTime;
            self.cur_pos = int(self.last_time % self.BUCKET_SIZE);
        return self.cur_pos;

    def check(self, period):
        if period >= self.BUCKET_SIZE:
            period = self.BUCKET_SIZE - 1
        return period


    def desc(self, pos) :
        if (pos == 0):
            pos = self.BUCKET_SIZE - 1;
        else:
            pos = pos - 1
        return pos
        
    def getTime(self):
        now = datetime.datetime.now()
        t = now - self.stime
        return (t.microseconds+(t.seconds+t.days*24*3600)*10**6)/10**6
        
    def clear(self, p):
        self.value[p] = 0;
        self.counts[p] = 0;


    def add(self, value):
        pos = self.getPosition()
        self.counts[pos] += 1
        self.value[pos] += value
        
    def getAvg(self, period):
        period = self.check(period);
        pos = self.getPosition();
        cnt = 0
        sumval = 0.0
        i = 0
        while i < period:
            sumval += self.value[pos]
            cnt += self.counts[pos]
            i += 1
            pos = self.desc(pos)
    
        if cnt == 0 :
            return 0 
        return sumval / cnt
    
    def getSum(self, period):
        period = self.check(period);
        pos = self.getPosition();
        sumval = 0
        i = 0
        while i < period:
            sumval += self.value[pos]
            i += 1
            pos = self.desc(pos)    
        return sumval    
    
def logo():
    print """
  ____                  _            
 / ___|  ___ ___  _   _| |_ ___ _ __ 
 \___ \ / __/   \| | | | __/ _ \ '__|
  ___) | (_| (+) | |_| | ||  __/ |   
 |____/ \___\___/ \__,_|\__\___|_|                                      
 Scouter version 0.2.3
 Open Source S/W Performance Monitoring  
   """

def shutdown():
    print "Good bye Scouter!!"

atexit.register(shutdown)
lineno = 0

deltaTable = dict()
def delta(key, value):
    if(deltaTable.has_key(key)):
        old = deltaTable.get(key)
        deltaTable[key] = value
        return value - old
    else:
        deltaTable[key] = value
        return 0

calcTable = dict()
def calc(pack, key, value):
    if(calcTable.has_key(key)):
        m = calcTable[key]
        m.add(value)
        pack.putInt(key, int(round(m.getAvg(10))))       
    else:
        m = MeterResource()
        m.add(value)
        calcTable[key] = m
        pack.putInt(key, int(round(m.getAvg(10))))
        
def getMeter(key, value):
    if(calcTable.has_key(key)):
        m = calcTable[key]
        m.add(value)
        return m
    else:
        m = MeterResource()
        m.add(value)
        calcTable[key] = m
        return m

def objtype():
    name = platform.system().lower()
    if(name=='darwin'):
        return 'osx'
    else:
        return name

scouter_name=""
def setObjname(name):
    global scouter_name
    scouter_name=name
    
def objname():
    if(scouter_name==""):
        return '/' + platform.node().replace(' ', '_')    
    else:
        return '/'+scouter_name
    
def diffSeconds(stime, etime):
    t = etime - stime
    return (t.microseconds+(t.seconds+t.days*24*3600)*10**6)/10**6
    