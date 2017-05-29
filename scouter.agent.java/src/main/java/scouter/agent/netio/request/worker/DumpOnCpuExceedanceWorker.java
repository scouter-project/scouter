package scouter.agent.netio.request.worker;

import scouter.agent.util.DumpUtil;
import scouter.net.RequestCmd;
import scouter.util.RequestQueue;
import scouter.util.ThreadUtil;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2016. 11. 18.
 */
public class DumpOnCpuExceedanceWorker extends Thread {
    private static DumpOnCpuExceedanceWorker instance;
    private RequestQueue<String> queue = new RequestQueue<String>(2);

    public final static synchronized DumpOnCpuExceedanceWorker getInstance() {
        if (instance == null) {
            instance = new DumpOnCpuExceedanceWorker();
            instance.setDaemon(true);
            instance.setName(ThreadUtil.getName(instance));
            instance.start();
        }
        return instance;
    }

    public void add(String dumpReason) {
        if(dumpReason == null) {
            queue.put("x");
        } else {
            queue.put(dumpReason);
        }
    }

    public void run() {
        while (true) {
            String reason = queue.get();
            try {
                if(RequestCmd.TRIGGER_DUMP_REASON_TYPE_CPU_EXCEEDED.equals(reason)) {
                    DumpUtil.autoDumpByCpuExceedance();
                } else {
                    DumpUtil.triggerThreadDump();
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}
