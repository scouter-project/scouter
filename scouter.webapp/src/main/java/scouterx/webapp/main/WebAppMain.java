package scouterx.webapp.main;

import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;

import java.util.TimeZone;

/**
 * Created by gunlee on 2017. 8. 25.
 */
public class WebAppMain {
	public static void main(String[] args) {
		Server server = new Server(6188);

		HandlerCollection handlers = new HandlerCollection();
		NCSARequestLog requestLog = new NCSARequestLog();
		requestLog.setFilename("./logs/http-request-yyyy_mm_dd.log");
		requestLog.setFilenameDateFormat("yyyy_MM_dd");
		requestLog.setRetainDays(10);
		requestLog.setAppend(true);
		requestLog.setExtended(true);
		requestLog.setLogCookies(false);
		requestLog.setLogTimeZone(TimeZone.getDefault().getID());
		RequestLogHandler requestLogHandler = new RequestLogHandler();
		requestLogHandler.setRequestLog(requestLog);

		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		handlers.addHandler(requestLogHandler);
		handlers.addHandler(context);

		server.setHandler(handlers);

		ServletHolder jerseyHolder = new ServletHolder(ServletContainer.class);
		jerseyHolder.setInitParameter("jersey.config.server.provider.packages", "scouterx.webapp.api");
		context.addServlet(jerseyHolder, "/rest/*");
		try {
			server.start();
			server.join();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

//	Configure conf = Configure.getInstance();
//        if (conf.net_http_server_enabled) {
//		Server server = new Server(conf.net_http_port);
//
//		HandlerCollection handlers = new HandlerCollection();
//		NCSARequestLog requestLog = new NCSARequestLog();
//		requestLog.setFilename(conf.log_dir + "/http-request-yyyy_mm_dd.log");
//		requestLog.setFilenameDateFormat("yyyy_MM_dd");
//		requestLog.setRetainDays(conf.log_keep_days);
//		requestLog.setAppend(true);
//		requestLog.setExtended(true);
//		requestLog.setLogCookies(false);
//		requestLog.setLogTimeZone(TimeZone.getDefault().getID());
//		RequestLogHandler requestLogHandler = new RequestLogHandler();
//		requestLogHandler.setRequestLog(requestLog);
//
//		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
//		context.setContextPath("/");
//		context.addServlet(new ServletHolder(CounterServlet.class), "/counter/*");
//		context.addServlet(new ServletHolder(RegisterServlet.class), "/register/*");
//
//		handlers.addHandler(requestLogHandler);
//		handlers.addHandler(context);
//
//		server.setHandler(handlers);
//
//		if (conf.net_http_api_enabled) {
//			ServletHolder jerseyHolder = new ServletHolder(ServletContainer.class);
//			jerseyHolder.setInitParameter("jersey.config.server.provider.packages", "scouter.server.http.api");
//			context.addServlet(jerseyHolder, "/rest/*");
//		}
//
//		try {
//			server.start();
//			server.join();
//		} catch (Exception e) {
//			scouter.server.Logger.println("HTTP", 10, "Failed to start http server", e);
//		}
//	}
}
