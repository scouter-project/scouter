package scouter.server
import scouter.server.util.ThreadScala
import scouter.util.ThreadUtil
import scouter.util.DateUtil
object Test {
    def main(args: Array[String]) {
        var running = true
        ThreadScala.startDaemon("scouter.server.Test", { running }) {
            println(DateUtil.timestamp() + "   ok");
            ThreadUtil.sleep(1000);
        }
        ThreadUtil.sleep(5000);
        running = false;
        ThreadUtil.sleep(5000);
    }
}
