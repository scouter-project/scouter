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

public class JmxServerValue {
    private String m_serverIP = null;
    private int m_port = 0;

    public JmxServerValue(String serverIP, int port) {
        m_serverIP = serverIP;
        m_port = port;
    }

    public String getServerIP() {
        return m_serverIP;
    }

    public int getPort() {
        return m_port;
    }
}
