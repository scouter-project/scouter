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

            if (value instanceof TextValue) {
                String text = CastUtil.cString(value);
                threadContents.keyValueDataList.add(new KeyValueData(key,text));
            } else {
                if (value instanceof DecimalValue) {
                    Long longValue = new Long( ((DecimalValue) value).value);
                    threadContents.keyValueDataList.add(new KeyValueData(key,longValue));
                } else if (value instanceof DoubleValue) {
                    Double doubleValue = new Double( ((DoubleValue)value).value );
                    threadContents.keyValueDataList.add(new KeyValueData(key,doubleValue));
                } else if( value instanceof FloatValue) {
                    Float floatValue = new Float( ((FloatValue)value).value );
                    threadContents.keyValueDataList.add(new KeyValueData(key,floatValue));
                }

            }
            if (key.startsWith("Service")) {
                serviceThread = true;
            }
        }
        threadContents.serviceThread=serviceThread;
        return threadContents;
    }
}
