# 응답시간 분포도(XLog) 보는 방법
![Englsh](https://img.shields.io/badge/language-English-red.svg) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](Reading-XLog_kr.md)

하나의 트랜잭션(서비스 수행)을 하나의 점으로 표현하는 차트이다.
세로축은 응답시간으로 가로축은 종료시간으로 출력한다.  
실시간 화면은 일정시간(2초) 간격으로 좌측으로 쉬프트 된다.

![XLog](../img/client/xlog.png)

Scouter에서는 응답 분포 차트를 XLog라고 부른다. XLog는 Transaction Log라는 의미에서 2004년에 처음 만들어졌다. 
XLog는 전체 트랜잭션을 한눈에 파악할 수 있고 느린 트랜잭션만을 선별하여 조회할 수있기 때문에 응용 프로그램을 튜닝하는데 가장 효과적이 모니터링 방법이라고 할 수있다.

![TxList](../img/client/xlog_txlist.png)
![Profile](../img/client/xlog_profile.png)

위 화면에서 처럼 왼쪽마우스 버튼을 이용하여 드레그하면 일부 점들을 선별하여 선택할 수 있다. 이렇게 선택된 트랜잭션들은 화면처럼 리스트로 나타난다.
리스트에서 하나를 선택하면 해당 트랜잭션에 대한 상세 정보를 볼 수 있다. 
