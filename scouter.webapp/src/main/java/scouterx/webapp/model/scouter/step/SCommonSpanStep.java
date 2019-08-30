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

package scouterx.webapp.model.scouter.step;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import scouter.lang.step.CommonSpanStep;
import scouter.lang.step.StepSingle;
import scouter.lang.value.Value;
import scouter.util.IPUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 08/11/2018
 */
@Getter
public abstract class SCommonSpanStep extends StepSingle {

    public int hash;
    public int elapsed;
    public int error;

    public long timestamp;
    public byte spanType;

    public Endpoint localEndpoint;
    public Endpoint remoteEndpoint;

    public boolean debug;
    public boolean shared;

    public List<SpanAnnotation> annotations;
    public Map<String, String> tags;

    @Getter
    @AllArgsConstructor
    public static class SpanAnnotation {
        long timestamp;
        String value;
    }

    @Getter
    public static class Endpoint {
        int hash;
        @Setter
        String serviceName;
        String ip;
        int port;

        public Endpoint(int hash, String ip, int port) {
            this.hash = hash;
            this.ip = ip;
            this.port = port;
        }
    }

    public void setProps(CommonSpanStep org) {
        this.parent = org.getParent();
        this.index = org.getIndex();
        this.start_time = org.getStart_time();
        this.hash = org.getHash();
        this.elapsed = org.getElapsed();
        this.error = org.getError();
        this.timestamp = org.getTimestamp();
        this.spanType = org.spanType;
        this.localEndpoint = new Endpoint(org.getLocalEndpointServiceName(), IPUtil.toString(org.getLocalEndpointIp()), org.getLocalEndpointPort());
        this.remoteEndpoint = new Endpoint(org.getRemoteEndpointServiceName(), IPUtil.toString(org.getRemoteEndpointIp()), org.getRemoteEndpointPort());
        this.debug = org.debug;
        this.shared = org.shared;

        this.annotations = new ArrayList<>();
        for (int i = 0; i < org.annotationTimestamps.size(); i++) {
            annotations.add(new SpanAnnotation(org.getAnnotationTimestamps().getLong(i), org.getAnnotationValues().getString(i)));
        }

        this.tags = new HashMap<>();
        for (Map.Entry<String, Value> entry : org.tags.toMap().entrySet()) {
            tags.put(entry.getKey(), entry.getValue().toString());
        }
    }
}