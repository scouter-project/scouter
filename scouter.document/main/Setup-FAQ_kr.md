# Setup FAQ
[![English](https://img.shields.io/badge/language-English-orange.svg)](Setup-FAQ.md) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](Setup-FAQ_kr.md)

설치과정에서 발생하는 빈번한 질문에 대해서 정리한다.

## Scouter 구성은 어떻게 되는가?
Scouter 는 에이전트, 서버, 클라이언트 구조를 가지고 있다.
에이전트는 데이터를 수집하고 서버에 전송한다.
서버는 데이터를 재가공하고 저장하며, 클라이언트의 요청에 응답한다.
클라이언트는 서버로 부터 성능데이터를 조회하여 화면에 차트를 포함한 다양한 방법으로 보여준다.
```
Agent[Linux]   ----\
Agent[Tomcat]  ------>  Server ——-> Client
Agent[MariaDB] ----/ 
```
에이전트는 Linux용, Tomcat 용, 그리고 MariaDB 용 에이전트로 구분할 수 있다. 앞으로 더 많은 오픈소스가 포함될 예정이다.

## Tomcat, Scouter 서버, 클라이언트를 서로 다른 서버에서 수행하려면
Scouter 의 에이전트, 서버, 클라이언트는 TCP/UDP 소켓으로 통신한다. 따라서 적절한 설정을 통해 상호 연결관계를 설정할 수 있다.

### 에이전트가 서버로 전송할 때
UDP 와 TCP 두 가지 프로토콜을 통해 데이터를 전송한다.
에이전트가 수집하는 일반적인 성능정보는 모두 UDP를 통해 전송한다.
TCP는 서버의 요청을 받아서 데이터를 전송한다. 
```
     UDP 6100
Agent  ====> Server 
     TCP 6100
```
에이전트 프로그램의 Scouter 서버에 대한 IP 주소는 127.0.0.1 가 디폴트 이다. 이것을 바꾸려면 
Agent가 설치된 Tomcat의 java 옵션에 다음과 같이 수정한다.
```
-Dsever_addr=192.168.0.1
-Dserver_udp_port=6100
-Dserver_tcp_port=6100
```
또는 Scouter 설정파일에 추가 할 수도 있다. 설정파일의 path 정보는 -Dscouter.config=scouter.conf 형식으로 설정한다.
```
sever_addr=192.168.0.1
server_udp_port=6100
server_tcp_port=6100
```


### 클라이언트가 서버에 요청할때
클라이언트는 서버에 데이터를 요청할때 TCP 6100 포트를 사용한다. 에이전트가 접속할 때와 동일한 포트를 사용한다.
```
Clinet ——-> Server 
     TCP 6100
```
서버는 TCP 6100 포트를 통해 에이전트와 클라이언트 모두와 통신한다.
클라이언트는 처음 실행될 때 Scouter 서버에 대한 연결정보를 입력하도록 되어있다.

Server Address: 192.168.0.1:6100

## 여러 Tomcat 인스턴스를 모니터링 하려면 설정해야 하는 것은?
Scouter는 모니터링 대상에게 계층형 이름을 부여하여 논리적으로 관리한다. 

/host/tomcat/context 

형식이다. 한 서버에 여러 톰켓 인스턴스를 설치하는 경우에는 Scouter 서버가 바라보는 모니터링 오브젝트의 이름이 중복된다. 그렇기 때문에 각 인스턴스에 고유한 이름을 부여해야 한다. 

Agent가 설치된 Tomcat의 java 옵션에서 다음과 같이 설정할 수 있다. 
```
-Dobj_name=MyTomcatInstName 
```
혹은 scouter.config에 

```
obj_name=MyTomcatInstName
```
라고 설정할 수 있다. 위 방법 중에 한가지를 사용하면 된다.
그러면 클라이언트 화면에서 여러 개의 톰켓 인스턴스를 확인할 수있다.

## Tomcat이 설치된 서버의 CPU, Memory 를 모니터링 하고 싶을때
scouter.agent.host를 릴리드할 예정임


