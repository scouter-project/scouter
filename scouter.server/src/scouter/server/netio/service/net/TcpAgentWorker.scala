package scouter.server.netio.service.net

import java.net.Socket
import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.lang.pack.Pack
import scouter.lang.pack.MapPack
import scouter.net.TcpFlag
import scouter.util.FileUtil
import scouter.net.TcpFlag
import scouter.server.Configure
import scouter.util.DateUtil

class TcpAgentWorker(socket: Socket, in: DataInputX, out: DataOutputX) {

    val remoteAddr = socket.getRemoteSocketAddress()
    socket.setSoTimeout(Configure.getInstance().tcp_agent_so_timeout)

    var lastWriteTime = System.currentTimeMillis()
    def write(cmd: String, p: Pack) {
        if (socket.isClosed())
            return

        try {
            out.writeText(cmd);
            out.writePack(p);
            out.flush();
            lastWriteTime = System.currentTimeMillis()
        } catch {
            case _: Throwable => close()
        }
    }
    def readPack(): Pack = {
        try {
            return if (socket.isClosed()) null else in.readPack()
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
    def read(handler: (DataInputX, DataOutputX) => Unit) {
        try {
            handler(in, out);
        } catch {
            case e: Throwable =>
                e.printStackTrace()
                close();
        }
    }

    def isClosed() = socket.isClosed()

    val conf = Configure.getInstance()

    def isExpired() = { System.currentTimeMillis() - lastWriteTime >= conf.tcp_agent_keepalive }
    def sendKeepAlive() {
        if (socket.isClosed())
            return
        write("KEEP_ALIVE", new MapPack())
        try {
            while (TcpFlag.HasNEXT == in.readByte()) {
                in.readPack()
            }
        } catch {
            case e: Throwable => close()
        }
    }

    def close() {
        FileUtil.close(in)
        FileUtil.close(out)
        FileUtil.close(socket)
        if(conf.debug_net){
            println("Agent : " + remoteAddr + " close");
        }
    }
}