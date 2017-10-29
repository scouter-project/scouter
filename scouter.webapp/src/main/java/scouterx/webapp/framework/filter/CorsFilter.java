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
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CorsFilter implements Filter {
    ConfigureAdaptor conf;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        //TODO why not added header?
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        if (StringUtils.isNotBlank(conf.getNetHttpApiCorsAllowOrigin())) {
            httpServletResponse.addHeader("Access-Control-Allow-Origin", conf.getNetHttpApiCorsAllowOrigin());
            httpServletResponse.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
            httpServletResponse.addHeader("Access-Control-Allow-Credentials", conf.getNetHttpApiCorsAllowCredentials());
        }

        chain.doFilter(request, response);

//        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
//        if (StringUtils.isNotBlank(conf.getNetHttpApiCorsAllowOrigin())) {
//            if (StringUtils.isBlank(httpServletResponse.getHeader("Access-Control-Allow-Origin"))) {
//                httpServletResponse.addHeader("Access-Control-Allow-Origin", conf.getNetHttpApiCorsAllowOrigin());
//                httpServletResponse.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
//                httpServletResponse.addHeader("Access-Control-Allow-Credentials", conf.getNetHttpApiCorsAllowCredentials());
//            }
//        }
    }

    @Override
    public void destroy() {
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        conf = ConfigureManager.getConfigure();
    }
}