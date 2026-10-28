package scouter.agent.counter.task;

import java.lang.management.ManagementFactory;

import com.sun.management.OperatingSystemMXBean;

import scouter.agent.counter.CounterBasket;
import scouter.agent.counter.anotation.Counter;
import scouter.agent.counter.meter.MeterResource;
import scouter.lang.TimeTypeEnum;
import scouter.lang.counters.CounterConstants;
import scouter.lang.pack.PerfCounterPack;
import scouter.lang.value.DecimalValue;
import scouter.lang.value.FloatValue;

public class ProcessCpuTask {

    private static final OperatingSystemMXBean OS =
        ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);

    public MeterResource cpuTimeInfo = new MeterResource();
    public MeterResource processCpuInfo = new MeterResource();

    private long oldCpu = -1L;

    @Counter
    public void cpu(CounterBasket pw) {
        if (OS == null) {
            return;
        }

        long cpu = OS.getProcessCpuTime();
        if (cpu > 0) {
            if (oldCpu > 0 && cpu >= oldCpu) {
                long dTime = cpu - oldCpu;
                cpuTimeInfo.add(dTime);

                PerfCounterPack p = pw.getPack(TimeTypeEnum.REALTIME);
                p.put(CounterConstants.JAVA_CPU_TIME, new DecimalValue(dTime));

                p = pw.getPack(TimeTypeEnum.FIVE_MIN);
                p.put(CounterConstants.JAVA_CPU_TIME,
                    new DecimalValue((long) cpuTimeInfo.getSum(300)));
            }
            oldCpu = cpu;
        }

        double load = OS.getProcessCpuLoad();
        if (load >= 0) {
            if (load > 1.0d) {
                load = 1.0d;
            }
            float procCpu = (float) (load * 100.0d);
            processCpuInfo.add(procCpu);

            PerfCounterPack p = pw.getPack(TimeTypeEnum.REALTIME);
            p.put(CounterConstants.JAVA_PROCESS_CPU, new FloatValue(procCpu));

            p = pw.getPack(TimeTypeEnum.FIVE_MIN);
            p.put(CounterConstants.JAVA_PROCESS_CPU,
                new FloatValue((float) processCpuInfo.getAvg(300)));
        }
    }
}