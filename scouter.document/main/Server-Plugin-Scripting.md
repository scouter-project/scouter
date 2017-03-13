# Scouter Plugin Guide
![Englsh](https://img.shields.io/badge/language-English-orange.svg) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](Server-Plugin-Scripting_kr.md)

## Collector Plugin
 - Default File Location : ${COLLECTOR_DIR}/plugin
 - Dynamic application
 - By java code
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

> public void process(AlertPack $pack)
> {

>  // your code...
>  ref.) scouter.lang.pack.AlertPack
>  
> }

#### CounterPack Plugin(counter.plug)

> public void process(PerfCounterPack $pack)
> {

>  // your code...
>  ref.) scouter.lang.pack.PerfCounterPack
>  
> }

#### ObjectPack Plugin(object.plug)

> public void process(ObjectPack $pack)
> {

>  // your code...
>  ref.) scouter.lang.pack.ObjectPack
>  
> }  
  
#### SummaryPack Plugin(summary.plug)

> public void process(SummaryPack $pack)
> {

>  // your code...
>  ref.) scouter.lang.pack.SummaryPack
>  
> }  
  
#### XLogPack Plugin(xlog.plug or xlogdb.plug)

> public void process(XLogPack $pack)
> {

>  // your code...
>  ref.) scouter.lang.pack.XLogPack
>  
> }
  
#### ProfilePack Plugin(xlogprofile.plug)

> public void process(XLogProfilePack $pack)
> {

>  // your code...
>  ref.) scouter.lang.pack.XLogProfilePack
>  
> }
  
  
### API

#### Common API
 - void log(Object c) : Logger를 통한 log
 - void println(Object c) : System.out를 통한 log
 - void logTo(String file, String msg) : 특정 file에 msg를 logging
 
#### XLog or XLogDB Plugin API
 - String objName(XLogPack p) : XLog의 Object Name을 반환
 - String objType(XLogPack p) : XLog의 Object Type을 반환
 - String service(XLogPack p) : XLog의 service를 반환
 - String error(XLogPack p) : XLog의 error를 반환
 - String userAgent(XLogPack p) : XLog의 user-agent를 반환
 - String referer(XLogPack p) : XLog의 referer를 반환
 - String login(XLogPack p) : XLog의 login 값을 반환
 - String desc(XLogPack p) : XLog의 desc 값을 반환
 - String group(XLogPack p) : XLog의 group 값을 반환
 
### AlertRule Plugin
  **TBD**
  
