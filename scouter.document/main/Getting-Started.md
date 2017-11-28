# Getting Started
[![English](https://img.shields.io/badge/language-English-orange.svg)](Getting-Started.md) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](Getting-Started_kr.md)

Scouter is a tool to monitor the performance Tomcat, MariaDB, Linux of the OSS.
First, a description will be given to simple installation and basic usage for those who are new to Scouter.

## To set the monitoring target
Scouter will make a basic environment for monitors is. Install the JDK and Tomcat.
Windows 32 OS environment is assumed. If you use something other OS modifications taking into account the common path and start a shell.
If you're familiar with Tomcat installation you may skip this information.

### Install JDK
Go to oracle.com should download and install the JDK7.
```
jdk-7u79-windows-i586.exe
jdk-7u79-linux-x64.tar.gz(Linux 64)
Ref)http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html
```
The rest is subject to common JDK installation guide.

Caution) installation directory, but you do not need to install this version of JRE, JDK version.

### Install Tomcat
Install Tomcat is compatible with JDK7. If not the registration service and install it possible to install the same version as the Tomcat7 version on Linux or Windows
```
apache-tomcat-7.0.62.zip
http://tomcat.apache.org/download-70.cgi
```
Tomcat installation can be completed simply by dragging and unzip the specified directory.

"C: \ Tomcat7" assumes that Tomcat is installed. If you install to a different directory and modify accordingly


### Running
Try to run the Tomcat access to the browser. See the Apache documentation for details.

http://tomcat.apache.org/tomcat-7.0-doc/index.html

Go to "$ {TOMCAT_HOME} / bin" has "startup.bat / startup.sh". When you run this shell Tomcat is started.

JAVA_HOME is not set correctly, the following error occurs.
```
c:\Tomcat7\bin>startup.bat
Neither the JAVA_HOME nor the JRE_HOME environment variable is defined
At least one of these environment variable is needed to run this program
```
c:\Tomcat7\bin>set JAVA_HOME=c:\java7

If you run: successfully installed and then "\ Tomcat7 \ bin \ startup.bat c"

```
6월 22, 2015 2:11:27 오후 org.apache.catalina.startup.HostConfig deployDirectory

정보: Deployment of web application directory C:\Tomcat7\webapps\ROOT has finish
ed in 94 ms
6월 22, 2015 2:11:27 오후 org.apache.coyote.AbstractProtocol start
정보: Starting ProtocolHandler ["http-bio-8080"]
6월 22, 2015 2:11:27 오후 org.apache.coyote.AbstractProtocol start
정보: Starting ProtocolHandler ["ajp-bio-8009"]
6월 22, 2015 2:11:27 오후 org.apache.catalina.startup.Catalina start
정보: Server startup in 1251 ms
```
Above, you can see the same message. 

When using a browser to access the 'http://127.0.0.1:8080' screen output will Tomcat installation is complete.

![Tomcat](../img/main/getting_started_tomcat.png)

## Installing the Scouter
For the Tomcat monitoring 'scouter' is needed to install the three components
In this example, install all the components on a server

### Agent installed and running
It downloads the agent component. 

https://github.com/scouter-project/scouter/releases/download/v0.4.0/scouter.agent.tar.gz

'c:\' If unzip the position, 'c:\scouter\agent.java\scouter.agent.jar' created a file named.

Create a start-up shell.
C:\Tomcat7\bin\scouter.bat

The contents of the file are as follows.
```
set JAVA_HOME=c:\java7
set JAVA_OPTS=-javaagent:c:\scouter\agent.java\scouter.agent.jar
startup.bat
```
If you run Tomcat, again, from the Start window, you can see the message below.
```
  ____                  _
 / ___|  ___ ___  _   _| |_ ___ _ __
 \___ \ / __/   \| | | | __/ _ \ '__|
  ___) | (_| (+) | |_| | ||  __/ |
 |____/ \___\___/ \__,_|\__\___|_|
 Scouter version 0.4.0 20151213
 Open Source S/W Performance Monitoring

20150622 14:31:53 [SCOUTER] loaded by system classloader
20150622 14:31:57 [SCOUTER] jar:file:/C:/scouter/agent.java/scouter.agent.jar
20150622 14:31:57 [SCOUTER] Version 0.4.0 20151213
```
If you see the above message, the installation of Agent will succeed


## Collector Server Installation and running
Install the server receives the data store. First, download the server receives from github.

https://github.com/scouter-project/scouter/releases/download/v0.4.0/scouter.server.tar.gz

As the agent, 'c:\' If unzip the position "c:\scouter\server\*.*" created directories and files.

RUN "C:\scouter\server\startup.bat"

```
C:\scouter\server>java -Xmx512m -classpath ./boot.jar scouter.boot.Boot ./lib
  ____                  _
 / ___|  ___ ___  _   _| |_ ___ _ __
 \___ \ / __/   \| | | | __/ _ \ '__|
  ___) | (_| (+) | |_| | ||  __/ |
 |____/ \___\___/ \__,_|\__\___|_|
 Scouter version 0.4.0 20151213
 Open Source S/W Performance Monitoring

System JRE version : 1.7.0_67

```

As it can be seen above the running state.

## Client Installation and running
The client is developed with Eclipse RCP. Therefore, when you download and install the appropriate version for each OS.

Here, downloads the files for Windows 32bit

https://github.com/scouter-project/scouter/releases/download/v0.4.0/scouter.client.product-win32.win32.x86.zip

The file "c: \ scouter \" After unpacking copy it to: Run the "c \ scouter \ scouter.client \ scouter.exe".

Login window appears when you run. The input into the input field as follows:
```
Server Address : 127.0.0.1:6100
ID : admin
Password : admin
```

![Scouter](../img/main/getting_started_scouter.png)

## Finish
Turning now to some service requests from a browser on Tomcat can be seen that the result is output to the display Dot.
To view each Dot is one containing the performance information of the service call (transaction), and detailed information
If you drag the screen with the left mouse button, the rectangular box appears, select the Dot to be analyzed using the box.