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

import org.slf4j.MDC;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 9. 6.
 */
public class LoggingInitServletFilter implements Filter {
    private static final String RID = Integer.toString(new Random(System.currentTimeMillis()).nextInt(30000) + 2000, 32);
    private static final String INSTANCE_ID_KEY = "scouter.instanceId";
    private static final String INSTANCE_ID = System.getProperty(INSTANCE_ID_KEY, "#");
    private static final String LOG_TRACE_HEADER = INSTANCE_ID + RID;

    public static final String requestId = "requestId";

    private static AtomicInteger counter = new AtomicInteger();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        initLogging(request);
        try {
            chain.doFilter(request, response);
        } finally {
            clear();
        }
    }

    void initLogging(ServletRequest request) {
        int requestId = counter.getAndIncrement();
        String logTraceId = LOG_TRACE_HEADER + requestId;
        MDC.put(LoggingInitServletFilter.requestId, logTraceId);
    }

    void clear() {
        MDC.remove(requestId);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException { }
    @Override
    public void destroy() { }
}
