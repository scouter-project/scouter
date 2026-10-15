package scouterx.webapp.layer.websock;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import scouterx.webapp.layer.websock.dto.ClientParams;
import scouterx.webapp.model.scouter.SActiveService;
import scouterx.webapp.model.scouter.SCounter;
import scouterx.webapp.request.RealTimeXLogRequest;
import scouterx.webapp.view.CommonResultView;
import scouterx.webapp.layer.controller.XLogController;
import scouterx.webapp.layer.controller.VisitorController;
import scouterx.webapp.layer.controller.ActiveServiceController;
import scouterx.webapp.layer.controller.CounterController;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@ServerEndpoint(value = "/scouter/v1/ws/paper/realTime", configurator = ClientIpConfigurator.class)
@Slf4j
public class PaperGlobalWebSocket {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Map<Session, ClientParams> subscriptions = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService globalTimer = Executors.newSingleThreadScheduledExecutor();

    // WebApp JAX-RS 컨트롤러 인스턴스 (스프링이나 DI 환경에 맞게 주입 또는 생성)
    private static final XLogController xLogController = new XLogController();
    private static final VisitorController visitorController = new VisitorController();
    private static final CounterController counterController = new CounterController();
    private static final ActiveServiceController activeServiceController = new ActiveServiceController();

    private static class GlobalRequestUnion {
        Set<Integer> objHashes = new HashSet<>();
        Set<String> counterNames = new HashSet<>();
        Set<String> objTypes = new HashSet<>();
        int minXlogLoop = Integer.MAX_VALUE;
        int minXlogIndex = Integer.MAX_VALUE;
    }

