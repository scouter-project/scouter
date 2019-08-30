# Setup FAQ
[![English](https://img.shields.io/badge/language-English-orange.svg)](Setup-FAQ.md) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](Setup-FAQ_kr.md)

This is FAQ about installation step.

## How about Scouter's architecture?
Scouter has agent-server-client architecture. Scouter Agent collects monitoring data and sends it to Scouter Server. Scouter Server processes and accumulates received data and sends it to Scouter Client by response result. Scouter Client reads processed monitoring and performance data from Server, display them on the user's screen with various views and perspectives.
```
Agent[Linux]   ----\
Agent[Tomcat]  ------>  Server ——-> Client
Agent[MariaDB] ----/ 
```
Currently there are Tomcat, Linux, and MariaDB agents. In the future more agents for more opensource software would be developed and included.

## How to install Scouter Server, Agent, Client each to different machines?
Scouter's three SW use TCP and UDP for their communication method. With configuring IP addresses and ports along to your environment, you can install three SW independently.

### How to communicate between Agent and Server
Scouter Agent uses both TCP and UDP. Agent reports its data via UDP to Scouter Server periodically. And uses TCP when reply Server's special requests. 
```
     UDP 6100
Agent  ====> Server 
     TCP 6100
```
Agent is configured to use 127.0.0.1 for Server's IP address. To change this, modify java options on Agent program.

```
-Dsever_addr=192.168.0.1
-Dserver_udp_port=6100
-Dserver_tcp_port=6100
```
Also configurable to modify configuration properties file.
```
sever_addr=192.168.0.1
server_udp_port=6100
server_tcp_port=6100
```

If you want to another configuration file, uses -Dscouter.config=<file path> option.

### How to communicate between Server and Client
Scouter Client uses TCP 6100 port to request monitoring data, same as Agent does.
```
Client ——-> Server 
     TCP 6100
```
TCP 6100 port are used by both Agent and Client. User should specify Server's endpoint information when Client program run at first.

Server Address: 192.168.0.1:6100

## How to monitor multiple Tomcat instances
Scouter used logical names and hierarchy for its monitoring targets.

/host/tomcat/context 

If you want to watch multiple Tomcat instances, you should give unique name of each on the same hierarchy. 
Names can be given by java options like, 
```
-Dobj_name=MyTomcatInstName 
```
or on scouter.config file,
```
obj_name=MyTomcatInstName
```

## How to monitor Tomcat host CPU and Memory simultaneously
TBD


