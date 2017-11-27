# SCOUTER가 특별한 이유
[![English](https://img.shields.io/badge/language-English-orange.svg)](What-special-in-SCOUTER.md) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](What-special-in-SCOUTER_kr.md)

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
Scouter는 빅 데이터 를 수집하고 각 서비스의 트랜잭션(요청)을 분석하기를 원합니다.
그래서 Scouter는 대량의 데이터를 제어 해야 합니다. Scouter가 압축된 파일 서비스 성능 및 프로파일 데이터 를 저장 하는 이유 입니다.

## 타겟 시스템에 대한 개별 요청을 추적
모든 서비스 콜은 개별적으로 추적(프로파일링)하고 그것을 저장 합니다.
이것은 압축된 아카이브 및 독립 클라이언트에서 가능합니다.

## Scouter 진행중인 스택덤프를 분석한다.
때로는 다른 스레드 정보에서 성능 문제를 이해하는 것은 분명 아닙니다.
그 때는 다른 방법을 생각해야 합니다. 여러 번 전체 스레드 스택을 수집하고 함께 스택을 분석 할 경우,
우리는 성능 문제를 해결 하기위한 또 다른 기회를 얻을 수 있습니다.(SFA)

