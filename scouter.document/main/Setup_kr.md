# Setup
[![English](https://img.shields.io/badge/language-English-orange.svg)](Setup.md) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](Setup_kr.md)

- 외부 링크
  - 블로그
    - [Scouter 소소한 시리즈 #1 - 설치](http://gunsdevlog.blogspot.kr/2017/07/scouter-apm-1.html)

***

## 1. Collector Server 설치

### 1.1. Prerequisite
* JDK 1.8+

### 1.2. Collector Server 설치 및 기동
1. 최신버전의 scouter-yyyyMMdd.tar.gz 다운로드 받는다.
 - [Release Page](https://github.com/scouter-project/scouter/releases)
2. 다운받은 파일을 압축 해제한다.(./scouter/server 디렉토리 확인)
3. start script를 실행한다.
```bash
cd ./scouter/server
./startup.sh
#Windows의 경우는 startup.bat 실행
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

### 1.3. Collector server에서 사용하는 Network Port
* UDP Receive Port : 6100 (성능 정보 수집용)
* TCP Service Port : 6100 (Agent 및 Client 통신용)

### 1.4. Configuration

#### 1.4.1. Configuration file
 * 설정 파일 위치
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
전체 옵션 및 default 값은 scouter client의 Collector > Configure 메뉴에서 확인이 가능하다.
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
전체 옵션 및 default 값은 scouter client의 Host > Configure 메뉴에서 확인이 가능하다.

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
* **만약 하나의 VM에서 여러개의 Tomcat 인스턴스를 사용한다면 각각의 configuration file을 정의해야 한다.**
  * 위 예에서처럼 -Dscouter.config 환경변수를 통해 conf 파일을 지정할 수 있다.
  * 또한 이 경우 하나의 VM에서 모니터링 대상의 이름이 중복되지 않도록 obj_name 옵션을 통해 이름을 지정하여야 한다.
  
#### 3.2.2 Java Option example ( Windows Service Option )
Append below java options in **${TOMCAT_DIR}/bin/tomcat${version}w.exe 
```bash
-javaagent:${SCOUTER_INSTALL_DIR}/scouter.agent.jar"
-Dscouter.config=${SCOUTER_INSTALL_DIR}/conf/scouter1.conf"
-Dobj_name=myFirstTomcat1"
```
* **${SCOUTER_INSTALL_DIR}** means the directory that contains scouter.agent.jar file.
* **윈도우 서비스를 통해 Tomcat을 실행하는 경우 tomcat${version}w.exe 옵션에 추가해야 한다.**
  * 해당 옵션은 tomcat${version}w.exe ( ex)tomcat9w.exe ) > Java > Java Options에서 추가할 수 있다.
  
  
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
전체 옵션 및 default 값은 scouter client의 Object > Configure 메뉴에서 확인이 가능하다.
***

## 4. Client
### 4.1. Prerequisite
* JRE 1.8+

### Start Client
1. Download suitable one of `scouter.client.product-${os}[.tar.gz|.zip]` 
 - [Release Page](https://github.com/scouter-project/scouter/releases)
 - **Important for Mac(OSX)**: run the command if you can not open it. `xattr -cr scouter.client.app`   
 - scouter client needs java 11+  
2. Extract all files to new directory
3. Execute scouter.exe(windows), scouter.app(osx) or scouter(linux)
4. You will see a dialog
5. Fill the authentication info(default : admin/admin) and press OK button
  (먼저 scouter server가 실행되어 있어야 함.)
6. The real-time chart will be shown.

***

## 5. Web API (Installation and usage)

* [Web API Guide](../tech/Web-API-Guide_kr.md)
