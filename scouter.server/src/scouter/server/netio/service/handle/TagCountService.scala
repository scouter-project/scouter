/*
 *  Copyright 2015 LG CNS.
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
package scouter.server.netio.service.handle;

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.asScalaSet
import scouter.lang.CounterKey
import scouter.lang.TimeTypeEnum
import scouter.lang.pack.MapPack
import scouter.lang.value.DoubleValue
import scouter.lang.value.MapValue
import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.net.TcpFlag
import scouter.server.core.AgentManager
import scouter.server.core.cache.CounterCache
import scouter.server.db.ObjectRD
import scouter.server.db.RealtimeCounterRD
import scouter.server.netio.service.anotation.ServiceHandler
import scouter.server.util.TimedSeries
import scouter.util.CastUtil
import scouter.util.DateUtil
import scouter.util.StringUtil
import scouter.net.RequestCmd
import scouter.server.tagcnt.TagCountConfig
import scouter.lang.value.TextValue
import scouter.server.tagcnt.TagCountProxy
import scouter.server.tagcnt.core.ValueCountTotal
import scouter.server.util.EnumerScala
import scouter.server.tagcnt.core.ValueCount

class TagCountService {

    @ServiceHandler(RequestCmd.TAGCNT_DIV_NAMES)
    def getDivNames(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val objType = param.getText("objType");
        
        val tagGroups = TagCountConfig.getTagGroups(); 
        while (tagGroups.hasMoreElements()) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writeValue(new TextValue(tagGroups.nextString()));
        }
    }

    @ServiceHandler(RequestCmd.TAGCNT_TAG_NAMES)
    def getTagNames(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val objType = param.getText("objType");
        val tagGroup = param.getText("tagGroup");
        
        val tagNames = TagCountConfig.getTagNames(tagGroup);
        while (tagNames.hasMoreElements()) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writeValue(new TextValue(tagNames.nextString()));
        }
    }
    
    @ServiceHandler(RequestCmd.TAGCNT_TAG_VALUES)
    def getTagValues(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val objType = param.getText("objType");
        val tagGroup = param.getText("tagGroup");
        val tagName = param.getText("tagName");
        var date = param.getText("date");
        if (StringUtil.isEmpty(date)) {
            date = DateUtil.yyyymmdd();
        }
        
        val valueCountTotal = TagCountProxy.getTagValueCountWithCache(date, objType, tagGroup, tagName, 100);
        if (valueCountTotal != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writeInt(valueCountTotal.howManyValues)
            dout.writeInt(valueCountTotal.totalCount)
            dout.writeInt(valueCountTotal.values.size())
            EnumerScala.forward(valueCountTotal.values, (vc: ValueCount) => {
                dout.writeValue(vc.tagValue)
                dout.writeLong(vc.valueCount)
            })
        }
    }

    @ServiceHandler(RequestCmd.TAGCNT_TAG_VALUE_DATA)
    def getTagValueCount(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val objType = param.getText("objType");
        val tagGroup = param.getText("tagGroup");
        val tagName = param.getText("tagName");
        val tagValue = param.get("tagValue");
        var date = param.getText("date");
        if (StringUtil.isEmpty(date)) {
            date = DateUtil.yyyymmdd();
        }
        
        val valueCount = TagCountProxy.getTagValueCountData( date, objType, tagGroup, tagName, tagValue);
        if (valueCount != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writeArray(valueCount)
        }
    }
}