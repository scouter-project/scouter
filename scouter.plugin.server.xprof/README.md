# XLog/Profile Kafka 서버 플러그인

XLog와 프로파일을 Kafka 토픽에 JSON으로 발행합니다. 토픽 기본값은
`scouter.xlog.profile`, 키는 `objHash:txid`입니다. 기존 JSON 필드는 유지합니다.

이벤트 콜백에서는 필요한 필드와 프로파일 바이트를 복사하고 제한된 큐에 넣습니다.
프로파일 해석, 텍스트 조회, JSON 변환, Kafka 초기화와 `send()`는 단일 데몬
스레드에서 수행합니다. Kafka 메타데이터/버퍼 대기가 수집 스레드로 전파되지 않습니다.
단, 데이터 복사와 큐 삽입에 드는 CPU/메모리 비용은 발생합니다.

수집 처리를 우선하는 best-effort 방식입니다. 큐 포화, 크기 제한 초과, 초기화
실패 시 데이터가 유실될 수 있으며 재발행하거나 디스크에 보관하지 않습니다.
큐는 제출 순서대로 처리하지만 동시 호출 간 순서나 Kafka 전달 성공을 보장하지 않습니다.

## 설정

서버 `conf/scouter.conf`:

```properties
ext_plugin_kafka_profile_enabled=true
ext_plugin_kafka_profile_bootstrap=127.0.0.1:9092
ext_plugin_kafka_profile_topic=scouter.xlog.profile
ext_plugin_kafka_profile_include_objHashs=
ext_plugin_kafka_profile_queue_capacity=256
ext_plugin_kafka_profile_max_profile_bytes=1048576
ext_plugin_kafka_profile_max_block_ms=100
ext_plugin_kafka_profile_debug=false
```

- 큐 크기는 최초 클래스 로딩 시 적용하며 변경 후 서버 재시작이 필요합니다.
- 프로파일 바이트 크기 제한은 복사 전에 적용합니다. 기본 큐가 보관하는 원본
  프로파일 바이트는 최대 약 256 MiB이며, 작업 중 데이터와 JSON/Kafka 버퍼는 별도입니다.
  서버 여유 메모리에 맞춰 큐 크기를 낮추세요. JSON 변환 후 메시지 크기는
  `ext_plugin_kafka_profile_max_request_size`(기본 1048576)의 제한도 받습니다.
- `max_block_ms`는 발행 작업 스레드의 메타데이터/버퍼 대기 제한입니다.
- 기존 `client_id`, `acks`, `retries`, `linger_ms`, `batch_size`, `compression`,
  `max_request_size`, `delivery_timeout_ms`, `request_timeout_ms`, `buffer_memory`
  설정도 동일한 `ext_plugin_kafka_profile_` 접두사로 지원합니다.
- producer 설정은 최초 초기화 성공 시 적용합니다. 초기화 실패 시 30초 동안
  발행 데이터를 버리고 이후 이벤트에서 재시도합니다.
- 오류 및 드롭 로그는 전체 합산 최대 30초에 한 번 출력합니다. 큐/크기/초기화
  거부 누적 수는 로그의 `dropped`와 `KafkaProfilePlugin.getDroppedCount()`로 확인합니다.
  Kafka 비동기 전송 실패 수는 이 거부 카운터에 포함되지 않습니다.
- 종료 시 신규 제출을 거부하고 대기열을 버립니다. 현재 작업을 인터럽트하고
  worker에서 Kafka close를 최대 1초 수행하며 호출자는 최대 2초 기다립니다.
  종료 시 미전송 데이터가 유실될 수 있습니다.

## 빌드 및 설치

```sh
mvn -pl scouter.plugin.server.xprof -am -Dmaven.test.skip=true clean package
```

동일 버전의 server/common이 로컬 Maven 저장소에 설치되어 있다면:

```sh
mvn -f scouter.plugin.server.xprof/pom.xml test -Dmaven.test.skip=false -DskipTests=false
mvn -f scouter.plugin.server.xprof/pom.xml -Dmaven.test.skip=true clean package
```

빌드 산물은 플러그인 코드와 공유 의존 라이브러리로 분리됩니다.

```text
target/
  scouter-plugin-server-xprof-2.21.4.jar
  lib/
    kafka-clients-3.7.2.jar
    jackson-databind-2.15.4.jar
    jackson-core-2.15.4.jar
    jackson-annotations-2.15.4.jar
    slf4j-api-1.7.25.jar
    lz4-java-1.8.0.jar
    snappy-java-1.1.10.5.jar
    zstd-jni-1.5.6-4.jar
```

플러그인 JAR와 `target/lib/*.jar`를 서버 설치 디렉터리의 `lib/`에 함께
복사하고 재시작합니다. 서버가 `lib/*.jar`를 클래스패스에 포함해야 합니다.
의존 라이브러리는 패키지 재배치 없이 원래 JAR로 생성되어 다른 플러그인도 공유할 수 있습니다.
이미 같은 라이브러리가 설치되어 있으면 호환되는 한 버전을 함께 사용하세요.

`scouter-server`와 `scouter-common`은 `provided`로 서버가 제공하며 복사 대상에서
제외합니다. 이들에만 필요한 전이 의존성 및 테스트 의존성도 `target/lib/`에 복사하지 않습니다.
플러그인의 compile/runtime 의존성과 그 전이 의존성은 자동으로 복사합니다.

`original-*` JAR는 더 이상 생성하지 않습니다. 이전 Shade 빌드 산물이나
제거된 의존성이 남지 않도록 `clean package`를 사용하세요.
서버에 설치했던 이전 fat JAR는 새 플러그인 JAR로 교체하고, sources/javadoc JAR는 배포하지 않습니다.
서버 로그의 `KafkaProfilePlugin=>xlog`, `KafkaProfilePlugin=>profile` 등록을 확인하세요.
