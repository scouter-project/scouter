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

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 25.
 */
public class ConfigureManager {
    private static final String SERVER_CONFIGURE_CLASS_NAME = "scouter.server.Configure";
    private static boolean isStandAlone = false;
    private static StandAloneConfigure conf = StandAloneConfigure.getInstance();

    static {
        try {
            Class.forName(SERVER_CONFIGURE_CLASS_NAME);
        } catch (ClassNotFoundException e) {
            isStandAlone = true;
        }
        //For standAlone mode testing on IDE.
        if ("true".equals(System.getProperty("scouterWebAppStandAlone"))) {
            isStandAlone = true;
        } else if ("false".equals(System.getProperty("scouterWebAppStandAlone"))) {
            isStandAlone = false;
        }

        System.out.println("[scouter web api embedded mode] " + !isStandAlone);
    }

    public static ConfigureAdaptor getConfigure() {
        return isStandAlone ? StandAloneConfigureAdaptor.getInstance() : EmbeddedConfigureAdaptor.getInstance();
    }

    public static StandAloneConfigure getStandAloneConfigure() {
        return conf;
    }

    private ConfigureManager() {
        try {
            Class.forName(SERVER_CONFIGURE_CLASS_NAME);
        } catch (ClassNotFoundException e) {
            isStandAlone = true;
        }
        System.out.println("[scouter web api embedded mode] " + !isStandAlone);
    }

    public String getLogDir() {
        if (isStandAlone) {
            return conf.log_dir;
        } else {
            return scouter.server.Configure.getInstance().log_dir;
        }
    }

    public int getLogKeepDays() {
        if (isStandAlone) {
            return conf.log_keep_days;
        } else {
            return scouter.server.Configure.getInstance().log_keep_days;
        }
    }

    public static boolean isStandAlone() {
        return isStandAlone;
    }
}
