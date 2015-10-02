## End user performance monitoring (Beta)

### Key features

#### Gathering script errors occurring on real user's browser.
Somewhere, somebody may feel bad user's experience against your web service now.
How can we recognize, gather and analyse those?
Now new feature of Scouter give us insight for our service's user experience. 

- We can see
    - error page url
    - error stack trace
    - User's OS, Machine, Browser...

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


### How to use
 - Just add one line script on your page.
 
