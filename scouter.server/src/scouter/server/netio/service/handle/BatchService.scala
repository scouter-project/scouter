package scouter.server.netio.service.handle

import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.lang.pack.MapPack
import scouter.lang.pack.BatchPack
import scouter.lang.pack.ObjectPack
import scouter.lang.pack.Pack
import scouter.lang.value.BlobValue
import scouter.lang.value.BooleanValue
import scouter.lang.value.MapValue
import scouter.net.RequestCmd
import scouter.net.TcpFlag
import scouter.server.Configure
import scouter.server.CounterManager
import scouter.server.core.AgentManager
import scouter.server.netio.AgentCall
import scouter.server.netio.service.anotation.ServiceHandler
import scouter.util.StringKeyLinkedMap.StringKeyLinkedEntry
import scouter.server.util.EnumerScala
import scouter.server.db.BatchDB;

class BatchService {
    @ServiceHandler(RequestCmd.BATCH_HISTORY_LIST)
    def read(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
        val param = din.readPack().asInstanceOf[MapPack]
        val objHash = param.getInt("objHash")
        val objInfo = AgentManager.getAgent(objHash)
        if(objInfo == null){
          return;
        }
        val objName = objInfo.objName
        val filter = param.getText("filter")
        var response = param.getLong("response")
        var from = param.getLong("from")
        var to = param.getLong("to")
        
        val handler = (time: Long, data: BatchPack) => {
            dout.writeByte(TcpFlag.HasNEXT);
            data.writeSimple(dout);
        }

        if (from > 0 && to > from) {
           BatchDB.read(objName, from, to, filter, response, handler)
        }
    }
}