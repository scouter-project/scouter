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

import socket,io
import time,sys
import json
import struct
from scouter.lang.inout import DataInputSocket, DataOutputX
from scouter.lang.pack import MapPack
from scouter.lang.value import TextValue
from scouter.lang.utility import *

import traceback
import binascii

class TCP():
    HasNext = 0x03
    NoNext = 0x04

handlerTable=dict()

TCP_AGENT=0xCAFE1001


localAddr='127.0.0.1'
def getLocalAddr():
    return localAddr
    
def startReqHandler(host, port, handlers):
    global listen_addr
    global handlerTable
    
    
    localAddr = '127.0.0.1'
    
    handlerTable=handlers
    while True: 
        try:
            BRUN=True
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            sock.connect((host,port))
            sock.settimeout(60000)
                                 
            out=DataOutputX()
            out.writeInt(TCP_AGENT)
            out.writeInt(binascii.crc32(objname()))
            sock.send(out.toByteArray())
            inx=DataInputSocket(sock)

            while BRUN:
                 try:                    
                     out=DataOutputX()
                     cmd = inx.readText()
                     pack = inx.readPack() 
                     
                     if handlerTable.has_key(cmd):
                         result=handlerTable[cmd](pack)
                     else:
                         print 'unknown command: ' + str(cmd)
                         result=MapPack()
                         result.putValue('msg', TextValue('unknown command: ' + str(cmd)))
                    
                     if result != None:
                         out.writeByte(TCP.HasNext)
                         out.writePack(result)
                    
                     out.writeByte(TCP.NoNext)
                     
                     sock.sendall(out.toByteArray())
                     
                 except:
                     traceback.print_exc(file=sys.stdout) 
                     sock.close() 
                     BRUN=False

        except:
            time.sleep(5)
            pass
    