package scouter.agent.plugin;

import scouter.lang.pack.PerfCounterPack;

abstract public class AbstractCounter extends AbstractPlugin {

    abstract public void counter(PerfCounterPack pack);

}
