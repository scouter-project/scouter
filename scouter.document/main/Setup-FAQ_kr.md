# Setup FAQ
[![Englsh](https://img.shields.io/badge/language-English-red.svg)](Setup-FAQ.md) ![Korean](https://img.shields.io/badge/language-Korean-blue.svg)

설치과정에서 발생하는 빈번한 질문에 대해서 정리한다.

## Scouter 구성은 어떻게 되는가?
Scouter는 에이전트, 서버, 클라이언트 구조를 가지고 있다.
에이전트는 데이터를 수집하고 서버에 전송한다.
서버는 데이터를 재 가공하고 저장하며, 클라이언트의 요청에 응답한다.
클라이언트는 서버로 부터 성능데이터를 조회하여 화면에 차트를 포함한 다양한 방법으로 보여준다.
```
Agent[Linux]   ----\
Agent[Tomcat]  ------>  Server ——-> Client
Agent[MariaDB] ----/ 
```
Agent는 다시 Linux용 Agent와 Tomcat용 그리고 MariaDB용 에이전트로 구분할 수 있다. 앞으로 더많은 오픈소스가 포함될 예정이다.

## Tomcat과 Scouter Server 그리고 클라이언트를 분리하여 서로다른 서버에서 수행하려면
Scouter의 에이전트 서버 클라이언트는 TCP/UDP를 통해서 통신한다 따라서 적절한 설정을 통해 
상호 연결관계를 설정할 수 있다.

### 에이전트가 서버로 전송할때
UDP와 TCP 두가지 프로토코을 통해 데이터를 전송한다.
에이전트가 수집하는 일반적인 성능정보는 모두 UDP를 통해 전송한다.
TCP는 서버의 요청을 받아서 데이터를 전송한다. 
```
     UDP 6100
Agent  ====> Server 
     TCP 6100
```
따라서 에이전트에서는 서버에 대한 IP주소는 127.0.0.1을 기본으로 한다. 이것을 바꾸려면 

Agent가 설치된 Tomcat의 java 옵션에서 다음과 같이 설정할 수 있다. 
```
-Dsever_addr=192.168.0.1
-Dserver_udp_port=6100
-Dserver_tcp_port=6100
```
또는 scouter설정 파일에 설정할 수도 있다. 설정파일은 -Dscouter.config=scouter.conf 형식으로 설정한다.
```
sever_addr=192.168.0.1
server_udp_port=6100
server_tcp_port=6100
```


### 클라이언트가 서버에 요청할때
클라이언트는 서버에 데이터를 요청할때 TCP6100을 사용한다. 에이전트가 접속할때와 동일한 포트를 사용한다.
```
Clinet ——-> Server 
     TCP 6100
```
서버는 6100TCP를 통해 에이전트와 클라이언트 모두와 통신한다.

클라이언트는 처음 실행하여 로그인할때  서버에 대한 연결정보를 입력하도록 되어있다.

Server Address: 192.168.0.1:6100

## 여러 Tomcat 인스턴스를 모니터링 하려면 설정해야하는 것은?
Scouter는 모니터링 대상에게 계층형 이름을 부여하여 논리적으로 관리한다. 

/host/tomcat/context 

형식이다. 그런데 한 서버에 여러 톰켓인스턴스를 설치하는 경우에는 Scouter서버가 바라보는 모니터링 오브젝트의 이름이 중복된다. 이런경우 각 인스턴스를 위한 이름을 부여해야한다. 

Agent가 설치된 Tomcat의 java 옵션에서 다음과 같이 설정할 수 있다. 
```
-Dscouter_name=MyTomcatInstName 
```
혹은 scouter.config에 

```
scouter_name=MyTomcatInstName
```
라고 설정할 수 있다. 위 방법중에 한가지만 사용하면 된다.
그러면 클라이언트 화면에서 여러개의 톰켓 인스턴스를 확인할 수있다.

## Tomcat이 설치된 서버의 Cpu, Mem모니터링 하고 싶을때
scouter.agent.host를 릴리드할 예정임


