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
package scouter.server.tagcnt;

import scouter.util.BitUtil
import scouter.util.HashUtil
import scouter.util.LongKeyMap
import scouter.util.LongSet
import scouter.util.StringEnumer
import scouter.util.StringKeyLinkedMap
import scouter.util.StringSet
import scouter.util.StringUtil;
import scouter.lang.constants.TagConstants

object TagCountConfig {

    val entries = new StringKeyLinkedMap[StringSet]();

    val keyToName = new LongKeyMap[String]();
   
    class Tag(_tagGroup: String, _tagName: String) {

        val tagGroup = _tagGroup;
        val tagName = _tagName;
        val key = BitUtil.composite(HashUtil.hash(tagGroup), HashUtil.hash(tagName));
        var set = entries.get(tagGroup);
        if (set == null) {
            set = new StringSet();
            entries.put(tagGroup, set);
        }
        set.put(tagName);

        keyToName.put(key, tagName);
    }

    def getTagNames(tagGroup: String): StringEnumer = {
        val tagNames = entries.get(tagGroup);
        return if (tagNames == null) StringSet.emptyEnumer else tagNames.keys();
    }

    def getTagGroups(): StringEnumer = {
        return entries.keys();
    }

    def getTagKey(tagGroup: String, tagName: String): Long = {
        return BitUtil.composite(HashUtil.hash(tagGroup), HashUtil.hash(tagName));
    }

    class Service {
        val total = new Tag(TagConstants.GROUP_SERVICE, TagConstants.NAME_TOTAL);
        val objectName = new Tag(TagConstants.GROUP_SERVICE, TagConstants.NAME_OBJECT);
 //       val ip = new Tag(TagConstants.GROUP_SERVICE, TagConstants.NAME_IP);
        val service = new Tag(TagConstants.GROUP_SERVICE, TagConstants.GROUP_SERVICE);
        
        val service_time_sum= new Tag(TagConstants.GROUP_SERVICE, TagConstants.NAME_SERVICE_TIME_SUM);
        val service_kbyte_sum = new Tag(TagConstants.GROUP_SERVICE, TagConstants.NAME_SERVICE_KBYTE_SUM);
        val service_error_sum = new Tag(TagConstants.GROUP_SERVICE, TagConstants.NAME_SERVICE_ERROR_SUM);
        
        val userAgent = new Tag(TagConstants.GROUP_SERVICE, TagConstants.NAME_USER_AGENT);
        val error = new Tag(TagConstants.GROUP_SERVICE, TagConstants.NAME_ERROR);
        val referer = new Tag(TagConstants.GROUP_SERVICE, TagConstants.NAME_REFERER);
        val group = new Tag(TagConstants.GROUP_SERVICE, TagConstants.NAME_GROUP);

 //       val elapsed = new Tag(TagConstants.GROUP_SERVICE, TagConstants.NAME_ELAPSED);
   //     val sqltime = new Tag(TagConstants.GROUP_SERVICE, TagConstants.NAME_SQLTIME);
   //     val apitime = new Tag(TagConstants.GROUP_SERVICE, TagConstants.NAME_APITIME);
   
        val sqlcount_sum = new Tag(TagConstants.GROUP_SERVICE, TagConstants.NAME_SQL_COUNT_SUM);
        val apicount_sum = new Tag(TagConstants.GROUP_SERVICE, TagConstants.NAME_API_COUNT_SUM);
        val sqltime_sum = new Tag(TagConstants.GROUP_SERVICE, TagConstants.NAME_SQLTIME_SUM);
        val apitime_sum = new Tag(TagConstants.GROUP_SERVICE, TagConstants.NAME_APITIME_SUM);

        
        val city = new Tag(TagConstants.GROUP_SERVICE, TagConstants.NAME_CITY);
        val nation = new Tag(TagConstants.GROUP_SERVICE, TagConstants.NAME_NATION);
   //     val userid = new Tag(TagConstants.GROUP_SERVICE, TagConstants.NAME_USERID);

          val login = new Tag(TagConstants.GROUP_SERVICE, TagConstants.NAME_LOGIN);
          val desc = new Tag(TagConstants.GROUP_SERVICE, TagConstants.NAME_DESC);

    }
    val service = new Service()

    class Alert {
        val total = new Tag(TagConstants.GROUP_ALERT, TagConstants.NAME_TOTAL);
        val objectName = new Tag(TagConstants.GROUP_ALERT, TagConstants.NAME_OBJECT);
        val level = new Tag(TagConstants.GROUP_ALERT, TagConstants.NAME_LEVEL);
        val title = new Tag(TagConstants.GROUP_ALERT, TagConstants.NAME_TITLE);
    }
    val alert = new Alert()

    def main(args: Array[String]) {
        System.out.println(StringUtil.toString(getTagGroups()));
        System.out.println(StringUtil.toString(getTagNames(TagConstants.GROUP_SERVICE)));
    }
}
