package scouter.server.tagcnt.first

import scouter.lang.value.Value

 class FirstTCData(_objType: String, _time: Long, _tagKey: Long, _tagValue: Value, _cnt: Int) {
        val objType = _objType;
        val time = _time;
        val tagKey = _tagKey;
        val tagValue = _tagValue;
        val cnt = _cnt;
 }