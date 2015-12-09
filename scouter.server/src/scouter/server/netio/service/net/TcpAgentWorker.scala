package scouter.server.netio.service.net

import java.net.Socket
import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.lang.pack.Pack
import scouter.lang.pack.MapPack
import scouter.net.TcpFlag
import scouter.util.FileUtil
import scouter.net.TcpFlag
import scouter.server.{Logger, Configure}
import scouter.util.DateUtil
import scouter.net.NetCafe

class TcpAgentWorker(socket: Socket, in: DataInputX, out: DataOutputX, protocol:Int) {

    val remoteAddr = socket.getRemoteSocketAddress()
    socket.setSoTimeout(Configure.getInstance().net_tcp_agent_so_timeout_ms)

    var lastWriteTime = System.currentTimeMillis()
    def write(cmd: String, p: Pack) {
        if (socket.isClosed())
            return

        try {
            protocol match {
                case NetCafe.TCP_AGENT =>
                    out.writeText(cmd);
                    out.writePack(p);
                case NetCafe.TCP_AGENT_V2 =>
                    val buff = new DataOutputX().writeText(cmd).writePack(p).toByteArray();
                    out.writeIntBytes(buff);
            }
            out.flush();

            lastWriteTime = System.currentTimeMillis()
        } catch {
            case _: Throwable => close()
        }
    }
    def readPack(): Pack = {
        try {
            return if (socket.isClosed()) null else {
                protocol match {
                    case NetCafe.TCP_AGENT =>
                        in.readPack()
                    case NetCafe.TCP_AGENT_V2 =>
                        val buff = in.readIntBytes()
                        new DataInputX(buff).readPack();
                    case _ =>
                         throw new RuntimeException("unknown potocol " );
                }
            }
        } catch {
            case _: Throwable => close();
        }
        return null;
    }
    def readByte(): Byte = {
        try {
            return if (socket.isClosed()) 0 else in.readByte()
        } catch {
            case e: Throwable =>
                e.printStackTrace()
                close();
        }
        return 0;
    }
    def read(handler: (Int, DataInputX, DataOutputX) => Unit) {
        try {
            handler(protocol, in, out);
        } catch {
            case e: Throwable =>
                e.printStackTrace()
                close();
        }
    }

    def isClosed() = socket.isClosed()

    val conf = Configure.getInstance()

    def isExpired() = { System.currentTimeMillis() - lastWriteTime >= conf.net_tcp_agent_keepalive_interval_ms }
    def sendKeepAlive(waitTime: Int) {
        if (socket.isClosed())
            return
        val orgSoTime = socket.getSoTimeout()
        socket.setSoTimeout(waitTime)
        write("KEEP_ALIVE", new MapPack())
        try {
            while (TcpFlag.HasNEXT == in.readByte()) {
                protocol match {
                    case NetCafe.TCP_AGENT =>
                        in.readPack()
                    case NetCafe.TCP_AGENT_V2 =>
                        in.readIntBytes()
                    //버림..
                }
            }
            socket.setSoTimeout(orgSoTime)
        } catch {
            case e: Throwable => close()
        }
    }

    def close() {
        FileUtil.close(in)
        FileUtil.close(out)
        FileUtil.close(socket)
        if (conf.log_tcp_action_enabled) {
            Logger.println("Agent : " + remoteAddr + " close");
        }
    }
}