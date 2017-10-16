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

package scouter.server.db.counter

import scouter.lang.TimeTypeEnum
import scouter.lang.value.ValueEnum;

object DailyCounterUtils {

    def getLength(valueType: Byte): Int = {
        valueType match {
            case ValueEnum.BOOLEAN =>
                return 2
            case ValueEnum.FLOAT =>
                return 5
            case ValueEnum.DOUBLE =>
                return 9
            case ValueEnum.DECIMAL =>
                return 10
            case _ => throw new RuntimeException("not supported type=" + valueType);
        }
       
    }

    def getBucketCount(timetype: Byte): Int = {
        timetype match {
            case TimeTypeEnum.ONE_MIN =>
                return 1440;
            case TimeTypeEnum.FIVE_MIN =>
                return 288;
            case TimeTypeEnum.TEN_MIN =>
                return 144;
            case TimeTypeEnum.HOUR =>
                return 24;
            case TimeTypeEnum.DAY =>
                return 1;
            case _ =>
                 return 0;
        }
        
    }

    def getBucketPos(timetype: Byte, hhmm: Int): Int = {
        val tm = hhmm;

        timetype match {
            case TimeTypeEnum.ONE_MIN =>
                return (((tm / 100) * 60 + tm % 100)).toInt
            case TimeTypeEnum.FIVE_MIN =>
                return (((tm / 100) * 60 + tm % 100) / 5).toInt
            case TimeTypeEnum.TEN_MIN =>
                return (((tm / 100) * 60 + tm % 100) / 10).toInt
            case TimeTypeEnum.HOUR =>
                return (tm / 100).toInt
            case TimeTypeEnum.DAY =>
                return 0;
            case _=>
                return 0;
        }
        
    }
    
}