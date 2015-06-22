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
SCOUTER is a performance monitoring tool for Open Source S/W such as Tomcat, MariaDB and Node.js
SCOUTER is going to be made in order to monitor open source Web middlewares or databases.

Enterprise IT has evolved based on commercial services. 
But now more and more systems are using open source S/W. 
If you use a Tomcat, SCOUTER will be the best choice to monitor it.

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
  - SCALA : developed by Scala. It will provide a variety of features and performance scalability that can not be written in Java.
  - HASH FILE : Server is using the hash index in order to store large data in files.
  - GZIP : Service performance data and profile data are stored is compressed.

- Client : provide the user interfaces
  - Eclipse RCP : SCOUTER is created as a standalone program. So the client can be connected to multiple servers at the same time. It makes easier that users monitor large scale systems.
  - SWT & GEF4 : Charts and Diagrams

### To-be
SCOUTER is just simple. This configuration is simple.It will quickly provide the functions of monitoring OSS.
- It will be deployed and upgraded through the Internet.
- Clients are avaliable on Windows, MAC OSX and Linux.
- It will also be automatically upgraded.
- Key features such as Response-distribution, Active-service, Tag-count analysis are provided to  users.


### Documents
 - [Getting Started](../../wiki/Getting-Started) wiki page.

### License
Licensed under the Apache License, Version 2.0

### Using Components & Icons
- This product includes GeoLite data created by MaxMind, available from
http://www.maxmind.com

- Download MaxMind GeoIP Data :  http://geolite.maxmind.com/download/geoip/database/GeoLiteCity.dat.gz

- Icons from : 
http://www.famfamfam.com/lab/icons/silk/
