# 자바 에이전트로 Custom MBean 값 추출하여 모니터링

## MBean 값 추출하여 저장까지
자바에이전트의 scouter.conf 파일에 아래 설정이 필요합니다
```
counter_custom_jmx_enabled=true
custom_jmx_set={COUNTER_NAME}|{MBEAN_NAME}|{ATTRIBUTE_NAME}||{COUNTER_NAME}|{MBEAN_NAME}|{ATTRIBUTE_NAME}||...
```

- COUNTER_NAME : 수집된 값이 저장될 키 값, 간단한 String
- MBEAN_NAME : MBean 의 Object Name, 정확히 단일개를 가리키는 값이어야 합니다
- ATTRIBUTE_NAME : 추출하고자 하는 MBean의 Attribute 이름, 그 값이 Number 타입 이어야 합니다

각 값들은 '|' 로 구분 되고 여러개를 지정하기 위해서는 각 세트를 '||' 로 구분합니다.

#### Example
```
counter_custom_jmx_enabled=true
custom_jmx_set=WriteThroughput|org.apache.cassandra.metrics:type=ClientRequest,scope=Write,name=Latency|OneMinuteRate||ReadThroughput|org.apache.cassandra.metrics:type=ClientRequest,scope=Read,name=Latency|OneMinuteRate||ConnectedClients|org.apache.cassandra.metrics:type=Client,name=connectedNativeClients|Value||BlockedTask|org.apache.cassandra.metrics:type=ThreadPools,path=request,scope=RequestResponseStage,name=CurrentlyBlockedTasks|Count||PendingTask|org.apache.cassandra.metrics:type=ThreadPools,path=request,scope=RequestResponseStage,name=PendingTasks|Value
```


## 추출하여 저장된 값 모니터링

1. 자바에이전트의 scouter.conf 에 obj_type 주기 :해당 오브젝트들을 그루핑 할 수 있는 obj_type 을 정하여 설정에 넣어줍니다 (ex. obj_type=cassandra)
2. 클라이언트에서 해당 오브젝트 우클릭하여 Define ObjectType (혹은 Edit ObjectType) 해주기 : Family 를 *javaee* 로 하고 적절한 Display Name 과 아이콘을 설정해 줍니다.
3. {스카우터서버_디렉토리}/conf/counter.site.xml 파일 수정해주기 : 2번 작업이 잘 적용되었으면 counters.site.xml 이 생겼을 것입니다
  ```xml
  <ObjectType disp="Cassandra" family="javaee" icon="java" name="cassandra" sub-object="false"/>
  ```

이부분에 추출하여 저장한 COUNTER_NAME 이 name 속성에 들어가도록, 그외 display name, icon, unit 등을 세팅해주고 저장합니다. 아래 예제를 참조하세요

```xml
   <ObjectType disp="Cassandra" family="javaee" icon="java" name="cassandra" sub-object="false">
     <Counter disp="WPS" icon="visitor.png" name="WriteThroughput" unit="wps"/>
     <Counter disp="RPS" icon="visitor.png" name="ReadThroughput" unit="rps"/>
     <Counter disp="ConnectedClients" icon="visitor.png" name="ConnectedClients" unit="cnt"/>
     <Counter disp="BlockedTask" icon="visitor.png" name="BlockedTask" unit="cnt"/>
     <Counter disp="PendingTask" icon="visitor.png" name="PendingTask" unit="cnt"/>
   </ObjectType>
```
4. 클라이언트 재시작 후 해당 카운터가 오브젝트뷰 -> 컨텍스트 메뉴 리스트에 나타나는지 확인하고 값이 정상적으로 차트에 그려지는지 확인합니다.
