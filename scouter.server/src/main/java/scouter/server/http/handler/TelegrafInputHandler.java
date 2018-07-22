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

package scouter.server.http.handler;

import scouter.server.http.model.InfluxSingleLine;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2018. 7. 22.
 */
public class TelegrafInputHandler {
    public void handlerRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        long receivedTime = System.currentTimeMillis();
        Map<String, String> uniqueLineString = new HashMap<String, String>();
        int lineCount = 0;
        while (true) {
            if (lineCount++ > 500) {
                //TODO 응답 주고 끝내자.
            }

            String lineString = request.getReader().readLine();
            if (lineString == null) {
                break;
            }
            uniqueLineString.put(InfluxSingleLine.toLineStringKey(lineString), lineString);
        }

        for (Map.Entry<String, String> lineStringEntry : uniqueLineString.entrySet()) {

        }
    }
}
