package scouter.server.plugin.builtin;

import scouter.lang.pack.Pack;
import scouter.lang.plugin.annotation.ServerPlugin;
import scouter.server.Configure;
import scouter.server.Logger;
import scouter.util.scan.Scanner;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2016. 3. 19.
 */
public class BuiltInPluginManager {

    public static Map<String, List<PluginInvocation>> pluginMap = new HashMap<String, List<PluginInvocation>>();

    public static void loadPlugins() {
        Set<String> classNames = new Scanner("scouter.plugin.server").process();

        Iterator<String> itr = classNames.iterator();

        while (itr.hasNext()) {
            try {
                Class c = Class.forName(itr.next());
                if (!Modifier.isPublic(c.getModifiers()))
                    continue;

                Method[] m = c.getDeclaredMethods();
                for (int i = 0; i < m.length; i++) {
                    ServerPlugin annotation = m[i].getAnnotation(ServerPlugin.class);
                    if (annotation == null)
                        continue;

                    String pluginPoint = annotation.value();
                    List<PluginInvocation> pluginList = pluginMap.get(pluginPoint);

                    if(pluginList == null) {
                        pluginList = new ArrayList<PluginInvocation>();
                        pluginMap.put(pluginPoint, pluginList);
                    }

                    Logger.println("[BuiltInPlugin]" + c.getName() + "=>" + m[i].getName());
                    pluginList.add(new PluginInvocation(c.newInstance(), m[i]));
                }
            } catch (Throwable t) {
                Logger.println("Server Plugin Load Error");
                if(Configure.getInstance()._trace) {
                    t.printStackTrace();
                }
            }
        }
    }

    public static List getPluginList(String pluginPoint) {
        if(pluginMap == null) return null;
        return pluginMap.get(pluginPoint);
    }

    public static void invokeAllPlugins(String pluginPoint, Pack pack) {
        if(pluginMap == null) return;

        List<PluginInvocation> pluginList = pluginMap.get(pluginPoint);

        if(pluginList == null) return;

        int count = pluginList.size();
        for(int i=0; i<count; i++) {
            PluginInvocation invocation = pluginList.get(i);
            invocation.process(pack);
        }
    }

}
