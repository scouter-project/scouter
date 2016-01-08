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

public class EndUserNavigationData {
	public int uri;
	public int ip;
	public int count;
	//
	public long navigationStart;
	public long unloadEventStart;
	public long unloadEventEnd;
	public long fetchStart;
	public long domainLookupStart;
	public long domainLookupEnd;
	public long connectStart;
	public long connectEnd;
	public long requestStart;
	public long responseStart;
	public long responseEnd;
	public long domLoading;
	public long domInteractive;
	public long domContentLoadedEventStart;
	public long domContentLoadedEventEnd;
	public long domComplete;
	public long loadEventStart;
	public long loadEventEnd;

    @Override
    public String toString() {
        return "EndUserNavigationData{" +
                "uri=" + uri +
                ", ip=" + ip +
                ", count=" + count +
                ", navigationStart=" + navigationStart +
                ", unloadEventStart=" + unloadEventStart +
                ", unloadEventEnd=" + unloadEventEnd +
                ", fetchStart=" + fetchStart +
                ", domainLookupStart=" + domainLookupStart +
                ", domainLookupEnd=" + domainLookupEnd +
                ", connectStart=" + connectStart +
                ", connectEnd=" + connectEnd +
                ", requestStart=" + requestStart +
                ", responseStart=" + responseStart +
                ", responseEnd=" + responseEnd +
                ", domLoading=" + domLoading +
                ", domInteractive=" + domInteractive +
                ", domContentLoadedEventStart=" + domContentLoadedEventStart +
                ", domContentLoadedEventEnd=" + domContentLoadedEventEnd +
                ", domComplete=" + domComplete +
                ", loadEventStart=" + loadEventStart +
                ", loadEventEnd=" + loadEventEnd +
                '}';
    }
}
