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
Scouter는 Open Source S/W를 위한 성능 모니터링 툴이다.
Tomcat, MariaDB 및 Node.js등의 오픈소스 기반은 웹서비스 미들웨어나 데이터베이스를 모니터링하기 위해 만들어 갈 것이다.

기업용 IT는 상용 서비스를 기반으로 진화하여 왔다. 그러나 이제는 점점 더 오픈 소스 기반의 시스템 구축이 늘어나고 있다. 
오픈소스 기반의 시스템을 구축할때 성능 관리 또한 같이 고려 되어야하는데 
오픈 소스인 Scouter는 가장 훌륭한 선택이 될 것이다.

### Modules
Scouter는 크게 3개의 모듈로 구성되었다.

- Agent : 성능정보를 수집하고 네트웍을 통해 서버에 전송한다.   
 - **ASM** : 바이트코드 제어를 위해 OW2의 (http://asm.ow2.org/) ASM라이브러리를 사용한다.
 - **Tools.jar** : 자바 쓰레드덤프, 힙덤프, 힙히스토그램등  JVM이 기본으로 제공하는 강력한 기능들을 같이 사용한다. 
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
