# Stand-alone Java Batch Agent

스카우터 APM는 WAS 뿐만 아니라 배치 모니터링 기능도 제공한다.
배치는 일반적으로 대량 건을 처리하는 경우가 많아 일반 APM으로는 모니터링에 한계가 있었다. 스카우터는 배치 특성을 고려하여 통계 중심으로 성능을 분석하지만 자바 함수 레벨까지 분석할 수 있다.

스카우터 배치 에이전트는 아래와 같은 기능을 제공한다.
- 수행시간 측정(CPU 사용량)
- SQL 프로파일링(SQL문, SQL 수행시간, SQL 처리건수, SQL 수행횟수)
- 주기적인 프로세스 스택 수집  

## 자바 옵션
스카우터 배치 에이전트의 기본 설치 방법은 WAS 기반 자바 에이전트와 동일하다.
자바 배치 실행 명령(쉘 스크립트 파일)에 -javaagent와 -Dscouter.config 설정을 추가하면 모니터링이 가능하다.

```
JAVA_OPTS=" ${JAVA_OPTS} -javaagent:${SCOUTER_AGENT_DIR}/scouter.agent.batch.jar"
JAVA_OPTS=" ${JAVA_OPTS} -Dscouter.config=${SCOUTER_AGENT_DIR}/scouter.batch.conf"
```

## 환경설정

### 환경설정 파일
`${SCOUTER_AGENT_DIR}/scouter.batch.conf` 같이 환경설정 파일을 지정하고, 내용을 수정함으로써 적용 옵션을 변경할 수 있다.
> 옵션이 없으면 기본 값이 적용된다.

### 예
*${appropriate_directory}/scouter.batch.conf*
```
# Stand-Alone mode
scouter_standalone=false

# Batch ID (batch_id_type: class,args,props) (batch_id: args->index number, props->key string)
batch_id_type=args
batch_id=0

# Scouter Server IP Address (Default : 127.0.0.1)
net_collector_ip=127.0.0.1

# Scouter Server Port (Default : 6100)
net_collector_udp_port=6100
net_collector_tcp_port=6100

# Scouter Name(Default : batch)
obj_name=exbatch
```
***

## 자바 에이전트와 차이점
스카우터 배치 에이전트가 스카우트 자바 에이전트와 다른 점은 배치 에이전트가 설치된 서버에 별도 스카스터 배치 데몬을 실행시켜야 한다는 것이다.
이는 배치 프로세스 항상 수행중이지 않고, 수십개 이상 프로세스가 동시에 수행하기 때문에 항상 데몬 형태로 배치 서버에 상주하면서 배치 에이전트 환경설정이나 통합 정보 수집 및 전송을 담당할 프로세스가 필요하다.
스카우터 배치 데몬은 scouter.agent.batch.jar 내 포함되어 있다.

*start-batch-agent.sh*
```
nohup java -cp ${SCOUTER_AGENT_DIR}/scouter.agent.batch.jar -Dscouter.config=${SCOUTER_AGENT_DIR}/scouter.batch.conf scouter.agent.batch.Main &
```
***
스카우터 배치 데몬이 실행되지 않으면 배치 클라이언트에서 환경설정 수정, 스택 수집, 배치 성능 카운트 수집 등 기능이 정상 동작하지 않는다.

## 스카우터 배치 모니터링 화면
![Scouter](../img/client/batch_monitor_example1.png.png)
