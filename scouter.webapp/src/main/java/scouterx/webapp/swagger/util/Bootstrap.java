package scouterx.webapp.swagger.util;

import io.swagger.jaxrs.config.BeanConfig;
import scouterx.webapp.framework.configure.ConfigureAdaptor;
import scouterx.webapp.framework.configure.ConfigureManager;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Bootstrap extends HttpServlet {
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        String serverIp = null;

        try {
            serverIp = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            serverIp = "127.0.0.1";
        }

        ConfigureAdaptor conf = ConfigureManager.getConfigure();

        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setVersion("1.0.2");
        beanConfig.setSchemes(new String[]{"http"});
        beanConfig.setDescription("Scouter WEB HTTP API Document");
        beanConfig.setTitle("HTTP API Howto");
        beanConfig.setHost(serverIp + ":" + String.valueOf(conf.getNetHttpPort()));
        beanConfig.setBasePath("/scouter");
        beanConfig.setResourcePackage("scouterx.webapp");
        beanConfig.setFilterClass("scouterx.webapp.swagger.util.AllAuthorizationFilterImpl");
        beanConfig.setScan(true);
    }
}