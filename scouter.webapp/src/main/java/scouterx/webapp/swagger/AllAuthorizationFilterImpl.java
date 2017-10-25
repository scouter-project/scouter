package scouterx.webapp.swagger;

import io.swagger.core.filter.AbstractSpecFilter;
import io.swagger.model.ApiDescription;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.Property;

import java.util.List;
import java.util.Map;

/**
 * @author leekyoungil (leekyoungil@gmail.com) on 2017. 10. 24.
 */
public class AllAuthorizationFilterImpl extends AbstractSpecFilter {

        public boolean isOperationAllowed(Operation operation,
                ApiDescription api,
                Map<String, List<String>> params,
                Map<String, String> cookies,
                Map<String, List<String>> headers) {
            //if(!api.getMethod().equals("get") || api.getPath().startsWith("/scouter/ignore"))
            //    return checkKey(params, headers);
            return true;
        }

    public boolean isParamAllowed(Parameter parameter,
                                  Operation operation,
                                  ApiDescription api,
                                  Map<String, List<String>> params,
                                  Map<String, String> cookies,
                                  Map<String, List<String>> headers) {
        return true;
    }

    public boolean isPropertyAllowed(Model model,
                                     Property property,
                                     String propertyName,
                                     Map<String, List<String>> params,
                                     Map<String, String> cookies,
                                     Map<String, List<String>> headers) {
        return true;
    }

    public boolean checkKey(Map<String, List<String>> params,
                            Map<String, List<String>> headers) {
//        String keyValue = null;
//        if(params.containsKey("api_key"))
//            keyValue = params.get("api_key").get(0);
//        else {
//            if(headers.containsKey("api_key"))
//                keyValue = headers.get("api_key").get(0);
//        }
//        if("special-key".equals(keyValue))
//            return true;
//        else
//            return false;
        return true;
    }

    public boolean isRemovingUnreferencedDefinitions() {
        return true;
    }
}
