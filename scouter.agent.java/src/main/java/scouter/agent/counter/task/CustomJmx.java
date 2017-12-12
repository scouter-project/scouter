package scouter.agent.counter.task;

import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.counter.CounterBasket;
import scouter.agent.counter.anotation.Counter;
import scouter.agent.counter.jmx.LazyPlatformMBeanServer;
import scouter.lang.TimeTypeEnum;
import scouter.lang.pack.PerfCounterPack;
import scouter.lang.value.FloatValue;
import scouter.util.StringEnumer;
import scouter.util.StringSet;
import scouter.util.StringUtil;

public class CustomJmx {

    Configure conf = Configure.getInstance();
    LazyPlatformMBeanServer mBeanServer;
    boolean mBeanServerEnable = true;

    @Counter
    public void extractJmx(CounterBasket pw) {
        if (conf.counter_custom_jmx_enabled == false || mBeanServerEnable == false) {
            return;
        }
        StringSet nameSet = conf.getCustomJmxSet();
        if (nameSet.size() < 1) {
            return;
        }
        if (mBeanServer == null) {
            mBeanServer = LazyPlatformMBeanServer.create();
        }
        try {
            if (mBeanServer.checkInit()) {
                StringEnumer stringEnumer = nameSet.keys();
                PerfCounterPack pack = pw.getPack(TimeTypeEnum.REALTIME);
                while (stringEnumer.hasMoreElements()) {
                    String next = stringEnumer.nextString();
                    String[] mbeanAndAttribute = StringUtil.split(next, "|");
                    if (mbeanAndAttribute.length != 3) continue;
                    float value = mBeanServer.getValue(mbeanAndAttribute[1], mbeanAndAttribute[2]);
                    if (value >= 0) {
                        pack.put(mbeanAndAttribute[0], new FloatValue(value));
                    }
                }
            }
        } catch (Exception e) {
            Logger.println("SC-555", e.getMessage(), e);
            mBeanServerEnable = false;
        }

    }
}
