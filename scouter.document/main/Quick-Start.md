# Quick Start
[![English](https://img.shields.io/badge/language-English-orange.svg)](Quick-Start.md) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](Quick-Start_kr.md)

This page provides a demo system where you can install the Scouter at once and try it out easily.

This quick Start proceeds in the following order.

> 1. **Download** integrated demonstration environment and a client program
> 2. Decompress the downloaded file. - **The installation is completed!**
> 3. Run the scouter server(Collector)
> 4. Run the scouter client(Viewer)
> 5. Run the host Agent - optional
> 6. Run the demonstration system (Tomcat with WAR)
> 7. **Browse the demonstration system(JPetStore) via a web browser**
> 8. Do a load test by apache jmeter

## Requirements
* JDK 7+ (& environment variable JAVA_HOME to be set)
* Windows / Linux / OS X

If No JDK, You have to download and install JDK first with the [oracle.com](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html).
If you have not set the environment variable JAVA_HOME, add to the OS env variables or modify running scripts that describe later on `Run demonstration system` section.

## architecture 
Modules           | desc
----------------- | --------------------------
Server(Collector) | processing data that agent transfer to
Host Agent        | transfer CPU, Memory, Disk performance information to the collector
Java Agent        | transfer real time performance data of the java program such as Heap Memory, Thread, Profiles to the collector
Client(Viewer)    | Client program to check the collected performance information

![scouter overview](../img/main/scouter-overview.png)

## Installation Scouter and run

#### (1) **Download** integrated demonstration environment and a client program
 - Download integrated demonstration environment
   - [Download demo-env1.tar.gz](https://github.com/scouter-project/scouter-demo/releases/download/v2.6.2/demo-env1.tar.gz) (It includes collector server, host agent, java agent, tomcat for demo system and start scripts...)
   - Windows Case : [Download demo-env1.zip](https://github.com/scouter-project/scouter-demo/releases/download/v2.6.2/demo-env1.zip)

 - Download a client program
   - download a client for your environment.
    - [scouter.client.product-win32.win32.x86_64.zip](https://github.com/scouter-project/scouter/releases/download/v2.6.2/scouter.client.product-win32.win32.x86_64.zip)
    - [scouter.client.product-win32.win32.x86.zip](https://github.com/scouter-project/scouter/releases/download/v2.6.2/scouter.client.product-win32.win32.x86.zip)
    - [scouter.client.product-macosx.cocoa.x86_64.tar.gz](https://github.com/scouter-project/scouter/releases/download/v2.6.2/scouter.client.product-macosx.cocoa.x86_64.tar.gz)
    - [scouter.client.product-linux.gtk.x86_64.tar.gz](https://github.com/scouter-project/scouter/releases/download/v2.6.2/scouter.client.product-linux.gtk.x86_64.tar.gz)

#### (2) Decompress the downloaded file.
 decompress file and all installation is done.
 decompress demo-env1.tar.gz to any directory you want to.
 
#### (3) Run the scouter server(Collector)
 run the command below at the directory you decompressed file.
 ```bash
 start-scouter-server.sh
 ```
 > Windows : 
 > ```bat
 > start-scouter-server.bat
 > ```

#### (4) Run the scouter client(Viewer)
 decompress the client file you downloaded.
 Click the scouter client execution file and run.  
* (**Important for Mac(OSX)**: run the command if you can not open it. `xattr -cr scouter.client.app`  
* scouter client needs java 11+
 
 On the login form, you can login with 127.0.0.1:6100 for collector server and admin/admin for default id/password.
 
![client login](../img/client/client-login.png)

#### (5) Run the host agent(Optional)
 Run the host agent for monitoring CPU, memory, IO and more of the OS.
 ```bash
 start-scouter-host.sh
 ```
 > Windows : 
 > ```bat
 > start-scouter-host.bat
 > ```

#### (6) Run the demonstration system (Tomcat with WAR)
 ```bash
 start-tomcat.sh
 ```

 > Windows 
 > ```bat
 > start-tomcat.bat
 > ```

If you met the error below, you may not set JAVA_HOME.
You can set JAVA_HOME on your os environment variable or modify some files - start-tomcat.bat and stop-tomcat.bat.(or .sh)
```
Neither the JAVA_HOME nor the JRE_HOME environment variable is defined
At least one of these environment variable is needed to run this program
```

for example, you can modify start-tomcat.bat and set JAVA_HOME for java installed directory like below on a Windows system. 
`set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_25`

```bat
@echo off
setlocal
set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_25
set originDir=%cd%
cd /D %~dp0
cd apache-tomcat-7.0.67\bin
startup.bat
cd /D %originDir%
```

#### (7) Browse the demonstration system via a web browser
Run a browser(chrome, safari...) and you can see the demo system while access http://127.0.0.1:8080/jpetstore.
And also you can see the web service request you triggered on the scouter client within 2 seconds.

![jpetstore main](../img/quickstart/jpet-main.png)
![jpetstore main](../img/quickstart/client-jpet-demo1.png)

#### (8) Do a load test by apache jmeter
You can make virtual load for verifying scouter's functionality.
run the command below, will load by jmeter in 5 minutes.
If you want to stop jmeter load, please press CTRL+C.

```bash
start-jmeter.sh
```
 > Windows
 > ```bat
 > start-jmeter.bat
 > ```

![jpetstore main](../img/quickstart/client-jmeter-demo1.png)

# Scouter client at a glance
This section will describe how the monitoring application via the scouter.

## 1. How to check running thread details
You can see running threads on Active Service View.
We can find why our system is so slow, why this request don't response, what is this thread doing now, what's slow thread, what's stucked thread by this view.
It display yellow color if the service is over than 3 seconds and red color 8 seconds.

![active service](../img/quickstart/active-service.png)


## 2. Service analysis by the XLog profiling data
The XLog graph shows completed request and more detail profiles when drag the area you want to see detail.

![active service](../img/quickstart/xlog1.png)
![active service](../img/quickstart/profile1.png)

## 3. Trace chained services ( Service architecture like SOA, MSA ... )
set `trace_interservice_enabled=true` on java agent configuration to enable HTTP inter-service trace.
You can see the profile below when you drag XLog and select `e2e.jsp`

![profile1](../img/quickstart/interservice1.png)

click the `call:..` on profile to get the another profile of callee service and click the `caller id` on top of profile summary to get back.

Also you can see service call tree diagram by click the `gxid` - from an user to database tables via services.
If click a table name again, used query on the table will be popped up.

![profile2](../img/quickstart/interservice2.png)

The flag (S/U/D) by a table name means Select, Update, Delete query each.

![profile2](../img/quickstart/topology-table.png)


## 4. Advanced features of scouter

Please read the detail manual pages for advanced features.

function     |    description
------------ | --------------
SFA (Stack Frequency Analyzer)  | the SFA gather thread stacks for a while, then analyze and make statistics for finding out inefficient codes that make system run slow.
[Trace connection Leak](../tech/JDBC-Connection-Leak-Trace.md)           | Trace Database Connection Leak
Add user id on a profile        | profile customizing with a plugin scripting feature.
[method level profiling](../use-case/Method-Profiling.md)          | how to profile deeper - method level profiling
[Trace Non-Servlet Java application](../use-case/NON-HTTP-Service-Trace.md)    | how to monitoring Non-Servlet Java application like socket deamons.

