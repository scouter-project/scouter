# XLog Case3 - Undestand Horizontal
![Englsh](https://img.shields.io/badge/language-English-red.svg) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](XLogCase3_kr.md)

응답분포(XLOG)의 대표적인 패턴중에 하나는 가로라인이 나타나는 것이다.  
가로 라인은 어떤 트랜잭션의 응답시간이 정형한 것이다. 
보통 자원에 대한 획득을 위해 일정시간 WAIT이 발생하는 경우 가로라인이 형성된다.
예를 들어 어떤자원을 조회하고 실패하면 3초 기다렸다 다시 조회하는 경우 3초,6초,9초의 지연이 발생하고
화면에서는 3초간격의 라인이 형성된다.

또는 테스트 할때 외부 연계를 시물레이션하기 위해 일정시간의 Sleep을 걸어두는 경우에도 
라인이 형성된다.

![Horizontal Line](../img/client/xlog_horizontal.png)

물로 어떤 경우에는 일정시간을 기다렸다 처리하도록 고의로 서비스 지연을 유도하는 경우도 있지만
최근의 인터넷 서비 환경에서는 그렇게 처리하지 않는다. 과거에는 부족한 자원 때문에 서서히 처리하도록 프로그램했지만
요즘에는 자원을 늘리는 방식을 택한다. 

따라서 가로 라인이 만들어진다는 것은 어떤 이유가 존재한다. 
반드시 운영자는 평상시 가로라인이 형성된다면 그 이유를 명확히 확인해야 한다.