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
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import scouter.server.Configure;
import scouter.server.Logger;
import scouter.server.http.servlet.CounterServlet;
import scouter.server.http.servlet.RegisterServlet;
import scouter.server.http.servlet.TelegrafInputServlet;
import scouter.util.StringUtil;
import scouter.util.ThreadUtil;

import javax.servlet.http.HttpServletRequest;
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
        System.setProperty("scouter_webapp_log_dir", conf.log_dir); //set slf4j logging dir

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

            ServletContextHandler context = null;
            handlers.addHandler(requestLogHandler);

            if (conf.net_http_api_enabled) {
                try {
                    Class c = Class.forName("scouterx.webapp.main.WebAppMain");
                    Object result = c.getMethod("setWebAppContext").invoke(null);
                    context = (ServletContextHandler) result;
                } catch (Throwable e) {
                    Logger.println("Error while loading webapp api context!");
                    System.out.println("Error while loading webapp api context!");
                    Logger.printStackTrace(e);
                    e.printStackTrace();
                }
            }

            if (context == null) {
                context = new ServletContextHandler(ServletContextHandler.SESSIONS);
                context.setContextPath("/");
            }
            context.addServlet(new ServletHolder(CounterServlet.class), "/counter/*");
            context.addServlet(new ServletHolder(RegisterServlet.class), "/register/*");
            context.addServlet(new ServletHolder(TelegrafInputServlet.class), "/telegraf/*");

            if (conf.net_http_api_gzip_enabled) {
                GzipHandler gzipHandler = new GzipHandler();
                gzipHandler.setIncludedMethods("GET", "POST", "PUT", "DELETE");
                gzipHandler.setMinGzipSize(1024);
                gzipHandler.setHandler(context);
                handlers.addHandler(gzipHandler);
            } else {
                handlers.addHandler(context);
            }

            server.setHandler(handlers);

            if (conf.net_http_api_enabled) {
                try {
                    Class c = Class.forName("scouterx.webapp.main.WebAppMain");
                    c.getMethod("setWebSocketServer", ServletContextHandler.class).invoke(null, context);
                } catch (Throwable e) {
                    Logger.println("Error while setWebSocketServer!");
                    System.out.println("Error while setWebSocketServer!");
                    Logger.printStackTrace(e);
                    e.printStackTrace();
                }
            }

            try {
                server.start();
                server.join();
            } catch (Exception e) {
                scouter.server.Logger.println("HTTP", 10, "Failed to start http server", e);
            }
        }
    }

    public static String getRemoteAddr(HttpServletRequest request) {
        String ip = request.getHeader("X-FORWARDED-FOR");
        if (ip == null) {
            return request.getRemoteAddr();
        } else {
            String[] ips = StringUtil.split(ip, ',');
            if (ips.length > 0) {
                return ips[0];
            } else {
                return request.getRemoteAddr();
            }
        }
    }
}
