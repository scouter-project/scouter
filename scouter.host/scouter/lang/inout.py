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

import io,sys
import struct
import array

from scouter.lang.value import *
from scouter.lang.pack import *

BYTE_MIN_VALUE=-128
BYTE_MAX_VALUE=127
SHORT_MIN_VALUE=-32768
SHORT_MAX_VALUE=32767
INT3_MIN_VALUE=-0x800000
INT3_MAX_VALUE=0x007fffff
INT_MIN_VALUE=-0x80000000
INT_MAX_VALUE=0x7fffffff
LONG5_MIN_VALUE=-0x8000000000
LONG5_MAX_VALUE=0x0000007fffffffff
LONG_MIN_VALUE=-0x8000000000000000
LONG_MAX_VALUE=0x7fffffffffffffff

class DataInputSocket():
    def __init__(self,v):
        self.socket=v
        
    def readBoolean(self):
        v=self.socket.recv(1)
        return struct.unpack('>?',v)[0]

    def readByte(self):
        v=self.socket.recv(1)
        return struct.unpack('>b',v)[0]
    
    def readShort(self):
        v=self.socket.recv(2)
        return struct.unpack('>h',v)[0]
    
    def readInt3(self):
        s=self.socket.recv(3)
        chrs=struct.unpack('>bBB',s)
        ch1=(chrs[0])
        ch2=(chrs[1])
        ch3=(chrs[2])
        return (ch1 << 16) + (ch2 << 8) + ch3
    
    def readInt(self):
        v=self.socket.recv(4)
        return struct.unpack('>i',v)[0]
    
    def readLong5(self):
        s=self.socket.recv(5)
        chrs=struct.unpack('>bBBBB',s)
        ch1=(chrs[0])
        ch2=(chrs[1])
        ch3=(chrs[2])
        ch4=(chrs[3])
        ch5=(chrs[4])
        return ((ch1 << 32) + (ch2 << 24) + (ch3 << 16)+ (ch4 << 8)+ (ch5 )) 
        
    def readLong(self):
        v=self.socket.recv(8)
        return struct.unpack('>q',v)[0]
        
    def readFloat(self):
        v=self.socket.recv(4)
        return struct.unpack('>f',v)[0]

    def readDouble(self):
        v=self.socket.recv(8)
        return struct.unpack('>d',v)[0]

    def readDecimal(self):
        ln = self.readByte()
        if (ln==0):
            return 0
        elif (ln==1):
            return self.readByte()
        elif (ln==2):
            return self.readShort()
        elif (ln==3):
            return self.readInt3()
        elif (ln==4):
            return self.readInt()
        elif (ln==5):
            return self.readLong5()
        else:
            return self.readLong()
        
    def readBlob(self):    
        baselen = self.readByte() & 0xff;
        if ( baselen==255):
            ln = self.readShort() & 0xffff;
            return self.socket.recv(ln);
        elif (baselen==254):
            ln = self.readInt()
            return self.socket.recv(ln)
        elif (baselen==0):
            return [];
        else:
            return self.socket.recv(baselen);
        
    def readText(self):
        arr=self.readBlob()
        if(len(arr)==0):
            return ''
        else:
            return arr.decode("utf-8")
        
    def readValue(self):
        tt=self.readByte()
        return createValue[tt]().read(self)

    def readPack(self):
        tt=self.readByte()
        return createPack[tt]().read(self)
        
class DataInputX():
    def __init__(self,v):
        self.buffer=io.BytesIO(v)
        
    def readBoolean(self):
        v=self.buffer.read(1)
        return struct.unpack('>?',v)[0]

    def readByte(self):
        v=self.buffer.read(1)
        return struct.unpack('>b',v)[0]
    
    def readShort(self):
        v=self.buffer.read(2)
        return struct.unpack('>h',v)[0]
    
    def readInt3(self):
        s=self.buffer.read(3)
        chrs=struct.unpack('>bBB',s)
        ch1=(chrs[0])
        ch2=(chrs[1])
        ch3=(chrs[2])
        return (ch1 << 16) + (ch2 << 8) + ch3
    
    def readInt(self):
        v=self.buffer.read(4)
        return struct.unpack('>i',v)[0]
    
    def readLong5(self):
        s=self.buffer.read(5)
        chrs=struct.unpack('>bBBBB',s)
        ch1=(chrs[0])
        ch2=(chrs[1])
        ch3=(chrs[2])
        ch4=(chrs[3])
        ch5=(chrs[4])
        return ((ch1 << 32) + (ch2 << 24) + (ch3 << 16)+ (ch4 << 8)+ (ch5 )) 
        
    def readLong(self):
        v=self.buffer.read(8)
        return struct.unpack('>q',v)[0]
        
    def readFloat(self):
        v=self.buffer.read(4)
        return struct.unpack('>f',v)[0]

    def readDouble(self):
        v=self.buffer.read(8)
        return struct.unpack('>d',v)[0]

    def readDecimal(self):
        ln = self.readByte()
        if (ln==0):
            return 0
        elif (ln==1):
            return self.readByte()
        elif (ln==2):
            return self.readShort()
        elif (ln==3):
            return self.readInt3()
        elif (ln==4):
            return self.readInt()
        elif (ln==5):
            return self.readLong5()
        else:
            return self.readLong()
        
    def readBlob(self):    
        baselen = self.readByte() & 0xff;
        if ( baselen==255):
            ln = self.readShort() & 0xffff;
            return self.buffer.read(ln);
        elif (baselen==254):
            ln = self.readInt()
            return self.buffer.read(ln)
        elif (baselen==0):
            return [];
        else:
            return self.buffer.read(baselen);
        
    def readText(self):
        arr=self.readBlob()
        if(len(arr)==0):
            return ''
        else:
            return arr.decode("utf-8")
        
    def readValue(self):
        tt=self.readByte()
        return createValue[tt]().read(self)

    def readPack(self):
        tt=self.readByte()
        return createPack[tt]().read(self)

