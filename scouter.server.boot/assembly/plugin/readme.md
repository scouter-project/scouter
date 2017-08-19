# This document is deprecated.
Refer to [the document of scouter](https://github.com/scouter-project/scouter/blob/master/scouter.document/main/Plugin-Guide.md)

## Collector Plugin
 - Default File Location : ${COLLECTOR_DIR}/plugin
 - Dynamic application By java code
 - Plugin Type
   - Pack Plugin
   - AlertRule Plugin
   
### Pack Plugin
  - Adding layer before store incoming data
  - Type
    - AlertPack Plugin
    - CounterPack Plugin
    - ObjectPack Plugin
    - SummaryPack Plugin
    - XLogPack Plugin
    - ProfilePack Plugin
  
#### AlertPack Plugin(alert.plug)

> public void process(AlertPack $pack, PluginHelper $$)
> {

>  // your code...
>  ref.) scouter.lang.pack.AlertPack
>  
> }

#### CounterPack Plugin(counter.plug)

> public void process(PerfCounterPack $pack, PluginHelper $$)
> {

>  // your code...
>  ref.) scouter.lang.pack.PerfCounterPack
>  
> }

#### ObjectPack Plugin(object.plug)

> public void process(ObjectPack $pack, PluginHelper $$)
> {

>  // your code...
>  ref.) scouter.lang.pack.ObjectPack
>  
> }  
  
#### SummaryPack Plugin(summary.plug)

> public void process(SummaryPack $pack, PluginHelper $$)
> {

>  // your code...
>  ref.) scouter.lang.pack.SummaryPack
>  
> }  
  
#### XLogPack Plugin(xlog.plug or xlogdb.plug, PluginHelper $$)

> public void process(XLogPack $pack)
> {

>  // your code...
>  ref.) scouter.lang.pack.XLogPack
>  
> }
  
#### ProfilePack Plugin(xlogprofile.plug, PluginHelper $$)

> public void process(XLogProfilePack $pack)
> {

>  // your code...
>  ref.) scouter.lang.pack.XLogProfilePack
>  
> }
  
  
### API

#### Common API
 - void log(Object c) : logging
 - void println(Object c) : standard out
 - void logTo(String file, String msg) : logging to a specific file
 
#### XLog or XLogDB Plugin API
 - String objName(XLogPack p) : get object name
 - String objType(XLogPack p) : get object type
 - String service(XLogPack p) : get service name
 - String error(XLogPack p) : get error name
 - String userAgent(XLogPack p) : get user agent
 - String referer(XLogPack p) : get referrer
 - String login(XLogPack p) : get login value
 - String desc(XLogPack p) : get desc value
 - String group(XLogPack p) : get group value

