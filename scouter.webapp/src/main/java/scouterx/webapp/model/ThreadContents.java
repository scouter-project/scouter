package scouterx.webapp.model;

import lombok.Data;
import scouter.lang.pack.MapPack;
import scouter.lang.value.*;
import scouter.util.CastUtil;
import scouter.util.FormatUtil;

import java.util.ArrayList;
import java.util.List;
@Data
public class ThreadContents {
    boolean serviceThread;
    int time;
    List<KeyValueData> keyValueDataList;

    public static ThreadContents of(MapPack pack) {
        ThreadContents threadContents = new ThreadContents();
        threadContents.keyValueDataList = new ArrayList<>();
        threadContents.time = CastUtil.cint(pack.get("Service Elapsed"));
        String[] names = scouter.util.SortUtil.sort_string(pack.keys(), pack.size());
        boolean serviceThread = false;
        for (String name : names) {
            String key = name;
            Value value = pack.get(key);
            if ("Stack Trace".equals(key)) {
                CastUtil.cString(value);
                continue;
            }
            String text = null;

            if (value instanceof TextValue) {
                text = CastUtil.cString(value);
                threadContents.keyValueDataList.add(new KeyValueData(key,text));
            } else {
                if (value instanceof DecimalValue) {
                    text = FormatUtil.print(value, "#,##0");
                } else if (value instanceof DoubleValue || value instanceof FloatValue) {
                    text = FormatUtil.print(value, "#,##0.0##");
                }
                threadContents.keyValueDataList.add(new KeyValueData(key,text));
            }
            if (key.startsWith("Service")) {
                serviceThread = true;
            }
        }
        threadContents.serviceThread=serviceThread;
        return threadContents;
    }
}
