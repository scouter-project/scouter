/*
*  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 *
 */

package scouter.server.core;

import java.util.Enumeration
import java.util.List
import scouter.lang.pack.ObjectPack
import scouter.util.IntKeyMap
import scouter.util.StringKeyLinkedMap;
import scouter.server.util.EnumerScala

class ObjectMap {

    private val objectTypeTable = new StringKeyLinkedMap[IntKeyMap[ObjectPack]]();
    private val objectTable = new IntKeyMap[ObjectPack]();
    private val objectNameTable = new StringKeyLinkedMap[ObjectPack]();

    def put(p: ObjectPack): Boolean = {
        this.synchronized {
            clearInvalidObjType(p);
            var m = objectTypeTable.get(p.objType);
            if (m == null) {
                m = new IntKeyMap[ObjectPack]();
                objectTypeTable.put(p.objType, m);
            }
            m.put(p.objHash, p);
            objectTable.put(p.objHash, p);
            objectNameTable.put(p.objName, p);
            return true;
        }
    }

    def getTypeObjects(objType: String): IntKeyMap[ObjectPack] = {
        return objectTypeTable.get(objType);
    }

    private val empty = new Enumeration[ObjectPack]() {
        override def hasMoreElements(): Boolean = {
            return false;
        }

        override def nextElement(): ObjectPack = {
            return null;
        }
    };

    def enumTypeObject(objType: String): Enumeration[ObjectPack] = {
        val im = objectTypeTable.get(objType);
        if (im == null)
            return empty;
        else
            return im.values();
    }

    def getObject(name: String): ObjectPack = {
        return objectNameTable.get(name);
    }

    def getObject(hash: Int): ObjectPack = {
        return objectTable.get(hash);
    }

    def objects(): Enumeration[ObjectPack] = {
        return objectTable.values();
    }

    def remove(objHash: Int) {
        this.synchronized {
            val p = objectTable.get(objHash);
            if (p == null)
                return ;
            val objName = p.objName;
            val objType = p.objType;

            objectNameTable.remove(objName);
            objectTable.remove(objHash);

            val m = objectTypeTable.get(objType);
            if (m != null) {
                m.remove(objHash);
                if (m.size() == 0) {
                    objectTypeTable.remove(objType);
                }
            }

        }
    }

    def clear() {
        this.synchronized {
            objectTypeTable.clear();
            objectNameTable.clear();
            objectTable.clear();
        }
    }

    private def clearInvalidObjType(p: ObjectPack) {
        val old = objectTable.get(p.objHash);
        if (old != null) {
            if (old.objType.equals(p.objType) == false) {
                remove(old.objHash);
            }
        }
    }

    def putAll(list: List[ObjectPack]) {
        if (list == null) {
            return ;
        }
        EnumerScala.foreach(list.iterator(), (o: ObjectPack) => put(o))
    }
}
