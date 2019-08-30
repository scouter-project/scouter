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

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 * <p>
 * It use scouter kv store in which data only can be added not delete or modify.
 * (We will make it better in a latter version.)
 */
public class UserTokenService {
    private final static String SESSION_STORE = "__WEB_SESSION__";

    private ConfigureAdaptor conf = ConfigureManager.getConfigure();
    private final static float TOUCH_RATE = 0.1f;
    public int sessionExpireSec = (int) (conf.getNetHttpApiSessionTimeout() * (1.0f + TOUCH_RATE));
    public int SessionTouchThresholdSec = (int) (conf.getNetHttpApiSessionTimeout() * TOUCH_RATE);

    private SessionIdGenerator sessionIdGenerator = new SessionIdGenerator();
    private CustomKvStoreService customKvStoreService = new CustomKvStoreService();

    /**
     * publish new token
     */
    public String publishToken(final Server server, final SUser user) {
        UserToken userToken = UserToken.newToken(user.getId(), sessionIdGenerator.generateSessionId(), server.getId());
        UserTokenCache.getInstance().put(userToken);

        String mergedStoreValue = getAndMergeToStoredValue(userToken);
        customKvStoreService.set(SESSION_STORE, userToken.getUserId(), mergedStoreValue, sessionExpireSec, server);

        return userToken.toBearerToken();
    }

    /**
     * check user session & renew token's footprint if valid
     */
    public void validateToken(UserToken token) {
        UserToken tokenTrusted = UserTokenCache.getInstance().get(token);
        if (tokenTrusted == null) {
            tokenTrusted = getStoredMatchedToken(token);
            if (tokenTrusted != null) {
                UserTokenCache.getInstance().put(tokenTrusted);
            }
        }
        if (tokenTrusted == null || tokenTrusted.isExpired(sessionExpireSec)) {
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
        UserToken renewedToken = token.renew();

        UserTokenCache.getInstance().putAsRecent(renewedToken);
        String mergedStoreValue = getAndMergeToStoredValue(renewedToken);

        customKvStoreService.set(SESSION_STORE, renewedToken.getUserId(), mergedStoreValue, sessionExpireSec, ServerManager.getInstance().getServer(renewedToken.getServerId()));
    }

    private UserToken getStoredMatchedToken(UserToken userToken) {
        String tokens = customKvStoreService.get(SESSION_STORE, userToken.getUserId(), ServerManager.getInstance().getServerIfNullDefault(userToken.getServerId()));
        Map<String, UserToken> userTokenMap = Arrays.stream(tokens.split(":"))
                .map(v -> UserToken.fromStoreValue(v, 0))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(UserToken::getToken, Function.identity()));

        return userTokenMap.get(userToken.getToken());
    }

    String getAndMergeToStoredValue(UserToken userToken) {
        String tokens = customKvStoreService.get(SESSION_STORE, userToken.getUserId(), ServerManager.getInstance().getServerIfNullDefault(userToken.getServerId()));
        return mergeStoredTokensWith(tokens, userToken);
    }

    String mergeStoredTokensWith(String tokens, UserToken userToken) {
        if (StringUtils.isBlank(tokens)) {
            return userToken.toStoreValue();
        }
        Map<String, UserToken> userTokenMap = Arrays.stream(tokens.split(":"))
                .map(v -> UserToken.fromStoreValue(v, 0))
                .filter(v -> v.isNotExpired(sessionExpireSec))
                .collect(Collectors.toMap(UserToken::getToken, Function.identity()));

        userTokenMap.put(userToken.getToken(), userToken);
        return userTokenMap.values().stream()
                .map(UserToken::toStoreValue)
                .collect(Collectors.joining(":"));
    }
}
