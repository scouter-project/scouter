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

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2018. 2. 24.
 */
public class UserTokenTest {

    @Test
    public void toStoreValue_test() {
        UserToken token = makeFixture1();
        String toStoreValue = token.toStoreValue();
        UserToken unmarshalled = UserToken.fromStoreValue(toStoreValue, token.getServerId());

        assertEquals(unmarshalled.getFootprintSec(), token.getFootprintSec());
        assertEquals(unmarshalled.getUserId(), token.getUserId());
        assertEquals(unmarshalled.getToken(), token.getToken());
    }

    @Test
    public void toBearerToken_test() {
        UserToken token = makeFixture1();
        String toBearerToken = token.toBearerToken();
        UserToken unmarshalled = UserToken.fromBearerToken(toBearerToken);

        assertTrue(token.getFootprintSec() > 0);
        assertTrue(unmarshalled.getFootprintSec() == 0);
        assertEquals(unmarshalled.getUserId(), token.getUserId());
        assertEquals(unmarshalled.getToken(), token.getToken());
    }

    private static UserToken makeFixture1() {
        return UserToken.newToken("testId", "test-token", 1000);
    }
}