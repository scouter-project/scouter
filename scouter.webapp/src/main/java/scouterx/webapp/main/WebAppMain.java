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

package scouterx.webapp.main;

import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scouter.util.SysJMX;
import scouter.util.ThreadUtil;
import scouter.util.logo.Logo;
import scouterx.webapp.framework.client.model.AgentModelThread;
import scouterx.webapp.framework.client.net.LoginMgr;
import scouterx.webapp.framework.client.net.LoginRequest;
import scouterx.webapp.framework.client.server.ServerManager;
import scouterx.webapp.framework.client.thread.ServerSessionObserver;
import scouterx.webapp.framework.configure.ConfigureAdaptor;
import scouterx.webapp.framework.configure.ConfigureManager;
import scouterx.webapp.framework.configure.ServerConfig;
import scouterx.webapp.framework.filter.CorsFilter;
import scouterx.webapp.framework.filter.LoggingInitServletFilter;
import scouterx.webapp.framework.filter.NoCacheFilter;
import scouterx.webapp.framework.filter.ReleaseResourceFilter;
import scouterx.webapp.layer.websock.BasicSocket;
import scouterx.webapp.swagger.Bootstrap;

import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import javax.ws.rs.core.Application;
import java.io.File;
import java.util.EnumSet;
import java.util.List;
import java.util.TimeZone;

//TODO shutdown hook, shutdown serverManager

/**
 * Created by gunlee on 2017. 8. 25.
 */
public class WebAppMain extends Application {
    private static boolean standAloneMode = false;

    /**
     * if you want use the Swagger.
     * add "-DisUseSwagger=true" your VM options When bootrun.
     * Not recommend using on **Production** environment.
     */
    public static boolean isStandAloneMode() {
        return standAloneMode;
    }

