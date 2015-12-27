# What special in SOUTER
[![Englsh](https://img.shields.io/badge/language-English-red.svg)](What-special-in-SCOUTER.md) ![Korean](https://img.shields.io/badge/language-Korean-blue.svg)

SCOUTER는 무엇인가?
SCOUTER는 애플리케이션의 성능을 모니터링하고 분석할 수 있다. 
SCOUTER는 그만의 특징들이 있다. 

## 설치형 솔루션이다.
SCOUTER를 사용하기 위해서는 모니터링 대상 시스템과 같은 내부망에 수집서버를 설치해야한다.
그렇게 함으로 SCOUTER는 좀더 많은 데이터를 대상시스템으로 부터 수집한다. 
이점은 SaaS형 모니터링 솔루션과 크게 다른점이다. 

SaaS형은 쉽게 사용할 수 있는 반면 설치형은 보다 상세한 분석이 가능하다.

## SCOUTER는 독립클라이언트를 뷰어로 사용한다.(WEB 기반이 아니)
SCOUTER는 Eclipse RCP platform으로 만들어지 독립 클라이언트이다. 그래서 웹형 뷰어보다 많은 성능 데이터를 제어할 수 있다.

## SCOUTER 파일 DB에 성능 데이터를 저장한다.

SCOUTER wants to collect bigger data and analyze each service transaction(request).
so SCOUTER should control a lot of data. That’s why SCOUTER save service performance and profile data on compressed files.

## SCOUTER 타겟 시스템에 대한 개별요청을 추적한다.(XLOG)
Every service call is individually traced(profiled) and saved it.
It is possible with the compressed archiving and  standalone clients.

## SCOUTER는 진행중인 스택덤프를 분석한다.
Sometimes it is not clear to understand the performance problem in a separate thread information.
At that time,  we have to think about different way. If we collect full thread stacks in many times and  analyze the stacks together, we could get an another chance to solve the performance problem.
(coming soon)

