/*
 *  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 *
 */
package scouterx.webapp.framework.client.net;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.Value;
import scouter.net.RequestCmd;
import scouter.net.TcpFlag;
import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.framework.client.server.ServerManager;
import scouterx.webapp.framework.exception.ErrorState;
import scouterx.webapp.framework.configure.ConfigureAdaptor;
import scouterx.webapp.framework.configure.ConfigureManager;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class TcpProxy implements AutoCloseable {
    private static final ConfigureAdaptor conf = ConfigureManager.getConfigure();
    private final ClientTCP tcp = new ClientTCP();
    private Server server;

    @Getter
    @Setter
    private long lastUsed;

    protected TcpProxy() {
    }

    protected TcpProxy(int serverId) {
        this.server = ServerManager.getInstance().getServer(serverId);
        log.info("TcpProxy created : pool-size:{}, {}", server.getConnectionPool().getCurrentPoolSize(), this);
    }

    public static synchronized TcpProxy getTcpProxy(final Server server) {
        Server _server = ServerManager.getInstance().getServerIfNullDefault(server);
        if (_server == null || _server.isOpen() == false) {
            throw ErrorState.COLLECTOR_NOT_CONNECTED.newBizException("server is not exist or before initializing! - "
                    + ((server == null) ? "null" : String.valueOf(server.getId())));
        }

        ConnectionPool pool = _server.getConnectionPool();
        TcpProxy tcpProxy = pool.getTcpProxy();
        return tcpProxy != null ? tcpProxy : new TcpProxy(_server.getId());
    }

    public static synchronized TcpProxy getTcpProxy(int serverId) {
        return getTcpProxy(ServerManager.getInstance().getServer(serverId));
    }

    public static synchronized void close(TcpProxy t) {
        if (t == null)
            return;
        try {
            t.close();
        } catch (Throwable throwable) {
        }
    }

    protected ClientTCP getClientTcp() {
        return tcp;
    }

    public Server getServer() {
        return this.server;
    }

    public synchronized void open() {
        if (tcp.connected() == false) {
            tcp.open(this.server.getId());
            if (tcp.connected() == false) {
                server.setOpen(false);
            } else {
                server.setOpen(true);
            }
        }
    }

    public synchronized void realClose() {
        String lastStack = "";
        if (conf.isTrace()) {
            lastStack = Arrays.stream(new Exception().getStackTrace()).map(StackTraceElement::toString).limit(6).collect(Collectors.joining("\\n "));
        }
        log.info("TcpProxy closed : pool-size:{}, {}, stack:{}", server.getConnectionPool().getCurrentPoolSize(), this, lastStack);
        sendClose();
        tcp.close();
    }

    @Override
    protected void finalize() throws Throwable {
        tcp.close();
    }

    public Pack getSingle(String cmd, Pack param) {
        List<Pack> values = process(cmd, param);
        if (values == null || values.size() == 0)
            return null;
        else
            return values.get(0);
    }

    public List<Pack> process(String cmd, Pack param) {

        final List<Pack> list = new ArrayList<Pack>();
        process(cmd, param, in -> {
            Pack p = in.readPack();
            list.add(p);
        });
        return list;
    }

    public Value getSingleValue(String cmd, Pack param) {
        List<Value> values = processValues(cmd, param);
        if (values == null || values.size() == 0)
            return null;
        else
            return values.get(0);
    }

    public Value getSingleValue(String cmd, Value param) {
        List<Value> values = processValues(cmd, param);
        if (values == null || values.size() == 0)
            return null;
        else
            return values.get(0);
    }

    public List<Value> processValues(String cmd, Value param) {
        final List<Value> list = new ArrayList<Value>();
        process(cmd, param, in -> {
            Value v = in.readValue();
            list.add(v);
        });
        return list;
    }

    public List<Value> processValues(String cmd, Pack param) {
        final List<Value> list = new ArrayList<Value>();
        process(cmd, param, in -> {
            Value v = in.readValue();
            list.add(v);
        });
        return list;
    }

    public boolean isValid() {
        return tcp.connected();
    }

    public synchronized void process(String cmd, Object param, INetReader recv) {
        open();
        if (tcp.connected() == false) {
            throw ErrorState.CLIENT_SOCKET_CLOSED.newBizException("[TcpProxy.process] client socket closed.");
        }

        long session = this.server.getSession();

        DataOutputX out = tcp.getOutput();
        DataInputX in = tcp.getInput();
        try {
            out.writeText(cmd);
            out.writeLong(session);
            if (param instanceof Value) {
                out.writeValue((Value) param);
            } else if (param instanceof Pack) {
                out.writePack((Pack) param);
            }
            out.flush();
            byte resFlag;
            while ((resFlag = in.readByte()) == TcpFlag.HasNEXT) {
                recv.process(in);
            }
            if (resFlag == TcpFlag.INVALID_SESSION) {
                server.setSession(0); // SessionObserver will relogin
                tcp.close();
                throw ErrorState.COLLECTOR_INVALID_SESSION.newBizException();
            }
        } catch (Throwable e) {
            tcp.close();
            throw new RuntimeException(e);
        }
    }

    public synchronized void sendClose() {
        if (tcp.connected() == false) {
            return;
        }
        DataOutputX out = tcp.getOutput();
        try {
            out.writeText(RequestCmd.CLOSE);
            out.flush();
        } catch (Exception e) {
        }
    }

    public static MapPack loginByCleanConnection(int serverId, MapPack param) throws IOException {
        TcpProxy proxy = new TcpProxy(serverId);
        proxy.open();
        if (proxy.isValid() == false) {
            return null;
        }
        param.put("ip", proxy.getLocalInetAddress().getHostAddress());
        DataOutputX out = proxy.getClientTcp().getOutput();
        DataInputX in = proxy.getClientTcp().getInput();
        try {
            out.writeText(RequestCmd.LOGIN);
            out.writeLong(0);
            out.writePack(param);
            out.flush();
            MapPack pack = null;
            while (in.readByte() == TcpFlag.HasNEXT) {
                pack = (MapPack) in.readPack();
            }
            return pack;
        } finally {
            proxy.realClose();
        }
    }

    public InetAddress getLocalInetAddress() {
        return tcp.getSocket().getLocalAddress();
    }

    @Override
    public void close() {
        ConnectionPool pool = this.getServer().getConnectionPool();
        if (this.isValid()) {
            pool.put(this);
        } else {
            this.realClose();
        }
    }
}