class DataOutputX():
    def __init__(self):
        self.buffer=io.BytesIO()
    
    def writeBoolean(self,v):
        self.buffer.write(struct.pack('>?',v))
        return self

    def writeByte(self,v):
        v = v & 0xFF
        self.buffer.write(struct.pack('>B',v))
        return self
        
    def writeShort(self,v):
        v = v & 0xFFFF
        self.buffer.write(struct.pack('>H',v))
        return self
    
    def writeInt3(self,v):
        v1=(v >> 16) & 0xFF
        v2=(v >> 8) & 0xFF
        v3=(v >> 0) & 0xFF
        self.buffer.write(struct.pack('>BBB',v1,v2,v3))
        return self;
    
    def writeInt(self, v):
        v = v & 0xFFFFFFFF
        self.buffer.write(struct.pack('>I',v))
        return self
    
    def writeLong5(self,v):
        v1=((v >> 32) & 0xFF)
        v2=((v >> 24) & 0xFF)
        v3=((v >> 16) & 0xFF)
        v4=((v >> 8) & 0xFF)
        v5=((v >> 0) & 0xFF)
        self.buffer.write(struct.pack('>BBBBB',v1,v2,v3,v4,v5))
        return self;
 
    def writeLong(self,v):
        v = v & 0xFFFFFFFFFFFFFFFF
        self.buffer.write(struct.pack('>Q',v))
        return self
    
    def writeFloat(self,v):
        self.buffer.write(struct.pack('>f',v))
        return self
    
    def writeDouble(self,v):
        self.buffer.write(struct.pack('>d',v))
        return self
    
    def writeDecimal(self, v):
        if (v == 0):
            self.writeByte(0)
        elif (BYTE_MIN_VALUE <= v <= BYTE_MAX_VALUE):
            self.writeByte(1)
            self.writeByte(v)
        elif (SHORT_MIN_VALUE <= v <= SHORT_MAX_VALUE):
            self.writeByte(2)
            self.writeShort(v)
        elif (INT3_MIN_VALUE <= v  <= INT3_MAX_VALUE):
            self.writeByte(3)
            self.writeInt3(v)
        elif (INT_MIN_VALUE <= v <= INT_MAX_VALUE):
            self.writeByte(4)
            self.writeInt(v)
        elif (LONG5_MIN_VALUE <= v <= LONG5_MAX_VALUE):
            self.writeByte(5)
            self.writeLong5(v)
        elif (LONG_MIN_VALUE <= v <= LONG_MAX_VALUE):
            self.writeByte(8)
            self.writeLong(v)
        return self

    def write(self, v):
        self.buffer.write(v)
        return self
    
    def writeBlob(self,v):
        if (v is None  or len(v)== 0):
            self.writeByte(0);
        else:
            ln = len(v);
            if (ln <= 253):
                self.writeByte(ln)
                self.write(v)
            elif (ln <= 65535):
                self.writeByte(255)
                self.writeShort(ln)
                self.write(v);
            else:
                self.writeByte(254)
                self.writeInt(ln)
                self.write(v);
        return self
    
    def writeText(self,v):
        if (v is None):
            self.writeByte(0)
        else:
            self.writeBlob(v.encode("utf-8"))                    
        return self
    
    def writeUTF(self,v):
        v=v.encode('utf-8')
        ln=len(v)
        self.buffer.write(struct.pack('>H',ln))
        self.buffer.write(v)
        return self
    
    def writeValue(self,v):
        self.writeByte(v.getValueType())
        v.write(self)
        return self
 
    def writePack(self,v):
        self.writeByte(v.getPackType())
        v.write(self)
        return self
 
    def toByteArray(self):
        return self.buffer.getvalue()
    
if __name__ == '__main__':
    x=dict()
    x["aa홍"]=10
    print  x["aa홍"]
    m=createPack[Pack.MAP]()
    m.putValue("aaa".decode('utf-8'), DecimalValue(10))
    m.putValue("bbb", TextValue(10))
    m.putValue("ccc", TextValue(None))
    m.putValue("eee", FloatValue(10))
    print m
      
    print m
    n=DataOutputX().writePack(m).toByteArray();
    m2=DataInputX(n).readPack()
    print m2
    print  m2.getValue("aaa홍".decode('utf-8'))
      
    t = TextValue('ttt강')
    tn=DataOutputX().writeValue(t).toByteArray();
    t2=DataInputX(tn).readValue()
    print t2.value
#     