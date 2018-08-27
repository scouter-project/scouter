# Telegraf Server Feature
[![English](https://img.shields.io/badge/language-English-orange.svg)](Telegraf-Server.md) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](Telegraf-Server_kr.md)

It can be integrated with Telegraf using the Telegraf Server feature of the Scouter collector. 

The Scouter collector is now interoperable with the HTTP output of Telegraf and will be provided with dedicated telegraf-scouter output later. 
  - [Telegraf HTTP Output plugin](https://github.com/influxdata/telegraf/tree/master/plugins/outputs/http)

You can monitor the performance information of various products through Telegraf's Input Plugin. Refer to the telegraf plugin page for current input. 
  - [Telegraf Input plugins](https://github.com/influxdata/telegraf/tree/master/plugins/inputs)


## Apply Telegraf Server function

### 1. Enable the Telegraf Server option
First, the Collector's http server must be enabled. 
  - `net_http_server_enabled=true`

And enable telegraf server function. 
  - `input_telegraf_enabled=true`

If you want to change the http port, set it to `net_http_port=xxx`. The default value is 6180. 
If you changed the above option, you must restart the collector. 

If the requested data needs to be checked, set `input_telegraf_debug_enabled=true' to log all the requested data.

### 2. Telegraf Http output settings
Set the end point of the Telegraf http output to the scouter server set above. 
```javascript
  [[outputs.http]]
    url = "http://my-scouter-server:6180/telegraf/metric"
    timeout = "5s"
    method = "POST"
    data_format = "influx"
```

The scouter processes the request every 2 seconds and mainly be used with the real time chart, so the request interval of telegraf is also adjusted to be between 2 seconds and 10 seconds. 

```javascript
[agent]
  ...
  interval = "4s"
  ...
  flush_interval = "4s"
```

### 3. Set counter mapping to Scouter Collector
You must map fields of measurement that is passed in telegraf to the scouter counter. 
There are a few things you need to do. 
  - Set line protocol measurement to the monitoring family of scouter counter.
  - Set object type.
  - Set object name.
  - Map fields of line protocol to counters
 
Set the following items and replace the $measurement$ part with the actual measurement name. 
The value on the right side of the equal sign indicates the default value. 
```properties
input_telegraf_$measurement$_debug_enabled=false
input_telegraf_$measurement$_objFamily_base=
input_telegraf_$measurement$_host_tag=host
input_telegraf_$measurement$_host_mappings=
input_telegraf_$measurement$_objName_base=
input_telegraf_$measurement$_objName_append_tags=
input_telegraf_$measurement$_objType_base=
input_telegraf_$measurement$_objType_prepend_tags=scouter_obj_type_prefix
input_telegraf_$measurement$_objType_append_tags=
input_telegraf_$measurement$_tag_filter=
input_telegraf_$measurement$_counter_mappings=
```
For example, if the measurement name is redis, `input_telegraf_$measurement$_debug_enabled` becomes` input_telegraf_$redis$_debug_enabled`. 
 
Let's take an example of telegraf's redis input. 

#### 3.1. Family Settings
Family can be thought of as a name that refers to a collection that has the same performance monitoring items. 
For example, the Host Family has performance information such as cpu, memory, and disk io. 
 
Here we set Family to redis.
```properties
# In case of setting as below, Family is registered as X$redis internally to prevent duplication of name with already provided family.
input_telegraf_$redis$_objFamily_base=redis
```

#### 3.2. Host Mapping Settings
일반적으로 설정을 변경할 필요는 없으나 간혹 특정 line protocol이 host tag를 가지지 않을 수 있으니 이런 경우는 다음 내용을 참고하여 설정하도록 한다. 
Usually, performance information is transmitted from several VMs, so it is necessary to identify the host name. 
Generally, it is not necessary to change the setting, but sometimes a certain line protocol may not have a host tag. 
 
The host name is sent to the `host` tag of the line protocol. If the tag that identifies host name is not `host`, you can change it with the following option.
  - `input_telegraf_$redis$_host_tag`

If the transmitted host name is different from the host name set in the scouter, you can also configure the mappings. 
(Usually the same.) 
  - `input_telegraf_$redis$_counter_mappings`
    - eg) `input_telegraf_$redis$_counter_mappings=hostname1:hostname-S1,hostname2:hostname-S2`
If you configure as above, you can also configure easily by using the screen of Scouter client. 
  - Enter `input_telegraf_$redis$_counter_mappings =` on the client's configuration input screen and double click on it, the following input window will pop up. 

![configure-widow-hostmapping.png](../img/main/configure-widow-hostmapping.png)

#### 3.3. Object name settings
The scouter calls the monitored things **object**. 
Now let's set the name of this object. 
 
The object name is the actually set to `/host-name/object-name`, so if the host is different, the same object name is OK.

```properties
input_telegraf_$redis$_objName_base=redis
input_telegraf_$redis$_objName_append_tags=port
```
I have set `port` in` input_telegraf_$redis$_objName_append_tags`, assuming there are multiple redis in one host. 
The incoming data(line protocol) is shown below.

>redis,**host**=sc-api-demo-s01.localdomain,**port**=30779,**scouter_obj_type_prefix**=SC-DEMO,**server**=172-0-0-0.internal  keyspace_hits=5507814i,expired_keys=1694047i,total_commands_processed=17575212i 1535289664000000000

The first `redis` is **measurement**, followed by` host`, `port`,` obj_type_prefix`, and `server` are **tag**. 
The following numeric information is called **field** in the line protocol. There are `keyspace_hits`,` expired_keys`, and `total_commands_processed` fields. 
 
The object name defined by the above setting will eventually become **redis_30779**. 
If you set `input_telegraf_$redis$_objName_append_tags` to `server,port`, the object name will be **redis_172-0-0-0.internal_30779**.

#### 3.4. Object type settings
Object type is a set of objects that are monitored at one time by the scouter. 
Generally, you can think of the same families in a system. 
 
예를 들어 Order 시스템의 여러 redis 인스턴스는 한번에 모니터링해야 하는 대상일 것이다. 
이러한 단위를 scouter에서는 **object type**이라고 한다. 
따라서 이 redis 인스턴스들의 object type을 정한다면, **ORDER\_SYSTEM\_redis**와 같은 식으로 정할 수 있을 것이다. 
여기서 앞에 붙는 ORDER\_SYSTEM과 같은 prefix를 telegraf의 tag에 추가하고 scouter에서는 이를 조합하여 object type을 정하게 된다. 
이 tag명은 `scouter_obj_type_prefix`이며, 이 값은 `input_telegraf_$redis$_objType_prepend_tags=xxx` 설정을 통해 변경할 수도 있다. 

For example, multiple redis instances of the Order system might be targets that you need to monitor at once. 
These units are called **object type** in the scouter. 
Therefore, if you specify the object type of these redis instances, you can set them like **ORDER\_SYSTEM\_redis**. 
Here, prefixes such as `ORDER_SYSTEM` which precedes them are added to the telegraf's tag, and the scouter combines them to determine the object type. 
This tag name is `scouter_obj_type_prefix` and this value can be changed by setting  `input_telegraf_$redis$_objType_prepend_tags=xxx`.
 
```properties
input_telegraf_$redis$_objType_base=redis
#input_telegraf_$redis$_objType_prepend_tags=scouter_obj_type_prefix
#input_telegraf_$redis$_objType_append_tags=
```
This sets the object type to **X$SC-DEMO_redis**. 
  
There are many ways to add tags to telegraf. The easiest way is to set them with global tags. 
Or you can add tags to each input, and you can set it up in various ways. 
 
```javascript
[global_tags]
  ## To prevent overlap with the built-in object types, 'X$' is automatically added by the scouter.
  scouter_obj_type_prefix = "SC-DEMO"
```

#### 3.5. Counter mapping settings
Finally, set the field of the line protocol to the counter of the scouter.
```properties
input_telegraf_$redis$_counter_mappings=keyspace_hits:ks-hits:ks-hits:ea:true,&expired_keys:expired::ea:true
```
Specify the comma-separated information for the field to be monitored as above. 
Fields that are not set here are discarded, so it is better to avoid fields that are not being monitored, so that they are discarded in the telegraf and are not forwarded to the scouter. 
The setting items of each field are separated by a colon. 

 - `fn:cn:cd?:u?:t?:s?`
   - `fn` - field name, **required**
     - The field name passed in the line protocol.
     - If you precede this name with &, it is designated as a delta counter.
       - The delta counter is a counter showing the amount of change per second.
       - If delta counter is specified, **_$delta** is added to name and **/s** is added to unit.
     - If you put 2-&('&&') before this name, you have both normal and delta counters.
   - `cn` - counter name, **required**
     - It is the counter name in the scouter.
   - `cd` - counter desc - optional, default : counter name
     - This value is used when the scouter displays the counter on the screen.
   - `u` - unit - optional
     - It is the unit of the value.
   - `t` - totalizable - optional, default : true
     - Whether this value can be summarized.
     - For example, throughput is true and memory utilization percent is false (it is strange to sum the memory usage percent of several VMs)
     - If this value is true, you can open the total chart on the screen of the scouter.
   - `s` - nomalizing seconds
     - This is the size of the time window to obtain the mean value of the counter.
     - default 0s for normal counter, default 30s for delta counter.

Likewise, it is easier to configure via the client screen. 

![configure-widow-countermapping](../img/main/configure-widow-countermapping.png)


#### 3.6. Counter mapping - tag filter
It can only be collected if the tag has a certain tag value. 
For example, when collecting cpu information of a VM with 4 cpu, the usage amount of each cpu and the usage amount of the entire cpu are all collected and can be classified by a specific tag value._
_f you want to collect only if the value of `cpu` tag is `cpu-total` or `cpu-0`, set it as follows.
```properties
input_telegraf_$cpu$_tag_filter=cpu:cpu-total,cpu:cpu-0
```

If you want to collect only the value of `cpu` tag except `cpu-total`, set this.
```properties
input_telegraf_$cpu$_tag_filter=cpu:!cpu-total
```


### 4. check counters.site.xml 
If the telegraf performance information is requested to the scouter collector after setting up as above, the meta information about the counter is automatically registered in counters.site.xml. 
However, even if you delete it from the configuration, the counter meta information is not deleted. 
To delete it, you have to delete it directly in counters.site.xml. Especially, if you modify the same counter many times, garbage of the same type may remain. Check this in counters.site.xml and modify it appropriately. 
(If you modify counters.site.xml, you must restart the collector server.) 
 
counters.site.xml is located in the collector's conf directory. 
  - counters-site.xml Example (Below are the automatically registered counters. The contents were briefly revised.)
```xml
<Counters>
<Types>
    <ObjectType disp="SC-DEMO_java" family="javaee" icon="tomcat" name="SC-DEMO_java"/>
    <ObjectType disp="SC-DEMO_mysql" family="X$mysql" icon="mysql" name="SC-DEMO_mysql"/>
    <ObjectType disp="SC-DEMO_redis" family="X$redis" icon="redis" name="SC-DEMO_redis"/>
    <ObjectType disp="SC-DEMO_nginx" family="X$nginx" icon="nginx" name="SC-DEMO_nginx"/>
  </Types>

  <Familys>
    <Family name="X$mysql">
      <Counter disp="com_update_$delta" name="com_update_$delta" unit="/s"/>
      <Counter disp="connections" name="connections" unit=""/>
    </Family>
    <Family name="X$redis">
      <Counter disp="total_commands_processed" name="total_commands_processed" total="true" unit="ea"/>
      <Counter disp="total_commands_processed_$delta" name="total_commands_processed_$delta" unit="ea/s"/>
    </Family>
    <Family name="X$nginx">
      <Counter disp="active-conn-working" name="writing" total="true" unit="ea"/>
      <Counter disp="requests_$delta" name="request-count_$delta" total="true" unit="ea/s"/>
    </Family>
  </Familys>
</Counters>
```

