# NON-HTTP 서비스 추적하기
[![English](https://img.shields.io/badge/language-English-orange.svg)](NON-HTTP-Service-Trace.md) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](NON-HTTP-Service-Trace_kr.md)

WAS(ex Tomcat)가 아닌 서버에서 서비스를 추적하는 방법을 설명한다. 

서비스가 시작되는 메소드를 hook_service_patterns에 등록해야한다.

```
hook_service_patterns=com.mypkg.MyClass.myservice
```

형식으로 풀패키지명과 클래스/메소드 이름까지 설정한다.

그런데 서비스 시작 부분을 찾기 위해서는 분석이 필요하다. 

참고)
```
_trace_auto_service_enabled=false
_trace_auto_service_backstack_enabled=true

hook_method_patterns=
hook_method_access_public_enabled=true
hook_method_access_private_enabled=false
hook_method_access_protected_enabled=false
hook_method_access_none_enabled=false
hook_method_ignore_prefixes=get,set
```
### 단계 1
먼저 프로파일 대상 클래스 들을 모두 지정한다 대게는 업무나 프레임웍 패키지를 모두 지정한다.
```
hook_method_patterns=com.mypkg*.*, org.other*.*
```
단 메소드가 많을 경우 hook_method_xxx옵션들을 이용하여 필터링 할 수 있다.. 

### 단계 2
hook_method는 단지 프로파일링할 대상 메소드를 지정하는 것이다. 그런데 서비스 추적이 시작되지 않으면 프로파일을 추적하지 않는다 
이때 자동 서비스 추적을 enable한다.
```
_trace_auto_service_enabled=true
```

### 단계 3
**프로세스를 재기동하고 서비스를 호출**하면 
종료되지 않는 서비스들을 볼 수 있다. 혹은 종료되었다면 XLog에서 상세한 정보를 조회할 수 있다. 

### 단계 4
찾아진 서비스 시작점을 hook_service_patterns 지정하고 hook_method 나 enable_auto_service_trace 옵션을 제거한다.
```
hook_service_patterns=com.mypkg.MyClass.myservice
```
