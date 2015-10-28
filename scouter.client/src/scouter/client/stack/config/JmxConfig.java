/*
 *  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package scouter.client.stack.config;

import java.util.ArrayList;

public class JmxConfig {
    private int m_count = 100;
    private int m_interval = 50;
    private String m_filePath = null;

    private ArrayList<JmxServerValue> m_serverList = null;

    public JmxConfig(int count, int interval, String filePath) {
        m_count = count;
        m_interval = interval;
        m_filePath = filePath;
    }

    public String getFilePath() {
        return m_filePath;
    }

    public int getCount() {
        return m_count;
    }

    public int getInterval() {
        return m_interval;
    }

    public ArrayList<JmxServerValue> getServerList() {
        return m_serverList;
    }

    public void addServer( String ip, int port ) {
        if ( m_serverList == null ) {
            m_serverList = new ArrayList<JmxServerValue>();
        }
        m_serverList.add(new JmxServerValue(ip, port));
    }
}
