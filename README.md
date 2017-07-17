[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.scouter-project/scouter-parent/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.scouter-project/scouter-parent)
[![contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://github.com/scouter-project/scouter/issues)


![scouter](./scouter.document/img/main/scouter-logo-w200.png)

![Englsh](https://img.shields.io/badge/language-English-orange.svg) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](README_kr.md)

## Application Performance Monitoring for Open Source S/Ws.

SCOUTER is an open source APM like new relic and appdynamics.
(APM means application performance monitoring or application performance management.)

 - Monitoring target (currently)
   - Java application - Web applications (on Tomcat, JBoss, Resin...), Standalone java applications
   - OS - Linux, Windows, Unix

 - Monitoring target (to-be : contributing welcome)
   - Redis, Apach HTTPD, nginX, Nodejs ...

![Screen](./scouter.document/img/main/dashboard-sample-1.png)

Users use application services on a system and the services use resources on the system.
You should understand this context in order to manage the system performance efficiently.
SCOUTER can help you.

- SCOUTER monitoring :
  - Users : ActiveUser, RecentUser, Today Visitor
  - Services : ActiveService, TPS, ResponseTime, Transaction Profile(class,sql,apicall)
  - Resources : Cpu,Memory,Network and Heap usage, Connection pools etc.

## At a glance(Click to watch the video)
[![Demo gif](https://j.gifs.com/yDqbAa.gif)](https://youtu.be/iuArTzsD7Ws)

## Documents
 - [Document Home](./scouter.document/index.md)
 - [Quick Start Guide (Quick Installation and Demo)](./scouter.document/main/Quick-Start.md)
 - [Client Screen Help](./scouter.document/client/How-To-Use-Client.md)

## Download
 - [Latest Release](https://github.com/scouter-project/scouter/releases/)

## Modules
Scouter has three modules:

- **Agent** : gather performance information and send to the server
  - **Java Agent (JVM Agent)** : gathering profiles and performance metrics of JVM & Web application server(eg. Tomcat)...
  - **Host Agent (OS Agent)** : gathering performance metrics of Linux, Windows and OSX...
  - **MariaDB Agent** : [to be announced]
<br>

- **Server (Collector)** : save the performance metrics from agents. The data is streamed to clients.
<br>

- **Client (Viewer)** : client program based on RCP.
<br>

## Facebook
 - [Scouter APM : Facebook Scouter user group](https://www.facebook.com/groups/scouterapm/)

## How to contribute
 - **Notice** : Pull request to **develop branch** only allowed.
 - Refer to the development guide below.
   - [Scouter developer guide](./scouter.document/tech/Developer-Guide.md)
 - Please note that you will have to complete a [CLA](http://goo.gl/forms/xSmYs8qM9J) for your first pull-request.

## Q&A
 - [Google Groups](https://groups.google.com/forum/#!forum/scouter-project)

## Blogging & Posts
 - [Applying Scouter APM to my service : by Kingbbode](https://translate.google.co.kr/translate?hl=ko&sl=ko&tl=en&u=http%3A%2F%2Fkingbbode.tistory.com%2F12)
 - [Effective monitoring by Scouter : by TMON](https://translate.google.co.kr/translate?hl=ko&sl=ko&tl=en&u=http%3A%2F%2Fblog.naver.com%2FPostView.nhn%3FblogId%3Dtmondev%26logNo%3D220870505665)
 - [Opensource performance monitoring, Scouter configurations : by SUN](https://translate.google.co.kr/translate?hl=ko&sl=ko&tl=en&u=http%3A%2F%2Fwww.popit.kr%2Fscouter-open-source-apm-config%2F)


## License
Licensed under the Apache License, Version 2.0
<br>
<br>
<br>


