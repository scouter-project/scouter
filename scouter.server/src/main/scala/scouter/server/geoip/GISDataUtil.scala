package scouter.server.geoip;

import java.io.File
import java.net.InetAddress
import java.net.UnknownHostException
import scouter.server.Configure
import scouter.server.Logger
import scouter.util.CompareUtil
import scouter.util.StopWatch
import com.maxmind.geoip.Location
import com.maxmind.geoip.LookupService;
import scouter.server.ConfObserver

object GISDataUtil {

    val conf = Configure.getInstance()

    var lookupService: LookupService = null

    var path = conf.geoip_data_city_file;
    ConfObserver.put("GISDataUtil") {
        if (CompareUtil.equals(path, conf.geoip_data_city_file) == false) {
            path = conf.geoip_data_city_file;
            load();
        }
    }
    load()

    def load() {
        try {
            val fGeoIpDB = new File(conf.geoip_data_city_file);
            if (fGeoIpDB.exists() == false) {
                Logger.println("S145", "GeoIP db file is not readable : " + fGeoIpDB.getCanonicalPath());
            } else {
                lookupService = new LookupService(fGeoIpDB, LookupService.GEOIP_MEMORY_CACHE);
                Logger.println("S146", "GeoIP db file is loaded : " + fGeoIpDB.getCanonicalPath());
            }
        } catch {
            case e: Throwable =>
                e.printStackTrace();
        }
    }

    def isOk(): Boolean = {
        lookupService != null;
    }
    val dummy = new Location();
    def getGeoIPInfos(ip: InetAddress): Location = {
        if (lookupService == null) {
            return dummy;
        }
        return lookupService.getLocation(ip);
    }

}
