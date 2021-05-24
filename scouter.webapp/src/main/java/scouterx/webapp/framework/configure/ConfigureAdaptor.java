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

package scouterx.webapp.framework.configure;

import scouter.util.StrMatch;

import java.util.List;
import java.util.Set;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 26.
 */
public interface ConfigureAdaptor {
    String getLogDir();
    int getLogKeepDays();
    int getNetHttpPort();
    String getNetHttpExtWebDir();
    boolean isNetHttpApiAuthIpEnabled();
    boolean isNetHttpApiAuthSessionEnabled();
    boolean isNetHttpApiAuthBearerTokenEnabled();
    boolean isNetHttpApiGzipEnabled();

    Set<String> getNetHttpApiAllowIps();
    Set<String> getNetHttpApiAllowIpExact();
    List<StrMatch> getNetHttpApiAllowIpMatch();

    String getNetHttpApiAuthIpHeaderKey();
    int getNetHttpApiSessionTimeout();
    List<ServerConfig> getServerConfigs();
    String getTempDir();
    boolean isTrace();
    int getNetWebappTcpClientPoolSize();
    int getNetWebappTcpClientPoolTimeout();
    int getNetWebappTcpClientSoTimeout();
    boolean isNetHttpApiSwaggerEnabled();
    String getNetHttpApiSwaggerHostIp();
    String getNetHttpApiCorsAllowOrigin();
    String getNetHttpApiCorsAllowCredentials();
}
