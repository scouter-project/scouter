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

package scouterx.webapp.layer.controller;

import io.swagger.annotations.Api;
import scouter.lang.counters.CounterEngine;
import scouter.util.HashUtil;
import scouterx.webapp.framework.client.model.AgentModelThread;
import scouterx.webapp.framework.client.model.AgentObject;
import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.framework.client.server.ServerManager;
import scouterx.webapp.framework.util.ZZ;
import scouterx.webapp.layer.service.ActiveServiceService;
import scouterx.webapp.layer.service.CounterService;
import scouterx.webapp.layer.service.InteractionCounterService;
import scouterx.webapp.model.InteractionCounterData;
import scouterx.webapp.model.scouter.SCounter;
import scouterx.webapp.view.CommonResultView;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2018. 8. 24.
 */
@Path("/v1/interactionCounter")
@Api("InteractionCounter")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class InteractionCounterController {
    public static final String JAVAEE = "javaee";
    @Context
    HttpServletRequest servletRequest;
    static Set<String> hostCounterSet = new HashSet<>();
    static Set<String> javaCounterSet = new HashSet<>();

    public static final String CPU = "Cpu";
    public static final String _CPU = ":" + CPU;

    public static final String ACTIVE_SPEED = "ActiveSpeed";
    public static final String _ACTIVE_SPEED = ":" + ACTIVE_SPEED;

    static {
        hostCounterSet.add(CPU);
        javaCounterSet.add(ACTIVE_SPEED);
    }

    private final InteractionCounterService service;
    private final CounterService counterService;
    private final ActiveServiceService activeServiceService;

    public InteractionCounterController() {
        this.service = new InteractionCounterService();
        this.counterService = new CounterService();
        this.activeServiceService = new ActiveServiceService();
    }

    /**
     * get current value of interaction counters about a type
     * uri : /interactionCounter/realTime/ofType/{objType}?serverId=1001010
     *
     * @param objType
     * @param serverId
     * @see scouter.lang.counters.CounterConstants
     */
    @GET
    @Path("/realTime/ofType/{objType}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<List<InteractionCounterData>> retrieveRealTimeCountersByObjType(
            @PathParam("objType") @Valid @NotNull final String objType,
            @QueryParam("serverId") final int serverId) {

        List<InteractionCounterData> counterList = service.retrieveRealTimeByObjType(objType,
                ServerManager.getInstance().getServerIfNullDefault(serverId));

        return CommonResultView.success(counterList);
    }

    /**
     * get current value of interaction counters of objects
     * uri : /interactionCounter/realTime?serverId=1001010&objHashes=100,200,300
     *
     * @see scouter.lang.counters.CounterConstants
     */
    @GET
    @Path("/realTime")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<List<InteractionCounterData>> retrieveRealTimeCountersByObjHashes(
            @QueryParam("objHashes") @Valid @NotNull final String objHashes,
            @QueryParam("serverId") final int serverId) {

        Set<Integer> objHashSet = ZZ.splitParamAsIntegerSet(objHashes);
        Server server = ServerManager.getInstance().getServerIfNullDefault(serverId);
        List<InteractionCounterData> counterList = service.retrieveRealTimeByObjHashes(objHashSet, server);

        Set<Integer> resultObjHashSet = new HashSet<>();
        for (InteractionCounterData data : counterList) {
            resultObjHashSet.add(data.fromObjHash);
            resultObjHashSet.add(data.toObjHash);
        }
        Map<String, SCounter> hostCounters = findHostCounters(resultObjHashSet, server);
        Map<String, SCounter> javaCounters = findJavaCounters(resultObjHashSet, server);
        AgentModelThread agentModel = AgentModelThread.getInstance();
        for (InteractionCounterData data : counterList) {
            AgentObject fromParent = agentModel.getAgentObject(data.fromObjHash);
            AgentObject toParent = agentModel.getAgentObject(data.toObjHash);

            data.addFromObjCounter(hostCounters.get(fromParent == null ? null : fromParent.getParentHash() + _CPU));
            data.addFromObjCounter(javaCounters.get(data.fromObjHash + _ACTIVE_SPEED));
            data.addToObjCounter(hostCounters.get(toParent == null ? null : toParent.getParentHash() + _CPU));
            data.addToObjCounter(javaCounters.get(data.toObjHash + _ACTIVE_SPEED));
        }
        return CommonResultView.success(counterList);
    }

    private Map<String, SCounter> findHostCounters(Set<Integer> objHashSet, Server server) {
        Map<Integer, AgentObject> agentObjectMap = AgentModelThread.getInstance().getAgentObjectMap();
        Set<Integer> parentHashes = objHashSet.stream()
                .map(objHash -> agentObjectMap.get(objHash))
                .filter(Objects::nonNull)
                .map(agent -> HashUtil.hash(agent.getParentName()))
                .collect(Collectors.toSet());

        List<SCounter> hostCounters = counterService
                .retrieveRealTimeCountersByObjHashes(parentHashes, hostCounterSet, server);

        return hostCounters.stream().collect(Collectors.toMap(o -> o.getObjHash() + ":" + o.getName(), o -> o));
    }

    private Map<String, SCounter> findJavaCounters(Set<Integer> objHashSet, Server server) {
        CounterEngine counterEngine = server.getCounterEngine();
        Map<Integer, AgentObject> agentObjectMap = AgentModelThread.getInstance().getAgentObjectMap();

        Set<Integer> javaHashes = objHashSet.stream()
                .map(objHash -> agentObjectMap.get(objHash))
                .filter(Objects::nonNull)
                .filter(agent -> JAVAEE.equals(counterEngine.getFamilyNameFromObjType(agent.getObjType())))
                .map(agent -> agent.getObjHash())
                .collect(Collectors.toSet());

        List<SCounter> javaCounters = counterService
                .retrieveRealTimeCountersByObjHashes(javaHashes, javaCounterSet, server);

        return javaCounters.stream().collect(Collectors.toMap(o -> o.getObjHash() + ":" + o.getName(), o -> o));
    }
}
