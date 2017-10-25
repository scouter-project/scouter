package scouterx.webapp.swagger;

import io.swagger.jaxrs.config.BeanConfig;
import scouterx.webapp.framework.configure.ConfigureAdaptor;
import scouterx.webapp.framework.configure.ConfigureManager;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.net.InetAddress;
import java.net.UnknownHostException;

import static scouterx.webapp.main.WebAppMain.IS_USE_SWAGGER;

public class Bootstrap extends HttpServlet {

    private final String apiVersion = "1.0.0";
    private final String filterClass = "scouterx.webapp.swagger.AllAuthorizationFilterImpl";

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        if (!IS_USE_SWAGGER) {
            return;
        }

        String serverIp = null;

        try {
            serverIp = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            serverIp = "127.0.0.1";
        }

        ConfigureAdaptor conf = ConfigureManager.getConfigure();

        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setVersion(this.apiVersion);
        beanConfig.setSchemes(new String[]{"http", "https"});
        beanConfig.setDescription("Scouter WEB HTTP API Document");
        beanConfig.setTitle("HTTP API Howto");
        beanConfig.setHost(serverIp + ":" + String.valueOf(conf.getNetHttpPort()));
        beanConfig.setBasePath("/scouter");
        beanConfig.setResourcePackage("scouterx.webapp");
        beanConfig.setFilterClass(this.filterClass);
        beanConfig.setScan(true);
    }
}