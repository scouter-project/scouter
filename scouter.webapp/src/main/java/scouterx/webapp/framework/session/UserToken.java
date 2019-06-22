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

package scouterx.webapp.framework.session;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import scouter.util.Hexa32;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2018. 2. 24.
 */
@Getter
@Setter
public class UserToken {
    String userId;
    String token;
    long footprintSec; //unix timestamp
    int serverId;
    boolean fromSession;

    private UserToken(String id, String token, int serverId) {
        this.userId = id;
        this.token = token;
        this.footprintSec = 0;
        this.serverId = serverId;
    }

    public String getStoreKey() {
        return userId;
    }

    public String toStoreValue() {
        return "V1." + footprintSec + "." + token + "." + userId;
    }

    public String toBearerToken() {
        return "V1." + token + "." + Hexa32.toString32(serverId) + "." + userId;
    }

    public boolean isExpired(int timeoutSec) {
        return footprintSec + timeoutSec < System.currentTimeMillis() / 1000;
    }

    public boolean isNotExpired(int timeoutSec) {
        return !isExpired(timeoutSec);
    }

    public boolean needToBeRenewed(int touchThresholdSec) {
        return footprintSec + touchThresholdSec < System.currentTimeMillis() / 1000;
    }

    public UserToken renew() {
        return UserToken.newToken(userId, token, serverId);
    }

    public static UserToken fromBearerToken(String token) {
        String arr[] = StringUtils.split(token, ".", 4);
        return new UserToken(arr[3], arr[1], (int) Hexa32.toLong32(arr[2]));
    }

    public static UserToken fromStoreValue(String token, int serverId) {
        String arr[] = StringUtils.split(token, ".", 4);
        if (arr.length < 4) {
            return null;
        }
        UserToken stored = new UserToken(arr[3], arr[2], serverId);
        stored.setFootprintSec(Long.parseLong(arr[1]));
        return stored;
    }

    public static UserToken newToken(String id, String token, int serverId) {
        UserToken newToken = new UserToken(id, token, serverId);
        newToken.setFootprintSec(System.currentTimeMillis() / 1000L);
        return newToken;
    }

    public static UserToken fromSessionId(String sessionId) {
        UserToken newToken = new UserToken(sessionId, null, 0);
        newToken.fromSession = true;
        newToken.setFootprintSec(System.currentTimeMillis() / 1000L);
        return newToken;
    }
}
