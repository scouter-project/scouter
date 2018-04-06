# Scouter Plugin Guide
[![English](https://img.shields.io/badge/language-English-orange.svg)](JavaAgent-Plugin-Scripting.md) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](JavaAgent-Plugin-Scripting_kr.md)

## Javaagent Plugin
 - Default File Location : ```${directory of scouter.agent.jar}/plugin```
 - or able to configure it for example - ```plugin_dir=/aaa/bbb/ccc/plugin```
 - Write java code on the specific text file then the code is dynamically loaded on runtime.
 - plugin types
   - Http-service plugin
   - Service plugin
   - HttpCall plugin
   - Capture plugin
   - JDBC-Pool plugin
 
### Http-service Plugin(httpservice.plug)

1. ```void start(WrContext $ctx, WrRequest $req, WrResponse $res)``` : invoked at the start of HttpServlet service() method
2. ```void end(WrContext $ctx, WrRequest $req, WrResponse $res)``` : invoked at the end of HttpServlet service() method
3. ```boolean reject(WrContext $ctx, WrRequest $req, WrResponse $res)``` : invoked at the start of HttpServlet service() method. If return true then the request is rejected
 
### Service Plugin(service.plug)
 **invoked when arrived the methods defined in the option ```hook_service_patterns```**
 
1. ```void start(WrContext $ctx, HookArgs $hook)``` : invoked when a service starts
2. ```void end(WrContext $ctx)``` : invoked when a service ends
 
### HttpCall Plugin(httpcall.plug)

1. ```void call(WrContext $ctx, WrHttpCallRequest $req)``` : invoked when an external call is invoked by httpClient and http client libraries.
 
### Capture Plugin(capture.plug)
 **invoked when arrived the methods defined in the options ```hook_args_patterns```, ```hook_return_patterns``` and ```hook_constructor_patterns```**
 
1. ```void capArgs(WrContext $ctx, HookArgs $hook)``` : invoked at the start of the method
2. ```void capReturn(WrContext $ctx, HookReturn $hook)``` : invoked at the end of the method
3. ```void capThis(WrContext $ctx, String $class, String $desc, Object $this)``` : invoked at a constructor
 
### JDBC-Pool Plugin(jdbcpool.plug)
1. ```String url(WrContext $ctx, String $msg, Object $pool)```
 : invoked when retrieve DB Connection URL
 
 
## API

### Common API
 - ```void log(Object c)``` : logging
 - ```void println(Object c)``` : System.out.println()
 - ```Object getFieldValue(Object o, String fieldName)``` : get field value as object of 'o'
 - ```Object invokeMethod(Object o, String methodName)``` : invoke the method
 - ```Object invokeMethod(Object o, String methodName, Object[] args)``` : invoke the method with args
 - ```Object invokeMethod(Object o, String methodName, Class[] argTypes, Object[] args)``` : invoke the method with args
 - ```Object newInstance(String className)``` : new instance of the class
 - ```Object newInstance(String className, ClassLoader loader)``` : new instance of the class from the classloader
 - ```Object newInstance(String className, Object[] args)``` : new instance of the class with arguments
 - ```Object newInstance(String className, ClassLoader loader, Object[] args)``` : new instance of the class with arguments from the classloader
 - ```Object newInstance(String className, ClassLoader loader, Class[] argTypes, Object[] args)``` : new instance of the class with arguments from the classloader
 - ```String toString(Object o)``` : invoke toString() of the object
 - ```String toString(Object o, String def)``` : invoke toString() of the object, if null, return def.
 - ```void alert(char level, String title, String message)``` : invoke alert (level : i\|w\|e\|f as info, warn, error, fatal).
 - ```Class[] makeArgTypes(Class class0, Class class1, ..., classN)``` : assemble argument types array to call the reflection method ```invokeMethod()```
 - ```Object[] makeArgs(Object obj0, Object obj1, ..., objN)``` : assemble arguments array to call the reflection method ```invokeMethod()```

### WrContext class API
 - ```String service()``` : get a service name of XLog from the trace context
 - ```void service(String name)``` : set a service Name of XLog  to the trace context
 - ```int serviceHash()``` : get a service hash value of XLog from the trace context
 - ```void remoteIp(String ip)``` : set a remote ip of XLog to the trace context
 - ```String remoteIp()``` : get a remote ip of XLog from the trace context
 - ```void error(String err)``` : set a error message of XLog to the trace context
 - ```boolean isError()``` : if error occurred in the trace context
 - ```void group(String group)``` : set a group name of XLog to the trace context
 - ```String group()``` : get a group name of XLog from the trace context
 - ```void login(String id)``` : set a login value of XLog to the trace context
 - ```String login()``` : get a login value of XLog from the trace context
 - ```void desc(String desc)``` : set a desc value of XLog to the trace context
 - ```String desc()``` : get a desc value of XLog from the trace context
 - ```String httpMethod()``` : get a http method
 - ```String httpQuery()``` : get a http query string
 - ```String httpContentType()``` : get a http content type
 - ```String userAgent()``` : get a user agent value
 - ```void profile(String msg)``` : profile a message to the XLog profile
 - ```void hashProfile(String msg, int value, int elapsed)``` : profile a message as hash value to the XLog profile
 - ```parameterizedProfile(int level, String msgFormat, int elapsed, String[] params)``` : profile a message format with parameters.
     - message example : "Hello, my name is %s and my age is %s"
     - level : 0-debug, 1-info, 2-warn, 3-error, 4-fatal
 - ```long txid()``` : get a txid of XLog
 - ```long gxid()``` : get a gxid of XLog
 - ```TraceContext inner()``` : get raw TraceContext를 반환
 
### WrRequest class API
 - ```String getCookie(String key)``` : get a cookie of the key from the HttpRequest
 - ```String getRequestURI()``` : get a request uri from the HttpRequest
 - ```String getRemoteAddr()``` : get a remote address from the HttpRequest
 - ```String getMethod()``` : get a http method from the HttpRequest
 - ```String getQueryString()``` : get a query string from the HttpRequest
 - ```String getParameter(String key)``` : get a http parameter of the key from the HttpRequest
 - ```Object getAttribute(String key)``` : get a http request attribute of the key from the HttpRequest
 - ```String getHeader(String key)``` : get a http header of the key from the HttpRequest
 - ```Enumeration getParameterNames()``` :
 - ```Enumeration getHeaderNames()``` :
 - ```WrSession getSession()``` : get the WrSession instance
 - ```Set getSessionNames()``` : get session attribute names from the HttpRequest
 - ```Object getSessionAttribute(String key)``` : get a session value of the key
 - ```Object inner()``` : get the raw HttpRequest object
 - ```boolean isOk()``` : check the plugin status
 - ```Throwable error()``` : get the error that occurred when the WrRequest method called.
 
### WrResponse class API
 - ```PrintWriter getWriter()``` : get the response writer
 - ```String getContentType()``` : get a content type of the response
 - ```String getCharacterEncoding()``` : get a encoding of the response
 - ```Object inner()``` : get the raw Response object
 - ```boolean isOk()``` : check the plugin status
 - ```Throwable error()``` : get the error that occurred when the WrResponse method called.
 
### WrSession class API
 - ```Object getAttribute(String key)``` :
 - ```Enumeration getAttributeNames()``` :
 - ```Object inner()``` : get the raw HttpSession object
 - ```boolean isOk()``` : check the plugin status
 - ```Throwable error()``` : get the error that occurred when the WrResponse method called.
 
### WrHttpCallRequest class API
 - ```void header(Object key, Object value)``` : add http header before the call invoked
 - ```Object inner()``` : get the http call object
 - ```boolean isOk()``` : check the plugin status
 - ```Throwable error()``` : get the error that occurred when the WrResponse method called.
 
### HookArgs class API
 - ```String getClassName()``` : get the class name of the invoked method
 - ```String getMethodName()``` : get the method name invoked
 - ```String getMethodDesc()``` : get the method description
 - ```Object getThis()``` : get this
 - ```Object[] getArgs()``` : get the method arguments
 - ```int getArgCount()``` : get the argument count

### HookReturn class API
 - ```String getClassName()``` : get the class name of the invoked method
 - ```String getMethodName()``` : get the method name invoked
 - ```String getMethodDesc()``` : get the method description
 - ```Object getThis()``` : get this
 - ```Object getReturn()``` : get a return value

 
