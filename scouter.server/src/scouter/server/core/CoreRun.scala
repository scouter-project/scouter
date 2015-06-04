package scouter.server.core

import scouter.server.ShutdownManager
import scouter.util.IShutdown

object CoreRun {
    val MAX_QUE_SIZE = 10000;

    var running = true;
    def shutdown() {
        running = false;
    }
    ShutdownManager.add(new IShutdown() {
       override def shutdown() {
            running = false;
        }
    });
}
