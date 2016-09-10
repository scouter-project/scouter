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

##### 예
*${appropriate_directory}/scouter.conf*
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
For more options, refer Options page(Coming soon).
***



![login](../img/main/live-demo-client-login.png)