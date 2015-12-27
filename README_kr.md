![scouter](./scouter.document/img/main/scouter-logo-w200.png)

[![Englsh](https://img.shields.io/badge/language-English-red.svg)](README.md) ![Korean](https://img.shields.io/badge/language-Korean-blue.svg)

## 오픈소스 S/W 어플리케이션 성능 모니터링

APM은 Application performance montoring 또는 application performance management를 의미하고 SCOUTER는 오픈소스 APM 도구로서 Java, WAS에 대한 모니터링 및 DB Agent를 통해 오픈소스 DB 모니터링 기능을 제공한다.

기업용 IT는 상용 서비스를 기반으로 진화하여 왔다. 그러나 이제는 점점 더 오픈 소스 기반의 시스템 구축이 늘어나고 있다. 오픈소스 기반의 시스템을 구축할때 성능 관리 또한 같이 고려 되어야하는데 오픈 소스인 Scouter는 가장 훌륭한 선택이 될 것이다.

 - 모니터링 대상 
   - 오픈소스 WAS - Tomcat, JBoss, Resin ...
   - 오픈소스 DB - MariaDB (클로즈 베타 테스트 진행중)

![Screen](./scouter.document/img/main/dashboard-sample-1.png)

사용자는 시스템에 서비스 요청을 보내고, 이를 통해 서비스는 시스템의 자원을 사용하게 된다.
시스템 성능을 잘 이해하고 관리하기 위해서는 사용자와 서비스, 자원간의 관계를 이해하고 접근하는 것이 중요하며 SCOUTER를 활용하여 보다 쉽게 이에 대한 접근이 가능하다.

- SCOUTER의 주요 모니터링 항목 :
  - 사용자 : ActiveUser, RecentUser, Today Visitor 등
  - 서비스 : ActiveService, TPS, ResponseTime, Transaction Profile(class,sql,apicall), TagCount 등
  - 자원 : Cpu,Memory,Network and Heap usage, Connection pools 등.

## 소개 동영상(클릭)
[![Demo gif](https://j.gifs.com/yDqbAa.gif)](https://youtu.be/iuArTzsD7Ws)

<iframe width="560" height="315" src="https://www.youtube.com/embed/iuArTzsD7Ws" frameborder="0" allowfullscreen></iframe>

## Documents
 - [Document Home](./scouter.document/index_kr.md)
 - [Quick Start(Scouter Demo 설치)](./scouter.document/main/Quick-Start_kr.md)
 - [Client 화면 설명](./scouter.document/client/How-To-Use-Client_kr.md)

## Download
 - [최신 Release](https://github.com/scouter-project/scouter/releases/)

## 모듈
스카우터는 세가지 주요 모듈로 구성된다 :

- **Agent** : 성능 데이터를 수집하여 수집 서버로 전송
  - **Tomcat Agent (Java Agent)** : JVM 과 Tomcat WAS 성능 수집
     - **ASM** :  using ASM library of OW2  (http://asm.ow2.org/) for BCI(byte code instrumentation)
     - **Tools.jar** : Java thread dumps, heap dumps, heap histograms, and the other powerful features provided by the JVM as the default.
     - **JMX** :  Some counters for Tomcat & JVM such as GC Count, GC Times etc
     
  - **Linux Agent (Host Agent)** : Linux, Windows 및 OSX 성능
     - **Sigar Lib** (https://github.com/hyperic/sigar) : A cross-platform process and system utilities module for Java
     - **Counters** : Cpu, Memory, Disk, Network
     
  -  **MariaDB Agent** : [to be announced]
<br>

- **Server (Collector)** : Agent가 전송한 데이터를 저장하고 Client 요청시 Client에게 적절한 데이터를 전송
  - **Scala** : Written in Scala. It will provide a variety of features and performance scalability that can not be written in Java.
  - **HASH FILE** : 고속의 자체 개발한 Hash 인덱스 방식의 파일 Repository 사용으로 최상의 속도를 동작하며 추가적인 DB 및 라이브러리의 설치가 불필요하여 압축 해제만으로 쉽게 설치 가능.
  - **GZIP** : 압축 옵션을 통해 저장 공간을 절약하도록 개발됨.
<br>

- **Client (Viewer)** : 수집된 데이터를 보기위한 Client 프로그램
  - **Eclipse RCP** : RCP로 개발된 Standalone 프로그램. C/S의 빠른 응답성 및 OS 호환성을 고려하여 RCP로 개발되었으며 하나의 Client View 에서 복수의(Multi) Collector Server를 한번에 모니터링이 가능함. 이를 통해 대형 시스템이나 지속적으로 확장되는 시스템에 대해서도 쉽게 모니터링이 가능하도록 개발됨.
  - **SWT & GEF4** : Charts and Diagrams
<br>

## Q&A
 - [Google Groups](https://groups.google.com/forum/#!forum/scouter-project)

## Facebook
 - [Facebook Scouter user group](https://www.facebook.com/groups/1525329794448529/)

## License
Licensed under the Apache License, Version 2.0
<br>
<br>
<br>


