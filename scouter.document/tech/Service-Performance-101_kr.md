# Service Performance 101
[![English](https://img.shields.io/badge/language-English-orange.svg)](Service-Performance-101.md) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](Service-Performance-101_kr.md)

성능관리의 시작은 성능그래프의 이해로 부터 시작된다.
만약 그래프가 주는 의미를 파악하지 못한다면 아무리 많은 데이터를 수집해도 문제를 해결 할 수 없습니다.

아래는 가상으로 사용자를 늘리는 성능테스트 중 SCOUTER를 모니터링하는 화면 캡처입니다.
![Screen](../img/tech/loadtest.png)

일반적으로 사용자는 서비스를 호출하고, 호출된 서비스는 자원을 사용합니다. 그래서 서로간의 관계가 중요합니다.

가상사용자가 무한이 증가하면 서비스 처리량은 포화지점에 가까워 집니다. 또한 서비스 응답 시간이 늘어나고, CPU 사용량은 100%를 향하게 됩니다.

당연한 일입니다. 하지만 종종 이런 원칙대로 되지 않는 경우가 있습니다.

예를 들어 사용자 수를 늘려도 응답이 느려지지 않으면 뭔가 잘못된 것입니다. 간단한 룰을 잊지 마세요.

CPU 사용량은 절대 100%를 넘지 않습니다.CPU usage never become over 100%. 
사용자 수에 관계없이 서비스 TPS(transaction per second)는 최대 TPS 이상 증가하지 않습니다, 조정작업이 없었다면.
만약 최대 TPS값이 늘지 않으면. 시스템이 조정되지 않습니다. 응답 시간에 속지 마십시오.
