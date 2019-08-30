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

package scouterx.webapp.layer.consumer;

import scouter.lang.constants.ParamConstant;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouterx.webapp.framework.client.model.AgentModelThread;
import scouterx.webapp.framework.client.model.AgentObject;
import scouterx.webapp.framework.client.net.TcpProxy;
import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.framework.client.server.ServerManager;
import scouterx.webapp.model.scouter.SCounter;
import scouterx.webapp.request.CounterAvgRequest;
import scouterx.webapp.request.CounterAvgRequestByObjHashes;
import scouterx.webapp.request.CounterAvgRequestByType;
import scouterx.webapp.request.CounterRequest;
import scouterx.webapp.request.CounterRequestByObjHashes;
import scouterx.webapp.request.CounterRequestByType;
import scouterx.webapp.view.AvgCounterView;
import scouterx.webapp.view.CounterView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 */
public class CounterConsumer {

    /**
     * get realtime counters's values by type
     * @param objType
     * @param counterNames
     * @param server
     * @return
     */
    public List<SCounter> retrieveRealTimeCountersByObjType(final String objType, Set<String> counterNames, final Server server) {
        return retrieveRealTimeCountersByObjTypeOrObjHashes(objType, null, counterNames, server);
    }

    /**
     * get realtime counters's values by obj hashes
     * @param objHashSet
     * @param counterNames
     * @param server
     * @return
     */
    public List<SCounter> retrieveRealTimeCountersByObjHashes(final Set<Integer> objHashSet, Set<String> counterNames, final Server server) {
        return retrieveRealTimeCountersByObjTypeOrObjHashes(null, objHashSet, counterNames, server);
    }

    /**
     * get counters's values by type in specific time range
     * @param request
     */
    public List<CounterView> retrieveCounterByObjType(CounterRequestByType request) {
        return retrieveCounterByObjTypeOrObjHashes(request);
    }

    /**
     * get counters's values by objs in specific time range
     * @param request
     */
    public List<CounterView> retrieveCounterByObjHashes(CounterRequestByObjHashes request) {
        return retrieveCounterByObjTypeOrObjHashes(request);
    }

    /**
     * get counters's values by type or obj hashes in specific time range
     * @param request
     *
     */
    private List<CounterView> retrieveCounterByObjTypeOrObjHashes(CounterRequest request) {
        Server server = ServerManager.getInstance().getServerIfNullDefault(request.getServerId());

        MapPack paramPack = new MapPack();
        if (request instanceof CounterRequestByType) {
            paramPack.put(ParamConstant.OBJ_TYPE, ((CounterRequestByType) request).getObjType());

        } else if (request instanceof CounterRequestByObjHashes) {
            ListValue objHashLv = paramPack.newList(ParamConstant.OBJ_HASH);
            for (Integer objHash : ((CounterRequestByObjHashes) request).getObjHashes()) {
                objHashLv.add(objHash);
            }
        }

        paramPack.put(ParamConstant.COUNTER, request.getCounter());

        Map<Integer, CounterView> counterViewMap = new HashMap<>();

        long startDateUnit = DateUtil.getDateUnit(request.getStartTimeMillis());
        for (long dateUnit = startDateUnit; DateUtil.dateUnitToTimeMillis(dateUnit) < request.getEndTimeMillis(); dateUnit++) {
            paramPack.put(ParamConstant.STIME, Math.max(DateUtil.dateUnitToTimeMillis(dateUnit) + 1000, request.getStartTimeMillis()));
            paramPack.put(ParamConstant.ETIME, Math.min(DateUtil.dateUnitToTimeMillis(dateUnit) + DateUtil.MILLIS_PER_DAY - 1000, request.getEndTimeMillis()));
            List<CounterView> counterViews = retrieveCounterInDay(request, server, paramPack);
            for (CounterView counterView : counterViews) {
                CounterView counterViewInMap = counterViewMap.get(counterView.getObjHash());
                if (counterViewInMap == null) {
                    counterViewMap.put(counterView.getObjHash(), counterView);
                } else {
                    counterViewInMap.getTimeList().addAll(counterView.getTimeList());
                    counterViewInMap.getValueList().addAll(counterView.getValueList());
                    counterViewInMap.setEndTimeMillis(counterView.getEndTimeMillis());
                }
            }
        }

        return new ArrayList<>(counterViewMap.values());
    }

    private List<CounterView> retrieveCounterInDay(CounterRequest request, Server server, MapPack paramPack) {
        List<CounterView> counterViewList = new ArrayList<>();
        try(TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
            tcpProxy.process(RequestCmd.COUNTER_PAST_TIME_ALL, paramPack, in -> {
                MapPack mapPack = (MapPack) in.readPack();

                if (mapPack != null) {
                    int objHash = mapPack.getInt(ParamConstant.OBJ_HASH);
                    ListValue timeList = mapPack.getList(ParamConstant.TIME);
                    ListValue valueList = mapPack.getList(ParamConstant.VALUE);

                    List<Double> valueToDoubleList = new ArrayList<>();
                    for (int i = 0; i < timeList.size(); i++) {
                        valueToDoubleList.add(valueList.getDouble(i));
                    }

                    AgentObject agentObject = AgentModelThread.getInstance().getAgentObject(objHash);
                    String objType = agentObject.getObjType();

                    CounterView counterView = CounterView.builder()
                            .objHash(objHash)
                            .objName(agentObject.getObjName())
                            .name(request.getCounter())
                            .displayName(server.getCounterEngine().getCounterDisplayName(objType, request.getCounter()))
                            .unit(server.getCounterEngine().getCounterUnit(objType, request.getCounter()))
                            .startTimeMillis(request.getStartTimeMillis())
                            .endTimeMillis(request.getEndTimeMillis())
                            .timeList(Arrays.stream(timeList.toObjectArray()).map(Long.class::cast).collect(Collectors.toList()))
                            .valueList(valueToDoubleList)
                            .build();

                    counterViewList.add(counterView);
                }
            });
        }
        return counterViewList;
    }

