package scouterx.webapp.swagger;

import io.swagger.jaxrs.config.BeanConfig;
import scouterx.webapp.framework.configure.ConfigureAdaptor;
import scouterx.webapp.framework.configure.ConfigureManager;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

/**
 * @author leekyoungil (leekyoungil@gmail.com) on 2017. 10. 24.
 */
public class Bootstrap extends HttpServlet {

    private final String apiVersion = "1.0.0";
    private final String filterClass = "scouterx.webapp.swagger.AllAuthorizationFilterImpl";

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        ConfigureAdaptor scouterConf = ConfigureManager.getConfigure();
        if (!scouterConf.isNetHttpApiSwaggerEnabled()) {
            return;
        }

        String serverIp = null;

        // @TODO : help @gunlee
        // if serverIp is not '127.0.0.1' always return 'login required'.
//        try {
//            serverIp = InetAddress.getLocalHost().getHostAddress();
//        } catch (UnknownHostException e) {
            serverIp = "127.0.0.1";
//        }

        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setVersion(this.apiVersion);
        beanConfig.setSchemes(new String[]{"http", "https"});
        beanConfig.setDescription("Scouter WEB HTTP API Document");
        beanConfig.setTitle("HTTP API Howto");
        beanConfig.setHost(serverIp + ":" + String.valueOf(scouterConf.getNetHttpPort()));
        beanConfig.setBasePath("/scouter");
        beanConfig.setResourcePackage("scouterx.webapp");
        beanConfig.setFilterClass(this.filterClass);
        beanConfig.setScan(true);
    }
}