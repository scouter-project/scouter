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

package scouterx.webapp.configure;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 26.
 */
public class StandAloneConfigureAdaptor implements ConfigureAdaptor {
    private static StandAloneConfigure conf = StandAloneConfigure.getInstance();

    private static StandAloneConfigureAdaptor instance = new StandAloneConfigureAdaptor();

    public static StandAloneConfigureAdaptor getInstance() {
        return instance;
    }

    private StandAloneConfigureAdaptor() {}

    @Override
    public String getLogDir() {
        return conf.log_dir;
    }

    @Override
    public int getLogKeepDays() {
        return conf.log_keep_days;
    }

    @Override
    public int getNetHttpPort() {
        return conf.net_http_port;
    }

    @Override
    public boolean isNetHttpApiAuthIpEnabled() {
        return conf.net_http_api_auth_ip_enabled;
    }

    @Override
    public boolean isNetHttpApiAuthSessionEnabled() {
        return conf.net_http_api_auth_session_enabled;
    }

    @Override
    public Set<String> getNetHttpApiAllowIps() {
        return Stream.of(conf.net_http_api_allow_ips.split(",")).collect(Collectors.toSet());
    }

    @Override
    public String getNetHttpApiAuthIpHeaderKey() {
        return conf.net_http_api_auth_ip_header_key;
    }
}
