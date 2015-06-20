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

from scouter.lang.value import *
from scouter.lang.pack import *

class Pack():
    MAP = 10
    PERF_COUNTER = 60
    TEXT = 50
    ALERT = 70
    OBJECT = 80
    
    def write(self, out):
        pass
    
    def read(self, inx):
        pass
    
    def getPackType(self):
        pass
 
 
createPack={
         Pack.MAP: (lambda : MapPack()),
         Pack.PERF_COUNTER: (lambda : PerfCounterPack()),
         Pack.OBJECT: (lambda : ObjectPack()),
         Pack.TEXT: (lambda : None),
         Pack.ALERT: (lambda : None),
        }
    
class ObjectPack(Pack):

    def __init__(self):
        self.objType = ''
        self.objHash = 0
        self.objName = ''
        self.address = ''
        self.version = ''
        self.alive = bool(1)
        self.wakeup = 0
        self.tags = createValue[Value.MAP]()
                
    def getPackType(self):
        return Pack.OBJECT
    
    def write(self, out):
        out.writeText(self.objType)
        out.writeDecimal(self.objHash)
        out.writeText(self.objName)
        out.writeText(self.address)
        out.writeText(self.version)
        out.writeBoolean(self.alive)
        out.writeDecimal(self.wakeup)
        out.writeValue(self.tags)
        
    def read(self, inx):
        self.objType = inx.readText()
        self.objHash = inx.readDecimal()
        self.objName = inx.readText()
        self.address = inx.readText()
        self.version = inx.readText()
        self.alive = inx.readBoolean()
        self.wakeup = inx.readDecimal()
        self.tags = inx.readValue()
        return self
        
    def __str__(self):
        return self.objType + " "  + self.objName + " " + self.address + "\n" + self.tags.__str__()
    
class PerfCounterPack(Pack):
    
    def __init__(self):
        self.time = 0
        self.objName = ''
        self.timeType = 0
        self.data = createValue[Value.MAP]()
        
    def getPackType(self):
        return Pack.PERF_COUNTER
    
    def write(self, out):
        out.writeLong(self.time)
        out.writeText(self.objName)
        out.writeByte(self.timeType)
        out.writeValue(self.data)
        
    def read(self, inx):
        self.time = inx.readLong()
        self.objName = inx.readText()
        self.timeType = inx.readByte()
        self.data = inx.readValue()
        return self
    
    def putValue(self, key,value):
        self.data.putValue(key, value)
        return self
 
    def putStr(self, key,value):
        self.data.putStr(key, value)
        return self
    
    def putInt(self, key,value):
        self.data.putInt(key, value)
        return self

    def putFloat(self, key,value):
        self.data.putFloat(key, value)
        return self
    
    def __str__(self):
        return "PerfCounterPack " + self.objName + "\n"  + self.data.__str__()
        

class MapPack(Pack):
        
    def __init__(self):
        self.table = dict()
            
    def __eq__(self, other):
        return self.table == other.table
    
    def __hash__(self, other):   
        return self.table.__hash__()
    
    def getPackType(self):
        return Pack.MAP
    
    def write(self, out):
        out.writeDecimal(len(self.table))
        for key in self.table.keys():
            out.writeText(key)
            out.writeValue(self.table[key])
    
    def read(self, inx):
        count = inx.readDecimal()
        while count >0:
            key = inx.readText()
            self.table[key] = inx.readValue()
            count -=1             
        return self  
      
    def __str__(self):
        out = dict();
        for key in self.table.keys():
            out[key]=str(self.table[key])
        return str(out)
    
    def getValue(self, key):
        return self.table[key]

    def getStr(self, key):
        v=self.table[key]
        if type(v) is TextValue:
            return v.value
        return str(v)
    
    def getInt(self, key):
        v=self.table[key]
        if isinstance(v, Number):
            return v.getInt()
        return 0

    def getFloat(self, key):
        v=self.table[key]
        if isinstance(v, Number):
            return v.getFloat()
        return 0.0

    def hasKey(self, key):
        return self.table.has_key(key)
    
    def remove(self, key):
        self.table.remove(key)

    def putValue(self, key,value):
        if value is None:
            self.table[key]=NullValue()
        else:
            self.table[key]=value
        return self
 
    def putStr(self, key,value):
        if value is None:
            self.table[key]=NullValue()
        else:
            self.table[key]=TextValue(value)
        return self
    
    def putInt(self, key,value):
        if value is None:
            self.table[key]=NullValue()
        else:
            self.table[key]=DecimalValue(value)
        return self

    def putFloat(self, key,value):
        if value is None:
            self.table[key]=NullValue()
        else:
            self.table[key]=FloatValue(value)
        return self
    
    def newList(self, key):
        self.table[key] = ListValue()
        return self.table[key]
 
   
