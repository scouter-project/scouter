# Setup
[![English](https://img.shields.io/badge/language-English-orange.svg)](Setup.md) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](Setup_kr.md)

- Outgoing Links
  - Blogging
    - [Scouter series #1 - Installation](https://translate.google.co.kr/translate?hl=ko&sl=ko&tl=en&u=https%3A%2F%2Fgunsdevlog.blogspot.kr%2F2017%2F07%2Fscouter-apm-1.html)

***

## 1. Collector Server Installation

### 1.1. Prerequisite
* JDK 1.8+ : scouter server

### 1.2. Collector Server Installation
1. download the latest version of scouter-yyyyMMdd.tar.gz.
 - [Release Page](https://github.com/scouter-project/scouter/releases)
2. Extract the file.(You can see the directory ./scouter/server)
3. execute start script.
```bash
cd ./scouter/server
./startup.sh
# Windows : use startup.bat file
```
```bash
  ____                  _
 / ___|  ___ ___  _   _| |_ ___ _ __
 \___ \ / __/   \| | | | __/ _ \ '__|
  ___) | (_| (+) | |_| | ||  __/ |
 |____/ \___\___/ \__,_|\__\___|_|
 Scouter version 0.0.1 ${date}
 Open Source Performance Monitoring
 System JRE version : 1.8.0_175
```

### 1.3. Network ports used by Collector server
* UDP Receive Port : 6100 (This port is used for gathering performance metrics.)
* TCP Service Port : 6100 (This port is used for communication with scouter client and agents.)

### 1.4. Configuration

#### 1.4.1. Configuration file
 * configuration file location.(default)
   * `server/conf/scouter.conf`

##### 1.4.2. Example

```properties
# Agent Control and Service Port(Default : TCP 6100)
net_tcp_listen_port=6100

# UDP Receive Port(Default : 6100)
net_udp_listen_port=6100

# DB directory(Default : ./database)
db_dir=./database

# Log directory(Default : ./logs)
log_dir=./logs
```
All options and default values are available from the scouter client's **Collector >  Configure** menu.

***

## 2. Host Agent
### 2.1. Prerequisite
* JDK 1.6+

### 2.2. Download and start Host agent
1. Download scouter-yyyyMMdd.tar.gz 
 - [Release Page](https://github.com/scouter-project/scouter/releases)
2. Extract the file.(you can see the directory ./scouter/agent.host for running host monitoring agent.)
3. Run it. (In some cases, root permission may be required.)

```bash
cd ./scouter/agent.host
./host.sh
```

All options and default values are available from the scouter client's **Host >  Configure** menu.

***

## 3. Tomcat Agent
### 3.1. Prerequisite
* JDK 1.6+

### 3.2. Start Tomcat with Scouter agent
1. Download scouter-yyyyMMdd.tar.gz 
 - [Release Page](https://github.com/scouter-project/scouter/releases)
2. Extract the file.(you can see scouter.agent.jar file on the directory ./scouter/agent.java)
3. Add -javaagent JVM options on your java application.
  (Refer to java option example below.)

#### 3.2.1 Java Option example
Append below options in **${TOMCAT_DIR}/bin/catalina.sh or startup.sh**
```bash
JAVA_OPTS=" ${JAVA_OPTS} -javaagent:${SCOUTER_AGENT_DIR}/scouter.agent.jar"
JAVA_OPTS=" ${JAVA_OPTS} -Dscouter.config=${SCOUTER_AGENT_DIR}/conf/scouter1.conf"
JAVA_OPTS=" ${JAVA_OPTS} -Dobj_name=myFirstTomcat1"
```
* **${SCOUTER_AGENT_DIR}** means the directory that contains scouter.agent.jar file.
* **If you are using multiple Tomcat instances in one VM, you must define their respective configuration files.**
  * You can specify the conf file through the -Dscouter.config environment variable as in the example above.
  * Also, in this case, you should specify the name through the obj_name option so that the name of the monitored object is not duplicated in one VM.
  
#### 3.2.2 Java Option example ( Windows Service Option )
Append below java options in **${TOMCAT_DIR}/bin/tomcat${version}w.exe**
```bash
-javaagent:${SCOUTER_INSTALL_DIR}/scouter.agent.jar"
-Dscouter.config=${SCOUTER_INSTALL_DIR}/conf/scouter1.conf"
-Dobj_name=myFirstTomcat1"
```
* **${SCOUTER_INSTALL_DIR}** means the directory that contains scouter.agent.jar file.
* **If you run Tomcat through a Windows service, you need to add it to the tomcat ${version}w.exe option.**
  * This Option tomcat${version}w.exe ( ex)tomcat9w.exe ) > Java > Java Options. 
  
  
### 3.3. Configuration

#### 3.3.1. Configuration example
```
# Scouter Server IP Address (Default : 127.0.0.1)
net_collector_ip=127.0.0.1

# Scouter Server Port (Default : 6100)
net_collector_udp_port=6100
net_collector_tcp_port=6100

# Scouter Name(Default : tomcat1)
obj_name=myFirstTomcat1
```
All options and default values are available from the scouter client's **Object >  Configure** menu.

***

## 4. Client
### 4.1. Prerequisite
* JRE 1.8+

### Start Client
1. Download suitable one of `scouter.client.product-${os}[.tar.gz|.zip]` 
 - [Release Page](https://github.com/scouter-project/scouter/releases)
2. Extract all files to new directory
3. Execute scouter.exe(windows), scouter.app(osx) or scouter(linux)  
  **Important:** run the command if can not run the file. `xattr -cr scouter.client.app`  
  scouter client needs java 11+
5. You will see a dialog
6. Fill the authentication info(default : admin/admin) and press OK button
  (First, the scouter server must be running.)
6. The real-time chart will be shown.

***

## 5. Web API (설치 및 설정)

* [Web API Guide](../tech/Web-API-Guide.md)
