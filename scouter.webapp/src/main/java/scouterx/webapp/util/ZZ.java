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

package scouterx.webapp.util;

import org.apache.commons.lang3.StringUtils;
import scouterx.webapp.configure.ConfigureAdaptor;
import scouterx.webapp.configure.ConfigureManager;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 */
public class ZZ {
    private static final ConfigureAdaptor conf = ConfigureManager.getConfigure();
    private static final char BRACKET = '[';
    private static final char COMMA = ',';

    public static String getRequestIp(HttpServletRequest request) {
        if (StringUtils.isNotBlank(conf.getNetHttpApiAuthIpHeaderKey())) {
            return request.getHeader(conf.getNetHttpApiAuthIpHeaderKey());
        } else {
            return request.getRemoteAddr();
        }
    }

    public static String ipByteToString(byte[] ip) {
        if (ip == null)
            return "0.0.0.0";
        try {
            StringBuffer sb = new StringBuffer();
            sb.append(ip[0] & 0xff);
            sb.append(".");
            sb.append(ip[1] & 0xff);
            sb.append(".");
            sb.append(ip[2] & 0xff);
            sb.append(".");
            sb.append(ip[3] & 0xff);
            return sb.toString();
        } catch (Throwable e) {
            return "0.0.0.0";
        }
    }

    public static String stripFirstLastBracket(String org) {
        if (org.charAt(0) == BRACKET) {
            return org.substring(1, org.length() - 1);
        } else {
            return org;
        }
    }

    /**
     * split string by comma and strip array bracket if exists
     * eg) String : "[aaa, bbb]" -> List["aaa", "bbb"]
     * eg) String : "aaa, bbb" -> List["aaa", "bbb"]
     * @param org
     * @return
     */
    public static List<String> splitParam(String org) {
        org = stripFirstLastBracket(org);
        return Arrays.asList(StringUtils.split(org, COMMA));
    }
}
