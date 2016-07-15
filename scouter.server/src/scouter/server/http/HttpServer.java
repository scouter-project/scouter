package scouter.server.http;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import scouter.server.Configure;
import scouter.server.http.servlet.CounterServlet;
import scouter.util.ThreadUtil;

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
			ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
			context.setContextPath("/");
	        server.setHandler(context);
	        context.addServlet(new ServletHolder(CounterServlet.class), "/counter/*");
	        try {
		        server.start();
		        server.join();
	        } catch (Exception e) {
	        	scouter.server.Logger.println("HTTP", 10, "Failed to start http server", e);
	        }
		}
	}

}
