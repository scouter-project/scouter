## Common API

| Name          | Parameter                                                  | Return | Desc                                                        |
|---------------|------------------------------------------------------------|--------|-------------------------------------------------------------|
| log           | Object                                                     | void   | Logger를 통한 log                                           |
| println       | Object                                                     | void   | System.out를 통한 log                                       |
| field         | Object : Java Object, String : Field Name                    | Object | Object의 filed 값을 가져옴                                  |
| method        | Object : Java Object, String : Method Name                   | Object | Object의 method를 강제invoke 함                             |
| method1       | Object : Java Object, String : Method Name                   | Object | Object의 method를 invoke 함                                 |
| method        | Object : Java Object, String : Method Name, String : Parameter | Object | Object의 method를 String 파라미터와 함께 invoke 함          |
| toString      | Object                                                     | String | Object 를 toString 하여 반환                                |
| toString      | Object, String : default String                              | String | Object 를 toString 하여 반환, null 이면 default string 반환 |
| alert         | char : Alert level, String : Title, String : message           | void   | Alert 을 보냄                                               |
| syshash       | Object                                                     | int    | Object 의 identityHash 값 반환                              |
| syshash       | HookArgs : Arguments, int : Index                            | int    | Arguments의 i 인덱스의 identyHash 값 반환                   |
| syshash       | HookArgs : This                                            | int    | This 의 identyHash 값 반환                                  |
| forward       | WrContext : context, int : uuid                              | void   | Async Thread 를 App service로 연결                          |
| forwardThread | WrContext : context, int : uuid                              | void   | Async Thread 를 Background service로 연결                   |
| receive       | WrContext : context, int : uuid                              | void   | 앞서 등록된 Service가 있으면 연결                           |

## Main Classes API

### WrContext
 - String service() : Service Name 을 반환
 - void service(String name) : Service Name 을 set
 - int serviceHash() : Service Hash 값을 반환
 - void remoteIp(String ip) : Remote IP 을 set
 - String remoteIp() : Remote IP를 반환
 - void error(String err) : 임의의 error 를 주입
 - boolean isError() : 에러 체크
 - void group(String group) : 임의의 group을 set
 - String group() : Group을 반환
 - void login(String id) : 임의의 사용자 ID 를 set
 - String login() : 사용자 ID를 반환
 - void desc(String desc) : 임의의 Desc를 set
 - String desc() : Desc를 반환
 - String httpMethod() : Http Method를 반환
 - String httpQuery() : Http Query를 반환
 - String httpContentType() : Http Content-type을 반환
 - String userAgent() : User-Agent를 반환
 - void profile(String msg) : Msg 를 profile에 기록
 - long txid() : txid 를 반환
 - long gxid() : gxid 를 반환
 - TraceContext inner() : context를 반환
 
### WrRequest
 - String getCookie(String key) : Cookie 값을 반환
 - String getRequestURI() : Request URI를 반환
 - String getRemoteAddr() : Remote Address를 반환
 - String getMethod() : Method 를 반환
 - String getQueryString() : Query String을 반환
 - String getParameter(String key) : Parameter를 반환
 - Object getAttribute(String key) : Attribute를 반환
 - String getHeader(String key) : Header값을 반환
 - Enumeration getParameterNames() : Parameter 값들을 반환
 - Enumeration getHeaderNames() : HeaderName들을 반환
 - WrSession getSession() : WrSession객체를 반환
 - Set getSessionNames() : Session Name들을 반환
 - Object getSessionAttribute(String key) : Session 값을 반환
 - Object inner() : Request Object를 반환
 - boolean isOk() : Plugin 상태 확인
 - Throwable error() : Error 확인
 
### WrResponse
 - PrintWriter getWriter() : Writer를 반환
 - String getContentType() : Content-type을 반환
 - String getCharacterEncoding() : Character-encoding을 반환
 - Object inner() : Response Object를 반환
 - boolean isOk() : Plugin 상태 확인
 - Throwable error() : Error 확인
 
### WrSession
 - getAttribute(String key) : Attribute를 반환
 - Enumeration getAttributeNames() : Attribute Names를 반환
 - Object inner() : Session Object를 반환
 - boolean isOk() : Plugin 상태 확인
 - Throwable error() : Error 확인
 
### WrHttpCallRequest
 - void header(Object key, Object value) : Header값 추가
 - String toString(Object value) : 80자 이하로 toString
 - Object inner() : Request Object를 반환
 - boolean isOk() : Plugin 상태 확인
 - Throwable error() : Error 확인
 
### HookArgs
 - String getClassName() : Class 이름 반환
 - String getMethodName() : Method 이름 반환
 - String getMethodDesc() : Method 의 Desc 반환
 - Object getThis() : this object 반환
 - Object[] getArgs() : Arguments 반환
 - int getArgCount() : Argument 갯수 반환

### HookReturn
 - String getClassName() : Class 이름 반환
 - String getMethodName() : Method 이름 반환
 - String getMethodDesc() : Method 의 Desc 반환
 - Object getThis() : this object 반환
 - Object getReturn() : Return 값 반환
 
## Http-service Plugin

1. void start(WrContext $ctx, WrRequest $req, WrResponse $res)
 : Http Service 시작 시점
2. void end(WrContext $ctx, WrRequest $req, WrResponse $res)
 : Http Service 종료 시점
3. boolean reject(WrContext $ctx, WrRequest $req, WrResponse $res)
 : Http Service 시작 시점에 reject 조건 (default : false)
 
## Service Plugin

1. void start(WrContext $ctx, HookArgs $hook)
 : Service 시작 시점
2. void end(WrContext $ctx)
 : Service 종료 시점
 
## HttpCall Plugin

1. void call(WrContext $ctx, WrHttpCallRequest $req)
 : Http Call 요청 시점
 
## Capture Plugin

1. void capArgs(WrContext $ctx, HookArgs $hook)
 : Method 시작 시점
2. void capReturn(WrContext $ctx, HookReturn $hook)
 : Method Return 시점
3. void capThis(WrContext $ctx, String $class, String $desc, Object $this)
 : Constructor 생성 시점
 
## JDBC-Pool Plugin

1. String url(WrContext $ctx, String $msg, Object $pool)
 : DB Connection URL 요청 시점