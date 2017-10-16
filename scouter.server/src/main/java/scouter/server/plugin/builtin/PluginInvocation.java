package scouter.server.plugin.builtin;

import scouter.lang.pack.Pack;
import scouter.server.Configure;
import scouter.server.Logger;

import java.lang.reflect.Method;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2016. 3. 19.
 */
public class PluginInvocation {
    Object object;
    Method method;

    public PluginInvocation(Object object, Method method) {
        this.object = object;
        this.method = method;
    }
    public void process(Pack pack) {
        try {
            method.invoke(object, pack);
        } catch (Throwable t) {
            Logger.println("G003", "[Plugin invoke fail]" + object.getClass() + " " + method + " " + t);
            if(Configure.getInstance()._trace) {
                t.printStackTrace();
            }
        }
    }
}
