# NON HTTP Server Trace
[![English](https://img.shields.io/badge/language-English-orange.svg)](NON-HTTP-Service-Trace.md) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](NON-HTTP-Service-Trace_kr.md)

This article explains non-hosted-by-WAS service. You should add entrypoint method of your service in hook_service_patterns configuration.

```
hook_service_patterns=com.mypkg.MyClass.myservice
```

Configuration format is consisted with full package paths and service method name. Analysis may be needed to determine what the entry  point is.

Here is live example,
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
### Step 1
At first, add package name patterns of business or framework package, which will be profiling target of Scouter Agent.
```
hook_method_patterns=com.mypkg*.*, org.other*.*
```
You can narrow down profiling range by adding hook_method_xxx option(s). 

### Step 2
hook_method options are used for specifying which methods should be traced. Scouter will start to profile that methods after the service trace is triggered on. So auto trace of service startup should be enabled below,
```
_trace_auto_service_enabled=true
```

### Step 3
You can  check the termination status or termination information on XLog, after service process was restarted.

### Step 4
Add found service entrypoint to hook_service_patterns, remove hook_method or enable_auto_service_trace option.
```
hook_service_patterns=com.mypkg.MyClass.myservice
```