    static {
        globalTimer.scheduleAtFixedRate(() -> {
            if (subscriptions.isEmpty()) return;

            try {
                // 1. [그룹화 및 합집합]
                Map<Integer, GlobalRequestUnion> serverGroups = new HashMap<>();
                for (ClientParams params : subscriptions.values()) {
                    int sid = params.getServerId();
                    GlobalRequestUnion union = serverGroups.computeIfAbsent(sid, k -> new GlobalRequestUnion());
                    
                    if (params.getObjHashes() != null) union.objHashes.addAll(params.getObjHashes());
                    if (params.getCounterNames() != null) union.counterNames.addAll(params.getCounterNames());
                    if (params.getObjTypes() != null) union.objTypes.addAll(params.getObjTypes());
                    if (params.getXlogLoop() < union.minXlogLoop) union.minXlogLoop = params.getXlogLoop();
                    if (params.getXlogIndex() < union.minXlogIndex) union.minXlogIndex = params.getXlogIndex();
                }

                // 2. [단일 수집]
                for (Map.Entry<Integer, GlobalRequestUnion> entry : serverGroups.entrySet()) {
                    int currentServerId = entry.getKey();
                    GlobalRequestUnion union = entry.getValue();
                    
                    if (union.objHashes.isEmpty()) continue;

                    // 컨트롤러 파라미터 요구사항(String)에 맞게 포맷 변환
                    // objHashes -> "[1234567, 89101112]" 형태의 문자열
                    String objHashesString = "[" + union.objHashes.stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(",")) + "]";
                    
                    // counterNames -> "TPS,HeapUsed" 형태의 문자열
                    String countersString = String.join(",", union.counterNames);

                    // --- 1. XLog 데이터 수집 ---
                    Object globalXlogData = retrieveXLogData(currentServerId, objHashesString, union.minXlogLoop, union.minXlogIndex);
                    
                    // --- 2. Visitor 데이터 수집 ---
                    CommonResultView<Long> visitorResult = visitorController.retrieveVisitorRealtimeByObjHashes(objHashesString, currentServerId);
                    Object globalVisitorData = visitorResult.getResult();

                    // --- 3. Active Service 데이터 수집 ---
                    Map<String, Object> globalActiveServiceMap = new HashMap<>();
                    for (String objType : union.objTypes) {
                        CommonResultView<List<SActiveService>> activeResult = activeServiceController.retrieveRealTimeActiveServiceListByObjType(objType, currentServerId);
                        globalActiveServiceMap.put(objType, activeResult.getResult());
                    }

                    // --- 4. Counter 데이터 수집 ---
                    Object globalCounterData = null;
                    if (!union.counterNames.isEmpty()) {
                        CommonResultView<List<SCounter>> counterResult = counterController.retrieveRealTimeCountersByObjHashes(objHashesString, countersString, currentServerId);
                        globalCounterData = counterResult.getResult();
                    }

                    // 3. [라우팅] - 수집된 컨트롤러 응답 객체(Data)를 세션별 필터링 후 전송
                    for (Map.Entry<Session, ClientParams> subEntry : subscriptions.entrySet()) {
                        Session session = subEntry.getKey();
                        ClientParams params = subEntry.getValue();

                        if (session.isOpen() && params.getServerId() == currentServerId) {
                            sendData(session, "XLOG", filterXLogData(globalXlogData, params));
                            sendData(session, "VISITOR", filterDataByObjHash(globalVisitorData, params.getObjHashes()));
                            
                            for (String reqObjType : params.getObjTypes()) {
                                if (globalActiveServiceMap.containsKey(reqObjType)) {
                                    sendData(session, "ACTIVE_SERVICE_" + reqObjType.toUpperCase(), globalActiveServiceMap.get(reqObjType));
                                }
                            }
                            sendData(session, "COUNTER", filterCounterData(globalCounterData, params));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    // --- WebApp JAX-RS Controller 연동 구체화 메서드 ---
    private static Object retrieveXLogData(int serverId, String objHashesString, int minLoop, int minIndex) {
        RealTimeXLogRequest xLogRequest = new RealTimeXLogRequest();
        xLogRequest.setServerId(serverId);
        xLogRequest.setXLogLoop(minLoop);
        xLogRequest.setXLogIndex(minIndex);
        xLogRequest.setObjHashes(objHashesString);

        Response response = xLogController.streamRealTimeXLog(xLogRequest);
        Object entity = response.getEntity(); 

        // 1. 반환된 Entity가 람다(StreamingOutput) 형식인 경우 직접 실행하여 데이터를 추출합니다.
        if (entity instanceof StreamingOutput) {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                // 스트리밍 출력을 메모리 버퍼에 기록
                ((StreamingOutput) entity).write(baos);
                String jsonStr = baos.toString("UTF-8");

                // 빈 데이터인 경우 처리
                if (jsonStr == null || jsonStr.trim().isEmpty()) {
                    return null;
                }

                // 2. 추출된 JSON 문자열을 sendData에서 다시 직렬화할 수 있도록 JsonNode(객체)로 변환
                return mapper.readTree(jsonStr);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        
        // StreamingOutput이 아닌 일반 객체인 경우 그대로 반환
        return entity; 
    }

    // --- 기본 웹소켓 메서드 ---
    @OnOpen
    public void onOpen(Session session) {
        subscriptions.put(session, new ClientParams());
        log.info("WebSocket Connect: {}", session.getUserProperties().get("client-ip"));
        String payload = String.format("{\"type\":\"%s\", \"data\":\"%s\"}", "MESSAGE", "You are now connected to " + this.getClass().getName());
        session.getAsyncRemote().sendText(payload);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            subscriptions.put(session, mapper.readValue(message, ClientParams.class));
            log.info("WebSocket Subscription Update: {} - {}", session.getUserProperties().get("client-ip"), message);
        } 
        catch (Exception e) { e.printStackTrace(); }
    }

    @OnClose
    public void onClose(Session session) {
        try {
            subscriptions.remove(session);
            session.close();
            log.info("WebSocket Close: {}", session.getUserProperties().get("client-ip"));
        } catch (IOException e) { e.printStackTrace(); }
    }

    @OnError
    public void onError(Session session, Throwable cause) {
        log.warn("WebSocket Error", cause);
        try {
            subscriptions.remove(session);
            session.close();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private static void sendData(Session session, String type, Object data) throws IOException {
        if (data == null) return;
        String payload = String.format("{\"type\":\"%s\", \"data\":%s}", type, mapper.writeValueAsString(data));
        session.getAsyncRemote().sendText(payload);
    }

    // --- 필터링 로직 
    private static Object filterXLogData(Object globalXlogData, ClientParams params) {
        if (globalXlogData == null || params.getObjHashes() == null) return null;

        // XLog 응답이 List 형태일 경우 세션의 objHash 기준으로 필터링
        if (globalXlogData instanceof List) {
            List<?> xlogList = (List<?>) globalXlogData;
            return xlogList.stream().filter(item -> {
                Integer objHash = extractObjHash(item);
                // 현재 세션이 요청한 objHash에 속하는 XLog인지 확인
                return objHash != null && params.getObjHashes().contains(objHash);
                
                // TODO (선택 사항): xlogLoop와 xlogIndex를 활용해 
                // 세션별로 이미 수신한 커서 이전의 데이터는 중복 전송되지 않도록 시간/인덱스 필터링 추가 가능
            }).collect(Collectors.toList());
        }
        return globalXlogData;
    }

    private static Object filterDataByObjHash(Object globalData, Set<Integer> targetHashes) {
        if (globalData == null || targetHashes == null) return null;

        if (globalData instanceof List) {
            List<?> dataList = (List<?>) globalData;
            return dataList.stream().filter(item -> {
                // SActiveService인 경우 명시적 캐스팅 처리
                if (item instanceof SActiveService) {
                    return targetHashes.contains(((SActiveService) item).getObjHash());
                }
                // 그 외 범용 객체인 경우 리플렉션을 통해 추출
                Integer objHash = extractObjHash(item);
                return objHash != null && targetHashes.contains(objHash);
            }).collect(Collectors.toList());
        }
        
        // [주의] Visitor API의 경우, 컨트롤러 리턴 타입이 개별 데이터 List가 아닌 CommonResultView<Long> (합산 값)일 수 있습니다.
        // 이 경우 단일 값은 세션별로 쪼갤 수 없으므로, 합집합을 요청한 그룹 전체의 합산 데이터가 그대로 반환됩니다.
        return globalData;
    }

    private static Object filterCounterData(Object allCounters, ClientParams params) {
        if (allCounters == null || params.getCounterNames() == null || params.getObjHashes() == null) return null;

        if (allCounters instanceof List) {
            List<?> counterList = (List<?>) allCounters;
            return counterList.stream().filter(item -> {
                if (item instanceof SCounter) {
                    SCounter counter = (SCounter) item;
                    // 해당 세션이 구독한 objHash와 counterName 모두에 일치하는 데이터만 추출
                    return params.getObjHashes().contains(counter.getObjHash()) &&
                           params.getCounterNames().contains(counter.getName());
                }
                return false;
            }).collect(Collectors.toList());
        }
        return allCounters;
    }

    /**
     * 다양한 모델 객체(Map 또는 특정 DTO)에서 objHash 값을 안전하게 추출하는 범용 헬퍼 메서드
     */
    private static Integer extractObjHash(Object item) {
        if (item == null) return null;
        try {
            if (item instanceof Map) {
                Object val = ((Map<?, ?>) item).get("objHash");
                if (val instanceof Number) return ((Number) val).intValue();
            } else {
                // 특정 클래스의 import 없이 리플렉션으로 getObjHash() 메서드 동적 호출 (XLogData 모델 등 대응)
                java.lang.reflect.Method method = item.getClass().getMethod("getObjHash");
                Object val = method.invoke(item);
                if (val instanceof Number) return ((Number) val).intValue();
            }
        } catch (Exception e) {
            // objHash 필드/메서드가 없는 객체인 경우 무시
        }
        return null;
    }
}
