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

package scouterx.webapp.framework.filter;

import lombok.extern.slf4j.Slf4j;
import scouterx.webapp.framework.exception.ErrorState;
import scouterx.webapp.framework.configure.ConfigureAdaptor;
import scouterx.webapp.framework.configure.ConfigureManager;
import scouterx.webapp.framework.util.ZZ;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import java.io.IOException;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 */
@Slf4j
public class AuthFilter implements ContainerRequestFilter {
    @Context
    private HttpServletRequest servletRequest;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        ConfigureAdaptor conf = ConfigureManager.getConfigure();

        //Check IP
        if (conf.isNetHttpApiAuthIpEnabled()) {
            if (conf.getNetHttpApiAllowIps().stream().anyMatch(ip -> ZZ.getRequestIp(servletRequest).contains(ip))) {
                return;
            }
        }

        //Check session
        if (conf.isNetHttpApiAuthSessionEnabled()) {
            HttpSession session = servletRequest.getSession();
            if(session == null || session.getAttribute("user") == null) {
                throw ErrorState.LOGIN_REQUIRED.newBizException();
            }
        }
    }
}
