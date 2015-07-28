## Open Source S/W Performance Monitoring
```
  ____                  _            
 / ___|  ___ ___  _   _| |_ ___ _ __ 
 \___ \ / __/   \| | | | __/ _ \ '__|
  ___) | (_| (+) | |_| | ||  __/ |   
 |____/ \___\___/ \__,_|\__\___|_|                                      
 Scouter version 0.0.1 20150601
 Open Source S/W Performance Monitoring 
```
SCOUTER is a Open Source APM and a database monitoring tool. It is monitoring the performance of Tomcat and  MariaDB. 
Enterprise IT has evolved based on commercial services. 
But now more and more systems are using open source S/W. 
If you use a Tomcat, SCOUTER will be the best choice to monitor it.

### Documents
 - [Getting Started](./Getting-Started)
 - [Getting Start Profiling SQL](./Getting-Start-Profile-SQL) 
 - [Client Screen Help](./How-To-Use-Client) 
 - [More Documents](../../wiki/)

### Download
 - [Latest Release](https://github.com/scouter-project/scouter/releases/)

### Modules
Scouter has three modules:

- Agent : gather performance information and send  to the server
  - Tomcat Agent : Performance for JVM & Tomcat 
     - **ASM** :  using ASM library of OW2  (http://asm.ow2.org/) for BCI(byte code instrumentation)
     - **Tools.jar** : Java thread dumps, heap dumps, heap histograms, and the other powerful features provided by the JVM as the default.
     - **JMX** :  Some counters for Tomcat & JVM such as GC Count, GC Times etc 
  - Linux Agent : Performance for Linux, Window and OSX
     - PSUtil(https://github.com/giampaolo/psutil) : A cross-platform process and system utilities module for Python
     - Counters : Cpu, Memory, Disk, Network
  -  MariaDB Agent : [to be announced]

- Server : received the data from agent and it stores the data. The data is serviced to clients.
  - Scala : Written in Scala. It will provide a variety of features and performance scalability that can not be written in Java.
  - HASH FILE : Server is using the hash index in order to store large data in files.
  - GZIP : Service performance data and profile data are stored is compressed.

- Client : provide the user interfaces
  - Eclipse RCP : SCOUTER is created as a standalone program. So the client can be connected to multiple servers at the same time. It makes easier that users monitor large scale systems.
  - SWT & GEF4 : Charts and Diagrams
  
SCOUTER is just simple. This configuration is simple. It will quickly provide the functions of monitoring OSS.
- It will be deployed and upgraded through the Internet.
- Clients are avaliable on Windows, MAC OSX and Linux.
- It will also be automatically upgraded.
- Key features such as Response-distribution, Active-service, Tag-count analysis are provided.
### License
Licensed under the Apache License, Version 2.0


