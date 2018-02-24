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

package scouterx.webapp.layer.service;

import org.apache.commons.lang3.StringUtils;
import scouterx.lib3.tomcat.SessionIdGenerator;
import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.framework.client.server.ServerManager;
import scouterx.webapp.framework.configure.ConfigureAdaptor;
import scouterx.webapp.framework.configure.ConfigureManager;
import scouterx.webapp.framework.exception.ErrorState;
import scouterx.webapp.framework.session.UserToken;
import scouterx.webapp.framework.session.UserTokenCache;
import scouterx.webapp.model.scouter.SUser;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 *
 * It use scouter kv store in which data only can be added not delete or modify.
 * (We will make it better in a latter version.)
 */
public class UserTokenService {
    private final static String SESSION_STORE = "__WEB_SESSION__";

    private ConfigureAdaptor conf = ConfigureManager.getConfigure();
    private final static float TOUCH_RATE = 0.1f;
    private int sessionExpireSec = (int) (conf.getNetHttpApiSessionTimeout() * (1.0f + TOUCH_RATE));
    private int SessionTouchThresholdSec = (int) (conf.getNetHttpApiSessionTimeout() * TOUCH_RATE);

    private SessionIdGenerator sessionIdGenerator = new SessionIdGenerator();
    private CustomKvStoreService customKvStoreService = new CustomKvStoreService();

    /**
     * publish new token
     */
    public String publishToken(final Server server, final SUser user) {
        UserToken userToken = UserToken.newToken(user.getId(), sessionIdGenerator.generateSessionId(), server.getId());
        UserTokenCache.getInstance().put(user.getId(), userToken);
        customKvStoreService.set(SESSION_STORE, userToken.getStoreKey(), userToken.toStoreValue(), server);
        return userToken.toBearerToken();
    }

    /**
     * check user session & renew token's footprint if valid
     */
    public void validateToken(UserToken token) {
        UserToken tokenTrusted = UserTokenCache.getInstance().get(token.getId());
        if (tokenTrusted == null) {
            String stored = customKvStoreService.get(SESSION_STORE, token.getStoreKey(), ServerManager.getInstance().getServerIfNullDefault(token.getServerId()));
            if (StringUtils.isNotBlank(stored)) {
                tokenTrusted = UserToken.fromStoreValue(stored, token.getServerId());
                if (tokenTrusted != null) {
                    UserTokenCache.getInstance().put(tokenTrusted.getId(), tokenTrusted);
                }
            }
        }
        if (tokenTrusted == null || !tokenTrusted.getToken().equals(token.getToken()) || tokenTrusted.isExpired(sessionExpireSec)) {
            throw ErrorState.SESSION_EXPIRED.newBizException();
        }
        if (tokenTrusted.needToBeRenewed(SessionTouchThresholdSec)) {
            touchToken(token);
        }
    }

    /**
     * renew token's footprint
     */
    private void touchToken(UserToken token) {
        UserToken userToken = token.renew();
        UserTokenCache.getInstance().put(userToken.getId(), userToken);
        customKvStoreService.set(SESSION_STORE, userToken.getStoreKey(), userToken.toStoreValue(),
                ServerManager.getInstance().getServer(token.getServerId()));
    }
}
