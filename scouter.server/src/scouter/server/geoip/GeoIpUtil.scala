package scouter.server.geoip;

import java.net.InetAddress;
import java.net.UnknownHostException;

import scouter.lang.TextTypes;
import scouter.lang.pack.XLogPack;
import scouter.io.DataInputX;
import scouter.server.db.TextPermWR;
import scouter.util.HashUtil;
import scouter.util.IntKeyLinkedMap;
import scouter.util.IntLinkedSet;
import scouter.util.StringUtil;

import com.maxmind.geoip.Location;

object GeoIpUtil {
    private val citySet = new IntLinkedSet().setMax(10000);
    private val locationCache = new IntKeyLinkedMap[Location]().setMax(10000);

    def setNationAndCity(p: XLogPack) {
        if (p.ipaddr == null || p.ipaddr.length != 4)
            return ;

        if (GISDataUtil.isOk() == false)
            return ;

        if (StringUtil.isEmpty(p.countryCode) == false)
            return ;

        if (isPrivateIp(p.ipaddr))
            return ;

        val ipInt = DataInputX.toInt(p.ipaddr, 0);
        if (ipInt == 0)
            return ;

        var loc = locationCache.get(ipInt);
        if (loc == null) {
            loc = GISDataUtil.getGeoIPInfos(InetAddress.getByAddress(p.ipaddr));
            if (loc == null)
                return;

            if (StringUtil.isEmpty(loc.city)) {
                loc.city = "unknown";
            }
            locationCache.put(ipInt, loc);

            val cityHash = HashUtil.hash(loc.city);
            if (citySet.contains(cityHash) == false) {
                citySet.put(cityHash);
                TextPermWR.add(TextTypes.CITY, cityHash, loc.city);
            }
        }

        p.countryCode = loc.countryCode;
        p.city = HashUtil.hash(loc.city);

    }

    private def isPrivateIp(ip: Array[Byte]): Boolean = {
        ip(0) & 0xff match {
            case 127 | 10 => return true
            case 172 => return (ip(1) >= 16 && ip(1) <= 31)
            case 192 => return ip(1) == 168
            case _ => return false
        }
        return false;
    }

}
