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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import scouterx.lib3.tomcat.SessionIdGenerator;
import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.framework.client.server.ServerManager;
import scouterx.webapp.framework.session.UserToken;
import scouterx.webapp.model.scouter.SUser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2018. 3. 11.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ServerManager.class})
public class UserTokenServiceTest {
    String vutUserId = "junit-user";

    @Mock
    CustomKvStoreService customKvStoreService;
    @Mock
    Server server;
    @Mock
    ServerManager serverManager;

    SessionIdGenerator sessionIdGenerator = new SessionIdGenerator();

    @InjectMocks
    UserTokenService sut = new UserTokenService();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mockStatic(ServerManager.class);
        when(ServerManager.getInstance()).thenReturn(serverManager);
    }

    @Test
    public void publishToken() {
        String bearer = sut.publishToken(server, new SUser(vutUserId));
        UserToken fromBearer = UserToken.fromBearerToken(bearer);
        assertEquals(vutUserId, fromBearer.getUserId());
    }

    @Test
    public void validateToken() {
        String bearer = sut.publishToken(server, new SUser(vutUserId));
        UserToken fromBearer = UserToken.fromBearerToken(bearer);

        sut.validateToken(fromBearer);
    }

    @Test
    public void getAndMergeToStoredValue() {
        UserToken userToken = UserToken.newToken(vutUserId, sessionIdGenerator.generateSessionId(), server.getId());

        UserToken token0 = UserToken.newToken(vutUserId, sessionIdGenerator.generateSessionId(), server.getId());
        UserToken token1 = UserToken.newToken(vutUserId, sessionIdGenerator.generateSessionId(), server.getId());
        UserToken token2 = UserToken.newToken(vutUserId, sessionIdGenerator.generateSessionId(), server.getId());

        String tokens = token0.toStoreValue() + ":" + token1.toStoreValue() + ":" + token2.toStoreValue();
        String merged = sut.mergeStoredTokensWith(tokens, userToken);

        assertTrue(merged.contains(userToken.getToken()));
    }

    @Test
    public void getAndMergeToStoredValue_with_expired_tokens() {
        UserToken userToken = UserToken.newToken(vutUserId, sessionIdGenerator.generateSessionId(), server.getId());

        UserToken token0 = UserToken.newToken(vutUserId, sessionIdGenerator.generateSessionId(), server.getId());
        UserToken token1 = UserToken.newToken(vutUserId, sessionIdGenerator.generateSessionId(), server.getId());
        UserToken token2 = UserToken.newToken(vutUserId, sessionIdGenerator.generateSessionId(), server.getId());

        token0.setFootprintSec(System.currentTimeMillis()/1000L - sut.sessionExpireSec - 1);
        token1.setFootprintSec(System.currentTimeMillis()/1000L - sut.sessionExpireSec - 1);
        token2.setFootprintSec(System.currentTimeMillis()/1000L - sut.sessionExpireSec - 1);

        String tokens = token0.toStoreValue() + ":" + token1.toStoreValue() + ":" + token2.toStoreValue();
        String merged = sut.mergeStoredTokensWith(tokens, userToken);

        assertEquals(merged, userToken.toStoreValue());
    }
}