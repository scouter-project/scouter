package scouterx.webapp.layer.consumer;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import scouter.lang.conf.ValueType;
import scouter.lang.conf.ValueTypeDesc;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.lang.value.MapValue;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouterx.webapp.framework.client.net.TcpProxy;
import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.model.configure.ConfObject;
import scouterx.webapp.model.configure.ConfigureData;
import scouterx.webapp.request.SetConfigRequest;

import java.util.*;

@Slf4j
public class ConfigureConsumer {
    final int STATUS_SUCCESS_CODE = 200;
    final int STATUS_FAIL_CODE = 404;


    //- save
    public ConfigureData saveServerConfig(SetConfigRequest configRequest, Server server) {
        ConfigureData configureData = new ConfigureData();
        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server.getId())) {
            MapPack param = new MapPack();
            param.put("setConfig", configRequest.getValues().replaceAll("\\\\", "\\\\\\\\"));
            MapPack out = (MapPack) tcpProxy.getSingle(RequestCmd.SET_CONFIGURE_SERVER, param);
            if(Objects.nonNull(out)){
                String config = out.getText("result");
                if ("true".equalsIgnoreCase(config)) {
                    loadConfigList(configureData,tcpProxy,RequestCmd.LIST_CONFIGURE_SERVER, null);
                    configureData.setStatus(STATUS_SUCCESS_CODE);
                }else{
                    // fail
                    configureData.setStatus(STATUS_FAIL_CODE);
                }
            }
        }
        return configureData;
    }
    public ConfigureData saveObjectConfig(SetConfigRequest configRequest, int objHash, Server server) {
        ConfigureData configureData = new ConfigureData();
        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server.getId())) {
            MapPack param = new MapPack();
            param.put("setConfig", configRequest.getValues().replaceAll("\\\\", "\\\\\\\\"));
            param.put("objHash", objHash);
            MapPack out = (MapPack) tcpProxy.getSingle(RequestCmd.SET_CONFIGURE_WAS, param);
            if(Objects.nonNull(out)){
                String config = out.getText("result");
                if ("true".equalsIgnoreCase(config)) {
                    MapPack param2 = new MapPack();
                    param2.put("objHash",objHash);
                    loadConfigList(configureData,tcpProxy,RequestCmd.LIST_CONFIGURE_WAS, param2);
                    configureData.setStatus(STATUS_SUCCESS_CODE);
                }else{
                    // fail
                    configureData.setStatus(STATUS_FAIL_CODE);
                }
            }
        }
        return configureData;
    }

    //- retrieve
    public ConfigureData retrieveObjectConfig(int objHash, Server server) {
        ConfigureData configureData = new ConfigureData();
        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server.getId())) {
            MapPack param = new MapPack();
            param.put("objHash", objHash);
            this.loadConfig(configureData,tcpProxy,RequestCmd.GET_CONFIGURE_WAS, param,false);
            loadConfigList(configureData,tcpProxy,RequestCmd.LIST_CONFIGURE_WAS, param);
            loadConfigDesc(configureData,tcpProxy,param);
            loadConfigValueType(configureData,tcpProxy,param);
            loadConfigValueTypeDesc(configureData,tcpProxy,param);
            configureData.setStatus(STATUS_SUCCESS_CODE);
        }
        return configureData;
    }
    public ConfigureData retrieveServerConfig(Server server,boolean isServer) {

        ConfigureData configureData = new ConfigureData();

        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server.getId())) {
            this.loadConfig(configureData,tcpProxy,RequestCmd.GET_CONFIGURE_SERVER, null,isServer);
            loadConfigList(configureData,tcpProxy,RequestCmd.LIST_CONFIGURE_SERVER, null);
            loadConfigDesc(configureData,tcpProxy,new MapPack());
            loadConfigValueType(configureData,tcpProxy,new MapPack());
            loadConfigValueTypeDesc(configureData,tcpProxy,new MapPack());
            configureData.setStatus(STATUS_SUCCESS_CODE);
        }

        return configureData;
    }
    // -- loading process methods
    private void loadConfig(ConfigureData configureData,TcpProxy tcpProxy,final String requestCmd, final MapPack param,boolean isServer) {
        MapPack pack = (MapPack)tcpProxy.getSingle(requestCmd,param);
        if(Objects.nonNull(pack)){
            if(isServer) {
                configureData.setContents(pack.getText("serverConfig"));
            }else{
                configureData.setContents(pack.getText("agentConfig"));
            }
        }
    }
    private void loadConfigList(ConfigureData configureData,TcpProxy tcpProxy,final String requestCmd, final MapPack param) {
        MapPack pack = (MapPack) tcpProxy.getSingle(requestCmd, param);
        if (pack != null) {
            final ListValue keyList = pack.getList("key");
            final ListValue valueList = pack.getList("value");
            final ListValue defaultList = pack.getList("default");
            for (int i = 0; i < keyList.size(); i++) {
                ConfObject obj = new ConfObject();
                String key = CastUtil.cString(keyList.get(i));
                String value = CastUtil.cString(valueList.get(i));
                String def = CastUtil.cString(defaultList.get(i));
                obj.key = Strings.nullToEmpty(key);
                obj.value = Strings.nullToEmpty(value);
                obj.def = Strings.nullToEmpty(def);
                configureData.getConfigStateList().add(obj);
            }
        }



    }
    private void loadConfigDesc(ConfigureData configureData,TcpProxy tcpProxy,final MapPack param) {
        MapPack pack= (MapPack)tcpProxy.getSingle(RequestCmd.CONFIGURE_DESC, param);
        if (Objects.nonNull(pack)) {
            Iterator<String> keys = pack.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                configureData.getDescMap().put(key, pack.getText(key));
                if (key.contains("$")) {
                    configureData.getDescMap().put(removeVariableString(key), pack.getText(key));
                }
            }
        }
    }
    private void loadConfigValueType(ConfigureData configureData,TcpProxy tcpProxy,final MapPack param) {
        MapPack pack = (MapPack) tcpProxy.getSingle(RequestCmd.CONFIGURE_VALUE_TYPE, param);
        if (Objects.nonNull(pack)) {
            Iterator<String> keys = pack.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                configureData.getValueTypeMap().put(key, ValueType.of(pack.getInt(key)));
                if (key.contains("$")) {
                    configureData.getValueTypeMap().put(this.removeVariableString(key), ValueType.of(pack.getInt(key)));
                }

            }
        }

    }
    private void loadConfigValueTypeDesc(ConfigureData configureData,TcpProxy tcpProxy,final MapPack param) {
        MapPack pack = (MapPack) tcpProxy.getSingle(RequestCmd.CONFIGURE_VALUE_TYPE_DESC, param);
        if (Objects.nonNull(pack)) {
            Iterator<String> keys = pack.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                configureData.getValueTypeDescMap().put(key, ValueTypeDesc.of((MapValue) pack.get(key)));
                if (key.contains("$")) {
                    configureData.getValueTypeDescMap().put(this.removeVariableString(key), ValueTypeDesc.of((MapValue) pack.get(key)));
                }
            }
        }
    }

    public String removeVariableString(String text) {
        StringBuilder resultBuilder = new StringBuilder(text.length());
        char[] org = text.toCharArray();
        boolean sink = false;
        for (int i = 0; i < org.length; i++) {
            switch(org[i]) {
                case '$':
                    sink = !sink;
                    break;
                default:
                    if (!sink) {
                        resultBuilder.append(org[i]);
                    }
                    break;
            }

        }
        return resultBuilder.toString();
    }



}
