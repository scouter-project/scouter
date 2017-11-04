package scouter.agent.plugin;

import scouter.agent.counter.CounterBasket;
import scouter.lang.TimeTypeEnum;

public class PluginCounter {

    static AbstractCounter plugIn;

    static {
        PluginLoader.getInstance();
    }

    public static void counter(CounterBasket cw) {

        if (plugIn != null) {
            try {
                plugIn.counter(cw.getPack(TimeTypeEnum.REALTIME));
            } catch (Throwable th) {
            }
        }

    }
}
