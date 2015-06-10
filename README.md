## Open Source S/W Performance Monitoring
```
  ____                  _            
 / ___|  ___ ___  _   _| |_ ___ _ __ 
 \___ \ / __/   \| | | | __/ _ \ '__|
  ___) | (_| (+) | |_| | ||  __/ |   
 |____/ \___\___/ \__,_|\__\___|_|                                      
 Scouter version 0.0.1 20150601
 Open Source S/W Performance Monitoring 
```
SCOUTER is a performance monitoring tool for Open Source S/W such as Tomcat, MariaDB and Node.js
SCOUTER is going to be made in order to monitor open source Web middlewares or databases.

Enterprise IT has evolved based on commercial services. 
But now more and more systems are using open source S/W. 
If you use a Tomcat, SCOUTER will be the best choice to monitor it.

### Modules
Scouter has three modules:

- Agent : gather performance information and send  to the server
 - **ASM** :  using ASM library of OW2  (http://asm.ow2.org/) for BCI(byte code instrumentation)
 - **Tools.jar** : Java thread dumps, heap dumps, heap histograms, and the other powerful features provided by the JVM as the default.
 - **JMX** :  CPU & MEM 

- Server :  데이터를 수신하여 클라이언트에 서비스 하고 저장한다.
 - **SCALA** : 개발언어로 스칼라를 사용하였다. 자바로 할수 없는 다양한 확장성있는 기능과 성능을 제공할 것이다.
 - HASH FILE : 성능데이터를 해쉬 인덱스를 통해 파일에저장, 대용량 데이터를 빠르게 처리한다. 
 - GZIP : 프로파일 데이터 압축 저장, 대용량 성능데이터를 적은 공간으로 처리할 수 있다.

- Client : 데이터를 화면에 보여준다
 - Eclipse RCP : 독립 실행 프로그램으로 만들어졌다. 여러대의 수집서버를 클라이언트단에서 통합함으로 대규모시스템에서 컴팩트안 운영이 가능하다.
 - SWT & GEF4 : 차트와 다이어그램

### To-be
Scouter 단순함을 추구하는 단순함을 추구하는 모니터링 툴이다 가능한 단수한 구성으로 빠르게 OSS를 모니터링 하는 기능들을 제공할 것이다.
- 인터넷 환경을 통해 배포되고 업그레이드 될 것이다.
- 윈도우, MAC OSX, Linux등의 환경에서 동작하는 클아이언트을 배포하고 
- 자동 업그레이드도 가능할 것이다.
- 기능의 복잡도보다는 응답 분포도, 액티브 서비스, 테그카운팅 분석등의 핵심기능을  사용자에게 제공할 것이다.

### License
Licensed under the Apache License, Version 2.0 (the "License");

### Using Components & Icons
- This product includes GeoLite data created by MaxMind, available from
http://www.maxmind.com

- Download MaxMind GeoIP Data :  http://geolite.maxmind.com/download/geoip/database/GeoLiteCity.dat.gz

- Icons from : 
http://www.famfamfam.com/lab/icons/silk/
