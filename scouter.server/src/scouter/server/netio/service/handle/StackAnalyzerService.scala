/*
 *  Copyright 2015 Scouter Project.
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
 */
package tuna.server.netio.service.handle;

import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.lang.pack.MapPack
import scouter.net.TcpFlag
import scouter.server.db.StackAnalyzerDB
import scouter.server.netio.service.anotation.ServiceHandler
import scouter.net.RequestCmd

class StackAnalyzerService {

    @ServiceHandler(RequestCmd.GET_STACK_ANALYZER)
    def read(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
        val param = din.readPack().asInstanceOf[MapPack];
        val objName = param.getText("objName");
        val from = param.getLong("from");
        val to = param.getLong("to");

        val handler = (time: Long, data: Array[Byte]) => {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.write(data);
        }
        StackAnalyzerDB.read(objName, from, to, handler)
    }
}
