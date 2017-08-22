package scouter.client.configuration.views;

import scouter.client.configuration.exception.ConfigLoadException;
import scouter.client.model.AgentModelThread;
import scouter.client.model.AgentObject;
import scouter.client.net.TcpProxy;
import scouter.client.util.ConsoleProxy;
import scouter.lang.pack.MapPack;
import scouter.net.RequestCmd;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 21.
 */
public class ConfigureFileHandleUtil {

    public static Map<AgentObject, Boolean> applyConfig(ConfApplyScopeEnum scope, String confKey, String confValue, String objType, int serverId) {
        Map<AgentObject, Boolean> resultMap = null;
        
        switch (scope) {
            case TYPE_IN_SERVER:
                resultMap = applyConfigToTypeInServer(confKey, confValue, objType, serverId);
                break;
            case TYPE_ALL:
                resultMap = applyConfigToTypeAllServer(confKey, confValue, objType);
                break;
            case FAMILY_IN_SERVER:
                break;
            case FAMILY_ALL:
                break;
            case THIS:
            default:
        }

        return resultMap;
    }

    private static Map<AgentObject, Boolean> applyConfigToTypeAllServer(String confKey, String confValue, String objType) {
        return applyConfigToTypeInServer(confKey, confValue, objType, 0);
    }

    private static Map<AgentObject, Boolean> applyConfigToTypeInServer(String confKey, String confValue, String objType, int serverId) {
    	Map<AgentObject, Boolean> resultMap = new HashMap<>();
        
        AgentModelThread.getInstance().getAgentObjectMap().values().stream()
                .filter(object -> objType.equals(object.getObjType()))
                .filter(object -> serverId == 0 || object.getServerId() == serverId)
                .forEach(object -> {
                    try {
                        if (!object.isAlive()) {
                            resultMap.put(object, false);
                        } else {
                            String text = loadConfigureText(object.getServerId(), object.getObjHash());
                            Pattern pattern = Pattern.compile("(?m)^" + confKey + "\\s*=.*\\n?");
                            Matcher matcher = pattern.matcher(text);
                            String replacement = confKey + "=" + confValue + "\n";

                            String replacedText = null;
                            if (matcher.find()) {
                                replacedText = matcher.replaceFirst(replacement);
                            } else {
                                replacedText = new StringBuilder(text)
                                        .append("\n\n#auto-added\n")
                                        .append(replacement).toString();
                            }

                            if (saveConfigure(object.getServerId(), object.getObjHash(), replacedText)) {
                                resultMap.put(object, true);
                            } else {
                                resultMap.put(object, false);
                            }
                        }
                    } catch (ConfigLoadException e) {
                        resultMap.put(object, false);
                    }
                });

        return resultMap;
    }

    public static String loadConfigureText(final int serverId) throws ConfigLoadException {
        return loadConfigureText(serverId, 0);
    }

    public static String loadConfigureText(final int serverId, final int objHash) throws ConfigLoadException {
        String content = null;
        MapPack resultMapPack = null;
        TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
        try {
            MapPack param = new MapPack();
            param.put("objHash", objHash);
            resultMapPack = (MapPack) tcp.getSingle(RequestCmd.GET_CONFIGURE_WAS, param);
        } catch (Throwable throwable) {
            ConsoleProxy.errorSafe(throwable.getMessage() + " : error on loadConfigureText.");
            throw new ConfigLoadException();
        } finally {
            TcpProxy.putTcpProxy(tcp);
        }
        if (resultMapPack != null) {
            if (objHash == 0) {
                content = resultMapPack.getText("serverConfig");
            } else {
                content = resultMapPack.getText("agentConfig");
            }
        }
        return content;
    }

    public static boolean saveConfigure(final int serverId, String configText) {
        return saveConfigure(serverId, 0, configText);
    }

    public static boolean saveConfigure(final int serverId, final int objHash, String configText) {
        boolean success = false;
        TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
        try {
            MapPack param = new MapPack();
            param.put("setConfig", configText.replaceAll("\\\\", "\\\\\\\\"));
            MapPack out = null;
            if (objHash == 0) {
                out = (MapPack) tcp.getSingle(RequestCmd.SET_CONFIGURE_SERVER, param);
            } else {
                param.put("objHash", objHash);
                out = (MapPack) tcp.getSingle(RequestCmd.SET_CONFIGURE_WAS, param);
            }

            if (out != null) {
                String config = out.getText("result");
                if ("true".equalsIgnoreCase(config)) {
                    success = true;
                } else {
                    success = false;
                }
            }
        } catch (Throwable throwable) {
            success = false;
            ConsoleProxy.errorSafe(throwable.getMessage() + " : error on saveConfigure.");
        } finally {
            TcpProxy.putTcpProxy(tcp);
        }
        return success;
    }
}
