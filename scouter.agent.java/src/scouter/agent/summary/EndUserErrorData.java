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
 */
package scouter.agent.summary;

public class  EndUserErrorData {
    public int count;

    public int host;
    public int uri;
    public int stacktrace;
	public int userAgent;
	public int name;
	public int message;
	public int file;
    public int lineNumber;
    public int columnNumber;

    @Override
    public String toString() {
        return "EndUserErrorData{" +
                "count=" + count +
                ", host=" + host +
                ", uri=" + uri +
                ", stacktrace=" + stacktrace +
                ", userAgent=" + userAgent +
                ", name=" + name +
                ", message=" + message +
                ", file=" + file +
                ", lineNumber=" + lineNumber +
                ", columnNumber=" + columnNumber +
                '}';
    }
}
