![scouter](./scouter.document/img/main/scouter-logo-w200.png)

![Englsh](https://img.shields.io/badge/language-English-orange.svg) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](README_kr.md)

## Application Performance Monitoring for Open Source S/Ws.

SCOUTER is an open source APM like new relic and appdynamics.
(APM means application performance monitoring or application performance management.)

 - Monitoring target (currently)
   - Java application - Web applications (on Tomcat, JBoss, Resin...), Standalone java applications
   - OS - Linux, Windows, Unix

 - Monitoring target (to-be : hopeful with every contributor)
   - Nodejs, Redis, Apach HTTPD, nginX, php ...

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

## Documents
 - [Document Home](./scouter.document/index.md)
 - [Quick Start Guide (Quick Installation and Demo)](./scouter.document/main/Quick-Start.md)
 - [Live Demo(Try to use scouter by connecting on live demo system)](./scouter.document/main/Live-Demo.md)
 - [Client Screen Help](./scouter.document/client/How-To-Use-Client.md)

## Download
 - [Latest Release](https://github.com/scouter-project/scouter/releases/)

## Modules
Scouter has three modules:

- **Agent** : gather performance information and send  to the server
  - **Tomcat Agent (Java Agent)** : Performance for JVM & Tomcat
     - **ASM** :  using ASM library of OW2  (http://asm.ow2.org/) for BCI(byte code instrumentation)
     - **Tools.jar** : Java thread dumps, heap dumps, heap histograms, and the other powerful features provided by the JVM as the default.
     - **JMX** :  Some counters for Tomcat & JVM such as GC Count, GC Times etc
     
  - **Linux Agent (Host Agent)** : Performance for Linux, Windows and OSX
     - **Sigar Lib** (https://github.com/hyperic/sigar) : A cross-platform process and system utilities module for Java
     - **Counters** : Cpu, Memory, Disk, Network
     
  -  **MariaDB Agent** : [to be announced]
<br>

- **Server (Collector)** : received the data from agent and it stores the data. The data is serviced to clients.
  - **Scala** : Written in Scala. It will provide a variety of features and performance scalability that can not be written in Java.
  - **HASH FILE** : Server is using the hash index in order to store large data in files.
  - **GZIP** : Service performance data and profile data are stored is compressed.
<br>

- **Client (Viewer)** : provide the user interfaces
  - **Eclipse RCP** : SCOUTER is created as a standalone program. So the client can be connected to multiple servers at the same time. It makes easier that users monitor large scale systems.
  - **SWT & GEF4** : Charts and Diagrams
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

## License
Licensed under the Apache License, Version 2.0
<br>
<br>
<br>


