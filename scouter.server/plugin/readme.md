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
  
  
### AlertRule Plugin
  **TBD**