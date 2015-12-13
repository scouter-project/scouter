![scouter](./scouter.document/img/main/scouter-logo-w200.png)

[![Englsh](https://img.shields.io/badge/language-English-red.svg)](README.md) ![Korean](https://img.shields.io/badge/language-Korean-blue.svg)

## 오픈소스 S/W 어플리케이션 성능 모니터링

APM은 Application performance montoring 또는 application performance management를 의미하고 SCOUTER는 오픈소스 APM 도구로서 Java, WAS에 대한 모니터링 및 DB Agent를 통해 오픈소스 DB 모니터링 기능을 제공한다.

 - 모니터링 대상 
   - 오픈소스 WAS - Tomcat, JBoss, Resin ...
   - 오픈소스 DB - MariaDB (클로즈 베타 테스트 진행중)

![Screen](./scouter.document/img/main/dashboard-sample-1.png)

Users request application services on a system.
The services use resources on the system.
You should understand this context in order to manage the system  performance effectively.
SCOUTER can help you.

- SCOUTER monitoring :
  - Users : ActiveUser, RecentUser, Today Visitor
  - Services : ActiveService, TPS, ResponseTime, Transaction Profile(class,sql,apicall), TagCount 
  - Resources : Cpu,Memory,Network and Heap usage, Connection pools etc.

## At a glance(Click to watch the video)
[![Demo gif](https://j.gifs.com/yDqbAa.gif)](https://youtu.be/iuArTzsD7Ws)

<iframe width="560" height="315" src="https://www.youtube.com/embed/iuArTzsD7Ws" frameborder="0" allowfullscreen></iframe>

## Documents
 - [Getting Started](../../wiki/Getting-Started)
 - [Getting Start Profiling SQL](../../wiki/Getting-Start-Profile-SQL) 
 - [Client Screen Help](../../wiki/How-To-Use-Client) 
 - [More Documents](../../wiki/)

## Download
 - [Latest Release](https://github.com/scouter-project/scouter/releases/)

## Modules
Scouter has three modules:

- Agent : gather performance information and send  to the server
  - Tomcat Agent (Java Agent) : Performance for JVM & Tomcat
     - **ASM** :  using ASM library of OW2  (http://asm.ow2.org/) for BCI(byte code instrumentation)
     - **Tools.jar** : Java thread dumps, heap dumps, heap histograms, and the other powerful features provided by the JVM as the default.
     - **JMX** :  Some counters for Tomcat & JVM such as GC Count, GC Times etc
  - Linux Agent (Host Agent) : Performance for Linux, Window and OSX
     - Sigar Lib (https://github.com/hyperic/sigar) : A cross-platform process and system utilities module for Java
     - Counters : Cpu, Memory, Disk, Network
  -  MariaDB Agent : [to be announced]

- Server (Collector) : received the data from agent and it stores the data. The data is serviced to clients.
  - Scala : Written in Scala. It will provide a variety of features and performance scalability that can not be written in Java.
  - HASH FILE : Server is using the hash index in order to store large data in files.
  - GZIP : Service performance data and profile data are stored is compressed.

- Client (Viewer) : provide the user interfaces
  - Eclipse RCP : SCOUTER is created as a standalone program. So the client can be connected to multiple servers at the same time. It makes easier that users monitor large scale systems.
  - SWT & GEF4 : Charts and Diagrams

## Q&A
 - [Google Groups](https://groups.google.com/forum/#!forum/scouter-project)

## Facebook
 - [Facebook Scouter user group](https://www.facebook.com/groups/1525329794448529/)

## License
Licensed under the Apache License, Version 2.0
<br>
<br>
<br>


