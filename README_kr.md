[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.scouter-project/scouter-parent/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.scouter-project/scouter-parent)
[![contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://github.com/scouter-project/scouter/issues)

 
![scouter](./scouter.document/img/main/scouter-logo-w200.png)

[![Englsh](https://img.shields.io/badge/language-English-orange.svg)](README.md) ![Korean](https://img.shields.io/badge/language-Korean-blue.svg)

## 어플리케이션 성능 모니터링

오픈소스 APM인 Scouter는 JVM(WAS, Standalone application)을 사용하는 어플리케이션 및 OS 자원에 대한 모니터링 모니터링 기능을 제공한다.
 - **APM** : Application performance montoring / application performance management
 - 모니터링 대상 (현재)
   - Java application - Web application (on Tomcat, JBoss, Resin ...), Standalone java application
   - OS - LInux, Windows, Unix
 - 모니터링 대상 (TOBE)
   - Redis, Apach HTTPD, nginX, Nodejs...


![Screen](./scouter.document/img/main/dashboard-sample-1.png)

사용자는 시스템에 서비스 요청을 보내고, 이를 통해 서비스는 시스템의 자원을 사용하게 된다.
시스템 성능을 잘 이해하고 관리하기 위해서는 사용자와 서비스, 자원간의 관계를 이해하고 접근하는 것이 중요하며 SCOUTER를 활용하여 보다 쉽게 이에 대한 접근이 가능하다.

- SCOUTER의 주요 모니터링 항목 :
  - 사용자 : Active User, Recent User, Today Visitor 등
  - 서비스 : Active Service, TPS, Response Time, Transaction Profile(class,sql,apicall) 등
  - 자원 : Cpu, Memory, Network and Heap usage, Connection pool 등.

## 소개 동영상(클릭)
[![Demo gif](https://j.gifs.com/yDqbAa.gif)](https://youtu.be/iuArTzsD7Ws)

## Documents
 - [Document Home](./scouter.document/index_kr.md)
 - [Quick Start(Scouter Demo 설치)](./scouter.document/main/Quick-Start_kr.md)
 - [Client 화면 설명](./scouter.document/client/How-To-Use-Client_kr.md)

## Download
 - [최신 Release](https://github.com/scouter-project/scouter/releases/)

## 모듈
### 스카우터는 세가지 주요 모듈로 구성된다 :
- **Agent** : 성능 데이터를 수집하여 수집 서버로 전송
  - **Tomcat Agent (Java Agent)** : JVM 과 Tomcat WAS 성능 수집
  - **Host Agent (OS Agent)** : Linux, Windows 및 OSX 성능
  -  **MariaDB Agent** : [to be announced]
- **Server (Collector)** : Agent가 전송한 데이터를 저장하고 Client 요청시 Client에게 적절한 데이터를 전송
- **Client (Viewer)** : 수집된 데이터를 보기 위한 RCP 기반 Client 프로그램


## Facebook
 - [Scouter APM 사용자 모임 - Facebook 그룹](https://www.facebook.com/groups/scouterapm/)

## Scouter에 기여하기
 - **Pull request**는 반드시 **develop branch**로 요청하여야 합니다.
 - 상세한 내용은 개발자 가이드를 참조하시기 바랍니다.
   - [Scouter 개발자 가이드](./scouter.document/tech/Developer-Guide_kr.md)
 - 최초 Pull-Request시 다음 [CLA](http://goo.gl/forms/xSmYs8qM9J)(Contributor License Agreement)에 서명하여 제출하여야 합니다.

## Q&A
 - [Google Groups](https://groups.google.com/forum/#!forum/scouter-project)

## Blogging & Posts
 - [내 서비스에 Scouter APM을 적용해보기](http://kingbbode.tistory.com/12)
 - [배치 모니터링, Scouter로 편하고 효율적으로! by TMON](http://blog.naver.com/PostView.nhn?blogId=tmondev&logNo=220870505665)
 - [오픈소스 성능 모니터링 도구 Scouter 설정 by SUN](http://www.popit.kr/scouter-open-source-apm-config/)

## License
Licensed under the Apache License, Version 2.0
<br>

