# What special in SOUTER
[![Englsh](https://img.shields.io/badge/language-English-red.svg)](What-special-in-SCOUTER.md) ![Korean](https://img.shields.io/badge/language-Korean-blue.svg)

Scouter 는 무엇인가?
Scouter 는 애플리케이션의 성능을 모니터링하고 분석할 수 있다. 
Scouter 는 그만의 특징들이 있다. 

## 설치형 솔루션
Scouter 를 사용하기 위해서 모니터링 대상 시스템과 같은 내부망에 수집 서버를 설치해야 한다.
이렇게 구성할 경우 SaaS 형 모니터링 솔루션에 비해 보다 상세한 데이터를 수집할 수 있다.
SaaS 형은 설치가 필요없기에 쉽게 사용할 수 있는 반면, Scouter 와 같은 설치형은 수집된 데이터가 보다 상세하기 때문에 분석이 효율적이다.

## 클라이언트를 뷰어
Scouter Client 는 Eclipse RCP Platform 으로 만들어지 독립 클라이언트이다. 그래서 웹형 뷰어보다 많은 성능 데이터를 제어할 수 있다.

## Scouter 파일 DB에 성능 데이터를 저장

Scouter wants to collect bigger data and analyze each service transaction(request).
so Scouter should control a lot of data. That’s why SCOUTER save service performance and profile data on compressed files.

## 타겟 시스템에 대한 개별 요청을 추적
Every service call is individually traced(profiled) and saved it.
It is possible with the compressed archiving and  standalone clients.

## Scouter 진행중인 스택덤프를 분석한다.
Sometimes it is not clear to understand the performance problem in a separate thread information.
At that time,  we have to think about different way. If we collect full thread stacks in many times and  analyze the stacks together, we could get an another chance to solve the performance problem.
(coming soon)

