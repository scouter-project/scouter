# Setup
![Englsh](https://img.shields.io/badge/language-English-orange.svg) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](Setup_kr.md)

Scouter do not require any installation except java.

***

## 1. Collector Server Installation

### 1.1. Prerequisite
* JDK 1.6+ (1.8+ recommended)

### 1.2. Collector Server Installation
1. download lateset version of scouter-yyyyMMdd.tar.gz.
 - [Release Page](https://github.com/scouter-project/scouter/releases)
2. Extract the file.(You can see the dircetory ./scouter/server)
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
 System JRE version : 1.7.0_51
```

### 1.3. Network ports used by Collector server
* UDP Receive Port : 6100 (This port is used for gathering performance metrics.)
* TCP Service Port : 6100 (This port is used for commuication with scouter client and agents.)

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
* JDK 1.5+(Required), 1.6+(Recommended)

### 2.2. Download and start Host agent
1. Download scouter-yyyyMMdd.tar.gz 
 - [Release Page](https://github.com/scouter-project/scouter/releases)
2. Extract the file.(you can see the directory ./scouter/agent.host for running host monitoring agent.)
3. Run it.

```bash
cd ./scouter/agent.host
./host.sh
```

All options and default values are available from the scouter client's **Host >  Configure** menu.

## 3. Tomcat Agent
### 3.1. Prerequisite
* JDK 1.5+(Required), 1.6+(Recommended)

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
All options and default values are available from the scuoter client's **Object >  Configure** menu.

***

## 4. Client
### 4.1. Prerequisite
* JRE 1.8+

### Start Client
1. Download suitable one of `scouter.client.product-${os}[.tar.gz|.zip]` 
 - [Release Page](https://github.com/scouter-project/scouter/releases)
2. Extract all files to new directory
3. Execute scouter.exe(windows), scouter.app(osx) or scouter(linux)
4. You will see a dialog
5. Fill the authentication info(default : admin/admin) and press OK button
  (First, the scouter server must be running.)
6. The real-time chart will be shown.
