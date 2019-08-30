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
import scouter.util.StringUtil;
import scouterx.webapp.framework.configure.ConfigureAdaptor;
import scouterx.webapp.framework.configure.ConfigureManager;
import scouterx.webapp.framework.exception.ErrorState;
import scouterx.webapp.framework.session.UserToken;
import scouterx.webapp.framework.session.WebRequestContext;
import scouterx.webapp.framework.util.ZZ;
import scouterx.webapp.layer.service.UserTokenService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 */
@Slf4j
public class AuthFilter implements ContainerRequestFilter {
    private static final String BEARER_PREFIX = "bearer ";

    @Context
    private HttpServletRequest servletRequest;

    UserTokenService userTokenService = new UserTokenService();

    @Override
    public void filter(ContainerRequestContext requestContext) {
        WebRequestContext.clearUserToken();
        ConfigureAdaptor conf = ConfigureManager.getConfigure();

        boolean violation = false;
        //Check IP
        if (conf.isNetHttpApiAuthIpEnabled()) {
            String ip = ZZ.getRequestIp(servletRequest);
            if (conf.getNetHttpApiAllowIpExact().contains(ip)) {
                return;
            }
            if (conf.getNetHttpApiAllowIpMatch().stream().anyMatch(match -> match.include(ip))) {
                return;
            }
            violation = true;
        }

        //Check token
        if (conf.isNetHttpApiAuthBearerTokenEnabled()) {
            String authHeader = servletRequest.getHeader("Authorization");
            if (StringUtil.isNotEmpty(authHeader)) {
                UserToken token = UserToken.fromBearerToken(trimToken(authHeader));
                userTokenService.validateToken(token);
                WebRequestContext.setUserToken(token);
                return;
            } else {
                if (!conf.isNetHttpApiAuthSessionEnabled()) {
                    throw ErrorState.SESSION_EXPIRED.newBizException();
                }
            }
        }

        //Check session
        if (conf.isNetHttpApiAuthSessionEnabled()) {
            HttpSession session = servletRequest.getSession();
            if(session == null || session.getAttribute("userId") == null) {
                throw ErrorState.LOGIN_REQUIRED.newBizException();
            }
            UserToken userToken = UserToken.fromSessionId((String) session.getAttribute("userId"));
            WebRequestContext.setUserToken(userToken);
        }

        if (violation) {
            throw ErrorState.SESSION_EXPIRED.newBizException();
        }
    }

    private String trimToken(String authHeader) {
        return StringUtil.limiting(authHeader, BEARER_PREFIX.length()).toLowerCase().equals(BEARER_PREFIX)
                ? authHeader.substring(7) : authHeader;
    }
}
