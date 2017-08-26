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

import scouter.server.Configure;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 26.
 */
public class EmbeddedConfigureAdaptor implements ConfigureAdaptor {
    private static Configure conf = Configure.getInstance();

    private static EmbeddedConfigureAdaptor instance = new EmbeddedConfigureAdaptor();
    public static EmbeddedConfigureAdaptor getInstance() {
        return instance;
    }

    private EmbeddedConfigureAdaptor() {}

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
}
