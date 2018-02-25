package scouterx.webapp.swagger;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.config.SwaggerContextService;
import io.swagger.models.Swagger;
import io.swagger.models.auth.ApiKeyAuthDefinition;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import scouterx.webapp.framework.configure.ConfigureAdaptor;
import scouterx.webapp.framework.configure.ConfigureManager;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

/**
 * @author leekyoungil (leekyoungil@gmail.com) on 2017. 10. 24.
 */
@Slf4j
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

        BeanConfig beanConfig = new BeanConfig();

        String serverIp = scouterConf.getNetHttpApiSwaggerHostIp();
        if (StringUtils.isNotBlank(serverIp)) {
            beanConfig.setHost(serverIp + ":" + String.valueOf(scouterConf.getNetHttpPort()));
        }

        beanConfig.setVersion(this.apiVersion);
        beanConfig.setSchemes(new String[]{"http", "https"});
        beanConfig.setDescription("<a href='https://github.com/scouter-project/scouter/blob/master/scouter.document/tech/Web-API-Guide.md' target='_blank'> [Scouter document page] Scouter Web API Guide</a>");
        beanConfig.setTitle("Scouter HTTP APIs");
        beanConfig.setBasePath("/scouter");
        beanConfig.setResourcePackage("scouterx.webapp");
        beanConfig.setFilterClass(this.filterClass);
        beanConfig.setScan(true);

        Swagger swagger = new Swagger();
        swagger.securityDefinition("Scouter auth token", new ApiKeyAuthDefinition("Authorization", In.HEADER));
        new SwaggerContextService().withServletConfig(config).updateSwagger(swagger);

    }
}