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

class Value():
    NULL = 0
    BOOLEAN = 10
    DECIMAL = 20
    FLOAT = 30
    DOUBLE = 40
    TEXT = 50
    BLOB = 60
    LIST = 70
    MAP = 80
    
    def write(self, out):
        pass
    
    def read(self, inx):
        pass
    
    def getValueType(self):
        pass

createValue={
         Value.NULL: (lambda : NullValue()),
         Value.BOOLEAN: (lambda : BooleanValue()),
         Value.DECIMAL: (lambda : DecimalValue()),
         Value.FLOAT: (lambda : FloatValue()),
         Value.DOUBLE: (lambda : DoubleValue()),
         Value.TEXT: (lambda : TextValue()),
         Value.BLOB: (lambda : BlobValue()),
         Value.LIST: (lambda : ListValue()),
         Value.MAP: (lambda : MapValue()),
         }


class Number():
    pass


class BlobValue(Value):
    def __init__(self, v=None):
        if (v is None):
            self.value = bytearray(0)
        else:
            self.value = v   
            
    def __eq__(self, other):
        return self.value==other.table
    
    def __hash__(self, other):   
        return self.value.__hash__()
    
    def getValueType(self):
        return Value.BLOB
    
    def write(self, out):
        out.writeBlob(self.value)
    
    def read(self, inx):
        self.value = inx.readBlob()
        return self
    
    def __str__(self):
        return str(self.value)
   
    def getInt(self):
        return 0

    def getFloat(self):
        return 0.0

class BooleanValue(Value):
    def __init__(self, v=False):
            self.value = v
            
    def __eq__(self, other):
        return self.value == other.table
    
    def __hash__(self, other):   
        if self.value : 
            return 1
        else:
            return 0
    
    def getValueType(self):
        return Value.BOOLEAN
    
    def write(self, out):
        out.writeBoolean(self.value)
    
    def read(self, inx):
        self.value = inx.readBloolean()
        return self
    
    def __str__(self):
        return str(self.value)   

    
    def getInt(self):
        return 1 if self.value else 0

    def getFloat(self):
        return 1.0 if self.value else 0.0

class DecimalValue(Value,Number):
    def __init__(self, v=0):
        self.value = v
            
    def __eq__(self, other):
        return self.value == other.table
    
    def __hash__(self, other):   
        return self.value.__hash__()
    
    def getValueType(self):
        return Value.DECIMAL
    
    def write(self, out):
        out.writeDecimal(self.value)
    
    def read(self, inx):
        self.value = inx.readDecimal()
        return self    
    def __str__(self):
        return str(self.value)
    
    def getInt(self):
        return int(self.value)

    def getFloat(self):
        return float(self.value)
           
class DoubleValue(Value,Number):
    def __init__(self, v=0.0):
        self.value = v
            
    def __eq__(self, other):
        return self.value == other.table
    
    def __hash__(self, other):   
        return self.value.__hash__()
    
    def getValueType(self):
        return Value.DOUBLE
    
    def write(self, out):
        out.writeDouble(self.value)
    
    def read(self, inx):
        self.value = inx.readDouble()
        return self    
    def __str__(self):
        return str(self.value)

    def getInt(self):
        return int(self.value)

    def getFloat(self):
        return float(self.value)
           
class FloatValue(Value,Number):
    def __init__(self, v=0.0):
        self.value = float(v)
            
    def __eq__(self, other):
        return self.value == other.table
    
    def __hash__(self, other):   
        return self.value.__hash__()
    
    def getValueType(self):
        return Value.FLOAT
    
    def write(self, out):
        out.writeFloat(self.value)
    
    def read(self, inx):
        self.value = inx.readFloat()
        return self    
    def __str__(self):
        return str(self.value)
    
    def getInt(self):
        return int(self.value)

    def getFloat(self):
        return float(self.value)

class ListValue(Value):
        
    def __init__(self, array=None):
        if(array==None):
            self.table = []
        else:
            self.table = array
            
    def __eq__(self, other):
        return self.table == other.table
    
    def __hash__(self, other):   
        return self.table.__hash__()
    
    def getValueType(self):
        return Value.LIST
      
    def addValue(self, value):
        if value is None:
            self.table.append(NullValue())
        else:
            self.table.append(value)
        return self
 
    def addStr(self, value):
        if value is None:
            self.table.append(NullValue())
        else:
            self.table.append(TextValue(value))
        return self
    
    def putInt(self, value):
        if value is None:
            self.table.append(NullValue())
        else:
            self.table.append(DecimalValue(value))
        return self

    def putFloat(self, value):
        if value is None:
            self.table.append(NullValue())
        else:
            self.table.append(FloatValue(value))
        return self
           
    def write(self, out):
        out.writeDecimal(len(self.table))
        for x in self.table:
            out.writeValue(x)
    
    def read(self, inx):
        count = inx.readDecimal()
        while count>0:
            self.table.append(inx.readValue())
            count -=1            
        return self    
    def __str__(self):
        out =[];
        for v in self.table :
            out.append(str(v))
        return str(out)
    
    def getInt(self):
        return 0

    def getFloat(self):
        return 0.0

    
class MapValue(Value):
        
    def __init__(self):
        self.table = dict()
            
    def __eq__(self, other):
        return self.table == other.table
    
    def __hash__(self, other):   
        return self.table.__hash__()
    
    def getValueType(self):
        return Value.MAP
    
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
    
    def get(self, key):
        return self.table[key]
    
    def getInt(self):
        return 0

    def getFloat(self):
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
        
class NullValue(Value):
    def __init__(self):
        self.value = None

    def __eq__(self, other):
        return self.value==other.table
    
    def __hash__(self, other):   
        return self.value.__hash__()
    
    def getValueType(self):
        return Value.BLOB
    
    def write(self, out):
        pass
    
    def read(self, inx):
        return self
    
    def __str__(self):
        return str(self.value)

    def getInt(self):
        return 0

    def getFloat(self):
        return 0.0

class TextValue(Value,Number):
    def __init__(self, v=''):
        self.value = str(v)
            
    def __eq__(self, other):
        return self.value == other.table
    
    def __hash__(self, other):   
        return self.value.__hash__()
    
    def getValueType(self):
        return Value.TEXT
    
    def write(self, out):
        out.writeText(self.value)
    
    def read(self, inx):
        self.value = inx.readText()
        return self 
       
    def __str__(self):
        return self.value

    def getInt(self):
        return int(self.value)

    def getFloat(self):
        return float(self.value)
    

