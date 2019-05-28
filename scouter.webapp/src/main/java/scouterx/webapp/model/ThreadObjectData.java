package scouterx.webapp.model;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import scouter.lang.value.ListValue;

/**
 * @author Created by David Kim (david100gom@gmail.com) on 2019. 5. 22.
 *
 * Github : https://github.com/david100gom
 */
@Getter
@ToString
@AllArgsConstructor
public class ThreadObjectData {

    long id;
    String name;
    String stat;
    long cpu;
    String txid;
    String elapsed;
    String service;

    public ThreadObjectData(Map<String,ListValue> threadObjectMap, int index) {
        this.id = threadObjectMap.get("id").getLong(index);
        this.name = threadObjectMap.get("name").getString(index);
        this.stat = threadObjectMap.get("stat").getString(index);
        this.cpu = threadObjectMap.get("cpu").getLong(index);
        this.txid = threadObjectMap.get("txid").getString(index);
        this.elapsed = threadObjectMap.get("elapsed").getString(index);
        this.service = threadObjectMap.get("service").getString(index);
    }

}
