# Setup
![Englsh](https://img.shields.io/badge/language-English-red.svg) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](Setup_kr.md)

Scouter do not require any installation except java.

***


## Server

### Prerequisite
* JDK 1.6+

### Start Server
1. Download scouter-yyyyMMdd.tar.gz - [Release Page](https://github.com/scouter-project/scouter/releases)
2. Extract /server directory and copy ${SCOUTER_SERVER_DIR}
3. Execute `startup.sh`(You can modify JVM Options in this file)
4. Done

```
  ____                  _
 / ___|  ___ ___  _   _| |_ ___ _ __
 \___ \ / __/   \| | | | __/ _ \ '__|
  ___) | (_| (+) | |_| | ||  __/ |
 |____/ \___\___/ \__,_|\__\___|_|
 Scouter version 0.0.1 20150602
 Open Source Performance Monitoring

System JRE version : 1.7.0_51
```
### Network Port
These ports can be changed in configuration file.
* UDP Receive Port : 6100
* TCP Service Port : 6100

### Configuration

#### Configuration file
You can configure server options by Adding or modifying(if exist) `server/conf/scouter.conf`
>If no options, all default value will be applied.

##### Example
*conf/scouter.conf*
```
# Agent Control and Service Port(Default : TCP 6100)
net_tcp_listen_port=6100

# UDP Receive Port(Default : 6100)
net_udp_listen_port=6100

# DB directory(Default : ./database)
db_dir=./database

# Log directory(Default : ./logs)
log_dir=./logs
```
For more options, refer Options page(Coming soon).
***


## Tomcat Agent
### Prerequisite
* JDK 1.5+(Required), 1.6+(Recommended)
* Tomcat 6 or 7

### Start Tomcat with Scouter agent
1. Download scouter-yyyyMMdd.tar.gz - [Release Page](https://github.com/scouter-project/scouter/releases)
2. Extract /agent/scouter.agent.jar and copy this file to ${SCOUTER_AGENT_DIR}
3. Append Tomcat java options. Refer [Java Options](#java-options)

4. ${TOMCAT_DIR}/bin/startup.sh
5. Done - You can see Scouter logo in catalina log.

### Java Options
Append below options in *${TOMCAT_DIR}/bin/catalina.sh or startup.sh*
```
JAVA_OPTS=" ${JAVA_OPTS} -javaagent:${SCOUTER_AGENT_DIR}/scouter.agent.jar"
JAVA_OPTS=" ${JAVA_OPTS} -Dscouter.config=${SCOUTER_AGENT_DIR}/scouter.conf"
```

### Configuration

#### Configuration file
You can configure agent options by modifying `${SCOUTER_AGENT_DIR}/scouter.conf`
>If no options, all default value will be applied.

##### Example
*${appropriate_directory}/scouter.conf*
```
# Scouter Server IP Address (Default : 127.0.0.1)
net_collector_ip=127.0.0.1

# Scouter Server Port (Default : 6100)
net_collector_udp_port=6100
net_collector_tcp_port=6100

# Scouter Name(Default : tomcat1)
obj_name=tomcat1
```
For more options, refer Options page(Coming soon).
***

## Client
### Prerequisite
* JRE 1.6+

### Start Client
1. Download suitable one of `scouter.client.product` - [Release Page](https://github.com/scouter-project/scouter/releases)
2. Extract all files to new directory
3. Execute scouter.exe(windows), scouter.app(osx) or scouter(linux)
4. You will see a dialog
5. Fill the authentication info and press OK button(If server is running)
6. The real-time chart will be shown.

**Example authentication info**
```
Server Address : 127.0.0.1:6100
ID : admin
password : admin
```