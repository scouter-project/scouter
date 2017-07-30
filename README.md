[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.scouter-project/scouter-parent/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.scouter-project/scouter-parent)
[![contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://github.com/scouter-project/scouter/issues)


![scouter](./scouter.document/img/main/scouter-logo-w200.png)

![Englsh](https://img.shields.io/badge/language-English-orange.svg) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](README_kr.md)

## Application Performance Monitoring for Open Source S/Ws.
SCOUTER is an open source APM like new relic and appdynamics.
(APM means application performance monitoring or application performance management.)

 - **Monitoring target (currently)**
   - Java application - Web applications (on Tomcat, JBoss, Resin...), Standalone java applications
   - OS - Linux, Windows, Unix

 - **Monitoring target (to-be : contributing welcome)**
   - Redis, Apach HTTPD, nginX, Nodejs ...

![Screen](./scouter.document/img/main/dashboard-sample-1.png)

Users use application services on a system and the services use resources on the system.
You should understand this context in order to manage the system performance efficiently.
SCOUTER can help you.

- **SCOUTER shows**
  - Metrics about users : Active user, Recently used user, Today visitor
  - Metrics about services : Active service, TPS, Response time, Application profiles(method profile, sql profile, external call profile...)
  - Metrics about resources : Cpu, Memory, Network and Heap usage, Connection pools etc.

## At a glance(Click to watch the video)
[![Demo gif](https://j.gifs.com/yDqbAa.gif)](https://youtu.be/iuArTzsD7Ws)

## Documents
 - [Document Home](./scouter.document/index.md)
 - [Quick Start Guide (Quick Installation and Demo)](./scouter.document/main/Quick-Start.md)
 - [Client Screen Help](./scouter.document/client/How-To-Use-Client.md)

## Download
 - [Latest Release](https://github.com/scouter-project/scouter/releases/)

## Modules
### Official modules:

- **Agent** : gather performance information and send to the server
  - **Java Agent (JVM Agent)** : gathering profiles and performance metrics of JVM & Web application server(eg. Tomcat)...
  - **Host Agent (OS Agent)** : gathering performance metrics of Linux, Windows and OSX...
  - **MariaDB Agent** : [to be announced]

- **Server (Collector)** : save the performance metrics from agents. The data is streamed to clients.

- **Client (Viewer)** : client program based on RCP.

### 3rd-party Agent
- **Pulse type agent** : [scouter-pulse-library](https://github.com/scouter-project/scouter-pulse)
  - **[aws-monitor](https://github.com/nices96/scouter-pulse-aws-monitor)** : gathering performance metrics of EC2, RDS, ELB from cloudwatch in AWS.

### Plugins
- **Server plugin**
  - **Sample**
    - **[scouter-plugin-server-null](https://github.com/scouter-project/scouter-plugin-server-null)** : sample plugin prints out data collected
  
  - **Alert**
    - **[scouter-plugin-server-email](https://github.com/scouter-project/scouter-plugin-server-alert-email)** : emails alters from Scouter
    - **[scouter-plugin-server-telegram](https://github.com/scouter-project/scouter-plugin-server-alert-telegram)** : transfer altert from Scouter to telegram
    - **[scouter-plugin-server-slack](https://github.com/scouter-project/scouter-plugin-server-alert-slack)** : transfer altert from Scouter to slack
    - **[scouter-plugin-server-line](https://github.com/scouter-project/scouter-plugin-server-alert-line)** : transfer altert from Scouter to line
    - **[scouter-plugin-server-dingtalk](https://github.com/scouter-project/scouter-plugin-server-alert-dingtalk)** : transfer altert from Scouter to dingtalk
    
  - **Counter** 
    - **[scouter-plugin-server-influxdb](https://github.com/scouter-project/scouter-plugin-server-influxdb)** : transfer performance data from Scouter to influxDB(time series DB)  

- **Agent plugin**
  - TBD
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
 - [Scouter series #1 - Installation](http://gunsdevlog.blogspot.kr/2017/07/scouter-apm-1.html)
 - [Scouter series #2 - basic monitoring(1/2)](http://gunsdevlog.blogspot.kr/2017/07/scouter-apm-2-12.html)
 - [Scouter series #3 - basic monitoring(2/2)](http://gunsdevlog.blogspot.kr/2017/07/scouter-apm-basic-monitoring-2.html)
 - [Applying Scouter APM to my service : by Kingbbode](http://kingbbode.tistory.com/12)
 - [Effective monitoring by Scouter : by TMON](http://blog.naver.com/PostView.nhn?blogId=tmondev&logNo=220870505665)
 - [Opensource performance monitoring, Scouter configurations : by SUN](http://www.popit.kr/scouter-open-source-apm-config/)
 - [Scouter, InfluxDB, Grafana](https://gunleeblog.wordpress.com/2016/04/01/open-source-apm-scouter-influxdb-grafana-%EC%97%B0%EB%8F%99-step-by-step/)
 - [Build my own agents by scouter pulse](https://gunleeblog.wordpress.com/2016/09/07/scouter-pulse%EB%A5%BC-%EC%9D%B4%EC%9A%A9%ED%95%98%EC%97%AC-%EB%82%98%EB%A7%8C%EC%9D%98-agent-%EB%A7%8C%EB%93%A4%EA%B8%B0/)
 
<br>
 
## License
Licensed under the Apache License, Version 2.0
<br>


