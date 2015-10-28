## Open Source S/W Applicaiton Performance Monitoring

SCOUTER is an open source APM and a database monitoring tool.
APM means application performance monitoring or application performance management.
 - Monitoring target (currently)
   - Opensource WAS - Tomcat, JBoss, Resin ...  
   - Opensource DB - MariaDB(on closed beta testing now)

<img src="https://raw.githubusercontent.com/scouter-project/scouter/master/scouter.client/gallery/scouter-logo-full4_blue_transp_all.png" width="200" align="right">

 - Monitoring target (hopeful with every contributor)
   - Nodejs, Redis, MongoDB, PostgreSQL, Apach HTTPD, nginX, php ...

![Screen](https://github.com/scouter-project/scouter-help/blob/master/misc/screen/dash1.png)

Users request application services on a system. 
The services use resources on the system.
You should understand this context in order to manage the system  performance effectively.
SCOUTER can help you.

- SCOUTER monitoring :
  - Users : ActiveUser, RecentUser, Today Visitor
  - Services : ActiveService, TPS, ResponseTime, Transaction Profile(class,sql,apicall), TagCount 
  - Resources : Cpu,Memory,Network and Heap usage, Connection pools etc.

### At a glance(Click to watch the video)
[![Demo gif](https://j.gifs.com/yDqbAa.gif)](https://youtu.be/iuArTzsD7Ws)

<iframe width="560" height="315" src="https://www.youtube.com/embed/iuArTzsD7Ws" frameborder="0" allowfullscreen></iframe>

### Documents
 - [Getting Started](../../wiki/Getting-Started)
 - [Getting Start Profiling SQL](../../wiki/Getting-Start-Profile-SQL) 
 - [Client Screen Help](../../wiki/How-To-Use-Client) 
 - [More Documents](../../wiki/)

### Download
 - [Latest Release](https://github.com/scouter-project/scouter/releases/)

### Modules
Scouter has three modules:

- Agent : gather performance information and send  to the server
  - Tomcat Agent (Java Agent) : Performance for JVM & Tomcat 
     - **ASM** :  using ASM library of OW2  (http://asm.ow2.org/) for BCI(byte code instrumentation)
     - **Tools.jar** : Java thread dumps, heap dumps, heap histograms, and the other powerful features provided by the JVM as the default.
     - **JMX** :  Some counters for Tomcat & JVM such as GC Count, GC Times etc 
  - Linux Agent (Host Agent) : Performance for Linux, Window and OSX
     - Sigar Lib(https://github.com/hyperic/sigar) : A cross-platform process and system utilities module for Java
     - Counters : Cpu, Memory, Disk, Network
  -  MariaDB Agent : [to be announced]

- Server (Collector) : received the data from agent and it stores the data. The data is serviced to clients.
  - Scala : Written in Scala. It will provide a variety of features and performance scalability that can not be written in Java.
  - HASH FILE : Server is using the hash index in order to store large data in files.
  - GZIP : Service performance data and profile data are stored is compressed.

- Client (Viewer) : provide the user interfaces
  - Eclipse RCP : SCOUTER is created as a standalone program. So the client can be connected to multiple servers at the same time. It makes easier that users monitor large scale systems.
  - SWT & GEF4 : Charts and Diagrams

### Q&A
 - [Google Groups](https://groups.google.com/forum/#!forum/scouter-project)
 
### License
Licensed under the Apache License, Version 2.0
<br>
<br>
<br>


