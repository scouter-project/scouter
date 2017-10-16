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

import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.layer.consumer.AccountConsumer;
import scouterx.webapp.framework.exception.ErrorState;
import scouterx.webapp.model.scouter.SUser;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 */
public class UserService {
    private final AccountConsumer accountConsumer;

    public UserService() {
        this.accountConsumer = new AccountConsumer();
    }

    public void login(final Server server, final SUser user) {
        boolean result = accountConsumer.login(server, user);
        if (!result) {
            throw ErrorState.LOGIN_FAIL.newBizException();
        }
    }
}
