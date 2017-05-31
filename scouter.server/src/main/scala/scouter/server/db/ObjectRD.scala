package scouter.server.db;

import java.io.File
import java.io.IOException
import java.util.ArrayList

import scouter.lang.TextTypes
import scouter.lang.pack.MapPack
import scouter.lang.pack.ObjectPack
import scouter.io.DataInputX
import scouter.server.db.obj.ObjectData
import scouter.server.db.obj.ObjectIndex
import scouter.util.FileUtil

object ObjectRD {

    def getObjName(date: String, objHash: Int): String = {
        var objName = TextRD.getString(date, TextTypes.OBJECT, objHash);
        if (objName == null) {
            try {
                val pack = getObjectPack(date, objHash);
                if (pack == null) {
                    return null;
                }
                objName = pack.objName;
                TextWR.add(date, TextTypes.OBJECT, objHash, objName);
            } catch {
                case e: Exception => e.printStackTrace();
            }
        }
        return objName;
    }

    def getObjectPack(date: String, hash: Int): ObjectPack = {

        val path = ObjectWR.getDBPath(date);
        if (new File(path).canRead() == false) {
            return null;
        }
        val file = path + "/obj";
        var idx: ObjectIndex = null;
        var reader: ObjectData = null;
        try {
            idx = ObjectIndex.open(file);
            reader = ObjectData.open(file);

            val fpos = idx.get(hash);
            if (fpos < 0)
                return null;
            val b = reader.read(fpos);
            if (b == null)
                return null;
            return new DataInputX(b).readPack().asInstanceOf[ObjectPack];
        } finally {
            FileUtil.close(idx);
            FileUtil.close(reader);
        }
    }

    def getDailyAgent(date: String): MapPack = {
        val list = getObjectList(date);

        val m = new MapPack();
        val objTypeLv = m.newList("objType");
        val objHashLv = m.newList("objHash");
        val objNameLv = m.newList("objName");

        val itr = list.iterator()
        while (itr.hasNext()) {
            val obj = itr.next()
            objTypeLv.add(obj.objType);
            objHashLv.add(obj.objHash);
            objNameLv.add(obj.objName);
        }
        return m;
    }

    def getObjectList(date: String): java.util.List[ObjectPack] = {
        var list = new ArrayList[ObjectPack]()
        read(date, (key: Array[Byte], data: Array[Byte]) => {
            try {
                val pack = new DataInputX(data).readPack().asInstanceOf[ObjectPack];
                list.add(pack)
            } catch {
                case e: Exception =>
                    e.printStackTrace();
                    return list
                case _: Throwable =>
                    return list
            }
        })
        return list
    }

    def read(date: String, handler: (Array[Byte], Array[Byte]) => Any) {

        val path = ObjectWR.getDBPath(date);
        if (new File(path).canRead() == false) {
            return ;
        }
        val file = path + "/obj";

        var idx: ObjectIndex = null;
        var reader: ObjectData = null;

        try {
            idx = ObjectIndex.open(file);
            reader = ObjectData.open(file);
            idx.read(handler, reader.read);
        } catch {
            case e: Exception => e.printStackTrace();
        } finally {
            FileUtil.close(idx);
            FileUtil.close(reader);
        }
    }
}
