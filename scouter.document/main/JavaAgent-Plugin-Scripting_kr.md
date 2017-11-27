# Scouter Plugin Guide
[![English](https://img.shields.io/badge/language-English-orange.svg)](JavaAgent-Plugin-Scripting.md) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](JavaAgent-Plugin-Scripting_kr.md)

## Javaagent Plugin
 - Default File Location : ```${directory of scouter.agent.jar}/plugin```
 - 또는 설정 가능 -> ```plugin_dir=/aaa/bbb/ccc/plugin```
 - plugin 파일에 java 코드 기록시 런타임에 동적으로 코드를 컴파일하여 로딩, 바로 적용됨
 - Plugin 종류
   - Http-service
   - Service
   - HttpCall
   - Capture
   - JDBC-Pool
 
### Http-service Plugin(httpservice.plug)

1. ```void start(WrContext $ctx, WrRequest $req, WrResponse $res)``` : Http Service 시작 시점
2. ```void end(WrContext $ctx, WrRequest $req, WrResponse $res)``` : Http Service 종료 시점
3. ```boolean reject(WrContext $ctx, WrRequest $req, WrResponse $res)``` : Http Service 시작 시점에 reject 조건 (default : false)
 
### Service Plugin(service.plug)
  **```hook_service_patterns``` 에 정의된 method 에서 호출됨**
 
1. ```void start(WrContext $ctx, HookArgs $hook)``` : Service 시작 시점
2. ```void end(WrContext $ctx)``` : Service 종료 시점
 
### HttpCall Plugin(httpcall.plug)

1. ```void call(WrContext $ctx, WrHttpCallRequest $req)``` : Http Call 요청 시점
 
### Capture Plugin(capture.plug)
 **```hook_args_patterns```, ```hook_return_patterns```, ```hook_constructor_patterns``` 에 정의된 method 에서 호출됨**
 
1. ```void capArgs(WrContext $ctx, HookArgs $hook)``` : Method 시작 시점
2. ```void capReturn(WrContext $ctx, HookReturn $hook)``` : Method Return 시점
3. ```void capThis(WrContext $ctx, String $class, String $desc, Object $this)``` : Constructor 생성 시점
 
### JDBC-Pool Plugin(jdbcpool.plug)

1. ```String url(WrContext $ctx, String $msg, Object $pool)```
 : DB Connection URL 요청 시점
 
 
## API

### Common API
 - ```void log(Object c)``` : Logger를 통한 log
 - ```void println(Object c)``` : System.out를 통한 log
 - ```Object getFieldValue(Object o, String fieldName)``` : get field value as object of 'o'
 - ```Object invokeMethod(Object o, String methodName)``` : invoke the method
 - ```Object invokeMethod(Object o, String methodName, Object[] args)``` : invoke the method with args
 - ```Object invokeMethod(Object o, String methodName, Class[] argTypes, Object[] args)``` : invoke the method with args
 - ```Object newInstance(String className)``` : new instance of the class
 - ```Object newInstance(String className, ClassLoader loader)``` : new instance of the class from the classloader
 - ```Object newInstance(String className, Object[] args)``` : new instance of the class with arguments
 - ```Object newInstance(String className, ClassLoader loader, Object[] args)``` : new instance of the class with arguments from the classloader
 - ```Object newInstance(String className, ClassLoader loader, Class[] argTypes, Object[] args)``` : new instance of the class with arguments from the classloader
 - ```String toString(Object o)``` : Object 를 toString 하여 반환
 - ```String toString(Object o, String def)``` : Object 를 toString 하여 반환, null 이면 default string 반환
 - ```void alert(char level, String title, String message)``` : Alert 을 보냄 (level : i\|w\|e\|f as info, warn, error, fatal).
 - ```Class[] makeArgTypes(Class class0, Class class1, ..., classN)``` : assemble argument types array to call the reflection method ```invokeMethod()```
 - ```Object[] makeArgs(Object obj0, Object obj1, ..., objN)``` : assemble arguments array to call the reflection method ```invokeMethod()```


### WrContext class API
 - ```String service()``` : Service Name 을 반환
 - ```void service(String name)``` : Service Name 을 set
 - ```int serviceHash()``` : Service Hash 값을 반환
 - ```void remoteIp(String ip)``` : Remote IP 을 set
 - ```String remoteIp()``` : Remote IP를 반환
 - ```void error(String err)``` : 임의의 error 를 주입
 - ```boolean isError()``` : 에러 체크
 - ```void group(String group)``` : 임의의 group을 set
 - ```String group()``` : Group을 반환
 - ```void login(String id)``` : 임의의 사용자 ID 를 set
 - ```String login()``` : 사용자 ID를 반환
 - ```void desc(String desc)``` : 임의의 Desc를 set
 - ```String desc()``` : Desc를 반환
 - ```String httpMethod()``` : Http Method를 반환
 - ```String httpQuery()``` : Http Query를 반환
 - ```String httpContentType()``` : Http Content-type을 반환
 - ```String userAgent()``` : User-Agent를 반환
 - ```void profile(String msg)``` : Msg 를 profile에 기록
 - ```void hashProfile(String msg, int value, int elapsed)``` : profile a message as hash value to the XLog profile
 - ```parameterizedProfile(int level, String msgFormat, int elapsed, String[] params)``` : profile a message format with parameters.
      - message example : "Hello, my name is %s and my age is %s"
      - level : 0-debug, 1-info, 2-warn, 3-error, 4-fatal
 - ```long txid()``` : txid 를 반환
 - ```long gxid()``` : gxid 를 반환
 - ```TraceContext inner()``` : context를 반환
 
### WrRequest class API
 - ```String getCookie(String key)``` : Cookie 값을 반환
 - ```String getRequestURI()``` : Request URI를 반환
 - ```String getRemoteAddr()``` : Remote Address를 반환
 - ```String getMethod()``` : Method 를 반환
 - ```String getQueryString()``` : Query String을 반환
 - ```String getParameter(String key)``` : Parameter를 반환
 - ```Object getAttribute(String key)``` : Attribute를 반환
 - ```String getHeader(String key)``` : Header값을 반환
 - ```Enumeration getParameterNames()``` : Parameter 값들을 반환
 - ```Enumeration getHeaderNames()``` : HeaderName들을 반환
 - ```WrSession getSession()``` : WrSession객체를 반환
 - ```Set getSessionNames()``` : Session Name들을 반환
 - ```Object getSessionAttribute(String key)``` : Session 값을 반환
 - ```Object inner()``` : Request Object를 반환
 - ```boolean isOk()``` : Plugin 상태 확인
 - ```Throwable error()``` : Error 확인
 
### WrResponse class API
 - ```PrintWriter getWriter()``` : Writer를 반환
 - ```String getContentType()``` : Content-type을 반환
 - ```String getCharacterEncoding()``` : Character-encoding을 반환
 - ```Object inner()``` : Response Object를 반환
 - ```boolean isOk()``` : Plugin 상태 확인
 - ```Throwable error()``` : Error 확인
 
### WrSession class API
 - ```getAttribute(String key)``` : Attribute를 반환
 - ```Enumeration getAttributeNames()``` : Attribute Names를 반환
 - ```Object inner()``` : Session Object를 반환
 - ```boolean isOk()``` : Plugin 상태 확인
 - ```Throwable error()``` : Error 확인
 
### WrHttpCallRequest class API
 - ```void header(Object key, Object value)``` : Header값 추가
 - ```Object inner()``` : Request Object를 반환
 - ```boolean isOk()``` : Plugin 상태 확인
 - ```Throwable error()``` : Error 확인
 
### HookArgs class API
 - ```String getClassName()``` : Class 이름 반환
 - ```String getMethodName()``` : Method 이름 반환
 - ```String getMethodDesc()``` : Method 의 Desc 반환
 - ```Object getThis()``` : this object 반환
 - ```Object[] getArgs()``` : Arguments 반환
 - ```int getArgCount()``` : Argument 갯수 반환

### HookReturn class API
 - ```String getClassName()``` : Class 이름 반환
 - ```String getMethodName()``` : Method 이름 반환
 - ```String getMethodDesc()``` : Method 의 Desc 반환
 - ```Object getThis()``` : this object 반환
 - ```Object getReturn()``` : Return 값 반환

 
