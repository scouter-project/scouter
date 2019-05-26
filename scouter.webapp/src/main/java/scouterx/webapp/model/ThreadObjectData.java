package scouterx.webapp.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.ObjectPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.ListValue;
import scouter.util.StringUtil;
import scouterx.webapp.framework.client.model.AgentModelThread;
import scouterx.webapp.framework.client.model.AgentObject;
import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.framework.client.server.ServerManager;
import scouterx.webapp.model.enums.ActiveServiceMode;
import scouterx.webapp.model.scouter.SActiveService;
import scouterx.webapp.model.scouter.SObject;

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

    public ThreadObjectData(Map<String,ListValue> hostDiskMap, int index) {
        this.id = hostDiskMap.get("id").getLong(index);
        this.name = hostDiskMap.get("name").getString(index);
        this.stat = hostDiskMap.get("stat").getString(index);
        this.cpu = hostDiskMap.get("cpu").getLong(index);
        this.txid = hostDiskMap.get("txid").getString(index);
        this.elapsed = hostDiskMap.get("elapsed").getString(index);
        this.service = hostDiskMap.get("service").getString(index);
    }

}
