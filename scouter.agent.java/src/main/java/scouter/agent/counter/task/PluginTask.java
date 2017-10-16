package scouter.agent.counter.task;

import scouter.agent.counter.CounterBasket;
import scouter.agent.counter.anotation.Counter;
import scouter.agent.plugin.PluginCounter;

public class PluginTask {

    @Counter
    public void pluginCounter(CounterBasket pw) {
        PluginCounter.counter(pw);
    }
}
