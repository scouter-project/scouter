## End user monitoring (Beta)

### 주요 기능
#### Script error 수집
- User's borwser 에서 발생하는 Client script error 수집
- 제공 정보
    - 오류 발생 page url
    - 오류 발생 stack trace
    - 오류 발생 OS, Machine, Browser, Environment 정보 등

#### Navigation timing 수집
- browser의 performance timing 정보 수집(real user의 page loading 체감 시간)
- 제공 정보
    - 호출 url
    - scouter server에서 발행한 gxid (server side timing 정보와 연동)
    - connection time
    - dns lookup time
    - request time
    - wait for response time
    - response start to end time(network time)
    - load event time
    - user's geo location...

#### Ajax timing 수집
- Ajax 요청에 대한 response time
- 제공 정보
  - 호출 url
  - Client에서 측정항 response time 정보
  - scouter server에서 발행한 gxid (server side timing 정보와 연동)