    public static void main(String[] args) throws Exception {
        WebAppMain.standAloneMode = true;
        Logo.print(true);

        initializeLogDir();

        final ConfigureAdaptor conf = ConfigureManager.getConfigure();
        connectScouterCollector();

        final Server server = new Server(conf.getNetHttpPort());

        HandlerCollection handlers = new HandlerCollection();

        RequestLogHandler requestLogHandler = setRequestLogHandler();
        handlers.addHandler(requestLogHandler);

        ServletContextHandler servletContextHandler = setWebAppContext();

        if (conf.isNetHttpApiGzipEnabled()) {
            GzipHandler gzipHandler = new GzipHandler();
            gzipHandler.setIncludedMethods("GET", "POST", "PUT", "DELETE");
            gzipHandler.setMinGzipSize(1024);
            gzipHandler.setHandler(servletContextHandler);
            handlers.addHandler(gzipHandler);
        } else {
            handlers.addHandler(servletContextHandler);
        }

        server.setHandler(handlers);
        setWebSocketServer(servletContextHandler);

        try {
            server.start();
            waitOnExit(server);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setWebSocketServer(ServletContextHandler servletContextHandler) throws ServletException, DeploymentException {
        // Add javax.websocket support
        ServerContainer container = WebSocketServerContainerInitializer.configureContext(servletContextHandler);
        container.setDefaultMaxSessionIdleTimeout(7*24*3600*1000);
        // Add echo endpoint to server container
        container.addEndpoint(BasicSocket.class);
    }

    private static ServletContextHandler setWebHttpApiHandler () {
        ConfigureAdaptor conf = ConfigureManager.getConfigure();

        String providerPackages = "scouterx.webapp";
        if (conf.isNetHttpApiSwaggerEnabled()) {
            providerPackages += ",io.swagger.jaxrs.listing";
        }

        final ServletHolder jerseyHolder = new ServletHolder(ServletContainer.class);
        jerseyHolder.setInitParameter("javax.ws.rs.Application", "scouterx.webapp.main.WebAppMain");
        jerseyHolder.setInitParameter("jersey.config.server.provider.packages", providerPackages);
        jerseyHolder.setInitOrder(1);

        final ServletContextHandler servletContextHandler = new ServletContextHandler();
        servletContextHandler.setSessionHandler(new SessionHandler());
        servletContextHandler.getSessionHandler().setMaxInactiveInterval(conf.getNetHttpApiSessionTimeout());
        servletContextHandler.setContextPath("/");

        servletContextHandler.addServlet(jerseyHolder, "/scouter/*");
        servletContextHandler.addServlet(setStaticContentHandler(), "/*");
        servletContextHandler.addServlet(setExtWebStaticContentHandler(), "/extweb/*");
        servletContextHandler.addServlet(setSwaggerBootstrapHandler(), "/swagger");

        addFilter(servletContextHandler);

        return servletContextHandler;
    }

    private static void addFilter (ServletContextHandler servletContextHandler) {
        servletContextHandler.addFilter(LoggingInitServletFilter.class, "/scouter/*", EnumSet.of(DispatcherType.REQUEST));
        servletContextHandler.addFilter(CorsFilter.class, "/scouter/*", EnumSet.of(DispatcherType.REQUEST));
        servletContextHandler.addFilter(NoCacheFilter.class, "/scouter/*", EnumSet.of(DispatcherType.REQUEST));
        servletContextHandler.addFilter(ReleaseResourceFilter.class, "/scouter/*", EnumSet.of(DispatcherType.REQUEST));
    }

    private static ServletHolder setSwaggerBootstrapHandler () {
        ServletHolder swaggerBootstrap = new ServletHolder(Bootstrap.class);
        swaggerBootstrap.setInitOrder(2);

        return swaggerBootstrap;
    }

    private static ServletHolder setStaticContentHandler () {
        String resourceBase = WebAppMain.class.getClassLoader().getResource("webroot/").toExternalForm();
        ServletHolder holderHome = new ServletHolder(DefaultServlet.class);
        holderHome.setInitParameter("resourceBase", resourceBase);
        holderHome.setInitParameter("dirAllowed","false");
        holderHome.setInitParameter("pathInfoOnly","true");

        return holderHome;
    }

    private static ServletHolder setExtWebStaticContentHandler () {
        String resourceBase = ConfigureManager.getConfigure().getNetHttpExtWebDir();
        ServletHolder holderHome = new ServletHolder(DefaultServlet.class);
        holderHome.setInitParameter("resourceBase", resourceBase);
        holderHome.setInitParameter("dirAllowed","true");
        holderHome.setInitParameter("pathInfoOnly","true");

        return holderHome;
    }

	private static RequestLogHandler setRequestLogHandler() {
		ConfigureAdaptor conf = ConfigureManager.getConfigure();

		NCSARequestLog requestLog = new NCSARequestLog();
		requestLog.setFilename("./logs/http-request-yyyy_mm_dd.log");
		requestLog.setFilenameDateFormat("yyyy_MM_dd");
		requestLog.setRetainDays(conf.getLogKeepDays());
		requestLog.setAppend(true);
		requestLog.setExtended(true);
		requestLog.setLogCookies(false);
		requestLog.setLogTimeZone(TimeZone.getDefault().getID());
		RequestLogHandler requestLogHandler = new RequestLogHandler();
		requestLogHandler.setRequestLog(requestLog);

		return requestLogHandler;
	}

    private static void connectScouterCollector() {
        Logger log = LoggerFactory.getLogger(WebAppMain.class);

        List<ServerConfig> serverConfigList = ConfigureManager.getConfigure().getServerConfigs();
        ServerManager srvMgr = ServerManager.getInstance();
        for (ServerConfig serverConfig : serverConfigList) {
            scouterx.webapp.framework.client.server.Server server = new scouterx.webapp.framework.client.server.Server(serverConfig.getIp(), serverConfig.getPort());
            if (srvMgr.getServer(server.getId()) == null) {
                srvMgr.addServer(server);
            } else {
                server = srvMgr.getServer(server.getId());
            }

            server.setUserId(serverConfig.getId());
            server.setPassword(serverConfig.getPassword());

            LoginRequest result = LoginMgr.login(server);
            if (result.success) {
                log.info("Successfully log in to {}:{}", server.getIp(), server.getPort());
                AgentModelThread.getInstance(); //preloading
            } else {
                server.setUserId(serverConfig.getId());
                server.setPassword(serverConfig.getPassword());
                log.error("Fail to log in to {}:{}", server.getIp(), server.getPort());
            }
        }
        ServerSessionObserver.load();
    }

    private static void waitOnExit(Server server) throws Exception {
        File exit = new File(SysJMX.getProcessPID() + ".scouter");
        try {
            exit.createNewFile();
        } catch (Exception e) {
            String tmp = System.getProperty("user.home", "/tmp");
            exit = new File(tmp, SysJMX.getProcessPID() + ".scouter.run");
            try {
                exit.createNewFile();
            } catch (Exception k) {
                System.exit(1);
            }
        }
        exit.deleteOnExit();
        System.out.println("System JRE version : " + System.getProperty("java.version"));
        while (true) {
            if (exit.exists() == false) {
                server.setStopTimeout(3000);
                server.stop();
                System.exit(0);
            }
            ThreadUtil.sleep(1000);
        }
    }

    private static void initializeLogDir() {
        ConfigureAdaptor conf = ConfigureManager.getConfigure();

        File logDir = new File(conf.getLogDir());
        if (!logDir.exists()) {
            logDir.mkdirs();
        }

        System.setProperty("scouter_webapp_log_dir", conf.getLogDir());

        Logger firstLogger = LoggerFactory.getLogger(WebAppMain.class);
        firstLogger.info("scouter webapp starting! Run-Mode:" + (WebAppMain.standAloneMode ? "StandAlone" : "Embedded"));
    }

    /**
     * Initialize context needed by scouter web http api application.
     * (This method also can be invoked from scouter.server's Http Server when this webapp runs as an embedded mode.)
     *
     */
    public static ServletContextHandler setWebAppContext() throws ServletException, DeploymentException {
        //The case - embedded mode (run in-process of scouter server)
        if (!standAloneMode) {
            initializeLogDir();
            connectScouterCollector();
        }
        ServletContextHandler handler = setWebHttpApiHandler();

        return handler;
    }
}