    /**
     * get daily counter (precision : 5 min avg) values by objType
     *
     */
    public List<AvgCounterView> retrieveAvgCounterByObjType(CounterAvgRequestByType request) {
        return retrieveAvgCounterByObjTypeOrHashes(request);
    }

    /**
     * get daily counter (precision : 5 min avg) values by objHashes
     *
     */
    public List<AvgCounterView> retrieveAvgCounterByObjHashes(CounterAvgRequestByObjHashes request) {
        return retrieveAvgCounterByObjTypeOrHashes(request);
    }

    /**
     * get daily counter (precision : 5 min avg) values by objType or hashes
     *
     */
    public List<AvgCounterView> retrieveAvgCounterByObjTypeOrHashes(CounterAvgRequest request) {
        MapPack paramPack = new MapPack();
        if (request instanceof CounterAvgRequestByType) {
            paramPack.put(ParamConstant.OBJ_TYPE, ((CounterAvgRequestByType) request).getObjType());
        } else if (request instanceof CounterAvgRequestByObjHashes) {
            ListValue objHashLv = paramPack.newList(ParamConstant.OBJ_HASH);
            for (Integer objHash : ((CounterAvgRequestByObjHashes) request).getObjHashes()) {
                objHashLv.add(objHash);
            }
        }
        paramPack.put(ParamConstant.SDATE, request.getStartYmd());
        paramPack.put(ParamConstant.EDATE, request.getEndYmd());
        paramPack.put(ParamConstant.COUNTER, request.getCounter());

        Map<Integer, AvgCounterView> counterViewMap = new HashMap<>();
        Server server = ServerManager.getInstance().getServerIfNullDefault(request.getServerId());

        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
            tcpProxy.process(RequestCmd.COUNTER_PAST_LONGDATE_ALL, paramPack, in -> {
                MapPack mapPack = (MapPack) in.readPack();
                if (mapPack != null) {
                    int objHash = mapPack.getInt(ParamConstant.OBJ_HASH);
                    ListValue timeList = mapPack.getList(ParamConstant.TIME);
                    ListValue valueList = mapPack.getList(ParamConstant.VALUE);

                    List<Double> valueToDoubleList = new ArrayList<>();
                    for (int i = 0; i < timeList.size(); i++) {
                        valueToDoubleList.add(valueList.getDouble(i));
                    }

                    AgentObject agentObject = AgentModelThread.getInstance().getAgentObject(objHash);
                    String objType = agentObject.getObjType();

                    AvgCounterView counterView = AvgCounterView.builder()
                            .objHash(objHash)
                            .objName(agentObject.getObjName())
                            .name(request.getCounter())
                            .displayName(server.getCounterEngine().getCounterDisplayName(objType, request.getCounter()))
                            .unit(server.getCounterEngine().getCounterUnit(objType, request.getCounter()))
                            .fromYmd(request.getStartYmd())
                            .toYmd(request.getEndYmd())
                            .timeList(Arrays.stream(timeList.toObjectArray()).map(Long.class::cast).collect(Collectors.toList()))
                            .valueList(valueToDoubleList)
                            .build();

                    AvgCounterView counterViewInMap = counterViewMap.get(counterView.getObjHash());

                    if (counterViewInMap == null) {
                        counterViewMap.put(counterView.getObjHash(), counterView);
                    } else {
                        counterViewInMap.getTimeList().addAll(counterView.getTimeList());
                        counterViewInMap.getValueList().addAll(counterView.getValueList());
                    }
                }
            });
        }

        return new ArrayList<>(counterViewMap.values());
    }








    /**
     * retrieve realtime counter value by objtype of objhash
     *
     */
    private List<SCounter> retrieveRealTimeCountersByObjTypeOrObjHashes(final String objType, final Set<Integer> objHashSet,
                                                                        Set<String> counterNames, final Server server) {
        MapPack paramPack = new MapPack();
        paramPack.put(ParamConstant.OBJ_TYPE, objType);
        ListValue counterNameLv = paramPack.newList(ParamConstant.COUNTER);
        for (String name : counterNames) {
            counterNameLv.add(name);
        }
        ListValue objHashLv = paramPack.newList(ParamConstant.OBJ_HASH);
        if (objHashSet != null) {
            for (Integer objHash : objHashSet) {
                objHashLv.add(objHash);
            }
        }

        MapPack outMapPack;
        try(TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
            outMapPack = (MapPack) tcpProxy.getSingle(RequestCmd.COUNTER_REAL_TIME_ALL_MULTI, paramPack);
        }

        if (outMapPack == null) {
            return Collections.emptyList();
        }

        ListValue rObjHashLv = (ListValue) outMapPack.get(ParamConstant.OBJ_HASH);
        ListValue rCounterNameLv = (ListValue) outMapPack.get(ParamConstant.COUNTER);
        ListValue rCounterValueLv = (ListValue) outMapPack.get(ParamConstant.VALUE);

        List<SCounter> resultList = new ArrayList<>();
        for(int i = 0; i < rObjHashLv.size(); i++) {
            int objHash = CastUtil.cint(rObjHashLv.get(i));
            String counterName = rCounterNameLv.getString(i);
            Object counterValue = rCounterValueLv.get(i).toJavaObject();

            resultList.add(new SCounter(objHash, counterName, counterValue));
        }
        return resultList;
    }
}
