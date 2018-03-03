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

import org.apache.commons.lang3.StringUtils;
import scouterx.webapp.framework.configure.ConfigureAdaptor;
import scouterx.webapp.framework.configure.ConfigureManager;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;
import java.io.IOException;

public class CorsFilter implements Filter {
    ConfigureAdaptor conf;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        String allowOrigin = conf.getNetHttpApiCorsAllowOrigin();
        String allowCredentials = conf.getNetHttpApiCorsAllowCredentials();

        if (StringUtils.isNotBlank(allowOrigin)) {
            if ("true".equals(allowCredentials) && "*".equals(allowOrigin)) {
                String hostHeader = httpServletRequest.getHeader("origin");
                if (StringUtils.isNotBlank(hostHeader)) {
                    allowOrigin = hostHeader;
                }
            }
            httpServletResponse.addHeader("Access-Control-Allow-Origin", allowOrigin);
            httpServletResponse.addHeader("Access-Control-Allow-Credentials", allowCredentials);
            httpServletResponse.addHeader("Access-Control-Max-Age", "600");

            httpServletResponse.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
            httpServletResponse.addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE, PATCH");

            if (httpServletRequest.getMethod().equals(HttpMethod.OPTIONS)) {
                return;
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        conf = ConfigureManager.getConfigure();
    }
}
