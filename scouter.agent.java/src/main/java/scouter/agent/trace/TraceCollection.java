package scouter.agent.trace;

import scouter.agent.Configure;
import scouter.agent.error.TOO_MANY_COLLECTION_MEMBERS;
import scouter.agent.netio.data.DataProxy;
import scouter.lang.AlertLevel;
import scouter.lang.TextTypes;
import scouter.lang.pack.AlertPack;
import scouter.lang.step.HashedMessageStep;
import scouter.lang.value.MapValue;
import scouter.util.ThreadUtil;

import java.util.Map;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2016. 9. 23.
 */
public class TraceCollection {
    private static Configure conf = Configure.getInstance();
    private static IllegalStateException tooManyCollectionMemebers = new TOO_MANY_COLLECTION_MEMBERS("TOO_MANY_COLLECTION_MEMBERS");

    public static void endPut(Map map) {
        int size = map.size();
        if(size > 0 && size % conf._hook_map_impl_warning_size == 0) {
            TraceContext ctx = TraceContextManager.getContext();

            if(ctx == null) return;
            if(ctx.error != 0) return;

            MapValue mv = new MapValue();
            mv.put(AlertPack.HASH_FLAG + TextTypes.SERVICE + "_service-name", ctx.serviceHash);
            String message = "Too many Map entries!\n" + ThreadUtil.getStackTrace(Thread.currentThread().getStackTrace(), 2);
            HashedMessageStep step = new HashedMessageStep();
            step.hash = DataProxy.sendHashedMessage(message);
            step.value = size;
            step.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
            ctx.profile.add(step);
            mv.put(AlertPack.HASH_FLAG + TextTypes.HASH_MSG + "_full-stack", step.hash);

            DataProxy.sendAlert(AlertLevel.WARN, "TOO_MANY_MAP_ENTRIES", "too many Map entries, over #" + size, mv);
            int errorMessageHash = DataProxy.sendError("too many Map entries, over #" + size);
            if (ctx.error == 0) {
                ctx.error = errorMessageHash;
            }
            ctx.offerErrorEntity(ErrorEntity.of(tooManyCollectionMemebers, errorMessageHash, 0, 0));
        }
    }
}
