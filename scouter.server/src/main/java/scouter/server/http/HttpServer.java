/*
*  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 *
 */
package scouter.server.http;

import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;
import scouter.server.Configure;
import scouter.server.http.servlet.CounterServlet;
import scouter.server.http.servlet.RegisterServlet;
import scouter.util.ThreadUtil;

import java.util.TimeZone;

public class HttpServer extends Thread {

    private static volatile HttpServer instance;

    public static HttpServer load() {
        if (instance == null) {
            synchronized (HttpServer.class) {
                if (instance == null) {
                    instance = new HttpServer();
                    instance.setName(ThreadUtil.getName(HttpServer.class));
                    instance.setDaemon(true);
                    instance.start();
                }
            }
        }
        return instance;
    }


    @Override
    public void run() {
        Configure conf = Configure.getInstance();
        if (conf.net_http_server_enabled) {
            Server server = new Server(conf.net_http_port);

            HandlerCollection handlers = new HandlerCollection();
            NCSARequestLog requestLog = new NCSARequestLog();
            requestLog.setFilename(conf.log_dir + "/http-request-yyyy_mm_dd.log");
            requestLog.setFilenameDateFormat("yyyy_MM_dd");
            requestLog.setRetainDays(conf.log_keep_days);
            requestLog.setAppend(true);
            requestLog.setExtended(true);
            requestLog.setLogCookies(false);
            requestLog.setLogTimeZone(TimeZone.getDefault().getID());
            RequestLogHandler requestLogHandler = new RequestLogHandler();
            requestLogHandler.setRequestLog(requestLog);

            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");
            context.addServlet(new ServletHolder(CounterServlet.class), "/counter/*");
            context.addServlet(new ServletHolder(RegisterServlet.class), "/register/*");

            handlers.addHandler(requestLogHandler);
            handlers.addHandler(context);

            server.setHandler(handlers);

            if (conf.net_http_api_enabled) {
                ServletHolder jerseyHolder = new ServletHolder(ServletContainer.class);
                jerseyHolder.setInitParameter("jersey.config.server.provider.packages", "scouter.server.http.api");
                context.addServlet(jerseyHolder, "/rest/*");
            }

            try {
                server.start();
                server.join();
            } catch (Exception e) {
                scouter.server.Logger.println("HTTP", 10, "Failed to start http server", e);
            }
        }
    }

}
