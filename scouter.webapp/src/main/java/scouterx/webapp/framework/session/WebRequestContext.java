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

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2018. 2. 24.
 */
@Getter
@Setter
public class WebRequestContext {
    private static ThreadLocal<UserToken> _userToken = new ThreadLocal<>();

    public static void setUserToken(UserToken userToken) {
        _userToken.set(userToken);
    }

    public static UserToken getUserToken() {
        return _userToken.get();
    }

    public static void clearUserToken() {
        _userToken.set(null);
    }

    public static void clearAll() {
        _userToken.set(null);
    }
}
