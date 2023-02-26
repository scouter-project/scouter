package scouterx.webapp.layer.consumer;


import scouter.lang.pack.MapPack;
import scouter.net.RequestCmd;
import scouterx.webapp.framework.client.net.TcpProxy;
import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.layer.service.ObjectService;
import scouterx.webapp.model.configure.ConfApplyScopeEnum;
import scouterx.webapp.model.configure.ConfObjectState;
import scouterx.webapp.request.SetConfigKvRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author yosong.heo (yosong.heo@gmail.com) on 2023. 2. 18.
 */
public class ConfigureKVHandleConsumer {


    private final ObjectService agentService;

    public ConfigureKVHandleConsumer(){
        this.agentService = new ObjectService();
    }

    public boolean saveKVServerConfig(SetConfigKvRequest configRequest, Server server) {
        boolean isSuccess = false;

        try {
            String text = loadConfigureText(server.getId(), 0);
            Pattern pattern = Pattern.compile("(?m)^" + configRequest.getKey() + "\\s*=.*\\n?");
            Matcher matcher = pattern.matcher(text);
            String replacement = configRequest.getKey() + "=" + configRequest.getValue() + "\n";

            String replacedText = null;
            if (matcher.find()) {
                replacedText = matcher.replaceFirst(replacement);
            } else {
                replacedText = new StringBuilder(text)
                        .append("\n\n#auto-added\n")
                        .append(replacement).toString();}

            if (saveConfigure(server.getId(), 0, replacedText)) {
                isSuccess = true;
            }
        }catch (Throwable e){
            e.printStackTrace();
        }
        return isSuccess;
    }
    public Optional<List<ConfObjectState>> applyConfig(ConfApplyScopeEnum scope, SetConfigKvRequest configRequest, String objType, Server server) {
        List<ConfObjectState> resulList = null;
        
        switch (scope) {
            case TYPE_IN_SERVER:
                resulList = applyConfigToTypeInServer(configRequest.getKey(), configRequest.getValue(), objType, server);
                break;
            default:
        }
        return Optional.ofNullable(resulList);
    }


    private  List<ConfObjectState> applyConfigToTypeInServer(String confKey, String confValue, String objType, Server server) {
        List<ConfObjectState> resultList= new ArrayList<>();
        this.agentService.retrieveObjectList(server)
                .stream()
                .filter(object -> objType.equals(object.getObjType()))
                .forEach(object -> {
                    try {
                        if (!object.isAlive()) {
                            resultList.add(new ConfObjectState(object.getObjHash(),false));
                        } else {
                            String text = loadConfigureText(server.getId(), object.getObjHash());
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

                            if (saveConfigure(server.getId(), object.getObjHash(), replacedText)) {
                                resultList.add(new ConfObjectState(object.getObjHash(),true));
                            } else {
                                resultList.add(new ConfObjectState(object.getObjHash(),false));
                            }
                        }
                    } catch (ConfigLoadException e) {
                        resultList.add(new ConfObjectState(object.getObjHash(),false));
                    }
                });

        return resultList;
    }


    private String loadConfigureText(final int serverId, final int objHash) throws ConfigLoadException {
        String content = null;
        MapPack resultMapPack = null;
        TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
        try {
            MapPack param = new MapPack();
            if(objHash == 0 ) {
                resultMapPack = (MapPack) tcp.getSingle(RequestCmd.GET_CONFIGURE_SERVER, param);
            }else{
                param.put("objHash", objHash);
                resultMapPack = (MapPack) tcp.getSingle(RequestCmd.GET_CONFIGURE_WAS, param);
            }

        } catch (Throwable throwable) {
//            ConsoleProxy.errorSafe(throwable.getMessage() + " : error on loadConfigureText.");
            throw new ConfigLoadException();
        }

        if (resultMapPack != null) {
            if(objHash == 0 ) {
                content = resultMapPack.getText("serverConfig");
            }else{
                content = resultMapPack.getText("agentConfig");
            }

        }
        return content;
    }


    private boolean saveConfigure(final int serverId, final int objHash, String configText) {
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
        }
        return success;
    }


    public class ConfigLoadException extends Exception{

    }
}
